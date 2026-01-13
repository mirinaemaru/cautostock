# API Examples

이 문서는 백테스팅 시스템의 REST API 사용 예제를 제공합니다.

## 목차
1. [Demo API](#demo-api)
2. [Backtest API](#backtest-api)
3. [Admin API](#admin-api)
4. [Query API](#query-api)
5. [워크플로우 예제](#워크플로우-예제)

---

## Demo API

### 1. 데모 데이터 생성

**Endpoint**: `POST /api/v1/demo/backtest/generate-data`

**설명**: 2024년 1년간의 합성 시장 데이터를 생성합니다.
- `005930` (삼성전자): 추세장 (상승/하락 사이클)
- `000660` (SK하이닉스): 횡보장 (범위 내 진동)

**요청**:
```bash
curl -X POST http://localhost:8080/api/v1/demo/backtest/generate-data \
  -H "Content-Type: application/json"
```

**응답**:
```json
{
  "message": "Demo dataset generated successfully",
  "symbols": ["005930", "000660"],
  "period": "2024-01-01 to 2024-12-31",
  "pattern": {
    "005930": "Trending market (uptrend/downtrend cycles)",
    "000660": "Ranging market (oscillating)"
  }
}
```

---

### 2. MA Crossover 전략 백테스트

**Endpoint**: `POST /api/v1/demo/backtest/ma-crossover`

**설명**: 이동평균선 골든크로스/데드크로스 전략을 실행합니다.
- **Short MA**: 5일
- **Long MA**: 20일
- **종목**: 005930 (삼성전자)
- **기간**: 2024-01-01 ~ 2024-12-31
- **초기 자본**: 10,000,000 KRW

**요청**:
```bash
curl -X POST http://localhost:8080/api/v1/demo/backtest/ma-crossover \
  -H "Content-Type: application/json"
```

**응답**:
```json
{
  "backtestId": "01JGSV7X8P9Q2R3S4T5U6V7W8X",
  "strategyId": "MA_CROSS_5_20",
  "symbol": "005930",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "initialCapital": 10000000,
  "finalCapital": 11245000,
  "totalReturn": 12.45,
  "annualReturn": 12.45,
  "performanceMetrics": {
    "sharpeRatio": 1.23,
    "sortinoRatio": 1.67,
    "maxDrawdown": -8.76,
    "maxDrawdownDuration": 0,
    "totalTrades": 24,
    "winningTrades": 14,
    "losingTrades": 10,
    "winRate": 58.33,
    "profitFactor": 1.87,
    "avgWin": 89642.85714286,
    "avgLoss": 47916.66666667,
    "avgTrade": 51875.00,
    "largestWin": 245000,
    "largestLoss": 135000,
    "totalProfit": 1255000,
    "totalLoss": 670000,
    "maxConsecutiveWins": 4,
    "maxConsecutiveLosses": 3
  },
  "riskMetrics": {
    "volatility": 2.45,
    "downsideDeviation": 1.87,
    "var95": -3.21,
    "cvar95": -4.56,
    "calmarRatio": 1.42,
    "recoveryFactor": 1.42
  },
  "trades": [
    {
      "tradeId": "01JGSV7X8P9Q2R3S4T5U6V7W8Y",
      "symbol": "005930",
      "side": "BUY",
      "entryTime": "2024-01-15T09:00:00",
      "entryPrice": 70000,
      "entryQty": 14,
      "exitTime": "2024-01-28T15:30:00",
      "exitPrice": 72500,
      "exitQty": 14,
      "grossPnl": 35000,
      "commission": 303.75,
      "netPnl": 34696.25,
      "returnPct": 3.53,
      "status": "CLOSED"
    }
    // ... more trades
  ],
  "equityCurve": [
    {
      "timestamp": "2024-01-01T09:00:00",
      "equity": 10000000
    },
    {
      "timestamp": "2024-01-15T10:00:00",
      "equity": 10034696
    }
    // ... more points
  ]
}
```

---

### 3. RSI 전략 백테스트

**Endpoint**: `POST /api/v1/demo/backtest/rsi`

**설명**: RSI 과매수/과매도 전략을 실행합니다.
- **RSI Period**: 14일
- **Overbought**: 70
- **Oversold**: 30
- **종목**: 000660 (SK하이닉스)
- **기간**: 2024-01-01 ~ 2024-12-31
- **초기 자본**: 10,000,000 KRW

**요청**:
```bash
curl -X POST http://localhost:8080/api/v1/demo/backtest/rsi \
  -H "Content-Type: application/json"
```

**응답**:
```json
{
  "backtestId": "01JGSV8Y9Q0R1S2T3U4V5W6X7Y",
  "strategyId": "RSI_14_30_70",
  "symbol": "000660",
  "totalReturn": 8.76,
  "winRate": 65.00,
  "profitFactor": 2.15,
  "totalTrades": 40
  // ... more metrics
}
```

---

### 4. 전략 비교

**Endpoint**: `POST /api/v1/demo/backtest/compare`

**설명**: 동일한 데이터(005930)에 대해 MA Crossover와 RSI 전략을 비교합니다.

**요청**:
```bash
curl -X POST http://localhost:8080/api/v1/demo/backtest/compare \
  -H "Content-Type: application/json"
```

**응답**:
```json
{
  "MA_Crossover": {
    "strategyId": "MA_CROSS_5_20",
    "totalReturn": 12.45,
    "sharpeRatio": 1.23,
    "maxDrawdown": -8.76,
    "totalTrades": 24,
    "winRate": 58.33
  },
  "RSI": {
    "strategyId": "RSI_14_30_70",
    "totalReturn": 9.87,
    "sharpeRatio": 1.05,
    "maxDrawdown": -6.54,
    "totalTrades": 38,
    "winRate": 63.16
  }
}
```

**해석**:
- MA Crossover: 더 높은 수익률 (12.45% vs 9.87%)
- RSI: 더 높은 승률 (63.16% vs 58.33%)
- MA Crossover: 더 적은 거래 (24 vs 38) → 거래 비용 절감

---

### 5. 데모 데이터 삭제

**Endpoint**: `DELETE /api/v1/demo/backtest/clear`

**설명**: 생성된 모든 데모 데이터를 삭제합니다.

**요청**:
```bash
curl -X DELETE http://localhost:8080/api/v1/demo/backtest/clear
```

**응답**:
```json
{
  "message": "Demo backtest data cleared successfully"
}
```

---

## Backtest API

### 1. 커스텀 백테스트 실행

**Endpoint**: `POST /api/v1/backtest/run`

**설명**: 사용자 정의 파라미터로 백테스트를 실행합니다.

**요청**:
```bash
curl -X POST http://localhost:8080/api/v1/backtest/run \
  -H "Content-Type: application/json" \
  -d '{
    "strategyId": "MA_CROSS_10_30",
    "symbols": ["005930"],
    "startDate": "2024-01-01",
    "endDate": "2024-12-31",
    "timeframe": "1d",
    "initialCapital": 20000000,
    "commission": 0.0015,
    "slippage": 0.0005,
    "strategyParams": {
      "shortPeriod": 10,
      "longPeriod": 30
    }
  }'
```

**응답**: (Demo API와 동일한 형식)

---

### 2. 백테스트 결과 조회

**Endpoint**: `GET /api/v1/backtest/results/{backtestId}`

**요청**:
```bash
curl -X GET http://localhost:8080/api/v1/backtest/results/01JGSV7X8P9Q2R3S4T5U6V7W8X
```

**응답**:
```json
{
  "backtestId": "01JGSV7X8P9Q2R3S4T5U6V7W8X",
  "strategyId": "MA_CROSS_5_20",
  "status": "COMPLETED",
  "totalReturn": 12.45,
  "createdAt": "2026-01-01T09:30:00"
  // ... full result
}
```

---

### 3. 백테스트 목록 조회

**Endpoint**: `GET /api/v1/backtest/results`

**요청**:
```bash
# 최근 10개 결과
curl -X GET "http://localhost:8080/api/v1/backtest/results?limit=10&offset=0"

# 특정 전략만 필터링
curl -X GET "http://localhost:8080/api/v1/backtest/results?strategyId=MA_CROSS_5_20"

# 날짜 범위 필터링
curl -X GET "http://localhost:8080/api/v1/backtest/results?startDate=2024-01-01&endDate=2024-12-31"
```

**응답**:
```json
{
  "results": [
    {
      "backtestId": "01JGSV7X...",
      "strategyId": "MA_CROSS_5_20",
      "totalReturn": 12.45,
      "createdAt": "2026-01-01T09:30:00"
    },
    {
      "backtestId": "01JGSV8Y...",
      "strategyId": "RSI_14_30_70",
      "totalReturn": 8.76,
      "createdAt": "2026-01-01T09:25:00"
    }
  ],
  "total": 2,
  "limit": 10,
  "offset": 0
}
```

---

### 4. 거래 내역 조회

**Endpoint**: `GET /api/v1/backtest/results/{backtestId}/trades`

**요청**:
```bash
curl -X GET http://localhost:8080/api/v1/backtest/results/01JGSV7X8P9Q2R3S4T5U6V7W8X/trades
```

**응답**:
```json
{
  "backtestId": "01JGSV7X8P9Q2R3S4T5U6V7W8X",
  "trades": [
    {
      "tradeId": "01JGSV7X8P9Q2R3S4T5U6V7W8Y",
      "symbol": "005930",
      "side": "BUY",
      "entryTime": "2024-01-15T09:00:00",
      "entryPrice": 70000,
      "entryQty": 14,
      "exitTime": "2024-01-28T15:30:00",
      "exitPrice": 72500,
      "exitQty": 14,
      "netPnl": 34696.25,
      "returnPct": 3.53
    }
    // ... more trades
  ],
  "totalTrades": 24
}
```

---

### 5. 성과 곡선 조회

**Endpoint**: `GET /api/v1/backtest/results/{backtestId}/equity-curve`

**요청**:
```bash
curl -X GET http://localhost:8080/api/v1/backtest/results/01JGSV7X8P9Q2R3S4T5U6V7W8X/equity-curve
```

**응답**:
```json
{
  "backtestId": "01JGSV7X8P9Q2R3S4T5U6V7W8X",
  "equityCurve": [
    {
      "timestamp": "2024-01-01T09:00:00",
      "equity": 10000000,
      "drawdown": 0.00
    },
    {
      "timestamp": "2024-01-15T10:00:00",
      "equity": 10034696,
      "drawdown": 0.00
    },
    {
      "timestamp": "2024-01-28T15:30:00",
      "equity": 9987420,
      "drawdown": -0.47
    }
    // ... more points
  ],
  "points": 25
}
```

---

## Admin API

### 1. 백테스트 삭제

**Endpoint**: `DELETE /api/v1/admin/backtest/{backtestId}`

**요청**:
```bash
curl -X DELETE http://localhost:8080/api/v1/admin/backtest/01JGSV7X8P9Q2R3S4T5U6V7W8X \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

**응답**:
```json
{
  "message": "Backtest deleted successfully",
  "backtestId": "01JGSV7X8P9Q2R3S4T5U6V7W8X"
}
```

---

### 2. 전략 활성화/비활성화

**Endpoint**: `PUT /api/v1/admin/strategies/{strategyId}/enabled`

**요청**:
```bash
# 활성화
curl -X PUT http://localhost:8080/api/v1/admin/strategies/MA_CROSS_5_20/enabled \
  -H "Content-Type: application/json" \
  -d '{"enabled": true}'

# 비활성화
curl -X PUT http://localhost:8080/api/v1/admin/strategies/MA_CROSS_5_20/enabled \
  -H "Content-Type: application/json" \
  -d '{"enabled": false}'
```

**응답**:
```json
{
  "strategyId": "MA_CROSS_5_20",
  "enabled": true,
  "message": "Strategy enabled successfully"
}
```

---

## Query API

### 1. 전략 성과 요약

**Endpoint**: `GET /api/v1/query/strategies/{strategyId}/performance`

**요청**:
```bash
curl -X GET "http://localhost:8080/api/v1/query/strategies/MA_CROSS_5_20/performance?period=1Y"
```

**응답**:
```json
{
  "strategyId": "MA_CROSS_5_20",
  "period": "1Y",
  "backtestsRun": 15,
  "avgTotalReturn": 11.23,
  "avgSharpeRatio": 1.15,
  "avgMaxDrawdown": -9.45,
  "bestBacktest": {
    "backtestId": "01JGSV7X...",
    "totalReturn": 18.76
  },
  "worstBacktest": {
    "backtestId": "01JGSV9Z...",
    "totalReturn": 4.32
  }
}
```

---

### 2. 거래 통계

**Endpoint**: `GET /api/v1/query/strategies/{strategyId}/trade-stats`

**요청**:
```bash
curl -X GET http://localhost:8080/api/v1/query/strategies/MA_CROSS_5_20/trade-stats
```

**응답**:
```json
{
  "strategyId": "MA_CROSS_5_20",
  "totalTrades": 360,
  "avgTradesPerBacktest": 24,
  "avgWinRate": 58.5,
  "avgProfitFactor": 1.89,
  "avgHoldingPeriodDays": 8.5,
  "avgTradeSize": 980000
}
```

---

## 워크플로우 예제

### 시나리오 1: 첫 백테스트 실행

```bash
# 1. 데모 데이터 생성
curl -X POST http://localhost:8080/api/v1/demo/backtest/generate-data

# 2. MA Crossover 백테스트 실행
curl -X POST http://localhost:8080/api/v1/demo/backtest/ma-crossover

# 3. RSI 백테스트 실행
curl -X POST http://localhost:8080/api/v1/demo/backtest/rsi

# 4. 결과 비교
curl -X POST http://localhost:8080/api/v1/demo/backtest/compare
```

---

### 시나리오 2: 파라미터 최적화

```bash
# MA 파라미터 조합 테스트
for short in 5 10 15; do
  for long in 20 30 50; do
    curl -X POST http://localhost:8080/api/v1/backtest/run \
      -H "Content-Type: application/json" \
      -d "{
        \"strategyId\": \"MA_CROSS_${short}_${long}\",
        \"symbols\": [\"005930\"],
        \"startDate\": \"2024-01-01\",
        \"endDate\": \"2024-12-31\",
        \"timeframe\": \"1d\",
        \"initialCapital\": 10000000,
        \"commission\": 0.0015,
        \"slippage\": 0.0005,
        \"strategyParams\": {
          \"shortPeriod\": $short,
          \"longPeriod\": $long
        }
      }"
    sleep 1
  done
done

# 결과 조회 및 비교
curl -X GET "http://localhost:8080/api/v1/backtest/results?limit=9&offset=0"
```

---

### 시나리오 3: 성과 분석

```bash
# 1. 백테스트 실행
RESULT=$(curl -s -X POST http://localhost:8080/api/v1/demo/backtest/ma-crossover)

# 2. backtestId 추출
BACKTEST_ID=$(echo $RESULT | jq -r '.backtestId')

# 3. 상세 거래 내역 조회
curl -X GET "http://localhost:8080/api/v1/backtest/results/${BACKTEST_ID}/trades" | jq '.'

# 4. 성과 곡선 조회
curl -X GET "http://localhost:8080/api/v1/backtest/results/${BACKTEST_ID}/equity-curve" | jq '.'

# 5. 결과 저장
curl -X GET "http://localhost:8080/api/v1/backtest/results/${BACKTEST_ID}" > backtest_result.json
```

---

### 시나리오 4: 정리 및 재시작

```bash
# 1. 오래된 백테스트 삭제 (Admin)
curl -X DELETE http://localhost:8080/api/v1/admin/backtest/old-backtest-id

# 2. 데모 데이터 삭제
curl -X DELETE http://localhost:8080/api/v1/demo/backtest/clear

# 3. 새 데이터 생성 및 재실행
curl -X POST http://localhost:8080/api/v1/demo/backtest/generate-data
curl -X POST http://localhost:8080/api/v1/demo/backtest/ma-crossover
```

---

## 에러 응답

### 400 Bad Request

```json
{
  "timestamp": "2026-01-01T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid date range: startDate must be before endDate",
  "path": "/api/v1/backtest/run"
}
```

### 404 Not Found

```json
{
  "timestamp": "2026-01-01T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Backtest not found: 01JGSV7X8P9Q2R3S4T5U6V7W8X",
  "path": "/api/v1/backtest/results/01JGSV7X8P9Q2R3S4T5U6V7W8X"
}
```

### 500 Internal Server Error

```json
{
  "timestamp": "2026-01-01T10:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Backtest execution failed: Insufficient historical data",
  "path": "/api/v1/backtest/run"
}
```

---

## 참고 사항

### 제한사항
- 최대 백테스트 실행 시간: **30초**
- 최대 거래 내역 반환: **1000개**
- 최대 성과 곡선 포인트: **10000개**

### 권장사항
- 데모 API는 **개발/테스트 환경**에서만 사용
- 프로덕션에서는 **실제 시장 데이터** 사용
- 대량 백테스트는 **배치 작업**으로 수행
- 결과는 **정기적으로 백업**

---

**작성일**: 2026-01-01
**버전**: 1.0
**작성자**: Claude Sonnet 4.5
