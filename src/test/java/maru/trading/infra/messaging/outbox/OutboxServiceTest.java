package maru.trading.infra.messaging.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import maru.trading.infra.persistence.jpa.entity.EventOutboxEntity;
import maru.trading.infra.persistence.jpa.repository.EventOutboxJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxService Test")
class OutboxServiceTest {

    @Mock
    private EventOutboxJpaRepository outboxRepository;

    private OutboxService outboxService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        outboxService = new OutboxService(outboxRepository, objectMapper);
    }

    @Nested
    @DisplayName("save() Tests")
    class SaveTests {

        @Test
        @DisplayName("Should save outbox event with all fields")
        void shouldSaveOutboxEventWithAllFields() {
            // Given
            Map<String, Object> payload = new HashMap<>();
            payload.put("orderId", "ORD_001");
            payload.put("symbol", "005930");

            OutboxEvent event = OutboxEvent.builder()
                    .eventId("EVT_001")
                    .eventType("ORDER_CREATED")
                    .occurredAt(LocalDateTime.now())
                    .payload(payload)
                    .build();

            when(outboxRepository.save(any(EventOutboxEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            outboxService.save(event);

            // Then
            ArgumentCaptor<EventOutboxEntity> captor = ArgumentCaptor.forClass(EventOutboxEntity.class);
            verify(outboxRepository).save(captor.capture());

            EventOutboxEntity savedEntity = captor.getValue();
            assertThat(savedEntity.getEventId()).isEqualTo("EVT_001");
            assertThat(savedEntity.getEventType()).isEqualTo("ORDER_CREATED");
            assertThat(savedEntity.getRetryCount()).isEqualTo(0);
            assertThat(savedEntity.getPayloadJson()).contains("orderId");
            assertThat(savedEntity.getPayloadJson()).contains("005930");
        }

        @Test
        @DisplayName("Should use current time when occurredAt is null")
        void shouldUseCurrentTimeWhenOccurredAtIsNull() {
            // Given
            OutboxEvent event = OutboxEvent.builder()
                    .eventId("EVT_002")
                    .eventType("ORDER_FILLED")
                    .payload(new HashMap<>())
                    .build();

            when(outboxRepository.save(any(EventOutboxEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            outboxService.save(event);
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);

            // Then
            ArgumentCaptor<EventOutboxEntity> captor = ArgumentCaptor.forClass(EventOutboxEntity.class);
            verify(outboxRepository).save(captor.capture());

            EventOutboxEntity savedEntity = captor.getValue();
            assertThat(savedEntity.getOccurredAt()).isAfter(before);
            assertThat(savedEntity.getOccurredAt()).isBefore(after);
        }

        @Test
        @DisplayName("Should generate unique outbox ID")
        void shouldGenerateUniqueOutboxId() {
            // Given
            OutboxEvent event1 = OutboxEvent.builder()
                    .eventId("EVT_001")
                    .eventType("ORDER_CREATED")
                    .payload(new HashMap<>())
                    .build();

            OutboxEvent event2 = OutboxEvent.builder()
                    .eventId("EVT_002")
                    .eventType("ORDER_CREATED")
                    .payload(new HashMap<>())
                    .build();

            ArgumentCaptor<EventOutboxEntity> captor = ArgumentCaptor.forClass(EventOutboxEntity.class);
            when(outboxRepository.save(any(EventOutboxEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            outboxService.save(event1);
            outboxService.save(event2);

            // Then
            verify(outboxRepository, times(2)).save(captor.capture());
            var savedEntities = captor.getAllValues();

            assertThat(savedEntities.get(0).getOutboxId())
                    .isNotEqualTo(savedEntities.get(1).getOutboxId());
        }

        @Test
        @DisplayName("Should handle complex payload")
        void shouldHandleComplexPayload() {
            // Given
            Map<String, Object> nestedPayload = new HashMap<>();
            nestedPayload.put("orderId", "ORD_001");
            nestedPayload.put("fills", java.util.List.of(
                    Map.of("fillId", "FILL_001", "qty", 10),
                    Map.of("fillId", "FILL_002", "qty", 5)
            ));

            OutboxEvent event = OutboxEvent.builder()
                    .eventId("EVT_001")
                    .eventType("ORDER_FILLED")
                    .payload(nestedPayload)
                    .build();

            when(outboxRepository.save(any(EventOutboxEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            outboxService.save(event);

            // Then
            ArgumentCaptor<EventOutboxEntity> captor = ArgumentCaptor.forClass(EventOutboxEntity.class);
            verify(outboxRepository).save(captor.capture());

            String payloadJson = captor.getValue().getPayloadJson();
            assertThat(payloadJson).contains("FILL_001");
            assertThat(payloadJson).contains("FILL_002");
        }
    }

    @Nested
    @DisplayName("findByEventId() Tests")
    class FindByEventIdTests {

        @Test
        @DisplayName("Should return entity when found")
        void shouldReturnEntityWhenFound() {
            // Given
            EventOutboxEntity entity = EventOutboxEntity.builder()
                    .outboxId("OUTBOX_001")
                    .eventId("EVT_001")
                    .eventType("ORDER_CREATED")
                    .occurredAt(LocalDateTime.now())
                    .payloadJson("{}")
                    .retryCount(0)
                    .build();

            when(outboxRepository.findByEventId("EVT_001")).thenReturn(Optional.of(entity));

            // When
            EventOutboxEntity result = outboxService.findByEventId("EVT_001");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEventId()).isEqualTo("EVT_001");
        }

        @Test
        @DisplayName("Should return null when not found")
        void shouldReturnNullWhenNotFound() {
            // Given
            when(outboxRepository.findByEventId("NONEXISTENT")).thenReturn(Optional.empty());

            // When
            EventOutboxEntity result = outboxService.findByEventId("NONEXISTENT");

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("JSON Serialization Tests")
    class JsonSerializationTests {

        @Test
        @DisplayName("Should serialize null payload")
        void shouldSerializeNullPayload() {
            // Given
            OutboxEvent event = OutboxEvent.builder()
                    .eventId("EVT_001")
                    .eventType("SIMPLE_EVENT")
                    .payload(null)
                    .build();

            when(outboxRepository.save(any(EventOutboxEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            outboxService.save(event);

            // Then
            ArgumentCaptor<EventOutboxEntity> captor = ArgumentCaptor.forClass(EventOutboxEntity.class);
            verify(outboxRepository).save(captor.capture());
            assertThat(captor.getValue().getPayloadJson()).isEqualTo("null");
        }

        @Test
        @DisplayName("Should serialize empty payload")
        void shouldSerializeEmptyPayload() {
            // Given
            OutboxEvent event = OutboxEvent.builder()
                    .eventId("EVT_001")
                    .eventType("EMPTY_EVENT")
                    .payload(new HashMap<>())
                    .build();

            when(outboxRepository.save(any(EventOutboxEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            outboxService.save(event);

            // Then
            ArgumentCaptor<EventOutboxEntity> captor = ArgumentCaptor.forClass(EventOutboxEntity.class);
            verify(outboxRepository).save(captor.capture());
            assertThat(captor.getValue().getPayloadJson()).isEqualTo("{}");
        }
    }
}
