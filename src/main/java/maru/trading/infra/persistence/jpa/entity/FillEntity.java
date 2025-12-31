package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.order.Side;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fills")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FillEntity {

	@Id
	@Column(name = "fill_id", columnDefinition = "CHAR(26)")
	private String fillId;

	@Column(name = "order_id", columnDefinition = "CHAR(26)", nullable = false)
	private String orderId;

	@Column(name = "account_id", columnDefinition = "CHAR(26)", nullable = false)
	private String accountId;

	@Column(name = "broker_order_no", length = 32)
	private String brokerOrderNo;

	@Column(name = "symbol", length = 16, nullable = false)
	private String symbol;

	@Enumerated(EnumType.STRING)
	@Column(name = "side", length = 8, nullable = false)
	private Side side;

	@Column(name = "fill_price", precision = 18, scale = 2, nullable = false)
	private BigDecimal fillPrice;

	@Column(name = "fill_qty", precision = 18, scale = 6, nullable = false)
	private BigDecimal fillQty;

	@Column(name = "fee", precision = 18, scale = 2, nullable = false)
	private BigDecimal fee;

	@Column(name = "tax", precision = 18, scale = 2, nullable = false)
	private BigDecimal tax;

	@Column(name = "fill_ts", nullable = false)
	private LocalDateTime fillTs;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
