from typing import Dict
from typing import List
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
    4. SQL 쿼리만 변환하고 설명은 추가하지 마세요.
    5. MySQL 문법을 사용하세요.
    6. 세미콜론(;)으로 끝내세요.
    """

    @staticmethod
    def format_schema_context(schemas: List[Dict]) -> str:
        """
        검색된 스키마를 프롬프트용 텍스트로 변환
        
        Args:
            schemas: 검색된 스키마 리스트
            
        Returns:
            포맷된 스키마 텍스트
        """
        if not schemas:
            return "사용 가능한 스키마가 없습니다."
        
        lines = ["**사용 가능한 테이블 스키마:**\n"]
        
        for schema in schemas:
            table_name = schema["table_name"]
            description = schema["metadata"]["description"]
            columns = json.loads(schema["metadata"]["columns"])
            foreign_keys = json.loads(schema["metadata"]["foreign_keys"])
            
            lines.append(f"### {table_name} ({description})")
            lines.append("컬럼:")
            
            for col in columns:
                nullable = "NULL 가능" if col["nullable"] else "NOT NULL"
                key_info = f" [{col['key']}]" if col.get("key") else ""
                lines.append(
                    f"  - {col['name']}: {col['description_ko']} "
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
    def format_few_shot_context(examples: List[Dict]) -> str:
        """
        검색된 예제를 프롬프트용 텍스트로 변환
        
        Args:
            examples: 검색된 예제 리스트
            
        Returns:
            포맷된 예제 텍스트
        """
        if not examples:
            return "참고할 예제가 없습니다."
        
        lines = ["**참고 예제:**\n"]
        
        for i, example in enumerate(examples, 1):
            question = example["question"]
            sql = example["metadata"]["sql"]
            explanation = example["metadata"]["explanation"]
            
            lines.append(f"예제 {i}:")
            lines.append(f"질문: {question}")
            lines.append(f"SQL: {sql}")
            lines.append(f"설명: {explanation}")
            lines.append("")
        
        return "\n".join(lines)

    @staticmethod
    def build_user_prompt(
        question: str, 
        schemas: str,
        few_shot: str) -> str:
        """
        사용자 질문 프롬프트 생성
        
        Args:
            question: 자연어 질문
            schemas: 검색된 스키마 리스트
            examples: 검색된 예제 리스트
            
        Returns:
            포맷된 프롬프트 텍스트
        """    
        return f"""
        {schemas}
        {few_shot}
        **사용자 질문:**
        {question}
        """