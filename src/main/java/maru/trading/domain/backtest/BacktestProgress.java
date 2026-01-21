package maru.trading.domain.backtest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Backtest progress information.
 *
 * Tracks the progress of an asynchronous backtest execution.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestProgress {

    /**
     * Job/Backtest ID.
     */
    private String jobId;

    /**
     * Current status.
     */
    private Status status;

    /**
     * Progress percentage (0-100).
     */
    private int progressPercent;

    /**
     * Current phase description.
     */
    private String currentPhase;

    /**
     * Total bars to process.
     */
    private int totalBars;

    /**
     * Bars processed so far.
     */
    private int processedBars;

    /**
     * Current processing symbol.
     */
    private String currentSymbol;

    /**
     * Error message if failed.
     */
    private String errorMessage;

    /**
     * When the job was queued.
     */
    private LocalDateTime queuedAt;

    /**
     * When execution started.
     */
    private LocalDateTime startedAt;

    /**
     * When execution completed.
     */
    private LocalDateTime completedAt;

    /**
     * Estimated remaining time in seconds.
     */
    private Long estimatedRemainingSeconds;

    /**
     * Progress status enum.
     */
    public enum Status {
        QUEUED,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    /**
     * Check if the backtest is still running.
     */
    public boolean isRunning() {
        return status == Status.QUEUED || status == Status.RUNNING;
    }

    /**
     * Check if the backtest has completed (successfully or not).
     */
    public boolean isDone() {
        return status == Status.COMPLETED || status == Status.FAILED || status == Status.CANCELLED;
    }

    /**
     * Check if the backtest completed successfully.
     */
    public boolean isSuccess() {
        return status == Status.COMPLETED;
    }

    /**
     * Create initial progress for a new job.
     */
    public static BacktestProgress initial(String jobId) {
        return BacktestProgress.builder()
                .jobId(jobId)
                .status(Status.QUEUED)
                .progressPercent(0)
                .currentPhase("Queued")
                .queuedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Create running progress.
     */
    public static BacktestProgress running(String jobId, int progressPercent, String phase,
                                           int totalBars, int processedBars) {
        return BacktestProgress.builder()
                .jobId(jobId)
                .status(Status.RUNNING)
                .progressPercent(progressPercent)
                .currentPhase(phase)
                .totalBars(totalBars)
                .processedBars(processedBars)
                .startedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Create completed progress.
     */
    public static BacktestProgress completed(String jobId) {
        return BacktestProgress.builder()
                .jobId(jobId)
                .status(Status.COMPLETED)
                .progressPercent(100)
                .currentPhase("Completed")
                .completedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Create failed progress.
     */
    public static BacktestProgress failed(String jobId, String errorMessage) {
        return BacktestProgress.builder()
                .jobId(jobId)
                .status(Status.FAILED)
                .currentPhase("Failed")
                .errorMessage(errorMessage)
                .completedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Create cancelled progress.
     */
    public static BacktestProgress cancelled(String jobId) {
        return BacktestProgress.builder()
                .jobId(jobId)
                .status(Status.CANCELLED)
                .currentPhase("Cancelled")
                .completedAt(LocalDateTime.now())
                .build();
    }
}
