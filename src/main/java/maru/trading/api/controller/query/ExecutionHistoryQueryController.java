package maru.trading.api.controller.query;

import maru.trading.api.dto.response.ExecutionHistoryResponse;
import maru.trading.infra.persistence.jpa.entity.ExecutionHistoryEntity;
import maru.trading.infra.persistence.jpa.repository.ExecutionHistoryJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Execution History Query API.
 *
 * Endpoints:
 * - GET /api/v1/query/execution-history            - List execution history with filters
 * - GET /api/v1/query/execution-history/{id}       - Get specific execution
 * - GET /api/v1/query/execution-history/strategy/{strategyId} - Get by strategy
 */
@RestController
@RequestMapping("/api/v1/query/execution-history")
public class ExecutionHistoryQueryController {

    private static final Logger log = LoggerFactory.getLogger(ExecutionHistoryQueryController.class);

    private final ExecutionHistoryJpaRepository executionHistoryRepository;

    public ExecutionHistoryQueryController(ExecutionHistoryJpaRepository executionHistoryRepository) {
        this.executionHistoryRepository = executionHistoryRepository;
    }

    /**
     * List execution history with filters.
     *
     * @param strategyId Filter by strategy ID
     * @param accountId Filter by account ID
     * @param executionType Filter by execution type (SIGNAL_GENERATED, ORDER_PLACED, etc.)
     * @param status Filter by status (SUCCESS, FAILED, PENDING)
     * @param symbol Filter by symbol
     * @param side Filter by side (BUY, SELL)
     * @param from Start date (alias: startDate)
     * @param to End date (alias: endDate)
     * @param page Page number (0-based)
     * @param size Page size
     * @param sort Sort order (e.g., executedAt,desc)
     */
    @GetMapping
    public ResponseEntity<ExecutionHistoryResponse.ExecutionHistoryList> listExecutionHistory(
            @RequestParam(required = false) String strategyId,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String executionType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String side,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sort) {

        log.info("Listing execution history: strategyId={}, accountId={}, type={}, status={}, symbol={}, side={}",
                strategyId, accountId, executionType, status, symbol, side);

        // Support both from/to and startDate/endDate parameter names
        LocalDate effectiveFrom = from != null ? from : startDate;
        LocalDate effectiveTo = to != null ? to : endDate;

        LocalDateTime endDateTime = effectiveTo != null ? effectiveTo.plusDays(1).atStartOfDay() : LocalDateTime.now();
        LocalDateTime startDateTime = effectiveFrom != null ? effectiveFrom.atStartOfDay() : endDateTime.minusDays(30);

        Pageable pageable = PageRequest.of(page, size);
        Page<ExecutionHistoryEntity> historyPage = executionHistoryRepository.findByFilters(
                strategyId, accountId, executionType, status, startDateTime, endDateTime, pageable);

        List<ExecutionHistoryResponse> executions = historyPage.getContent().stream()
                .map(ExecutionHistoryResponse::fromEntity)
                .collect(Collectors.toList());

        // Count success and failed
        long successCount = strategyId != null
                ? executionHistoryRepository.countByStrategyIdAndStatus(strategyId, "SUCCESS")
                : 0;
        long failedCount = strategyId != null
                ? executionHistoryRepository.countByStrategyIdAndStatus(strategyId, "FAILED")
                : 0;

        return ResponseEntity.ok(ExecutionHistoryResponse.ExecutionHistoryList.builder()
                .executions(executions)
                .totalCount((int) historyPage.getTotalElements())
                .page(page)
                .pageSize(size)
                .successCount(successCount)
                .failedCount(failedCount)
                .build());
    }

    /**
     * Get specific execution by ID.
     */
    @GetMapping("/{executionId}")
    public ResponseEntity<ExecutionHistoryResponse> getExecution(@PathVariable String executionId) {

        log.info("Fetching execution: {}", executionId);

        return executionHistoryRepository.findById(executionId)
                .map(entity -> ResponseEntity.ok(ExecutionHistoryResponse.fromEntity(entity)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get execution history by strategy.
     */
    @GetMapping("/strategy/{strategyId}")
    public ResponseEntity<ExecutionHistoryResponse.ExecutionHistoryList> getByStrategy(
            @PathVariable String strategyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        log.info("Fetching execution history for strategy: {}", strategyId);

        LocalDateTime endDate = to != null ? to.plusDays(1).atStartOfDay() : LocalDateTime.now();
        LocalDateTime startDate = from != null ? from.atStartOfDay() : endDate.minusDays(30);

        Pageable pageable = PageRequest.of(page, size);
        Page<ExecutionHistoryEntity> historyPage = executionHistoryRepository.findByFilters(
                strategyId, null, null, null, startDate, endDate, pageable);

        List<ExecutionHistoryResponse> executions = historyPage.getContent().stream()
                .map(ExecutionHistoryResponse::fromEntity)
                .collect(Collectors.toList());

        long successCount = executionHistoryRepository.countByStrategyIdAndStatus(strategyId, "SUCCESS");
        long failedCount = executionHistoryRepository.countByStrategyIdAndStatus(strategyId, "FAILED");

        return ResponseEntity.ok(ExecutionHistoryResponse.ExecutionHistoryList.builder()
                .executions(executions)
                .totalCount((int) historyPage.getTotalElements())
                .page(page)
                .pageSize(size)
                .successCount(successCount)
                .failedCount(failedCount)
                .build());
    }

    /**
     * Get daily execution statistics.
     */
    @GetMapping("/daily-stats")
    public ResponseEntity<Map<String, Object>> getDailyStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String strategyId,
            @RequestParam(required = false) String accountId) {

        log.info("Getting daily execution stats: startDate={}, endDate={}", startDate, endDate);

        LocalDateTime endDateTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : LocalDateTime.now();
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : endDateTime.minusDays(30);

        List<ExecutionHistoryEntity> executions = executionHistoryRepository.findByCreatedAtBetween(
                startDateTime, endDateTime);

        // Group by date and calculate stats
        Map<String, Map<String, Object>> dailyStats = new HashMap<>();
        for (ExecutionHistoryEntity exec : executions) {
            String dateKey = exec.getCreatedAt().toLocalDate().toString();
            Map<String, Object> dayStats = dailyStats.computeIfAbsent(dateKey, k -> {
                Map<String, Object> stats = new HashMap<>();
                stats.put("date", dateKey);
                stats.put("totalCount", 0);
                stats.put("successCount", 0);
                stats.put("failedCount", 0);
                return stats;
            });

            dayStats.put("totalCount", (int) dayStats.get("totalCount") + 1);
            if ("SUCCESS".equals(exec.getStatus())) {
                dayStats.put("successCount", (int) dayStats.get("successCount") + 1);
            } else if ("FAILED".equals(exec.getStatus())) {
                dayStats.put("failedCount", (int) dayStats.get("failedCount") + 1);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("items", dailyStats.values());
        result.put("startDate", startDateTime.toLocalDate());
        result.put("endDate", endDateTime.toLocalDate());
        result.put("totalDays", dailyStats.size());

        return ResponseEntity.ok(result);
    }

    /**
     * Get execution statistics by symbol.
     * Groups executions by symbol and calculates success/failure counts and success rate.
     */
    @GetMapping("/symbol-stats")
    public ResponseEntity<Map<String, Object>> getSymbolStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String strategyId) {

        log.info("Getting symbol execution stats: startDate={}, endDate={}, accountId={}, strategyId={}",
                startDate, endDate, accountId, strategyId);

        LocalDateTime endDateTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : LocalDateTime.now();
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : endDateTime.minusDays(30);

        List<ExecutionHistoryEntity> executions = executionHistoryRepository.findByCreatedAtBetween(
                startDateTime, endDateTime);

        // Filter by accountId and strategyId if provided
        if (accountId != null && !accountId.isEmpty()) {
            executions = executions.stream()
                    .filter(e -> accountId.equals(e.getAccountId()))
                    .collect(Collectors.toList());
        }
        if (strategyId != null && !strategyId.isEmpty()) {
            executions = executions.stream()
                    .filter(e -> strategyId.equals(e.getStrategyId()))
                    .collect(Collectors.toList());
        }

        // Group by symbol and calculate statistics
        Map<String, Map<String, Object>> symbolStats = new HashMap<>();
        for (ExecutionHistoryEntity exec : executions) {
            String symbol = exec.getSymbol();
            if (symbol == null || symbol.isEmpty()) {
                symbol = "UNKNOWN";
            }

            Map<String, Object> stats = symbolStats.computeIfAbsent(symbol, k -> {
                Map<String, Object> s = new HashMap<>();
                s.put("symbol", k);
                s.put("totalCount", 0);
                s.put("successCount", 0);
                s.put("failedCount", 0);
                s.put("pendingCount", 0);
                s.put("avgExecutionTimeMs", 0L);
                s.put("totalExecutionTimeMs", 0L);
                return s;
            });

            stats.put("totalCount", (int) stats.get("totalCount") + 1);

            if ("SUCCESS".equals(exec.getStatus())) {
                stats.put("successCount", (int) stats.get("successCount") + 1);
            } else if ("FAILED".equals(exec.getStatus())) {
                stats.put("failedCount", (int) stats.get("failedCount") + 1);
            } else {
                stats.put("pendingCount", (int) stats.get("pendingCount") + 1);
            }

            if (exec.getExecutionTimeMs() != null) {
                long totalTime = (long) stats.get("totalExecutionTimeMs") + exec.getExecutionTimeMs();
                stats.put("totalExecutionTimeMs", totalTime);
            }
        }

        // Calculate averages and success rates
        for (Map<String, Object> stats : symbolStats.values()) {
            int totalCount = (int) stats.get("totalCount");
            int successCount = (int) stats.get("successCount");
            long totalExecutionTimeMs = (long) stats.get("totalExecutionTimeMs");

            if (totalCount > 0) {
                stats.put("avgExecutionTimeMs", totalExecutionTimeMs / totalCount);
                stats.put("successRate", Math.round(successCount * 100.0 / totalCount * 100) / 100.0);
            } else {
                stats.put("successRate", 0.0);
            }
            stats.remove("totalExecutionTimeMs"); // Remove internal field
        }

        // Sort by totalCount descending
        List<Map<String, Object>> sortedStats = symbolStats.values().stream()
                .sorted((a, b) -> Integer.compare((int) b.get("totalCount"), (int) a.get("totalCount")))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", sortedStats);
        result.put("totalSymbols", symbolStats.size());
        result.put("startDate", startDateTime.toLocalDate());
        result.put("endDate", endDateTime.toLocalDate());

        return ResponseEntity.ok(result);
    }

    /**
     * Export execution history to CSV.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExecutionHistory(
            @RequestParam(required = false) String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String strategyId,
            @RequestParam(required = false) String accountId) {

        log.info("Exporting execution history: format={}, startDate={}, endDate={}", format, startDate, endDate);

        LocalDateTime endDateTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : LocalDateTime.now();
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : endDateTime.minusDays(30);

        List<ExecutionHistoryEntity> executions = executionHistoryRepository.findByCreatedAtBetween(
                startDateTime, endDateTime);

        // Build CSV
        StringBuilder csv = new StringBuilder();
        csv.append("ExecutionId,StrategyId,AccountId,ExecutionType,Status,CreatedAt\n");
        for (ExecutionHistoryEntity exec : executions) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s\n",
                    exec.getExecutionId(),
                    exec.getStrategyId(),
                    exec.getAccountId(),
                    exec.getExecutionType(),
                    exec.getStatus(),
                    exec.getCreatedAt()));
        }

        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=execution-history.csv")
                .body(csv.toString().getBytes());
    }
}
