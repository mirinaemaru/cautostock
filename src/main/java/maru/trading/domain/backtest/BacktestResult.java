package maru.trading.domain.backtest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.execution.Fill;
import maru.trading.domain.order.Order;
import maru.trading.domain.signal.Signal;
import maru.trading.domain.execution.Position;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Backtest execution result.
 *
 * Contains all trading activity and performance metrics
 * from a backtest simulation.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestResult {

    /**
     * Backtest ID.
     */
    private String backtestId;

    /**
     * Configuration used.
     */
    private BacktestConfig config;

    /**
     * Execution timestamps.
     */
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // ========== Trading Activity ==========

    /**
     * All signals generated during backtest.
     */
    @Builder.Default
    private List<Signal> signals = new ArrayList<>();

    /**
     * All orders placed during backtest.
     */
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    /**
     * All fills executed during backtest.
     */
    @Builder.Default
    private List<Fill> fills = new ArrayList<>();

    /**
     * All positions held during backtest.
     */
    @Builder.Default
    private List<Position> positions = new ArrayList<>();

    /**
     * All simulated trades (entry + exit).
     */
    @Builder.Default
    private List<BacktestTrade> trades = new ArrayList<>();

    // ========== Performance ==========

    /**
     * Final capital after backtest.
     */
    private BigDecimal finalCapital;

    /**
     * Total return percentage.
     */
    private BigDecimal totalReturn;

    /**
     * Performance metrics.
     */
    private PerformanceMetrics performanceMetrics;

    /**
     * Risk metrics.
     */
    private RiskMetrics riskMetrics;

    /**
     * Equity curve (capital over time).
     */
    private EquityCurve equityCurve;
}
