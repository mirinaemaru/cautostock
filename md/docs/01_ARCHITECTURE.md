# 아키텍처 개요

- 스타일: Layered + Hexagonal(Ports & Adapters)
- 주요 레이어
  - domain: 비즈니스 규칙(순수)
  - application: 유스케이스/워크플로우
  - infra: DB/Outbox/Scheduler
  - broker: KIS Adapter
  - api: Controller/Admin/Query

- 이벤트 기반 처리
  - DB 트랜잭션 + Outbox
  - At-least-once 발행
