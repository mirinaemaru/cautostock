# Trading System MVP (B안)

이 프로젝트는 KIS OpenAPI 기반 자동매매 시스템의 MVP 구현을 목표로 한다.

본 레포지토리의 `/docs` 문서는 **Claude Code를 통한 코드 자동 생성의 기준 문서**이며,
모든 구현은 이 문서를 “계약(Contract)”으로 삼는다.

우선 목표:
- Spring Boot + MariaDB 기반
- Admin/Query API
- 이벤트 기반(Outbox)
- KIS Adapter는 Stub 수준으로 시작

⚠️ 실계좌 연동(KIS LIVE)은 MVP 범위에 포함하지 않는다.
