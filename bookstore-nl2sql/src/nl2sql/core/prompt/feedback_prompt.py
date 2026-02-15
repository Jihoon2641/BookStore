from langchain_core.prompts import ChatPromptTemplate

from nl2sql.models.feedback import FeedbackIssueType


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


def create_feedback_classification_prompt() -> ChatPromptTemplate:
    return ChatPromptTemplate.from_messages(
        [
            (
                "system",
                "당신은 NL2SQL 피드백 분류기다. 사용자 피드백을 주어진 스키마 형식으로 분류해라.",
            ),
            (
                "user",
                "[질문]\n{query}\n\n"
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
        ]
    )


def create_feedback_refinement_prompt() -> ChatPromptTemplate:
    return ChatPromptTemplate.from_messages(
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
