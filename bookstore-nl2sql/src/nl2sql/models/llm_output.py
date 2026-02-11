from pydantic import BaseModel, Field


class SQLOutput(BaseModel):
    sql: str = Field(description="생성된 SQL 쿼리")
    explanation: str = Field(description="SQL 설명")
