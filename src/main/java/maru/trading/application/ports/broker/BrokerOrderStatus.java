package maru.trading.application.ports.broker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.order.OrderStatus;

import java.math.BigDecimal;

/**
 * 브로커 주문 상태
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrokerOrderStatus {
	private String brokerOrderNo;
	private OrderStatus status;
	private BigDecimal filledQty;
	private BigDecimal avgPrice;
	private String message;
}
