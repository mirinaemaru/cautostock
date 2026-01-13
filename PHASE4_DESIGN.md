# Phase 4 설계: 백테스팅 엔진

**작성일**: 2026-01-01
**상태**: 📝 Design Phase
**목표**: 과거 데이터로 전략 성과 측정 및 최적화

---

## 📋 Executive Summary

Phase 4는 백테스팅 엔진을 구현하여 전략의 과거 성과를 측정하고 파라미터를 최적화합니다.

**핵심 목표**:
- 📊 과거 데이터 재생 (Historical Data Replay)
- 📈 전략 성과 측정 (Performance Metrics)
- 🎯 파라미터 최적화 (Parameter Optimization)
- 📉 리스크 분석 (Risk Analysis)

**기대 효과**:
- 실전 투입 전 전략 검증
- 최적 파라미터 발견
- 리스크/수익 프로파일 분석
- 전략 간 비교 분석

---

## 🎯 요구사항

### 1. 기능 요구사항

#### FR-1: 과거 데이터 재생
- **설명**: 과거 시장 데이터를 시간 순으로 재생
- **입력**: 시작일, 종료일, 심볼 목록, 타임프레임
- **출력**: 재생된 틱/바 데이터
- **제약**: 실제 시간 흐름을 시뮬레이션 (순서 보장)

**예시**:
```java
BacktestConfig config = BacktestConfig.builder()
    .startDate(LocalDate.of(2025, 1, 1))
    .endDate(LocalDate.of(2025, 12, 31))
    .symbols(List.of("005930", "000660"))
    .timeframe("1m")
    .build();
```

#### FR-2: 전략 실행
- **설명**: 과거 데이터에 대해 전략을 실행하고 매매 신호 생성
- **입력**: 전략 ID, 전략 파라미터, 데이터
- **출력**: 신호 목록, 주문 목록, 체결 목록
- **제약**: 실제 시스템과 동일한 로직 사용

#### FR-3: 성과 측정
- **설명**: 백테스트 결과를 분석하여 성과 지표 계산
- **지표**:
  - 총 수익률 (Total Return)
  - 연간 수익률 (Annual Return)
  - 샤프 비율 (Sharpe Ratio)
  - 최대 낙폭 (Max Drawdown)
  - 승률 (Win Rate)
  - 평균 손익비 (Profit Factor)

**출력 예시**:
```json
{
  "totalReturn": 15.3,
  "annualReturn": 18.5,
  "sharpeRatio": 1.42,
  "maxDrawdown": -8.2,
  "winRate": 62.5,
  "profitFactor": 1.85,
  "totalTrades": 127,
  "winningTrades": 79,
  "losingTrades": 48
}
```

#### FR-4: 파라미터 최적화
- **설명**: 다양한 파라미터 조합을 시도하여 최적값 탐색
- **방법**:
  - Grid Search (격자 탐색)
  - Random Search (무작위 탐색)
  - Genetic Algorithm (유전 알고리즘) - 선택
- **입력**: 파라미터 범위, 최적화 목표 (샤프 비율, 총 수익률 등)
- **출력**: 최적 파라미터 세트, 성과 비교

**예시**:
```java
OptimizationConfig config = OptimizationConfig.builder()
    .strategy("MA_CROSSOVER")
    .parameterSpace(Map.of(
        "shortPeriod", Range.of(3, 10),   // 3~10
        "longPeriod", Range.of(15, 30)    // 15~30
    ))
    .objective("sharpe_ratio")
    .method(OptimizationMethod.GRID_SEARCH)
    .build();
```

#### FR-5: 리스크 분석
- **설명**: 포트폴리오 리스크 지표 계산
- **지표**:
  - VaR (Value at Risk)
  - CVaR (Conditional VaR)
  - Beta, Alpha
  - Volatility
  - Correlation Matrix

### 2. 비기능 요구사항

#### NFR-1: 성능
- 1년치 1분봉 데이터 백테스트: < 5분
- 파라미터 최적화 (100회): < 30분
- 메모리 사용: < 4GB

#### NFR-2: 정확성
- 실제 시스템과 100% 동일한 로직
- Lookahead Bias 방지
- Survivorship Bias 방지

#### NFR-3: 확장성
- 다중 전략 동시 백테스트
- 다중 심볼 포트폴리오
- 병렬 처리 지원

---

## 🏗️ 아키텍처 설계

### 1. 전체 구조

```
┌─────────────────────────────────────────────┐
│         Backtest Engine API                  │
│  (BacktestController, BacktestRequest)      │
└────────────────┬────────────────────────────┘
                 │
┌────────────────▼────────────────────────────┐
│      Backtest Orchestrator                   │
│  - runBacktest()                             │
│  - optimizeParameters()                      │
└────────────────┬────────────────────────────┘
                 │
        ┌────────┴────────┐
        │                 │
┌───────▼──────┐  ┌──────▼──────────┐
│ Data Replay  │  │ Performance     │
│ Engine       │  │ Analyzer        │
└───────┬──────┘  └──────┬──────────┘
        │                 │
┌───────▼──────┐  ┌──────▼──────────┐
│ Historical   │  │ Metrics         │
│ Data Source  │  │ Calculator      │
└──────────────┘  └─────────────────┘
```

### 2. 핵심 컴포넌트

#### 2.1 BacktestEngine

**책임**: 백테스트 전체 오케스트레이션

```java
public interface BacktestEngine {
    /**
     * Run backtest with given configuration.
     */
    BacktestResult runBacktest(BacktestConfig config);

    /**
     * Optimize strategy parameters.
     */
    OptimizationResult optimize(OptimizationConfig config);

    /**
     * Compare multiple strategies.
     */
    ComparisonResult compare(List<BacktestConfig> configs);
}
```

#### 2.2 DataReplayEngine

**책임**: 과거 데이터 시간 순 재생

```java
public interface DataReplayEngine {
    /**
     * Load historical data from source.
     */
    void loadData(LocalDate startDate, LocalDate endDate, List<String> symbols);

    /**
     * Replay next bar/tick.
     * Returns false when replay is complete.
     */
    boolean replayNext();

    /**
     * Get current replay timestamp.
     */
    LocalDateTime getCurrentTime();
}
```

**특징**:
- Iterator 패턴 사용
- 타임스탬프 순 정렬 보장
- Lookahead Bias 방지

#### 2.3 PerformanceAnalyzer

**책임**: 백테스트 결과 분석

```java
public interface PerformanceAnalyzer {
    /**
     * Calculate performance metrics.
     */
    PerformanceMetrics analyze(BacktestResult result);

    /**
     * Calculate risk metrics.
     */
    RiskMetrics analyzeRisk(BacktestResult result);

    /**
     * Generate equity curve.
     */
    EquityCurve generateEquityCurve(BacktestResult result);
}
```

#### 2.4 ParameterOptimizer

**책임**: 파라미터 최적화

```java
public interface ParameterOptimizer {
    /**
     * Find optimal parameters using grid search.
     */
    OptimizationResult gridSearch(OptimizationConfig config);

    /**
     * Find optimal parameters using random search.
     */
    OptimizationResult randomSearch(OptimizationConfig config);
}
```

### 3. 데이터 모델

#### BacktestConfig

```java
public class BacktestConfig {
    private String backtestId;
    private String strategyId;
    private Map<String, Object> strategyParams;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> symbols;
    private String timeframe;  // "1m", "5m", "1h", "1d"
    private BigDecimal initialCapital;  // 초기 자본
    private BigDecimal commission;      // 수수료율
    private BigDecimal slippage;        // 슬리피지
}
```

#### BacktestResult

```java
public class BacktestResult {
    private String backtestId;
    private BacktestConfig config;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Trading activity
    private List<Signal> signals;
    private List<Order> orders;
    private List<Fill> fills;
    private List<Position> positions;

    // Performance
    private BigDecimal finalCapital;
    private BigDecimal totalReturn;
    private PerformanceMetrics metrics;
    private RiskMetrics riskMetrics;
    private EquityCurve equityCurve;
}
```

#### PerformanceMetrics

```java
public class PerformanceMetrics {
    private BigDecimal totalReturn;       // 총 수익률
    private BigDecimal annualReturn;      // 연간 수익률
    private BigDecimal sharpeRatio;       // 샤프 비율
    private BigDecimal sortinoRatio;      // 소르티노 비율
    private BigDecimal maxDrawdown;       // 최대 낙폭
    private BigDecimal maxDrawdownDuration;  // 최대 낙폭 기간

    private int totalTrades;
    private int winningTrades;
    private int losingTrades;
    private BigDecimal winRate;           // 승률
    private BigDecimal profitFactor;      // 손익비
    private BigDecimal avgWin;            // 평균 수익
    private BigDecimal avgLoss;           // 평균 손실
}
```

#### RiskMetrics

```java
public class RiskMetrics {
    private BigDecimal volatility;        // 변동성 (연율화)
    private BigDecimal beta;              // 베타
    private BigDecimal alpha;             // 알파
    private BigDecimal var95;             // VaR 95%
    private BigDecimal cvar95;            // CVaR 95%
    private BigDecimal calmarRatio;       // 칼마 비율
}
```

### 4. 데이터베이스 스키마

#### backtest_runs (백테스트 실행 기록)

```sql
CREATE TABLE backtest_runs (
    backtest_id CHAR(26) PRIMARY KEY,
    strategy_id CHAR(26) NOT NULL,
    strategy_version_id CHAR(26),

    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    symbols JSON NOT NULL,
    timeframe VARCHAR(8) NOT NULL,

    initial_capital DECIMAL(18,2) NOT NULL,
    final_capital DECIMAL(18,2),
    total_return DECIMAL(10,4),

    status VARCHAR(16) NOT NULL,  -- PENDING, RUNNING, COMPLETED, FAILED

    created_at DATETIME(3) NOT NULL,
    started_at DATETIME(3),
    completed_at DATETIME(3),

    config_json JSON NOT NULL,
    result_json JSON,

    INDEX idx_strategy (strategy_id),
    INDEX idx_status (status),
    INDEX idx_created (created_at)
);
```

#### backtest_trades (백테스트 거래 내역)

```sql
CREATE TABLE backtest_trades (
    trade_id CHAR(26) PRIMARY KEY,
    backtest_id CHAR(26) NOT NULL,

    symbol VARCHAR(16) NOT NULL,
    side VARCHAR(8) NOT NULL,
    entry_date DATETIME(3) NOT NULL,
    exit_date DATETIME(3),

    entry_price DECIMAL(18,4) NOT NULL,
    exit_price DECIMAL(18,4),
    qty DECIMAL(18,6) NOT NULL,

    pnl DECIMAL(18,2),
    pnl_pct DECIMAL(10,4),

    FOREIGN KEY (backtest_id) REFERENCES backtest_runs(backtest_id),
    INDEX idx_backtest (backtest_id),
    INDEX idx_symbol (symbol)
);
```

#### historical_bars (과거 바 데이터)

```sql
CREATE TABLE historical_bars (
    bar_id CHAR(26) PRIMARY KEY,
    symbol VARCHAR(16) NOT NULL,
    timeframe VARCHAR(8) NOT NULL,
    bar_timestamp DATETIME(3) NOT NULL,

    open_price DECIMAL(18,4) NOT NULL,
    high_price DECIMAL(18,4) NOT NULL,
    low_price DECIMAL(18,4) NOT NULL,
    close_price DECIMAL(18,4) NOT NULL,
    volume BIGINT NOT NULL,

    created_at DATETIME(3) NOT NULL,

    UNIQUE INDEX uk_symbol_timeframe_timestamp (symbol, timeframe, bar_timestamp),
    INDEX idx_symbol_timestamp (symbol, bar_timestamp)
);
```

---

## 🔄 백테스트 프로세스

### 1. 데이터 준비 단계

```
1. Load Historical Data
   - Query: SELECT * FROM historical_bars
            WHERE symbol IN (symbols)
              AND bar_timestamp BETWEEN start_date AND end_date
            ORDER BY bar_timestamp ASC

2. Validate Data
   - Check gaps
   - Check duplicates
   - Fill missing data (optional)

3. Initialize Environment
   - Set initial capital
   - Create virtual account
   - Reset risk state
```

### 2. 시뮬레이션 단계

```
WHILE has_more_data:
    1. Replay Next Bar
       - Load next bar from historical_bars
       - Update current_timestamp

    2. Update Market Data
       - Push bar to BarCache
       - Calculate indicators (MA, RSI, etc.)

    3. Execute Strategy
       - Call StrategyEngine.evaluate()
       - Generate signals

    4. Process Signals
       - Validate with SignalPolicy
       - Check RiskEngine

    5. Place Orders
       - Create virtual orders
       - Apply commission & slippage

    6. Simulate Fills
       - Match orders against historical prices
       - Update positions
       - Calculate P&L

    7. Record State
       - Save trade to backtest_trades
       - Update equity curve
```

### 3. 분석 단계

```
1. Calculate Performance Metrics
   - Total return
   - Sharpe ratio
   - Max drawdown
   - Win rate

2. Calculate Risk Metrics
   - Volatility
   - VaR / CVaR
   - Beta, Alpha

3. Generate Reports
   - Equity curve chart
   - Drawdown chart
   - Trade distribution
   - Monthly returns

4. Save Results
   - INSERT INTO backtest_runs
   - Save result_json
```

---

## 🎯 파라미터 최적화 알고리즘

### Grid Search (격자 탐색)

```python
# Pseudo-code
for shortPeriod in range(3, 11):  # 3~10
    for longPeriod in range(15, 31):  # 15~30
        if shortPeriod >= longPeriod:
            continue

        params = {"shortPeriod": shortPeriod, "longPeriod": longPeriod}
        result = run_backtest(params)

        if result.sharpe_ratio > best_sharpe:
            best_sharpe = result.sharpe_ratio
            best_params = params

# Total combinations: 8 * 16 = 128
```

**장점**: 모든 조합 탐색
**단점**: 조합 폭발 (파라미터 많으면 느림)

### Random Search (무작위 탐색)

```python
# Pseudo-code
for i in range(100):  # 100회 시도
    shortPeriod = random.randint(3, 10)
    longPeriod = random.randint(15, 30)

    if shortPeriod >= longPeriod:
        continue

    params = {"shortPeriod": shortPeriod, "longPeriod": longPeriod}
    result = run_backtest(params)

    if result.sharpe_ratio > best_sharpe:
        best_sharpe = result.sharpe_ratio
        best_params = params
```

**장점**: 빠름, 고차원에 유리
**단점**: 최적해 보장 안됨

---

## 🚧 함정 및 주의사항

### 1. Lookahead Bias (미래 정보 사용)

**문제**: 미래 데이터를 사용하여 과거 결정을 내리는 오류

**예시 (잘못된 코드)**:
```java
// BAD: 전체 데이터로 MA 계산 후 과거로 돌아감
List<Bar> allBars = loadAllBars();  // 미래 데이터 포함
BigDecimal ma = calculateMA(allBars, 20);
// 과거 시점에서 미래 데이터를 알고 있음!
```

**해결**:
```java
// GOOD: 현재 시점까지만 사용
List<Bar> barsUntilNow = loadBarsUntil(currentTimestamp);
BigDecimal ma = calculateMA(barsUntilNow, 20);
```

### 2. Survivorship Bias (생존 편향)

**문제**: 현재 남아있는 종목만 백테스트 (상장폐지 종목 제외)

**해결**:
- 상장폐지 종목 포함
- ETF/인덱스 사용

### 3. Overfitting (과적합)

**문제**: 과거 데이터에만 잘 맞는 파라미터

**해결**:
- Train/Test 데이터 분리
- Walk-forward Analysis
- Out-of-sample 테스트

### 4. Transaction Costs (거래 비용)

**문제**: 수수료/슬리피지 미반영 시 과대평가

**해결**:
```java
BigDecimal commission = 0.0003;  // 0.03%
BigDecimal slippage = 0.0001;    // 0.01%
BigDecimal totalCost = orderValue * (commission + slippage);
```

---

## 📊 구현 우선순위

### Phase 4.1: 기본 백테스트 엔진 (High Priority)

**목표**: 단일 전략, 단일 심볼 백테스트

**구현**:
- BacktestEngine
- DataReplayEngine
- PerformanceAnalyzer (기본 지표만)
- historical_bars 테이블
- backtest_runs 테이블

**예상 소요**: 2주

### Phase 4.2: 성과 분석 강화 (Medium Priority)

**목표**: 상세 리스크 지표 및 차트

**구현**:
- RiskMetrics (VaR, CVaR, Beta, Alpha)
- EquityCurve 생성
- Drawdown 차트
- Trade 분포 차트

**예상 소요**: 1주

### Phase 4.3: 파라미터 최적화 (Medium Priority)

**목표**: Grid Search, Random Search

**구현**:
- ParameterOptimizer
- Parallel execution
- Optimization results 저장

**예상 소요**: 1주

### Phase 4.4: 고급 기능 (Low Priority)

**목표**: Walk-forward, Monte Carlo

**구현**:
- Walk-forward Analysis
- Monte Carlo Simulation
- Portfolio Backtest (다중 심볼)

**예상 소요**: 2주

---

## 🧪 테스트 계획

### Unit Tests

- DataReplayEngine - 데이터 순서 검증
- PerformanceAnalyzer - 지표 계산 정확도
- ParameterOptimizer - 최적화 알고리즘

### Integration Tests

- E2E Backtest - 전체 백테스트 플로우
- Optimization - 파라미터 최적화 플로우

### Validation Tests

- Known Strategy Test - 알려진 전략으로 검증
- Benchmark Comparison - 벤치마크 대비 성과

---

## 📈 성공 지표

**Phase 4.1 완료 기준**:
- ✅ 1년치 1분봉 백테스트 < 5분
- ✅ 성과 지표 10개 이상 계산
- ✅ 백테스트 결과 DB 저장
- ✅ 20개 이상 단위 테스트 통과

**Phase 4.2 완료 기준**:
- ✅ 리스크 지표 5개 이상 계산
- ✅ Equity Curve 시각화
- ✅ Drawdown 차트 생성

**Phase 4.3 완료 기준**:
- ✅ Grid Search 구현
- ✅ 100회 백테스트 < 30분
- ✅ 최적 파라미터 발견

---

## 🔮 향후 확장

**Phase 5 이후**:
- 실시간 모니터링 + 백테스트 비교
- 전략 앙상블 (다중 전략 조합)
- 기계학습 기반 파라미터 학습
- 웹 대시보드 (차트, 리포트)

---

**작성자**: Claude Sonnet 4.5
**작성일**: 2026-01-01
**다음 단계**: Phase 4.1 구현 계획 수립

