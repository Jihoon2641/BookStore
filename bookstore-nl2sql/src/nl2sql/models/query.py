from datetime import datetime

from pydantic import BaseModel, Field


class QueryRequest(BaseModel):
    query: str = Field(..., description="자연어 질문")
    user_id: str | None = Field(None, description="사용자(관리자) ID")


class SchemaContext(BaseModel):
    """검색된 스키마 정보"""

    table_name: str = Field(..., description="테이블 이름")
    description: str = Field(..., description="테이블 설명")
    columns: str = Field(..., description="컬럼 정보, JSON 형식")
    foreign_keys: str = Field(..., description="외래키 정보, JSON 형식")
    distance: float = Field(..., description="유사도")


class FewShotContext(BaseModel):
    """few-shot 정보"""

    question: str = Field(..., description="자연어 질문")
    sql: str = Field(..., description="정답 SQL 쿼리")
    explanation: str = Field(..., description="SQL 설명")
    distance: float = Field(..., description="유사도")


class QueryResponse(BaseModel):
    """쿼리 처리 결과"""

    question: str = Field(..., description="자연어 질문")
    sql: str = Field(..., description="생성된 SQL 쿼리")
    generator_type: str = Field(..., description="생성기 타입")
    confidence: float | None = Field(None, description="생성 신뢰도")
    explanation: str | None = Field(None, description="SQL 설명")

    # 검색된 컨텍스트
    retrieved_schema: list[SchemaContext] = Field(description="검색된 스키마 컨텍스트")
    retrieved_few_shot: list[FewShotContext] = Field(description="검색된 few-shot 컨텍스트")

    # 메타 정보
    execution_time_ms: int = Field(..., description="실행 시간 (밀리초)")
    timestamp: datetime = Field(default_factory=datetime.now)

    # SQL 검증 결과
    validation_passed: bool = Field(default=True, description="SQL 검증 통과 여부")
    validation_error: str | None = Field(default=None, description="검증 실패 시 오류 메시지")
    parsed_sql: str | None = Field(default=None, description="파싱 및 포맷팅된 SQL (검증 통과 시)")


class QueryLog(BaseModel):
    """쿼리 로그"""

    timestamp: str
    question: str
    sql: str
    generator_type: str
    confidence: float | None = None
    execution_time_ms: int
    user_id: str | None = None
