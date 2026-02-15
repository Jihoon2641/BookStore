# src/nl2sql/core/prompt/prompt_template.py

import json


class PromptTemplate:
    """SQL 생성 프롬프트 템플릿"""

    SYSTEM_PROMPT = """
당신은 MySQL SQL 전문가입니다.
사용자의 자연어 질문을 정확한 SQL 쿼리로 변환하는 것이 목표입니다.

**중요 규칙:**
1. SELECT 쿼리만 생성하세요. (INSERT, UPDATE, DELETE 쿼리 금지)
2. 제공된 스키마 정보만을 사용하세요.
3. 존재하지 않는 테이블이나 컬럼을 사용하지 마세요.
4. MySQL 문법을 사용하세요.
5. 세미콜론(;)으로 끝내세요.
6. 반드시 JSON 형식으로 응답하세요.
"""

    @staticmethod
    def format_schema_context(schemas: list) -> str:
        """검색된 스키마를 프롬프트용 텍스트로 변환"""
        if not schemas:
            return "사용 가능한 스키마가 없습니다."

        lines = ["**사용 가능한 테이블 스키마:**\n"]

        for schema in schemas:
            if isinstance(schema, dict):
                table_name = schema["table_name"]
                metadata = schema.get("metadata", schema)
                description = metadata.get("description", "")
                columns_raw = metadata.get("columns", "[]")
                fk_raw = metadata.get("foreign_keys", "[]")
            else:
                table_name = getattr(schema, "table_name", "")
                description = getattr(schema, "description", "")
                columns_raw = getattr(schema, "columns", "[]")
                fk_raw = getattr(schema, "foreign_keys", "[]")

            columns = json.loads(columns_raw) if isinstance(columns_raw, str) else columns_raw
            foreign_keys = json.loads(fk_raw) if isinstance(fk_raw, str) else fk_raw

            lines.append(f"### {table_name} ({description})")
            lines.append("컬럼:")

            for col in columns:
                nullable = "NULL 가능" if col.get("nullable") else "NOT NULL"
                key_info = f" [{col['key']}]" if col.get("key") else ""
                lines.append(
                    f"  - {col['name']}: {col.get('description_ko', col.get('description', ''))} "
                    f"({col['type']}, {nullable}{key_info})"
                )

            if foreign_keys:
                lines.append("외래키:")
                for fk in foreign_keys:
                    lines.append(
                        f"  - {fk['column_name']} → {fk['referenced_table']}.{fk['referenced_column']}"
                    )

            lines.append("")

        return "\n".join(lines)

    @staticmethod
    def format_few_shot_context(examples: list) -> str:
        """검색된 예제를 프롬프트용 텍스트로 변환"""
        if not examples:
            return "참고할 예제가 없습니다."

        lines = ["**참고 예제:**\n"]

        for i, example in enumerate(examples, 1):
            if isinstance(example, dict):
                query = example["query"]
                metadata = example.get("metadata", example)
                sql = metadata.get("sql", "")
                explanation = metadata.get("explanation", "")
            else:
                query = getattr(example, "query", "")
                sql = getattr(example, "sql", "")
                explanation = getattr(example, "explanation", "")

            lines.append(f"예제 {i}:")
            lines.append(f"질문: {query}")
            lines.append(f"SQL: {sql}")
            lines.append(f"설명: {explanation}")
            lines.append("")

        return "\n".join(lines)

    @staticmethod
    def build_user_prompt(
        query: str, schemas: str, few_shot: str, format_instructions: str
    ) -> str:
        """
        일반 사용자 질문 프롬프트 생성

        Args:
            query: 자연어 질문
            schemas: 포맷된 스키마 텍스트
            few_shot: 포맷된 예제 텍스트
            format_instructions: 출력 형식 지시사항

        Returns:
            포맷된 프롬프트 텍스트
        """
        return f"""
{schemas}

{few_shot}

**사용자 질문:**
{query}

**출력 형식:**
{format_instructions}

**요구사항:**
- 위 스키마와 예제를 참고하여 정확한 SQL을 생성하세요.
- 반드시 지정된 JSON 형식으로 응답하세요.
- SQL은 세미콜론(;)으로 끝나야 합니다.
"""

    @staticmethod
    def build_retry_prompt(
        query: str,
        schemas: str,
        few_shot: str,
        previous_sql: str,
        error_message: str,
        suggestions: list[str],
        format_instructions: str,
    ) -> str:
        """
        재생성용 프롬프트 (오류 피드백 포함)

        Args:
            query: 원본 자연어 질문
            schemas: 포맷된 스키마 텍스트
            few_shot: 포맷된 예제 텍스트
            previous_sql: 이전에 생성된 SQL (오류 있음)
            error_message: 검증 오류 메시지
            suggestions: 수정 제안 사항 리스트
            format_instructions: 출력 형식 지시사항

        Returns:
            오류 피드백이 포함된 재생성 프롬프트
        """

        suggestions_text = "\n".join([f"  - {s}" for s in suggestions])

        return f"""
{schemas}

{few_shot}

**사용자 질문:**
{query}

**이전 SQL 생성 실패:**
아래 SQL은 검증에 실패했습니다.
```sql
{previous_sql}
```

**검증 오류:**
{error_message}

**수정 제안:**
{suggestions_text}

**출력 형식:**
{format_instructions}

**재생성 요구사항:**
1. 위 오류를 수정하여 올바른 SQL을 생성하세요.
2. 제공된 스키마에 정의된 테이블과 컬럼만 사용하세요.
3. 반드시 지정된 JSON 형식으로 응답하세요.
4. SELECT 쿼리만 작성하세요.
5. SQL은 세미콜론(;)으로 끝나야 합니다.
"""
