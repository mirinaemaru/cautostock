package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "positions")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionEntity {

	@Id
	@Column(name = "position_id", columnDefinition = "CHAR(26)")
	private String positionId;

	@Column(name = "account_id", columnDefinition = "CHAR(26)", nullable = false)
	private String accountId;

	@Column(name = "symbol", length = 16, nullable = false)
	private String symbol;

	@Column(name = "qty", precision = 18, scale = 6, nullable = false)
	private BigDecimal qty;

	@Column(name = "avg_price", precision = 18, scale = 2, nullable = false)
	private BigDecimal avgPrice;

	@Column(name = "realized_pnl", precision = 18, scale = 2, nullable = false)
	private BigDecimal realizedPnl;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	/**
	 * 매수 체결 반영
	 */
	public void applyBuyFill(BigDecimal fillQty, BigDecimal fillPrice) {
		BigDecimal totalCost = this.qty.multiply(this.avgPrice)
				.add(fillQty.multiply(fillPrice));
		BigDecimal totalQty = this.qty.add(fillQty);

		this.qty = totalQty;
		this.avgPrice = totalCost.divide(totalQty, 2, RoundingMode.HALF_UP);
		this.updatedAt = LocalDateTime.now();
	}

	/**
	 * 매도 체결 반영
	 */
	public void applySellFill(BigDecimal fillQty, BigDecimal fillPrice) {
		if (this.qty.compareTo(fillQty) < 0) {
			throw new IllegalStateException("Insufficient position quantity");
		}

		// 실현손익 계산
		BigDecimal pnl = fillQty.multiply(fillPrice.subtract(this.avgPrice));
		this.realizedPnl = this.realizedPnl.add(pnl);

		// 수량 감소
		this.qty = this.qty.subtract(fillQty);

		// 전량 청산 시 평단 리셋
		if (this.qty.compareTo(BigDecimal.ZERO) == 0) {
			this.avgPrice = BigDecimal.ZERO;
		}

		this.updatedAt = LocalDateTime.now();
	}
}
