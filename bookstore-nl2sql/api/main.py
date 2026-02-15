import os
from functools import lru_cache
from threading import Lock
from uuid import uuid4

from fastapi import FastAPI, HTTPException
from sqlalchemy import text

from api.model import ChatRequest, ChatResponse
from nl2sql.core.nl2sql_processor import NL2SQLProcessor
from nl2sql.models.feedback import FeedbackRequest
from nl2sql.models.query import QueryRequest


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
_session_store: dict[str, dict[str, str]] = {}


def _get_session(session_id: str | None) -> dict[str, str] | None:
    if not session_id:
        return None
    with _session_lock:
        return _session_store.get(session_id)


def _set_session(session_id: str, query: str, sql: str, explanation: str) -> None:
    with _session_lock:
        _session_store[session_id] = {
            "query": query,
            "sql": sql,
            "explanation": explanation,
        }


@app.post("/v1/chat", response_model=ChatResponse)
def chat(payload: ChatRequest) -> ChatResponse:
    try:
        processor = get_processor()
        # 1) 최초 질문 단계: query가 오면 SQL 생성 후 확인 단계로 전환
        if (payload.query or "").strip():
            query_text = payload.query.strip()
            query_result = processor.process(QueryRequest(query=query_text))
            session_id = payload.session_id or str(uuid4())
            sql_text = (query_result.parsed_sql or query_result.sql or "").strip()
            explanation = (query_result.explanation or "").strip()

            _set_session(
                session_id=session_id,
                query=query_result.query,
                sql=sql_text,
                explanation=explanation,
            )
            return ChatResponse(
                session_id=session_id,
                status="AWAITING_CONFIRMATION",
                message="SQL을 생성했습니다. 결과가 마음에 드셨나요?",
                query=query_result.query,
                sql=sql_text,
                explanation=explanation,
                validation_passed=query_result.validation_passed,
            )

        # 2) 이후 단계는 session_id 필수
        if not payload.session_id:
            raise HTTPException(
                status_code=400,
                detail="query가 없으면 session_id가 필요합니다.",
            )

        session_data = _get_session(payload.session_id)
        if session_data is None:
            raise HTTPException(status_code=404, detail="session_id를 찾을 수 없습니다.")

        # 3) 만족 여부가 아직 없으면 확인 단계 안내
        if payload.satisfied is None:
            return ChatResponse(
                session_id=payload.session_id,
                status="AWAITING_CONFIRMATION",
                message="마음에 드셨나요? yes/no를 선택해주세요.",
                query=session_data["query"],
                sql=session_data["sql"],
                explanation=session_data["explanation"],
            )

        feedback_request = FeedbackRequest(
            query=session_data["query"],
            sql=session_data["sql"],
            explanation=session_data["explanation"],
            satisfied=payload.satisfied,
            feedback_text=payload.feedback_text,
        )

        # 4) 만족한 경우: 저장 후 종료
        if payload.satisfied:
            feedback_result = processor.handle_feedback(feedback_request)
            return ChatResponse(
                session_id=payload.session_id,
                status="DONE",
                message=feedback_result.message or "확인되었습니다.",
                query=session_data["query"],
                sql=session_data["sql"],
                explanation=session_data["explanation"],
            )

        # 5) 불만족 + 사유 없음: 사유 입력 유도
        if not (payload.feedback_text or "").strip():
            return ChatResponse(
                session_id=payload.session_id,
                status="AWAITING_FEEDBACK_REASON",
                message="어떤 부분이 마음에 들지 않으셨나요?",
                query=session_data["query"],
                sql=session_data["sql"],
                explanation=session_data["explanation"],
            )

        # 6) 불만족 + 사유 있음: 교정 후 (성공 시) 실행
        feedback_result = processor.handle_feedback(feedback_request)
        repair_result = processor.repair_from_feedback(feedback_request)

        final_sql = session_data["sql"]
        final_explanation = session_data["explanation"]
        rows = None
        row_count = None
        execution_error = None

        if repair_result.success and repair_result.corrected_sql:
            final_sql = repair_result.corrected_sql
            final_explanation = repair_result.change_summary or final_explanation
            _set_session(
                session_id=payload.session_id,
                query=session_data["query"],
                sql=final_sql,
                explanation=final_explanation,
            )
            try:
                clean_sql = final_sql.strip().rstrip(";")
                with processor.db_connector.get_db() as session:
                    cursor = session.execute(text(clean_sql))
                    fetched = cursor.mappings().fetchmany(payload.max_rows)
                    rows = [dict(row) for row in fetched]
                    row_count = len(rows)
            except Exception as exec_err:
                execution_error = str(exec_err)

        done_message = feedback_result.message or "피드백이 반영되었습니다."
        if repair_result.success:
            done_message = "피드백을 반영해 SQL을 수정했습니다."

        return ChatResponse(
            session_id=payload.session_id,
            status="DONE",
            message=done_message,
            query=session_data["query"],
            sql=final_sql,
            explanation=final_explanation,
            repair=repair_result,
            rows=rows,
            row_count=row_count,
            execution_error=execution_error,
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"chat 처리 실패: {e}") from e
