package maru.trading.domain.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 주문 도메인 모델
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
	private String orderId;
	private String accountId;
	private String strategyId;
	private String strategyVersionId;
	private String signalId;
	private String symbol;
	private Side side;
	private OrderType orderType;
	private String ordDvsn;
	private BigDecimal qty;
	private BigDecimal price;
	private OrderStatus status;
	private String idempotencyKey;
	private String brokerOrderNo;

	/**
	 * Validate that the order can be modified.
	 * Throws OrderModificationException if order cannot be modified.
	 */
	public void validateModifiable() {
		if (status != OrderStatus.NEW && status != OrderStatus.SENT && status != OrderStatus.ACCEPTED) {
			throw new OrderModificationException(
					"Order cannot be modified in status: " + status,
					orderId,
					status);
		}
	}

	/**
	 * Validate that the order can be cancelled.
	 * Throws OrderCancellationException if order cannot be cancelled.
	 */
	public void validateCancellable() {
		if (status != OrderStatus.NEW && status != OrderStatus.SENT && status != OrderStatus.ACCEPTED) {
			throw new OrderCancellationException(
					"Order cannot be cancelled in status: " + status,
					orderId,
					status);
		}
	}
}
