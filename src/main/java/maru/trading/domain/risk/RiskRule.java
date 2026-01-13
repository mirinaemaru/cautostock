package maru.trading.domain.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 리스크 룰
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskRule {
	private String riskRuleId;
	private RiskRuleScope scope;
	private String accountId;
	private String symbol;
	private BigDecimal maxPositionValuePerSymbol;
	private Integer maxOpenOrders;
	private Integer maxOrdersPerMinute;
	private BigDecimal dailyLossLimit;
	private Integer consecutiveOrderFailuresLimit;

	public static RiskRule defaultGlobalRule() {
		return RiskRule.builder()
				.scope(RiskRuleScope.GLOBAL)
				.maxPositionValuePerSymbol(BigDecimal.valueOf(1000000))
				.maxOpenOrders(5)
				.maxOrdersPerMinute(10)
				.dailyLossLimit(BigDecimal.valueOf(50000))
				.consecutiveOrderFailuresLimit(5)
				.build();
	}
}
