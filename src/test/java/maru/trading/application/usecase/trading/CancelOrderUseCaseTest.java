package maru.trading.application.usecase.trading;

import maru.trading.application.ports.broker.BrokerClient;
import maru.trading.application.ports.broker.BrokerResult;
import maru.trading.application.ports.repo.OrderRepository;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.OrderCancellationException;
import maru.trading.domain.order.OrderStatus;
import maru.trading.domain.order.OrderType;
import maru.trading.domain.order.Side;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.messaging.outbox.OutboxEvent;
import maru.trading.infra.messaging.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelOrderUseCase Test")
class CancelOrderUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private BrokerClient brokerClient;

    @Mock
    private OutboxService outboxService;

    @Mock
    private UlidGenerator ulidGenerator;

    @InjectMocks
    private CancelOrderUseCase cancelOrderUseCase;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .orderId("ORDER_001")
                .accountId("ACC_001")
                .symbol("005930")
                .side(Side.BUY)
                .orderType(OrderType.LIMIT)
                .qty(BigDecimal.valueOf(10))
                .price(BigDecimal.valueOf(70000))
                .status(OrderStatus.ACCEPTED)
                .build();
    }

    @Test
    @DisplayName("Should cancel order successfully")
    void shouldCancelOrderSuccessfully() {
        // Given
        String orderId = "ORDER_001";
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(brokerClient.cancelOrder(orderId)).thenReturn(BrokerResult.success("Cancelled"));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ulidGenerator.generateInstance()).thenReturn("EVENT_001");

        // When
        Order result = cancelOrderUseCase.execute(orderId);

        // Then
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(brokerClient).cancelOrder(orderId);
        verify(orderRepository).save(any());
    }

    @Test
    @DisplayName("Should throw exception when order not found")
    void shouldThrowExceptionWhenOrderNotFound() {
        // Given
        String orderId = "NON_EXISTENT";
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> cancelOrderUseCase.execute(orderId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    @DisplayName("Should throw exception when broker rejects cancel")
    void shouldThrowExceptionWhenBrokerRejectsCancelRequest() {
        // Given
        String orderId = "ORDER_001";
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(brokerClient.cancelOrder(orderId))
                .thenReturn(BrokerResult.failure("ORDER_ALREADY_FILLED", "Order already filled"));

        // When & Then
        assertThatThrownBy(() -> cancelOrderUseCase.execute(orderId))
                .isInstanceOf(OrderCancellationException.class);
    }

    @Test
    @DisplayName("Should publish ORDER_CANCELLED event")
    void shouldPublishOrderCancelledEvent() {
        // Given
        String orderId = "ORDER_001";
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(brokerClient.cancelOrder(orderId)).thenReturn(BrokerResult.success("Cancelled"));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ulidGenerator.generateInstance()).thenReturn("EVENT_001");

        // When
        cancelOrderUseCase.execute(orderId);

        // Then
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxService).save(eventCaptor.capture());

        OutboxEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo("ORDER_CANCELLED");
        assertThat(event.getPayload()).containsEntry("orderId", orderId);
    }

    @Test
    @DisplayName("Should throw exception when order already filled")
    void shouldThrowExceptionWhenOrderAlreadyFilled() {
        // Given
        Order filledOrder = Order.builder()
                .orderId("ORDER_001")
                .accountId("ACC_001")
                .symbol("005930")
                .side(Side.BUY)
                .orderType(OrderType.LIMIT)
                .qty(BigDecimal.valueOf(10))
                .price(BigDecimal.valueOf(70000))
                .status(OrderStatus.FILLED)
                .build();

        when(orderRepository.findById("ORDER_001")).thenReturn(Optional.of(filledOrder));

        // When & Then
        assertThatThrownBy(() -> cancelOrderUseCase.execute("ORDER_001"))
                .isInstanceOf(OrderCancellationException.class);
    }
}
