package maru.trading.application.usecase.trading;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.application.ports.broker.BrokerAck;
import maru.trading.application.ports.broker.BrokerClient;
import maru.trading.application.ports.repo.RiskStateRepository;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.OrderStatus;
import maru.trading.domain.risk.RiskDecision;
import maru.trading.domain.risk.RiskLimitExceededException;
import maru.trading.domain.risk.RiskState;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.messaging.outbox.OutboxEvent;
import maru.trading.infra.messaging.outbox.OutboxService;
import maru.trading.infra.persistence.jpa.entity.OrderEntity;
import maru.trading.infra.persistence.jpa.repository.OrderJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 주문 실행 Use Case
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceOrderUseCase {

	private final OrderJpaRepository orderRepository;
	private final EvaluateRiskUseCase evaluateRiskUseCase;
	private final BrokerClient brokerClient;
	private final OutboxService outboxService;
	private final RiskStateRepository riskStateRepository;

	/**
	 * 주문 생성 및 전송
	 */
	@Transactional
	public Order execute(Order order) {
		log.info("Place order: orderId={}, symbol={}, side={}, qty={}",
				order.getOrderId(), order.getSymbol(), order.getSide(), order.getQty());

		// 1. 멱등성 체크
		String idempotencyKey = order.getIdempotencyKey();
		if (idempotencyKey != null) {
			OrderEntity existing = orderRepository.findByIdempotencyKey(idempotencyKey)
					.orElse(null);
			if (existing != null) {
				log.warn("Duplicate order detected: idempotencyKey={}", idempotencyKey);
				return toOrder(existing);
			}
		}

		// 2. 리스크 평가
		RiskDecision riskDecision = evaluateRiskUseCase.evaluate(order);
		if (!riskDecision.isApproved()) {
			log.warn("Order rejected by risk engine: reason={}", riskDecision.getReason());
			throw new RiskLimitExceededException(riskDecision.getReason());
		}

		// 2.5. 주문 빈도 추적을 위한 타임스탬프 기록
		recordOrderTimestamp(order.getAccountId());

		// 3. 주문 저장 (NEW 상태)
		OrderEntity orderEntity = OrderEntity.builder()
				.orderId(order.getOrderId())
				.accountId(order.getAccountId())
				.strategyId(order.getStrategyId())
				.strategyVersionId(order.getStrategyVersionId())
				.signalId(order.getSignalId())
				.symbol(order.getSymbol())
				.side(order.getSide())
				.orderType(order.getOrderType())
				.ordDvsn(order.getOrdDvsn())
				.qty(order.getQty())
				.price(order.getPrice())
				.status(OrderStatus.NEW)
				.idempotencyKey(idempotencyKey)
				.build();

		OrderEntity saved = orderRepository.save(orderEntity);

		// 4. 브로커에 주문 전송
		try {
			BrokerAck ack = brokerClient.placeOrder(toOrder(saved));

			if (ack.isSuccess()) {
				// 전송 성공
				saved.updateStatus(OrderStatus.SENT);
				saved.updateBrokerOrderNo(ack.getBrokerOrderNo());
				orderRepository.save(saved);

				log.info("Order sent successfully: orderId={}, brokerOrderNo={}",
						saved.getOrderId(), ack.getBrokerOrderNo());

				// 이벤트 발행
				publishOrderEvent(saved, "ORDER_SENT");

			} else {
				// 전송 실패
				saved.updateRejection(ack.getErrorCode(), ack.getMessage());
				orderRepository.save(saved);

				log.error("Order rejected by broker: orderId={}, error={}",
						saved.getOrderId(), ack.getMessage());

				// 이벤트 발행
				publishOrderEvent(saved, "ORDER_REJECTED");
			}

		} catch (Exception e) {
			// 전송 에러
			log.error("Failed to send order to broker: orderId={}", saved.getOrderId(), e);
			saved.updateRejection("BROKER_ERROR", e.getMessage());
			saved.updateStatus(OrderStatus.ERROR);  // updateRejection이 REJECTED로 설정하므로 이후에 ERROR로 덮어씀
			orderRepository.save(saved);

			// 이벤트 발행
			publishOrderEvent(saved, "ORDER_ERROR");
		}

		return toOrder(saved);
	}

	private Order toOrder(OrderEntity entity) {
		return Order.builder()
				.orderId(entity.getOrderId())
				.accountId(entity.getAccountId())
				.strategyId(entity.getStrategyId())
				.strategyVersionId(entity.getStrategyVersionId())
				.signalId(entity.getSignalId())
				.symbol(entity.getSymbol())
				.side(entity.getSide())
				.orderType(entity.getOrderType())
				.ordDvsn(entity.getOrdDvsn())
				.qty(entity.getQty())
				.price(entity.getPrice())
				.status(entity.getStatus())
				.idempotencyKey(entity.getIdempotencyKey())
				.brokerOrderNo(entity.getBrokerOrderNo())
				.build();
	}

	private void publishOrderEvent(OrderEntity order, String eventType) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("orderId", order.getOrderId());
		payload.put("accountId", order.getAccountId());
		payload.put("symbol", order.getSymbol());
		payload.put("side", order.getSide().name());
		payload.put("qty", order.getQty());
		payload.put("status", order.getStatus().name());
		payload.put("brokerOrderNo", order.getBrokerOrderNo());

		OutboxEvent event = OutboxEvent.builder()
				.eventId(UlidGenerator.generate())
				.eventType(eventType)
				.occurredAt(LocalDateTime.now())
				.payload(payload)
				.build();

		outboxService.save(event);
	}

	/**
	 * 주문 빈도 추적을 위한 타임스탬프 기록
	 */
	private void recordOrderTimestamp(String accountId) {
		try {
			RiskState state = riskStateRepository.findByAccountId(accountId)
					.orElseGet(() -> riskStateRepository.findGlobalState()
							.orElse(RiskState.defaultState()));

			// 타임스탬프 기록
			state.recordOrderTimestamp(LocalDateTime.now());

			// 저장
			riskStateRepository.save(state);
			log.debug("Recorded order timestamp for accountId={}", accountId);
		} catch (Exception e) {
			// 타임스탬프 기록 실패는 주문 처리에 영향을 주지 않도록
			log.warn("Failed to record order timestamp for accountId={}: {}", accountId, e.getMessage());
		}
	}
}
