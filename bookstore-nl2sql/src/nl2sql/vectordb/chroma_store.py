import chromadb
import yaml
from chromadb.config import Settings


class ChromaStore:
    """
    ChromaDB 벡터 스토어 관리 클래스
    """

    def __init__(self, config_path: str = "config/vectorstore.yaml"):
        """
        Args:
            config_path: 벡터 스토어 설정 YAML 파일 경로
        """

        with open(config_path) as f:
            config = yaml.safe_load(f)

        self.chroma_config = config["chroma"]

        import os

        host = os.getenv("CHROMA_HOST", self.chroma_config["host"])
        port = os.getenv("CHROMA_PORT", str(self.chroma_config["port"]))

        self.chroma_client = chromadb.HttpClient(
            host=host,
            port=port,
            settings=Settings(
                anonymized_telemetry=False,
                allow_reset=True,
            ),
        )

        self.schema_collection = None
        self.few_shot_collection = None

    def init_schema_collection(self, reset: bool = False):
        """
        스키마 컬렉션 초기화

        Args:
            reset: 기존 컬렉션 삭제 여부
        """
        if reset:
            try:
                self.chroma_client.delete_collection(name="schema_store")
            except Exception as e:
                print(f"컬렉션 삭제 실패: {e}")

        self.schema_collection = self.chroma_client.get_or_create_collection(
            name="schema_store", metadata={"description": "데이터베이스 스키마 정보"}
        )

    def init_few_shot_collection(self, reset: bool = False):
        """
        few shot 컬렉션 초기화

        Args:
            reset: 기존 컬렉션 삭제 여부
        """
        if reset:
            try:
                self.chroma_client.delete_collection(name="few_shot_store")
            except Exception as e:
                print(f"컬렉션 삭제 실패: {e}")

        self.few_shot_collection = self.chroma_client.get_or_create_collection(
            name="few_shot_store", metadata={"description": "few shot 데이터"}
        )

    def add_schema(
        self, table_name: str, embedding: list[float], document: str, metadata: dict
    ) -> None:
        """
        스키자 정보 저장

        Args:
            table_name: 테이블 이름
            embedding: 임베딩 벡터
            document: 스키마 정보
            metadata: 메타데이터
        """

        if self.schema_collection is None:
            raise ValueError("스키마 컬렉션이 초기화되지 않았습니다.")

        self.schema_collection.add(
            ids=[table_name], embeddings=[embedding], documents=[document], metadatas=[metadata]
        )

    def add_few_shot(
        self, example_id: str, embedding: list[float], query: str, metadata: dict
    ) -> None:
        """
        few shot 데이터 저장

        Args:
            example_id: 예제 고유 ID
            embedding: 임베딩 벡터
            query: 자연어 질문
            metadata: 메타데이터
        """

        if self.few_shot_collection is None:
            raise ValueError("few shot 컬렉션이 초기화되지 않았습니다.")

        self.few_shot_collection.add(
            ids=[example_id], embeddings=[embedding], documents=[query], metadatas=[metadata]
        )

    def search_schema(self, query_embedding: list[float], top_k: int = 3) -> list[dict]:
        """
        스키마 검색

        Args:
            query_embedding: 쿼리 임베딩 벡터
            top_k: 검색할 상위 k개 결과

        Returns:
            검색 결과 리스트
        """
        if self.schema_collection is None:
            raise ValueError("스키마 컬렉션이 초기화되지 않았습니다.")

        results = self.schema_collection.query(query_embeddings=[query_embedding], n_results=top_k)

        return [
            {
                "table_name": results["ids"][0][i],
                "document": results["documents"][0][i],
                "metadata": results["metadatas"][0][i],
                "distance": results["distances"][0][i],
            }
            for i in range(len(results["ids"][0]))
        ]

    def search_few_shot(self, query_embedding: list[float], top_k: int = 3) -> list[dict]:
        """
        few shot 데이터 검색

        Args:
            query_embedding: 쿼리 임베딩 벡터
            top_k: 검색할 상위 k개 결과

        Returns:
            검색 결과 리스트
        """
        if self.few_shot_collection is None:
            raise ValueError("few shot 컬렉션이 초기화되지 않았습니다.")

        results = self.few_shot_collection.query(
            query_embeddings=[query_embedding], n_results=top_k
        )

        return [
            {
                "example_id": results["ids"][0][i],
                "query": results["documents"][0][i],
                "metadata": results["metadatas"][0][i],
                "distance": results["distances"][0][i],
            }
            for i in range(len(results["ids"][0]))
        ]

    def health_check(self) -> bool:
        """
        ChromaDB 연결 상태 확인
        """
        try:
            self.chroma_client.heartbeat()
            return True
        except Exception as e:
            print(f"ChromaDB 연결 실패: {e}")
            return False
