"""
쿼리 처리 워크플로우

1. 사용자 질문 입력
2. RAG로 스키마/예제 검색
3. LLM으로 SQL 생성
4. SQL 검증
5. [실패 시] 재생성
6. 최종 결과 반환
"""

from langchain_core.output_parsers import PydanticOutputParser
from nl2sql.error.syntax.sql_validator import SQLValidator
from nl2sql.models.sql_validation_result import SqlValidationResult
from typing import Optional
from nl2sql.models.query import SchemaContext, QueryResponse, FewShotContext, QueryRequest
from typing import Tuple, Dict, List
from nl2sql.core.prompt.prompt_template import PromptTemplate
from nl2sql.vectordb.chroma_store import ChromaStore
from openai import OpenAI
from nl2sql.embedding.embedding import get_embedding_model
import time
from datetime import datetime
from sqlalchemy import text
from nl2sql.models.llm_output import SQLOutput
from nl2sql.core.database.db_connector import DBConnector

from loguru import logger

class QueryProcessor:

    def __init__(
        self,
        open_ai_key: str = None,
        model: str = "gpt-4o-mini",
        enable_validation: bool = True
    ):
        self.chroma = ChromaStore()
        self.embedding_model = get_embedding_model()
        self.llm = OpenAI(
            api_key=open_ai_key
        )
        self.model = model
        self.enable_validation = enable_validation
        self.prompt_template = PromptTemplate()

        self.db_connector = DBConnector()

        self.output_parser = PydanticOutputParser(pydantic_object=SQLOutput)

        # 스키마 초기화
        self.chroma.init_schema_collection(reset=False)
        self.chroma.init_few_shot_collection(reset=False)

        self.validator = None
        if self.enable_validation:
            self._init_validator()

    def _init_validator(self):
        """
        SQL 검증기 초기화
        """
        try:
            with self.db_connector.get_db() as session:
                result = session.execute(text("SHOW TABLES"))
                tables = [row[0] for row in result]
                self.validator = SQLValidator(tables)
        except Exception as e:
            logger.error(f"SQL 검증기 초기화 실패: {e}")
            self.validator = None

    def _search_schema(self, question: str, top_k: int = 3) -> List[Dict]:
        """
        질문과 관련된 스키마 검색

        Args:
            question: 자연어 질문
            top_k: 검색할 스키마 개수
            
        Returns:
            검색된 스키마 리스트
        """
        
        query_embedding = self.embedding_model.encode_single(question)
        results = self.chroma.search_schema(query_embedding, top_k=top_k)
        return results

    def _search_few_shot(self, question: str, top_k: int = 5) -> List[Dict]:
        """
        질문과 관련된 Few-shot 예제 검색

        Args:
            question: 자연어 질문
            top_k: 검색할 예제 개수
            
        Returns:
            검색된 예제 리스트
        """
        
        query_embedding = self.embedding_model.encode_single(question)
        results = self.chroma.search_few_shot(query_embedding, top_k=top_k)
        return results

    def _build_prompt(
        self,
        question: str,
        schemas: List[Dict],
        few_shot: List[Dict]
    ) -> Tuple[str, str]:
        """
        LLM 호출을 위한 프롬프트 구성

        Args:
            question: 자연어 질문
            schemas: 검색된 스키마 리스트
            few_shot: 검색된 Few-shot 예제 리스트
            
        Returns:
            (프롬프트, 시스템 메시지)
        """
        
        schema_context = self.prompt_template.format_schema_context(schemas)
        few_shot_context = self.prompt_template.format_few_shot_context(few_shot)

        format_instructions = self.output_parser.get_format_instructions()

        system_prompt = self.prompt_template.SYSTEM_PROMPT
        user_prompt = self.prompt_template.build_user_prompt(
            question=question,
            schemas=schema_context,
            few_shot=few_shot_context,
            format_instructions=format_instructions
        )

        return user_prompt, system_prompt

    def _call_llm(self, system_prompt: str, user_prompt: str) -> str:
        """
        LLM 호출

        Args:
            system_prompt: 시스템 메시지
            user_prompt: 사용자 메시지
            
        Returns:
            LLM 응답
        """
        
        response = self.llm.chat.completions.create(
            model=self.model,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt}
            ],
            temperature=0.1,
        )

        raw_output = response.choices[0].message.content.strip()

        try:
            parsed_output = self.output_parser.parse(raw_output)

            return parsed_output
        except Exception as e:
            logger.error(f"SQL 파싱 실패: {e}")

            return SQLOutput(sql=raw_output)

    def _validate_sql(self, sql: str) -> Tuple[bool, Optional[str], Optional[str], List[str]]:
        """
        SQL 검증
        
        Args:
            sql: 검증할 SQL 쿼리
            
        Returns:
            (검증 통과 여부, 오류 메시지, 파싱된 SQL, 수정 제안)
        """
        
        if not self.enable_validation or not self.validator:
            logger.warning("SQL 검증기가 초기화 되지 않았습니다. 검증을 수행하지 않습니다.")
            return True, None, sql, []
        
        validation_result = self.validator.validate(sql)

        if validation_result.is_valid:
            logger.info("SQL 검증 성공")
            return True, None, validation_result.parsed_sql, []     
        else:
            logger.warning("SQL 검증 실패: %s", validation_result.error_message)
            return (
                False,
                validation_result.error_message,
                sql,
                validation_result.suggestion
            )

    def process(
        self,
        request: QueryRequest,
        schema_top_k: int = 3,
        few_shot_top_k: int = 2
    ) -> QueryResponse:
        """
        전체 쿼리 처리 워크플로우

        Args:
            request: 쿼리 요청
            schema_top_k: 검색할 스키마 개수
            few_shot_top_k: 검색할 예제 개수
            
        Returns:
            쿼리 응답
        """

        start_time = time.time()

        schemas = self._search_schema(request.query, top_k=schema_top_k)
        few_shot = self._search_few_shot(request.query, top_k=few_shot_top_k)

        user_prompt, system_prompt = self._build_prompt(
            question=request.query,
            schemas=schemas,
            few_shot=few_shot
        )
        
        sql_output = self._call_llm(system_prompt, user_prompt)
        sql = sql_output.sql

        is_valid, error_message, parsed_sql, suggestions = self._validate_sql(sql)

        end_time = time.time()

        return QueryResponse(
            question=request.query,
            sql=sql,
            generator_type="RAG",
            confidence=None,
            retrieved_schema=[
                SchemaContext(
                    table_name=s["table_name"],
                    description=s["metadata"]["description"],
                    columns=s["metadata"]["columns"],
                    foreign_keys=s["metadata"]["foreign_keys"],
                    distance=s["distance"]
                ) for s in schemas
            ],
            retrieved_few_shot=[
                FewShotContext(
                    question=f["question"],
                    sql=f["metadata"]["sql"],
                    explanation=f["metadata"]["explanation"],
                    distance=f["distance"]
                ) for f in few_shot
            ],
            execution_time_ms=int((end_time - start_time) * 1000),
            timestamp=datetime.now(),
            validation_passed=is_valid,
            validation_error=error_message,
            parsed_sql=parsed_sql
        )

    def regenerate_sql(
        self,
        original_request: QueryRequest,
        previous_sql: str,
        error_message: str,
        suggestions: List[str],
        schemas: List[Dict],
        few_shot: List[Dict]
    ) -> QueryResponse:
        """
        SQL 재생성

        Args:
            original_request: 원본 쿼리 요청
            previous_sql: 이전 SQL 쿼리
            error_message: 오류 메시지
            suggestions: 제안된 SQL 리스트
            schemas: 검색된 스키마 리스트
            few_shot: 검색된 Few-shot 예제 리스트
            
        Returns:
            쿼리 응답
        """

        start_time = time.time()
        
        schema_context = self.prompt_template.format_schema_context(schemas)
        few_shot_context = self.prompt_template.format_few_shot_context(few_shot)
        format_instructions = self.output_parser.get_format_instructions()

        user_prompt = self.prompt_template.build_retry_prompt(
            question=original_request.query,
            schemas=schema_context,
            few_shot=few_shot_context,
            format_instructions=format_instructions,
            previous_sql=previous_sql,
            error_message=error_message,
            suggestions=suggestions
        )

        system_prompt = self.prompt_template.SYSTEM_PROMPT

        sql_output = self._call_llm(system_prompt, user_prompt)
        sql = sql_output.sql

        is_valid, error_message, parsed_sql, suggestions = self._validate_sql(sql)

        end_time = time.time()

        return QueryResponse(
            question=original_request.query,
            sql=sql,
            generator_type="RAG",
            confidence=None,
            retrieved_schema=[
                SchemaContext(
                    table_name=s["table_name"] if isinstance(s, dict) else s.table_name,
                    description=s["metadata"]["description"] if isinstance(s, dict) else s.description,
                    columns=s["metadata"]["columns"] if isinstance(s, dict) else s.columns,
                    foreign_keys=s["metadata"]["foreign_keys"] if isinstance(s, dict) else s.foreign_keys,
                    distance=s["distance"] if isinstance(s, dict) else s.distance
                ) for s in schemas
            ],
            retrieved_few_shot=[
                FewShotContext(
                    question=f["question"] if isinstance(f, dict) else f.question,
                    sql=f["metadata"]["sql"] if isinstance(f, dict) else f.sql,
                    explanation=f["metadata"]["explanation"] if isinstance(f, dict) else f.explanation,
                    distance=f["distance"] if isinstance(f, dict) else f.distance
                ) for f in few_shot
            ],
            execution_time_ms=int((end_time - start_time) * 1000),
            timestamp=datetime.now(),
            validation_passed=is_valid,
            validation_error=error_message,
            parsed_sql=parsed_sql
        )

    def close(self):
        """
        자원 해제
        """
        if self.db_connector:
            self.db_connector.close()