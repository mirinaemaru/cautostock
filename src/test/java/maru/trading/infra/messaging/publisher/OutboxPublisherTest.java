package maru.trading.infra.messaging.publisher;

import maru.trading.infra.persistence.jpa.entity.EventDlqEntity;
import maru.trading.infra.persistence.jpa.entity.EventOutboxEntity;
import maru.trading.infra.persistence.jpa.repository.EventDlqJpaRepository;
import maru.trading.infra.persistence.jpa.repository.EventOutboxJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxPublisher Test")
class OutboxPublisherTest {

    @Mock
    private EventOutboxJpaRepository outboxRepository;

    @Mock
    private EventDlqJpaRepository dlqRepository;

    private OutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {
        outboxPublisher = new OutboxPublisher(outboxRepository, dlqRepository);
        // Set default values using reflection
        ReflectionTestUtils.setField(outboxPublisher, "enabled", true);
        ReflectionTestUtils.setField(outboxPublisher, "batchSize", 100);
        ReflectionTestUtils.setField(outboxPublisher, "maxRetry", 3);
    }

    @Nested
    @DisplayName("publishPendingEvents() Tests")
    class PublishPendingEventsTests {

        @Test
        @DisplayName("Should skip when disabled")
        void shouldSkipWhenDisabled() {
            // Given
            ReflectionTestUtils.setField(outboxPublisher, "enabled", false);

            // When
            outboxPublisher.publishPendingEvents();

            // Then
            verifyNoInteractions(outboxRepository);
        }

        @Test
        @DisplayName("Should skip when no unpublished events")
        void shouldSkipWhenNoUnpublishedEvents() {
            // Given
            when(outboxRepository.findUnpublishedEventsWithRetryLimit(anyInt()))
                    .thenReturn(Collections.emptyList());

            // When
            outboxPublisher.publishPendingEvents();

            // Then
            verify(outboxRepository).findUnpublishedEventsWithRetryLimit(3);
            verify(outboxRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should publish event and mark as published")
        void shouldPublishEventAndMarkAsPublished() {
            // Given
            EventOutboxEntity event = createOutboxEvent("EVT_001", "ORDER_CREATED", 0);
            when(outboxRepository.findUnpublishedEventsWithRetryLimit(anyInt()))
                    .thenReturn(List.of(event));

            // When
            outboxPublisher.publishPendingEvents();

            // Then
            verify(outboxRepository).save(event);
            assertThat(event.isPublished()).isTrue();
        }

        @Test
        @DisplayName("Should publish multiple events")
        void shouldPublishMultipleEvents() {
            // Given
            EventOutboxEntity event1 = createOutboxEvent("EVT_001", "ORDER_CREATED", 0);
            EventOutboxEntity event2 = createOutboxEvent("EVT_002", "ORDER_FILLED", 0);
            when(outboxRepository.findUnpublishedEventsWithRetryLimit(anyInt()))
                    .thenReturn(List.of(event1, event2));

            // When
            outboxPublisher.publishPendingEvents();

            // Then
            verify(outboxRepository, times(2)).save(any(EventOutboxEntity.class));
        }

        @Test
        @DisplayName("Should increment retry count on failure")
        void shouldIncrementRetryCountOnFailure() {
            // Given
            EventOutboxEntity event = spy(createOutboxEvent("EVT_001", "ORDER_CREATED", 0));
            when(outboxRepository.findUnpublishedEventsWithRetryLimit(anyInt()))
                    .thenReturn(List.of(event));

            // Simulate failure by making the event's markAsPublished throw an error
            // (In real scenario, publishEvent might fail - here we test retry logic)
            doAnswer(inv -> {
                throw new RuntimeException("Simulated failure");
            }).when(event).markAsPublished();

            // When
            outboxPublisher.publishPendingEvents();

            // Then
            verify(event).incrementRetry(anyString());
        }
    }

    @Nested
    @DisplayName("cleanupPublishedEvents() Tests")
    class CleanupPublishedEventsTests {

        @Test
        @DisplayName("Should skip when disabled")
        void shouldSkipWhenDisabled() {
            // Given
            ReflectionTestUtils.setField(outboxPublisher, "enabled", false);

            // When
            outboxPublisher.cleanupPublishedEvents();

            // Then
            verifyNoInteractions(outboxRepository);
        }

        @Test
        @DisplayName("Should delete old published events")
        void shouldDeleteOldPublishedEvents() {
            // Given
            EventOutboxEntity oldEvent1 = createOutboxEvent("EVT_001", "OLD_EVENT", 0);
            oldEvent1.markAsPublished();
            EventOutboxEntity oldEvent2 = createOutboxEvent("EVT_002", "OLD_EVENT", 0);
            oldEvent2.markAsPublished();

            when(outboxRepository.findPublishedEventsBefore(any(LocalDateTime.class)))
                    .thenReturn(List.of(oldEvent1, oldEvent2));

            // When
            outboxPublisher.cleanupPublishedEvents();

            // Then
            verify(outboxRepository).deleteAll(List.of(oldEvent1, oldEvent2));
        }

        @Test
        @DisplayName("Should skip when no old events to clean")
        void shouldSkipWhenNoOldEventsToClean() {
            // Given
            when(outboxRepository.findPublishedEventsBefore(any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            outboxPublisher.cleanupPublishedEvents();

            // Then
            verify(outboxRepository, never()).deleteAll(anyList());
        }
    }

    @Nested
    @DisplayName("Dead Letter Queue Tests")
    class DeadLetterQueueTests {

        @Test
        @DisplayName("Should move event to DLQ after max retries")
        void shouldMoveEventToDlqAfterMaxRetries() {
            // Given
            EventOutboxEntity event = spy(createOutboxEvent("EVT_001", "FAILING_EVENT", 2));
            when(outboxRepository.findUnpublishedEventsWithRetryLimit(anyInt()))
                    .thenReturn(List.of(event));

            // Simulate failure
            doAnswer(inv -> {
                throw new RuntimeException("Persistent failure");
            }).when(event).markAsPublished();

            // When
            outboxPublisher.publishPendingEvents();

            // Then - should move to DLQ (retry count becomes 3 which equals maxRetry)
            ArgumentCaptor<EventDlqEntity> dlqCaptor = ArgumentCaptor.forClass(EventDlqEntity.class);
            verify(dlqRepository).save(dlqCaptor.capture());
            verify(outboxRepository).delete(event);

            EventDlqEntity dlqEvent = dlqCaptor.getValue();
            assertThat(dlqEvent.getOriginalEventId()).isEqualTo("EVT_001");
            assertThat(dlqEvent.getEventType()).isEqualTo("FAILING_EVENT");
        }

        @Test
        @DisplayName("Should keep event in outbox if retry count is below max")
        void shouldKeepEventInOutboxIfRetryCountIsBelowMax() {
            // Given
            EventOutboxEntity event = spy(createOutboxEvent("EVT_001", "FAILING_EVENT", 0));
            when(outboxRepository.findUnpublishedEventsWithRetryLimit(anyInt()))
                    .thenReturn(List.of(event));

            // Simulate failure
            doAnswer(inv -> {
                throw new RuntimeException("Temporary failure");
            }).when(event).markAsPublished();

            // When
            outboxPublisher.publishPendingEvents();

            // Then - should NOT move to DLQ, just save with incremented retry
            verify(dlqRepository, never()).save(any(EventDlqEntity.class));
            verify(outboxRepository).save(event);
        }
    }

    // ==================== Helper Methods ====================

    private EventOutboxEntity createOutboxEvent(String eventId, String eventType, int retryCount) {
        return EventOutboxEntity.builder()
                .outboxId("OUTBOX_" + eventId)
                .eventId(eventId)
                .eventType(eventType)
                .occurredAt(LocalDateTime.now())
                .payloadJson("{\"test\": true}")
                .retryCount(retryCount)
                .build();
    }
}
