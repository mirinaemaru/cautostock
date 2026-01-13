# Phase 4 구현 계획

**작성일**: 2026-01-01
**상태**: 📋 Planning
**기반**: PHASE4_DESIGN.md

---

## 📅 구현 일정

### Phase 4.1: 기본 백테스트 엔진 (2주)

**Week 1**: 데이터 및 코어 엔진

| Day | 작업 | 파일 | 예상 시간 |
|-----|------|------|----------|
| 1 | DB 스키마 마이그레이션 | V10__create_backtest_tables.sql | 2h |
| 1-2 | Historical Data 모델 | HistoricalBar entity | 4h |
| 2-3 | DataReplayEngine | DataReplayEngine.java | 8h |
| 3-4 | BacktestEngine 인터페이스 | BacktestEngine.java | 6h |
| 4-5 | BacktestOrchestrator | BacktestOrchestrator.java | 10h |

**Week 2**: 분석 및 통합

| Day | 작업 | 파일 | 예상 시간 |
|-----|------|------|----------|
| 6-7 | PerformanceAnalyzer | PerformanceAnalyzer.java | 10h |
| 7-8 | MetricsCalculator | MetricsCalculator.java | 8h |
| 8-9 | Backtest API Controller | BacktestController.java | 6h |
| 9-10 | Integration Tests | BacktestE2ETest.java | 8h |

**총 소요**: 62시간 (약 2주)

---

## 📂 파일 구조

```
src/main/java/maru/trading/
├── backtest/
│   ├── domain/
│   │   ├── BacktestConfig.java
│   │   ├── BacktestResult.java
│   │   ├── PerformanceMetrics.java
│   │   ├── RiskMetrics.java
│   │   ├── EquityCurve.java
│   │   └── Trade.java
│   ├── engine/
│   │   ├── BacktestEngine.java (interface)
│   │   ├── BacktestEngineImpl.java
│   │   ├── DataReplayEngine.java
│   │   └── VirtualBroker.java
│   ├── analyzer/
│   │   ├── PerformanceAnalyzer.java
│   │   ├── RiskAnalyzer.java
│   │   └── MetricsCalculator.java
│   └── optimizer/
│       ├── ParameterOptimizer.java
│       ├── GridSearchOptimizer.java
│       └── RandomSearchOptimizer.java
├── infra/persistence/jpa/entity/
│   ├── HistoricalBarEntity.java
│   ├── BacktestRunEntity.java
│   └── BacktestTradeEntity.java
├── infra/persistence/jpa/repository/
│   ├── HistoricalBarJpaRepository.java
│   ├── BacktestRunJpaRepository.java
│   └── BacktestTradeJpaRepository.java
└── api/controller/
    └── BacktestController.java

src/main/resources/db/migration/
└── V10__create_backtest_tables.sql

src/test/java/maru/trading/backtest/
├── DataReplayEngineTest.java
├── PerformanceAnalyzerTest.java
├── MetricsCalculatorTest.java
└── BacktestE2ETest.java
```

---

## 🔧 상세 구현 가이드

### 1. V10 Migration (Day 1)

**파일**: `V10__create_backtest_tables.sql`

```sql
-- Historical bars table
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

-- Backtest runs table
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

-- Backtest trades table
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

---

### 2. DataReplayEngine (Day 2-3)

**파일**: `src/main/java/maru/trading/backtest/engine/DataReplayEngine.java`

```java
package maru.trading.backtest.engine;

import maru.trading.domain.market.MarketBar;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

/**
 * Replays historical market data in chronological order.
 *
 * Key Features:
 * - Ensures time-order consistency
 * - Prevents lookahead bias
 * - Supports multiple symbols
 */
public interface DataReplayEngine {
    /**
     * Load historical bars from database.
     *
     * @param config Backtest configuration
     */
    void loadData(BacktestConfig config);

    /**
     * Check if more data available.
     */
    boolean hasNext();

    /**
     * Replay next bar.
     * Returns null when replay is complete.
     */
    MarketBar next();

    /**
     * Get current simulation timestamp.
     */
    LocalDateTime getCurrentTime();

    /**
     * Reset replay to beginning.
     */
    void reset();
}
```

**구현 클래스**: `DataReplayEngineImpl.java`

```java
@Component
public class DataReplayEngineImpl implements DataReplayEngine {

    private final HistoricalBarJpaRepository historicalBarRepository;
    private Iterator<HistoricalBarEntity> iterator;
    private LocalDateTime currentTime;

    @Override
    public void loadData(BacktestConfig config) {
        // Query historical bars ordered by timestamp
        List<HistoricalBarEntity> bars = historicalBarRepository
            .findBySymbolsAndDateRange(
                config.getSymbols(),
                config.getStartDate(),
                config.getEndDate(),
                config.getTimeframe()
            );

        // Sort by timestamp (critical for preventing lookahead bias)
        bars.sort(Comparator.comparing(HistoricalBarEntity::getBarTimestamp));

        this.iterator = bars.iterator();
        this.currentTime = null;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public MarketBar next() {
        if (!hasNext()) {
            return null;
        }

        HistoricalBarEntity entity = iterator.next();
        currentTime = entity.getBarTimestamp();

        // Convert to domain model
        return MarketBar.from(entity);
    }

    // ... other methods
}
```

---

### 3. BacktestOrchestrator (Day 4-5)

**파일**: `src/main/java/maru/trading/backtest/engine/BacktestOrchestrator.java`

```java
@Service
public class BacktestOrchestrator {

    private final DataReplayEngine replayEngine;
    private final StrategyEngine strategyEngine;
    private final VirtualBroker virtualBroker;
    private final PerformanceAnalyzer performanceAnalyzer;

    /**
     * Run backtest.
     */
    @Transactional
    public BacktestResult runBacktest(BacktestConfig config) {
        log.info("Starting backtest: {}", config.getBacktestId());

        // 1. Initialize
        replayEngine.loadData(config);
        virtualBroker.initialize(config.getInitialCapital());

        BacktestContext context = new BacktestContext();
        context.setConfig(config);

        // 2. Simulation loop
        while (replayEngine.hasNext()) {
            MarketBar bar = replayEngine.next();

            // 2.1 Update market data
            context.addBar(bar);

            // 2.2 Execute strategy
            SignalDecision decision = strategyEngine.evaluate(
                loadStrategyContext(context, bar.getSymbol())
            );

            // 2.3 Process signal
            if (decision.isActionable()) {
                Order order = createOrderFromSignal(decision, bar);

                // 2.4 Execute order (virtual)
                Fill fill = virtualBroker.executeOrder(order, bar);

                if (fill != null) {
                    context.addTrade(fill);
                }
            }

            // 2.5 Update portfolio state
            virtualBroker.updatePortfolio(bar);
        }

        // 3. Analyze results
        BacktestResult result = buildResult(context);
        PerformanceMetrics metrics = performanceAnalyzer.analyze(result);
        result.setMetrics(metrics);

        // 4. Save to database
        saveBacktestResult(result);

        log.info("Backtest complete: {} trades, return: {}%",
                result.getTrades().size(),
                result.getTotalReturn());

        return result;
    }
}
```

---

### 4. VirtualBroker (Day 4-5)

**파일**: `src/main/java/maru/trading/backtest/engine/VirtualBroker.java`

```java
/**
 * Simulates broker behavior for backtest.
 *
 * Responsibilities:
 * - Execute orders at historical prices
 * - Apply commission and slippage
 * - Maintain virtual portfolio
 */
@Component
public class VirtualBroker {

    private BigDecimal cash;
    private Map<String, Position> positions;
    private BigDecimal commission;
    private BigDecimal slippage;

    public void initialize(BigDecimal initialCapital) {
        this.cash = initialCapital;
        this.positions = new HashMap<>();
    }

    /**
     * Execute order using historical bar prices.
     */
    public Fill executeOrder(Order order, MarketBar bar) {
        // Determine fill price
        BigDecimal fillPrice = determineFillPrice(order, bar);

        // Apply slippage
        if (order.getSide() == Side.BUY) {
            fillPrice = fillPrice.multiply(BigDecimal.ONE.add(slippage));
        } else {
            fillPrice = fillPrice.multiply(BigDecimal.ONE.subtract(slippage));
        }

        // Calculate costs
        BigDecimal notional = fillPrice.multiply(order.getQty());
        BigDecimal commissionFee = notional.multiply(commission);

        // Update cash
        if (order.getSide() == Side.BUY) {
            cash = cash.subtract(notional).subtract(commissionFee);
        } else {
            cash = cash.add(notional).subtract(commissionFee);
        }

        // Update position
        updatePosition(order.getSymbol(), order.getSide(), order.getQty(), fillPrice);

        // Create fill
        return Fill.builder()
                .fillPrice(fillPrice)
                .fillQty(order.getQty())
                .commission(commissionFee)
                .build();
    }

    private BigDecimal determineFillPrice(Order order, MarketBar bar) {
        if (order.getOrderType() == OrderType.MARKET) {
            // Use open price (conservative)
            return bar.getOpenPrice();
        } else {
            // Limit order: check if price touched during bar
            BigDecimal limitPrice = order.getPrice();
            if (order.getSide() == Side.BUY) {
                return limitPrice.compareTo(bar.getHighPrice()) <= 0 ? limitPrice : null;
            } else {
                return limitPrice.compareTo(bar.getLowPrice()) >= 0 ? limitPrice : null;
            }
        }
    }
}
```

---

### 5. PerformanceAnalyzer (Day 6-7)

**파일**: `src/main/java/maru/trading/backtest/analyzer/PerformanceAnalyzer.java`

```java
@Component
public class PerformanceAnalyzer {

    /**
     * Calculate performance metrics.
     */
    public PerformanceMetrics analyze(BacktestResult result) {
        List<Trade> trades = result.getTrades();

        // Calculate total return
        BigDecimal initialCapital = result.getConfig().getInitialCapital();
        BigDecimal finalCapital = result.getFinalCapital();
        BigDecimal totalReturn = finalCapital.subtract(initialCapital)
                .divide(initialCapital, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // Calculate Sharpe ratio
        List<BigDecimal> dailyReturns = calculateDailyReturns(result.getEquityCurve());
        BigDecimal sharpeRatio = calculateSharpeRatio(dailyReturns);

        // Calculate max drawdown
        BigDecimal maxDrawdown = calculateMaxDrawdown(result.getEquityCurve());

        // Calculate win rate
        int winningTrades = (int) trades.stream()
                .filter(t -> t.getPnl().compareTo(BigDecimal.ZERO) > 0)
                .count();
        BigDecimal winRate = BigDecimal.valueOf(winningTrades)
                .divide(BigDecimal.valueOf(trades.size()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // Build metrics
        return PerformanceMetrics.builder()
                .totalReturn(totalReturn)
                .sharpeRatio(sharpeRatio)
                .maxDrawdown(maxDrawdown)
                .totalTrades(trades.size())
                .winningTrades(winningTrades)
                .losingTrades(trades.size() - winningTrades)
                .winRate(winRate)
                .build();
    }

    private BigDecimal calculateSharpeRatio(List<BigDecimal> dailyReturns) {
        // Mean daily return
        BigDecimal mean = dailyReturns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(dailyReturns.size()), 8, RoundingMode.HALF_UP);

        // Standard deviation
        BigDecimal variance = dailyReturns.stream()
                .map(r -> r.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(dailyReturns.size()), 8, RoundingMode.HALF_UP);

        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        // Sharpe = mean / stdDev * sqrt(252) (annualized)
        if (stdDev.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return mean.divide(stdDev, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(Math.sqrt(252)));
    }

    private BigDecimal calculateMaxDrawdown(EquityCurve curve) {
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        BigDecimal peak = BigDecimal.ZERO;

        for (EquityPoint point : curve.getPoints()) {
            if (point.getEquity().compareTo(peak) > 0) {
                peak = point.getEquity();
            }

            BigDecimal drawdown = peak.subtract(point.getEquity())
                    .divide(peak, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
        }

        return maxDrawdown;
    }
}
```

---

## 🧪 테스트 계획 (Day 9-10)

### BacktestE2ETest.java

```java
@SpringBootTest
@ActiveProfiles("test")
class BacktestE2ETest {

    @Autowired
    private BacktestOrchestrator backtestOrchestrator;

    @Autowired
    private HistoricalBarJpaRepository historicalBarRepository;

    @Test
    @DisplayName("Complete backtest flow with known strategy")
    void testCompleteBacktestFlow() {
        // Given - Load historical data (100 bars)
        loadHistoricalData("TEST_SYMBOL", 100);

        BacktestConfig config = BacktestConfig.builder()
                .strategyId("MA_CROSSOVER")
                .strategyParams(Map.of(
                        "shortPeriod", 5,
                        "longPeriod", 20
                ))
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 3, 31))
                .symbols(List.of("TEST_SYMBOL"))
                .timeframe("1d")
                .initialCapital(BigDecimal.valueOf(10000000))
                .commission(BigDecimal.valueOf(0.0003))
                .slippage(BigDecimal.valueOf(0.0001))
                .build();

        // When
        BacktestResult result = backtestOrchestrator.runBacktest(config);

        // Then
        assertThat(result.getFinalCapital()).isNotNull();
        assertThat(result.getTotalReturn()).isNotNull();
        assertThat(result.getMetrics()).isNotNull();
        assertThat(result.getMetrics().getTotalTrades()).isGreaterThan(0);
    }
}
```

---

## 📊 MVP Deliverables

### Phase 4.1 완료 시 제공

**API Endpoints**:
```
POST /api/v1/backtest/run
  - Request: BacktestConfig
  - Response: BacktestResult

GET /api/v1/backtest/{backtestId}
  - Response: BacktestResult

GET /api/v1/backtest/runs
  - Response: List<BacktestSummary>
```

**Performance Metrics**:
- Total Return
- Sharpe Ratio
- Max Drawdown
- Win Rate
- Profit Factor
- Total Trades

**Database Tables**:
- historical_bars
- backtest_runs
- backtest_trades

**Tests**:
- 20+ unit tests
- 5+ integration tests
- 1 E2E test

---

## 🚀 실행 예제

### API Call

```bash
curl -X POST http://localhost:8080/api/v1/backtest/run \
  -H "Content-Type: application/json" \
  -d '{
    "strategyId": "MA_CROSSOVER",
    "strategyParams": {
      "shortPeriod": 5,
      "longPeriod": 20
    },
    "startDate": "2025-01-01",
    "endDate": "2025-12-31",
    "symbols": ["005930"],
    "timeframe": "1d",
    "initialCapital": 10000000,
    "commission": 0.0003,
    "slippage": 0.0001
  }'
```

### Response

```json
{
  "backtestId": "01KDVK...",
  "status": "COMPLETED",
  "config": { ... },
  "finalCapital": 11530000,
  "totalReturn": 15.3,
  "metrics": {
    "totalReturn": 15.3,
    "sharpeRatio": 1.42,
    "maxDrawdown": -8.2,
    "winRate": 62.5,
    "totalTrades": 127,
    "winningTrades": 79,
    "losingTrades": 48
  }
}
```

---

**작성자**: Claude Sonnet 4.5
**작성일**: 2026-01-01
**다음 단계**: Demo Controller 구현 → Phase 4.1 시작 준비 완료
