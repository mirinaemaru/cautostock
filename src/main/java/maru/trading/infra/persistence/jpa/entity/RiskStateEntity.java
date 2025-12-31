package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.risk.KillSwitchStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_state")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskStateEntity {

	@Id
	@Column(name = "risk_state_id", columnDefinition = "CHAR(26)")
	private String riskStateId;

	@Column(name = "scope", length = 16, nullable = false)
	private String scope; // GLOBAL, ACCOUNT

	@Column(name = "account_id", columnDefinition = "CHAR(26)")
	private String accountId;

	@Enumerated(EnumType.STRING)
	@Column(name = "kill_switch_status", length = 8, nullable = false)
	@Builder.Default
	private KillSwitchStatus killSwitchStatus = KillSwitchStatus.OFF;

	@Column(name = "kill_switch_reason", length = 64)
	private String killSwitchReason;

	@Column(name = "daily_pnl", precision = 18, scale = 2, nullable = false)
	@Builder.Default
	private BigDecimal dailyPnl = BigDecimal.ZERO;

	@Column(name = "exposure", precision = 18, scale = 2, nullable = false)
	@Builder.Default
	private BigDecimal exposure = BigDecimal.ZERO;

	@Column(name = "consecutive_order_failures", nullable = false)
	@Builder.Default
	private Integer consecutiveOrderFailures = 0;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public void toggleKillSwitch(KillSwitchStatus status, String reason) {
		this.killSwitchStatus = status;
		this.killSwitchReason = reason;
		this.updatedAt = LocalDateTime.now();
	}
}
