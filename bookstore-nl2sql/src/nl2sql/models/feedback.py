from enum import Enum

from pydantic import Field
from pydantic import BaseModel


class FeedbackIssueType(str, Enum):
    """
    피드백 오류 타입
    OUTPUT_SHAPE_ERROR: 결과 컬럼이나 형식이 잘못됨
    INTENT_MISMATCH: 질문 의도가 잘못 반영됨
    SCOPE_FILTER_ERROR: 범위 조건(WHERE, HAVING)이 잘못됨
    RELATION_AGG_ERROR: 관계(JOIN)나 집계(SUM, AVG 등)가 잘못됨
    """
    OUTPUT_SHAPE_ERROR = "OUTPUT_SHAPE_ERROR"
    INTENT_MISMATCH = "INTENT_MISMATCH"
    SCOPE_FILTER_ERROR = "SCOPE_FILTER_ERROR"
    RELATION_AGG_ERROR = "RELATION_AGG_ERROR"


class FeedbackRequest(BaseModel):
    query: str = Field(..., description="사용자 질문")
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


class SQLRefinementResult(BaseModel):
    corrected_sql: str = Field(..., description="교정된 SQL")
    change_summary: str = Field(..., description="핵심 수정 요약")
    intent_alignment_check: str = Field(..., description="질문 의도 반영 검토")


class FeedbackRepairResult(BaseModel):
    success: bool = Field(..., description="교정 성공 여부")
    issue_type: FeedbackIssueType = Field(..., description="분류된 오류 타입")
    issue_reason: str = Field(..., description="분류 근거")
    corrected_sql: str | None = Field(None, description="최종 교정 SQL")
    change_summary: str | None = Field(None, description="최종 수정 요약")
    intent_alignment_check: str | None = Field(None, description="의도 반영 검토")
    attempts: int = Field(..., description="교정 시도 횟수")
    validator_error: str | None = Field(None, description="검증 실패 메시지")
    validator_suggestions: list[str] = Field(default_factory=list, description="검증 수정 제안")
    saved_to_few_shot: bool = Field(False, description="few_shot.json 저장 여부")
    example_id: str | None = Field(None, description="few_shot 저장 example_id")
