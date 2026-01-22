package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Dead Letter Queue Entity.
 *
 * Stores events that failed to publish after max retries.
 * Allows manual intervention and reprocessing.
 */
@Entity
@Table(name = "event_dlq")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDlqEntity {

	@Id
	@Column(name = "dlq_id", columnDefinition = "CHAR(26)")
	private String dlqId;

	@Column(name = "original_event_id", columnDefinition = "CHAR(26)", nullable = false)
	private String originalEventId;

	@Column(name = "event_type", length = 64, nullable = false)
	private String eventType;

	@Column(name = "payload", columnDefinition = "JSON", nullable = false)
	private String payload;

	@Column(name = "failure_reason", columnDefinition = "TEXT")
	private String failureReason;

	@Column(name = "retry_count", nullable = false)
	@Builder.Default
	private Integer retryCount = 0;

	@Column(name = "last_retry_at")
	private LocalDateTime lastRetryAt;

	@Column(name = "reprocessed_at")
	private LocalDateTime reprocessedAt;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}

	/**
	 * Increment retry count for manual reprocessing attempt.
	 */
	public void incrementRetry(String error) {
		this.retryCount++;
		this.lastRetryAt = LocalDateTime.now();
		this.failureReason = error;
	}

	/**
	 * Mark as successfully reprocessed.
	 */
	public void markAsReprocessed() {
		this.reprocessedAt = LocalDateTime.now();
	}

	/**
	 * Check if already reprocessed.
	 */
	public boolean isReprocessed() {
		return reprocessedAt != null;
	}
}
