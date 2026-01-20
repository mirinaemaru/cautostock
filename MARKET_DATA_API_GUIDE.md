# Market Data Subscription API Guide

실시간 시장 데이터 구독을 동적으로 관리할 수 있는 Admin API 가이드입니다.

## 📋 목차

- [API 엔드포인트 목록](#api-엔드포인트-목록)
- [1. 종목 추가](#1-종목-추가)
- [2. 종목 삭제](#2-종목-삭제)
- [3. 구독 종목 조회](#3-구독-종목-조회)
- [4. 구독 상태 확인](#4-구독-상태-확인)
- [5. 재구독](#5-재구독)
- [사용 시나리오](#사용-시나리오)
- [주의사항](#주의사항)

---

## API 엔드포인트 목록

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/admin/market-data/symbols` | 종목 추가 |
| DELETE | `/api/v1/admin/market-data/symbols` | 종목 삭제 |
| GET | `/api/v1/admin/market-data/symbols` | 구독 종목 조회 |
| GET | `/api/v1/admin/market-data/status` | 구독 상태 확인 |
| POST | `/api/v1/admin/market-data/resubscribe` | 재구독 |

**Base URL**: `http://localhost:8099`

---

## 1. 종목 추가

서버 재시작 없이 새로운 종목을 구독 목록에 추가합니다.

### Request

```bash
POST /api/v1/admin/market-data/symbols
Content-Type: application/json

{
  "symbols": ["005490", "000270"]
}
```

### cURL 예시

```bash
curl -X POST http://localhost:8099/api/v1/admin/market-data/symbols \
  -H "Content-Type: application/json" \
  -d '{
    "symbols": ["005490", "000270"]
  }'
```

### Response (성공)

```json
{
  "ok": true,
  "message": "Added 2 symbols to subscription"
}
```

### Response (실패)

```json
{
  "ok": false,
  "message": "Invalid symbol: symbol cannot be null or blank"
}
```

### 종목 코드 예시

| 종목코드 | 종목명 |
|---------|--------|
| 005490 | POSCO홀딩스 |
| 000270 | 기아 |
| 035720 | 카카오 |
| 006400 | 삼성SDI |
| 051900 | LG생활건강 |

---

## 2. 종목 삭제

구독 중인 종목을 목록에서 제거합니다.

### Request

```bash
DELETE /api/v1/admin/market-data/symbols
Content-Type: application/json

{
  "symbols": ["005380", "051910"]
}
```

### cURL 예시

```bash
curl -X DELETE http://localhost:8099/api/v1/admin/market-data/symbols \
  -H "Content-Type: application/json" \
  -d '{
    "symbols": ["005380", "051910"]
  }'
```

### Response (성공)

```json
{
  "ok": true,
  "message": "Removed 2 symbols from subscription"
}
```

### Response (실패 - 모든 종목 삭제 시도)

```json
{
  "ok": false,
  "message": "Cannot remove all symbols - at least one symbol must remain subscribed"
}
```

**주의**: 최소 1개 이상의 종목은 구독 상태를 유지해야 합니다.

---

## 3. 구독 종목 조회

현재 구독 중인 모든 종목을 조회합니다.

### Request

```bash
GET /api/v1/admin/market-data/symbols
```

### cURL 예시

```bash
curl http://localhost:8099/api/v1/admin/market-data/symbols
```

### Response

```json
{
  "symbols": [
    "005930",
    "035420",
    "000660",
    "051910",
    "005380",
    "005490",
    "000270"
  ],
  "total": 7,
  "subscriptionId": "01KFCGZK5M80QTV81ZJ7V0Q7P1",
  "active": true
}
```

### Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| symbols | String[] | 구독 중인 종목 코드 목록 |
| total | Integer | 총 구독 종목 수 |
| subscriptionId | String | 현재 활성 구독 ID (ULID) |
| active | Boolean | 구독 활성 상태 |

---

## 4. 구독 상태 확인

시장 데이터 구독 서비스의 전체 상태를 확인합니다.

### Request

```bash
GET /api/v1/admin/market-data/status
```

### cURL 예시

```bash
curl http://localhost:8099/api/v1/admin/market-data/status
```

### Response (정상)

```json
{
  "subscribed": true,
  "subscriptionId": "01KFCGZK5M80QTV81ZJ7V0Q7P1",
  "symbolCount": 7,
  "connected": true,
  "message": "Active subscription with 7 symbols"
}
```

### Response (비활성)

```json
{
  "subscribed": false,
  "subscriptionId": null,
  "symbolCount": 0,
  "connected": false,
  "message": "No active subscription"
}
```

---

## 5. 재구독

WebSocket 연결 문제가 있을 때 또는 수동으로 재구독이 필요할 때 사용합니다.

### Request

```bash
POST /api/v1/admin/market-data/resubscribe
```

### cURL 예시

```bash
curl -X POST http://localhost:8099/api/v1/admin/market-data/resubscribe
```

### Response (성공)

```json
{
  "ok": true,
  "message": "Successfully resubscribed to market data"
}
```

### 사용 시나리오

- WebSocket 연결이 끊어진 후 재연결
- 구독 상태가 불명확할 때
- 네트워크 문제 해결 후

---

## 사용 시나리오

### 시나리오 1: 새로운 종목 추가

**목표**: POSCO홀딩스(005490)와 기아(000270)를 구독 목록에 추가

```bash
# 1. 현재 구독 종목 확인
curl http://localhost:8099/api/v1/admin/market-data/symbols

# 2. 새 종목 추가
curl -X POST http://localhost:8099/api/v1/admin/market-data/symbols \
  -H "Content-Type: application/json" \
  -d '{"symbols": ["005490", "000270"]}'

# 3. 추가 확인
curl http://localhost:8099/api/v1/admin/market-data/symbols
```

**결과**: 기존 5개 + 새로 추가 2개 = 총 7개 종목 구독

---

### 시나리오 2: 관심 없는 종목 제거

**목표**: 현대차(005380)와 LG화학(051910)을 구독에서 제외

```bash
# 1. 현재 구독 종목 확인
curl http://localhost:8099/api/v1/admin/market-data/symbols

# 2. 종목 삭제
curl -X DELETE http://localhost:8099/api/v1/admin/market-data/symbols \
  -H "Content-Type: application/json" \
  -d '{"symbols": ["005380", "051910"]}'

# 3. 삭제 확인
curl http://localhost:8099/api/v1/admin/market-data/symbols
```

**결과**: 7개 - 2개 = 5개 종목 구독

---

### 시나리오 3: 전체 종목 교체

**목표**: 기존 종목을 모두 제거하고 새로운 종목으로 교체

```bash
# 1. 현재 상태 확인
curl http://localhost:8099/api/v1/admin/market-data/symbols

# 2. 먼저 새 종목 추가 (카카오, 삼성SDI)
curl -X POST http://localhost:8099/api/v1/admin/market-data/symbols \
  -H "Content-Type: application/json" \
  -d '{"symbols": ["035720", "006400"]}'

# 3. 그 다음 기존 종목 삭제 (삼성전자, NAVER 등)
curl -X DELETE http://localhost:8099/api/v1/admin/market-data/symbols \
  -H "Content-Type: application/json" \
  -d '{"symbols": ["005930", "035420", "000660"]}'

# 4. 최종 확인
curl http://localhost:8099/api/v1/admin/market-data/symbols
```

**주의**: 항상 최소 1개 이상의 종목을 유지해야 합니다.

---

### 시나리오 4: 연결 문제 해결

**목표**: WebSocket 연결이 끊어진 후 재연결

```bash
# 1. 상태 확인
curl http://localhost:8099/api/v1/admin/market-data/status

# 2. 재구독 실행
curl -X POST http://localhost:8099/api/v1/admin/market-data/resubscribe

# 3. 재연결 확인
curl http://localhost:8099/api/v1/admin/market-data/status
```

---

## 주의사항

### ⚠️ 중요 제약사항

1. **최소 1개 종목 유지**
   - 모든 종목을 삭제할 수 없습니다
   - 최소 1개 이상의 종목은 항상 구독 상태를 유지해야 합니다

2. **중복 추가 방지**
   - 이미 구독 중인 종목을 다시 추가하면 무시됩니다
   - 새로운 종목만 추가됩니다

3. **구독 재생성**
   - 종목 추가/삭제 시 기존 구독이 해제되고 새로운 구독이 생성됩니다
   - `subscriptionId`가 새로운 ULID로 변경됩니다
   - 잠시(1~2초) 동안 데이터 수신이 중단될 수 있습니다

4. **실시간 적용**
   - 서버 재시작이 필요 없습니다
   - API 호출 즉시 적용됩니다
   - 5초 이내에 새 종목의 틱 데이터를 수신하기 시작합니다

### 📊 성능 고려사항

- **권장 종목 수**: 5~20개
- **최대 종목 수**: 제한 없음 (단, 너무 많으면 성능 저하)
- **구독 변경 주기**: 가급적 빈번한 변경 자제 (1분에 1회 이하 권장)

### 🔒 보안

현재 API는 인증이 구현되어 있지 않습니다. 프로덕션 환경에서는 다음을 고려하세요:

- API 인증 추가 (JWT, API Key 등)
- 관리자 권한 체크
- Rate Limiting
- IP 화이트리스트

---

## 로그 확인

API 호출 후 로그를 확인하여 동작을 검증할 수 있습니다:

```bash
# 로그 실시간 확인
tail -f trading-system.log | grep -E "Adding symbols|Removing symbols|Market data subscription"
```

**예시 로그**:

```
2026-01-20 11:00:00.123 [http-nio-8099-exec-1] INFO  m.t.a.c.a.MarketDataAdminController - Adding symbols to subscription: [005490, 000270]
2026-01-20 11:00:00.124 [http-nio-8099-exec-1] INFO  m.t.a.service.MarketDataService - Adding 2 new symbols to subscription
2026-01-20 11:00:00.234 [http-nio-8099-exec-1] INFO  m.t.a.service.MarketDataService - Market data subscription created: subscriptionId=01KFCH1234ABCD, symbols=[005930, 035420, 000660, 051910, 005380, 005490, 000270]
```

---

## 트러블슈팅

### 문제: API 호출 시 404 에러

**원인**: 서버가 시작되지 않았거나 컨트롤러가 로드되지 않음

**해결**:
```bash
# 서버 상태 확인
curl http://localhost:8099/actuator/health

# 서버 재시작
./run-with-env.sh
```

### 문제: 종목 추가 후에도 틱 데이터가 수신되지 않음

**원인**: STUB 모드에서는 설정된 종목만 시뮬레이션

**해결**:
```bash
# 재구독 실행
curl -X POST http://localhost:8099/api/v1/admin/market-data/resubscribe

# 상태 확인
curl http://localhost:8099/api/v1/admin/market-data/status
```

### 문제: "Cannot remove all symbols" 에러

**원인**: 마지막 남은 종목을 삭제하려고 시도

**해결**:
```bash
# 먼저 새 종목을 추가한 후 기존 종목 삭제
curl -X POST http://localhost:8099/api/v1/admin/market-data/symbols \
  -H "Content-Type: application/json" \
  -d '{"symbols": ["새종목코드"]}'

curl -X DELETE http://localhost:8099/api/v1/admin/market-data/symbols \
  -H "Content-Type: application/json" \
  -d '{"symbols": ["기존종목코드"]}'
```

---

## 관련 문서

- [API_EXAMPLES.md](./API_EXAMPLES.md) - 전체 API 사용 예시
- [DEMO_SCENARIOS.md](./DEMO_SCENARIOS.md) - 데모 시나리오
- [md/docs/04_API_OPENAPI.md](./md/docs/04_API_OPENAPI.md) - OpenAPI 명세

---

**업데이트**: 2026-01-20
**버전**: 0.1.0-SNAPSHOT
