package maru.trading.application.usecase.trading;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.domain.order.Order;
import maru.trading.domain.risk.RiskDecision;
import maru.trading.domain.risk.RiskEngine;
import maru.trading.domain.risk.RiskRule;
import maru.trading.domain.risk.RiskState;
import maru.trading.infra.persistence.jpa.entity.RiskStateEntity;
import maru.trading.infra.persistence.jpa.repository.OrderJpaRepository;
import maru.trading.infra.persistence.jpa.repository.RiskStateJpaRepository;
import org.springframework.stereotype.Service;

/**
 * 리스크 평가 Use Case
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluateRiskUseCase {

	private final RiskEngine riskEngine = new RiskEngine();
	private final RiskStateJpaRepository riskStateRepository;
	private final OrderJpaRepository orderRepository;

	/**
	 * Pre-Trade 리스크 평가
	 */
	public RiskDecision evaluate(Order order) {
		log.info("Evaluate risk for order: orderId={}, accountId={}",
				order.getOrderId(), order.getAccountId());

		// 리스크 룰 조회 (여기서는 기본값 사용)
		RiskRule rule = RiskRule.defaultGlobalRule();

		// 리스크 상태 조회
		RiskState state = getRiskState(order.getAccountId());

		// 미체결 주문 수 조회 및 설정
		long openOrderCount = orderRepository.countOpenOrdersByAccountId(order.getAccountId());
		state = RiskState.builder()
				.riskStateId(state.getRiskStateId())
				.scope(state.getScope())
				.accountId(state.getAccountId())
				.killSwitchStatus(state.getKillSwitchStatus())
				.killSwitchReason(state.getKillSwitchReason())
				.dailyPnl(state.getDailyPnl())
				.exposure(state.getExposure())
				.consecutiveOrderFailures(state.getConsecutiveOrderFailures())
				.openOrderCount((int) openOrderCount)
				.build();

		// 리스크 평가
		RiskDecision decision = riskEngine.evaluatePreTrade(order, rule, state);

		if (!decision.isApproved()) {
			log.warn("Risk check rejected: reason={}, rule={}",
					decision.getReason(), decision.getRuleViolated());
		}

		return decision;
	}

	private RiskState getRiskState(String accountId) {
		RiskStateEntity entity = riskStateRepository
				.findByScopeAndAccountId("ACCOUNT", accountId)
				.orElseGet(() -> riskStateRepository
						.findByScope("GLOBAL")
						.orElse(null));

		if (entity == null) {
			return RiskState.defaultState();
		}

		return RiskState.builder()
				.riskStateId(entity.getRiskStateId())
				.scope(entity.getScope())
				.accountId(entity.getAccountId())
				.killSwitchStatus(entity.getKillSwitchStatus())
				.killSwitchReason(entity.getKillSwitchReason())
				.dailyPnl(entity.getDailyPnl())
				.exposure(entity.getExposure())
				.consecutiveOrderFailures(entity.getConsecutiveOrderFailures())
				.build();
	}
}
