# Phase 4 Complete: 백테스팅 엔진

**완료일**: 2026-01-01
**상태**: ✅ 100% 구현 완료
**테스트**: 18 tests (100% 통과)

---

## 📊 개요

Phase 4에서는 트레이딩 전략을 과거 데이터로 검증할 수 있는 **백테스팅 엔진**을 구현했습니다.

### 주요 기능

✅ **히스토리컬 데이터 관리** - OHLCV 바 데이터 저장 및 조회
✅ **백테스트 실행** - 과거 데이터 기반 전략 시뮬레이션
✅ **가상 브로커** - 주문 체결 시뮬레이션 (수수료/슬리피지 적용)
✅ **성능 분석** - 15개 성능 지표 + 10개 리스크 지표
✅ **REST API** - 백테스트 CRUD 및 조회
✅ **데모 API** - 샘플 데이터 생성 및 원클릭 백테스트

---

## 🏗️ 아키텍처

### 백테스팅 파이프라인

```
1. DataReplayEngine
   - 히스토리컬 데이터 로드
   - Iterator 패턴으로 순차 재생
   - Lookahead Bias 방지

2. StrategyEngine
   - 바 데이터 입력
   - 지표 계산 (MA, RSI 등)
   - 신호 생성 (BUY/SELL/HOLD)

3. VirtualBroker
   - 주문 접수
   - 시장가/지정가 체결 시뮬레이션
   - 수수료 0.15% + 슬리피지 0.05% 적용

4. PerformanceAnalyzer
   - 트레이드 집계
   - 성능 메트릭 계산
   - 리스크 메트릭 계산
   - 자산 곡선 생성
```

### 핵심 컴포넌트

#### 1. Database Layer

**HistoricalBarEntity** - 히스토리컬 OHLCV 데이터
```sql
CREATE TABLE historical_bars (
    bar_id CHAR(26) PRIMARY KEY,
    symbol VARCHAR(16) NOT NULL,
    timeframe VARCHAR(8) NOT NULL,
    bar_timestamp DATETIME(3) NOT NULL,
    open_price DECIMAL(18,4),
    high_price DECIMAL(18,4),
    low_price DECIMAL(18,4),
    close_price DECIMAL(18,4),
    volume BIGINT,
    UNIQUE INDEX uk_symbol_timeframe_timestamp (symbol, timeframe, bar_timestamp)
);
```

**BacktestRunEntity** - 백테스트 실행 메타데이터
```sql
CREATE TABLE backtest_runs (
    backtest_id CHAR(26) PRIMARY KEY,
    strategy_id CHAR(26) NOT NULL,
    symbols TEXT NOT NULL,
    start_date DATETIME(3),
    end_date DATETIME(3),
    initial_capital DECIMAL(18,2),
    final_capital DECIMAL(18,2),
    total_return DECIMAL(10,6),
    total_trades INTEGER,
    status VARCHAR(16),
    performance_metrics JSON,
    risk_metrics JSON
);
```

**BacktestTradeEntity** - 백테스트 트레이드 기록
```sql
CREATE TABLE backtest_trades (
    trade_id CHAR(26) PRIMARY KEY,
    backtest_id CHAR(26) NOT NULL,
    symbol VARCHAR(16),
    side VARCHAR(8),
    entry_time DATETIME(3),
    entry_price DECIMAL(18,4),
    exit_time DATETIME(3),
    exit_price DECIMAL(18,4),
    net_pnl DECIMAL(18,4),
    return_pct DECIMAL(10,6)
);
```

#### 2. Domain Layer

**BacktestConfig** - 백테스트 설정
```java
BacktestConfig config = BacktestConfig.builder()
    .backtestId("01...")
    .strategyId("MA_CROSS_5_20")
    .symbols(List.of("005930"))
    .startDate(LocalDate.of(2024, 1, 1))
    .endDate(LocalDate.of(2024, 12, 31))
    .timeframe("1d")
    .initialCapital(BigDecimal.valueOf(10_000_000))
    .commission(BigDecimal.valueOf(0.0015))  // 0.15%
    .slippage(BigDecimal.valueOf(0.0005))    // 0.05%
    .build();
```

**PerformanceMetrics** - 15개 성능 지표
- Total Return (%)
- Annual Return (%)
- Sharpe Ratio
- Sortino Ratio
- Max Drawdown (%)
- Total Trades
- Win Rate (%)
- Profit Factor
- Avg Win / Avg Loss
- Largest Win / Largest Loss
- Max Consecutive Wins / Losses

**RiskMetrics** - 10개 리스크 지표
- Volatility
- Downside Deviation
- VaR (95%)
- CVaR (95%)
- Calmar Ratio
- Recovery Factor

#### 3. Implementation Layer

**BacktestEngineImpl** - 메인 오케스트레이터
```java
@Service
public class BacktestEngineImpl implements BacktestEngine {
    public BacktestResult run(BacktestConfig config) {
        // 1. 데이터 로드
        dataReplayEngine.loadData(config);

        // 2. 가상 브로커 초기화
        virtualBroker.reset(config.getInitialCapital());

        // 3. 바 단위로 전략 실행
        while (dataReplayEngine.hasNext()) {
            HistoricalBarEntity bar = dataReplayEngine.next();

            // 전략 평가
            SignalDecision decision = strategy.evaluate(context);

            // 주문 생성 및 제출
            if (decision.isTradeable()) {
                Order order = createOrder(decision);
                virtualBroker.submitOrder(order);
            }

            // 체결 처리
            List<Fill> fills = virtualBroker.processBar(bar);
            processFills(fills);
        }

        // 4. 성능 분석
        PerformanceMetrics metrics = performanceAnalyzer.analyze(result);

        return result;
    }
}
```

**VirtualBrokerImpl** - 주문 체결 시뮬레이션
```java
@Component
public class VirtualBrokerImpl implements VirtualBroker {
    private BigDecimal commission = BigDecimal.valueOf(0.0015);
    private BigDecimal slippage = BigDecimal.valueOf(0.0005);

    @Override
    public List<Fill> processBar(HistoricalBarEntity bar) {
        for (Order order : pendingOrders.values()) {
            Fill fill = tryFillOrder(order, bar);
            if (fill != null) {
                // 체결 성공
                updateCashBalance(fill);
                newFills.add(fill);
            }
        }
        return newFills;
    }

    private Fill tryFillOrder(Order order, HistoricalBarEntity bar) {
        BigDecimal fillPrice = null;

        if (order.getOrderType() == MARKET) {
            fillPrice = bar.getOpenPrice();
        } else if (order.getOrderType() == LIMIT) {
            if (order.getSide() == BUY && bar.getLowPrice() <= order.getPrice()) {
                fillPrice = order.getPrice();
            }
        }

        // 슬리피지 적용
        if (order.getSide() == BUY) {
            fillPrice = fillPrice.multiply(BigDecimal.valueOf(1.0005));
        } else {
            fillPrice = fillPrice.multiply(BigDecimal.valueOf(0.9995));
        }

        return createFill(order, fillPrice);
    }
}
```

**PerformanceAnalyzerImpl** - 메트릭 계산
```java
@Component
public class PerformanceAnalyzerImpl implements PerformanceAnalyzer {
    @Override
    public PerformanceMetrics analyze(BacktestResult result) {
        // 수익률 계산
        BigDecimal totalReturn = calculateTotalReturn(result);
        BigDecimal annualReturn = calculateAnnualReturn(result);

        // 트레이드 통계
        int totalTrades = trades.size();
        int winningTrades = trades.stream().filter(BacktestTrade::isWinner).count();
        BigDecimal winRate = (winningTrades / totalTrades) * 100;

        // Sharpe Ratio
        BigDecimal sharpeRatio = (annualReturn - riskFreeRate) / volatility;

        // Max Drawdown
        BigDecimal maxDrawdown = calculateMaxDrawdown(result);

        return PerformanceMetrics.builder()
            .totalReturn(totalReturn)
            .sharpeRatio(sharpeRatio)
            .maxDrawdown(maxDrawdown)
            // ... 기타 메트릭
            .build();
    }
}
```

---

## 📡 REST API

### 1. 백테스트 실행

```bash
POST /api/v1/admin/backtests

{
  "strategyId": "MA_CROSS_5_20",
  "symbols": ["005930"],
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "timeframe": "1d",
  "initialCapital": 10000000,
  "commission": 0.0015,
  "slippage": 0.0005,
  "strategyParams": {
    "shortPeriod": 5,
    "longPeriod": 20
  }
}
```

**응답:**
```json
{
  "backtestId": "01...",
  "strategyId": "MA_CROSS_5_20",
  "symbols": ["005930"],
  "initialCapital": 10000000,
  "finalCapital": 11500000,
  "totalReturn": 15.0,
  "totalTrades": 45,
  "performance": {
    "winRate": 62.5,
    "sharpeRatio": 1.85,
    "maxDrawdown": -8.5,
    "profitFactor": 2.3
  }
}
```

### 2. 백테스트 목록 조회

```bash
GET /api/v1/admin/backtests
```

### 3. 백테스트 상세 조회

```bash
GET /api/v1/admin/backtests/{backtestId}
```

### 4. 백테스트 트레이드 조회

```bash
GET /api/v1/admin/backtests/{backtestId}/trades
```

### 5. 백테스트 삭제

```bash
DELETE /api/v1/admin/backtests/{backtestId}
```

---

## 🎮 데모 API

### 1. 샘플 데이터 생성

```bash
POST /api/v1/demo/backtest/generate-data
```

**생성되는 데이터:**
- **005930 (삼성전자)**: 트렌딩 마켓 (상승/하락 사이클)
- **000660 (SK하이닉스)**: 레인징 마켓 (횡보장)
- **기간**: 2024년 1월 1일 ~ 12월 31일
- **타임프레임**: 1일봉

### 2. MA Crossover 데모 실행

```bash
POST /api/v1/demo/backtest/ma-crossover
```

MA(5) / MA(20) 골든크로스/데드크로스 전략 백테스트

### 3. RSI 데모 실행

```bash
POST /api/v1/demo/backtest/rsi
```

RSI(14) 과매수(70) / 과매도(30) 전략 백테스트

### 4. 전략 비교 데모

```bash
POST /api/v1/demo/backtest/compare
```

동일 데이터에 대한 MA Crossover vs RSI 전략 비교

### 5. 데이터 삭제

```bash
DELETE /api/v1/demo/backtest/clear
```

---

## 🧪 테스트

### VirtualBrokerImplTest (9 tests)

- ✅ 시장가 매수 주문 체결 (슬리피지 적용)
- ✅ 시장가 매도 주문 체결 (슬리피지 적용)
- ✅ 지정가 매수 주문 체결 (조건 충족 시)
- ✅ 지정가 매수 주문 미체결 (조건 미충족 시)
- ✅ 지정가 매도 주문 체결
- ✅ 멀티 심볼 주문 처리
- ✅ 체결 이력 추적
- ✅ 주문 취소
- ✅ 상태 리셋

### PerformanceAnalyzerImplTest (9 tests)

- ✅ 총 수익률 계산
- ✅ 승률 계산
- ✅ Profit Factor 계산
- ✅ 평균 수익/손실 계산
- ✅ 최대 수익/손실 계산
- ✅ 연속 승/패 계산
- ✅ 빈 트레이드 리스트 처리
- ✅ 자산 곡선 생성
- ✅ 리스크 메트릭 계산

---

## 💡 사용 예제

### 예제 1: MA Crossover 전략 백테스트

```java
// 설정
BacktestConfig config = BacktestConfig.builder()
    .strategyId("MA_CROSS_5_20")
    .symbols(List.of("005930"))
    .startDate(LocalDate.of(2024, 1, 1))
    .endDate(LocalDate.of(2024, 12, 31))
    .timeframe("1d")
    .initialCapital(BigDecimal.valueOf(10_000_000))
    .strategyParams(Map.of("shortPeriod", 5, "longPeriod", 20))
    .build();

// 실행
BacktestResult result = backtestEngine.run(config);

// 결과 확인
System.out.println("Total Return: " + result.getTotalReturn() + "%");
System.out.println("Sharpe Ratio: " + result.getPerformanceMetrics().getSharpeRatio());
System.out.println("Max Drawdown: " + result.getPerformanceMetrics().getMaxDrawdown() + "%");
System.out.println("Win Rate: " + result.getPerformanceMetrics().getWinRate() + "%");
```

### 예제 2: 여러 파라미터 최적화

```java
for (int shortPeriod = 3; shortPeriod <= 10; shortPeriod++) {
    for (int longPeriod = 15; longPeriod <= 30; longPeriod += 5) {
        Map<String, Object> params = Map.of(
            "shortPeriod", shortPeriod,
            "longPeriod", longPeriod
        );

        BacktestConfig config = BacktestConfig.builder()
            .strategyId("MA_CROSS_" + shortPeriod + "_" + longPeriod)
            .strategyParams(params)
            // ... 기타 설정
            .build();

        BacktestResult result = backtestEngine.run(config);

        if (result.getPerformanceMetrics().getSharpeRatio() > bestSharpe) {
            bestParams = params;
            bestSharpe = result.getPerformanceMetrics().getSharpeRatio();
        }
    }
}

System.out.println("Best params: " + bestParams);
```

---

## 📈 성능 메트릭 설명

### Return Metrics

**Total Return (%)**: 전체 수익률
```
Total Return = (Final Capital - Initial Capital) / Initial Capital × 100
```

**Annual Return (%)**: 연간화 수익률
```
Annual Return = Total Return / Years
```

### Risk-Adjusted Returns

**Sharpe Ratio**: 위험 대비 수익률 (높을수록 좋음, >1.0 우수)
```
Sharpe Ratio = (Annual Return - Risk-Free Rate) / Volatility
```

**Sortino Ratio**: 하방 위험 대비 수익률
```
Sortino Ratio = (Annual Return - Risk-Free Rate) / Downside Deviation
```

**Calmar Ratio**: MDD 대비 수익률
```
Calmar Ratio = Annual Return / Max Drawdown
```

### Trade Statistics

**Win Rate (%)**: 승률
```
Win Rate = Winning Trades / Total Trades × 100
```

**Profit Factor**: 총이익/총손실 비율 (>1.0 필수, >2.0 우수)
```
Profit Factor = Total Profit / Total Loss
```

### Risk Metrics

**Max Drawdown (%)**: 최대 낙폭 (음수)
```
MDD = Max(Peak - Trough) / Peak × 100
```

**VaR (95%)**: 95% 신뢰구간 최대 손실
**CVaR (95%)**: VaR 초과 시 평균 손실

---

## 🔧 고급 기능

### 1. 커스텀 전략 구현

```java
@Component
public class MyCustomStrategy implements StrategyEngine {
    @Override
    public SignalDecision evaluate(StrategyContext context) {
        List<MarketBar> bars = context.getBars();

        // 커스텀 로직 구현
        if (/* 매수 조건 */) {
            return SignalDecision.buy(
                BigDecimal.valueOf(10),
                "Custom buy signal",
                300
            );
        }

        return SignalDecision.hold("No signal");
    }
}
```

### 2. 수수료/슬리피지 커스터마이징

```java
virtualBroker.setCommission(BigDecimal.valueOf(0.002));  // 0.2%
virtualBroker.setSlippage(BigDecimal.valueOf(0.001));    // 0.1%
```

### 3. 멀티 심볼 백테스트

```java
BacktestConfig config = BacktestConfig.builder()
    .symbols(List.of("005930", "000660", "035720"))
    // ... 기타 설정
    .build();
```

---

## ⚠️ 주의사항

### Lookahead Bias 방지
- DataReplayEngine은 Iterator 패턴 사용
- 각 시점에서 과거 데이터만 접근 가능
- 미래 정보 누출 불가

### 현실적인 시뮬레이션
- 수수료: 기본 0.15% (국내 증권사 평균)
- 슬리피지: 기본 0.05% (시장가 주문 시)
- 시장가 주문: Open 가격에 체결
- 지정가 주문: High/Low 가격 확인 후 체결

### 성능 고려사항
- 대용량 데이터 (1년치 1분봉 = 수만 건): 메모리 주의
- 멀티 심볼 백테스트: 데이터 양 증가
- 파라미터 최적화: Grid Search 시 시간 소요

---

## 📝 다음 단계

### Phase 5: KIS 실제 연동
- PAPER 모드 실제 API 연동
- WebSocket 실시간 시세
- 실제 체결 수신

### 추가 개선 사항
- 백테스트 병렬 실행
- 파라미터 최적화 (Grid Search, Genetic Algorithm)
- 리포트 생성 (PDF, 차트)
- 추가 전략 (Bollinger Bands, MACD)

---

## 📚 참고 자료

- [백테스팅 가이드](BACKTEST_GUIDE.md)
- [API 예제](API_EXAMPLES.md)
- [테스트 시나리오](TEST_SCENARIOS.md)

---

**문의**: Phase 4 백테스팅 엔진 관련 질문은 이슈로 등록해주세요.
