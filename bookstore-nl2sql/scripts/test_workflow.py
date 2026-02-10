# """
# QueryProcessor 검증 테스트 스크립트

# 테스트 항목:
# 1. 정상 쿼리 생성 및 검증 통과
# 2. 잘못된 테이블명 사용 시 검증 실패
# 3. 세미콜론 누락 시 검증 실패
# 4. 재생성 기능 테스트
# """

# import os
# import sys
# from pathlib import Path

# # 프로젝트 루트를 PYTHONPATH에 추가
# project_root = Path(__file__).parent.parent
# sys.path.insert(0, str(project_root / "src"))

# from nl2sql.core.query_processor import QueryProcessor
# from nl2sql.models.query import QueryRequest
# from dotenv import load_dotenv
# from loguru import logger
# import json

# # 환경변수 로드
# load_dotenv()

# def print_separator(title: str):
#     """구분선 출력"""
#     print("\n" + "="*80)
#     print(f"  {title}")
#     print("="*80 + "\n")

# def print_response(response):
#     """응답 결과를 보기 좋게 출력"""
#     print(f"📝 질문: {response.question}")
#     print(f"🤖 생성된 SQL:\n{response.sql}\n")
    
#     if response.validation_passed:
#         print("✅ 검증 통과")
#         if response.parsed_sql:
#             print(f"📋 포맷된 SQL:\n{response.parsed_sql}\n")
#     else:
#         print("❌ 검증 실패")
#         print(f"오류: {response.validation_error}")
    
#     print(f"⏱️  실행 시간: {response.execution_time_ms}ms")
#     print(f"📚 검색된 스키마: {[s.table_name for s in response.retrieved_schema]}")
#     print(f"💡 검색된 예제: {len(response.retrieved_few_shot)}개")

# def test_valid_query():
#     """테스트 1: 정상 쿼리 생성 및 검증 통과"""
#     print_separator("테스트 1: 정상 쿼리 생성")
    
#     processor = QueryProcessor(
#         open_ai_key=os.getenv("OPENAI_API_KEY"),
#         enable_validation=True
#     )
    
#     # 정상적인 질문
#     request = QueryRequest(query="재고가 10개 미만인 책을 조회해줘")
#     response = processor.process(request)
    
#     print_response(response)
    
#     assert response.validation_passed, "정상 쿼리는 검증을 통과해야 합니다"
#     print("\n✅ 테스트 1 통과!")
    
#     processor.close()

# def test_invalid_table():
#     """테스트 2: 잘못된 테이블명 사용 시 검증 실패"""
#     print_separator("테스트 2: 잘못된 테이블명 사용")
    
#     processor = QueryProcessor(
#         open_ai_key=os.getenv("OPENAI_API_KEY"),
#         enable_validation=True
#     )
    
#     # 존재하지 않는 테이블을 언급하는 질문
#     request = QueryRequest(query="invalid_books 테이블에서 데이터를 가져와줘")
#     response = processor.process(request)
    
#     print_response(response)
    
#     # 검증 실패가 예상됨
#     if not response.validation_passed:
#         print("\n✅ 테스트 2 통과! (예상대로 검증 실패)")
#     else:
#         print("\n⚠️ 테스트 2: LLM이 올바른 테이블을 사용함 (검증 통과)")
    
#     processor.close()

# def test_missing_semicolon():
#     """테스트 3: 세미콜론 누락 (LLM이 누락할 가능성 낮음)"""
#     print_separator("테스트 3: 세미콜론 누락 시나리오")
    
#     processor = QueryProcessor(
#         open_ai_key=os.getenv("OPENAI_API_KEY"),
#         enable_validation=True
#     )
    
#     request = QueryRequest(query="books 테이블의 모든 데이터")
#     response = processor.process(request)
    
#     print_response(response)
    
#     print("\n✅ 테스트 3 완료!")
    
#     processor.close()

# def test_regeneration():
#     """테스트 4: 재생성 기능 테스트"""
#     print_separator("테스트 4: SQL 재생성")
    
#     processor = QueryProcessor(
#         open_ai_key=os.getenv("OPENAI_API_KEY"),
#         enable_validation=True
#     )
    
#     # 첫 시도
#     print("🔹 1차 시도:")
#     request = QueryRequest(query="재고가 5개 미만인 책 조회")
#     response1 = processor.process(request)
    
#     print(f"생성된 SQL: {response1.sql}")
#     print(f"검증 통과: {response1.validation_passed}")
    
#     # 강제로 재생성 시뮬레이션
#     if response1.validation_passed:
#         print("\n⚠️ 1차 시도가 성공했으므로 재생성을 강제로 시뮬레이션합니다.")
        
#         # 가짜 오류 시나리오
#         fake_error = "존재하지 않는 테이블이 포함되어 있습니다: invalid_table"
#         fake_suggestions = ["사용 가능한 테이블: books, users, orders"]
        
#         print(f"\n🔹 2차 시도 (재생성):")
#         print(f"이전 오류: {fake_error}")
        
#         response2 = processor.regenerate_sql(
#             original_request=request,
#             previous_sql=response1.sql,
#             error_message=fake_error,
#             suggestions=fake_suggestions,
#             schemas=response1.retrieved_schema,
#             few_shot=response1.retrieved_few_shot
#         )
        
#         print(f"재생성된 SQL: {response2.sql}")
#         print(f"검증 통과: {response2.validation_passed}")
#     else:
#         # 실제로 실패한 경우 재생성
#         print("\n🔹 2차 시도 (실제 재생성):")
#         response2 = processor.regenerate_sql(
#             original_request=request,
#             previous_sql=response1.sql,
#             error_message=response1.validation_error,
#             suggestions=["수정 제안"],
#             schemas=response1.retrieved_schema,
#             few_shot=response1.retrieved_few_shot
#         )
        
#         print_response(response2)
    
#     print("\n✅ 테스트 4 완료!")
    
#     processor.close()

# def test_multiple_queries():
#     """테스트 5: 여러 질문 연속 처리"""
#     print_separator("테스트 5: 여러 질문 연속 처리")
    
#     processor = QueryProcessor(
#         open_ai_key=os.getenv("OPENAI_API_KEY"),
#         enable_validation=True
#     )
    
#     queries = [
#         "가장 비싼 책 5권은?",
#         "어제 주문한 사용자는?",
#         "출판사별 평균 도서 가격은?",
#         "인기 도서 중 재고가 있는 책은?",
#     ]
    
#     results = []
#     for i, query in enumerate(queries, 1):
#         print(f"\n📝 질문 {i}: {query}")
#         request = QueryRequest(query=query)
#         response = processor.process(request)
        
#         print(f"SQL: {response.sql}")
#         print(f"검증: {'✅ 통과' if response.validation_passed else '❌ 실패'}")
#         print(f"시간: {response.execution_time_ms}ms")
        
#         results.append({
#             "query": query,
#             "sql": response.sql,
#             "validation_passed": response.validation_passed,
#             "execution_time_ms": response.execution_time_ms
#         })
    
#     # 결과 요약
#     print("\n" + "="*80)
#     print("📊 결과 요약")
#     print("="*80)
    
#     total = len(results)
#     passed = sum(1 for r in results if r["validation_passed"])
#     avg_time = sum(r["execution_time_ms"] for r in results) / total
    
#     print(f"총 질문: {total}개")
#     print(f"검증 통과: {passed}개 ({passed/total*100:.1f}%)")
#     print(f"평균 처리 시간: {avg_time:.1f}ms")
    
#     print("\n✅ 테스트 5 완료!")
    
#     processor.close()

# def main():
#     """모든 테스트 실행"""
    
#     print("="*80)
#     print("  NL2SQL QueryProcessor 검증 테스트")
#     print("="*80)
    
#     try:
#         # 각 테스트 실행
#         test_valid_query()
#         test_invalid_table()
#         test_missing_semicolon()
#         test_regeneration()
#         test_multiple_queries()
        
#         print("\n" + "="*80)
#         print("  🎉 모든 테스트 완료!")
#         print("="*80)
        
#     except Exception as e:
#         logger.error(f"테스트 실패: {e}")
#         import traceback
#         traceback.print_exc()

# if __name__ == "__main__":
#     main()