package maru.trading.application.usecase.trading;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.application.ports.repo.PositionRepository;
import maru.trading.application.ports.repo.RiskRuleRepository;
import maru.trading.application.ports.repo.RiskStateRepository;
import maru.trading.domain.execution.Position;
import maru.trading.domain.order.Order;
import maru.trading.domain.risk.RiskDecision;
import maru.trading.domain.risk.RiskEngine;
import maru.trading.domain.risk.RiskRule;
import maru.trading.domain.risk.RiskState;
import maru.trading.infra.persistence.jpa.repository.OrderJpaRepository;
import org.springframework.stereotype.Service;

/**
 * 리스크 평가 Use Case
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluateRiskUseCase {

	private final RiskEngine riskEngine = new RiskEngine();
	private final RiskStateRepository riskStateRepository;
	private final RiskRuleRepository riskRuleRepository;
	private final PositionRepository positionRepository;
	private final OrderJpaRepository orderRepository;

	/**
	 * Pre-Trade 리스크 평가
	 */
	public RiskDecision evaluate(Order order) {
		log.info("Evaluate risk for order: orderId={}, accountId={}",
				order.getOrderId(), order.getAccountId());

		// 리스크 룰 조회 (DB에서 조회 후 없으면 기본값 사용)
		RiskRule rule = riskRuleRepository.findGlobalRule()
				.orElse(RiskRule.defaultGlobalRule());

		// 리스크 상태 조회 (orderFrequencyTracker 포함)
		RiskState state = getRiskState(order.getAccountId());

		// 미체결 주문 수 조회 및 설정 (orderFrequencyTracker 보존)
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
				.orderFrequencyTracker(state.getOrderFrequencyTracker())
				.build();

		// 기존 포지션 조회 (포지션 노출 한도 체크용)
		Position existingPosition = positionRepository
				.findByAccountAndSymbol(order.getAccountId(), order.getSymbol())
				.orElse(null);

		// 리스크 평가 (기존 포지션 포함)
		RiskDecision decision = riskEngine.evaluatePreTrade(order, rule, state, existingPosition);

		if (!decision.isApproved()) {
			log.warn("Risk check rejected: reason={}, rule={}",
					decision.getReason(), decision.getRuleViolated());
		}

		return decision;
	}

	private RiskState getRiskState(String accountId) {
		// RiskStateRepository (어댑터) 사용 - orderFrequencyTracker 포함하여 변환
		return riskStateRepository.findByAccountId(accountId)
				.orElseGet(() -> riskStateRepository.findGlobalState()
						.orElse(RiskState.defaultState()));
	}
}
