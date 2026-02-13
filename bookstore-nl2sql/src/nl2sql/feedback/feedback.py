import sqlparse
from sqlparse.sql import Identifier, IdentifierList
from sqlparse.tokens import Keyword
from nl2sql.models.feedback import FeedbackRequest, FeedbackResponse
from nl2sql.embedding.embedding import get_embedding_model
from nl2sql.vectordb.chroma_store import ChromaStore
from loguru import logger
import json
from pathlib import Path

class Feedback:
    """
        피드백 처리 클래스
        - 피드백을 받아 Few-shot 데이터셋에 추가
    """
    DUPLICATE_DISTANCE_THRESHOLD = 0.20

    def __init__(self, few_shot_path: str):
        self.few_shot_path = Path(few_shot_path)
        self.embedding_model = None
        self.chroma = None
        self._init_similarity_components()

    def _init_similarity_components(self) -> None:
        """
        유사도 기반 중복 검사를 위한 컴포넌트 초기화
        """
        try:
            self.embedding_model = get_embedding_model()
            self.chroma = ChromaStore()
            self.chroma.init_few_shot_collection(reset=False)
        except Exception as e:
            # 벡터 스토어 사용 불가 시 문자열 중복 검사로 폴백
            logger.warning(f"유사도 중복 검사 초기화 실패, 문자열 중복 검사로 폴백: {e}")
            self.embedding_model = None
            self.chroma = None

    def handle(self, request: FeedbackRequest) -> FeedbackResponse:
        if request.satisfied:
            return self._handle_positive(request)
        else:
            return self._handle_negative(request)
    
    def _handle_positive(self, request: FeedbackRequest) -> FeedbackResponse:
        """
        만족스러운 피드백 처리
        - Few-shot 데이터셋에 추가
        """
        try:
            is_duplicate, duplicate_reason = self._is_duplicate(request.question)
            if is_duplicate:
                logger.info(f"이미 존재하는 few_shot 질문입니다 : {request.question}, reason={duplicate_reason}")
                return FeedbackResponse(
                    success=True,
                    message=f"이미 학습된 유사 질문이 존재합니다. ({duplicate_reason})"
                )

            # 새 example_id 생성
            example_id = self._generate_next_id()
            
            # few_shot.json에 추가
            self._append_to_json(example_id, request)

            return FeedbackResponse(
                success=True,
                message="학습데이터에 추가되었습니다.",
                example_id=example_id
            )
        
        except Exception as e:
            return FeedbackResponse(
                success=False,
                message=f"학습데이터 처리 중 오류 발생: {str(e)}"
            )
        
    
    def _handle_negative(self, request: FeedbackRequest) -> FeedbackResponse:
        """
        불만족스러운 피드백 처리
        - 피드백 분석
        - 데이터셋 업데이트
        """

        return FeedbackResponse(
            success=False,
            message="불만족스러운 이유를 알려주세요."
        )

    def _is_duplicate(self, question: str) -> tuple[bool, str]:
        """
        중복 질문인지 확인
        - 벡터 유사도 검색 기반으로만 판단
        """
        if self.embedding_model is None or self.chroma is None:
            return False, "no_similarity_backend"

        try:
            query_embedding = self.embedding_model.encode_single(question)
            results = self.chroma.search_few_shot(query_embedding, top_k=1)
            if not results:
                return False, "no_nearest_neighbor"

            top1 = results[0]
            distance = top1["distance"]
            if distance <= self.DUPLICATE_DISTANCE_THRESHOLD:
                return True, f"semantic_match:{top1['example_id']} distance={distance:.4f}"
            return False, f"semantic_non_duplicate distance={distance:.4f}"
        except Exception as e:
            logger.warning(f"유사도 중복 검사 실패: {e}")
            return False, "similarity_check_failed"

    def _generate_next_id(self) -> str:

        data = self._load_json()
        examples = data.get("examples", [])

        # .json에 few_shot이 없을 경우
        if not examples:
            return "example_001"

        last_id = examples[-1]["example_id"]
        num = int(last_id.split("_")[1]) + 1
        return f"example_{num:03d}"

    def _append_to_json(self, example_id: str, request: FeedbackRequest):
        """
        few_shot.json 파일에 새로운 예제 추가
        """
        data = self._load_json()
        
        new_example = {
            "example_id": example_id,
            "question": request.question,
            "sql": request.sql,
            "explanation": request.explanation,
            "tables_used": self._extract_tables_from_sql(request.sql),
            "tags": ["사용자 피드백"]
        }

        data["examples"].append(new_example)

        with open(self.few_shot_path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=4)
        
        logger.info(f"새로운 예제 추가됨: {example_id}")

    def _load_json(self) -> dict:
        """
        few_shot.json 파일 로드
        """
        if not self.few_shot_path.exists():
            return {"version": "1.0.0", "last_updated": "", "examples": []}
        
        with open(self.few_shot_path, encoding="utf-8") as f:
            return json.load(f)
        
    def _extract_tables_from_sql(self, sql: str) -> list[str]:
        """
        SQL에서 테이블 이름 추출
        """
        
        tables=[]
        parsed = sqlparse.parse(sql)

        if not parsed:
            return tables

        statement = parsed[0]
        from_seen = False

        for token in statement.tokens:
            if from_seen:
                if isinstance(token, IdentifierList):
                    for identifier in token.get_identifiers():
                        tables.append(identifier.get_real_name())
                elif isinstance(token, Identifier):
                    tables.append(token.get_real_name())
                from_seen = False

            if token.ttype is Keyword and token.value.upper() == "FROM":
                from_seen = True
            elif token.ttype is Keyword and "JOIN" in token.value.upper():
                from_seen = True

        return tables

        
