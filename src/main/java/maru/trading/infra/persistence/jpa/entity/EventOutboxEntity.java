package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_outbox")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventOutboxEntity {

	@Id
	@Column(name = "outbox_id", columnDefinition = "CHAR(26)")
	private String outboxId;

	@Column(name = "event_id", length = 64, nullable = false, unique = true)
	private String eventId;

	@Column(name = "event_type", length = 64, nullable = false)
	private String eventType;

	@Column(name = "occurred_at", nullable = false)
	private LocalDateTime occurredAt;

	@Column(name = "payload_json", columnDefinition = "JSON", nullable = false)
	private String payloadJson;

	@Column(name = "published_at")
	private LocalDateTime publishedAt;

	@Column(name = "retry_count", nullable = false)
	@Builder.Default
	private Integer retryCount = 0;

	@Column(name = "last_error", length = 512)
	private String lastError;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}

	public void markAsPublished() {
		this.publishedAt = LocalDateTime.now();
	}

	public void incrementRetry(String error) {
		this.retryCount++;
		this.lastError = error;
	}

	public boolean isPublished() {
		return publishedAt != null;
	}
}
