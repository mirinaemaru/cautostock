package maru.trading.api.controller.demo;

import maru.trading.application.backtest.*;
import maru.trading.domain.backtest.BacktestConfig;
import maru.trading.domain.backtest.optimization.OptimizationConfig;
import maru.trading.domain.backtest.portfolio.PortfolioBacktestConfig;
import maru.trading.domain.backtest.portfolio.PortfolioBacktestResult;
import maru.trading.domain.backtest.walkforward.WalkForwardConfig;
import maru.trading.domain.backtest.walkforward.WalkForwardResult;
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
 * Advanced Backtest Demo Controller.
 *
 * Provides demo endpoints for advanced backtest features:
 * - Walk-Forward Analysis
 * - Portfolio Backtesting
 * - Random Search Optimization
 */
@RestController
@RequestMapping("/api/v1/demo/advanced")
@ConditionalOnProperty(
        name = "trading.demo.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class AdvancedBacktestDemoController {

    private static final Logger log = LoggerFactory.getLogger(AdvancedBacktestDemoController.class);

    private final WalkForwardAnalyzer walkForwardAnalyzer;
    private final PortfolioBacktestEngine portfolioEngine;
    private final RandomSearchOptimizer randomSearchOptimizer;
    private final GridSearchOptimizer gridSearchOptimizer;

    public AdvancedBacktestDemoController(
            WalkForwardAnalyzer walkForwardAnalyzer,
            PortfolioBacktestEngine portfolioEngine,
            RandomSearchOptimizer randomSearchOptimizer,
            GridSearchOptimizer gridSearchOptimizer) {
        this.walkForwardAnalyzer = walkForwardAnalyzer;
        this.portfolioEngine = portfolioEngine;
        this.randomSearchOptimizer = randomSearchOptimizer;
        this.gridSearchOptimizer = gridSearchOptimizer;
    }

    /**
     * Run Walk-Forward Analysis demo.
     *
     * POST /api/v1/demo/advanced/walk-forward
     */
    @PostMapping("/walk-forward")
    public ResponseEntity<Map<String, Object>> runWalkForwardDemo() {
        log.info("Running Walk-Forward Analysis Demo");

        try {
            // Base configuration
            BacktestConfig baseConfig = BacktestConfig.builder()
                    .strategyId("MA_CROSS_WF")
                    .symbols(List.of("005930"))
                    .timeframe("1d")
                    .initialCapital(BigDecimal.valueOf(10_000_000))
                    .commission(BigDecimal.valueOf(0.0015))
                    .slippage(BigDecimal.valueOf(0.0005))
                    .build();

            // Optimization configuration
            Map<String, List<Object>> paramRanges = new HashMap<>();
            paramRanges.put("shortPeriod", List.of(5, 10));
            paramRanges.put("longPeriod", List.of(20, 30));

            OptimizationConfig optConfig = OptimizationConfig.builder()
                    .optimizationId(UlidGenerator.generate())
                    .baseConfig(baseConfig)
                    .parameterRanges(paramRanges)
                    .method(OptimizationConfig.OptimizationMethod.GRID_SEARCH)
                    .objective(OptimizationConfig.OptimizationObjective.SHARPE_RATIO)
                    .maxRuns(100)
                    .build();

            // Walk-Forward configuration
            WalkForwardConfig wfConfig = WalkForwardConfig.builder()
                    .walkForwardId(UlidGenerator.generate())
                    .baseConfig(baseConfig)
                    .optimizationConfig(optConfig)
                    .analysisStartDate(LocalDate.of(2024, 1, 1))
                    .analysisEndDate(LocalDate.of(2024, 12, 31))
                    .inSampleDays(180)  // 6 months
                    .outOfSampleDays(90) // 3 months
                    .stepDays(90)        // 3 months
                    .minWindows(2)
                    .build();

            // Run analysis
            WalkForwardResult result = walkForwardAnalyzer.analyze(wfConfig);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("walkForwardId", result.getWalkForwardId());
            response.put("totalWindows", result.getTotalWindows());
            response.put("combinedOutOfSampleReturn", result.getCombinedOutOfSampleReturn());
            response.put("avgOutOfSampleSharpeRatio", result.getAvgOutOfSampleSharpeRatio());
            response.put("stabilityScore", result.getStabilityScore());
            response.put("durationMs", result.getDurationMs());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Walk-Forward demo failed: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Demo failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Run Portfolio Backtest demo.
     *
     * POST /api/v1/demo/advanced/portfolio
     */
    @PostMapping("/portfolio")
    public ResponseEntity<Map<String, Object>> runPortfolioDemo() {
        log.info("Running Portfolio Backtest Demo");

        try {
            // Portfolio configuration
            Map<String, BigDecimal> symbolWeights = new HashMap<>();
            symbolWeights.put("005930", BigDecimal.valueOf(0.4));  // 40% Samsung
            symbolWeights.put("000660", BigDecimal.valueOf(0.3));  // 30% SK Hynix
            symbolWeights.put("035420", BigDecimal.valueOf(0.3));  // 30% NAVER (placeholder)

            Map<String, Object> strategyParams = new HashMap<>();
            strategyParams.put("shortPeriod", 5);
            strategyParams.put("longPeriod", 20);

            PortfolioBacktestConfig config = PortfolioBacktestConfig.builder()
                    .portfolioBacktestId(UlidGenerator.generate())
                    .portfolioName("Korean Tech Portfolio")
                    .symbolWeights(symbolWeights)
                    .strategyId("MA_CROSS_5_20")
                    .strategyParams(strategyParams)
                    .startDate(LocalDate.of(2024, 1, 1))
                    .endDate(LocalDate.of(2024, 12, 31))
                    .timeframe("1d")
                    .initialCapital(BigDecimal.valueOf(10_000_000))
                    .commission(BigDecimal.valueOf(0.0015))
                    .slippage(BigDecimal.valueOf(0.0005))
                    .build();

            // Run portfolio backtest
            PortfolioBacktestResult result = portfolioEngine.run(config);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioBacktestId", result.getPortfolioBacktestId());
            response.put("portfolioName", config.getPortfolioName());
            response.put("totalReturn", result.getTotalReturn());
            response.put("finalCapital", result.getFinalCapital());
            response.put("sharpeRatio", result.getPortfolioMetrics().getSharpeRatio());
            response.put("maxDrawdown", result.getPortfolioMetrics().getMaxDrawdown());
            response.put("symbolsCount", result.getSymbolResults().size());
            response.put("durationMs", result.getDurationMs());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Portfolio demo failed: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Demo failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Run Random Search Optimization demo.
     *
     * POST /api/v1/demo/advanced/random-search
     */
    @PostMapping("/random-search")
    public ResponseEntity<Map<String, Object>> runRandomSearchDemo() {
        log.info("Running Random Search Optimization Demo");

        try {
            // Define parameter ranges (larger space than grid search)
            Map<String, List<Object>> parameterRanges = new HashMap<>();
            parameterRanges.put("shortPeriod", List.of(3, 5, 7, 10, 12, 15));
            parameterRanges.put("longPeriod", List.of(15, 20, 25, 30, 40, 50));

            // Base configuration
            BacktestConfig baseConfig = BacktestConfig.builder()
                    .strategyId("MA_CROSS_RANDOM")
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
                    .method(OptimizationConfig.OptimizationMethod.RANDOM_SEARCH)
                    .objective(OptimizationConfig.OptimizationObjective.SHARPE_RATIO)
                    .maxRuns(20)  // Test only 20 random combinations (vs 36 total)
                    .build();

            // Run optimization
            var result = randomSearchOptimizer.optimize(optConfig);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("optimizationId", result.getOptimizationId());
            response.put("method", "RANDOM_SEARCH");
            response.put("bestParameters", result.getBestParameters());
            response.put("bestObjectiveValue", result.getBestObjectiveValue());
            response.put("totalRuns", result.getTotalRuns());
            response.put("durationMs", result.getDurationMs());
            response.put("bestBacktest", Map.of(
                    "totalReturn", result.getBestBacktestResult().getTotalReturn(),
                    "sharpeRatio", result.getBestBacktestResult().getPerformanceMetrics().getSharpeRatio(),
                    "totalTrades", result.getBestBacktestResult().getTrades().size()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Random Search demo failed: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Demo failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
