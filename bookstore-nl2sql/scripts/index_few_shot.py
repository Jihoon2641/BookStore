"""
Few-shot 데이터를 ChromaDB에 인덱싱하는 스크립트
"""

import json
from pathlib import Path

from nl2sql.embedding.embedding import get_embedding_model
from nl2sql.vectordb.chroma_store import ChromaStore


def main():

    print("Few-shot 데이터 인덱싱을 시작")

    few_shot_example = Path("data/metadata/few_shot.json")

    if not few_shot_example.exists():
        raise FileNotFoundError("few_shot_examples.json 파일이 없습니다.")

    with open(few_shot_example) as f:
        few_shot_examples = json.load(f)

    embedding_model = get_embedding_model()

    chroma = ChromaStore()

    if not chroma.health_check():
        raise Exception("ChromaDB 연결 실패")

    chroma.init_few_shot_collection(reset=True)

    examples = few_shot_examples["examples"]

    for example in examples:
        example_id = example["example_id"]
        question = example["question"]
        sql = example["sql"]
        explanation = example["explanation"]
        tables_used = example.get("tables_used", [])
        tags = example.get("tags", [])
        notes = example.get("notes", "")

        embedding = embedding_model.encode_single(question)

        metadata = {
            "sql": sql,
            "explanation": explanation,
            "tables_used": json.dumps(tables_used, ensure_ascii=False),
            "tags": json.dumps(tags, ensure_ascii=False),
            "notes": notes,
        }

        chroma.add_few_shot(
            example_id=example_id, embedding=embedding, questions=question, metadata=metadata
        )

    print("Few-shot 데이터 인덱싱이 완료되었습니다.")

    test_query = "가장 많이 팔린 책"
    query_embedding = embedding_model.encode_single(test_query)
    results = chroma.search_few_shot(query_embedding, top_k=3)

    for i, result in enumerate(results, 1):
        print(f"\n{i}. [{result['metadata']}] {result['question']}")
        print(f"   거리: {result['distance']:.4f}")
        print(f"   SQL: {result['metadata']['sql'][:80]}...")
        print(f"   설명: {result['metadata']['explanation']}")


if __name__ == "__main__":
    main()
