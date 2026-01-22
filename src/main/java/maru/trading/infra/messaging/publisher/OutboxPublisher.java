package maru.trading.infra.messaging.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.EventDlqEntity;
import maru.trading.infra.persistence.jpa.entity.EventOutboxEntity;
import maru.trading.infra.persistence.jpa.repository.EventDlqJpaRepository;
import maru.trading.infra.persistence.jpa.repository.EventOutboxJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 이벤트 발행 서비스
 *
 * Outbox 테이블에서 미발행 이벤트를 주기적으로 읽어 외부로 발행합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

	private final EventOutboxJpaRepository outboxRepository;
	private final EventDlqJpaRepository dlqRepository;

	@Value("${trading.outbox.publisher.enabled:true}")
	private boolean enabled;

	@Value("${trading.outbox.publisher.batch-size:100}")
	private int batchSize;

	@Value("${trading.outbox.publisher.max-retry:3}")
	private int maxRetry;

	/**
	 * 미발행 이벤트를 주기적으로 발행
	 *
	 * 기본 1초마다 실행 (application.yml에서 설정 가능)
	 */
	@Scheduled(fixedDelayString = "${trading.outbox.publisher.poll-interval-ms:1000}")
	@Transactional
	public void publishPendingEvents() {
		if (!enabled) {
			return;
		}

		List<EventOutboxEntity> unpublished = outboxRepository
				.findUnpublishedEventsWithRetryLimit(maxRetry);

		if (unpublished.isEmpty()) {
			return;
		}

		log.debug("Publishing {} pending events", unpublished.size());

		for (EventOutboxEntity event : unpublished) {
			try {
				// 실제 발행 로직 (Kafka, RabbitMQ, HTTP 등)
				publishEvent(event);

				// 발행 성공 시 표시
				event.markAsPublished();
				outboxRepository.save(event);

				log.info("Published event: eventId={}, eventType={}",
						event.getEventId(), event.getEventType());

			} catch (Exception e) {
				log.error("Failed to publish event: eventId={}, retry={}",
						event.getEventId(), event.getRetryCount(), e);

				// 재시도 카운트 증가
				event.incrementRetry(e.getMessage());

				// 최대 재시도 초과 시 DLQ로 이동
				if (event.getRetryCount() >= maxRetry) {
					moveToDeadLetterQueue(event, e.getMessage());
				} else {
					outboxRepository.save(event);
				}
			}
		}
	}

	/**
	 * Move failed event to Dead Letter Queue.
	 * This allows manual intervention and reprocessing.
	 */
	private void moveToDeadLetterQueue(EventOutboxEntity event, String failureReason) {
		try {
			EventDlqEntity dlqEvent = EventDlqEntity.builder()
					.dlqId(UlidGenerator.generate())
					.originalEventId(event.getEventId())
					.eventType(event.getEventType())
					.payload(event.getPayloadJson())
					.failureReason(failureReason)
					.retryCount(event.getRetryCount())
					.lastRetryAt(LocalDateTime.now())
					.build();

			dlqRepository.save(dlqEvent);

			// Remove from outbox after moving to DLQ
			outboxRepository.delete(event);

			log.warn("Event moved to DLQ: eventId={}, eventType={}, retryCount={}",
					event.getEventId(), event.getEventType(), event.getRetryCount());

		} catch (Exception dlqError) {
			log.error("Failed to move event to DLQ: eventId={}", event.getEventId(), dlqError);
			// Keep in outbox with error - don't lose the event
			outboxRepository.save(event);
		}
	}

	/**
	 * 실제 이벤트 발행 (Stub 구현)
	 *
	 * MVP에서는 로그만 출력
	 * 실제 구현 시 Kafka, RabbitMQ, HTTP Webhook 등으로 전송
	 */
	private void publishEvent(EventOutboxEntity event) {
		// MVP: 로그만 출력
		log.info("[OUTBOX] Event published: eventId={}, eventType={}, payload={}",
				event.getEventId(),
				event.getEventType(),
				event.getPayloadJson());

		// 실제 구현 예시:
		// kafkaTemplate.send("trading-events", event.getEventType(), event.getPayloadJson());
		// rabbitTemplate.convertAndSend("trading-exchange", event.getEventType(), event.getPayloadJson());
		// webClient.post().uri("/webhook").bodyValue(event.getPayloadJson()).retrieve().toBodilessEntity().block();
	}

	/**
	 * 발행된 이벤트 정리 (오래된 이벤트 삭제)
	 *
	 * 매일 새벽 3시에 실행
	 */
	@Scheduled(cron = "0 0 3 * * *")
	@Transactional
	public void cleanupPublishedEvents() {
		if (!enabled) {
			return;
		}

		// 7일 이전 발행된 이벤트 삭제
		java.time.LocalDateTime before = java.time.LocalDateTime.now().minusDays(7);
		List<EventOutboxEntity> oldEvents = outboxRepository.findPublishedEventsBefore(before);

		if (!oldEvents.isEmpty()) {
			outboxRepository.deleteAll(oldEvents);
			log.info("Cleaned up {} old published events", oldEvents.size());
		}
	}
}
