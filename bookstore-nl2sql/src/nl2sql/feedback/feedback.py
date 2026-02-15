from loguru import logger

from nl2sql.common.sql_parser import extract_tables_from_sql
from nl2sql.models.feedback import FeedbackRequest, FeedbackResponse

from .duplicate_checker import DuplicateChecker
from .few_shot_store import FewShotJsonStore


class Feedback:
    """
    사용자 피드백 처리
    - 만족: 중복 검사 후 few-shot 적재
    - 불만족: 사유 수집 안내
    """

    def __init__(self, few_shot_path: str):
        self.store = FewShotJsonStore(few_shot_path)
        self.duplicate_checker = None
        self._init_duplicate_checker()

    def _init_duplicate_checker(self) -> None:
        try:
            self.duplicate_checker = DuplicateChecker()
        except Exception as e:
            logger.warning(f"중복 검사기 초기화 실패: {e}")
            self.duplicate_checker = None

    def handle(self, request: FeedbackRequest) -> FeedbackResponse:
        if request.satisfied:
            return self._handle_positive(request)
        return self._handle_negative(request)

    def _handle_positive(self, request: FeedbackRequest) -> FeedbackResponse:
        try:
            is_duplicate, reason = self._is_duplicate(request.query)
            if is_duplicate:
                logger.info(f"이미 존재하는 few_shot 질문입니다 : {request.query}, reason={reason}")
                return FeedbackResponse(
                    success=True,
                    message=f"이미 학습된 유사 질문이 존재합니다. ({reason})",
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

    def _is_duplicate(self, query: str) -> tuple[bool, str]:
        if self.duplicate_checker is None:
            return False, "no_similarity_backend"

        try:
            return self.duplicate_checker.is_duplicate(query)
        except Exception as e:
            logger.warning(f"유사도 중복 검사 실패: {e}")
            return False, "similarity_check_failed"
