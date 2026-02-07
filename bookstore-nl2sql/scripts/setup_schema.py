"""
스키마 메타데이터 초기 생성 스크립트

1. DB에서 테이블/컬럼 정보 추출
2. schema.json 파일 생성
3. 사용자가 수동으로 테이블 및 컬럼 설명 입력 해야함
"""

from pathlib import Path
import json
from nl2sql.core.db_connector import DBConnector
from nl2sql.core.schema_indexer import SchemaIndexer
from nl2sql.models.shema import SchemaMetadata
from datetime import datetime

def main():
    print("DB 스키마 추출 시작")

    db_connector = DBConnector()

    indexer = SchemaIndexer(db_connector)
    tables = indexer.extract_schema()

    print(f"총 {len(tables)} 개의 테이블 추출 완료")

    metadata = SchemaMetadata(
        version="1.0.0",
        last_updated=datetime.now(),
        tables=tables
    )

    output_path = Path("data/metadata/schema.json")
    output_path.parent.mkdir(parents=True, exist_ok=True)

    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(
            metadata.model_dump(),
            f,
            indent=2,
            ensure_ascii=False,
            default=str
        )

    print(f"스키마 메타데이터 저장 완료: {output_path}")

    db_connector.close()

if __name__ == "__main__":
    main()