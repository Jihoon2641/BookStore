from nl2sql.core.nl2sql_processor import NL2SQLProcessor
from dotenv import load_dotenv
import os
from nl2sql.models.query import QueryRequest

load_dotenv()

openai_key = os.getenv("OPENAI_API_KEY")
model = os.getenv("OPENAI_MODEL", "gpt-4o-mini")

langchain_processor = NL2SQLProcessor(openai_key, model)

response = langchain_processor.process(QueryRequest(query="이번달에 가장 많이 주문한 사용자는?"))

print(response)