package maru.trading.domain.risk;

import lombok.extern.slf4j.Slf4j;
import maru.trading.domain.order.Order;

import java.math.BigDecimal;

/**
 * 리스크 엔진 (Pre-Trade 체크)
 */
@Slf4j
public class RiskEngine {

	/**
	 * Pre-Trade 리스크 체크
	 */
	public RiskDecision evaluatePreTrade(
			Order order,
			RiskRule rule,
			RiskState state
	) {
		log.debug("Evaluate pre-trade risk: orderId={}, symbol={}, side={}",
				order.getOrderId(), order.getSymbol(), order.getSide());

		// 1. Kill Switch 체크
		if (state.getKillSwitchStatus() == KillSwitchStatus.ON) {
			return RiskDecision.reject(
					"Kill switch is ON",
					"KILL_SWITCH"
			);
		}

		// 2. 일일 손실 한도 체크
		if (rule.getDailyLossLimit() != null &&
				state.getDailyPnl().compareTo(rule.getDailyLossLimit().negate()) < 0) {
			return RiskDecision.reject(
					"Daily loss limit exceeded: " + state.getDailyPnl(),
					"DAILY_LOSS_LIMIT"
			);
		}

		// 3. 최대 미체결 주문 수 체크
		if (rule.getMaxOpenOrders() != null &&
				state.getOpenOrderCount() >= rule.getMaxOpenOrders()) {
			return RiskDecision.reject(
					"Max open orders exceeded: " + state.getOpenOrderCount(),
					"MAX_OPEN_ORDERS"
			);
		}

		// 4. 종목당 최대 투자금액 체크
		BigDecimal orderValue = order.getQty().multiply(order.getPrice() != null ? order.getPrice() : BigDecimal.ZERO);
		if (rule.getMaxPositionValuePerSymbol() != null &&
				orderValue.compareTo(rule.getMaxPositionValuePerSymbol()) > 0) {
			return RiskDecision.reject(
					"Max position value per symbol exceeded: " + orderValue,
					"MAX_POSITION_VALUE"
			);
		}

		// 5. 연속 실패 체크
		if (rule.getConsecutiveOrderFailuresLimit() != null &&
				state.getConsecutiveOrderFailures() >= rule.getConsecutiveOrderFailuresLimit()) {
			return RiskDecision.reject(
					"Consecutive order failures limit exceeded: " + state.getConsecutiveOrderFailures(),
					"CONSECUTIVE_FAILURES"
			);
		}

		// 모든 체크 통과
		return RiskDecision.approve();
	}

	/**
	 * Kill Switch 트리거 여부 판단
	 */
	public boolean shouldTriggerKillSwitch(RiskRule rule, RiskState state) {
		// 일일 손실 한도 초과 시
		if (rule.getDailyLossLimit() != null &&
				state.getDailyPnl().compareTo(rule.getDailyLossLimit().negate()) < 0) {
			return true;
		}

		// 연속 실패 한도 초과 시
		if (rule.getConsecutiveOrderFailuresLimit() != null &&
				state.getConsecutiveOrderFailures() >= rule.getConsecutiveOrderFailuresLimit()) {
			return true;
		}

		return false;
	}
}
