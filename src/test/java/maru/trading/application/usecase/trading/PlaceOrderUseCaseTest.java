package maru.trading.application.usecase.trading;

import maru.trading.TestFixtures;
import maru.trading.application.ports.broker.BrokerAck;
import maru.trading.application.ports.broker.BrokerClient;
import maru.trading.application.ports.repo.RiskStateRepository;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.OrderStatus;
import maru.trading.domain.order.OrderType;
import maru.trading.domain.order.Side;
import maru.trading.domain.risk.RiskDecision;
import maru.trading.domain.risk.RiskLimitExceededException;
import maru.trading.domain.risk.RiskState;
import maru.trading.infra.messaging.outbox.OutboxEvent;
import maru.trading.infra.messaging.outbox.OutboxService;
import maru.trading.infra.persistence.jpa.entity.OrderEntity;
import maru.trading.infra.persistence.jpa.repository.OrderJpaRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * PlaceOrderUseCase 테스트
 *
 * 테스트 범위:
 * 1. 멱등성 체크 (Idempotency)
 * 2. 리스크 체크 거부
 * 3. 브로커 전송 성공
 * 4. 브로커 거부
 * 5. 브로커 에러
 * 6. 이벤트 발행
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlaceOrderUseCase 테스트")
class PlaceOrderUseCaseTest {

    @Mock
    private OrderJpaRepository orderRepository;

    @Mock
    private EvaluateRiskUseCase evaluateRiskUseCase;

    @Mock
    private BrokerClient brokerClient;

    @Mock
    private OutboxService outboxService;

    @Mock
    private RiskStateRepository riskStateRepository;

    @InjectMocks
    private PlaceOrderUseCase placeOrderUseCase;

    private Order testOrder;
    private OrderEntity testOrderEntity;

    @BeforeEach
    void setUp() {
        testOrder = TestFixtures.placeMarketOrderWithPrice(
            "ORDER_001",
            "ACC_001",
            "005930",
            Side.BUY,
            BigDecimal.valueOf(10),
            BigDecimal.valueOf(70000),
            "IDEMPOTENCY_KEY_001"
        );

        testOrderEntity = OrderEntity.builder()
            .orderId("ORDER_001")
            .accountId("ACC_001")
            .symbol("005930")
            .side(Side.BUY)
            .orderType(OrderType.MARKET)
            .qty(BigDecimal.valueOf(10))
            .price(BigDecimal.valueOf(70000))
            .status(OrderStatus.NEW)
            .idempotencyKey("IDEMPOTENCY_KEY_001")
            .build();
    }

    // ==================== 1. Idempotency Tests ====================

    @Test
    @DisplayName("멱등성 체크 - 동일 키로 재요청 시 기존 주문 반환")
    void testIdempotency_DuplicateKey_ReturnExisting() {
        // Given
        OrderEntity existingOrder = OrderEntity.builder()
            .orderId("ORDER_EXISTING")
            .accountId("ACC_001")
            .symbol("005930")
            .side(Side.BUY)
            .orderType(OrderType.MARKET)
            .qty(BigDecimal.valueOf(10))
            .price(BigDecimal.valueOf(70000))
            .status(OrderStatus.SENT)
            .idempotencyKey("IDEMPOTENCY_KEY_001")
            .brokerOrderNo("BROKER_123")
            .build();

        when(orderRepository.findByIdempotencyKey("IDEMPOTENCY_KEY_001"))
            .thenReturn(Optional.of(existingOrder));

        // When
        Order result = placeOrderUseCase.execute(testOrder);

        // Then
        assertThat(result.getOrderId()).isEqualTo("ORDER_EXISTING");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.SENT);
        assertThat(result.getBrokerOrderNo()).isEqualTo("BROKER_123");

        // Verify no risk check or broker call
        verify(evaluateRiskUseCase, never()).evaluate(any());
        verify(brokerClient, never()).placeOrder(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("멱등성 체크 - 신규 키로 요청 시 정상 처리")
    void testIdempotency_NewKey_Process() {
        // Given
        when(orderRepository.findByIdempotencyKey("IDEMPOTENCY_KEY_001"))
            .thenReturn(Optional.empty());
        when(evaluateRiskUseCase.evaluate(any())).thenReturn(RiskDecision.approve());
        when(orderRepository.save(any())).thenReturn(testOrderEntity);
        when(brokerClient.placeOrder(any())).thenReturn(BrokerAck.success("BROKER_123"));

        // When
        Order result = placeOrderUseCase.execute(testOrder);

        // Then
        verify(orderRepository).findByIdempotencyKey("IDEMPOTENCY_KEY_001");
        verify(evaluateRiskUseCase).evaluate(any());
        verify(brokerClient).placeOrder(any());
    }

    @Test
    @DisplayName("멱등성 키 없이 요청 - 정상 처리")
    void testIdempotency_NoKey_Process() {
        // Given
        Order orderWithoutKey = TestFixtures.placeMarketOrderWithPrice(
            "ORDER_002",
            "ACC_001",
            "005930",
            Side.BUY,
            BigDecimal.valueOf(10),
            BigDecimal.valueOf(70000),
            null // No idempotency key
        );

        when(evaluateRiskUseCase.evaluate(any())).thenReturn(RiskDecision.approve());
        when(orderRepository.save(any())).thenReturn(testOrderEntity);
        when(brokerClient.placeOrder(any())).thenReturn(BrokerAck.success("BROKER_123"));

        // When
        Order result = placeOrderUseCase.execute(orderWithoutKey);

        // Then
        verify(orderRepository, never()).findByIdempotencyKey(any());
        verify(evaluateRiskUseCase).evaluate(any());
        verify(brokerClient).placeOrder(any());
    }

    // ==================== 2. Risk Check Tests ====================

    @Test
    @DisplayName("리스크 체크 거부 - RiskLimitExceededException 발생")
    void testRiskCheck_Rejected_ThrowException() {
        // Given
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(evaluateRiskUseCase.evaluate(any()))
            .thenReturn(RiskDecision.reject("Daily loss limit exceeded", "DAILY_LOSS_LIMIT"));

        // When & Then
        assertThatThrownBy(() -> placeOrderUseCase.execute(testOrder))
            .isInstanceOf(RiskLimitExceededException.class)
            .hasMessageContaining("Daily loss limit exceeded");

        // Verify no broker call or save
        verify(brokerClient, never()).placeOrder(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("리스크 체크 승인 - 정상 처리")
    void testRiskCheck_Approved_Process() {
        // Given
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(evaluateRiskUseCase.evaluate(any())).thenReturn(RiskDecision.approve());
        when(orderRepository.save(any())).thenReturn(testOrderEntity);
        when(brokerClient.placeOrder(any())).thenReturn(BrokerAck.success("BROKER_123"));

        // When
        Order result = placeOrderUseCase.execute(testOrder);

        // Then
        verify(evaluateRiskUseCase).evaluate(any());
        verify(brokerClient).placeOrder(any());
    }

    // ==================== 3. Broker Success Tests ====================

    @Test
    @DisplayName("브로커 전송 성공 - SENT 상태로 업데이트")
    void testBrokerSuccess_UpdateToSent() {
        // Given
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(evaluateRiskUseCase.evaluate(any())).thenReturn(RiskDecision.approve());

        OrderEntity savedEntity = OrderEntity.builder()
            .orderId("ORDER_001")
            .accountId("ACC_001")
            .symbol("005930")
            .side(Side.BUY)
            .orderType(OrderType.MARKET)
            .qty(BigDecimal.valueOf(10))
            .price(BigDecimal.valueOf(70000))
            .status(OrderStatus.NEW)
            .idempotencyKey("IDEMPOTENCY_KEY_001")
            .build();

        when(orderRepository.save(any())).thenReturn(savedEntity);
        when(brokerClient.placeOrder(any())).thenReturn(BrokerAck.success("BROKER_123"));

        // When
        Order result = placeOrderUseCase.execute(testOrder);

        // Then
        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository, atLeast(2)).save(orderCaptor.capture());

        // Verify status updated to SENT
        OrderEntity finalSave = orderCaptor.getAllValues().get(orderCaptor.getAllValues().size() - 1);
        assertThat(finalSave.getStatus()).isEqualTo(OrderStatus.SENT);
        assertThat(finalSave.getBrokerOrderNo()).isEqualTo("BROKER_123");
    }

    @Test
    @DisplayName("브로커 전송 성공 - ORDER_SENT 이벤트 발행")
    void testBrokerSuccess_PublishEvent() {
        // Given
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(evaluateRiskUseCase.evaluate(any())).thenReturn(RiskDecision.approve());
        when(orderRepository.save(any())).thenReturn(testOrderEntity);
        when(brokerClient.placeOrder(any())).thenReturn(BrokerAck.success("BROKER_123"));

        // When
        placeOrderUseCase.execute(testOrder);

        // Then
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxService).save(eventCaptor.capture());

        OutboxEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo("ORDER_SENT");
        assertThat(event.getPayload()).containsEntry("orderId", "ORDER_001");
        assertThat(event.getPayload()).containsEntry("symbol", "005930");
    }

    // ==================== 4. Broker Rejection Tests ====================

    @Test
    @DisplayName("브로커 거부 - REJECTED 상태로 업데이트")
    void testBrokerRejection_UpdateToRejected() {
        // Given
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(evaluateRiskUseCase.evaluate(any())).thenReturn(RiskDecision.approve());

        OrderEntity savedEntity = OrderEntity.builder()
            .orderId("ORDER_001")
            .accountId("ACC_001")
            .symbol("005930")
            .side(Side.BUY)
            .orderType(OrderType.MARKET)
            .qty(BigDecimal.valueOf(10))
            .price(BigDecimal.valueOf(70000))
            .status(OrderStatus.NEW)
            .idempotencyKey("IDEMPOTENCY_KEY_001")
            .build();

        when(orderRepository.save(any())).thenReturn(savedEntity);
        when(brokerClient.placeOrder(any()))
            .thenReturn(BrokerAck.failure("INSUFFICIENT_BALANCE", "Insufficient balance"));

        // When
        Order result = placeOrderUseCase.execute(testOrder);

        // Then
        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository, atLeast(2)).save(orderCaptor.capture());

        // Verify rejection info updated
        OrderEntity finalSave = orderCaptor.getAllValues().get(orderCaptor.getAllValues().size() - 1);
        assertThat(finalSave.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(finalSave.getRejectCode()).isEqualTo("INSUFFICIENT_BALANCE");
        assertThat(finalSave.getRejectMessage()).isEqualTo("Insufficient balance");
    }

    @Test
    @DisplayName("브로커 거부 - ORDER_REJECTED 이벤트 발행")
    void testBrokerRejection_PublishEvent() {
        // Given
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(evaluateRiskUseCase.evaluate(any())).thenReturn(RiskDecision.approve());
        when(orderRepository.save(any())).thenReturn(testOrderEntity);
        when(brokerClient.placeOrder(any()))
            .thenReturn(BrokerAck.failure("INSUFFICIENT_BALANCE", "Insufficient balance"));

        // When
        placeOrderUseCase.execute(testOrder);

        // Then
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxService).save(eventCaptor.capture());

        OutboxEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo("ORDER_REJECTED");
        assertThat(event.getPayload()).containsEntry("orderId", "ORDER_001");
    }

    // ==================== 5. Broker Error Tests ====================

    @Test
    @DisplayName("브로커 에러 - ERROR 상태로 업데이트")
    void testBrokerError_UpdateToError() {
        // Given
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(evaluateRiskUseCase.evaluate(any())).thenReturn(RiskDecision.approve());

        // Return the same entity that was passed to save (to capture mutations)
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(brokerClient.placeOrder(any()))
            .thenThrow(new RuntimeException("Network timeout"));

        // When
        Order result = placeOrderUseCase.execute(testOrder);

        // Then
        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository, atLeast(2)).save(orderCaptor.capture());

        // Verify error info updated
        OrderEntity finalSave = orderCaptor.getAllValues().get(orderCaptor.getAllValues().size() - 1);
        assertThat(finalSave.getStatus()).isEqualTo(OrderStatus.ERROR);
        assertThat(finalSave.getRejectCode()).isEqualTo("BROKER_ERROR");
        assertThat(finalSave.getRejectMessage()).contains("Network timeout");
    }

    @Test
    @DisplayName("브로커 에러 - ORDER_ERROR 이벤트 발행")
    void testBrokerError_PublishEvent() {
        // Given
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(evaluateRiskUseCase.evaluate(any())).thenReturn(RiskDecision.approve());
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(brokerClient.placeOrder(any()))
            .thenThrow(new RuntimeException("Network timeout"));

        // When
        placeOrderUseCase.execute(testOrder);

        // Then
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxService).save(eventCaptor.capture());

        OutboxEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo("ORDER_ERROR");
        assertThat(event.getPayload()).containsEntry("orderId", "ORDER_001");
    }

    // ==================== 6. Full Flow Integration Tests ====================

    @Test
    @DisplayName("전체 플로우 - 성공 시나리오")
    void testFullFlow_Success() {
        // Given
        when(orderRepository.findByIdempotencyKey("IDEMPOTENCY_KEY_001"))
            .thenReturn(Optional.empty());
        when(evaluateRiskUseCase.evaluate(any()))
            .thenReturn(RiskDecision.approve());

        OrderEntity newEntity = OrderEntity.builder()
            .orderId("ORDER_001")
            .accountId("ACC_001")
            .symbol("005930")
            .side(Side.BUY)
            .orderType(OrderType.MARKET)
            .qty(BigDecimal.valueOf(10))
            .price(BigDecimal.valueOf(70000))
            .status(OrderStatus.NEW)
            .idempotencyKey("IDEMPOTENCY_KEY_001")
            .build();

        when(orderRepository.save(any())).thenReturn(newEntity);
        when(brokerClient.placeOrder(any())).thenReturn(BrokerAck.success("BROKER_123"));

        // When
        Order result = placeOrderUseCase.execute(testOrder);

        // Then
        // 1. Idempotency check
        verify(orderRepository).findByIdempotencyKey("IDEMPOTENCY_KEY_001");

        // 2. Risk evaluation
        verify(evaluateRiskUseCase).evaluate(any());

        // 3. Save order
        verify(orderRepository, atLeast(2)).save(any());

        // 4. Broker call
        verify(brokerClient).placeOrder(any());

        // 5. Event published
        verify(outboxService).save(any());

        // Result should be SENT status
        assertThat(result.getStatus()).isEqualTo(OrderStatus.SENT);
    }
}
