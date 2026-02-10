import asyncio
from concurrent.futures import ThreadPoolExecutor
from typing import List, Union
import numpy as np
from .embedding import get_embedding_model


class AsyncEmbeddingModel:
    """
    비동기 임베딩 모델 래퍼

    - ThreadPoolExecutor를 사용하여 동기 임베딩 모델을 비동기적으로 호출
    - 싱글 워커 환경에서 안전
    """

    def __init__(self, max_workers: int = 1):
        """
        Args:
            max_workers: 스레드 풀 워커 수 (기본값: 1)
        """
        self.executor = ThreadPoolExecutor(max_workers=max_workers)
        self.model = get_embedding_model()

    async def encode_async(
        self, texts: Union[str, List[str]], batch_size: int = 32
    ) -> List[List[float]]:
        """
        비동기 임베딩 생성

        Args:
            texts: 단일 텍스트 또는 텍스트 리스트
            batch_size: 배치 크기

        Returns:
            임베딩 벡터 리스트
        """

        if isinstance(texts, str):
            texts = [texts]

        # ThreadPoolExecutor에서 동기 함수 실행
        loop = asyncio.get_running_loop()
        embeddings = await loop.run_in_executor(
            self.executor, self.model.encode, texts, batch_size, False, True
        )

        return embeddings[0].tolist()

    async def encode_single_async(self, text: str) -> List[float]:
        """
        비동기 단일 텍스트 임베딩

        Args:
            text: 단일 텍스트

        Returns:
            임베딩 벡터 리스트
        """
        embedding = await self.encode_async(text)
        return embedding[0]

    def cleanup(self):
        """
        자원 해제
        """
        self.executor.shutdown(wait=True)


_async_embedding_model: AsyncEmbeddingModel = None


def get_async_embedding_model() -> AsyncEmbeddingModel:
    """
    전역 비동기 임베딩 모델 인스턴스 반환
    """
    global _async_embedding_model
    if _async_embedding_model is None:
        _async_embedding_model = AsyncEmbeddingModel()
    return _async_embedding_model
