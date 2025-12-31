# 비기능 요구사항

- 트랜잭션 정합성 우선
- 멱등성(idempotency_key) 필수
- 장애 시 데이터 유실 금지
- 로그에 correlationId 포함
- 운영자는 Admin API만 접근
