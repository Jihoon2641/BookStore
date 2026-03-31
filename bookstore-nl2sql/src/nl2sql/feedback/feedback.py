from loguru import logger

from nl2sql.common.sql_parser import extract_tables_from_sql
from nl2sql.models.feedback import FeedbackRequest, FeedbackResponse

from .few_shot_store import FewShotJsonStore


class Feedback:
    """
    사용자 피드백 처리
    - 만족: 중복 검사 후 few-shot 적재
    - 불만족: 사유 수집 안내
    """

    def __init__(self, few_shot_path: str):
        self.store = FewShotJsonStore(few_shot_path)

    def handle(self, request: FeedbackRequest) -> FeedbackResponse:
        if request.satisfied:
            return self._handle_positive(request)
        return self._handle_negative(request)

    def _handle_positive(self, request: FeedbackRequest) -> FeedbackResponse:
        try:
            if self.store.has_query(request.query):
                logger.info(f"이미 존재하는 few_shot 질문입니다 : {request.query}")
                return FeedbackResponse(
                    success=True,
                    message="이미 few-shot에 존재하는 질문입니다.",
                )

            example_id = self.store.generate_next_id()
            tables_used = extract_tables_from_sql(request.sql)
            self.store.append_example(
                example_id=example_id,
                query=request.query,
                sql=request.sql,
                explanation=request.explanation,
                tables_used=tables_used,
            )
            logger.info(f"새로운 예제 추가됨: {example_id}")

            return FeedbackResponse(
                success=True,
                message="학습데이터에 추가되었습니다.",
                example_id=example_id,
            )
        except Exception as e:
            return FeedbackResponse(
                success=False,
                message=f"학습데이터 처리 중 오류 발생: {str(e)}",
            )

    def _handle_negative(self, request: FeedbackRequest) -> FeedbackResponse:
        if request.feedback_text and request.feedback_text.strip():
            return FeedbackResponse(
                success=False,
                message="불만족 피드백이 접수되었습니다. 다음 개선 단계에서 원인 분석에 활용됩니다.",
            )

        return FeedbackResponse(
            success=False,
            message="불만족스러운 이유를 알려주세요.",
        )
