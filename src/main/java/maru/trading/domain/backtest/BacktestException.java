package maru.trading.domain.backtest;

/**
 * Exception thrown during backtest execution.
 */
public class BacktestException extends Exception {

    public BacktestException(String message) {
        super(message);
    }

    public BacktestException(String message, Throwable cause) {
        super(message, cause);
    }
}
