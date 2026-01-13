package maru.trading.domain.backtest.optimization;

/**
 * Exception thrown during parameter optimization.
 */
public class OptimizationException extends Exception {

    public OptimizationException(String message) {
        super(message);
    }

    public OptimizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
