from datetime import datetime
from nl2sql.models.query import QueryResponse, QueryRequest, SchemaContext, FewShotContext
from nl2sql.chain.retry_chain import create_retry_chain
from nl2sql.chain.validation_chain import create_validation_chain
from nl2sql.chain.generation_chain import create_generation_chain
from nl2sql.chain.retrieval_chain import create_retrieval_chain
from nl2sql.core.database.db_connector import DBConnector
from nl2sql.embedding.embedding import get_embedding_model
from nl2sql.vectordb.chroma_store import ChromaStore
import time

class NL2SQLProcessor:
    
    def __init__(self, openai_key: str, model: str):
        self.chroma = ChromaStore()
        self.embedding_model = get_embedding_model()
        self.db_connector = DBConnector()

        self.chroma.init_schema_collection(reset=False)
        self.chroma.init_few_shot_collection(reset=False)

        self.retrieval_chain = create_retrieval_chain(
            self.chroma,
            self.embedding_model
        )

        self.generation_chain = create_generation_chain(
            model,
            openai_key
        )

        self.validation_chain = create_validation_chain(
            self.db_connector
        )

        self.retry_chain = create_retry_chain(
            self.generation_chain,
            self.validation_chain
        )

        self.pipeline = (
            self.retrieval_chain
            | self.generation_chain
            | self.validation_chain
            | self.retry_chain
        )

    def process(self, request: QueryRequest) -> QueryResponse:

        start_time = time.time()

        result = self.pipeline.invoke({"query": request.query})

        end_time = time.time()

        return QueryResponse(
            question=request.query,
            sql=result["sql"],
            generator_type="RAG",
            confidence=None,
            # Pydantic 모델로 명시적 변환
            retrieved_schema=[
                SchemaContext(
                    table_name=s["table_name"],
                    description=s["metadata"]["description"],
                    columns=s["metadata"]["columns"],
                    foreign_keys=s["metadata"]["foreign_keys"],
                    distance=s["distance"]
                )
                for s in result["schemas"]
            ],
            retrieved_few_shot=[
                FewShotContext(
                    question=f["question"],
                    sql=f["metadata"]["sql"],
                    explanation=f["metadata"]["explanation"],
                    distance=f["distance"]
                )
                for f in result["few_shot"]
            ],
            explanation=result.get("explanation", ""),
            execution_time_ms=int((end_time - start_time) * 1000),
            timestamp=datetime.now(),
            validation_passed=result["is_valid"],
            validation_error=result.get("error_message"),
            parsed_sql=result.get("parsed_sql")
        )