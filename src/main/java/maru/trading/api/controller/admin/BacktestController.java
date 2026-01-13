package maru.trading.api.controller.admin;

import maru.trading.api.dto.request.BacktestRequest;
import maru.trading.api.dto.response.BacktestResponse;
import maru.trading.api.dto.response.BacktestSummaryResponse;
import maru.trading.api.dto.response.BacktestTradeResponse;
import maru.trading.domain.backtest.BacktestConfig;
import maru.trading.domain.backtest.BacktestEngine;
import maru.trading.domain.backtest.BacktestException;
import maru.trading.domain.backtest.BacktestResult;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.BacktestRunEntity;
import maru.trading.infra.persistence.jpa.entity.BacktestTradeEntity;
import maru.trading.infra.persistence.jpa.repository.BacktestRunJpaRepository;
import maru.trading.infra.persistence.jpa.repository.BacktestTradeJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Backtest Admin Controller.
 *
 * Provides REST API endpoints for running and analyzing backtests.
 *
 * Endpoints:
 * - POST   /api/v1/admin/backtests           - Run a new backtest
 * - GET    /api/v1/admin/backtests           - List all backtests
 * - GET    /api/v1/admin/backtests/{id}      - Get backtest result
 * - GET    /api/v1/admin/backtests/{id}/trades - Get backtest trades
 * - DELETE /api/v1/admin/backtests/{id}      - Delete backtest
 */
@RestController
@RequestMapping("/api/v1/admin/backtests")
public class BacktestController {

    private static final Logger log = LoggerFactory.getLogger(BacktestController.class);

    private final BacktestEngine backtestEngine;
    private final BacktestRunJpaRepository backtestRunRepository;
    private final BacktestTradeJpaRepository backtestTradeRepository;

    public BacktestController(
            BacktestEngine backtestEngine,
            BacktestRunJpaRepository backtestRunRepository,
            BacktestTradeJpaRepository backtestTradeRepository) {
        this.backtestEngine = backtestEngine;
        this.backtestRunRepository = backtestRunRepository;
        this.backtestTradeRepository = backtestTradeRepository;
    }

    /**
     * Run a new backtest.
     *
     * POST /api/v1/admin/backtests
     *
     * Request body:
     * {
     *   "strategyId": "MA_CROSS_5_20",
     *   "symbols": ["005930", "035720"],
     *   "startDate": "2024-01-01",
     *   "endDate": "2024-12-31",
     *   "timeframe": "1d",
     *   "initialCapital": 10000000,
     *   "commission": 0.0015,
     *   "slippage": 0.0005,
     *   "strategyParams": {
     *     "shortPeriod": 5,
     *     "longPeriod": 20
     *   }
     * }
     */
    @PostMapping
    public ResponseEntity<BacktestResponse> runBacktest(@RequestBody BacktestRequest request) {
        log.info("Received backtest request: strategy={}, symbols={}, period={} to {}",
                request.getStrategyId(), request.getSymbols(), request.getStartDate(), request.getEndDate());

        try {
            // Validate request
            validateBacktestRequest(request);

            // Create backtest config
            BacktestConfig config = BacktestConfig.builder()
                    .backtestId(UlidGenerator.generate())
                    .strategyId(request.getStrategyId())
                    .symbols(request.getSymbols())
                    .startDate(LocalDate.parse(request.getStartDate()))
                    .endDate(LocalDate.parse(request.getEndDate()))
                    .timeframe(request.getTimeframe() != null ? request.getTimeframe() : "1d")
                    .initialCapital(request.getInitialCapital())
                    .commission(request.getCommission() != null ? request.getCommission() : java.math.BigDecimal.valueOf(0.0015))
                    .slippage(request.getSlippage() != null ? request.getSlippage() : java.math.BigDecimal.valueOf(0.0005))
                    .strategyParams(request.getStrategyParams() != null ? request.getStrategyParams() : new HashMap<>())
                    .build();

            // Run backtest
            BacktestResult result = backtestEngine.run(config);

            // Build response
            BacktestResponse response = BacktestResponse.fromDomain(result);

            log.info("Backtest completed successfully: backtestId={}, totalReturn={}%, trades={}",
                    config.getBacktestId(), result.getTotalReturn(), result.getTrades().size());

            return ResponseEntity.ok(response);

        } catch (BacktestException e) {
            log.error("Backtest failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BacktestResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during backtest: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BacktestResponse.error("Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * List all backtests.
     *
     * GET /api/v1/admin/backtests
     */
    @GetMapping
    public ResponseEntity<List<BacktestSummaryResponse>> listBacktests() {
        log.info("Listing all backtests");

        List<BacktestRunEntity> runs = backtestRunRepository.findAll();
        List<BacktestSummaryResponse> summaries = runs.stream()
                .map(BacktestSummaryResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(summaries);
    }

    /**
     * Get backtest result by ID.
     *
     * GET /api/v1/admin/backtests/{backtestId}
     */
    @GetMapping("/{backtestId}")
    public ResponseEntity<BacktestResponse> getBacktest(@PathVariable String backtestId) {
        log.info("Retrieving backtest: backtestId={}", backtestId);

        return backtestRunRepository.findById(backtestId)
                .map(entity -> {
                    BacktestResponse response = BacktestResponse.fromEntity(entity);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    log.warn("Backtest not found: backtestId={}", backtestId);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(BacktestResponse.error("Backtest not found: " + backtestId));
                });
    }

    /**
     * Get backtest trades.
     *
     * GET /api/v1/admin/backtests/{backtestId}/trades
     */
    @GetMapping("/{backtestId}/trades")
    public ResponseEntity<List<BacktestTradeResponse>> getBacktestTrades(@PathVariable String backtestId) {
        log.info("Retrieving backtest trades: backtestId={}", backtestId);

        // Check if backtest exists
        if (!backtestRunRepository.existsById(backtestId)) {
            log.warn("Backtest not found: backtestId={}", backtestId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<BacktestTradeEntity> trades = backtestTradeRepository.findByBacktestIdOrderByEntryTimeAsc(backtestId);
        List<BacktestTradeResponse> responses = trades.stream()
                .map(BacktestTradeResponse::fromEntity)
                .toList();

        log.info("Found {} trades for backtestId={}", responses.size(), backtestId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Delete backtest.
     *
     * DELETE /api/v1/admin/backtests/{backtestId}
     */
    @DeleteMapping("/{backtestId}")
    public ResponseEntity<Map<String, String>> deleteBacktest(@PathVariable String backtestId) {
        log.info("Deleting backtest: backtestId={}", backtestId);

        if (!backtestRunRepository.existsById(backtestId)) {
            log.warn("Backtest not found: backtestId={}", backtestId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Delete trades first (foreign key constraint)
        backtestTradeRepository.deleteByBacktestId(backtestId);

        // Delete backtest run
        backtestRunRepository.deleteById(backtestId);

        log.info("Backtest deleted: backtestId={}", backtestId);
        return ResponseEntity.ok(Map.of("message", "Backtest deleted successfully", "backtestId", backtestId));
    }

    /**
     * Validate backtest request.
     */
    private void validateBacktestRequest(BacktestRequest request) {
        if (request.getStrategyId() == null || request.getStrategyId().isBlank()) {
            throw new IllegalArgumentException("Strategy ID is required");
        }
        if (request.getSymbols() == null || request.getSymbols().isEmpty()) {
            throw new IllegalArgumentException("At least one symbol is required");
        }
        if (request.getStartDate() == null || request.getStartDate().isBlank()) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (request.getEndDate() == null || request.getEndDate().isBlank()) {
            throw new IllegalArgumentException("End date is required");
        }
        if (request.getInitialCapital() == null || request.getInitialCapital().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Initial capital must be positive");
        }

        LocalDate start = LocalDate.parse(request.getStartDate());
        LocalDate end = LocalDate.parse(request.getEndDate());
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
    }
}
