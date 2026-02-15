import os

from langchain_core.output_parsers import PydanticOutputParser
from langchain_openai import ChatOpenAI

from nl2sql.core.prompt.feedback_prompt import (
    ISSUE_GUIDANCE,
    create_feedback_refinement_prompt,
)
from nl2sql.models.feedback import (
    FeedbackClassificationResult,
    FeedbackRequest,
    SQLRefinementResult,
)


class FeedbackSQLRefiner:
    """
    분류 결과를 기반으로 SQL을 재생성한다.
    출력 파서는 SQLRefinementResult 하나로 고정한다.
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
        self.output_parser = PydanticOutputParser(pydantic_object=SQLRefinementResult)
        self.prompt = create_feedback_refinement_prompt()
        self.chain = self.prompt | self.llm | self.output_parser

    def refine(
        self,
        request: FeedbackRequest,
        classification: FeedbackClassificationResult,
        current_sql: str,
        validator_error: str | None = None,
        validator_suggestions: list[str] | None = None,
    ) -> SQLRefinementResult:
        feedback_text = (request.feedback_text or "").strip()
        if not feedback_text:
            raise ValueError("feedback_text가 비어 있어 SQL을 교정할 수 없습니다.")

        issue_guidance = ISSUE_GUIDANCE[classification.issue_type]
        suggestions_text = ", ".join(validator_suggestions or [])

        return self.chain.invoke(
            {
                "issue_type": classification.issue_type.value,
                "issue_reason": classification.issue_reason,
                "issue_guidance": issue_guidance,
                "query": request.query,
                "current_sql": current_sql,
                "feedback_text": feedback_text,
                "validator_error": validator_error or "",
                "validator_suggestions": suggestions_text,
                "format_instructions": self.output_parser.get_format_instructions(),
            }
        )
