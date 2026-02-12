from langchain_core.runnables import chain
from loguru import logger

def create_retry_chain(
    generate_chain,
    validation_chain,
    max_retries: int = 2
):
    """
    재시도 체인 생성
    
    - 검증 실패 시 최대 max_retries번까지 재생성
    - 재시도 횟수 초과 시 실패 상태 유지
    """
    
    @chain
    def retry_logic(output):
        """재시도 로직 (최대 max_retries번)"""
        
        if output.get("is_valid", True):
            logger.info("SQL 검증 통과")
            return output
        
        retry_count = output.get("retry_count", 0)
        
        if retry_count >= max_retries:
            logger.warning(f"최대 재시도 횟수({max_retries}번) 초과")
            return {
                **output,
                "retry_exceeded": True,
                "error_message": f"최대 재시도 횟수({max_retries}번) 초과. 마지막 오류: {output.get('error_message', '알 수 없음')}"
            }
        
        logger.info(f"SQL 재생성 시도 ({retry_count + 1}/{max_retries})")
        
        retry_prompt = f"""
        이전 SQL 생성 실패 (재시도 {retry_count + 1}/{max_retries}):
        {output['sql']}
        
        검증 오류:
        {output.get('error_message', '알 수 없는 오류')}
        
        수정 제안:
        {', '.join(output.get('suggestions', []))}
        
        다시 생성해주세요.
        """
        
        question = output.get('question', '')
        
        try:
            new_output = generate_chain.invoke({
                "question": question + "\n\n" + retry_prompt,
                "schemas": output.get('schemas', []),
                "few_shot": output.get('few_shot', [])
            })
            
            validated_output = validation_chain.invoke({
                **output,
                'sql': new_output.sql,
                'explanation': getattr(new_output, 'explanation', ''),
                'retry_count': retry_count + 1
            })
            
            return retry_logic(validated_output)
            
        except Exception as e:
            logger.error(f"재생성 중 오류 발생: {e}")
            return {
                **output,
                "retry_exceeded": True,
                "error_message": f"재생성 실패: {str(e)}"
            }
    
    return retry_logic