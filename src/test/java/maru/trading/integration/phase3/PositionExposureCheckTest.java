package maru.trading.integration.phase3;

import maru.trading.TestFixtures;
import maru.trading.application.ports.broker.BrokerAck;
import maru.trading.application.ports.broker.BrokerClient;
import maru.trading.application.usecase.trading.PlaceOrderUseCase;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.Side;
import maru.trading.domain.risk.RiskLimitExceededException;
import maru.trading.domain.risk.RiskRule;
import maru.trading.domain.risk.RiskRuleScope;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.PositionEntity;
import maru.trading.infra.persistence.jpa.entity.RiskRuleEntity;
import maru.trading.infra.persistence.jpa.repository.PositionJpaRepository;
import maru.trading.infra.persistence.jpa.repository.RiskRuleJpaRepository;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Phase 3.4 - Position Exposure Check Test.
 *
 * Tests position exposure limiting:
 * - Existing position: 10 shares @ 70,000 = 700,000
 * - maxPositionValuePerSymbol = 1,000,000
 * - New order: 10 shares @ 40,000 = 400,000
 * - Total: 1,100,000 → REJECT
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Phase 3.4 - Position Exposure Check Test")
class PositionExposureCheckTest {

    @Autowired
    private PlaceOrderUseCase placeOrderUseCase;

    @Autowired
    private PositionJpaRepository positionRepository;

    @Autowired
    private RiskRuleJpaRepository riskRuleRepository;

    @MockBean
    private BrokerClient brokerClient;

    private String accountId;
    private String symbol;

    @BeforeEach
    void setUp() {
        accountId = "ACC_POS_001";
        symbol = "005930";

        // Setup position exposure limit
        RiskRuleEntity ruleEntity = RiskRuleEntity.builder()
                .riskRuleId(UlidGenerator.generate())
                .scope(RiskRuleScope.GLOBAL)
                .dailyLossLimit(BigDecimal.valueOf(10000000))
                .maxOpenOrders(100)
                .maxOrdersPerMinute(1000)
                .maxPositionValuePerSymbol(BigDecimal.valueOf(1000000)) // KEY: 1M limit
                .consecutiveOrderFailuresLimit(100)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        riskRuleRepository.save(ruleEntity);

        given(brokerClient.placeOrder(any()))
                .willReturn(BrokerAck.success("BROKER-POS-TEST"));
    }

    @Test
    @DisplayName("Should reject order when position exposure exceeds limit")
    void testPositionExposure_ExceedsLimit() {
        // Given - Existing position: 10 shares @ 70,000 = 700,000
        PositionEntity position = PositionEntity.builder()
                .positionId(UlidGenerator.generate())
                .accountId(accountId)
                .symbol(symbol)
                .qty(BigDecimal.valueOf(10))
                .avgPrice(BigDecimal.valueOf(70000))
                .realizedPnl(BigDecimal.ZERO)
                .updatedAt(LocalDateTime.now())
                .build();
        positionRepository.save(position);

        // When - New order: 10 shares @ 40,000 = 400,000
        // Total would be: 700,000 + 400,000 = 1,100,000 > 1,000,000
        Order order = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(), accountId, symbol, Side.BUY,
                BigDecimal.valueOf(10), BigDecimal.valueOf(40000), UlidGenerator.generate()
        );

        // Then - Should reject due to position exposure
        assertThatThrownBy(() -> placeOrderUseCase.execute(order))
                .isInstanceOf(RiskLimitExceededException.class)
                .hasMessageContaining("exposure");
    }
}
