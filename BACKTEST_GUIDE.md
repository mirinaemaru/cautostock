# Backtest Guide

## 목차
1. [백테스팅이란?](#백테스팅이란)
2. [빠른 시작](#빠른-시작)
3. [전략 개발 가이드](#전략-개발-가이드)
4. [결과 해석](#결과-해석)
5. [베스트 프랙티스](#베스트-프랙티스)
6. [고급 기능](#고급-기능)
7. [FAQ](#faq)

---

## 백테스팅이란?

백테스팅(Backtesting)은 **과거 시장 데이터를 사용하여 트레이딩 전략의 성과를 시뮬레이션**하는 프로세스입니다.

### 왜 백테스팅이 중요한가?

- ✅ **실제 자본 리스크 없이** 전략 검증
- ✅ **객관적인 성과 지표**로 전략 비교
- ✅ **최적 파라미터** 탐색
- ✅ **리스크 특성** 이해 (최대 낙폭, 변동성 등)

### 주의사항 ⚠️

백테스팅은 **과거 데이터 기반**이므로:
- ❌ 미래 수익을 보장하지 않음
- ❌ 과최적화(Overfitting) 위험
- ❌ 실제 거래와 차이 존재 (슬리피지, 체결 지연 등)

---

## 빠른 시작

### 1️⃣ 데모 데이터 생성

```bash
curl -X POST http://localhost:8080/api/v1/demo/backtest/generate-data
```

**응답 예시**:
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

### 2️⃣ MA Crossover 전략 실행

```bash
curl -X POST http://localhost:8080/api/v1/demo/backtest/ma-crossover
```

### 3️⃣ 결과 확인

```json
{
  "backtestId": "01JGSV...",
  "strategyId": "MA_CROSS_5_20",
  "totalReturn": 12.45,
  "annualReturn": 12.45,
  "sharpeRatio": 1.23,
  "maxDrawdown": -8.76,
  "totalTrades": 24,
  "winRate": 58.33,
  "profitFactor": 1.87
}
```

---

## 전략 개발 가이드

### 단계 1: 전략 클래스 생성

```java
package maru.trading.domain.backtest;

public class MyCustomStrategy implements StrategyEngine {

    @Override
    public SignalDecision evaluate(String strategyId,
                                   List<Bar> recentBars,
                                   Map<String, Object> params) {
        // 전략 로직 구현

        if (buyCondition) {
            return SignalDecision.createBuy("My reason");
        } else if (sellCondition) {
            return SignalDecision.createSell("My reason");
        } else {
            return SignalDecision.createHold();
        }
    }
}
```

### 단계 2: 파라미터 정의

```java
Map<String, Object> params = new HashMap<>();
params.put("myParam1", 14);
params.put("myParam2", 70);
params.put("myParam3", 30);
```

### 단계 3: 백테스트 설정

```java
BacktestConfig config = BacktestConfig.builder()
    .backtestId(UlidGenerator.generate())
    .strategyId("MY_STRATEGY")
    .symbols(List.of("005930"))
    .startDate(LocalDate.of(2024, 1, 1))
    .endDate(LocalDate.of(2024, 12, 31))
    .timeframe("1d")
    .initialCapital(BigDecimal.valueOf(10_000_000))
    .commission(BigDecimal.valueOf(0.0015))      // 0.15%
    .slippage(BigDecimal.valueOf(0.0005))        // 0.05%
    .strategyParams(params)
    .build();
```

### 단계 4: 실행 및 분석

```java
BacktestResult result = backtestEngine.run(config);
PerformanceMetrics metrics = result.getPerformanceMetrics();

System.out.println("Total Return: " + metrics.getTotalReturn() + "%");
System.out.println("Sharpe Ratio: " + metrics.getSharpeRatio());
System.out.println("Max Drawdown: " + metrics.getMaxDrawdown() + "%");
```

---

## 결과 해석

### 수익성 지표

| 지표 | 의미 | 좋은 값 |
|------|------|---------|
| **Total Return** | 총 수익률 (%) | > 10% |
| **Annual Return** | 연환산 수익률 (%) | > 15% |
| **Profit Factor** | 총이익 / 총손실 | > 1.5 |
| **Win Rate** | 승률 (%) | > 50% |

### 리스크 지표

| 지표 | 의미 | 좋은 값 |
|------|------|---------|
| **Sharpe Ratio** | 위험 대비 수익 | > 1.0 |
| **Sortino Ratio** | 하방 위험 대비 수익 | > 1.5 |
| **Max Drawdown** | 최대 낙폭 (%) | < -20% |
| **Volatility** | 수익률 변동성 | 낮을수록 안정적 |

### 거래 지표

| 지표 | 의미 | 좋은 값 |
|------|------|---------|
| **Total Trades** | 총 거래 횟수 | 30-100 (과도한 거래 주의) |
| **Avg Win** | 평균 이익 | > Avg Loss |
| **Avg Loss** | 평균 손실 | 작을수록 좋음 |
| **Max Consecutive Losses** | 최대 연속 손실 | < 5 |

### 예시 분석

```json
{
  "totalReturn": 18.50,        // ✅ 좋음 (> 10%)
  "sharpeRatio": 1.45,         // ✅ 좋음 (> 1.0)
  "maxDrawdown": -12.30,       // ✅ 양호 (< -20%)
  "winRate": 62.50,            // ✅ 우수 (> 50%)
  "profitFactor": 2.15,        // ✅ 우수 (> 1.5)
  "totalTrades": 48,           // ✅ 적정
  "maxConsecutiveLosses": 4    // ✅ 양호 (< 5)
}
```

**종합 평가**: 이 전략은 **수익성**, **안정성**, **거래 빈도** 모두 우수한 편입니다.

---

## 베스트 프랙티스

### 1. 충분한 데이터 기간 사용

```java
// ❌ 나쁨: 너무 짧은 기간
.startDate(LocalDate.of(2024, 11, 1))
.endDate(LocalDate.of(2024, 12, 31))  // 2개월

// ✅ 좋음: 최소 1년 이상
.startDate(LocalDate.of(2024, 1, 1))
.endDate(LocalDate.of(2024, 12, 31))  // 1년
```

### 2. 현실적인 수수료/슬리피지 설정

```java
// ❌ 나쁨: 수수료 무시
.commission(BigDecimal.ZERO)
.slippage(BigDecimal.ZERO)

// ✅ 좋음: 현실적 설정
.commission(BigDecimal.valueOf(0.0015))  // 0.15% (KRX 일반)
.slippage(BigDecimal.valueOf(0.0005))    // 0.05%
```

### 3. 과최적화 방지

```java
// ❌ 나쁨: 파라미터 100개 조합 테스트 → 가장 좋은 것만 선택
for (int p1 = 1; p1 <= 50; p1++) {
    for (int p2 = 1; p2 <= 50; p2++) {
        // 2500개 조합 → 과최적화 위험!
    }
}

// ✅ 좋음: 합리적 범위 + Out-of-Sample 검증
// In-Sample: 2024-01-01 ~ 2024-06-30
// Out-of-Sample: 2024-07-01 ~ 2024-12-31
```

### 4. 다양한 시장 환경 테스트

```java
// ✅ 상승장, 하락장, 횡보장 모두 테스트
generateTrendingMarketData("005930", startDate, endDate);  // 추세장
generateRangingMarketData("000660", startDate, endDate);   // 횡보장
```

### 5. 리스크 관리 포함

```java
// ✅ 손절/익절 로직 포함
if (currentLoss > maxLossPerTrade) {
    return SignalDecision.createSell("Stop Loss");
}
if (currentProfit > targetProfit) {
    return SignalDecision.createSell("Take Profit");
}
```

---

## 고급 기능

### 1. 전략 비교 백테스팅

```bash
curl -X POST http://localhost:8080/api/v1/demo/backtest/compare
```

**응답**:
```json
{
  "MA_Crossover": {
    "totalReturn": 12.45,
    "sharpeRatio": 1.23,
    "totalTrades": 24
  },
  "RSI": {
    "totalReturn": 8.76,
    "sharpeRatio": 0.98,
    "totalTrades": 38
  }
}
```

### 2. 커스텀 전략 REST API

**POST /api/v1/backtest/run**

```json
{
  "strategyId": "MY_STRATEGY",
  "symbols": ["005930"],
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "timeframe": "1d",
  "initialCapital": 10000000,
  "commission": 0.0015,
  "slippage": 0.0005,
  "strategyParams": {
    "param1": 14,
    "param2": 70
  }
}
```

### 3. 성과 곡선 생성

```java
EquityCurve curve = performanceAnalyzer.generateEquityCurve(result);
List<EquityPoint> points = curve.getPoints();

for (EquityPoint point : points) {
    System.out.println(point.getTimestamp() + ": " + point.getEquity());
}
```

**출력 예시**:
```
2024-01-01T09:00: 10000000
2024-01-15T10:00: 10050000
2024-01-16T10:00: 9970000
2024-01-17T10:00: 10120000
...
```

### 4. 리스크 지표 분석

```java
RiskMetrics riskMetrics = performanceAnalyzer.analyzeRisk(result);

System.out.println("Volatility: " + riskMetrics.getVolatility());
System.out.println("VaR (95%): " + riskMetrics.getVar95());
System.out.println("CVaR (95%): " + riskMetrics.getCvar95());
System.out.println("Calmar Ratio: " + riskMetrics.getCalmarRatio());
```

---

## FAQ

### Q1. 백테스팅에 얼마나 많은 데이터가 필요한가요?

**A**: 최소 **1년 이상**의 데이터를 권장합니다. 이상적으로는 **2-3년** 이상의 데이터로 다양한 시장 환경을 커버하는 것이 좋습니다.

### Q2. Sharpe Ratio가 음수인데 괜찮나요?

**A**: ❌ **음수 Sharpe Ratio**는 **무위험 수익률보다 낮은 수익**을 의미합니다. 전략을 재검토해야 합니다.

### Q3. Win Rate가 낮아도 수익성이 좋을 수 있나요?

**A**: ✅ 가능합니다. **Win Rate 40%**이지만 **Profit Factor > 2.0**이면 "적게 이기고 크게 따는" 전략입니다.

### Q4. 과최적화를 어떻게 방지하나요?

**A**:
1. **Out-of-Sample 검증** - 데이터를 Train/Test로 분리
2. **Walk-Forward 분석** - 롤링 윈도우로 재검증
3. **파라미터 단순화** - 복잡한 전략일수록 과최적화 위험
4. **다양한 종목 테스트** - 여러 종목에서 일관된 성과 확인

### Q5. 실제 거래와 백테스트 결과가 다른 이유는?

**A**:
- **슬리피지** - 백테스트에서 설정한 것보다 클 수 있음
- **체결 지연** - 실시간 체결은 지연 발생
- **유동성** - 대량 주문 시 가격 영향
- **시장 변화** - 과거와 현재 시장 구조 차이

### Q6. 데모 데이터는 얼마나 현실적인가요?

**A**: 데모 데이터는 **교육 목적**으로 생성된 합성 데이터입니다. 실제 거래를 위해서는 **실제 과거 데이터**를 사용하세요.

### Q7. 여러 종목을 동시에 백테스트할 수 있나요?

**A**: 현재는 **단일 종목**만 지원합니다. 포트폴리오 백테스팅은 향후 추가 예정입니다.

### Q8. 백테스트 결과를 어떻게 저장하나요?

**A**: 모든 백테스트 결과는 자동으로 `backtest_results`, `backtest_trades` 테이블에 저장됩니다.

```sql
SELECT * FROM backtest_results ORDER BY created_at DESC LIMIT 10;
```

---

## 관련 문서

- [PHASE4_COMPLETE.md](PHASE4_COMPLETE.md) - Phase 4 구현 상세
- [API_EXAMPLES.md](API_EXAMPLES.md) - API 사용 예제
- [TEST_SCENARIOS.md](TEST_SCENARIOS.md) - 테스트 시나리오
- `/md/docs/04_API_OPENAPI.md` - OpenAPI 명세

---

**작성일**: 2026-01-01
**버전**: 1.0
**작성자**: Claude Sonnet 4.5
