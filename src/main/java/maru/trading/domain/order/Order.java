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
	 * Check if the order can be cancelled.
	 * Returns true for SENT, ACCEPTED, PART_FILLED states.
	 */
	public boolean isCancellable() {
		if (status == null) {
			return false;
		}
		return status == OrderStatus.SENT ||
			   status == OrderStatus.ACCEPTED ||
			   status == OrderStatus.PART_FILLED;
	}

	/**
	 * Check if the order can be modified.
	 * Returns true for SENT, ACCEPTED, PART_FILLED states.
	 */
	public boolean isModifiable() {
		if (status == null) {
			return false;
		}
		return status == OrderStatus.SENT ||
			   status == OrderStatus.ACCEPTED ||
			   status == OrderStatus.PART_FILLED;
	}

	/**
	 * Validate that the order can be modified.
	 * Throws OrderModificationException if order cannot be modified.
	 */
	public void validateModifiable() {
		if (!isModifiable()) {
			throw new OrderModificationException(
					"Order cannot be modified in current state: " + status,
					orderId,
					status);
		}
	}

	/**
	 * Validate that the order can be cancelled.
	 * Throws OrderCancellationException if order cannot be cancelled.
	 */
	public void validateCancellable() {
		if (!isCancellable()) {
			throw new OrderCancellationException(
					"Order cannot be cancelled in current state: " + status,
					orderId,
					status);
		}
	}
}
