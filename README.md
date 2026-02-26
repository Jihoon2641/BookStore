# BookStore

전자상거래 핵심 API(Spring Boot) + 관리자 대시보드(React) + NL2SQL 질의 변환(FastAPI)로 구성된 멀티 모듈 프로젝트입니다.

## 프로젝트 구성

- `bookstore-api`: Spring Boot 백엔드 (주문/회원/도서/모니터링 API)
- `bookstore-frontend`: React + MUI 기반 관리자 대시보드
- `bookstore-nl2sql`: FastAPI 기반 NL2SQL 서비스 (LangChain + ChromaDB + LLM)
- `docker-compose.yml`: MySQL, API, NL2SQL, Nginx, Prometheus, Node Exporter, ChromaDB 통합 기동

---

## 사용 기술 (라이브러리별 정리)

### 1) Spring Boot API (`bookstore-api`)

- `Spring Boot 3.5.9 + Java 21`
  - `org.springframework.boot:spring-boot-starter-web`, `webflux`: REST API, WebClient 기반 외부 호출
  - `spring-boot-starter-validation`: DTO 유효성 검증
  - `spring-boot-starter-security`: JWT 기반 인증/인가 + 접근 제어
  - `spring-boot-starter-data-jpa`: JPA 엔티티/레포지토리 사용 (감사 필드 포함 BaseEntity)
  - `spring-boot-starter-actuator`: 헬스/메트릭 노출
  - `spring-boot-starter-aop`: AOP/필터/이벤트 기반 처리 지원
  - `spring-boot-starter-log4j2`: 로그 백엔드 교체 및 운영 로그 분리(RollingFile Info/Warn/Error)

- `mybatis-spring-boot-starter`
  - SQL 매핑이 필요한 영역을 XML Mapper로 분리(도서/주문/로그/권한 등)
  - 동시성 제어가 중요한 구간은 `FOR UPDATE` + 벌크 업데이트(CASE/IN) 사용

- `bucket4j`
  - IP 기반 Rate Limit 구현 (`tryConsume`, 토큰 버킷, 분당 100 요청)

- `jjwt 0.12.6`
  - JWT 발급/검증 (`JwtUtil`) 및 Security Context 구성

- `mapstruct`
  - 엔티티 ↔ 도메인/응답 DTO 변환에 사용

- `lombok`
  - 보일러플레이트 축소 (`@Getter`, `@Builder` 등)

- `springdoc-openapi`
  - Swagger UI 기반 API 문서 제공 (`/v3/api-docs`, `/swagger-ui`)

- `micrometer-registry-prometheus`
  - 운영 지표를 Prometheus 형식으로 노출 (`/actuator/prometheus`)

### 2) Frontend (`bookstore-frontend`)

- `react@19 + react-dom@19`, `vite@6`
  - SPA + 라우팅(`react-router`)
- `@mui/material`
  - Admin Template 기반 UI 구성
- `@emotion/*`, `@iconify/react`, `dayjs`, `es-toolkit`
  - 스타일/아이콘/유틸
- `@vitejs/plugin-react-swc`, `typescript`, `eslint`, `prettier`
  - 빌드·형상·정적 분석 체계

### 3) NL2SQL (`bookstore-nl2sql`)

- `FastAPI` + `uvicorn`
  - `/v1/chat` 단일 엔드포인트로 생성/확인/피드백 처리
- `langchain`, `langchain-openai`
  - 체이닝 기반 파이프라인 구성(검색→생성→검증→재시도→피드백)
- `openai`
  - LLM 호출(GPT-4o-mini 기본)
- `sentence-transformers`
  - 쿼리/메타데이터 임베딩 생성
- `chromadb`
  - 스키마/예제(few-shot) 벡터 검색
- `sqlparse`, `sqlalchemy`, `pymysql`
  - SQL 검증 및 DB 메타데이터/테스트 실행
- `pydantic`
  - 요청/응답 스키마 검증 (`ChatRequest/Response`)

---

## 아키텍처/패턴 정리

### 헥사고날 스타일(Port & Adapter) 경향
- `domain`, `application`, `adapter`, `port` 패키지 구조가 분리되어 있으며, 핵심 유스케이스는 인터페이스(Port)로 노출하고 Adapter가 영속화/외부 연동을 처리.

### Outbox 패턴(명시적)
- 주문 생성 시점에 `order_log_outbox`를 함께 저장
- 생성/실패 내역을 이벤트(`OrderLogCreatedEvent`)로 발행
- 스케줄러가 PENDING/FAILED 레코드를 주기적 재처리
- 별도 스레드(`@Async`) + 트랜잭션 이벤트 리스너(`@TransactionalEventListener(AFTER_COMMIT)`)로 로그 적재 비동기 분리

### 보안/접근 제어
- JWT 인증 필터(`JwtAuthenticationFilter`) + `SecurityFilterChain`
- `ROLE_USER`, `ROLE_LEVEL_1~SUPER_ADMIN` 계층(RoleHierarchy) 방식 적용
- `/api/v1/admin/monitoring/**`은 `ROLE_LEVEL_1` 이상 허용
- 인증 없는 경로(`/actuator/health`, 스웨거, 공개 회원가입/로그인 API) 예외 처리

### 이상 요청 방어
- `RequestLoggingFilter`에서 Bot UA 탐지 + Rate limit + 응답/요청 로그 저장
- Bot UA 패턴/허용봇 구분과 요청 초과 시 429 응답, 남은 토큰 헤더 제공
- 운영 감사/이상감지 로그를 `user_logs`에 저장

### 관측성(Observability)
- Spring Actuator 지표를 Prometheus가 수집
- Prometheus + Node Exporter 조합으로 컨테이너/호스트/DB/HikariCP/JVM 지표를 합성
- 백엔드에서 `MonitoringService`로 `/api/v1/admin/monitoring/*` API화

### NL2SQL의 대화형 처리 모델
- 1차 질의: SQL 생성 + 검증 결과 반환 + `session_id` 발급
- 만족/미만감 처리:
  - 만족: 피드백 저장/결과 종료
  - 미만감/이유 제공: 피드백 분류 + 재생성 + 검증 + 최대 재시도
- 성공 시 재실행까지 수행하여 쿼리 결과/행 수를 응답에 포함

---

## 핵심 기능/라이브러리 사용 맵

- 인증/회원
  - 사용자 회원가입/로그인: `bookstore-api/src/main/java/.../user/...`
  - 관리자 회원가입/로그인: `bookstore-api/src/main/java/.../admin/...`
- 주문
  - 주문 생성: `bookstore-api/src/main/java/.../order/...`
  - 재고 차감은 MySQL 트랜잭션 내에서 `FOR UPDATE` + 배치 UPDATE 사용
- 도서 수집
  - 네이버 검색 API(WebClient) 호출 후 `books` 저장 (INSERT IGNORE)
- 모니터링
  - 백엔드: Actuator + 커스텀 Prometheus 조회 어댑터
  - 프론트: 주기적 polling(15초)으로 `/api/v1/admin/monitoring/*` 호출
- 보안/로깅
  - JWT + FilterChain + RateLimit + Bot detection + Log4j2 구성

---

## 실행/운영 포인트

- 공통(필수)
  - Java 21, Node 20, Python 3.11 권장
  - 각 모듈별 환경변수는 별도 `.env` 사용
- Docker
  - `docker-compose up -d`로 MySQL, backend, frontend proxy, NL2SQL, Prometheus, Node Exporter까지 일괄 실행 가능
  - 외부 노출 포트(요약): `8080`, `3039`, `8001`, `9090`, `9100`, `8000`

---

## 주의/보완 포인트(문서화 권장)

- 패스워드는 현재 평문 비교/저장이 확인됨(요청/운영 문서에 보안 강화 필요)
- NL2SQL은 상태(stateful) 세션 기반으로 동작하여 동시 다중 세션 관리 정책(세션 만료/저장소 분리) 고려 필요
- `.env` 파일에 실제 키가 포함되어 있으므로 운영 저장소에서는 비밀관리(Secret Manager)로 전환 권장
