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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
     * @param from Start date
     * @param to End date
     * @param page Page number (0-based)
     * @param size Page size
     */
    @GetMapping
    public ResponseEntity<ExecutionHistoryResponse.ExecutionHistoryList> listExecutionHistory(
            @RequestParam(required = false) String strategyId,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String executionType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        log.info("Listing execution history: strategyId={}, accountId={}, type={}, status={}",
                strategyId, accountId, executionType, status);

        LocalDateTime endDate = to != null ? to.plusDays(1).atStartOfDay() : LocalDateTime.now();
        LocalDateTime startDate = from != null ? from.atStartOfDay() : endDate.minusDays(30);

        Pageable pageable = PageRequest.of(page, size);
        Page<ExecutionHistoryEntity> historyPage = executionHistoryRepository.findByFilters(
                strategyId, accountId, executionType, status, startDate, endDate, pageable);

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
}
