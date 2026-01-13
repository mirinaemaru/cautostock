package maru.trading.domain.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 리스크 상태
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskState {
	private String riskStateId;
	private String scope; // GLOBAL, ACCOUNT
	private String accountId;
	private KillSwitchStatus killSwitchStatus;
	private String killSwitchReason;
	private BigDecimal dailyPnl;
	private BigDecimal exposure;
	private Integer consecutiveOrderFailures;
	private Integer openOrderCount; // 현재 미체결 주문 수
	private OrderFrequencyTracker orderFrequencyTracker;

	public static RiskState defaultState() {
		return RiskState.builder()
				.killSwitchStatus(KillSwitchStatus.OFF)
				.dailyPnl(BigDecimal.ZERO)
				.exposure(BigDecimal.ZERO)
				.consecutiveOrderFailures(0)
				.openOrderCount(0)
				.orderFrequencyTracker(new OrderFrequencyTracker())
				.build();
	}

	public void toggleKillSwitch(KillSwitchStatus status, String reason) {
		this.killSwitchStatus = status;
		this.killSwitchReason = reason;
	}

	public void incrementFailureCount() {
		this.consecutiveOrderFailures++;
	}

	public void resetFailureCount() {
		this.consecutiveOrderFailures = 0;
	}

	public void updateDailyPnl(BigDecimal pnl) {
		this.dailyPnl = this.dailyPnl.add(pnl);
	}
}
