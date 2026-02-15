from sqlalchemy import text
from langchain_core.runnables import RunnableLambda

from nl2sql.common.sql_parser import extract_tables_from_sql
from nl2sql.core.database.db_connector import DBConnector
from nl2sql.error.syntax.sql_validator import SQLValidator
from nl2sql.feedback.few_shot_store import FewShotJsonStore
from nl2sql.models.feedback import (
    FeedbackRepairResult,
    FeedbackRequest,
)
from .feedback_classifier import FeedbackClassifier
from .feedback_refiner import FeedbackSQLRefiner


class FeedbackRepairOrchestrator:
    """
    불만족 피드백 기반 SQL 교정 오케스트레이션
    1) 오류 타입 분류
    2) 타입 기반 SQL 교정
    3) SQL Validator 검증
    4) 실패 시 validator 피드백을 반영해 재시도
    """

    def __init__(
        self,
        few_shot_path: str | None = None,
        openai_key: str | None = None,
        model: str = "gpt-4o-mini",
        max_retries: int = 2,
    ):
        self.max_retries = max_retries
        self.classifier = FeedbackClassifier(openai_key=openai_key, model=model)
        self.refiner = FeedbackSQLRefiner(openai_key=openai_key, model=model)
        self.db_connector = DBConnector()
        self.validator = SQLValidator(self._get_table_names())
        self.few_shot_store = FewShotJsonStore(few_shot_path) if few_shot_path else None
        self.pipeline = (
            RunnableLambda(self._classify_step)
            | RunnableLambda(self._repair_with_retry_step)
            | RunnableLambda(self._finalize_step)
        )

    def _get_table_names(self) -> list[str]:
        with self.db_connector.get_db() as session:
            result = session.execute(text("SHOW TABLES"))
            return [row[0] for row in result]

    def repair(self, request: FeedbackRequest) -> FeedbackRepairResult:
        return self.pipeline.invoke({"request": request})

    def _classify_step(self, payload: dict) -> dict:
        request: FeedbackRequest = payload["request"]
        if request.satisfied:
            raise ValueError("satisfied=True 요청은 repair 대상이 아닙니다.")
        if not (request.feedback_text and request.feedback_text.strip()):
            raise ValueError("feedback_text가 필요합니다.")

        classification = self.classifier.classify(request)
        return {
            "request": request,
            "classification": classification,
        }

    def _repair_with_retry_step(self, payload: dict) -> dict:
        request: FeedbackRequest = payload["request"]
        classification = payload["classification"]
        current_sql = request.sql
        last_error: str | None = None
        last_suggestions: list[str] = []
        last_refined = None
        successful_attempt = None

        for attempt in range(1, self.max_retries + 2):
            refined = self.refiner.refine(
                request=request,
                classification=classification,
                current_sql=current_sql,
                validator_error=last_error,
                validator_suggestions=last_suggestions,
            )
            last_refined = refined

            with self.db_connector.get_db() as session:
                validation = self.validator.validate(refined.corrected_sql, session=session)

            if validation.is_valid:
                successful_attempt = attempt
                break

            current_sql = refined.corrected_sql
            last_error = validation.error_message
            last_suggestions = validation.suggestion or []

        return {
            "request": request,
            "classification": classification,
            "last_refined": last_refined,
            "last_error": last_error,
            "last_suggestions": last_suggestions,
            "successful_attempt": successful_attempt,
        }

    def _finalize_step(self, payload: dict) -> FeedbackRepairResult:
        request: FeedbackRequest = payload["request"]
        classification = payload["classification"]
        last_refined = payload["last_refined"]
        last_error = payload["last_error"]
        last_suggestions = payload["last_suggestions"]
        successful_attempt = payload["successful_attempt"]

        if successful_attempt is not None:
            saved_to_few_shot, example_id = self._save_to_few_shot_if_enabled(
                request=request,
                corrected_sql=last_refined.corrected_sql,
            )
            return FeedbackRepairResult(
                success=True,
                issue_type=classification.issue_type,
                issue_reason=classification.issue_reason,
                corrected_sql=last_refined.corrected_sql,
                change_summary=last_refined.change_summary,
                intent_alignment_check=last_refined.intent_alignment_check,
                attempts=successful_attempt,
                validator_error=None,
                validator_suggestions=[],
                saved_to_few_shot=saved_to_few_shot,
                example_id=example_id,
            )

        return FeedbackRepairResult(
            success=False,
            issue_type=classification.issue_type,
            issue_reason=classification.issue_reason,
            corrected_sql=last_refined.corrected_sql if last_refined else None,
            change_summary=last_refined.change_summary if last_refined else None,
            intent_alignment_check=last_refined.intent_alignment_check if last_refined else None,
            attempts=self.max_retries + 1,
            validator_error=last_error,
            validator_suggestions=last_suggestions,
            saved_to_few_shot=False,
            example_id=None,
        )

    def _save_to_few_shot_if_enabled(
        self,
        request: FeedbackRequest,
        corrected_sql: str,
    ) -> tuple[bool, str | None]:
        if self.few_shot_store is None:
            return False, None

        example_id = self.few_shot_store.generate_next_id()
        self.few_shot_store.append_example(
            example_id=example_id,
            query=request.query,
            sql=corrected_sql,
            explanation=request.explanation,
            tables_used=extract_tables_from_sql(corrected_sql),
        )
        return True, example_id
