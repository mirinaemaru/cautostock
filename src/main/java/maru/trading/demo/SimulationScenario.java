package maru.trading.demo;

/**
 * Market data simulation scenarios.
 */
public enum SimulationScenario {
    /**
     * Golden Cross: MA5 crosses above MA20 (bullish signal).
     * Pattern: Stable → Downtrend → Strong Uptrend
     */
    GOLDEN_CROSS,

    /**
     * Death Cross: MA5 crosses below MA20 (bearish signal).
     * Pattern: Stable → Uptrend → Crash
     */
    DEATH_CROSS,

    /**
     * RSI Oversold: RSI crosses below 30 (buy signal).
     * Pattern: Stable → Decline → Bounce → Drop
     */
    RSI_OVERSOLD,

    /**
     * RSI Overbought: RSI crosses above 70 (sell signal).
     * Pattern: Stable → Rise → Correction → Surge
     */
    RSI_OVERBOUGHT,

    /**
     * Volatile: Random volatile price movements (±5%).
     */
    VOLATILE,

    /**
     * Stable: Stable price with small fluctuations (±0.1%).
     */
    STABLE
}
