package maru.trading.infra.persistence.adapter;

import maru.trading.application.ports.repo.RiskRuleRepository;
import maru.trading.domain.risk.RiskRule;
import maru.trading.domain.risk.RiskRuleScope;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.RiskRuleEntity;
import maru.trading.infra.persistence.jpa.repository.RiskRuleJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementation for RiskRuleRepository.
 */
@Component
public class RiskRuleRepositoryAdapter implements RiskRuleRepository {

    private final RiskRuleJpaRepository riskRuleJpaRepository;
    private final UlidGenerator ulidGenerator;

    public RiskRuleRepositoryAdapter(
            RiskRuleJpaRepository riskRuleJpaRepository,
            UlidGenerator ulidGenerator) {
        this.riskRuleJpaRepository = riskRuleJpaRepository;
        this.ulidGenerator = ulidGenerator;
    }

    @Override
    public RiskRule save(RiskRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Risk rule cannot be null");
        }

        // Generate ID if not present
        String riskRuleId = rule.getRiskRuleId();
        if (riskRuleId == null || riskRuleId.isBlank()) {
            riskRuleId = ulidGenerator.generateInstance();
        }

        RiskRuleEntity entity = RiskRuleEntity.builder()
                .riskRuleId(riskRuleId)
                .scope(rule.getScope())
                .accountId(rule.getAccountId())
                .symbol(rule.getSymbol())
                .maxPositionValuePerSymbol(rule.getMaxPositionValuePerSymbol())
                .maxOpenOrders(rule.getMaxOpenOrders())
                .maxOrdersPerMinute(rule.getMaxOrdersPerMinute())
                .dailyLossLimit(rule.getDailyLossLimit())
                .consecutiveOrderFailuresLimit(rule.getConsecutiveOrderFailuresLimit())
                .build();

        RiskRuleEntity savedEntity = riskRuleJpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<RiskRule> findById(String riskRuleId) {
        return riskRuleJpaRepository.findById(riskRuleId)
                .map(this::toDomain);
    }

    @Override
    public Optional<RiskRule> findGlobalRule() {
        return riskRuleJpaRepository.findByScope(RiskRuleScope.GLOBAL)
                .map(this::toDomain);
    }

    @Override
    public Optional<RiskRule> findAccountRule(String accountId) {
        return riskRuleJpaRepository.findByScopeAndAccountId(RiskRuleScope.PER_ACCOUNT, accountId)
                .map(this::toDomain);
    }

    @Override
    public Optional<RiskRule> findSymbolRule(String accountId, String symbol) {
        return riskRuleJpaRepository.findByScopeAndAccountIdAndSymbol(
                        RiskRuleScope.PER_SYMBOL, accountId, symbol)
                .map(this::toDomain);
    }

    @Override
    public Optional<RiskRule> findApplicableRule(String accountId, String symbol) {
        List<RiskRuleEntity> rules = riskRuleJpaRepository.findApplicableRules(accountId, symbol);

        // Return the first rule (most specific)
        return rules.isEmpty()
                ? Optional.empty()
                : Optional.of(toDomain(rules.get(0)));
    }

    @Override
    public List<RiskRule> findRulesForAccount(String accountId) {
        return riskRuleJpaRepository.findRulesForAccount(accountId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RiskRule> findAll() {
        return riskRuleJpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String riskRuleId) {
        riskRuleJpaRepository.deleteById(riskRuleId);
    }

    private RiskRule toDomain(RiskRuleEntity entity) {
        return RiskRule.builder()
                .riskRuleId(entity.getRiskRuleId())
                .scope(entity.getScope())
                .accountId(entity.getAccountId())
                .symbol(entity.getSymbol())
                .maxPositionValuePerSymbol(entity.getMaxPositionValuePerSymbol())
                .maxOpenOrders(entity.getMaxOpenOrders())
                .maxOrdersPerMinute(entity.getMaxOrdersPerMinute())
                .dailyLossLimit(entity.getDailyLossLimit())
                .consecutiveOrderFailuresLimit(entity.getConsecutiveOrderFailuresLimit())
                .build();
    }
}
