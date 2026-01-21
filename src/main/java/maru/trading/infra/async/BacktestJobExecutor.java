package maru.trading.infra.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import maru.trading.domain.backtest.*;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.BacktestJobEntity;
import maru.trading.infra.persistence.jpa.repository.BacktestJobJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Backtest Job Executor.
 *
 * Manages asynchronous backtest execution with:
 * - Thread pool management
 * - Job queue processing
 * - Progress tracking
 * - Cancellation support
 */
@Service
public class BacktestJobExecutor {

    private static final Logger log = LoggerFactory.getLogger(BacktestJobExecutor.class);

    private final BacktestJobJpaRepository jobRepository;
    private final ObjectMapper objectMapper;

    // In-memory tracking for running jobs
    private final Map<String, CompletableFuture<BacktestResult>> runningFutures = new ConcurrentHashMap<>();
    private final Map<String, BacktestProgress> progressMap = new ConcurrentHashMap<>();
    private final Map<String, BacktestResult> resultCache = new ConcurrentHashMap<>();
    private final Map<String, Consumer<BacktestProgress>> progressListeners = new ConcurrentHashMap<>();

    private ExecutorService executor;

    @Value("${backtest.executor.poolSize:4}")
    private int poolSize;

    @Value("${backtest.executor.queueCapacity:100}")
    private int queueCapacity;

    public BacktestJobExecutor(BacktestJobJpaRepository jobRepository, ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing BacktestJobExecutor with pool size: {}", poolSize);
        executor = new ThreadPoolExecutor(
                poolSize,
                poolSize * 2,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down BacktestJobExecutor");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Submit a backtest job for asynchronous execution.
     *
     * @param config Backtest configuration
     * @param backtestRunner Function to execute the backtest
     * @return Job ID
     */
    public String submit(BacktestConfig config, BacktestRunner backtestRunner) {
        String jobId = UlidGenerator.generate();
        String backtestId = config.getBacktestId() != null ? config.getBacktestId() : jobId;

        log.info("Submitting backtest job: {}", jobId);

        // Create job entity
        BacktestJobEntity jobEntity = BacktestJobEntity.builder()
                .jobId(jobId)
                .jobType(BacktestJobEntity.JobType.BACKTEST)
                .relatedId(backtestId)
                .status("QUEUED")
                .progressPercent(0)
                .currentPhase("Queued")
                .config(serializeConfig(config))
                .queuedAt(LocalDateTime.now())
                .build();

        jobRepository.save(jobEntity);

        // Initialize progress
        BacktestProgress initialProgress = BacktestProgress.initial(jobId);
        progressMap.put(jobId, initialProgress);

        // Submit to executor
        CompletableFuture<BacktestResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                // Update status to running
                updateJobStatus(jobId, "RUNNING", 0, "Loading data");
                updateProgress(jobId, BacktestProgress.running(jobId, 0, "Loading data", 0, 0));

                // Execute backtest with progress callback
                BacktestResult result = backtestRunner.run(config, (percent, phase, total, processed) -> {
                    updateJobStatus(jobId, "RUNNING", percent, phase);
                    updateProgress(jobId, BacktestProgress.builder()
                            .jobId(jobId)
                            .status(BacktestProgress.Status.RUNNING)
                            .progressPercent(percent)
                            .currentPhase(phase)
                            .totalBars(total)
                            .processedBars(processed)
                            .build());
                });

                // Mark as completed
                updateJobStatus(jobId, "COMPLETED", 100, "Completed");
                updateProgress(jobId, BacktestProgress.completed(jobId));
                resultCache.put(jobId, result);

                // Save result summary
                saveResultSummary(jobId, result);

                log.info("Backtest job {} completed successfully", jobId);
                return result;

            } catch (Exception e) {
                log.error("Backtest job {} failed: {}", jobId, e.getMessage(), e);
                updateJobStatus(jobId, "FAILED", -1, "Failed: " + e.getMessage());
                updateProgress(jobId, BacktestProgress.failed(jobId, e.getMessage()));
                throw new CompletionException(e);
            }
        }, executor);

        runningFutures.put(jobId, future);

        // Clean up on completion
        future.whenComplete((result, error) -> {
            runningFutures.remove(jobId);
        });

        return jobId;
    }

    /**
     * Get progress of a job.
     */
    public BacktestProgress getProgress(String jobId) {
        // Check in-memory first
        BacktestProgress progress = progressMap.get(jobId);
        if (progress != null) {
            return progress;
        }

        // Check database
        return jobRepository.findById(jobId)
                .map(this::toProgress)
                .orElse(null);
    }

    /**
     * Get result of a completed job.
     */
    public BacktestResult getResult(String jobId) {
        return resultCache.get(jobId);
    }

    /**
     * Cancel a running job.
     */
    public boolean cancel(String jobId) {
        CompletableFuture<BacktestResult> future = runningFutures.get(jobId);
        if (future != null && !future.isDone()) {
            boolean cancelled = future.cancel(true);
            if (cancelled) {
                updateJobStatus(jobId, "CANCELLED", -1, "Cancelled by user");
                updateProgress(jobId, BacktestProgress.cancelled(jobId));
                log.info("Backtest job {} cancelled", jobId);
            }
            return cancelled;
        }

        // Try to cancel from DB if not in memory
        return jobRepository.findRunningJobById(jobId)
                .map(job -> {
                    job.cancel();
                    jobRepository.save(job);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Register a progress listener for a job.
     */
    public void addProgressListener(String jobId, Consumer<BacktestProgress> listener) {
        progressListeners.put(jobId, listener);
    }

    /**
     * Remove progress listener.
     */
    public void removeProgressListener(String jobId) {
        progressListeners.remove(jobId);
    }

    /**
     * Get number of running jobs.
     */
    public long getRunningJobCount() {
        return jobRepository.countRunningJobs();
    }

    /**
     * Get number of queued jobs.
     */
    public long getQueuedJobCount() {
        return jobRepository.countQueuedJobs();
    }

    private void updateJobStatus(String jobId, String status, int progress, String phase) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(status);
            if (progress >= 0) {
                job.setProgressPercent(progress);
            }
            job.setCurrentPhase(phase);
            if ("RUNNING".equals(status) && job.getStartedAt() == null) {
                job.setStartedAt(LocalDateTime.now());
            }
            if ("COMPLETED".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status)) {
                job.setCompletedAt(LocalDateTime.now());
            }
            jobRepository.save(job);
        });
    }

    private void updateProgress(String jobId, BacktestProgress progress) {
        progressMap.put(jobId, progress);

        // Notify listener if registered
        Consumer<BacktestProgress> listener = progressListeners.get(jobId);
        if (listener != null) {
            try {
                listener.accept(progress);
            } catch (Exception e) {
                log.warn("Progress listener error for job {}: {}", jobId, e.getMessage());
            }
        }
    }

    private void saveResultSummary(String jobId, BacktestResult result) {
        try {
            String summary = objectMapper.writeValueAsString(Map.of(
                    "finalCapital", result.getFinalCapital(),
                    "totalReturn", result.getTotalReturn(),
                    "totalTrades", result.getTrades() != null ? result.getTrades().size() : 0
            ));

            jobRepository.findById(jobId).ifPresent(job -> {
                job.setResultSummary(summary);
                job.complete(summary);
                jobRepository.save(job);
            });
        } catch (Exception e) {
            log.warn("Failed to save result summary for job {}: {}", jobId, e.getMessage());
        }
    }

    private String serializeConfig(BacktestConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            log.warn("Failed to serialize config: {}", e.getMessage());
            return "{}";
        }
    }

    private BacktestProgress toProgress(BacktestJobEntity entity) {
        return BacktestProgress.builder()
                .jobId(entity.getJobId())
                .status(BacktestProgress.Status.valueOf(entity.getStatus()))
                .progressPercent(entity.getProgressPercent() != null ? entity.getProgressPercent() : 0)
                .currentPhase(entity.getCurrentPhase())
                .errorMessage(entity.getErrorMessage())
                .queuedAt(entity.getQueuedAt())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .build();
    }

    /**
     * Functional interface for backtest execution with progress callback.
     */
    @FunctionalInterface
    public interface BacktestRunner {
        BacktestResult run(BacktestConfig config, ProgressCallback progressCallback) throws BacktestException;
    }

    /**
     * Progress callback interface.
     */
    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int percent, String phase, int totalBars, int processedBars);
    }
}
