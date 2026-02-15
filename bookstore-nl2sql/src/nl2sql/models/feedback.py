from enum import Enum

from pydantic import Field
from pydantic import BaseModel


class FeedbackIssueType(str, Enum):
    OUTPUT_SHAPE_ERROR = "OUTPUT_SHAPE_ERROR"
    INTENT_MISMATCH = "INTENT_MISMATCH"
    SCOPE_FILTER_ERROR = "SCOPE_FILTER_ERROR"
    RELATION_AGG_ERROR = "RELATION_AGG_ERROR"


class FeedbackRequest(BaseModel):
    question: str = Field(..., description="사용자 질문")
    sql: str = Field(..., description="생성된 SQL")
    explanation: str = Field(..., description="SQL 설명")
    satisfied: bool = Field(..., description="만족 여부")
    feedback_text: str | None = Field(None, description="불만족 이유(자유 텍스트)")

class FeedbackResponse(BaseModel):
    success: bool = Field(..., description="성공 여부")
    message: str = Field(..., description="메시지")
    example_id: str | None = Field(None, description="생성된 예제 ID 예: example_001")


class FeedbackClassificationResult(BaseModel):
    issue_type: FeedbackIssueType = Field(..., description="분류된 피드백 오류 타입")
    issue_reason: str = Field(..., description="분류 근거")
    confidence: float = Field(..., ge=0.0, le=1.0, description="분류 신뢰도(0~1)")
