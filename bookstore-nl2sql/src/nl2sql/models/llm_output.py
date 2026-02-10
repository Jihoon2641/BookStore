from pydantic import BaseModel, Field

class SQLOutput(BaseModel):
    sql: str = Field(description="생성된 SQL 쿼리")
