package maru.trading.infra.persistence.jpa.repository;

import maru.trading.domain.risk.RiskRuleScope;
import maru.trading.infra.persistence.jpa.entity.RiskRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for risk rules.
 */
@Repository
public interface RiskRuleJpaRepository extends JpaRepository<RiskRuleEntity, String> {

    /**
     * Find global risk rule.
     */
    Optional<RiskRuleEntity> findByScope(RiskRuleScope scope);

    /**
     * Find risk rule for specific account.
     */
    Optional<RiskRuleEntity> findByScopeAndAccountId(RiskRuleScope scope, String accountId);

    /**
     * Find risk rule for specific account and symbol.
     */
    Optional<RiskRuleEntity> findByScopeAndAccountIdAndSymbol(
            RiskRuleScope scope, String accountId, String symbol);

    /**
     * Find all risk rules matching account and symbol.
     * Returns rules in priority order: PER_SYMBOL > PER_ACCOUNT > GLOBAL
     */
    @Query("SELECT r FROM RiskRuleEntity r WHERE " +
            "(r.scope = 'GLOBAL') OR " +
            "(r.scope = 'PER_ACCOUNT' AND r.accountId = :accountId) OR " +
            "(r.scope = 'PER_SYMBOL' AND r.accountId = :accountId AND r.symbol = :symbol) " +
            "ORDER BY CASE r.scope " +
            "WHEN 'PER_SYMBOL' THEN 1 " +
            "WHEN 'PER_ACCOUNT' THEN 2 " +
            "WHEN 'GLOBAL' THEN 3 END")
    List<RiskRuleEntity> findApplicableRules(
            @Param("accountId") String accountId,
            @Param("symbol") String symbol);

    /**
     * Find all rules for an account (including global).
     */
    @Query("SELECT r FROM RiskRuleEntity r WHERE " +
            "r.scope = 'GLOBAL' OR " +
            "(r.accountId = :accountId)")
    List<RiskRuleEntity> findRulesForAccount(@Param("accountId") String accountId);
}
