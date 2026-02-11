from langchain_core.runnables import RunnableLambda, RunnableParallel
from nl2sql.embedding.embedding import EmbeddingModel
from nl2sql.vectordb.chroma_store import ChromaStore
from langchain_core.retrievers import BaseRetriever

class SchemaRetrievalChain(BaseRetriever):
    chroma_store: ChromaStore
    embedding_model: EmbeddingModel

    def __init__(self, chroma_store: ChromaStore, embedding_model: EmbeddingModel):
        super().__init__(
            chroma_store=chroma_store,
            embedding_model=embedding_model
        )

    def _get_relevant_documents(self, query):
        if isinstance(query, dict):
            query_text = query.get("query", "")
        else:
            query_text = query
        
        embedding = self.embedding_model.encode_single(query_text)
        results = self.chroma_store.search_schema(embedding, top_k=3)
        return results

class FewShotRetrievalChain(BaseRetriever):
    chroma_store: ChromaStore
    embedding_model: EmbeddingModel

    def __init__(self, chroma_store: ChromaStore, embedding_model: EmbeddingModel):
        super().__init__(
            chroma_store=chroma_store,
            embedding_model=embedding_model
        )

    def _get_relevant_documents(self, query):
        if isinstance(query, dict):
            query_text = query.get("query", "")
        else:
            query_text = query
        
        embedding = self.embedding_model.encode_single(query_text)
        results = self.chroma_store.search_few_shot(embedding, top_k=2)
        return results

def create_retrieval_chain(chroma_store: ChromaStore, embedding_model: EmbeddingModel):
    schema_retriever = SchemaRetrievalChain(chroma_store, embedding_model)
    few_shot_retriever = FewShotRetrievalChain(chroma_store, embedding_model)
    
    return RunnableParallel(
        question=RunnableLambda(lambda x: x.get("query", x)),
        schemas=schema_retriever,
        few_shot=few_shot_retriever
    )