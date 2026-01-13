package maru.trading.application.usecase.risk;

import maru.trading.application.ports.repo.RiskRuleRepository;
import maru.trading.application.ports.repo.RiskStateRepository;
import maru.trading.domain.risk.KillSwitchStatus;
import maru.trading.domain.risk.RiskEngine;
import maru.trading.domain.risk.RiskRule;
import maru.trading.domain.risk.RiskState;
import maru.trading.infra.messaging.outbox.OutboxEvent;
import maru.trading.infra.messaging.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Use case for updating risk state with P&L changes.
 *
 * Called by ApplyFillUseCase after each fill to:
 * 1. Update daily PnL in risk state
 * 2. Check if kill switch should be triggered
 * 3. Trigger kill switch if necessary
 */
@Service
public class UpdateRiskStateWithPnlUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateRiskStateWithPnlUseCase.class);

    private final RiskStateRepository riskStateRepository;
    private final RiskRuleRepository riskRuleRepository;
    private final OutboxService outboxService;
    private final RiskEngine riskEngine;

    public UpdateRiskStateWithPnlUseCase(
            RiskStateRepository riskStateRepository,
            RiskRuleRepository riskRuleRepository,
            OutboxService outboxService) {
        this.riskStateRepository = riskStateRepository;
        this.riskRuleRepository = riskRuleRepository;
        this.outboxService = outboxService;
        this.riskEngine = new RiskEngine();
    }

    /**
     * Execute the use case.
     *
     * @param accountId Account ID
     * @param realizedPnlDelta Change in realized PnL (can be negative)
     * @return Updated risk state
     */
    @Transactional
    public RiskState execute(String accountId, BigDecimal realizedPnlDelta) {
        log.debug("Updating risk state with PnL: accountId={}, pnlDelta={}",
                accountId, realizedPnlDelta);

        // Load risk state
        RiskState state = riskStateRepository.findByAccountId(accountId)
                .orElseGet(() -> {
                    log.info("Creating new risk state for account: {}", accountId);
                    RiskState newState = RiskState.defaultState();
                    newState = RiskState.builder()
                            .scope("ACCOUNT")
                            .accountId(accountId)
                            .killSwitchStatus(KillSwitchStatus.OFF)
                            .dailyPnl(BigDecimal.ZERO)
                            .exposure(BigDecimal.ZERO)
                            .consecutiveOrderFailures(0)
                            .openOrderCount(0)
                            .orderFrequencyTracker(new maru.trading.domain.risk.OrderFrequencyTracker())
                            .build();
                    return riskStateRepository.save(newState);
                });

        // Update daily PnL
        state.updateDailyPnl(realizedPnlDelta);

        BigDecimal newDailyPnl = state.getDailyPnl();
        log.info("Updated daily PnL: accountId={}, oldPnl={}, delta={}, newPnl={}",
                accountId, newDailyPnl.subtract(realizedPnlDelta), realizedPnlDelta, newDailyPnl);

        // Load risk rule
        RiskRule rule = riskRuleRepository.findApplicableRule(accountId, null)
                .orElse(RiskRule.defaultGlobalRule());

        // Check if kill switch should be triggered
        boolean shouldTriggerKillSwitch = riskEngine.shouldTriggerKillSwitch(rule, state);

        if (shouldTriggerKillSwitch && state.getKillSwitchStatus() == KillSwitchStatus.OFF) {
            // Trigger kill switch
            String reason = buildKillSwitchReason(rule, state);
            state.toggleKillSwitch(KillSwitchStatus.ON, reason);

            log.warn("KILL SWITCH TRIGGERED: accountId={}, reason={}", accountId, reason);

            // Publish kill switch event
            publishKillSwitchTriggeredEvent(accountId, reason);
        }

        // Save updated state
        RiskState updatedState = riskStateRepository.save(state);

        log.debug("Risk state updated: accountId={}, dailyPnl={}, killSwitch={}",
                accountId, updatedState.getDailyPnl(), updatedState.getKillSwitchStatus());

        return updatedState;
    }

    private String buildKillSwitchReason(RiskRule rule, RiskState state) {
        StringBuilder reason = new StringBuilder();

        // Check daily loss limit
        if (rule.getDailyLossLimit() != null &&
                state.getDailyPnl().compareTo(rule.getDailyLossLimit().negate()) < 0) {
            reason.append("Daily loss limit exceeded: ")
                    .append(state.getDailyPnl())
                    .append(" < -")
                    .append(rule.getDailyLossLimit());
        }

        // Check consecutive failures
        if (rule.getConsecutiveOrderFailuresLimit() != null &&
                state.getConsecutiveOrderFailures() >= rule.getConsecutiveOrderFailuresLimit()) {
            if (reason.length() > 0) {
                reason.append("; ");
            }
            reason.append("Consecutive failures: ")
                    .append(state.getConsecutiveOrderFailures())
                    .append(" >= ")
                    .append(rule.getConsecutiveOrderFailuresLimit());
        }

        return reason.toString();
    }

    private void publishKillSwitchTriggeredEvent(String accountId, String reason) {
        OutboxEvent event = OutboxEvent.builder()
                .eventId(accountId + "_killswitch_" + System.currentTimeMillis())
                .eventType("KillSwitchTriggered")
                .occurredAt(LocalDateTime.now())
                .payload(Map.of(
                        "accountId", accountId,
                        "reason", reason
                ))
                .build();

        outboxService.save(event);
    }
}
