import difflib
import json
import re

import sqlparse
from sqlalchemy import text
from sqlalchemy.exc import OperationalError, ProgrammingError
from sqlalchemy.orm import Session
from sqlparse.sql import Statement
from sqlparse.tokens import DML

from nl2sql.common.sql_parser import extract_table_aliases_from_statement
from nl2sql.embedding.embedding import get_embedding_model
from nl2sql.models.sql_validation_result import SqlValidationResult
from nl2sql.vectordb.chroma_store import ChromaStore

class SQLValidator:
    def __init__(self, schema_tables: list[str] | None = None):
        self.schema_tables = set(schema_tables) if schema_tables else set()
        self._embedding_model = None
        self._chroma = None
        self._schema_cache: dict[str, dict] = {}

    def _init_schema_search(self) -> None:
        if self._embedding_model is not None and self._chroma is not None:
            return
        self._embedding_model = get_embedding_model()
        self._chroma = ChromaStore()
        self._chroma.init_schema_collection(reset=False)

    def validate(self, sql: str, session: Session | None = None) -> SqlValidationResult:
        """SQL 문법 검증"""

        # 1. 빈 쿼리
        if not sql or not sql.strip():
            return SqlValidationResult(
                is_valid=False,
                error_message="SQL 쿼리가 비어 있습니다.",
                suggestion=["SQL 쿼리를 입력해주세요."],
            )

        # 2. SQL 파싱
        try:
            parsed = sqlparse.parse(sql)
        except Exception as e:
            return SqlValidationResult(
                is_valid=False,
                error_message=f"SQL 파싱 실패: {str(e)}",
                suggestion=["SQL 문법을 확인해주세요."],
            )

        # 다중 쿼리
        if len(parsed) > 1:
            return SqlValidationResult(
                is_valid=False,
                error_message="다중 쿼리는 지원하지 않습니다.",
                suggestion=["하나의 쿼리만 입력해주세요."],
            )

        if not parsed:
            return SqlValidationResult(
                is_valid=False,
                error_message="SQL 쿼리가 비어 있습니다.",
                suggestion=["SQL 쿼리를 입력해주세요."],
            )

        statement = parsed[0]

        # SELECT 문 검증
        if not self._is_select_query(statement):
            return SqlValidationResult(
                is_valid=False,
                error_message="SELECT 문만 지원합니다., INSERT, UPDATE, DELETE 문은 지원하지 않습니다.",
                suggestion=["SELECT 문을 입력해주세요."],
            )

        formatted_sql = sqlparse.format(sql.strip(), reindent=True, keyword_case="upper")

        # 세미콜론 보정
        formatted_sql = self._ensure_semicolon(formatted_sql)
        statement = sqlparse.parse(formatted_sql)[0]

        # 테이블/컬럼 환각 보정
        if self.schema_tables:
            formatted_sql, statement = self._auto_rewrite_schema_hallucination(
                formatted_sql,
                statement,
            )

        # 컬럼 존재 여부 및 데이터 타입 불일치 검증
        if session:
            explain_result = self._validate_with_explain(formatted_sql, session)
            if not explain_result.is_valid:
                return explain_result

        return SqlValidationResult(is_valid=True, parsed_sql=formatted_sql)

    # SELECT 문 검증
    def _is_select_query(self, statement: Statement) -> bool:
        first_token = statement.token_first(skip_ws=True, skip_cm=True)
        if first_token and first_token.ttype is DML:
            return first_token.value.upper() == "SELECT"
        return False

    # 세미콜론 보정
    def _ensure_semicolon(self, sql: str) -> str:
        fixed = sql.strip()
        if not fixed.endswith(";"):
            fixed += ";"
        return fixed

    # 테이블/컬럼 환각 보정
    def _auto_rewrite_schema_hallucination(
        self,
        sql: str,
        statement: Statement,
    ) -> tuple[str, Statement]:
        rewritten_sql = sql
        table_alias_map = extract_table_aliases_from_statement(statement)

        for alias, table_name in list(table_alias_map.items()):
            if table_name in self.schema_tables:
                continue
            replacement = self._semantic_table_match(table_name)
            if not replacement or replacement == table_name:
                continue
            rewritten_sql = self._replace_table_identifier(rewritten_sql, table_name, replacement)
            table_alias_map[alias] = replacement

        reparsed = sqlparse.parse(rewritten_sql)
        if reparsed:
            statement = reparsed[0]
            table_alias_map = extract_table_aliases_from_statement(statement)

        qualified_refs = re.findall(
            r"\b([A-Za-z_][A-Za-z0-9_]*)\s*\.\s*([A-Za-z_][A-Za-z0-9_]*)\b",
            rewritten_sql,
        )
        for qualifier, column_name in qualified_refs:
            table_name = table_alias_map.get(qualifier)
            if not table_name or table_name not in self.schema_tables:
                continue

            valid_columns = self._get_table_columns(table_name)
            if not valid_columns or column_name in valid_columns:
                continue

            replacement = self._closest_column(column_name, valid_columns)
            if not replacement or replacement == column_name:
                continue

            rewritten_sql = self._replace_qualified_column(
                rewritten_sql,
                qualifier=qualifier,
                old_column=column_name,
                new_column=replacement,
            )

        reparsed = sqlparse.parse(rewritten_sql)
        if reparsed:
            return rewritten_sql, reparsed[0]
        return rewritten_sql, statement

    # 테이블 식별자 교체
    def _replace_table_identifier(self, sql: str, old_table: str, new_table: str) -> str:
        pattern = re.compile(rf"\b{re.escape(old_table)}\b", re.IGNORECASE)
        return pattern.sub(new_table, sql)

    # 한정된 컬럼 교체
    def _replace_qualified_column(
        self,
        sql: str,
        qualifier: str,
        old_column: str,
        new_column: str,
    ) -> str:
        pattern = re.compile(
            rf"\b{re.escape(qualifier)}\s*\.\s*{re.escape(old_column)}\b",
            re.IGNORECASE,
        )
        return pattern.sub(f"{qualifier}.{new_column}", sql)

    def _semantic_table_match(self, table_name: str) -> str | None:
        try:
            self._init_schema_search()
            query_embedding = self._embedding_model.encode_single(table_name)
            results = self._chroma.search_schema(query_embedding, top_k=1)
            if not results:
                return None
            return results[0]["table_name"]
        except Exception:
            return None

    def _get_table_columns(self, table_name: str) -> set[str]:
        if table_name in self._schema_cache:
            return set(self._schema_cache[table_name].get("columns", []))

        try:
            self._init_schema_search()
            query_embedding = self._embedding_model.encode_single(table_name)
            results = self._chroma.search_schema(query_embedding, top_k=1)
            if not results:
                self._schema_cache[table_name] = {"columns": []}
                return set()

            metadata = results[0].get("metadata", {})
            raw_columns = metadata.get("columns", "[]")
            columns_obj = json.loads(raw_columns) if isinstance(raw_columns, str) else raw_columns
            columns = [c.get("name") for c in columns_obj if isinstance(c, dict) and c.get("name")]
            self._schema_cache[table_name] = {"columns": columns}
            return set(columns)
        except Exception:
            self._schema_cache[table_name] = {"columns": []}
            return set()

    def _closest_column(self, column_name: str, candidates: set[str]) -> str | None:
        if not candidates:
            return None
        matches = difflib.get_close_matches(column_name, list(candidates), n=1, cutoff=0.0)
        return matches[0] if matches else None

    def _validate_with_explain(self, sql: str, session: Session) -> SqlValidationResult:
        clean_sql = sql.rstrip().rstrip(";")

        try:
            session.execute(text(f"EXPLAIN {clean_sql}"))
            return SqlValidationResult(is_valid=True, parsed_sql=sql)
        except OperationalError as e:
            error_msg = str(e.orig) if hasattr(e, "orig") else str(e)
            suggestion = self._build_explain_suggestion(error_msg)
            return SqlValidationResult(
                is_valid=False,
                error_message=f"[EXPLAIN 검증 실패] {error_msg}",
                suggestion=suggestion,
                parsed_sql=sql,
            )
        except ProgrammingError as e:
            error_msg = str(e.orig) if hasattr(e, "orig") else str(e)
            suggestion = self._build_explain_suggestion(error_msg)
            return SqlValidationResult(
                is_valid=False,
                error_message=f"[EXPLAIN 검증 실패] {error_msg}",
                suggestion=suggestion,
                parsed_sql=sql,
            )

    def _build_explain_suggestion(self, error_msg: str) -> list[str]:
        suggestions = []
        error_lower = error_msg.lower()

        if "unknown column" in error_lower:
            suggestions.append("존재하지 않는 컬럼이 사용되었습니다. 스키마의 실제 컬럼명을 확인해주세요.")
        elif "truncated incorrect" in error_lower or "incorrect" in error_lower:
            suggestions.append(
                "데이터 타입이 일치하지 않습니다. "
                "WHERE 절이나 함수 인자의 타입을 확인하세요. "
                "(예: 문자열 컬럼에 숫자 비교, 날짜 컬럼에 잘못된 형식)"
            )
        elif "doesn't exist" in error_lower:
            suggestions.append("존재하지 않는 테이블이 사용되었습니다. 스키마의 실제 테이블명을 확인해주세요.")
        else:
            suggestions.append(f"[EXPLAIN] 실행 중 오류가 발생했습니다. {error_msg}")
        
        return suggestions
