package maru.trading.application.usecase.trading;

import maru.trading.application.ports.broker.BrokerClient;
import maru.trading.application.ports.broker.BrokerResult;
import maru.trading.application.ports.repo.OrderRepository;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.OrderModificationException;
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
@DisplayName("ModifyOrderUseCase Test")
class ModifyOrderUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private BrokerClient brokerClient;

    @Mock
    private OutboxService outboxService;

    @Mock
    private UlidGenerator ulidGenerator;

    @InjectMocks
    private ModifyOrderUseCase modifyOrderUseCase;

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
    @DisplayName("Should modify order quantity successfully")
    void shouldModifyOrderQuantitySuccessfully() {
        // Given
        String orderId = "ORDER_001";
        BigDecimal newQty = BigDecimal.valueOf(20);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(brokerClient.modifyOrder(orderId, newQty, null)).thenReturn(BrokerResult.success("Modified"));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ulidGenerator.generateInstance()).thenReturn("EVENT_001");

        // When
        Order result = modifyOrderUseCase.execute(orderId, newQty, null);

        // Then
        assertThat(result.getQty()).isEqualTo(newQty);
        verify(brokerClient).modifyOrder(orderId, newQty, null);
    }

    @Test
    @DisplayName("Should modify order price successfully")
    void shouldModifyOrderPriceSuccessfully() {
        // Given
        String orderId = "ORDER_001";
        BigDecimal newPrice = BigDecimal.valueOf(72000);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(brokerClient.modifyOrder(orderId, null, newPrice)).thenReturn(BrokerResult.success("Modified"));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ulidGenerator.generateInstance()).thenReturn("EVENT_001");

        // When
        Order result = modifyOrderUseCase.execute(orderId, null, newPrice);

        // Then
        assertThat(result.getPrice()).isEqualTo(newPrice);
        verify(brokerClient).modifyOrder(orderId, null, newPrice);
    }

    @Test
    @DisplayName("Should modify both quantity and price")
    void shouldModifyBothQuantityAndPrice() {
        // Given
        String orderId = "ORDER_001";
        BigDecimal newQty = BigDecimal.valueOf(15);
        BigDecimal newPrice = BigDecimal.valueOf(71000);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(brokerClient.modifyOrder(orderId, newQty, newPrice)).thenReturn(BrokerResult.success("Modified"));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ulidGenerator.generateInstance()).thenReturn("EVENT_001");

        // When
        Order result = modifyOrderUseCase.execute(orderId, newQty, newPrice);

        // Then
        assertThat(result.getQty()).isEqualTo(newQty);
        assertThat(result.getPrice()).isEqualTo(newPrice);
    }

    @Test
    @DisplayName("Should throw exception when no parameter provided")
    void shouldThrowExceptionWhenNoParameterProvided() {
        // When & Then
        assertThatThrownBy(() -> modifyOrderUseCase.execute("ORDER_001", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one of newQty or newPrice must be provided");
    }

    @Test
    @DisplayName("Should throw exception when order not found")
    void shouldThrowExceptionWhenOrderNotFound() {
        // Given
        String orderId = "NON_EXISTENT";
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> modifyOrderUseCase.execute(orderId, BigDecimal.TEN, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    @DisplayName("Should throw exception when broker rejects modification")
    void shouldThrowExceptionWhenBrokerRejectsModification() {
        // Given
        String orderId = "ORDER_001";
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(brokerClient.modifyOrder(any(), any(), any()))
                .thenReturn(BrokerResult.failure("INVALID_QTY", "Invalid quantity"));

        // When & Then
        assertThatThrownBy(() -> modifyOrderUseCase.execute(orderId, BigDecimal.valueOf(1000000), null))
                .isInstanceOf(OrderModificationException.class);
    }

    @Test
    @DisplayName("Should publish ORDER_MODIFIED event")
    void shouldPublishOrderModifiedEvent() {
        // Given
        String orderId = "ORDER_001";
        BigDecimal newQty = BigDecimal.valueOf(20);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(brokerClient.modifyOrder(orderId, newQty, null)).thenReturn(BrokerResult.success("Modified"));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ulidGenerator.generateInstance()).thenReturn("EVENT_001");

        // When
        modifyOrderUseCase.execute(orderId, newQty, null);

        // Then
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxService).save(eventCaptor.capture());

        OutboxEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo("ORDER_MODIFIED");
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
        assertThatThrownBy(() -> modifyOrderUseCase.execute("ORDER_001", BigDecimal.valueOf(20), null))
                .isInstanceOf(OrderModificationException.class);
    }
}
