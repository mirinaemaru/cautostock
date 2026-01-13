package maru.trading.domain.backtest.optimization;

/**
 * Interface for parameter optimization strategies.
 */
public interface ParameterOptimizer {

    /**
     * Run parameter optimization.
     *
     * @param config Optimization configuration
     * @return Optimization result with best parameters
     * @throws OptimizationException if optimization fails
     */
    OptimizationResult optimize(OptimizationConfig config) throws OptimizationException;
}
