package maru.trading.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response containing strategy scheduler status information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerStatusResponse {

    /**
     * Whether the scheduler is currently enabled.
     */
    private boolean enabled;

    /**
     * Whether the scheduler bean is active (created).
     * If false, the scheduler is disabled via application property.
     */
    private boolean active;

    /**
     * Schedule cron expression.
     */
    private String cronExpression;

    /**
     * Last execution timestamp.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastExecutionTime;

    /**
     * Total number of executions since startup or reset.
     */
    private int executionCount;

    /**
     * Number of successful executions.
     */
    private int successCount;

    /**
     * Number of executions with errors.
     */
    private int errorCount;

    /**
     * Number of currently active strategies.
     */
    private int activeStrategyCount;

    /**
     * Status message.
     */
    private String message;
}
