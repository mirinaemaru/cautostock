package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.AlertLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA Repository for Alert Log entities.
 */
@Repository
public interface AlertLogJpaRepository extends JpaRepository<AlertLogEntity, String> {

    // Find by severity with date range and pagination
    Page<AlertLogEntity> findBySeverityAndSentAtBetween(
            String severity,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);

    // Find by category with date range and pagination
    Page<AlertLogEntity> findByCategoryAndSentAtBetween(
            String category,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);

    // Find by severity and category with date range and pagination
    Page<AlertLogEntity> findBySeverityAndCategoryAndSentAtBetween(
            String severity,
            String category,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);

    // Find by date range only with pagination
    Page<AlertLogEntity> findBySentAtBetween(
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);

    // Find by date range ordered by sent_at descending (for stats)
    List<AlertLogEntity> findBySentAtBetweenOrderBySentAtDesc(
            LocalDateTime from,
            LocalDateTime to);

    // Find by related event ID
    List<AlertLogEntity> findByRelatedEventId(String relatedEventId);

    // Find recent critical alerts
    List<AlertLogEntity> findBySeverityOrderBySentAtDesc(String severity);

    // Count by severity and date range
    @Query("SELECT COUNT(a) FROM AlertLogEntity a WHERE a.severity = :severity " +
           "AND a.sentAt BETWEEN :from AND :to")
    long countBySeverityAndSentAtBetween(
            @Param("severity") String severity,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // Count by category and date range
    @Query("SELECT COUNT(a) FROM AlertLogEntity a WHERE a.category = :category " +
           "AND a.sentAt BETWEEN :from AND :to")
    long countByCategoryAndSentAtBetween(
            @Param("category") String category,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // Count successful alerts in date range
    @Query("SELECT COUNT(a) FROM AlertLogEntity a WHERE a.success = true " +
           "AND a.sentAt BETWEEN :from AND :to")
    long countSuccessfulBySentAtBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // Count failed alerts in date range
    @Query("SELECT COUNT(a) FROM AlertLogEntity a WHERE a.success = false " +
           "AND a.sentAt BETWEEN :from AND :to")
    long countFailedBySentAtBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // Find by channel
    List<AlertLogEntity> findByChannelOrderBySentAtDesc(String channel);

    // Delete old alerts (for cleanup)
    void deleteBySentAtBefore(LocalDateTime before);
}
