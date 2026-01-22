package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.EventOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventOutboxJpaRepository extends JpaRepository<EventOutboxEntity, String> {

	/**
	 * 이벤트 ID로 조회 (인덱스 활용)
	 */
	Optional<EventOutboxEntity> findByEventId(String eventId);

	@Query("SELECT e FROM EventOutboxEntity e WHERE e.publishedAt IS NULL " +
			"ORDER BY e.occurredAt ASC")
	List<EventOutboxEntity> findUnpublishedEvents();

	@Query("SELECT e FROM EventOutboxEntity e WHERE e.publishedAt IS NULL " +
			"AND e.retryCount < :maxRetry " +
			"ORDER BY e.occurredAt ASC")
	List<EventOutboxEntity> findUnpublishedEventsWithRetryLimit(@Param("maxRetry") int maxRetry);

	@Query("SELECT e FROM EventOutboxEntity e WHERE e.publishedAt IS NOT NULL " +
			"AND e.publishedAt < :before")
	List<EventOutboxEntity> findPublishedEventsBefore(@Param("before") LocalDateTime before);
}
