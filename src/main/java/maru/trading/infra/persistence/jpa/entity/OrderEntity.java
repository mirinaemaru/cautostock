package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.order.OrderStatus;
import maru.trading.domain.order.OrderType;
import maru.trading.domain.order.Side;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {

	@Id
	@Column(name = "order_id", columnDefinition = "CHAR(26)")
	private String orderId;

	@Column(name = "account_id", columnDefinition = "CHAR(26)", nullable = false)
	private String accountId;

	@Column(name = "strategy_id", columnDefinition = "CHAR(26)")
	private String strategyId;

	@Column(name = "strategy_version_id", columnDefinition = "CHAR(26)")
	private String strategyVersionId;

	@Column(name = "signal_id", columnDefinition = "CHAR(26)")
	private String signalId;

	@Column(name = "symbol", length = 16, nullable = false)
	private String symbol;

	@Enumerated(EnumType.STRING)
	@Column(name = "side", length = 8, nullable = false)
	private Side side;

	@Enumerated(EnumType.STRING)
	@Column(name = "order_type", length = 8, nullable = false)
	private OrderType orderType;

	@Column(name = "ord_dvsn", length = 4, nullable = false)
	private String ordDvsn;

	@Column(name = "qty", precision = 18, scale = 6, nullable = false)
	private BigDecimal qty;

	@Column(name = "price", precision = 18, scale = 2)
	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 16, nullable = false)
	private OrderStatus status;

	@Column(name = "idempotency_key", length = 128, nullable = false, unique = true)
	private String idempotencyKey;

	@Column(name = "broker_order_no", length = 32)
	private String brokerOrderNo;

	@Column(name = "reject_code", length = 64)
	private String rejectCode;

	@Column(name = "reject_message", length = 255)
	private String rejectMessage;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public void updateStatus(OrderStatus newStatus) {
		this.status = newStatus;
	}

	public void updateBrokerOrderNo(String brokerOrderNo) {
		this.brokerOrderNo = brokerOrderNo;
	}

	public void updateRejection(String code, String message) {
		this.rejectCode = code;
		this.rejectMessage = message;
		this.status = OrderStatus.REJECTED;
	}
}
