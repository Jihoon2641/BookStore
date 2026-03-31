import json
import logging
import os
from functools import lru_cache
from logging.handlers import RotatingFileHandler
from pathlib import Path
from threading import Lock
from typing import Any
from uuid import uuid4

from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from pydantic import ValidationError

from api.model import (
    ChatResponse,
    ChatWsError,
    ChatWsRequest,
    ChatWsResponse,
    SessionData,
    build_feedback_request_from_session,
)
from nl2sql.core.nl2sql_processor import NL2SQLProcessor
from nl2sql.models.query import QueryRequest

MAX_DISSATISFACTION_ATTEMPTS = 3
UNSATISFIED_LOG_PATH = Path("logs/nl2sql_unsatisfied.log")


@lru_cache(maxsize=1)
def get_processor() -> NL2SQLProcessor:
    openai_key = os.getenv("OPENAI_API_KEY")
    model = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
    if not openai_key:
        raise RuntimeError("OPENAI_API_KEY 환경변수가 필요합니다.")
    return NL2SQLProcessor(openai_key=openai_key, model=model)


app = FastAPI(
    title="BookStore NL2SQL API",
    version="0.1.0",
    description="챗봇형 단일 엔드포인트: 질문(SQL 생성)과 피드백(저장/교정)을 처리합니다.",
)

_session_lock = Lock()
_session_store: dict[str, dict[str, Any]] = {}


def _get_session(session_id: str | None) -> dict[str, Any] | None:
    if not session_id:
        return None
    with _session_lock:
        return _session_store.get(session_id)


def _set_session(
    session_id: str,
    query: str,
    sql: str,
    explanation: str,
    dissatisfaction_count: int | None = None,
) -> None:
    with _session_lock:
        previous = _session_store.get(session_id, {})
        _session_store[session_id] = {
            "query": query,
            "sql": sql,
            "explanation": explanation,
            "dissatisfaction_count": (
                dissatisfaction_count
                if dissatisfaction_count is not None
                else int(previous.get("dissatisfaction_count", 0))
            ),
        }


def _get_dissatisfaction_count(session_data: dict[str, Any]) -> int:
    return SessionData.from_mapping(session_data).dissatisfaction_count


@lru_cache(maxsize=1)
def _get_unsatisfied_logger() -> logging.Logger:
    UNSATISFIED_LOG_PATH.parent.mkdir(parents=True, exist_ok=True)

    logger = logging.getLogger("nl2sql.unsatisfied")
    logger.setLevel(logging.INFO)
    logger.propagate = False

    if not logger.handlers:
        handler = RotatingFileHandler(
            UNSATISFIED_LOG_PATH,
            maxBytes=2_000_000,
            backupCount=5,
            encoding="utf-8",
        )
        handler.setFormatter(logging.Formatter("%(asctime)s %(message)s"))
        logger.addHandler(handler)
    return logger


def _roll_unsatisfied_log(
    session_id: str,
    session_data: dict[str, Any],
    feedback_text: str,
    attempt_count: int,
) -> None:
    snapshot = SessionData.from_mapping(session_data)
    payload = {
        "session_id": session_id,
        "query": snapshot.query,
        "sql": snapshot.sql,
        "explanation": snapshot.explanation,
        "feedback_text": feedback_text,
        "attempt_count": attempt_count,
        "max_attempts": MAX_DISSATISFACTION_ATTEMPTS,
    }
    _get_unsatisfied_logger().info(json.dumps(payload, ensure_ascii=False))


def _to_confirmation_response(
    session_id: str,
    session_data: dict[str, Any],
    validation_passed: bool | None = None,
    message: str = "SQL을 생성했습니다. 결과가 마음에 드셨나요?",
) -> ChatResponse:
    snapshot = SessionData.from_mapping(session_data)
    return ChatResponse(
        session_id=session_id,
        status="AWAITING_CONFIRMATION",
        message=message,
        query=snapshot.query,
        sql=snapshot.sql,
        explanation=snapshot.explanation,
        validation_passed=validation_passed,
        attempt_count=snapshot.dissatisfaction_count,
        max_attempts=MAX_DISSATISFACTION_ATTEMPTS,
    )


async def _send_ws_chat_response(websocket: WebSocket, response: ChatResponse) -> None:
    payload = ChatWsResponse(**response.model_dump()).model_dump()
    await websocket.send_json(payload)


async def _send_ws_error(websocket: WebSocket, message: str, code: str = "BAD_REQUEST") -> None:
    await websocket.send_json(ChatWsError(code=code, message=message).model_dump())


@app.websocket("/v1/chat/ws")
async def chat_ws(websocket: WebSocket) -> None:
    await websocket.accept()
    processor: NL2SQLProcessor | None = None

    while True:
        try:
            raw_payload = await websocket.receive_json()
        except WebSocketDisconnect:
            break
        except Exception:
            await _send_ws_error(websocket, "JSON 형식 메시지를 보내주세요.")
            continue

        try:
            payload = ChatWsRequest.model_validate(raw_payload)
        except ValidationError as e:
            message = e.errors()[0].get("msg", "요청 형식이 올바르지 않습니다.")
            await _send_ws_error(websocket, str(message))
            continue

        try:
            if processor is None:
                processor = get_processor()

            if payload.action == "ASK":
                query_text = payload.query_text
                query_result = processor.process(QueryRequest(query=query_text))
                session_id = payload.session_id or str(uuid4())
                sql_text = (query_result.parsed_sql or query_result.sql or "").strip()
                explanation = (query_result.explanation or "").strip()

                _set_session(
                    session_id=session_id,
                    query=query_result.query,
                    sql=sql_text,
                    explanation=explanation,
                    dissatisfaction_count=0,
                )
                session_data = _get_session(session_id) or {}
                await _send_ws_chat_response(
                    websocket,
                    _to_confirmation_response(
                        session_id=session_id,
                        session_data=session_data,
                        validation_passed=query_result.validation_passed,
                    ),
                )
                continue

            if payload.action == "CONFIRM":
                session_id = str(payload.session_id)
                session_data = _get_session(session_id)
                if session_data is None:
                    await _send_ws_error(
                        websocket,
                        "session_id를 찾을 수 없습니다.",
                        code="SESSION_NOT_FOUND",
                    )
                    continue

                if payload.satisfied:
                    feedback_request = build_feedback_request_from_session(
                        session_data=session_data,
                        satisfied=True,
                        feedback_text=None,
                    )
                    feedback_result = processor.handle_feedback(feedback_request)
                    await _send_ws_chat_response(
                        websocket,
                        ChatResponse(
                            session_id=session_id,
                            status="DONE",
                            message=feedback_result.message or "확인되었습니다.",
                            query=session_data["query"],
                            sql=session_data["sql"],
                            explanation=session_data["explanation"],
                            attempt_count=_get_dissatisfaction_count(session_data),
                            max_attempts=MAX_DISSATISFACTION_ATTEMPTS,
                        ),
                    )
                else:
                    current_attempts = _get_dissatisfaction_count(session_data)
                    if current_attempts >= MAX_DISSATISFACTION_ATTEMPTS:
                        await _send_ws_chat_response(
                            websocket,
                            ChatResponse(
                                session_id=session_id,
                                status="MAX_ATTEMPTS_EXCEEDED",
                                message=(
                                    "최대 3회 교정 시도를 완료했습니다. "
                                    "추가 개선은 수동 분석으로 진행됩니다."
                                ),
                                query=session_data["query"],
                                sql=session_data["sql"],
                                explanation=session_data["explanation"],
                                attempt_count=current_attempts,
                                max_attempts=MAX_DISSATISFACTION_ATTEMPTS,
                            ),
                        )
                    else:
                        await _send_ws_chat_response(
                            websocket,
                            ChatResponse(
                                session_id=session_id,
                                status="AWAITING_FEEDBACK_REASON",
                                message="어떤 부분이 마음에 들지 않으셨나요?",
                                query=session_data["query"],
                                sql=session_data["sql"],
                                explanation=session_data["explanation"],
                                attempt_count=current_attempts,
                                max_attempts=MAX_DISSATISFACTION_ATTEMPTS,
                            ),
                        )
                continue

            if payload.action == "FEEDBACK":
                session_id = str(payload.session_id)
                feedback_text = payload.feedback_reason
                session_data = _get_session(session_id)
                if session_data is None:
                    await _send_ws_error(
                        websocket,
                        "session_id를 찾을 수 없습니다.",
                        code="SESSION_NOT_FOUND",
                    )
                    continue

                current_attempts = _get_dissatisfaction_count(session_data)
                if current_attempts >= MAX_DISSATISFACTION_ATTEMPTS:
                    _roll_unsatisfied_log(
                        session_id=session_id,
                        session_data=session_data,
                        feedback_text=feedback_text,
                        attempt_count=current_attempts,
                    )
                    await _send_ws_chat_response(
                        websocket,
                        ChatResponse(
                            session_id=session_id,
                            status="MAX_ATTEMPTS_EXCEEDED",
                            message=(
                                "최대 3회 교정 시도 이후에도 불만족으로 판단되어 "
                                "로그 파일로 롤링 저장했습니다."
                            ),
                            query=session_data["query"],
                            sql=session_data["sql"],
                            explanation=session_data["explanation"],
                            attempt_count=current_attempts,
                            max_attempts=MAX_DISSATISFACTION_ATTEMPTS,
                        ),
                    )
                    continue

                next_attempt_count = current_attempts + 1
                feedback_request = build_feedback_request_from_session(
                    session_data=session_data,
                    satisfied=False,
                    feedback_text=feedback_text,
                )
                processor.handle_feedback(feedback_request)
                repair_result = processor.repair_from_feedback(feedback_request)

                if repair_result.success and repair_result.corrected_sql:
                    final_sql = repair_result.corrected_sql
                    final_explanation = repair_result.change_summary or session_data["explanation"]
                    _set_session(
                        session_id=session_id,
                        query=session_data["query"],
                        sql=final_sql,
                        explanation=final_explanation,
                        dissatisfaction_count=next_attempt_count,
                    )
                    refreshed_session = _get_session(session_id) or {}
                    await _send_ws_chat_response(
                        websocket,
                        ChatResponse(
                            session_id=session_id,
                            status="AWAITING_CONFIRMATION",
                            message=(
                                "피드백을 반영해 SQL을 수정했습니다. "
                                "결과가 마음에 드셨나요?"
                            ),
                            query=refreshed_session["query"],
                            sql=refreshed_session["sql"],
                            explanation=refreshed_session["explanation"],
                            repair=repair_result,
                            attempt_count=next_attempt_count,
                            max_attempts=MAX_DISSATISFACTION_ATTEMPTS,
                        ),
                    )
                else:
                    _set_session(
                        session_id=session_id,
                        query=session_data["query"],
                        sql=session_data["sql"],
                        explanation=session_data["explanation"],
                        dissatisfaction_count=next_attempt_count,
                    )

                    if next_attempt_count >= MAX_DISSATISFACTION_ATTEMPTS:
                        _roll_unsatisfied_log(
                            session_id=session_id,
                            session_data=session_data,
                            feedback_text=feedback_text,
                            attempt_count=next_attempt_count,
                        )
                        await _send_ws_chat_response(
                            websocket,
                            ChatResponse(
                                session_id=session_id,
                                status="MAX_ATTEMPTS_EXCEEDED",
                                message=(
                                    "최대 3회 교정 시도 이후에도 불만족으로 판단되어 "
                                    "로그 파일로 롤링 저장했습니다."
                                ),
                                query=session_data["query"],
                                sql=session_data["sql"],
                                explanation=session_data["explanation"],
                                repair=repair_result,
                                attempt_count=next_attempt_count,
                                max_attempts=MAX_DISSATISFACTION_ATTEMPTS,
                            ),
                        )
                    else:
                        await _send_ws_chat_response(
                            websocket,
                            ChatResponse(
                                session_id=session_id,
                                status="AWAITING_FEEDBACK_REASON",
                                message=(
                                    "교정 시도에 실패했습니다. "
                                    "원인을 조금 더 구체적으로 알려주세요."
                                ),
                                query=session_data["query"],
                                sql=session_data["sql"],
                                explanation=session_data["explanation"],
                                repair=repair_result,
                                attempt_count=next_attempt_count,
                                max_attempts=MAX_DISSATISFACTION_ATTEMPTS,
                            ),
                        )
                continue

            await _send_ws_error(websocket, "지원하지 않는 action입니다.")
        except WebSocketDisconnect:
            break
        except Exception as e:
            try:
                await _send_ws_error(websocket, f"처리 중 오류가 발생했습니다: {e}", code="INTERNAL_ERROR")
            except Exception:
                break
