package maru.trading.domain.order;

import maru.trading.domain.shared.DomainException;
import maru.trading.domain.shared.ErrorCode;

public class OrderNotFoundException extends DomainException {
	public OrderNotFoundException(String orderId) {
		super(ErrorCode.ORDER_001, "Order not found: " + orderId);
	}
}
