package maru.trading.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a backtest.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestRequest {

    /**
     * Strategy ID to backtest (e.g., "MA_CROSS_5_20", "RSI_30_70").
     * Required.
     */
    private String strategyId;

    /**
     * List of symbols to test (e.g., ["005930", "035720"]).
     * Required. At least one symbol.
     */
    private List<String> symbols;

    /**
     * Start date in ISO format (e.g., "2024-01-01").
     * Required.
     */
    private String startDate;

    /**
     * End date in ISO format (e.g., "2024-12-31").
     * Required.
     */
    private String endDate;

    /**
     * Timeframe for bars (e.g., "1m", "5m", "1h", "1d").
     * Default: "1d"
     */
    private String timeframe;

    /**
     * Initial capital for backtest (e.g., 10000000 = 10M KRW).
     * Required. Must be positive.
     */
    private BigDecimal initialCapital;

    /**
     * Commission rate per trade (e.g., 0.0015 = 0.15%).
     * Default: 0.0015
     */
    private BigDecimal commission;

    /**
     * Slippage rate per trade (e.g., 0.0005 = 0.05%).
     * Default: 0.0005
     */
    private BigDecimal slippage;

    /**
     * Strategy-specific parameters.
     * Example for MA crossover: {"shortPeriod": 5, "longPeriod": 20}
     * Example for RSI: {"period": 14, "overbought": 70, "oversold": 30}
     * Optional.
     */
    private Map<String, Object> strategyParams;
}
