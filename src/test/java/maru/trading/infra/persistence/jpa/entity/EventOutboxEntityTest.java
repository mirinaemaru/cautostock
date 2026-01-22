package maru.trading.infra.persistence.jpa.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EventOutboxEntity Test")
class EventOutboxEntityTest {

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create entity with all fields")
        void shouldCreateEntityWithAllFields() {
            // Given
            LocalDateTime now = LocalDateTime.now();

            // When
            EventOutboxEntity entity = EventOutboxEntity.builder()
                    .outboxId("OUTBOX_001")
                    .eventId("EVT_001")
                    .eventType("ORDER_CREATED")
                    .occurredAt(now)
                    .payloadJson("{\"orderId\": \"ORD_001\"}")
                    .retryCount(0)
                    .build();

            // Then
            assertThat(entity.getOutboxId()).isEqualTo("OUTBOX_001");
            assertThat(entity.getEventId()).isEqualTo("EVT_001");
            assertThat(entity.getEventType()).isEqualTo("ORDER_CREATED");
            assertThat(entity.getOccurredAt()).isEqualTo(now);
            assertThat(entity.getPayloadJson()).isEqualTo("{\"orderId\": \"ORD_001\"}");
            assertThat(entity.getRetryCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should default retryCount to 0")
        void shouldDefaultRetryCountToZero() {
            // When
            EventOutboxEntity entity = EventOutboxEntity.builder()
                    .outboxId("OUTBOX_001")
                    .eventId("EVT_001")
                    .eventType("ORDER_CREATED")
                    .occurredAt(LocalDateTime.now())
                    .payloadJson("{}")
                    .build();

            // Then
            assertThat(entity.getRetryCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("markAsPublished() Tests")
    class MarkAsPublishedTests {

        @Test
        @DisplayName("Should set publishedAt to current time")
        void shouldSetPublishedAtToCurrentTime() {
            // Given
            EventOutboxEntity entity = EventOutboxEntity.builder()
                    .outboxId("OUTBOX_001")
                    .eventId("EVT_001")
                    .eventType("ORDER_CREATED")
                    .occurredAt(LocalDateTime.now())
                    .payloadJson("{}")
                    .build();

            assertThat(entity.getPublishedAt()).isNull();

            // When
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            entity.markAsPublished();
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);

            // Then
            assertThat(entity.getPublishedAt()).isNotNull();
            assertThat(entity.getPublishedAt()).isAfter(before);
            assertThat(entity.getPublishedAt()).isBefore(after);
        }

        @Test
        @DisplayName("Should make isPublished return true")
        void shouldMakeIsPublishedReturnTrue() {
            // Given
            EventOutboxEntity entity = EventOutboxEntity.builder()
                    .outboxId("OUTBOX_001")
                    .eventId("EVT_001")
                    .eventType("ORDER_CREATED")
                    .occurredAt(LocalDateTime.now())
                    .payloadJson("{}")
                    .build();

            assertThat(entity.isPublished()).isFalse();

            // When
            entity.markAsPublished();

            // Then
            assertThat(entity.isPublished()).isTrue();
        }
    }

    @Nested
    @DisplayName("incrementRetry() Tests")
    class IncrementRetryTests {

        @Test
        @DisplayName("Should increment retry count")
        void shouldIncrementRetryCount() {
            // Given
            EventOutboxEntity entity = EventOutboxEntity.builder()
                    .outboxId("OUTBOX_001")
                    .eventId("EVT_001")
                    .eventType("ORDER_CREATED")
                    .occurredAt(LocalDateTime.now())
                    .payloadJson("{}")
                    .retryCount(0)
                    .build();

            // When
            entity.incrementRetry("Connection timeout");

            // Then
            assertThat(entity.getRetryCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should set last error message")
        void shouldSetLastErrorMessage() {
            // Given
            EventOutboxEntity entity = EventOutboxEntity.builder()
                    .outboxId("OUTBOX_001")
                    .eventId("EVT_001")
                    .eventType("ORDER_CREATED")
                    .occurredAt(LocalDateTime.now())
                    .payloadJson("{}")
                    .retryCount(0)
                    .build();

            // When
            entity.incrementRetry("Connection timeout");

            // Then
            assertThat(entity.getLastError()).isEqualTo("Connection timeout");
        }

        @Test
        @DisplayName("Should increment multiple times")
        void shouldIncrementMultipleTimes() {
            // Given
            EventOutboxEntity entity = EventOutboxEntity.builder()
                    .outboxId("OUTBOX_001")
                    .eventId("EVT_001")
                    .eventType("ORDER_CREATED")
                    .occurredAt(LocalDateTime.now())
                    .payloadJson("{}")
                    .retryCount(0)
                    .build();

            // When
            entity.incrementRetry("Error 1");
            entity.incrementRetry("Error 2");
            entity.incrementRetry("Error 3");

            // Then
            assertThat(entity.getRetryCount()).isEqualTo(3);
            assertThat(entity.getLastError()).isEqualTo("Error 3");
        }
    }

    @Nested
    @DisplayName("isPublished() Tests")
    class IsPublishedTests {

        @Test
        @DisplayName("Should return false when publishedAt is null")
        void shouldReturnFalseWhenPublishedAtIsNull() {
            // Given
            EventOutboxEntity entity = EventOutboxEntity.builder()
                    .outboxId("OUTBOX_001")
                    .eventId("EVT_001")
                    .eventType("ORDER_CREATED")
                    .occurredAt(LocalDateTime.now())
                    .payloadJson("{}")
                    .build();

            // Then
            assertThat(entity.isPublished()).isFalse();
        }

        @Test
        @DisplayName("Should return true when publishedAt is set")
        void shouldReturnTrueWhenPublishedAtIsSet() {
            // Given
            EventOutboxEntity entity = EventOutboxEntity.builder()
                    .outboxId("OUTBOX_001")
                    .eventId("EVT_001")
                    .eventType("ORDER_CREATED")
                    .occurredAt(LocalDateTime.now())
                    .payloadJson("{}")
                    .publishedAt(LocalDateTime.now())
                    .build();

            // Then
            assertThat(entity.isPublished()).isTrue();
        }
    }
}
