package maru.trading.domain.backtest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Backtest configuration.
 *
 * Defines parameters for running a backtest simulation.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestConfig {

    /**
     * Unique backtest ID (ULID).
     */
    private String backtestId;

    /**
     * Strategy to test.
     */
    private String strategyId;

    /**
     * Strategy parameters (e.g., MA periods, RSI thresholds).
     */
    private Map<String, Object> strategyParams;

    /**
     * Backtest date range.
     */
    private LocalDate startDate;
    private LocalDate endDate;

    /**
     * Symbols to test.
     */
    private List<String> symbols;

    /**
     * Bar timeframe: 1m, 5m, 15m, 1h, 1d, etc.
     */
    @Builder.Default
    private String timeframe = "1m";

    /**
     * Initial capital for simulation.
     */
    @Builder.Default
    private BigDecimal initialCapital = BigDecimal.valueOf(10_000_000); // 10M KRW

    /**
     * Commission rate (e.g., 0.001 = 0.1%).
     */
    @Builder.Default
    private BigDecimal commission = BigDecimal.valueOf(0.001);

    /**
     * Slippage rate (e.g., 0.0005 = 0.05%).
     */
    @Builder.Default
    private BigDecimal slippage = BigDecimal.valueOf(0.0005);
}
