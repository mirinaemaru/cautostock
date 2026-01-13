package maru.trading.broker.kis;

import lombok.extern.slf4j.Slf4j;
import maru.trading.application.ports.broker.BrokerAck;
import maru.trading.application.ports.broker.BrokerClient;
import maru.trading.application.ports.broker.BrokerOrderStatus;
import maru.trading.application.ports.broker.BrokerResult;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * KIS Broker Client (Stub 구현)
 *
 * MVP에서는 실제 KIS API를 호출하지 않고 로그만 출력합니다.
 * 실제 구현 시 KIS REST API를 호출하도록 변경해야 합니다.
 */
@Slf4j
@Component
public class KisBrokerClient implements BrokerClient {

	@Override
	public BrokerAck placeOrder(Order order) {
		log.info("[KIS STUB] Place order: orderId={}, symbol={}, side={}, qty={}, price={}",
				order.getOrderId(),
				order.getSymbol(),
				order.getSide(),
				order.getQty(),
				order.getPrice());

		// Stub: 항상 성공으로 응답
		String brokerOrderNo = generateBrokerOrderNo();

		log.info("[KIS STUB] Order accepted: brokerOrderNo={}", brokerOrderNo);

		return BrokerAck.success(brokerOrderNo);

		// 실제 구현 예시:
		// KisOrderRequest request = mapper.toKisOrderRequest(order);
		// KisOrderResponse response = kisRestClient.placeOrder(request);
		// return mapper.toBrokerAck(response);
	}

	@Override
	public BrokerResult cancelOrder(String orderId) {
		log.info("[KIS STUB] Cancel order: orderId={}", orderId);

		// Stub: 항상 성공으로 응답
		log.info("[KIS STUB] Order cancelled successfully");

		return BrokerResult.success("Order cancelled");

		// 실제 구현 예시:
		// KisCancelRequest request = new KisCancelRequest(orderId);
		// KisCancelResponse response = kisRestClient.cancelOrder(request);
		// return mapper.toBrokerResult(response);
	}

	@Override
	public BrokerResult modifyOrder(String orderId, java.math.BigDecimal newQty, java.math.BigDecimal newPrice) {
		log.info("[KIS STUB] Modify order: orderId={}, newQty={}, newPrice={}",
				orderId, newQty, newPrice);

		// Stub: 항상 성공으로 응답
		log.info("[KIS STUB] Order modified successfully");

		return BrokerResult.success("Order modified");

		// 실제 구현 예시:
		// KisModifyRequest request = new KisModifyRequest(orderId, newQty, newPrice);
		// KisModifyResponse response = kisRestClient.modifyOrder(request);
		// return mapper.toBrokerResult(response);
	}

	@Override
	public BrokerOrderStatus getOrderStatus(String brokerOrderNo) {
		log.info("[KIS STUB] Get order status: brokerOrderNo={}", brokerOrderNo);

		// Stub: 항상 FILLED 상태로 응답
		BrokerOrderStatus status = BrokerOrderStatus.builder()
				.brokerOrderNo(brokerOrderNo)
				.status(OrderStatus.FILLED)
				.filledQty(java.math.BigDecimal.ONE)
				.avgPrice(java.math.BigDecimal.valueOf(72000))
				.message("Filled")
				.build();

		log.info("[KIS STUB] Order status: status={}", status.getStatus());

		return status;

		// 실제 구현 예시:
		// KisOrderStatusRequest request = new KisOrderStatusRequest(brokerOrderNo);
		// KisOrderStatusResponse response = kisRestClient.getOrderStatus(request);
		// return mapper.toBrokerOrderStatus(response);
	}

	/**
	 * 브로커 주문번호 생성 (Stub용)
	 */
	private String generateBrokerOrderNo() {
		return "KIS" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
	}
}
