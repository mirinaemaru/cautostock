package maru.trading.application.usecase.risk;

import maru.trading.application.ports.repo.RiskRuleRepository;
import maru.trading.domain.risk.RiskRule;
import maru.trading.domain.risk.RiskRuleScope;
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
 * Use case for updating risk rules.
 *
 * Allows creating and updating risk rules at different scopes:
 * - GLOBAL: System-wide defaults
 * - PER_ACCOUNT: Account-specific overrides
 * - PER_SYMBOL: Account+Symbol specific overrides
 */
@Service
public class UpdateRiskRuleUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateRiskRuleUseCase.class);

    private final RiskRuleRepository riskRuleRepository;
    private final OutboxService outboxService;

    public UpdateRiskRuleUseCase(
            RiskRuleRepository riskRuleRepository,
            OutboxService outboxService) {
        this.riskRuleRepository = riskRuleRepository;
        this.outboxService = outboxService;
    }

    /**
     * Create or update a global risk rule.
     */
    @Transactional
    public RiskRule updateGlobalRule(
            BigDecimal maxPositionValuePerSymbol,
            Integer maxOpenOrders,
            Integer maxOrdersPerMinute,
            BigDecimal dailyLossLimit,
            Integer consecutiveOrderFailuresLimit) {

        log.info("Updating global risk rule");

        RiskRule rule = RiskRule.builder()
                .scope(RiskRuleScope.GLOBAL)
                .maxPositionValuePerSymbol(maxPositionValuePerSymbol)
                .maxOpenOrders(maxOpenOrders)
                .maxOrdersPerMinute(maxOrdersPerMinute)
                .dailyLossLimit(dailyLossLimit)
                .consecutiveOrderFailuresLimit(consecutiveOrderFailuresLimit)
                .build();

        RiskRule savedRule = riskRuleRepository.save(rule);

        publishRiskRuleUpdatedEvent(savedRule);

        log.info("Global risk rule updated: ruleId={}", savedRule.getRiskRuleId());

        return savedRule;
    }

    /**
     * Create or update an account-specific risk rule.
     */
    @Transactional
    public RiskRule updateAccountRule(
            String accountId,
            BigDecimal maxPositionValuePerSymbol,
            Integer maxOpenOrders,
            Integer maxOrdersPerMinute,
            BigDecimal dailyLossLimit,
            Integer consecutiveOrderFailuresLimit) {

        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("Account ID cannot be null or blank");
        }

        log.info("Updating risk rule for account: {}", accountId);

        RiskRule rule = RiskRule.builder()
                .scope(RiskRuleScope.PER_ACCOUNT)
                .accountId(accountId)
                .maxPositionValuePerSymbol(maxPositionValuePerSymbol)
                .maxOpenOrders(maxOpenOrders)
                .maxOrdersPerMinute(maxOrdersPerMinute)
                .dailyLossLimit(dailyLossLimit)
                .consecutiveOrderFailuresLimit(consecutiveOrderFailuresLimit)
                .build();

        RiskRule savedRule = riskRuleRepository.save(rule);

        publishRiskRuleUpdatedEvent(savedRule);

        log.info("Account risk rule updated: ruleId={}, accountId={}",
                savedRule.getRiskRuleId(), accountId);

        return savedRule;
    }

    /**
     * Create or update a symbol-specific risk rule.
     */
    @Transactional
    public RiskRule updateSymbolRule(
            String accountId,
            String symbol,
            BigDecimal maxPositionValuePerSymbol,
            Integer maxOpenOrders,
            Integer maxOrdersPerMinute,
            BigDecimal dailyLossLimit,
            Integer consecutiveOrderFailuresLimit) {

        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("Account ID cannot be null or blank");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }

        log.info("Updating risk rule for account+symbol: {}, {}", accountId, symbol);

        RiskRule rule = RiskRule.builder()
                .scope(RiskRuleScope.PER_SYMBOL)
                .accountId(accountId)
                .symbol(symbol)
                .maxPositionValuePerSymbol(maxPositionValuePerSymbol)
                .maxOpenOrders(maxOpenOrders)
                .maxOrdersPerMinute(maxOrdersPerMinute)
                .dailyLossLimit(dailyLossLimit)
                .consecutiveOrderFailuresLimit(consecutiveOrderFailuresLimit)
                .build();

        RiskRule savedRule = riskRuleRepository.save(rule);

        publishRiskRuleUpdatedEvent(savedRule);

        log.info("Symbol risk rule updated: ruleId={}, accountId={}, symbol={}",
                savedRule.getRiskRuleId(), accountId, symbol);

        return savedRule;
    }

    /**
     * Delete a risk rule.
     */
    @Transactional
    public void deleteRule(String riskRuleId) {
        log.info("Deleting risk rule: {}", riskRuleId);

        riskRuleRepository.delete(riskRuleId);

        publishRiskRuleDeletedEvent(riskRuleId);

        log.info("Risk rule deleted: ruleId={}", riskRuleId);
    }

    private void publishRiskRuleUpdatedEvent(RiskRule rule) {
        OutboxEvent event = OutboxEvent.builder()
                .eventId(rule.getRiskRuleId())
                .eventType("RiskRuleUpdated")
                .occurredAt(LocalDateTime.now())
                .payload(Map.of(
                        "riskRuleId", rule.getRiskRuleId(),
                        "scope", rule.getScope().name(),
                        "accountId", rule.getAccountId() != null ? rule.getAccountId() : "",
                        "symbol", rule.getSymbol() != null ? rule.getSymbol() : ""
                ))
                .build();

        outboxService.save(event);
    }

    private void publishRiskRuleDeletedEvent(String riskRuleId) {
        OutboxEvent event = OutboxEvent.builder()
                .eventId(riskRuleId)
                .eventType("RiskRuleDeleted")
                .occurredAt(LocalDateTime.now())
                .payload(Map.of("riskRuleId", riskRuleId))
                .build();

        outboxService.save(event);
    }
}
