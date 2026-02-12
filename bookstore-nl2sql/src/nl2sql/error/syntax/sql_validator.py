from sqlalchemy import text
import sqlparse
from sqlparse.sql import Identifier, IdentifierList, Statement
from sqlparse.tokens import DML, Keyword

from nl2sql.models.sql_validation_result import SqlValidationResult
from sqlalchemy.orm import Session
from sqlalchemy.exc import ProgrammingError, OperationalError

class SQLValidator:
    def __init__(self, schema_tables: list[str] | None = None):
        self.schema_tables = set(schema_tables) if schema_tables else set()

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

        # 세미콜론 확인
        formatted_sql = sqlparse.format(sql.strip(), reindent=True, keyword_case="upper")
        if not formatted_sql.rstrip().endswith(";"):
            return SqlValidationResult(
                is_valid=False,
                error_message="SQL 쿼리가 세미콜론으로 끝나지 않습니다.",
                parsed_sql=formatted_sql + ";",
                suggestion=["SQL 쿼리의 끝에 세미콜론(;)을 추가해주세요."],
            )

        # 테이블 존재 여부 검증
        if self.schema_tables:
            table_validation = self._validate_tables(statement)
            if not table_validation.is_valid:
                return table_validation

        # 컬럼 존재 여부 및 데이터 타입 불일치 검증
        if session:
            explain_result = self._validate_with_explain(formatted_sql, session)
            if not explain_result.is_valid:
                return explain_result

        return SqlValidationResult(is_valid=True, parsed_sql=formatted_sql)

    def _is_select_query(self, statement: Statement) -> bool:
        first_token = statement.token_first(skip_ws=True, skip_cm=True)
        if first_token and first_token.ttype is DML:
            return first_token.value.upper() == "SELECT"
        return False

    def _validate_tables(self, statement: Statement) -> SqlValidationResult:
        tables_in_query = self._extract_tables(statement)
        invalid_tables = [t for t in tables_in_query if t not in self.schema_tables]

        if invalid_tables:
            return SqlValidationResult(
                is_valid=False,
                error_message=f"존재하지 않는 테이블이 포함되어 있습니다: {', '.join(invalid_tables)}",
                suggestion=[f"사용 가능한 테이블 = {', '.join(sorted(self.schema_tables))}"],
            )
        return SqlValidationResult(is_valid=True)

    def _extract_tables(self, statement: Statement) -> list[str]:
        tables = []
        from_seen = False

        for token in statement.tokens:
            if from_seen:
                if isinstance(token, IdentifierList):
                    for identifier in token.get_identifiers():
                        tables.append(identifier.get_real_name())
                elif isinstance(token, Identifier):
                    tables.append(token.get_real_name())

                from_seen = False

            if token.ttype is Keyword and token.value.upper() == "FROM":
                from_seen = True

            if token.ttype is Keyword and "JOIN" in token.value.upper():
                from_seen = True

        return tables

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