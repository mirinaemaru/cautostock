package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import maru.trading.infra.config.UlidGenerator;

import java.time.LocalDateTime;

/**
 * Backtest Job entity for async execution tracking.
 */
@Entity
@Table(name = "backtest_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestJobEntity {

    @Id
    @Column(name = "job_id", length = 26)
    private String jobId;

    @Column(name = "job_type", nullable = false, length = 32)
    private String jobType;

    @Column(name = "related_id", length = 26)
    private String relatedId;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "progress_percent")
    private Integer progressPercent;

    @Column(name = "current_phase", length = 64)
    private String currentPhase;

    @Column(name = "config", columnDefinition = "JSON")
    private String config;

    @Column(name = "result_summary", columnDefinition = "JSON")
    private String resultSummary;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "queued_at", nullable = false)
    private LocalDateTime queuedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (jobId == null) {
            jobId = UlidGenerator.generate();
        }
        if (queuedAt == null) {
            queuedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "QUEUED";
        }
        if (progressPercent == null) {
            progressPercent = 0;
        }
    }

    /**
     * Mark job as running.
     */
    public void start() {
        this.status = "RUNNING";
        this.startedAt = LocalDateTime.now();
        this.currentPhase = "Starting";
    }

    /**
     * Update progress.
     */
    public void updateProgress(int percent, String phase) {
        this.progressPercent = percent;
        this.currentPhase = phase;
    }

    /**
     * Mark job as completed.
     */
    public void complete(String resultSummary) {
        this.status = "COMPLETED";
        this.progressPercent = 100;
        this.currentPhase = "Completed";
        this.resultSummary = resultSummary;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark job as failed.
     */
    public void fail(String errorMessage) {
        this.status = "FAILED";
        this.currentPhase = "Failed";
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark job as cancelled.
     */
    public void cancel() {
        this.status = "CANCELLED";
        this.currentPhase = "Cancelled";
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Check if job is still running.
     */
    public boolean isRunning() {
        return "QUEUED".equals(status) || "RUNNING".equals(status);
    }

    /**
     * Check if job is done.
     */
    public boolean isDone() {
        return "COMPLETED".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status);
    }

    /**
     * Job types.
     */
    public static class JobType {
        public static final String BACKTEST = "BACKTEST";
        public static final String OPTIMIZATION = "OPTIMIZATION";
        public static final String WALKFORWARD = "WALKFORWARD";
        public static final String PORTFOLIO = "PORTFOLIO";
        public static final String MONTE_CARLO = "MONTE_CARLO";
    }
}
