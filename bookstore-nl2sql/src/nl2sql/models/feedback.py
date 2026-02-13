from pydantic import Field
from pydantic import BaseModel

class FeedbackRequest(BaseModel):
    question: str = Field(..., description="사용자 질문")
    sql: str = Field(..., description="생성된 SQL")
    explanation: str = Field(..., description="SQL 설명")
    satisfied: bool = Field(..., description="만족 여부")

class FeedbackResponse(BaseModel):
    success: bool = Field(..., description="성공 여부")
    message: str = Field(..., description="메시지")
    example_id: str | None = Field(None, description="생성된 예제 ID 예: example_001")
    