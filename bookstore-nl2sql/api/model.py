from pydantic import BaseModel, Field

from nl2sql.models.feedback import FeedbackRepairResult


class ChatRequest(BaseModel):
    session_id: str | None = Field(None, description="대화 세션 ID")
    query: str | None = Field(None, description="최초 자연어 질문")
    satisfied: bool | None = Field(None, description="사용자 만족 여부")
    feedback_text: str | None = Field(None, description="불만족 이유")
    max_rows: int = Field(50, ge=1, le=1000, description="재실행 시 최대 반환 행 수")


class ChatResponse(BaseModel):
    session_id: str
    status: str = Field(
        ...,
        description="AWAITING_CONFIRMATION | AWAITING_FEEDBACK_REASON | DONE",
    )
    message: str = Field(..., description="UI에 노출할 안내 문구")
    query: str | None = None
    sql: str | None = None
    explanation: str | None = None
    validation_passed: bool | None = None
    repair: FeedbackRepairResult | None = Field(
        None,
        description="불만족 + feedback_text 조건에서 수행된 SQL 교정 결과",
    )
    rows: list[dict] | None = Field(None, description="교정 성공 후 실행 결과")
    row_count: int | None = Field(None, description="실행 결과 행 수")
    execution_error: str | None = Field(None, description="SQL 실행 실패 시 오류")
