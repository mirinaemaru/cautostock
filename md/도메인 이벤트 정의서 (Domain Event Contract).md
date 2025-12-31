도메인 이벤트 정의서 (Domain Event Contract)

(B안 – KIS OpenAPI 연계 자동매매 시스템 / MVP)

목적:

시스템 내 주요 상태 변화(신호/리스크/주문/체결/포지션/손익/운영)를 이벤트로 표준화

감사로그/알림/모니터링/백테스트/리플레이에 동일 포맷 재사용

이벤트 기반 처리(Outbox/Message Bus/로그 스트림) 시 계약(Contract) 역할

1. 이벤트 공통 규격 (Envelope)
1.1 공통 필드
필드	타입	설명
eventId	string	이벤트 고유 ID(ULID/UUID)
eventType	string	이벤트 타입(예: SignalGenerated)
occurredAt	string(datetime)	발생 시각(UTC 또는 KST 고정)
producedBy	string	발행 컴포넌트(예: trading-application)
version	string	이벤트 스키마 버전(예: 1.0)
correlationId	string	요청/플로우 추적용(예: runId, requestId)
causationId	string	직접 원인 이벤트 ID(옵션)
environment	string	PAPER / LIVE / BACKTEST
payload	object	이벤트별 상세 내용
meta	object	(옵션) 디버그/추적 메타
1.2 공통 JSON 예시
{
  "eventId": "evt_01HZXK8Q8V5Q2QG9T3KQ2G8H1C",
  "eventType": "SignalGenerated",
  "occurredAt": "2025-12-18T08:10:15.123+09:00",
  "producedBy": "trading-application",
  "version": "1.0",
  "correlationId": "run_01HZXK8MZ2R3J7F0FQ0E2X9P9A",
  "causationId": "evt_01HZXK8K5G8FQ4A1C9KXK1B2C3",
  "environment": "PAPER",
  "payload": {},
  "meta": {
    "traceId": "7c2f0d9c8e0c4a3a",
    "host": "trading-api-1"
  }
}

2. 이벤트 분류 & 발행 시점
2.1 분류

Signal: 전략 실행 결과(신호)

Risk: 사전/사후 리스크 판단, Kill Switch

Order/Execution: 주문 발행/상태 변화/체결 반영

Position/PnL: 포지션/손익 업데이트

Ops: 운영/장애/연동 상태, 알림 발송

2.2 발행 원칙

DB 정합성 보장을 위해 Outbox Pattern 권장

트랜잭션 커밋 후 이벤트 발행

소비자는 중복 처리 가능(At-least-once) 를 전제로 구현

eventId 기반 멱등 처리

3. 이벤트 상세 정의 (MVP)

아래 payload는 최소 필드 중심(MVP).
v2에서 확장 필드는 meta 또는 payload 하위에 추가한다.

3.1 SignalGenerated

설명: 전략 실행 결과 신호 생성

발행 시점: 신호 저장 완료 직후

payload 스키마(요약)
필드	타입	필수	설명
signalId	string	Y	신호 ID
strategyId	string	Y	전략 ID
strategyVersionId	string	Y	전략 버전 ID
symbol	string	Y	종목
signalType	string	Y	BUY/SELL/HOLD
targetType	string	Y	QTY/WEIGHT
targetValue	number	Y	목표 값
ttlSeconds	integer	Y	TTL
reason	string	N	HOLD 사유 등
indicators	object	N	지표 스냅샷(키-값)
JSON 예시
{
  "eventId": "evt_01HZXK9B6B2E0WZ2J2M5YQJ8C1",
  "eventType": "SignalGenerated",
  "occurredAt": "2025-12-18T08:11:00.000+09:00",
  "producedBy": "trading-application",
  "version": "1.0",
  "correlationId": "run_01HZXK8MZ2R3J7F0FQ0E2X9P9A",
  "environment": "PAPER",
  "payload": {
    "signalId": "sig_01HZXK9B1KZ7Q6XQ4Q3N6M2D0R",
    "strategyId": "str_01HZXK7YJ1ZQ9A1D3C4B5A6F7G",
    "strategyVersionId": "strv_01HZXK7Z5E0M1R2T3Y4U5I6O7P",
    "symbol": "005930",
    "signalType": "BUY",
    "targetType": "QTY",
    "targetValue": 1,
    "ttlSeconds": 60,
    "indicators": {
      "ma5": 72010.2,
      "ma20": 71980.4
    }
  }
}

3.2 RiskEvaluated

설명: 주문 전 사전 리스크 평가 결과

발행 시점: risk engine 평가 완료 직후(승인/차단 모두)

payload 스키마(요약)
필드	타입	필수	설명
evaluationId	string	Y	평가 ID
accountId	string	Y	계좌
signalId	string	N	연관 신호
symbol	string	Y	종목
side	string	Y	BUY/SELL
approved	boolean	Y	승인 여부
reasonCode	string	N	차단 사유 코드
reasonMessage	string	N	차단 메시지
limits	object	N	적용된 룰/한도 스냅샷
JSON 예시(승인)
{
  "eventId": "evt_01HZXK9QW0VQ7D8J9K0L1M2N3O",
  "eventType": "RiskEvaluated",
  "occurredAt": "2025-12-18T08:11:01.200+09:00",
  "producedBy": "trading-application",
  "version": "1.0",
  "correlationId": "run_01HZXK8MZ2R3J7F0FQ0E2X9P9A",
  "environment": "PAPER",
  "payload": {
    "evaluationId": "re_01HZXK9Q1A2B3C4D5E6F7G8H9I",
    "accountId": "acct_01HZXK6A0B1C2D3E4F5G6H7I8J",
    "signalId": "sig_01HZXK9B1KZ7Q6XQ4Q3N6M2D0R",
    "symbol": "005930",
    "side": "BUY",
    "approved": true,
    "limits": {
      "maxPositionValuePerSymbol": 100000,
      "maxOpenOrders": 1
    }
  }
}

JSON 예시(차단)
{
  "eventId": "evt_01HZXKA3F1P2Q3R4S5T6U7V8W9",
  "eventType": "RiskEvaluated",
  "occurredAt": "2025-12-18T08:12:00.500+09:00",
  "producedBy": "trading-application",
  "version": "1.0",
  "correlationId": "run_01HZXK8MZ2R3J7F0FQ0E2X9P9A",
  "environment": "PAPER",
  "payload": {
    "evaluationId": "re_01HZXKA2AAABBBCCCDDDEEEFFF",
    "accountId": "acct_01HZXK6A0B1C2D3E4F5G6H7I8J",
    "signalId": "sig_01HZXK9B1KZ7Q6XQ4Q3N6M2D0R",
    "symbol": "005930",
    "side": "BUY",
    "approved": false,
    "reasonCode": "RISK_MAX_POSITION_VALUE",
    "reasonMessage": "Max position value per symbol exceeded",
    "limits": {
      "maxPositionValuePerSymbol": 100000
    }
  }
}

3.3 OrderPlaced

설명: 주문이 브로커로 전송되어 주문번호를 확보

발행 시점: EMS 전송 성공(ACK 수신) 직후

payload 스키마(요약)
필드	타입	필수	설명
orderId	string	Y	내부 주문 ID
accountId	string	Y	계좌
symbol	string	Y	종목
side	string	Y	BUY/SELL
orderType	string	Y	LIMIT/MARKET
ordDvsn	string	Y	KIS ORD_DVSN
qty	number	Y	수량
price	number	N	지정가 가격(시장가는 0 또는 null)
idempotencyKey	string	Y	멱등키
brokerOrderNo	string	Y	브로커 주문번호
JSON 예시
{
  "eventId": "evt_01HZXKBPQWERTYUIOPASDFGHJK",
  "eventType": "OrderPlaced",
  "occurredAt": "2025-12-18T08:11:02.000+09:00",
  "producedBy": "trading-broker-kis",
  "version": "1.0",
  "correlationId": "run_01HZXK8MZ2R3J7F0FQ0E2X9P9A",
  "environment": "PAPER",
  "payload": {
    "orderId": "ord_01HZXKBP1A2B3C4D5E6F7G8H9I",
    "accountId": "acct_01HZXK6A0B1C2D3E4F5G6H7I8J",
    "symbol": "005930",
    "side": "BUY",
    "orderType": "LIMIT",
    "ordDvsn": "00",
    "qty": 1,
    "price": 72010,
    "idempotencyKey": "idem_acct005930_buy_20251218T081100",
    "brokerOrderNo": "0000123456"
  }
}

3.4 OrderStatusChanged

설명: 주문 상태 전이(접수/부분체결/체결/취소/거부/에러)

발행 시점: 주문 상태가 변경되어 저장 완료 직후

payload 스키마(요약)
필드	타입	필수	설명
orderId	string	Y	내부 주문
brokerOrderNo	string	N	브로커 주문번호
prevStatus	string	Y	이전 상태
newStatus	string	Y	신규 상태
reasonCode	string	N	거부/에러 사유
reasonMessage	string	N	메시지
raw	object	N	브로커 원문(요약)
JSON 예시(거부)
{
  "eventId": "evt_01HZXKC8REJECTED0000000000",
  "eventType": "OrderStatusChanged",
  "occurredAt": "2025-12-18T08:11:02.300+09:00",
  "producedBy": "trading-broker-kis",
  "version": "1.0",
  "correlationId": "run_01HZXK8MZ2R3J7F0FQ0E2X9P9A",
  "environment": "PAPER",
  "payload": {
    "orderId": "ord_01HZXKBP1A2B3C4D5E6F7G8H9I",
    "brokerOrderNo": "0000123456",
    "prevStatus": "SENT",
    "newStatus": "REJECTED",
    "reasonCode": "BROKER_INVALID_PRICE",
    "reasonMessage": "Invalid price for current tick size"
  }
}

3.5 FillReceived

설명: 체결(Fill) 수신(부분체결 포함)

발행 시점: fill 저장 완료 직후

payload 스키마(요약)
필드	타입	필수	설명
fillId	string	Y	체결 ID
orderId	string	Y	내부 주문
brokerOrderNo	string	N	브로커 주문
accountId	string	Y	계좌
symbol	string	Y	종목
side	string	Y	BUY/SELL
fillPrice	number	Y	체결가
fillQty	number	Y	체결수량
fee	number	N	수수료
tax	number	N	세금
fillTs	string(datetime)	Y	체결시각
JSON 예시
{
  "eventId": "evt_01HZXKD1FILL00000000000000",
  "eventType": "FillReceived",
  "occurredAt": "2025-12-18T08:11:05.010+09:00",
  "producedBy": "trading-broker-kis",
  "version": "1.0",
  "correlationId": "run_01HZXK8MZ2R3J7F0FQ0E2X9P9A",
  "environment": "PAPER",
  "payload": {
    "fillId": "fill_01HZXKD0A1B2C3D4E5F6G7H8I9",
    "orderId": "ord_01HZXKBP1A2B3C4D5E6F7G8H9I",
    "brokerOrderNo": "0000123456",
    "accountId": "acct_01HZXK6A0B1C2D3E4F5G6H7I8J",
    "symbol": "005930",
    "side": "BUY",
    "fillPrice": 72010,
    "fillQty": 1,
    "fee": 0,
    "tax": 0,
    "fillTs": "2025-12-18T08:11:05.000+09:00"
  }
}

3.6 PositionUpdated

설명: 체결 반영 후 포지션 수량/평단 변경

발행 시점: positions 갱신 완료 직후

payload 스키마(요약)
필드	타입	필수	설명
accountId	string	Y	계좌
symbol	string	Y	종목
prevQty	number	Y	이전 수량
newQty	number	Y	신규 수량
prevAvgPrice	number	Y	이전 평단
newAvgPrice	number	Y	신규 평단
realizedPnlDelta	number	N	실현손익 증분
JSON 예시
{
  "eventId": "evt_01HZXKE0POS000000000000000",
  "eventType": "PositionUpdated",
  "occurredAt": "2025-12-18T08:11:05.200+09:00",
  "producedBy": "trading-application",
  "version": "1.0",
  "correlationId": "run_01HZXK8MZ2R3J7F0FQ0E2X9P9A",
  "environment": "PAPER",
  "payload": {
    "accountId": "acct_01HZXK6A0B1C2D3E4F5G6H7I8J",
    "symbol": "005930",
    "prevQty": 0,
    "newQty": 1,
    "prevAvgPrice": 0,
    "newAvgPrice": 72010,
    "realizedPnlDelta": 0
  }
}

3.7 PnlUpdated

설명: 손익(실현/미실현/총액) 갱신 또는 스냅샷 생성

발행 시점: snapshot 저장 완료 직후(주기적/장마감)

payload 스키마(요약)
필드	타입	필수	설명
accountId	string	Y	계좌
snapshotTs	string(datetime)	Y	스냅샷 시각
totalValue	number	Y	총자산
cash	number	Y	현금
realizedPnl	number	Y	실현손익
unrealizedPnl	number	Y	미실현손익
JSON 예시
{
  "eventId": "evt_01HZXKF2PNL000000000000000",
  "eventType": "PnlUpdated",
  "occurredAt": "2025-12-18T08:12:00.000+09:00",
  "producedBy": "trading-application",
  "version": "1.0",
  "correlationId": "snap_20251218T081200_acct01",
  "environment": "PAPER",
  "payload": {
    "accountId": "acct_01HZXK6A0B1C2D3E4F5G6H7I8J",
    "snapshotTs": "2025-12-18T08:12:00.000+09:00",
    "totalValue": 1001200,
    "cash": 928000,
    "realizedPnl": 0,
    "unrealizedPnl": 1200
  }
}

3.8 KillSwitchChanged

설명: Kill Switch 상태 변경(ON/OFF/ARMED)

발행 시점: kill switch 상태 저장 완료 직후

payload 스키마(요약)
필드	타입	필수	설명
accountId	string	N	계좌 단위(없으면 전역)
prevStatus	string	Y	이전
newStatus	string	Y	신규
reason	string	Y	MANUAL, DAILY_LOSS_LIMIT, CONSECUTIVE_FAILS 등
details	object	N	임계값/측정값 등
JSON 예시
{
  "eventId": "evt_01HZXKG9KILL00000000000000",
  "eventType": "KillSwitchChanged",
  "occurredAt": "2025-12-18T08:14:10.000+09:00",
  "producedBy": "trading-api",
  "version": "1.0",
  "correlationId": "req_01HZXKG8ADMINTOGGLE",
  "environment": "PAPER",
  "payload": {
    "accountId": "acct_01HZXK6A0B1C2D3E4F5G6H7I8J",
    "prevStatus": "OFF",
    "newStatus": "ON",
    "reason": "MANUAL",
    "details": {
      "operator": "admin",
      "note": "demo finale"
    }
  }
}

3.9 AlertDispatched

설명: 알림 발송(슬랙/이메일 등) 결과

발행 시점: notifier가 “발송 시도” 후 결과 확정 시

payload 스키마(요약)
필드	타입	필수	설명
alertId	string	Y	알림 ID
severity	string	Y	INFO/WARN/CRIT
category	string	Y	OPS/RISK/ORDER 등
channel	string	Y	SLACK/EMAIL 등
success	boolean	Y	성공 여부
message	string	Y	메시지
relatedEventId	string	N	연관 이벤트
JSON 예시
{
  "eventId": "evt_01HZXKH7ALERT000000000000",
  "eventType": "AlertDispatched",
  "occurredAt": "2025-12-18T08:14:11.000+09:00",
  "producedBy": "trading-infra",
  "version": "1.0",
  "correlationId": "req_01HZXKG8ADMINTOGGLE",
  "environment": "PAPER",
  "payload": {
    "alertId": "alt_01HZXKH6A1B2C3D4E5F6G7H8I9",
    "severity": "CRIT",
    "category": "RISK",
    "channel": "SLACK",
    "success": true,
    "message": "KILL_SWITCH=ON reason=MANUAL account=acct_01H...",
    "relatedEventId": "evt_01HZXKG9KILL00000000000000"
  }
}

4. 이벤트 타입 목록(요약)
이벤트 타입	의미	주요 소비자(예시)
SignalGenerated	신호 생성	OMS, 모니터링
RiskEvaluated	리스크 승인/차단	OMS, 알림
OrderPlaced	주문 전송/접수	모니터링
OrderStatusChanged	주문 상태 전이	대시보드, 알림
FillReceived	체결 수신	포지션/손익
PositionUpdated	포지션 갱신	리스크, UI
PnlUpdated	손익 스냅샷	리스크, 리포트
KillSwitchChanged	거래 중단/재개	OMS, 알림
AlertDispatched	알림 발송 결과	운영
5. 버전/호환성 정책

version은 스키마 변경 시 증가

1.0 → 필드 추가(하위 호환) OK

필드 이름 변경/타입 변경은 2.0으로 올리고 병행 지원 권장

소비자는 모르는 필드를 무시해야 함(Forward compatible)

6. 저장/전송 권장(Outbox)

outbox 테이블 예시 컬럼

event_id, event_type, occurred_at, payload_json, published_at, retry_count

발행 실패 시 재시도(지수 백오프)

소비자는 event_id로 멱등 처리