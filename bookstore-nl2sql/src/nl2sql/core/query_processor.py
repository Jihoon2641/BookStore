"""
쿼리 처리 워크플로우

1. 스키마 검색
2. 예제 검색
3. 프롬프트 구성
4. LLM 호출
5. SQL 반환
"""

from nl2sql.models.query import SchemaContext
from nl2sql.models.query import QueryResponse
from nl2sql.models.query import FewShotContext
from chromadb.api.types import QueryRequest
from typing import Tuple
from typing import Dict
from typing import List
from nl2sql.core.prompt_template import PromptTemplate
from nl2sql.vectordb.chroma_store import ChromaStore
from openai import OpenAI
from nl2sql.embedding.embedding import get_embedding_model
import time
from datetime import datetime

class QueryProcessor:

    def __init__(
        self,
        open_ai_key: str = None,
        model: str = "gpt-4o-mini"
    ):
        self.chroma = ChromaStore()
        self.embedding_model = get_embedding_model()
        self.llm = OpenAI(
            api_key=open_ai_key
        )
        self.model = model

        self.prompt_template = PromptTemplate()

        self.chroma.init_schema_collection(reset=False)
        self.chroma.init_few_shot_collection(reset=False)

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

        system_prompt = self.prompt_template.SYSTEM_PROMPT
        user_prompt = self.prompt_template.build_user_prompt(
            question=question,
            schemas=schema_context,
            few_shot=few_shot_context
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

        sql = response.choices[0].message.content.strip()

        return sql

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
        
        sql = self._call_llm(system_prompt, user_prompt)

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
            timestamp=datetime.now()
        )
