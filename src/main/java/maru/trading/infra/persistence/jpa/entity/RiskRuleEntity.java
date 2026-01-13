package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.risk.RiskRuleScope;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for risk rules.
 */
@Entity
@Table(name = "risk_rules", indexes = {
        @Index(name = "idx_risk_rules_scope_account_symbol",
                columnList = "scope, account_id, symbol")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskRuleEntity {

    @Id
    @Column(name = "risk_rule_id", columnDefinition = "CHAR(26)")
    private String riskRuleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", length = 16, nullable = false)
    private RiskRuleScope scope;

    @Column(name = "account_id", columnDefinition = "CHAR(26)")
    private String accountId;

    @Column(name = "symbol", length = 16)
    private String symbol;

    @Column(name = "max_position_value_per_symbol", precision = 18, scale = 2)
    private BigDecimal maxPositionValuePerSymbol;

    @Column(name = "max_open_orders")
    private Integer maxOpenOrders;

    @Column(name = "max_orders_per_minute")
    private Integer maxOrdersPerMinute;

    @Column(name = "daily_loss_limit", precision = 18, scale = 2)
    private BigDecimal dailyLossLimit;

    @Column(name = "consecutive_order_failures_limit")
    private Integer consecutiveOrderFailuresLimit;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
