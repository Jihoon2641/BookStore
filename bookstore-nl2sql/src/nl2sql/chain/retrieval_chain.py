from langchain_core.runnables import RunnableLambda, RunnableParallel, chain
from nl2sql.embedding.embedding import EmbeddingModel
from nl2sql.vectordb.chroma_store import ChromaStore

def _extract_query_text(query) -> str:
    if isinstance(query, dict):
        return query.get("query", "")
    return query

def create_retrieval_chain(chroma_store: ChromaStore, embedding_model: EmbeddingModel):
    @chain
    def schema_retriever(query):
        query_text = _extract_query_text(query)

        embedding = embedding_model.encode_single(query_text)
        results = chroma_store.search_schema(embedding, top_k=3)
        return results

    @chain
    def few_shot_retriever(query):
        query_text = _extract_query_text(query)

        embedding = embedding_model.encode_single(query_text)
        results = chroma_store.search_few_shot(embedding, top_k=2)
        return results

    return RunnableParallel(
        query=RunnableLambda(lambda x: x.get("query", x)),
        schemas=schema_retriever,
        few_shot=few_shot_retriever
    )
