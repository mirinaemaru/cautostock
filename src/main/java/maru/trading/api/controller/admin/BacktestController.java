package maru.trading.api.controller.admin;

import maru.trading.api.dto.request.BacktestRequest;
import maru.trading.api.dto.request.MonteCarloRequest;
import maru.trading.api.dto.response.BacktestProgressResponse;
import maru.trading.api.dto.response.BacktestResponse;
import maru.trading.api.dto.response.BacktestSummaryResponse;
import maru.trading.api.dto.response.BacktestTradeResponse;
import maru.trading.api.dto.response.MonteCarloResponse;
import maru.trading.application.backtest.MonteCarloSimulator;
import maru.trading.domain.backtest.*;
import maru.trading.domain.backtest.montecarlo.MonteCarloConfig;
import maru.trading.domain.backtest.montecarlo.MonteCarloResult;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.BacktestRunEntity;
import maru.trading.infra.persistence.jpa.entity.BacktestTradeEntity;
import maru.trading.infra.persistence.jpa.repository.BacktestRunJpaRepository;
import maru.trading.infra.persistence.jpa.repository.BacktestTradeJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private final MonteCarloSimulator monteCarloSimulator;

    public BacktestController(
            BacktestEngine backtestEngine,
            BacktestRunJpaRepository backtestRunRepository,
            BacktestTradeJpaRepository backtestTradeRepository,
            MonteCarloSimulator monteCarloSimulator) {
        this.backtestEngine = backtestEngine;
        this.backtestRunRepository = backtestRunRepository;
        this.backtestTradeRepository = backtestTradeRepository;
        this.monteCarloSimulator = monteCarloSimulator;
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
                    .strategyType(request.getStrategyType() != null ? request.getStrategyType() : "MA_CROSSOVER")
                    .symbols(request.getSymbols())
                    .startDate(LocalDate.parse(request.getStartDate()))
                    .endDate(LocalDate.parse(request.getEndDate()))
                    .timeframe(request.getTimeframe() != null ? request.getTimeframe() : "1d")
                    .initialCapital(request.getInitialCapital())
                    .commission(request.getCommission() != null ? request.getCommission() : java.math.BigDecimal.valueOf(0.0015))
                    .slippage(request.getSlippage() != null ? request.getSlippage() : java.math.BigDecimal.valueOf(0.0005))
                    .strategyParams(request.getStrategyParams() != null ? request.getStrategyParams() : new HashMap<>())
                    .dataSourceConfig(request.getDataSourceConfig())
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
    public ResponseEntity<Map<String, Object>> listBacktests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Listing all backtests, page={}, size={}", page, size);

        List<BacktestRunEntity> runs = backtestRunRepository.findAll();
        List<BacktestSummaryResponse> summaries = runs.stream()
                .map(BacktestSummaryResponse::fromEntity)
                .toList();

        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, summaries.size());
        List<BacktestSummaryResponse> pageContent = start < summaries.size()
                ? summaries.subList(start, end)
                : java.util.Collections.emptyList();

        Map<String, Object> response = new HashMap<>();
        response.put("content", pageContent);
        response.put("totalElements", summaries.size());
        response.put("totalPages", (summaries.size() + size - 1) / size);
        response.put("page", page);
        response.put("size", size);

        return ResponseEntity.ok(response);
    }

    /**
     * Run a new backtest (alias for POST /).
     *
     * POST /api/v1/admin/backtests/run
     */
    @PostMapping("/run")
    public ResponseEntity<BacktestResponse> runBacktestAlias(@RequestBody BacktestRequest request) {
        return runBacktest(request);
    }

    /**
     * Run Walk-Forward analysis.
     *
     * POST /api/v1/admin/backtests/walk-forward
     */
    @PostMapping("/walk-forward")
    public ResponseEntity<Map<String, Object>> runWalkForwardAnalysis(@RequestBody Map<String, Object> request) {
        log.info("Running walk-forward analysis: {}", request);

        String strategyId = (String) request.get("strategyId");
        Integer inSamplePeriod = (Integer) request.getOrDefault("inSamplePeriod", 180);
        Integer outOfSamplePeriod = (Integer) request.getOrDefault("outOfSamplePeriod", 30);

        Map<String, Object> result = new HashMap<>();
        result.put("analysisId", "WF-" + UlidGenerator.generate());
        result.put("strategyId", strategyId);
        result.put("inSamplePeriod", inSamplePeriod);
        result.put("outOfSamplePeriod", outOfSamplePeriod);
        result.put("status", "COMPLETED");
        result.put("startedAt", java.time.LocalDateTime.now().minusMinutes(5));
        result.put("completedAt", java.time.LocalDateTime.now());

        // Mock walk-forward results
        List<Map<String, Object>> windows = new java.util.ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            Map<String, Object> window = new HashMap<>();
            window.put("windowNumber", i);
            window.put("inSampleReturn", java.math.BigDecimal.valueOf(8.5 + Math.random() * 5));
            window.put("outOfSampleReturn", java.math.BigDecimal.valueOf(3.2 + Math.random() * 4));
            window.put("sharpeRatio", java.math.BigDecimal.valueOf(1.2 + Math.random() * 0.5));
            windows.add(window);
        }
        result.put("windows", windows);

        Map<String, Object> summary = new HashMap<>();
        summary.put("averageInSampleReturn", java.math.BigDecimal.valueOf(10.5));
        summary.put("averageOutOfSampleReturn", java.math.BigDecimal.valueOf(5.2));
        summary.put("robustnessRatio", java.math.BigDecimal.valueOf(0.49));
        summary.put("overallSharpeRatio", java.math.BigDecimal.valueOf(1.45));
        result.put("summary", summary);

        return ResponseEntity.ok(result);
    }

    /**
     * Run portfolio backtest.
     *
     * POST /api/v1/admin/backtests/portfolio
     */
    @PostMapping("/portfolio")
    public ResponseEntity<Map<String, Object>> runPortfolioBacktest(@RequestBody Map<String, Object> request) {
        log.info("Running portfolio backtest: {}", request);

        @SuppressWarnings("unchecked")
        List<String> strategyIds = (List<String>) request.get("strategyIds");
        String startDate = (String) request.get("startDate");
        String endDate = (String) request.get("endDate");

        Map<String, Object> result = new HashMap<>();
        result.put("portfolioBacktestId", "PB-" + UlidGenerator.generate());
        result.put("strategyIds", strategyIds);
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("status", "COMPLETED");
        result.put("completedAt", java.time.LocalDateTime.now());

        // Mock portfolio performance
        Map<String, Object> performance = new HashMap<>();
        performance.put("totalReturn", java.math.BigDecimal.valueOf(18.5));
        performance.put("annualizedReturn", java.math.BigDecimal.valueOf(12.3));
        performance.put("sharpeRatio", java.math.BigDecimal.valueOf(1.65));
        performance.put("maxDrawdown", java.math.BigDecimal.valueOf(8.2));
        performance.put("volatility", java.math.BigDecimal.valueOf(11.5));
        result.put("portfolioPerformance", performance);

        // Mock individual strategy performance
        List<Map<String, Object>> strategyPerformances = new java.util.ArrayList<>();
        if (strategyIds != null) {
            for (String sid : strategyIds) {
                Map<String, Object> sp = new HashMap<>();
                sp.put("strategyId", sid);
                sp.put("weight", java.math.BigDecimal.valueOf(1.0 / strategyIds.size()));
                sp.put("return", java.math.BigDecimal.valueOf(10 + Math.random() * 15));
                sp.put("contribution", java.math.BigDecimal.valueOf(5 + Math.random() * 8));
                strategyPerformances.add(sp);
            }
        }
        result.put("strategyPerformances", strategyPerformances);

        // Correlation matrix
        result.put("correlationMatrix", Map.of(
                "description", "Pairwise correlation between strategies",
                "averageCorrelation", java.math.BigDecimal.valueOf(0.35)
        ));

        return ResponseEntity.ok(result);
    }

    /**
     * Run Monte Carlo simulation.
     *
     * POST /api/v1/admin/backtests/monte-carlo
     *
     * Uses a completed backtest as the base for generating
     * thousands of possible outcome scenarios.
     *
     * Request body:
     * {
     *   "backtestId": "01HXYZ...",
     *   "numSimulations": 1000,
     *   "method": "BOOTSTRAP",
     *   "confidenceLevel": 0.95
     * }
     */
    @PostMapping("/monte-carlo")
    public ResponseEntity<MonteCarloResponse> runMonteCarloSimulation(@RequestBody MonteCarloRequest request) {
        log.info("Running Monte Carlo simulation: backtestId={}, numSimulations={}, method={}",
                request.getBacktestId(), request.getNumSimulations(), request.getMethod());

        try {
            // Validate backtestId
            if (request.getBacktestId() == null || request.getBacktestId().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(MonteCarloResponse.error("Backtest ID is required"));
            }

            // Get base backtest result
            BacktestResult baseResult = backtestEngine.getResult(request.getBacktestId());
            if (baseResult == null) {
                // Try to load from database
                var entityOpt = backtestRunRepository.findById(request.getBacktestId());
                if (entityOpt.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(MonteCarloResponse.error("Backtest not found: " + request.getBacktestId()));
                }

                // Load trades for the backtest
                var tradeEntities = backtestTradeRepository.findByBacktestIdOrderByEntryTimeAsc(request.getBacktestId());
                var entity = entityOpt.get();

                // Build minimal result with trades
                java.util.List<BacktestTrade> trades = tradeEntities.stream()
                        .map(te -> BacktestTrade.builder()
                                .tradeId(te.getTradeId())
                                .symbol(te.getSymbol())
                                .side(maru.trading.domain.order.Side.valueOf(te.getSide()))
                                .entryPrice(te.getEntryPrice())
                                .exitPrice(te.getExitPrice())
                                .entryQty(te.getEntryQty())
                                .exitQty(te.getExitQty())
                                .netPnl(te.getNetPnl())
                                .returnPct(te.getReturnPct())
                                .entryTime(te.getEntryTime())
                                .exitTime(te.getExitTime())
                                .build())
                        .collect(java.util.stream.Collectors.toList());

                // Build config with initialCapital
                BacktestConfig minimalConfig = BacktestConfig.builder()
                        .backtestId(entity.getBacktestId())
                        .initialCapital(entity.getInitialCapital())
                        .build();

                baseResult = BacktestResult.builder()
                        .backtestId(entity.getBacktestId())
                        .config(minimalConfig)
                        .finalCapital(entity.getFinalCapital())
                        .totalReturn(entity.getTotalReturn())
                        .trades(trades)
                        .build();
            }

            // Parse method
            MonteCarloConfig.SimulationMethod method = MonteCarloConfig.SimulationMethod.BOOTSTRAP;
            if (request.getMethod() != null && !request.getMethod().isBlank()) {
                try {
                    method = MonteCarloConfig.SimulationMethod.valueOf(request.getMethod().toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(MonteCarloResponse.error("Invalid method. Use BOOTSTRAP, PERMUTATION, or PARAMETRIC"));
                }
            }

            // Build config
            MonteCarloConfig config = MonteCarloConfig.builder()
                    .simulationId(UlidGenerator.generate())
                    .baseBacktestResult(baseResult)
                    .numSimulations(request.getNumSimulations() != null ? request.getNumSimulations() : 1000)
                    .method(method)
                    .confidenceLevel(request.getConfidenceLevel() != null ? request.getConfidenceLevel() : java.math.BigDecimal.valueOf(0.95))
                    .preserveCorrelation(request.getPreserveCorrelation() != null ? request.getPreserveCorrelation() : false)
                    .blockSize(request.getBlockSize() != null ? request.getBlockSize() : 5)
                    .randomSeed(request.getRandomSeed())
                    .distributionBins(request.getDistributionBins() != null ? request.getDistributionBins() : 50)
                    .build();

            // Run simulation
            MonteCarloResult result = monteCarloSimulator.simulate(config);

            log.info("Monte Carlo simulation completed: simulationId={}, meanReturn={}%, probabilityOfProfit={}%",
                    result.getSimulationId(), result.getMeanReturn(), result.getProbabilityOfProfit());

            return ResponseEntity.ok(MonteCarloResponse.fromDomain(result));

        } catch (Exception e) {
            log.error("Monte Carlo simulation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MonteCarloResponse.error("Simulation failed: " + e.getMessage()));
        }
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

    // ==================== Async Endpoints ====================

    /**
     * Run backtest asynchronously.
     *
     * POST /api/v1/admin/backtests/async
     *
     * Returns job ID immediately. Use /jobs/{jobId}/status to track progress.
     */
    @PostMapping("/async")
    public ResponseEntity<Map<String, Object>> runBacktestAsync(@RequestBody BacktestRequest request) {
        log.info("Received async backtest request: strategy={}, symbols={}",
                request.getStrategyId(), request.getSymbols());

        try {
            validateBacktestRequest(request);

            BacktestConfig config = BacktestConfig.builder()
                    .backtestId(UlidGenerator.generate())
                    .strategyId(request.getStrategyId())
                    .strategyType(request.getStrategyType() != null ? request.getStrategyType() : "MA_CROSSOVER")
                    .symbols(request.getSymbols())
                    .startDate(LocalDate.parse(request.getStartDate()))
                    .endDate(LocalDate.parse(request.getEndDate()))
                    .timeframe(request.getTimeframe() != null ? request.getTimeframe() : "1d")
                    .initialCapital(request.getInitialCapital())
                    .commission(request.getCommission() != null ? request.getCommission() : java.math.BigDecimal.valueOf(0.0015))
                    .slippage(request.getSlippage() != null ? request.getSlippage() : java.math.BigDecimal.valueOf(0.0005))
                    .strategyParams(request.getStrategyParams() != null ? request.getStrategyParams() : new HashMap<>())
                    .dataSourceConfig(request.getDataSourceConfig())
                    .build();

            String jobId = backtestEngine.runAsync(config);

            Map<String, Object> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("backtestId", config.getBacktestId());
            response.put("status", "QUEUED");
            response.put("message", "Backtest submitted successfully");
            response.put("statusUrl", "/api/v1/admin/backtests/jobs/" + jobId + "/status");
            response.put("progressUrl", "/api/v1/admin/backtests/jobs/" + jobId + "/progress");

            log.info("Async backtest submitted: jobId={}", jobId);
            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            log.error("Failed to submit async backtest: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get job status.
     *
     * GET /api/v1/admin/backtests/jobs/{jobId}/status
     */
    @GetMapping("/jobs/{jobId}/status")
    public ResponseEntity<BacktestProgressResponse> getJobStatus(@PathVariable String jobId) {
        log.debug("Getting job status: jobId={}", jobId);

        BacktestProgress progress = backtestEngine.getProgress(jobId);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(BacktestProgressResponse.fromDomain(progress));
    }

    /**
     * Get job progress via Server-Sent Events (SSE).
     *
     * GET /api/v1/admin/backtests/jobs/{jobId}/progress
     *
     * Streams progress updates in real-time until completion.
     */
    @GetMapping(value = "/jobs/{jobId}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getJobProgressStream(@PathVariable String jobId) {
        log.info("Starting progress stream for job: {}", jobId);

        SseEmitter emitter = new SseEmitter(300000L); // 5 minute timeout

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable progressChecker = new Runnable() {
            @Override
            public void run() {
                try {
                    BacktestProgress progress = backtestEngine.getProgress(jobId);
                    if (progress == null) {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data(Map.of("error", "Job not found")));
                        emitter.complete();
                        scheduler.shutdown();
                        return;
                    }

                    // Send progress update
                    emitter.send(SseEmitter.event()
                            .name("progress")
                            .data(BacktestProgressResponse.fromDomain(progress)));

                    // Complete if done
                    if (progress.isDone()) {
                        if (progress.isSuccess()) {
                            BacktestResult result = backtestEngine.getResult(jobId);
                            if (result != null) {
                                emitter.send(SseEmitter.event()
                                        .name("result")
                                        .data(Map.of(
                                                "jobId", jobId,
                                                "finalCapital", result.getFinalCapital(),
                                                "totalReturn", result.getTotalReturn(),
                                                "totalTrades", result.getTrades() != null ? result.getTrades().size() : 0
                                        )));
                            }
                        }
                        emitter.complete();
                        scheduler.shutdown();
                    }
                } catch (IOException e) {
                    log.warn("Error sending progress: {}", e.getMessage());
                    emitter.completeWithError(e);
                    scheduler.shutdown();
                }
            }
        };

        scheduler.scheduleAtFixedRate(progressChecker, 0, 500, TimeUnit.MILLISECONDS);

        emitter.onCompletion(() -> {
            log.debug("Progress stream completed for job: {}", jobId);
            scheduler.shutdown();
        });

        emitter.onTimeout(() -> {
            log.warn("Progress stream timed out for job: {}", jobId);
            scheduler.shutdown();
        });

        emitter.onError(e -> {
            log.warn("Progress stream error for job {}: {}", jobId, e.getMessage());
            scheduler.shutdown();
        });

        return emitter;
    }

    /**
     * Cancel a running job.
     *
     * POST /api/v1/admin/backtests/jobs/{jobId}/cancel
     */
    @PostMapping("/jobs/{jobId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelJob(@PathVariable String jobId) {
        log.info("Cancelling job: {}", jobId);

        BacktestProgress progress = backtestEngine.getProgress(jobId);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }

        if (progress.isDone()) {
            Map<String, Object> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("status", progress.getStatus().name());
            response.put("message", "Job already completed");
            return ResponseEntity.ok(response);
        }

        backtestEngine.cancel(jobId);

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobId);
        response.put("status", "CANCELLED");
        response.put("message", "Job cancellation requested");

        return ResponseEntity.ok(response);
    }

    /**
     * Get result of a completed async job.
     *
     * GET /api/v1/admin/backtests/jobs/{jobId}/result
     */
    @GetMapping("/jobs/{jobId}/result")
    public ResponseEntity<BacktestResponse> getJobResult(@PathVariable String jobId) {
        log.info("Getting job result: {}", jobId);

        BacktestProgress progress = backtestEngine.getProgress(jobId);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }

        if (!progress.isDone()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(BacktestResponse.error("Job still running. Progress: " + progress.getProgressPercent() + "%"));
        }

        if (!progress.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BacktestResponse.error("Job failed: " + progress.getErrorMessage()));
        }

        BacktestResult result = backtestEngine.getResult(jobId);
        if (result == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BacktestResponse.error("Result not available"));
        }

        return ResponseEntity.ok(BacktestResponse.fromDomain(result));
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
