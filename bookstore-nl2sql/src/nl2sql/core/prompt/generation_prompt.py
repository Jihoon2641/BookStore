import json

from langchain_core.prompts import ChatPromptTemplate


def create_generation_prompt() -> ChatPromptTemplate:
    return ChatPromptTemplate.from_messages(
        [
            (
                "system",
                """당신은 MySQL SQL 전문가입니다.
        사용자의 자연어 질문을 정확한 SQL 쿼리로 변환하는 것이 목표입니다.
        
        **중요 규칙:**
        1. SELECT 쿼리만 생성하세요.
        2. 제공된 스키마 정보만을 사용하세요.
        3. 존재하지 않는 테이블이나 컬럼을 사용하지 마세요.
        4. MySQL 문법을 사용하세요.
        5. 세미콜론(;)으로 끝내세요.
        """,
            ),
            (
                "user",
                """
        {schemas}
        
        {few_shot}
        
        **사용자 질문:**
        {query}
        
        **출력 형식:**
        {format_instructions}
        """,
            ),
        ]
    )


def format_schemas(schemas: list[dict]) -> str:
    if not schemas:
        return "사용 가능한 스키마가 없습니다."

    lines = ["**사용 가능한 테이블 스키마:**\n"]

    for schema in schemas:
        table_name = schema["table_name"]
        metadata = schema.get("metadata", {})
        description = metadata.get("description", "")

        lines.append(f"### {table_name} ({description})")

        columns_raw = metadata.get("columns", "[]")
        columns = json.loads(columns_raw) if isinstance(columns_raw, str) else columns_raw

        if columns:
            lines.append("컬럼:")
            for col in columns:
                nullable = "NULL 가능" if col.get("nullable") else "NOT NULL"
                key_info = f" [{col.get('key', '')}]" if col.get("key") else ""
                col_desc = col.get("description_ko", col.get("description", ""))
                lines.append(
                    f"  - {col['name']}: {col_desc} ({col['type']}, {nullable}{key_info})"
                )

        fk_raw = metadata.get("foreign_keys", "[]")
        foreign_keys = json.loads(fk_raw) if isinstance(fk_raw, str) else fk_raw

        if foreign_keys:
            lines.append("외래키:")
            for fk in foreign_keys:
                lines.append(
                    f"  - {fk['column_name']} → {fk['referenced_table']}.{fk['referenced_column']}"
                )

        lines.append("")

    return "\n".join(lines)


def format_few_shot(examples: list[dict]) -> str:
    if not examples:
        return "참고할 예제가 없습니다."

    lines = ["**참고 예제:**\n"]

    for i, example in enumerate(examples, 1):
        query = example["query"]
        metadata = example.get("metadata", {})
        sql = metadata.get("sql", "")
        explanation = metadata.get("explanation", "")

        lines.append(f"예제 {i}:")
        lines.append(f"질문: {query}")
        lines.append(f"SQL: {sql}")
        if explanation:
            lines.append(f"설명: {explanation}")
        lines.append("")

    return "\n".join(lines)
