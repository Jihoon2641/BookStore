from nl2sql.embedding.embedding import get_embedding_model
from nl2sql.vectordb.chroma_store import ChromaStore


class DuplicateChecker:
    def __init__(self, distance_threshold: float = 0.20):
        self.distance_threshold = distance_threshold
        self.embedding_model = get_embedding_model()
        self.chroma = ChromaStore()
        self.chroma.init_few_shot_collection(reset=False)

    def is_duplicate(self, question: str) -> tuple[bool, str]:
        query_embedding = self.embedding_model.encode_single(question)
        results = self.chroma.search_few_shot(query_embedding, top_k=1)
        if not results:
            return False, "가장 유사한 질문 없음"

        top1 = results[0]
        distance = top1["distance"]
        if distance <= self.distance_threshold:
            return True, f"의미적 일치:{top1['example_id']} 거리={distance:.4f}"

        return False, f"유사한 질문 없음, 거리={distance:.4f}"
