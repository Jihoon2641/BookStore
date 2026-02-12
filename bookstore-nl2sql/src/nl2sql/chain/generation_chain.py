import json
from langchain_core.runnables import chain
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import PydanticOutputParser
from langchain_openai import ChatOpenAI

from nl2sql.models.llm_output import SQLOutput

def create_generation_chain(llm_model: str, openai_key: str):

    llm = ChatOpenAI(
        model=llm_model,
        api_key=openai_key,
        temperature=0.1
    )

    output_parser = PydanticOutputParser(pydantic_object=SQLOutput)

    prompt = ChatPromptTemplate.from_messages([
        ("system", """당신은 MySQL SQL 전문가입니다.
        사용자의 자연어 질문을 정확한 SQL 쿼리로 변환하는 것이 목표입니다.
        
        **중요 규칙:**
        1. SELECT 쿼리만 생성하세요.
        2. 제공된 스키마 정보만을 사용하세요.
        3. 존재하지 않는 테이블이나 컬럼을 사용하지 마세요.
        4. MySQL 문법을 사용하세요.
        5. 세미콜론(;)으로 끝내세요.
        """),
        ("user", """
        {schemas}
        
        {few_shot}
        
        **사용자 질문:**
        {question}
        
        **출력 형식:**
        {format_instructions}
        """)
    ])
    
    @chain
    def generate_and_convert(input_data):
        """
        SQL 생성 및 딕셔너리 변환
        
        입력: {
            "question": "...",
            "schemas": [...],
            "few_shot": [...]
        }
        
        출력: {
            "question": "...",
            "schemas": [...],
            "few_shot": [...],
            "sql": "...",
            "explanation": "..."
        }
        """

        prompt_input = {
            "question": input_data.get("question", ""),
            "schemas": format_schemas(input_data.get("schemas", [])),
            "few_shot": format_few_shot(input_data.get("few_shot", [])),
            "format_instructions": output_parser.get_format_instructions()
        }
        
        messages = prompt.invoke(prompt_input)
        llm_output = llm.invoke(messages)
        sql_output = output_parser.invoke(llm_output)
        
        return {
            **input_data,
            'sql': sql_output.sql,
            'explanation': sql_output.explanation
        }
    
    return generate_and_convert

def format_schemas(schemas):
    """스키마를 읽기 쉬운 텍스트로 변환"""
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

def format_few_shot(examples):
    """Few-shot 예제를 읽기 쉬운 텍스트로 변환"""
    if not examples:
        return "참고할 예제가 없습니다."
    
    lines = ["**참고 예제:**\n"]
    
    for i, example in enumerate(examples, 1):
        question = example["question"]
        metadata = example.get("metadata", {})
        sql = metadata.get("sql", "")
        explanation = metadata.get("explanation", "")
        
        lines.append(f"예제 {i}:")
        lines.append(f"질문: {question}")
        lines.append(f"SQL: {sql}")
        if explanation:
            lines.append(f"설명: {explanation}")
        lines.append("")
    
    return "\n".join(lines)
