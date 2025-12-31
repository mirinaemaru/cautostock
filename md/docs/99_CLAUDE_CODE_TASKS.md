# Claude Code 작업 지시서

너는 숙련된 Java/Spring 백엔드 엔지니어다.
아래 규칙을 반드시 지켜 코드를 생성하라.

---

## 1. 기술 스택

- Java 17
- Spring Boot 3.x
- Spring Web / Validation / Actuator
- JPA(Hibernate)
- MariaDB
- Flyway
- Lombok
- Jackson
- Gradle or Maven (자유)

---

## 2. 프로젝트 구조

- `/docs` 문서를 단일 진실 소스(Source of Truth)로 사용
- 패키지 구조는 `02_PACKAGE_STRUCTURE.md`를 따른다
- domain 계층은 Spring 의존 최소화

---

## 3. 구현 우선순위

### Phase 1 (필수)
1. 프로젝트 스캐폴딩
2. DB 스키마(Flyway)
3. JPA Entity + Repository
4. OpenAPI 기반 Controller/DTO
5. Global Error Handler
6. Event Outbox 저장

### Phase 2
7. TradingWorkflow Skeleton
8. Risk Engine + Kill Switch
9. Demo API
10. Health Check

### Phase 3 (Stub)
11. KIS Broker Adapter (실제 호출 X, 로그 출력)

---

## 4. 코딩 규칙

- 모든 상태 변경은 트랜잭션 내부에서 처리
- 이벤트는 DB 저장 후 발행
- enum은 문자열 저장
- 날짜/시간은 `DATETIME(3)` 기준
- 테스트 코드는 선택

---

## 5. 산출물

- 컴파일 가능한 코드
- application.yml 예시
- Flyway SQL
- README.md (실행 방법)

---

## 6. 주의사항

- 실제 KIS 주문은 호출하지 말 것
- PAPER 기준으로만 동작
- 비즈니스 판단은 문서에 없는 임의 판단 금지
