from typing import Any, Literal, Mapping

from pydantic import BaseModel, Field, model_validator

from nl2sql.models.feedback import FeedbackRepairResult, FeedbackRequest


class SessionData(BaseModel):
    query: str
    sql: str
    explanation: str
    dissatisfaction_count: int = 0

    @classmethod
    def from_mapping(cls, payload: Mapping[str, Any]) -> "SessionData":
        return cls(
            query=str(payload.get("query", "")),
            sql=str(payload.get("sql", "")),
            explanation=str(payload.get("explanation", "")),
            dissatisfaction_count=int(payload.get("dissatisfaction_count", 0)),
        )


class ChatResponse(BaseModel):
    session_id: str
    status: str = Field(
        ...,
        description=(
            "AWAITING_CONFIRMATION | AWAITING_FEEDBACK_REASON | "
            "DONE | MAX_ATTEMPTS_EXCEEDED"
        ),
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
    attempt_count: int | None = Field(None, description="사용자 불만족 기준 교정 시도 횟수")
    max_attempts: int | None = Field(None, description="최대 교정 시도 횟수")


class ChatWsRequest(BaseModel):
    action: Literal["ASK", "CONFIRM", "FEEDBACK"] = Field(..., description="웹소켓 액션")
    session_id: str | None = Field(None, description="대화 세션 ID")
    query: str | None = Field(None, description="자연어 질문(ASK 전용)")
    satisfied: bool | None = Field(None, description="만족 여부(CONFIRM 전용)")
    feedback_text: str | None = Field(None, description="불만족 이유(FEEDBACK 전용)")

    @model_validator(mode="after")
    def validate_required_by_action(self) -> "ChatWsRequest":
        if self.action == "ASK":
            if not (self.query and self.query.strip()):
                raise ValueError("query는 필수입니다.")

        if self.action == "CONFIRM":
            if not self.session_id:
                raise ValueError("session_id가 필요합니다.")
            if self.satisfied is None:
                raise ValueError("satisfied 값이 필요합니다.")

        if self.action == "FEEDBACK":
            if not self.session_id:
                raise ValueError("session_id가 필요합니다.")
            if not (self.feedback_text and self.feedback_text.strip()):
                raise ValueError("feedback_text는 비어 있을 수 없습니다.")

        return self

    @property
    def query_text(self) -> str:
        return (self.query or "").strip()

    @property
    def feedback_reason(self) -> str:
        return (self.feedback_text or "").strip()


class ChatWsResponse(ChatResponse):
    type: Literal["chat.response"] = "chat.response"


class ChatWsError(BaseModel):
    type: Literal["chat.error"] = "chat.error"
    code: str = "BAD_REQUEST"
    message: str


def build_feedback_request_from_session(
    session_data: Mapping[str, Any],
    satisfied: bool,
    feedback_text: str | None,
) -> FeedbackRequest:
    snapshot = SessionData.from_mapping(session_data)
    return FeedbackRequest(
        query=snapshot.query,
        sql=snapshot.sql,
        explanation=snapshot.explanation,
        satisfied=satisfied,
        feedback_text=feedback_text,
    )
