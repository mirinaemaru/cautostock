package maru.trading.application.backtest;

import maru.trading.domain.backtest.*;
import maru.trading.domain.order.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for PerformanceAnalyzerImpl.
 */
class PerformanceAnalyzerImplTest {

    private PerformanceAnalyzerImpl analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new PerformanceAnalyzerImpl();
    }

    @Test
    @DisplayName("Should calculate total return correctly")
    void testTotalReturn() {
        // Given: Initial 10M → Final 12M = 20% return
        BacktestResult result = createBacktestResult(
                BigDecimal.valueOf(10_000_000),
                BigDecimal.valueOf(12_000_000),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                List.of()
        );

        // When
        PerformanceMetrics metrics = analyzer.analyze(result);

        // Then: Total return should be 20%
        assertThat(metrics.getTotalReturn()).isCloseTo(BigDecimal.valueOf(20.0), within(BigDecimal.valueOf(0.01)));
    }

    @Test
    @DisplayName("Should calculate win rate correctly")
    void testWinRate() {
        // Given: 3 winning trades, 2 losing trades
        List<BacktestTrade> trades = List.of(
                createTrade(Side.BUY, BigDecimal.valueOf(1000), true),   // Win
                createTrade(Side.BUY, BigDecimal.valueOf(500), true),    // Win
                createTrade(Side.BUY, BigDecimal.valueOf(-300), false),  // Loss
                createTrade(Side.BUY, BigDecimal.valueOf(800), true),    // Win
                createTrade(Side.BUY, BigDecimal.valueOf(-200), false)   // Loss
        );

        BacktestResult result = createBacktestResult(
                BigDecimal.valueOf(10_000_000),
                BigDecimal.valueOf(10_001_800),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                trades
        );

        // When
        PerformanceMetrics metrics = analyzer.analyze(result);

        // Then: Win rate should be 60% (3/5)
        assertThat(metrics.getTotalTrades()).isEqualTo(5);
        assertThat(metrics.getWinningTrades()).isEqualTo(3);
        assertThat(metrics.getLosingTrades()).isEqualTo(2);
        assertThat(metrics.getWinRate()).isCloseTo(BigDecimal.valueOf(60.0), within(BigDecimal.valueOf(0.01)));
    }

    @Test
    @DisplayName("Should calculate profit factor correctly")
    void testProfitFactor() {
        // Given: Gross profit = 2300, Gross loss = 500
        List<BacktestTrade> trades = List.of(
                createTrade(Side.BUY, BigDecimal.valueOf(1000), true),   // Win
                createTrade(Side.BUY, BigDecimal.valueOf(500), true),    // Win
                createTrade(Side.BUY, BigDecimal.valueOf(-300), false),  // Loss
                createTrade(Side.BUY, BigDecimal.valueOf(800), true),    // Win
                createTrade(Side.BUY, BigDecimal.valueOf(-200), false)   // Loss
        );

        BacktestResult result = createBacktestResult(
                BigDecimal.valueOf(10_000_000),
                BigDecimal.valueOf(10_001_800),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                trades
        );

        // When
        PerformanceMetrics metrics = analyzer.analyze(result);

        // Then: Profit factor should be 2300 / 500 = 4.6
        assertThat(metrics.getTotalProfit()).isEqualByComparingTo(BigDecimal.valueOf(2300));
        assertThat(metrics.getTotalLoss()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(metrics.getProfitFactor()).isCloseTo(BigDecimal.valueOf(4.6), within(BigDecimal.valueOf(0.01)));
    }

    @Test
    @DisplayName("Should calculate average win and loss correctly")
    void testAverageWinLoss() {
        // Given
        List<BacktestTrade> trades = List.of(
                createTrade(Side.BUY, BigDecimal.valueOf(1000), true),   // Win
                createTrade(Side.BUY, BigDecimal.valueOf(500), true),    // Win
                createTrade(Side.BUY, BigDecimal.valueOf(-300), false),  // Loss
                createTrade(Side.BUY, BigDecimal.valueOf(800), true),    // Win
                createTrade(Side.BUY, BigDecimal.valueOf(-200), false)   // Loss
        );

        BacktestResult result = createBacktestResult(
                BigDecimal.valueOf(10_000_000),
                BigDecimal.valueOf(10_001_800),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                trades
        );

        // When
        PerformanceMetrics metrics = analyzer.analyze(result);

        // Then
        BigDecimal avgWin = BigDecimal.valueOf(2300).divide(BigDecimal.valueOf(3), 8, RoundingMode.HALF_UP);
        BigDecimal avgLoss = BigDecimal.valueOf(500).divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);

        assertThat(metrics.getAvgWin()).isCloseTo(avgWin, within(BigDecimal.valueOf(0.01)));
        assertThat(metrics.getAvgLoss()).isCloseTo(avgLoss, within(BigDecimal.valueOf(0.01)));
    }

    @Test
    @DisplayName("Should find largest win and loss correctly")
    void testLargestWinLoss() {
        // Given
        List<BacktestTrade> trades = List.of(
                createTrade(Side.BUY, BigDecimal.valueOf(1000), true),
                createTrade(Side.BUY, BigDecimal.valueOf(500), true),
                createTrade(Side.BUY, BigDecimal.valueOf(-300), false),
                createTrade(Side.BUY, BigDecimal.valueOf(1500), true),   // Largest win
                createTrade(Side.BUY, BigDecimal.valueOf(-800), false)   // Largest loss
        );

        BacktestResult result = createBacktestResult(
                BigDecimal.valueOf(10_000_000),
                BigDecimal.valueOf(10_001_900),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                trades
        );

        // When
        PerformanceMetrics metrics = analyzer.analyze(result);

        // Then
        assertThat(metrics.getLargestWin()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(metrics.getLargestLoss()).isEqualByComparingTo(BigDecimal.valueOf(800));
    }

    @Test
    @DisplayName("Should calculate max consecutive wins and losses")
    void testMaxConsecutiveWinsLosses() {
        // Given: W-W-W-L-W-L-L-L-W
        List<BacktestTrade> trades = List.of(
                createTrade(Side.BUY, BigDecimal.valueOf(100), true),    // W
                createTrade(Side.BUY, BigDecimal.valueOf(200), true),    // W
                createTrade(Side.BUY, BigDecimal.valueOf(150), true),    // W (max consecutive wins = 3)
                createTrade(Side.BUY, BigDecimal.valueOf(-100), false),  // L
                createTrade(Side.BUY, BigDecimal.valueOf(50), true),     // W
                createTrade(Side.BUY, BigDecimal.valueOf(-50), false),   // L
                createTrade(Side.BUY, BigDecimal.valueOf(-75), false),   // L
                createTrade(Side.BUY, BigDecimal.valueOf(-125), false),  // L (max consecutive losses = 3)
                createTrade(Side.BUY, BigDecimal.valueOf(100), true)     // W
        );

        BacktestResult result = createBacktestResult(
                BigDecimal.valueOf(10_000_000),
                BigDecimal.valueOf(10_000_250),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                trades
        );

        // When
        PerformanceMetrics metrics = analyzer.analyze(result);

        // Then
        assertThat(metrics.getMaxConsecutiveWins()).isEqualTo(3);
        assertThat(metrics.getMaxConsecutiveLosses()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should handle empty trade list")
    void testEmptyTrades() {
        // Given
        BacktestResult result = createBacktestResult(
                BigDecimal.valueOf(10_000_000),
                BigDecimal.valueOf(10_000_000),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                List.of()
        );

        // When
        PerformanceMetrics metrics = analyzer.analyze(result);

        // Then: Should return zero/null values without errors
        assertThat(metrics.getTotalTrades()).isNull();
        assertThat(metrics.getWinningTrades()).isNull();
        assertThat(metrics.getLosingTrades()).isNull();
    }

    @Test
    @DisplayName("Should generate equity curve")
    void testEquityCurve() {
        // Given
        List<BacktestTrade> trades = List.of(
                createTradeWithTime(BigDecimal.valueOf(1000), LocalDateTime.of(2024, 1, 15, 10, 0)),
                createTradeWithTime(BigDecimal.valueOf(-300), LocalDateTime.of(2024, 1, 16, 10, 0)),
                createTradeWithTime(BigDecimal.valueOf(500), LocalDateTime.of(2024, 1, 17, 10, 0))
        );

        BacktestConfig config = BacktestConfig.builder()
                .backtestId("TEST_001")
                .strategyId("TEST_STRATEGY")
                .initialCapital(BigDecimal.valueOf(10_000_000))
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .timeframe("1d")
                .build();

        BacktestResult result = BacktestResult.builder()
                .backtestId("TEST_001")
                .config(config)
                .startTime(LocalDateTime.of(2024, 1, 1, 9, 0))
                .endTime(LocalDate.of(2024, 12, 31).atTime(23, 59, 59))
                .finalCapital(BigDecimal.valueOf(10_001_200))
                .totalReturn(BigDecimal.valueOf(0.012))
                .trades(new ArrayList<>(trades))
                .build();

        // When
        EquityCurve curve = analyzer.generateEquityCurve(result);

        // Then: Should have initial point + 3 trade points = 4 points
        assertThat(curve.getPoints()).hasSize(4);

        // Initial equity = 10M
        assertThat(curve.getPoints().get(0).getEquity()).isEqualByComparingTo(BigDecimal.valueOf(10_000_000));

        // After trade 1: 10M + 1000 = 10,001,000
        assertThat(curve.getPoints().get(1).getEquity()).isEqualByComparingTo(BigDecimal.valueOf(10_001_000));

        // After trade 2: 10,001,000 - 300 = 10,000,700
        assertThat(curve.getPoints().get(2).getEquity()).isEqualByComparingTo(BigDecimal.valueOf(10_000_700));

        // After trade 3: 10,000,700 + 500 = 10,001,200
        assertThat(curve.getPoints().get(3).getEquity()).isEqualByComparingTo(BigDecimal.valueOf(10_001_200));
    }

    @Test
    @DisplayName("Should calculate risk metrics")
    void testRiskMetrics() {
        // Given
        List<BacktestTrade> trades = createSampleTrades();

        BacktestResult result = createBacktestResult(
                BigDecimal.valueOf(10_000_000),
                BigDecimal.valueOf(10_100_000),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                trades
        );

        // When
        RiskMetrics riskMetrics = analyzer.analyzeRisk(result);

        // Then: Should calculate volatility, VaR, etc.
        assertThat(riskMetrics).isNotNull();
        assertThat(riskMetrics.getVolatility()).isNotNull();
        assertThat(riskMetrics.getVar95()).isNotNull();
        assertThat(riskMetrics.getCvar95()).isNotNull();
    }

    // ========== Helper Methods ==========

    private BacktestResult createBacktestResult(BigDecimal initialCapital, BigDecimal finalCapital,
                                                LocalDate startDate, LocalDate endDate,
                                                List<BacktestTrade> trades) {
        BacktestConfig config = BacktestConfig.builder()
                .backtestId("TEST_001")
                .strategyId("TEST_STRATEGY")
                .initialCapital(initialCapital)
                .startDate(startDate)
                .endDate(endDate)
                .timeframe("1d")
                .build();

        return BacktestResult.builder()
                .backtestId("TEST_001")
                .config(config)
                .startTime(startDate.atStartOfDay())
                .endTime(endDate.atTime(23, 59, 59))
                .finalCapital(finalCapital)
                .totalReturn(finalCapital.subtract(initialCapital)
                        .divide(initialCapital, 8, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)))
                .trades(new ArrayList<>(trades))
                .build();
    }

    private BacktestTrade createTrade(Side side, BigDecimal netPnl, boolean isWinner) {
        return BacktestTrade.builder()
                .tradeId("TRADE_" + System.nanoTime())
                .backtestId("TEST_001")
                .symbol("005930")
                .side(side)
                .entryTime(LocalDateTime.now())
                .entryPrice(BigDecimal.valueOf(70_000))
                .entryQty(BigDecimal.valueOf(10))
                .exitTime(LocalDateTime.now())
                .exitPrice(isWinner ? BigDecimal.valueOf(71_000) : BigDecimal.valueOf(69_000))
                .exitQty(BigDecimal.valueOf(10))
                .netPnl(netPnl)
                .returnPct(netPnl.divide(BigDecimal.valueOf(700_000), 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)))
                .status("CLOSED")
                .build();
    }

    private BacktestTrade createTradeWithTime(BigDecimal netPnl, LocalDateTime exitTime) {
        return BacktestTrade.builder()
                .tradeId("TRADE_" + System.nanoTime())
                .backtestId("TEST_001")
                .symbol("005930")
                .side(Side.BUY)
                .entryTime(exitTime.minusDays(1))
                .entryPrice(BigDecimal.valueOf(70_000))
                .entryQty(BigDecimal.valueOf(10))
                .exitTime(exitTime)
                .exitPrice(BigDecimal.valueOf(70_000).add(netPnl.divide(BigDecimal.valueOf(10), RoundingMode.HALF_UP)))
                .exitQty(BigDecimal.valueOf(10))
                .netPnl(netPnl)
                .returnPct(netPnl.divide(BigDecimal.valueOf(700_000), 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)))
                .status("CLOSED")
                .build();
    }

    private List<BacktestTrade> createSampleTrades() {
        return List.of(
                createTrade(Side.BUY, BigDecimal.valueOf(1000), true),
                createTrade(Side.BUY, BigDecimal.valueOf(500), true),
                createTrade(Side.BUY, BigDecimal.valueOf(-300), false),
                createTrade(Side.BUY, BigDecimal.valueOf(800), true),
                createTrade(Side.BUY, BigDecimal.valueOf(-200), false),
                createTrade(Side.BUY, BigDecimal.valueOf(1500), true),
                createTrade(Side.BUY, BigDecimal.valueOf(-400), false)
        );
    }
}
