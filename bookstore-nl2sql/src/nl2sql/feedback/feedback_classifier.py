import os

from langchain_core.output_parsers import PydanticOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

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
        self.prompt = ChatPromptTemplate.from_messages([
            (
                "system",
                "당신은 NL2SQL 피드백 분류기다. 사용자 피드백을 주어진 스키마 형식으로 분류해라.",
            ),
            (
                "user",
                "[질문]\n{question}\n\n"
                "[현재 SQL]\n{sql}\n\n"
                "[사용자 불만족 피드백]\n{feedback_text}\n\n"
                "분류 후보:\n"
                "- OUTPUT_SHAPE_ERROR\n"
                "- INTENT_MISMATCH\n"
                "- SCOPE_FILTER_ERROR\n"
                "- RELATION_AGG_ERROR\n\n"
                "규칙:\n"
                "1) issue_type은 후보 중 정확히 하나\n"
                "2) issue_reason은 한국어 1~2문장\n"
                "3) confidence는 0~1 실수\n\n"
                "{format_instructions}",
            ),
        ])
        self.chain = self.prompt | self.llm | self.output_parser

    def classify(self, request: FeedbackRequest) -> FeedbackClassificationResult:
        feedback_text = (request.feedback_text or "").strip()
        if not feedback_text:
            raise ValueError("feedback_text가 비어 있어 분류할 수 없습니다.")
        return self.chain.invoke(
            {
                "question": request.question,
                "sql": request.sql,
                "feedback_text": feedback_text,
                "format_instructions": self.output_parser.get_format_instructions(),
            }
        )
