"""
임베딩 생성 

- sentenceTransformer 모델 로드 로드 및 관리
- 텍스트 -> 벡터 변환
- 배치 처리 
"""

from typing import List
from typing import Union
from pathlib import Path
from sentence_transformers import SentenceTransformer
import numpy as np

class EmbeddingModel:

    _instance = None
    _model = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self):
        if self._model is None:
            self._load_model()

    def _load_model(self):
        model_path = Path("models/multilingual-e5-large")

        if not model_path.exists():
            raise FileNotFoundError(f"모델을 찾을 수 없습니다: {model_path}")
        else:
            self._model = SentenceTransformer(str(model_path))

        self._model.save(str(model_path))

    def encode(self, 
    texts: Union[str, List[str]], 
    batch_size: int = 32, 
    show_progress_bar: bool = False, 
    normalize_embeddings: bool = False) -> np.ndarray:
        """
        텍스트를 임베딩 벡터로 변환
        
        Args:
            texts: 단일 텍스트 또는 텍스트 리스트
            batch_size: 배치 크기
            show_progress_bar: 진행 바 표시 여부
            normalize_embeddings: L2 정규화 여부
            
        Returns:
            임베딩 벡터 (shape: [n, dim])
        """

        if isinstance(texts, str):
            texts = [texts]

        embeddings = self._model.encode(
            texts,
            batch_size=batch_size,
            show_progress_bar=show_progress_bar,
            normalize_embeddings=normalize_embeddings,
            convert_to_numpy=True
        )

        return embeddings

    def encode_single(self, text: str) -> List[float]:
        """
        단일 텍스트를 임베딩 벡터로 변환
        
        Args:
            text: 단일 텍스트
            
        Returns:
            임베딩 벡터
        """
        embedding = self.encode(text, normalize_embeddings=True)
        return embedding[0].tolist()

    def encode_batch(self, texts: List[str], batch_size: int = 32) -> List[List[float]]:
        """
        배치 텍스트 임베딩
        
        Args:
            texts: 텍스트 리스트
            batch_size: 배치 크기
            
        Returns:
            임베딩 벡터 리스트
        """
        embeddings = self.encode(texts, batch_size=batch_size, show_progress_bar=len(texts) > 10)
        return embeddings.tolist()

    
    @property
    def dimension(self) -> int:
        """
        임베딩 벡터 차원
        """
        return self._model.get_sentence_embedding_dimension()

_embedding_model: EmbeddingModel = None

def get_embedding_model() -> EmbeddingModel:
    """
    전역 임베딩 모델 인스턴스 반환
    """
    global _embedding_model
    if _embedding_model is None:
        _embedding_model = EmbeddingModel()
    return _embedding_model
