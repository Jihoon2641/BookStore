"""
스키마 정보를 ChromaDB에 인덱싱

- EmbeddingModel 사용
- ChromaDB에 저장
"""

import json
from pathlib import Path

from nl2sql.embedding.embedding import get_embedding_model
from nl2sql.vectordb.chroma_store import ChromaStore


def format_columns_for_search(columns: list) -> str:
    """
    컬럼 정보를 검색에 적합한 형태로 포맷

    Args:
        columns: 컬럼 리스트

    Returns:
        포맷된 컬럼 정보
    """
    lines = []

    for col in columns:
        lines.append(f"{col['name']}: {col['description_ko']} ({col['type']})")
    return "\n".join(lines)


def main():
    print("스키마 인덱싱 시작")

    schema_path = Path("data/metadata/schema.json")

    if not schema_path.exists():
        print("스키마 파일이 존재하지 않습니다.")
        return

    with open(schema_path, encoding="utf-8") as f:
        schema_metadata = json.load(f)

    embedding_model = get_embedding_model()

    chroma = ChromaStore()

    if not chroma.health_check():
        print("ChromaDB 연결 실패")
        return

    chroma.init_schema_collection(reset=True)

    tables = schema_metadata["tables"]

    for table in tables:
        table_name = table["table_name"]
        description = table["description_ko"]
        columns = table["columns"]
        foreign_keys = table.get("foreign_keys", [])

        # 검색용 텍스트 생성
        columns_text = format_columns_for_search(columns)
        search_text = f"{description}\n{columns_text}"

        # 임베딩 생성
        embedding = embedding_model.encode_single(search_text)

        # 메타데이터 구성

        metadata = {
            "table_name": table_name,
            "description": description,
            "columns": json.dumps(columns, ensure_ascii=False),
            "foreign_keys": json.dumps(foreign_keys, ensure_ascii=False),
            "column_count": len(columns),
            "foreign_key_count": len(foreign_keys),
        }

        chroma.add_schema(
            table_name=table_name, embedding=embedding, document=search_text, metadata=metadata
        )

        # 테스트
        query_embedding = embedding_model.encode_single(
            "사용자의 주문 정보를 알고 싶은데 어떤 테이블들을 참고해야하지?"
        )
        results = chroma.search_schema(query_embedding, top_k=2)

        for i, result in enumerate(results, 1):
            print(f"\n{i}. {result['table_name']}")
            print(f"   거리: {result['distance']:.4f}")
            print(f"   설명: {result['metadata']['description']}")


if __name__ == "__main__":
    main()
