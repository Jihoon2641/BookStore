from pydantic import BaseModel, ConfigDict, Field


class FewShot(BaseModel):
    example_id: str = Field(..., description="예제 고유 ID")
    query: str = Field(..., description="자연어 질문")
    sql: str = Field(..., description="정답 SQL 쿼리")
    explanation: str = Field(..., description="SQL 설명")

    tables_used: list[str] = Field(default_factory=list, description="사용된 테이블 목록")
    tags: list[str] = Field(default_factory=list, description="태그 (예: [필터링, 정렬, 집계])")
    notes: str | None = Field(None, description="추가 설명")

    model_config = ConfigDict(
        json_schema_extra={
            "example": {
                "example_id": "example_001",
                "query": "재고가 10개 미만인 책은?",
                "sql": "SELECT title, stock FROM books WHERE stock < 10",
                "explanation": "WHERE 절로 재고 조건 필터링",
                "tables_used": ["books"],
                "tags": ["필터링", "단순조회"],
                "notes": "재고가 10개 미만인 책을 조회하는 예제",
            }
        }
    )
