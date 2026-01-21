package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.BacktestJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for backtest job entities.
 */
@Repository
public interface BacktestJobJpaRepository extends JpaRepository<BacktestJobEntity, String> {

    /**
     * Find jobs by status.
     */
    List<BacktestJobEntity> findByStatusOrderByQueuedAtAsc(String status);

    /**
     * Find jobs by type.
     */
    List<BacktestJobEntity> findByJobTypeOrderByQueuedAtDesc(String jobType);

    /**
     * Find jobs by related ID.
     */
    List<BacktestJobEntity> findByRelatedId(String relatedId);

    /**
     * Find running jobs.
     */
    @Query("SELECT j FROM BacktestJobEntity j WHERE j.status IN ('QUEUED', 'RUNNING') ORDER BY j.queuedAt ASC")
    List<BacktestJobEntity> findRunningJobs();

    /**
     * Find queued jobs to process.
     */
    @Query("SELECT j FROM BacktestJobEntity j WHERE j.status = 'QUEUED' ORDER BY j.queuedAt ASC")
    List<BacktestJobEntity> findQueuedJobs();

    /**
     * Count running jobs.
     */
    @Query("SELECT COUNT(j) FROM BacktestJobEntity j WHERE j.status = 'RUNNING'")
    long countRunningJobs();

    /**
     * Count queued jobs.
     */
    @Query("SELECT COUNT(j) FROM BacktestJobEntity j WHERE j.status = 'QUEUED'")
    long countQueuedJobs();

    /**
     * Find job by ID with running status.
     */
    @Query("SELECT j FROM BacktestJobEntity j WHERE j.jobId = :jobId AND j.status IN ('QUEUED', 'RUNNING')")
    Optional<BacktestJobEntity> findRunningJobById(@Param("jobId") String jobId);

    /**
     * Find recent completed jobs.
     */
    @Query("SELECT j FROM BacktestJobEntity j WHERE j.status = 'COMPLETED' ORDER BY j.completedAt DESC")
    List<BacktestJobEntity> findRecentCompletedJobs();
}
