FROM python:3.11-slim as builder

COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

WORKDIR /app

COPY pyproject.toml poetry.lock* ./

# uv를 사용하여 가상환경 생성 및 설치 (poetry.lock 기반)
# --no-dev: 개발용 패키지 제외
RUN uv venv /app/.venv && \
    uv pip install --no-cache -r <(uv pip compile pyproject.toml)

FROM python:3.11-slim

WORKDIR /app

# 빌드 단계에서 생성된 가상환경만 복사
COPY --from=builder /app/.venv /app/.venv
ENV PATH="/app/.venv/bin:$PATH"

COPY src/ /app/src/

# 모델 경로 환경 변수 (실행 시 -v로 마운트 필수)
ENV MODEL_PATH="/app/models/multilingual-e5-large"
ENV PYTHONPATH="/app/src"

CMD ["python", "-m", "nl2sql.main"]