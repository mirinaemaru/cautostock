package maru.trading.domain.risk;

/**
 * Risk rule scope.
 *
 * Defines the level at which a risk rule applies:
 * - GLOBAL: Applies to all accounts
 * - PER_ACCOUNT: Specific to one account
 * - PER_SYMBOL: Specific to account+symbol combination
 */
public enum RiskRuleScope {
    /**
     * Global rule applying to all trading activity.
     */
    GLOBAL,

    /**
     * Rule specific to an account.
     */
    PER_ACCOUNT,

    /**
     * Rule specific to an account-symbol combination.
     */
    PER_SYMBOL
}
