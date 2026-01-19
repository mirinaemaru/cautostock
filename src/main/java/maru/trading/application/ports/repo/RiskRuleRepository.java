package maru.trading.application.ports.repo;

import maru.trading.domain.risk.RiskRule;
import maru.trading.domain.risk.RiskRuleScope;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for risk rules.
 */
public interface RiskRuleRepository {

    /**
     * Save or update a risk rule.
     */
    RiskRule save(RiskRule rule);

    /**
     * Find risk rule by ID.
     */
    Optional<RiskRule> findById(String riskRuleId);

    /**
     * Find global risk rule.
     */
    Optional<RiskRule> findGlobalRule();

    /**
     * Find risk rule for specific account.
     */
    Optional<RiskRule> findAccountRule(String accountId);

    /**
     * Find risk rule for specific account and symbol.
     */
    Optional<RiskRule> findSymbolRule(String accountId, String symbol);

    /**
     * Find the most specific applicable rule for account and symbol.
     * Priority: PER_SYMBOL > PER_ACCOUNT > GLOBAL
     *
     * @return The most specific rule that applies
     */
    Optional<RiskRule> findApplicableRule(String accountId, String symbol);

    /**
     * Find all rules for an account (including global).
     */
    List<RiskRule> findRulesForAccount(String accountId);

    /**
     * Find all risk rules.
     */
    List<RiskRule> findAll();

    /**
     * Delete a risk rule.
     */
    void delete(String riskRuleId);
}
