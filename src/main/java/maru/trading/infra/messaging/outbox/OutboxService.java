package maru.trading.infra.messaging.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.EventOutboxEntity;
import maru.trading.infra.persistence.jpa.repository.EventOutboxJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Outbox 이벤트 저장 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

	private final EventOutboxJpaRepository outboxRepository;
	private final ObjectMapper objectMapper;

	/**
	 * 이벤트를 Outbox에 저장
	 *
	 * @param event Outbox 이벤트
	 */
	@Transactional
	public void save(OutboxEvent event) {
		log.debug("Save outbox event: eventId={}, eventType={}",
				event.getEventId(), event.getEventType());

		EventOutboxEntity entity = EventOutboxEntity.builder()
				.outboxId(UlidGenerator.generate())
				.eventId(event.getEventId())
				.eventType(event.getEventType())
				.occurredAt(event.getOccurredAt() != null ? event.getOccurredAt() : LocalDateTime.now())
				.payloadJson(toJson(event.getPayload()))
				.retryCount(0)
				.build();

		outboxRepository.save(entity);
	}

	/**
	 * 이벤트 ID로 Outbox에서 이벤트 조회
	 */
	@Transactional(readOnly = true)
	public EventOutboxEntity findByEventId(String eventId) {
		return outboxRepository.findAll().stream()
				.filter(e -> e.getEventId().equals(eventId))
				.findFirst()
				.orElse(null);
	}

	private String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to serialize event payload", e);
		}
	}
}
