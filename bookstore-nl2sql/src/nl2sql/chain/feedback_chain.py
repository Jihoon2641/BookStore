from langchain_core.runnables import RunnableLambda

from nl2sql.error.semantic.feedback_repair_orchestrator import FeedbackRepairOrchestrator
from nl2sql.feedback.feedback import Feedback
from nl2sql.models.feedback import FeedbackRequest


def create_feedback_chain(
    few_shot_path: str,
    openai_key: str | None,
    model: str,
):
    feedback = Feedback(few_shot_path)
    feedback_repair_orchestrator = FeedbackRepairOrchestrator(
        few_shot_path=few_shot_path,
        openai_key=openai_key,
        model=model,
    )

    def _to_feedback_request(raw) -> FeedbackRequest | None:
        if raw is None:
            return None
        if isinstance(raw, FeedbackRequest):
            return raw
        if isinstance(raw, dict):
            return FeedbackRequest(**raw)
        raise ValueError("feedback_request 형식이 올바르지 않습니다.")

    def _model_to_dict(model_obj):
        if hasattr(model_obj, "model_dump"):
            return model_obj.model_dump()
        if hasattr(model_obj, "dict"):
            return model_obj.dict()
        return model_obj

    def _feedback_step(payload: dict) -> dict:
        feedback_request = _to_feedback_request(payload.get("feedback_request"))

        if feedback_request is None:
            return {
                **payload,
                "feedback_processed": False,
            }

        if feedback_request.satisfied or not (feedback_request.feedback_text or "").strip():
            return {
                **payload,
                "feedback_processed": False,
            }

        repair_result = feedback_repair_orchestrator.repair(feedback_request)
        return {
            **payload,
            "feedback_processed": True,
            "feedback_repair_result": _model_to_dict(repair_result),
        }

    chain = RunnableLambda(_feedback_step)
    return feedback, feedback_repair_orchestrator, chain
