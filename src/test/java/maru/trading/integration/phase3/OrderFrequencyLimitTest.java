package maru.trading.integration.phase3;

import maru.trading.TestFixtures;
import maru.trading.application.ports.broker.BrokerAck;
import maru.trading.application.ports.broker.BrokerClient;
import maru.trading.application.usecase.trading.PlaceOrderUseCase;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.Side;
import maru.trading.domain.risk.KillSwitchStatus;
import maru.trading.domain.risk.RiskLimitExceededException;
import maru.trading.domain.risk.RiskRule;
import maru.trading.domain.risk.RiskRuleScope;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.RiskRuleEntity;
import maru.trading.infra.persistence.jpa.entity.RiskStateEntity;
import maru.trading.infra.persistence.jpa.repository.RiskRuleJpaRepository;
import maru.trading.infra.persistence.jpa.repository.RiskStateJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Phase 3.4 - Order Frequency Limit Test.
 *
 * Tests order frequency limiting:
 * - maxOrdersPerMinute = 3
 * - 3 orders succeed
 * - 4th order rejected with RiskLimitExceededException
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Phase 3.4 - Order Frequency Limit Test")
class OrderFrequencyLimitTest {

    @Autowired
    private PlaceOrderUseCase placeOrderUseCase;

    @Autowired
    private RiskRuleJpaRepository riskRuleRepository;

    @Autowired
    private RiskStateJpaRepository riskStateRepository;

    @MockBean
    private BrokerClient brokerClient;

    private String accountId;
    private String symbol;

    @BeforeEach
    void setUp() {
        accountId = "ACC_FREQ_001";
        symbol = "005930";

        // Setup strict frequency limit: 3 orders per minute
        RiskRuleEntity ruleEntity = RiskRuleEntity.builder()
                .riskRuleId(UlidGenerator.generate())
                .scope(RiskRuleScope.GLOBAL)
                .dailyLossLimit(BigDecimal.valueOf(10000000))
                .maxOpenOrders(100)
                .maxOrdersPerMinute(3) // KEY: Only 3 orders per minute
                .maxPositionValuePerSymbol(BigDecimal.valueOf(100000000))
                .consecutiveOrderFailuresLimit(100)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        riskRuleRepository.save(ruleEntity);

        // Initialize RiskState for the account
        RiskStateEntity riskState = RiskStateEntity.builder()
                .riskStateId(UlidGenerator.generate())
                .scope("ACCOUNT")
                .accountId(accountId)
                .killSwitchStatus(KillSwitchStatus.OFF)
                .killSwitchReason(null)
                .dailyPnl(BigDecimal.ZERO)
                .exposure(BigDecimal.ZERO)
                .openOrderCount(0)
                .consecutiveOrderFailures(0)
                .updatedAt(LocalDateTime.now())
                .build();
        riskStateRepository.save(riskState);

        given(brokerClient.placeOrder(any()))
                .willReturn(BrokerAck.success("BROKER-FREQ-TEST"));
    }

    @Test
    @DisplayName("Should allow 3 orders within 1 minute")
    void testOrderFrequency_WithinLimit() {
        // Given - 3 orders
        Order order1 = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(), accountId, symbol, Side.BUY,
                BigDecimal.valueOf(1), BigDecimal.valueOf(70000), UlidGenerator.generate()
        );

        Order order2 = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(), accountId, symbol, Side.BUY,
                BigDecimal.valueOf(1), BigDecimal.valueOf(70100), UlidGenerator.generate()
        );

        Order order3 = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(), accountId, symbol, Side.BUY,
                BigDecimal.valueOf(1), BigDecimal.valueOf(70200), UlidGenerator.generate()
        );

        // When & Then - All 3 should succeed
        Order result1 = placeOrderUseCase.execute(order1);
        Order result2 = placeOrderUseCase.execute(order2);
        Order result3 = placeOrderUseCase.execute(order3);

        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result3).isNotNull();
    }

    @Test
    @DisplayName("Should reject 4th order when frequency limit exceeded")
    void testOrderFrequency_ExceedsLimit() {
        // Given - Place 3 orders first
        for (int i = 0; i < 3; i++) {
            Order order = TestFixtures.placeLimitOrder(
                    UlidGenerator.generate(), accountId, symbol, Side.BUY,
                    BigDecimal.valueOf(1), BigDecimal.valueOf(70000 + i * 100), UlidGenerator.generate()
            );
            placeOrderUseCase.execute(order);
        }

        // When - 4th order
        Order order4 = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(), accountId, symbol, Side.BUY,
                BigDecimal.valueOf(1), BigDecimal.valueOf(70300), UlidGenerator.generate()
        );

        // Then - Should reject
        assertThatThrownBy(() -> placeOrderUseCase.execute(order4))
                .isInstanceOf(RiskLimitExceededException.class)
                .hasMessageContaining("frequency");
    }
}
