from datetime import datetime

from pydantic import BaseModel, Field


class ColumnInfo(BaseModel):
    """
    컬럼 정보
    """

    name: str = Field(..., description="컬럼 이름, 예: stock")
    type: str = Field(..., description="데이터 타입, 예: BIGINT")
    nullable: bool = Field(..., description="NULL 여부")
    key: str | None = Field(
        None,
        description="키 종류, 예: PRIMARY KEY, FOREIGN KEY, UNIQUE KEY, INDEX, UNIQUE, NOT NULL",
    )
    default: str | None = Field(None, description="기본값, 예: 0")
    extra: str | None = Field(None, description="추가 정보, 예: AUTO_INCREMENT")
    description_ko: str = Field(..., description="컬럼 설명, 예: 재고 수량")


class TableSchema(BaseModel):
    """
    테이블 스키마
    """

    table_name: str = Field(..., description="테이블 이름")
    columns: list[ColumnInfo] = Field(..., description="컬럼 목록")
    description_ko: str = Field(..., description="테이블 설명, 예: 도서 정보")

    foreign_keys: list[dict] = Field(..., description="외래키 정보")


class SchemaMetadata(BaseModel):
    """
    스키마 메타데이터
    """

    version: str = Field(..., description="스키마 버전")
    last_updated: datetime = Field(..., description="마지막 업데이트 일시")
    tables: list[TableSchema] = Field(..., description="테이블 목록")
