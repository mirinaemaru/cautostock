package maru.trading.application.ports.broker;

import maru.trading.domain.order.Order;

/**
 * 브로커 주문 실행 포트
 */
public interface BrokerClient {

	/**
	 * 주문 전송
	 */
	BrokerAck placeOrder(Order order);

	/**
	 * 주문 취소
	 */
	BrokerResult cancelOrder(String orderId);

	/**
	 * 주문 상태 조회
	 */
	BrokerOrderStatus getOrderStatus(String brokerOrderNo);
}
