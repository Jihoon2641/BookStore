from langchain_core.runnables import chain
from langchain_core.output_parsers import PydanticOutputParser
from langchain_openai import ChatOpenAI

from nl2sql.core.prompt.generation_prompt import (
    create_generation_prompt,
    format_few_shot,
    format_schemas,
)
from nl2sql.models.llm_output import SQLOutput

def create_generation_chain(llm_model: str, openai_key: str):

    llm = ChatOpenAI(
        model=llm_model,
        api_key=openai_key,
        temperature=0.1
    )

    output_parser = PydanticOutputParser(pydantic_object=SQLOutput)

    prompt = create_generation_prompt()
    
    @chain
    def generate_and_convert(input_data):
        """
        SQL 생성 및 딕셔너리 변환
        
        입력: {
            "query": "...",
            "schemas": [...],
            "few_shot": [...]
        }
        
        출력: {
            "query": "...",
            "schemas": [...],
            "few_shot": [...],
            "sql": "...",
            "explanation": "..."
        }
        """

        prompt_input = {
            "query": input_data.get("query", ""),
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
