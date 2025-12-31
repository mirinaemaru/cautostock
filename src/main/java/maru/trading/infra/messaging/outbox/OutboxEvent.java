package maru.trading.infra.messaging.outbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Outbox 이벤트 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {
	private String eventId;
	private String eventType;
	private LocalDateTime occurredAt;
	private Map<String, Object> payload;
}
