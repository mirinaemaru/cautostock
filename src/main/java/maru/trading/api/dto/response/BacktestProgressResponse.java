package maru.trading.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.backtest.BacktestProgress;

import java.time.LocalDateTime;

/**
 * Response DTO for backtest job progress.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestProgressResponse {

    /**
     * Job ID for tracking.
     */
    private String jobId;

    /**
     * Current status of the job.
     * Values: QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED
     */
    private String status;

    /**
     * Progress percentage (0-100).
     */
    private int progressPercent;

    /**
     * Current phase description.
     * Examples: "Loading data", "Processing bars", "Calculating metrics", "Completed"
     */
    private String currentPhase;

    /**
     * Total number of bars to process.
     */
    private int totalBars;

    /**
     * Number of bars processed so far.
     */
    private int processedBars;

    /**
     * Error message if status is FAILED.
     */
    private String errorMessage;

    /**
     * Timestamp when job was queued.
     */
    private LocalDateTime queuedAt;

    /**
     * Timestamp when job started executing.
     */
    private LocalDateTime startedAt;

    /**
     * Timestamp when job completed (or failed/cancelled).
     */
    private LocalDateTime completedAt;

    /**
     * Estimated time remaining in seconds.
     * Calculated based on current progress rate.
     */
    private Long estimatedSecondsRemaining;

    /**
     * Convert from domain BacktestProgress to response DTO.
     */
    public static BacktestProgressResponse fromDomain(BacktestProgress progress) {
        if (progress == null) {
            return null;
        }

        Long estimatedRemaining = null;
        if (progress.getStartedAt() != null && progress.getProgressPercent() > 0 && progress.getProgressPercent() < 100) {
            long elapsedMillis = java.time.Duration.between(progress.getStartedAt(), LocalDateTime.now()).toMillis();
            double progressRate = progress.getProgressPercent() / 100.0;
            long totalEstimatedMillis = (long) (elapsedMillis / progressRate);
            estimatedRemaining = (totalEstimatedMillis - elapsedMillis) / 1000;
        }

        return BacktestProgressResponse.builder()
                .jobId(progress.getJobId())
                .status(progress.getStatus() != null ? progress.getStatus().name() : null)
                .progressPercent(progress.getProgressPercent())
                .currentPhase(progress.getCurrentPhase())
                .totalBars(progress.getTotalBars())
                .processedBars(progress.getProcessedBars())
                .errorMessage(progress.getErrorMessage())
                .queuedAt(progress.getQueuedAt())
                .startedAt(progress.getStartedAt())
                .completedAt(progress.getCompletedAt())
                .estimatedSecondsRemaining(estimatedRemaining)
                .build();
    }
}
