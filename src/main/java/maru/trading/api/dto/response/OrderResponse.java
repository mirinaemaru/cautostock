package maru.trading.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.order.OrderStatus;
import maru.trading.domain.order.OrderType;
import maru.trading.domain.order.Side;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {
	private String orderId;
	private String accountId;
	private String strategyId;
	private String strategyVersionId;
	private String symbol;
	private Side side;
	private OrderType orderType;
	private String ordDvsn;
	private BigDecimal qty;
	private BigDecimal price;
	private OrderStatus status;
	private String idempotencyKey;
	private String brokerOrderNo;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
