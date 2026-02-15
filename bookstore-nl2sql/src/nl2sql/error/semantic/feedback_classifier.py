import os

from langchain_core.output_parsers import PydanticOutputParser
from langchain_openai import ChatOpenAI

from nl2sql.core.prompt.feedback_prompt import create_feedback_classification_prompt
from nl2sql.models.feedback import (
    FeedbackClassificationResult,
    FeedbackRequest,
)


class FeedbackClassifier:
    """
    불만족 피드백을 LLM으로 4개 오류 타입 중 하나로 분류한다.
    """

    def __init__(self, openai_key: str | None = None, model: str = "gpt-4o-mini"):
        api_key = openai_key or os.getenv("OPENAI_API_KEY")
        if not api_key:
            raise ValueError("OPENAI_API_KEY가 필요합니다.")

        self.llm = ChatOpenAI(
            model=model,
            api_key=api_key,
            temperature=0,
        )
        self.output_parser = PydanticOutputParser(
            pydantic_object=FeedbackClassificationResult
        )
        self.prompt = create_feedback_classification_prompt()
        self.chain = self.prompt | self.llm | self.output_parser

    def classify(self, request: FeedbackRequest) -> FeedbackClassificationResult:
        feedback_text = (request.feedback_text or "").strip()
        if not feedback_text:
            raise ValueError("feedback_text가 비어 있어 분류할 수 없습니다.")
        return self.chain.invoke(
            {
                "query": request.query,
                "sql": request.sql,
                "feedback_text": feedback_text,
                "format_instructions": self.output_parser.get_format_instructions(),
            }
        )
