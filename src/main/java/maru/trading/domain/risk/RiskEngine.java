package maru.trading.domain.risk;

import lombok.extern.slf4j.Slf4j;
import maru.trading.domain.execution.Position;
import maru.trading.domain.market.TradingSession;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.Side;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * 리스크 엔진 (Pre-Trade 체크)
 */
@Slf4j
public class RiskEngine {

	/**
	 * Pre-Trade 리스크 체크 (기본)
	 */
	public RiskDecision evaluatePreTrade(
			Order order,
			RiskRule rule,
			RiskState state
	) {
		return evaluatePreTrade(order, rule, state, null);
	}

	/**
	 * Pre-Trade 리스크 체크 (포지션 포함)
	 */
	public RiskDecision evaluatePreTrade(
			Order order,
			RiskRule rule,
			RiskState state,
			Position existingPosition
	) {
		return evaluatePreTrade(order, rule, state, existingPosition, false, Set.of(), Set.of());
	}

	/**
	 * Pre-Trade 리스크 체크 (전체 파라미터)
	 */
	public RiskDecision evaluatePreTrade(
			Order order,
			RiskRule rule,
			RiskState state,
			Position existingPosition,
			boolean marketHoursEnabled,
			Set<TradingSession> allowedSessions,
			Set<LocalDate> publicHolidays
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
				state.getDailyPnl() != null &&
				state.getDailyPnl().compareTo(rule.getDailyLossLimit().negate()) < 0) {
			return RiskDecision.reject(
					"Daily loss limit exceeded: " + state.getDailyPnl(),
					"DAILY_LOSS_LIMIT"
			);
		}

		// 3. 최대 미체결 주문 수 체크
		if (rule.getMaxOpenOrders() != null &&
				state.getOpenOrderCount() != null &&
				state.getOpenOrderCount() >= rule.getMaxOpenOrders()) {
			return RiskDecision.reject(
					"Max open orders exceeded: " + state.getOpenOrderCount(),
					"MAX_OPEN_ORDERS"
			);
		}

		// 4. 주문 빈도 체크
		if (rule.getMaxOrdersPerMinute() != null &&
				state.wouldExceedOrderFrequencyLimit(rule.getMaxOrdersPerMinute())) {
			return RiskDecision.reject(
					"Order frequency limit exceeded",
					"ORDER_FREQUENCY_LIMIT"
			);
		}

		// 5. 포지션 노출 한도 체크 (기존 포지션 고려)
		RiskDecision positionExposureDecision = checkPositionExposure(order, rule, existingPosition);
		if (!positionExposureDecision.isApproved()) {
			return positionExposureDecision;
		}

		// 6. 연속 실패 체크
		if (rule.getConsecutiveOrderFailuresLimit() != null &&
				state.getConsecutiveOrderFailures() != null &&
				state.getConsecutiveOrderFailures() >= rule.getConsecutiveOrderFailuresLimit()) {
			return RiskDecision.reject(
					"Consecutive order failures limit exceeded: " + state.getConsecutiveOrderFailures(),
					"CONSECUTIVE_FAILURES"
			);
		}

		// 7. 거래시간 체크 (비활성화 또는 허용 세션이 없으면 fail-safe로 승인)
		// Market hours check is disabled or no allowed sessions - approve (fail-safe)
		// Actual market hours validation would be implemented here when enabled

		// 모든 체크 통과
		return RiskDecision.approve();
	}

	/**
	 * 포지션 노출 한도 체크
	 * - 기존 포지션이 없으면 주문 금액만 확인
	 * - 기존 포지션이 있으면 포지션 증가/감소를 고려
	 */
	private RiskDecision checkPositionExposure(Order order, RiskRule rule, Position existingPosition) {
		if (rule.getMaxPositionValuePerSymbol() == null) {
			return RiskDecision.approve();
		}

		BigDecimal orderPrice = order.getPrice() != null ? order.getPrice() : BigDecimal.ZERO;
		BigDecimal orderValue = order.getQty().multiply(orderPrice);

		// 기존 포지션이 없는 경우 - 주문 금액만 확인
		if (existingPosition == null || existingPosition.isFlat()) {
			if (orderValue.compareTo(rule.getMaxPositionValuePerSymbol()) > 0) {
				return RiskDecision.reject(
						"Position exposure limit exceeded: " + orderValue,
						"POSITION_EXPOSURE_LIMIT"
				);
			}
			return RiskDecision.approve();
		}

		// 포지션 축소 주문인지 확인 (long 포지션에 SELL, short 포지션에 BUY)
		boolean isReducingPosition =
			(existingPosition.isLong() && order.getSide() == Side.SELL) ||
			(existingPosition.isShort() && order.getSide() == Side.BUY);

		// 포지션 축소 주문은 무조건 승인 (노출 감소)
		if (isReducingPosition) {
			return RiskDecision.approve();
		}

		// 포지션 증가 주문 - 현재 포지션 + 주문 금액 확인
		BigDecimal currentPositionValue = existingPosition.getAvgPrice()
				.multiply(BigDecimal.valueOf(Math.abs(existingPosition.getQty())));
		BigDecimal totalExposure = currentPositionValue.add(orderValue);

		if (totalExposure.compareTo(rule.getMaxPositionValuePerSymbol()) > 0) {
			return RiskDecision.reject(
					"Position exposure limit exceeded: current=" + currentPositionValue + ", order=" + orderValue,
					"POSITION_EXPOSURE_LIMIT"
			);
		}

		return RiskDecision.approve();
	}

	/**
	 * Kill Switch 트리거 여부 판단
	 */
	public boolean shouldTriggerKillSwitch(RiskRule rule, RiskState state) {
		// 일일 손실 한도 초과 시
		if (rule.getDailyLossLimit() != null &&
				state.getDailyPnl() != null &&
				state.getDailyPnl().compareTo(rule.getDailyLossLimit().negate()) < 0) {
			return true;
		}

		// 연속 실패 한도 초과 시
		if (rule.getConsecutiveOrderFailuresLimit() != null &&
				state.getConsecutiveOrderFailures() != null &&
				state.getConsecutiveOrderFailures() >= rule.getConsecutiveOrderFailuresLimit()) {
			return true;
		}

		return false;
	}
}
