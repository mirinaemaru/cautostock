package maru.trading.api.controller.demo;

import maru.trading.api.dto.response.BacktestResponse;
import maru.trading.demo.BacktestDataGenerator;
import maru.trading.domain.backtest.BacktestConfig;
import maru.trading.domain.backtest.BacktestEngine;
import maru.trading.domain.backtest.BacktestResult;
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
 * Backtest Demo Controller.
 *
 * Provides demo endpoints for quick backtest testing.
 *
 * IMPORTANT: Only enabled in demo/development environments.
 */
@RestController
@RequestMapping("/api/v1/demo/backtest")
@ConditionalOnProperty(
        name = "trading.demo.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class BacktestDemoController {

    private static final Logger log = LoggerFactory.getLogger(BacktestDemoController.class);

    private final BacktestDataGenerator dataGenerator;
    private final BacktestEngine backtestEngine;

    public BacktestDemoController(BacktestDataGenerator dataGenerator, BacktestEngine backtestEngine) {
        this.dataGenerator = dataGenerator;
        this.backtestEngine = backtestEngine;
    }

    /**
     * Generate demo dataset.
     *
     * POST /api/v1/demo/backtest/generate-data
     */
    @PostMapping("/generate-data")
    public ResponseEntity<Map<String, Object>> generateDemoData() {
        log.info("Generating demo backtest dataset...");

        dataGenerator.generateDemoDataset();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Demo dataset generated successfully");
        response.put("symbols", List.of("005930", "000660"));
        response.put("period", "2024-01-01 to 2024-12-31");
        response.put("pattern", Map.of(
            "005930", "Trending market (uptrend/downtrend cycles)",
            "000660", "Ranging market (oscillating)"
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Run MA Crossover strategy backtest demo.
     *
     * POST /api/v1/demo/backtest/ma-crossover
     */
    @PostMapping("/ma-crossover")
    public ResponseEntity<BacktestResponse> runMACrossoverDemo() {
        log.info("========================================");
        log.info("Running MA Crossover Backtest Demo");
        log.info("========================================");

        try {
            // Configuration
            Map<String, Object> params = new HashMap<>();
            params.put("shortPeriod", 5);
            params.put("longPeriod", 20);

            BacktestConfig config = BacktestConfig.builder()
                    .backtestId(UlidGenerator.generate())
                    .strategyId("MA_CROSS_5_20")
                    .symbols(List.of("005930"))
                    .startDate(LocalDate.of(2024, 1, 1))
                    .endDate(LocalDate.of(2024, 12, 31))
                    .timeframe("1d")
                    .initialCapital(BigDecimal.valueOf(10_000_000)) // 10M KRW
                    .commission(BigDecimal.valueOf(0.0015)) // 0.15%
                    .slippage(BigDecimal.valueOf(0.0005)) // 0.05%
                    .strategyParams(params)
                    .build();

            // Run backtest
            BacktestResult result = backtestEngine.run(config);

            // Return response
            BacktestResponse response = BacktestResponse.fromDomain(result);

            log.info("========================================");
            log.info("MA Crossover Backtest Complete");
            log.info("========================================");
            log.info("Total Return: {}%", result.getTotalReturn());
            log.info("Total Trades: {}", result.getTrades().size());
            log.info("Sharpe Ratio: {}", result.getPerformanceMetrics().getSharpeRatio());
            log.info("Max Drawdown: {}%", result.getPerformanceMetrics().getMaxDrawdown());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("MA Crossover demo failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(BacktestResponse.error("Demo failed: " + e.getMessage()));
        }
    }

    /**
     * Run RSI strategy backtest demo.
     *
     * POST /api/v1/demo/backtest/rsi
     */
    @PostMapping("/rsi")
    public ResponseEntity<BacktestResponse> runRSIDemo() {
        log.info("========================================");
        log.info("Running RSI Backtest Demo");
        log.info("========================================");

        try {
            // Configuration
            Map<String, Object> params = new HashMap<>();
            params.put("period", 14);
            params.put("overbought", 70);
            params.put("oversold", 30);

            BacktestConfig config = BacktestConfig.builder()
                    .backtestId(UlidGenerator.generate())
                    .strategyId("RSI_14_30_70")
                    .symbols(List.of("000660"))
                    .startDate(LocalDate.of(2024, 1, 1))
                    .endDate(LocalDate.of(2024, 12, 31))
                    .timeframe("1d")
                    .initialCapital(BigDecimal.valueOf(10_000_000))
                    .commission(BigDecimal.valueOf(0.0015))
                    .slippage(BigDecimal.valueOf(0.0005))
                    .strategyParams(params)
                    .build();

            // Run backtest
            BacktestResult result = backtestEngine.run(config);

            // Return response
            BacktestResponse response = BacktestResponse.fromDomain(result);

            log.info("========================================");
            log.info("RSI Backtest Complete");
            log.info("========================================");
            log.info("Total Return: {}%", result.getTotalReturn());
            log.info("Total Trades: {}", result.getTrades().size());
            log.info("Win Rate: {}%", result.getPerformanceMetrics().getWinRate());
            log.info("Profit Factor: {}", result.getPerformanceMetrics().getProfitFactor());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("RSI demo failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(BacktestResponse.error("Demo failed: " + e.getMessage()));
        }
    }

    /**
     * Run comparison backtest (both strategies on same data).
     *
     * POST /api/v1/demo/backtest/compare
     */
    @PostMapping("/compare")
    public ResponseEntity<Map<String, BacktestResponse>> runComparisonDemo() {
        log.info("========================================");
        log.info("Running Strategy Comparison Demo");
        log.info("========================================");

        try {
            String symbol = "005930";
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 12, 31);
            BigDecimal initialCapital = BigDecimal.valueOf(10_000_000);

            // MA Crossover backtest
            Map<String, Object> maParams = new HashMap<>();
            maParams.put("shortPeriod", 5);
            maParams.put("longPeriod", 20);

            BacktestConfig maConfig = BacktestConfig.builder()
                    .backtestId(UlidGenerator.generate())
                    .strategyId("MA_CROSS_5_20")
                    .symbols(List.of(symbol))
                    .startDate(startDate)
                    .endDate(endDate)
                    .timeframe("1d")
                    .initialCapital(initialCapital)
                    .commission(BigDecimal.valueOf(0.0015))
                    .slippage(BigDecimal.valueOf(0.0005))
                    .strategyParams(maParams)
                    .build();

            BacktestResult maResult = backtestEngine.run(maConfig);

            // RSI backtest
            Map<String, Object> rsiParams = new HashMap<>();
            rsiParams.put("period", 14);
            rsiParams.put("overbought", 70);
            rsiParams.put("oversold", 30);

            BacktestConfig rsiConfig = BacktestConfig.builder()
                    .backtestId(UlidGenerator.generate())
                    .strategyId("RSI_14_30_70")
                    .symbols(List.of(symbol))
                    .startDate(startDate)
                    .endDate(endDate)
                    .timeframe("1d")
                    .initialCapital(initialCapital)
                    .commission(BigDecimal.valueOf(0.0015))
                    .slippage(BigDecimal.valueOf(0.0005))
                    .strategyParams(rsiParams)
                    .build();

            BacktestResult rsiResult = backtestEngine.run(rsiConfig);

            // Build response
            Map<String, BacktestResponse> response = new HashMap<>();
            response.put("MA_Crossover", BacktestResponse.fromDomain(maResult));
            response.put("RSI", BacktestResponse.fromDomain(rsiResult));

            log.info("========================================");
            log.info("Strategy Comparison Complete");
            log.info("========================================");
            log.info("MA Crossover - Return: {}%, Trades: {}, Sharpe: {}",
                    maResult.getTotalReturn(), maResult.getTrades().size(),
                    maResult.getPerformanceMetrics().getSharpeRatio());
            log.info("RSI - Return: {}%, Trades: {}, Sharpe: {}",
                    rsiResult.getTotalReturn(), rsiResult.getTrades().size(),
                    rsiResult.getPerformanceMetrics().getSharpeRatio());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Comparison demo failed: {}", e.getMessage(), e);
            Map<String, BacktestResponse> errorResponse = new HashMap<>();
            errorResponse.put("error", BacktestResponse.error("Demo failed: " + e.getMessage()));
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Clear all backtest data.
     *
     * DELETE /api/v1/demo/backtest/clear
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clearData() {
        log.info("Clearing demo backtest data...");

        dataGenerator.clearHistoricalData();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Demo backtest data cleared successfully");

        return ResponseEntity.ok(response);
    }
}
