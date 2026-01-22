package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.EventDlqEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for Dead Letter Queue.
 */
@Repository
public interface EventDlqJpaRepository extends JpaRepository<EventDlqEntity, String> {

	/**
	 * Find unprocessed DLQ events (not yet reprocessed).
	 */
	@Query("SELECT e FROM EventDlqEntity e WHERE e.reprocessedAt IS NULL ORDER BY e.createdAt ASC")
	List<EventDlqEntity> findUnprocessedEvents();

	/**
	 * Find unprocessed events by event type.
	 */
	@Query("SELECT e FROM EventDlqEntity e WHERE e.reprocessedAt IS NULL AND e.eventType = :eventType ORDER BY e.createdAt ASC")
	List<EventDlqEntity> findUnprocessedByEventType(@Param("eventType") String eventType);

	/**
	 * Find by original event ID.
	 */
	Optional<EventDlqEntity> findByOriginalEventId(String originalEventId);

	/**
	 * Count unprocessed events.
	 */
	@Query("SELECT COUNT(e) FROM EventDlqEntity e WHERE e.reprocessedAt IS NULL")
	long countUnprocessed();

	/**
	 * Count unprocessed events by event type.
	 */
	@Query("SELECT COUNT(e) FROM EventDlqEntity e WHERE e.reprocessedAt IS NULL AND e.eventType = :eventType")
	long countUnprocessedByEventType(@Param("eventType") String eventType);

	/**
	 * Find events created within a date range.
	 */
	List<EventDlqEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

	/**
	 * Find reprocessed events older than specified date (for cleanup).
	 */
	@Query("SELECT e FROM EventDlqEntity e WHERE e.reprocessedAt IS NOT NULL AND e.reprocessedAt < :before")
	List<EventDlqEntity> findReprocessedBefore(@Param("before") LocalDateTime before);
}
