import os

from langchain_core.output_parsers import PydanticOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

from nl2sql.models.feedback import (
    FeedbackClassificationResult,
    FeedbackIssueType,
    FeedbackRequest,
    SQLRefinementResult,
)


ISSUE_GUIDANCE = {
    FeedbackIssueType.OUTPUT_SHAPE_ERROR: (
        "결과 컬럼, 별칭, 정렬, LIMIT/TopN, 형식을 우선 교정하세요. "
        "질문에서 요구된 출력 요소가 모두 SELECT에 반영되도록 수정하세요."
    ),
    FeedbackIssueType.INTENT_MISMATCH: (
        "사용자 질문의 핵심 의도를 먼저 재정의하고, 그 의도와 직접 연결되는 SQL 로직으로 교정하세요. "
        "불필요한 계산/조건이 있다면 제거하세요."
    ),
    FeedbackIssueType.SCOPE_FILTER_ERROR: (
        "WHERE/HAVING/기간 조건을 우선 교정하세요. "
        "누락/과도 조건을 조정해 범위를 정확히 맞추세요."
    ),
    FeedbackIssueType.RELATION_AGG_ERROR: (
        "JOIN 키, 조인 타입, GROUP BY, 집계 함수(COUNT/SUM/AVG 등)를 우선 교정하세요. "
        "중복 집계 가능성이 있으면 DISTINCT 또는 선집계를 고려하세요."
    ),
}


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
        self.prompt = ChatPromptTemplate.from_messages(
            [
                (
                    "system",
                    "당신은 MySQL SQL 교정 전문가다. 주어진 오류 타입 지시를 따르고 SELECT 쿼리만 생성한다.",
                ),
                (
                    "user",
                    "[오류 타입]\n{issue_type}\n\n"
                    "[오류 근거]\n{issue_reason}\n\n"
                    "[오류 타입별 교정 지시]\n{issue_guidance}\n\n"
                    "[사용자 질문]\n{query}\n\n"
                    "[현재 SQL]\n{current_sql}\n\n"
                    "[사용자 불만족 피드백]\n{feedback_text}\n\n"
                    "[검증 오류]\n{validator_error}\n\n"
                    "[검증 제안]\n{validator_suggestions}\n\n"
                    "규칙:\n"
                    "1) SELECT 쿼리만 생성\n"
                    "2) MySQL 문법 사용\n"
                    "3) 세미콜론(;)으로 종료\n"
                    "4) 기존 SQL 구조는 필요 범위에서만 수정\n\n"
                    "{format_instructions}",
                ),
            ]
        )
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
