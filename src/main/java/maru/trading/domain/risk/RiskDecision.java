package maru.trading.domain.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리스크 평가 결과
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskDecision {
	private boolean approved;
	private String reason;
	private String ruleViolated;

	public static RiskDecision approve() {
		return RiskDecision.builder()
				.approved(true)
				.reason("Risk check passed")
				.build();
	}

	public static RiskDecision reject(String reason, String ruleViolated) {
		return RiskDecision.builder()
				.approved(false)
				.reason(reason)
				.ruleViolated(ruleViolated)
				.build();
	}
}
