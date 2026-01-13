package maru.trading.api.controller.demo;

import maru.trading.application.backtest.GridSearchOptimizer;
import maru.trading.domain.backtest.BacktestConfig;
import maru.trading.domain.backtest.optimization.OptimizationConfig;
import maru.trading.domain.backtest.optimization.OptimizationResult;
import maru.trading.infra.config.UlidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parameter Optimization Demo Controller.
 *
 * Provides demo endpoints for parameter optimization.
 */
@RestController
@RequestMapping("/api/v1/demo/optimization")
@ConditionalOnProperty(
        name = "trading.demo.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OptimizationDemoController {

    private static final Logger log = LoggerFactory.getLogger(OptimizationDemoController.class);

    private final GridSearchOptimizer gridSearchOptimizer;

    public OptimizationDemoController(GridSearchOptimizer gridSearchOptimizer) {
        this.gridSearchOptimizer = gridSearchOptimizer;
    }

    /**
     * Run MA Crossover parameter optimization.
     *
     * POST /api/v1/demo/optimization/ma-crossover
     */
    @PostMapping("/ma-crossover")
    public ResponseEntity<Map<String, Object>> optimizeMACrossover() {
        log.info("========================================");
        log.info("Running MA Crossover Parameter Optimization");
        log.info("========================================");

        try {
            // Define parameter ranges
            Map<String, List<Object>> parameterRanges = new HashMap<>();
            parameterRanges.put("shortPeriod", List.of(5, 10, 15));
            parameterRanges.put("longPeriod", List.of(20, 30, 50));

            // Base configuration
            BacktestConfig baseConfig = BacktestConfig.builder()
                    .strategyId("MA_CROSS_OPTIMIZED")
                    .symbols(List.of("005930"))
                    .startDate(LocalDate.of(2024, 1, 1))
                    .endDate(LocalDate.of(2024, 12, 31))
                    .timeframe("1d")
                    .initialCapital(BigDecimal.valueOf(10_000_000))
                    .commission(BigDecimal.valueOf(0.0015))
                    .slippage(BigDecimal.valueOf(0.0005))
                    .build();

            // Optimization configuration
            OptimizationConfig optConfig = OptimizationConfig.builder()
                    .optimizationId(UlidGenerator.generate())
                    .baseConfig(baseConfig)
                    .parameterRanges(parameterRanges)
                    .method(OptimizationConfig.OptimizationMethod.GRID_SEARCH)
                    .objective(OptimizationConfig.OptimizationObjective.SHARPE_RATIO)
                    .maxRuns(100)
                    .build();

            // Run optimization
            OptimizationResult result = gridSearchOptimizer.optimize(optConfig);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("optimizationId", result.getOptimizationId());
            response.put("bestParameters", result.getBestParameters());
            response.put("bestObjectiveValue", result.getBestObjectiveValue());
            response.put("totalRuns", result.getTotalRuns());
            response.put("durationMs", result.getDurationMs());
            response.put("bestBacktest", Map.of(
                    "totalReturn", result.getBestBacktestResult().getTotalReturn(),
                    "sharpeRatio", result.getBestBacktestResult().getPerformanceMetrics().getSharpeRatio(),
                    "maxDrawdown", result.getBestBacktestResult().getPerformanceMetrics().getMaxDrawdown(),
                    "totalTrades", result.getBestBacktestResult().getTrades().size()
            ));

            log.info("========================================");
            log.info("MA Crossover Optimization Complete");
            log.info("========================================");
            log.info("Best parameters: {}", result.getBestParameters());
            log.info("Best Sharpe Ratio: {}", result.getBestObjectiveValue());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("MA Crossover optimization failed: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Optimization failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Run RSI parameter optimization.
     *
     * POST /api/v1/demo/optimization/rsi
     */
    @PostMapping("/rsi")
    public ResponseEntity<Map<String, Object>> optimizeRSI() {
        log.info("========================================");
        log.info("Running RSI Parameter Optimization");
        log.info("========================================");

        try {
            // Define parameter ranges
            Map<String, List<Object>> parameterRanges = new HashMap<>();
            parameterRanges.put("period", List.of(10, 14, 20));
            parameterRanges.put("overbought", List.of(65, 70, 75));
            parameterRanges.put("oversold", List.of(25, 30, 35));

            // Base configuration
            BacktestConfig baseConfig = BacktestConfig.builder()
                    .strategyId("RSI_OPTIMIZED")
                    .symbols(List.of("000660"))
                    .startDate(LocalDate.of(2024, 1, 1))
                    .endDate(LocalDate.of(2024, 12, 31))
                    .timeframe("1d")
                    .initialCapital(BigDecimal.valueOf(10_000_000))
                    .commission(BigDecimal.valueOf(0.0015))
                    .slippage(BigDecimal.valueOf(0.0005))
                    .build();

            // Optimization configuration
            OptimizationConfig optConfig = OptimizationConfig.builder()
                    .optimizationId(UlidGenerator.generate())
                    .baseConfig(baseConfig)
                    .parameterRanges(parameterRanges)
                    .method(OptimizationConfig.OptimizationMethod.GRID_SEARCH)
                    .objective(OptimizationConfig.OptimizationObjective.PROFIT_FACTOR)
                    .maxRuns(100)
                    .build();

            // Run optimization
            OptimizationResult result = gridSearchOptimizer.optimize(optConfig);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("optimizationId", result.getOptimizationId());
            response.put("bestParameters", result.getBestParameters());
            response.put("bestObjectiveValue", result.getBestObjectiveValue());
            response.put("totalRuns", result.getTotalRuns());
            response.put("durationMs", result.getDurationMs());
            response.put("bestBacktest", Map.of(
                    "totalReturn", result.getBestBacktestResult().getTotalReturn(),
                    "profitFactor", result.getBestBacktestResult().getPerformanceMetrics().getProfitFactor(),
                    "winRate", result.getBestBacktestResult().getPerformanceMetrics().getWinRate(),
                    "totalTrades", result.getBestBacktestResult().getTrades().size()
            ));

            log.info("========================================");
            log.info("RSI Optimization Complete");
            log.info("========================================");
            log.info("Best parameters: {}", result.getBestParameters());
            log.info("Best Profit Factor: {}", result.getBestObjectiveValue());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("RSI optimization failed: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Optimization failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
