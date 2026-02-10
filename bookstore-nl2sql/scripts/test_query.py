"""
쿼리 처리 테스트 스크립트
"""

import os

from dotenv import load_dotenv

from nl2sql.core.query_processor import QueryProcessor
from nl2sql.models.query import QueryRequest

load_dotenv()


def main():
    print("=" * 60)
    print("NL2SQL 쿼리 처리 테스트")
    print("=" * 60)
    print()

    # QueryProcessor 초기화
    processor = QueryProcessor(
        open_ai_key=os.getenv("OPENAI_API_KEY"), model=os.getenv("OPENAI_MODEL", "gpt-4o-mini")
    )

    # 테스트 질문들
    test_questions = [
        "재고가 10개 미만인 책은?",
        "가장 비싼 책 3권은?",
        "어제 주문한 사용자는?",
        "출판사별 평균 도서 가격은?",
    ]

    for i, question in enumerate(test_questions, 1):
        print(f"\n{'=' * 60}")
        print(f"테스트 {i}: {question}")
        print(f"{'=' * 60}\n")

        # 쿼리 처리
        request = QueryRequest(query=question)
        response = processor.process(request)

        # 결과 출력
        print("\n생성된 SQL:")
        print(f"   {response.sql}\n")

        print("검색된 스키마:")
        for schema in response.retrieved_schema:
            print(f"   - {schema.table_name} (거리: {schema.distance:.4f})")

        print("\n검색된 예제:")
        for example in response.retrieved_few_shot:
            print(f"   - {example.question} (거리: {example.distance:.4f})")

        print(f"\n실행 시간: {response.execution_time_ms}ms")
        print()


if __name__ == "__main__":
    main()
