package maru.trading.domain.backtest.portfolio;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Portfolio Backt configuration.
 *
 * Backtests multiple symbols simultaneously with portfolio-level risk management.
 */
@Getter
@Builder
public class PortfolioBacktestConfig {

    /**
     * Portfolio backtest ID.
     */
    private final String portfolioBacktestId;

    /**
     * Portfolio name.
     */
    private final String portfolioName;

    /**
     * Symbols in portfolio with their weights.
     *
     * Example:
     * {
     *   "005930": 0.4,  // 40% Samsung
     *   "000660": 0.3,  // 30% SK Hynix
     *   "035420": 0.3   // 30% NAVER
     * }
     */
    private final Map<String, BigDecimal> symbolWeights;

    /**
     * Strategy ID to use for all symbols.
     */
    private final String strategyId;

    /**
     * Strategy type for factory creation.
     * Valid values: MA_CROSSOVER, RSI, BOLLINGER_BANDS, MACD
     */
    @Builder.Default
    private final String strategyType = "MA_CROSSOVER";

    /**
     * Strategy parameters.
     */
    private final Map<String, Object> strategyParams;

    /**
     * Start date.
     */
    private final LocalDate startDate;

    /**
     * End date.
     */
    private final LocalDate endDate;

    /**
     * Timeframe (e.g., "1d", "1h").
     */
    @Builder.Default
    private final String timeframe = "1d";

    /**
     * Initial capital for entire portfolio.
     */
    private final BigDecimal initialCapital;

    /**
     * Commission rate (decimal, e.g., 0.0015 for 0.15%).
     */
    @Builder.Default
    private final BigDecimal commission = BigDecimal.valueOf(0.0015);

    /**
     * Slippage rate (decimal).
     */
    @Builder.Default
    private final BigDecimal slippage = BigDecimal.valueOf(0.0005);

    /**
     * Rebalancing frequency (days).
     *
     * 0 = no rebalancing (buy and hold)
     * 30 = monthly rebalancing
     */
    @Builder.Default
    private final int rebalancingFrequencyDays = 0;

    /**
     * Portfolio-level risk limit (max loss per day).
     */
    private final BigDecimal portfolioMaxDailyLoss;
}
