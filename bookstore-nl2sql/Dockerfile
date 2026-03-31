FROM python:3.11-slim AS builder

COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/
WORKDIR /app

COPY pyproject.toml uv.lock ./
RUN uv venv /app/.venv && \
    UV_PROJECT_ENVIRONMENT=/app/.venv uv sync --frozen --no-dev --no-install-project

FROM python:3.11-slim
WORKDIR /app

COPY --from=builder /app/.venv /app/.venv
ENV PATH="/app/.venv/bin:$PATH"
ENV PYTHONPATH="/app/src"

COPY src/ /app/src/
COPY api/ /app/api/
COPY config/ /app/config/
COPY data/ /app/data/

EXPOSE 8000
CMD ["uvicorn", "api.main:app", "--host", "0.0.0.0", "--port", "8000"]
