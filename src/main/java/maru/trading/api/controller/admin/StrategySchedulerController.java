package maru.trading.api.controller.admin;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import maru.trading.api.dto.request.SchedulerTriggerRequest;
import maru.trading.api.dto.response.AckResponse;
import maru.trading.api.dto.response.SchedulerStatusResponse;
import maru.trading.application.ports.repo.StrategyRepository;
import maru.trading.application.scheduler.StrategyScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin controller for Strategy Scheduler management.
 *
 * Endpoints:
 * - GET  /api/v1/admin/scheduler/status - Get scheduler status
 * - POST /api/v1/admin/scheduler/enable - Enable scheduler
 * - POST /api/v1/admin/scheduler/disable - Disable scheduler
 * - POST /api/v1/admin/scheduler/trigger - Manual trigger for a strategy
 * - POST /api/v1/admin/scheduler/execute-all - Execute all strategies once
 * - POST /api/v1/admin/scheduler/reset-stats - Reset scheduler statistics
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/scheduler")
public class StrategySchedulerController {

    private final StrategyScheduler strategyScheduler;
    private final StrategyRepository strategyRepository;
    private final boolean schedulerBeanActive;

    /**
     * Constructor with optional StrategyScheduler injection.
     * StrategyScheduler may not be created if disabled via property.
     */
    @Autowired
    public StrategySchedulerController(
            @Autowired(required = false) StrategyScheduler strategyScheduler,
            StrategyRepository strategyRepository) {
        this.strategyScheduler = strategyScheduler;
        this.strategyRepository = strategyRepository;
        this.schedulerBeanActive = strategyScheduler != null;

        if (!schedulerBeanActive) {
            log.warn("StrategyScheduler bean is not active. " +
                    "Set trading.scheduler.strategy.enabled=true to enable it.");
        }
    }

    /**
     * Get scheduler status.
     *
     * GET /api/v1/admin/scheduler/status
     *
     * Returns current state including:
     * - enabled/disabled status
     * - last execution time
     * - execution statistics
     * - active strategy count
     */
    @GetMapping("/status")
    public ResponseEntity<SchedulerStatusResponse> getStatus() {
        log.debug("Getting scheduler status");

        if (!schedulerBeanActive) {
            return ResponseEntity.ok(SchedulerStatusResponse.builder()
                    .enabled(false)
                    .active(false)
                    .cronExpression("0 * * * * *")
                    .executionCount(0)
                    .successCount(0)
                    .errorCount(0)
                    .activeStrategyCount(0)
                    .message("Scheduler is disabled via application property")
                    .build());
        }

        try {
            StrategyScheduler.SchedulerStatus status = strategyScheduler.getStatus();
            int activeStrategyCount = strategyRepository.findActiveStrategies().size();

            String message;
            if (status.enabled()) {
                message = String.format("Scheduler is running with %d active strategies", activeStrategyCount);
            } else {
                message = "Scheduler is paused (disabled at runtime)";
            }

            return ResponseEntity.ok(SchedulerStatusResponse.builder()
                    .enabled(status.enabled())
                    .active(true)
                    .cronExpression("0 * * * * *")
                    .lastExecutionTime(status.lastExecutionTime())
                    .executionCount(status.executionCount())
                    .successCount(status.successCount())
                    .errorCount(status.errorCount())
                    .activeStrategyCount(activeStrategyCount)
                    .message(message)
                    .build());

        } catch (Exception e) {
            log.error("Failed to get scheduler status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Enable the scheduler.
     *
     * POST /api/v1/admin/scheduler/enable
     *
     * Re-enables the scheduler if it was disabled at runtime.
     * Note: This does not work if scheduler is disabled via application property.
     */
    @PostMapping("/enable")
    public ResponseEntity<AckResponse> enableScheduler() {
        log.info("Enabling scheduler");

        if (!schedulerBeanActive) {
            return ResponseEntity.badRequest()
                    .body(AckResponse.builder()
                            .ok(false)
                            .message("Scheduler bean is not active. " +
                                    "Set trading.scheduler.strategy.enabled=true and restart.")
                            .build());
        }

        try {
            boolean wasEnabled = strategyScheduler.isEnabled();
            strategyScheduler.setEnabled(true);

            String message = wasEnabled
                    ? "Scheduler was already enabled"
                    : "Scheduler enabled successfully";

            log.info(message);

            return ResponseEntity.ok(AckResponse.builder()
                    .ok(true)
                    .message(message)
                    .build());

        } catch (Exception e) {
            log.error("Failed to enable scheduler", e);
            return ResponseEntity.internalServerError()
                    .body(AckResponse.builder()
                            .ok(false)
                            .message("Failed to enable scheduler: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Disable the scheduler.
     *
     * POST /api/v1/admin/scheduler/disable
     *
     * Temporarily disables the scheduler at runtime.
     * The scheduler will skip executions until re-enabled.
     */
    @PostMapping("/disable")
    public ResponseEntity<AckResponse> disableScheduler() {
        log.info("Disabling scheduler");

        if (!schedulerBeanActive) {
            return ResponseEntity.badRequest()
                    .body(AckResponse.builder()
                            .ok(false)
                            .message("Scheduler bean is not active")
                            .build());
        }

        try {
            boolean wasEnabled = strategyScheduler.isEnabled();
            strategyScheduler.setEnabled(false);

            String message = wasEnabled
                    ? "Scheduler disabled successfully"
                    : "Scheduler was already disabled";

            log.info(message);

            return ResponseEntity.ok(AckResponse.builder()
                    .ok(true)
                    .message(message)
                    .build());

        } catch (Exception e) {
            log.error("Failed to disable scheduler", e);
            return ResponseEntity.internalServerError()
                    .body(AckResponse.builder()
                            .ok(false)
                            .message("Failed to disable scheduler: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Manually trigger a single strategy execution.
     *
     * POST /api/v1/admin/scheduler/trigger
     * Body: {"strategyId": "...", "symbol": "005930", "accountId": "..."}
     *
     * Useful for testing or manual intervention.
     * Works even if scheduler is disabled.
     */
    @PostMapping("/trigger")
    public ResponseEntity<AckResponse> triggerStrategy(
            @Valid @RequestBody SchedulerTriggerRequest request) {
        log.info("Manual trigger requested: strategyId={}, symbol={}, accountId={}",
                request.getStrategyId(), request.getSymbol(), request.getAccountId());

        if (!schedulerBeanActive) {
            return ResponseEntity.badRequest()
                    .body(AckResponse.builder()
                            .ok(false)
                            .message("Scheduler bean is not active")
                            .build());
        }

        try {
            strategyScheduler.triggerManually(
                    request.getStrategyId(),
                    request.getSymbol(),
                    request.getAccountId()
            );

            log.info("Manual trigger completed for strategy {}", request.getStrategyId());

            return ResponseEntity.ok(AckResponse.builder()
                    .ok(true)
                    .message(String.format("Strategy %s triggered successfully for symbol %s",
                            request.getStrategyId(), request.getSymbol()))
                    .build());

        } catch (IllegalArgumentException e) {
            log.warn("Invalid trigger request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(AckResponse.builder()
                            .ok(false)
                            .message(e.getMessage())
                            .build());

        } catch (Exception e) {
            log.error("Failed to trigger strategy {}", request.getStrategyId(), e);
            return ResponseEntity.internalServerError()
                    .body(AckResponse.builder()
                            .ok(false)
                            .message("Failed to trigger strategy: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Execute all active strategies once immediately.
     *
     * POST /api/v1/admin/scheduler/execute-all
     *
     * Triggers the same execution that runs on schedule.
     * Works even if scheduler is disabled (bypasses the enabled check).
     */
    @PostMapping("/execute-all")
    public ResponseEntity<AckResponse> executeAllStrategies() {
        log.info("Manual execution of all strategies requested");

        if (!schedulerBeanActive) {
            return ResponseEntity.badRequest()
                    .body(AckResponse.builder()
                            .ok(false)
                            .message("Scheduler bean is not active")
                            .build());
        }

        try {
            // Temporarily enable if disabled, then execute
            boolean wasEnabled = strategyScheduler.isEnabled();
            if (!wasEnabled) {
                strategyScheduler.setEnabled(true);
            }

            strategyScheduler.executeStrategies();

            // Restore previous state
            if (!wasEnabled) {
                strategyScheduler.setEnabled(false);
            }

            int activeCount = strategyRepository.findActiveStrategies().size();
            log.info("Manual execution completed for {} active strategies", activeCount);

            return ResponseEntity.ok(AckResponse.builder()
                    .ok(true)
                    .message(String.format("Executed %d active strategies", activeCount))
                    .build());

        } catch (Exception e) {
            log.error("Failed to execute all strategies", e);
            return ResponseEntity.internalServerError()
                    .body(AckResponse.builder()
                            .ok(false)
                            .message("Failed to execute strategies: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Reset scheduler statistics.
     *
     * POST /api/v1/admin/scheduler/reset-stats
     *
     * Resets execution count, success count, and error count to zero.
     */
    @PostMapping("/reset-stats")
    public ResponseEntity<AckResponse> resetStatistics() {
        log.info("Resetting scheduler statistics");

        if (!schedulerBeanActive) {
            return ResponseEntity.badRequest()
                    .body(AckResponse.builder()
                            .ok(false)
                            .message("Scheduler bean is not active")
                            .build());
        }

        try {
            strategyScheduler.resetStatistics();

            return ResponseEntity.ok(AckResponse.builder()
                    .ok(true)
                    .message("Scheduler statistics reset successfully")
                    .build());

        } catch (Exception e) {
            log.error("Failed to reset scheduler statistics", e);
            return ResponseEntity.internalServerError()
                    .body(AckResponse.builder()
                            .ok(false)
                            .message("Failed to reset statistics: " + e.getMessage())
                            .build());
        }
    }
}
