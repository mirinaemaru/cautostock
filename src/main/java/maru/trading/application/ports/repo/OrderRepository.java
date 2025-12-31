package maru.trading.application.ports.repo;

import maru.trading.domain.order.Order;
import maru.trading.domain.order.OrderStatus;

import java.util.List;
import java.util.Optional;

/**
 * 주문 저장소 포트
 */
public interface OrderRepository {

	Order save(Order order);

	Optional<Order> findById(String orderId);

	Optional<Order> findByIdempotencyKey(String idempotencyKey);

	List<Order> findOpenOrdersByAccountId(String accountId);

	long countOpenOrdersByAccountId(String accountId);

	void updateStatus(String orderId, OrderStatus newStatus);

	void updateBrokerOrderNo(String orderId, String brokerOrderNo);
}
