package maru.trading.domain.risk;

import maru.trading.TestFixtures;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RiskEngine 도메인 테스트 (간소화 버전)
 *
 * 테스트 범위:
 * 1. Kill Switch 체크
 * 2. Daily PnL Limit 체크
 * 3. Max Open Orders 체크
 * 4. Max Position Value 체크
 * 5. Consecutive Failures 체크
 * 6. Kill Switch 자동 트리거 조건
 */
@DisplayName("RiskEngine 도메인 테스트")
class RiskEngineTest {

    private RiskEngine riskEngine;
    private Order testOrder;
    private RiskRule testRule;
    private RiskState testState;

    @BeforeEach
    void setUp() {
        riskEngine = new RiskEngine();
        testOrder = TestFixtures.placeMarketOrderWithPrice(
            "ORDER_001", "ACC_001", "005930",
            Side.BUY, BigDecimal.valueOf(10), BigDecimal.valueOf(70000), "KEY_001"
        );
        testRule = TestFixtures.createDefaultRiskRule("RULE_001");
        testState = RiskState.defaultState();
    }

    // ==================== 1. Kill Switch Tests ====================

    @Test
    @DisplayName("Kill Switch ON 시 주문 거부")
    void testKillSwitch_On_Reject() {
        // Given
        testState.toggleKillSwitch(KillSwitchStatus.ON, "Manual activation");

        // When
        RiskDecision decision = riskEngine.evaluatePreTrade(testOrder, testRule, testState);

        // Then
        assertThat(decision.isApproved()).isFalse();
        assertThat(decision.getRuleViolated()).isEqualTo("KILL_SWITCH");
    }

    @Test
    @DisplayName("Kill Switch OFF 시 주문 승인")
    void testKillSwitch_Off_Approve() {
        // Given - default state has Kill Switch OFF

        // When
        RiskDecision decision = riskEngine.evaluatePreTrade(testOrder, testRule, testState);

        // Then
        assertThat(decision.isApproved()).isTrue();
    }

    // ==================== 2. Daily PnL Limit Tests ====================

    @Test
    @DisplayName("일일 손실 한도 초과 시 주문 거부")
    void testDailyPnlLimit_Exceeded_Reject() {
        // Given
        testState.updateDailyPnl(BigDecimal.valueOf(-60000)); // Loss of 60,000 > limit of 50,000

        // When
        RiskDecision decision = riskEngine.evaluatePreTrade(testOrder, testRule, testState);

        // Then
        assertThat(decision.isApproved()).isFalse();
        assertThat(decision.getRuleViolated()).isEqualTo("DAILY_LOSS_LIMIT");
    }

    @Test
    @DisplayName("일일 손실 한도 내 주문 승인")
    void testDailyPnlLimit_WithinLimit_Approve() {
        // Given
        testState.updateDailyPnl(BigDecimal.valueOf(-30000)); // Loss of 30,000 < limit of 50,000

        // When
        RiskDecision decision = riskEngine.evaluatePreTrade(testOrder, testRule, testState);

        // Then
        assertThat(decision.isApproved()).isTrue();
    }

    // ==================== 3. Max Open Orders Tests ====================

    @Test
    @DisplayName("최대 미체결 주문 수 초과 시 주문 거부")
    void testMaxOpenOrders_Exceeded_Reject() {
        // Given
        RiskState stateWithManyOrders = RiskState.builder()
            .killSwitchStatus(KillSwitchStatus.OFF)
            .dailyPnl(BigDecimal.ZERO)
            .openOrderCount(5) // At limit
            .consecutiveOrderFailures(0)
            .build();

        // When
        RiskDecision decision = riskEngine.evaluatePreTrade(testOrder, testRule, stateWithManyOrders);

        // Then
        assertThat(decision.isApproved()).isFalse();
        assertThat(decision.getRuleViolated()).isEqualTo("MAX_OPEN_ORDERS");
    }

    @Test
    @DisplayName("최대 미체결 주문 수 내 주문 승인")
    void testMaxOpenOrders_WithinLimit_Approve() {
        // Given
        RiskState stateWithFewOrders = RiskState.builder()
            .killSwitchStatus(KillSwitchStatus.OFF)
            .dailyPnl(BigDecimal.ZERO)
            .openOrderCount(3) // Below limit of 5
            .consecutiveOrderFailures(0)
            .build();

        // When
        RiskDecision decision = riskEngine.evaluatePreTrade(testOrder, testRule, stateWithFewOrders);

        // Then
        assertThat(decision.isApproved()).isTrue();
    }

    // ==================== 4. Max Position Value Tests ====================

    @Test
    @DisplayName("종목당 최대 투자금액 초과 시 주문 거부")
    void testMaxPositionValue_Exceeded_Reject() {
        // Given
        RiskRule lowValueRule = RiskRule.builder()
            .riskRuleId("RULE_LOW_VAL")
            .scope(RiskRuleScope.GLOBAL)
            .maxPositionValuePerSymbol(BigDecimal.valueOf(500000)) // Low limit
            .maxOpenOrders(10)
            .maxOrdersPerMinute(10)
            .dailyLossLimit(BigDecimal.valueOf(50000))
            .consecutiveOrderFailuresLimit(5)
            .build();

        // Order value: 10 * 70,000 = 700,000 > 500,000
        Order largeOrder = TestFixtures.placeMarketOrderWithPrice(
            "ORDER_002", "ACC_001", "005930",
            Side.BUY, BigDecimal.valueOf(10), BigDecimal.valueOf(70000), "KEY_002"
        );

        // When
        RiskDecision decision = riskEngine.evaluatePreTrade(largeOrder, lowValueRule, testState);

        // Then
        assertThat(decision.isApproved()).isFalse();
        assertThat(decision.getRuleViolated()).isEqualTo("MAX_POSITION_VALUE");
    }

    @Test
    @DisplayName("종목당 최대 투자금액 내 주문 승인")
    void testMaxPositionValue_WithinLimit_Approve() {
        // Given - Order value: 10 * 70,000 = 700,000 < 1,000,000 (default rule)

        // When
        RiskDecision decision = riskEngine.evaluatePreTrade(testOrder, testRule, testState);

        // Then
        assertThat(decision.isApproved()).isTrue();
    }

    // ==================== 5. Consecutive Failures Tests ====================

    @Test
    @DisplayName("연속 실패 한도 초과 시 주문 거부")
    void testConsecutiveFailures_Exceeded_Reject() {
        // Given
        RiskState stateWithFailures = RiskState.builder()
            .killSwitchStatus(KillSwitchStatus.OFF)
            .dailyPnl(BigDecimal.ZERO)
            .openOrderCount(0)
            .consecutiveOrderFailures(5) // At limit
            .build();

        // When
        RiskDecision decision = riskEngine.evaluatePreTrade(testOrder, testRule, stateWithFailures);

        // Then
        assertThat(decision.isApproved()).isFalse();
        assertThat(decision.getRuleViolated()).isEqualTo("CONSECUTIVE_FAILURES");
    }

    @Test
    @DisplayName("연속 실패 한도 내 주문 승인")
    void testConsecutiveFailures_WithinLimit_Approve() {
        // Given
        RiskState stateWithFewFailures = RiskState.builder()
            .killSwitchStatus(KillSwitchStatus.OFF)
            .dailyPnl(BigDecimal.ZERO)
            .openOrderCount(0)
            .consecutiveOrderFailures(2) // Below limit
            .build();

        // When
        RiskDecision decision = riskEngine.evaluatePreTrade(testOrder, testRule, stateWithFewFailures);

        // Then
        assertThat(decision.isApproved()).isTrue();
    }

    // ==================== 6. Kill Switch Auto-Trigger Tests ====================

    @Test
    @DisplayName("일일 손실 한도 초과 시 Kill Switch 트리거 필요")
    void testShouldTriggerKillSwitch_DailyLossExceeded() {
        // Given
        testState.updateDailyPnl(BigDecimal.valueOf(-60000)); // Exceeds 50,000 limit

        // When
        boolean shouldTrigger = riskEngine.shouldTriggerKillSwitch(testRule, testState);

        // Then
        assertThat(shouldTrigger).isTrue();
    }

    @Test
    @DisplayName("연속 실패 한도 초과 시 Kill Switch 트리거 필요")
    void testShouldTriggerKillSwitch_ConsecutiveFailuresExceeded() {
        // Given
        RiskState stateWithManyFailures = RiskState.builder()
            .killSwitchStatus(KillSwitchStatus.OFF)
            .dailyPnl(BigDecimal.ZERO)
            .openOrderCount(0)
            .consecutiveOrderFailures(5)
            .build();

        // When
        boolean shouldTrigger = riskEngine.shouldTriggerKillSwitch(testRule, stateWithManyFailures);

        // Then
        assertThat(shouldTrigger).isTrue();
    }

    @Test
    @DisplayName("정상 상태에서는 Kill Switch 트리거 불필요")
    void testShouldTriggerKillSwitch_Normal_NoTrigger() {
        // Given - Normal state

        // When
        boolean shouldTrigger = riskEngine.shouldTriggerKillSwitch(testRule, testState);

        // Then
        assertThat(shouldTrigger).isFalse();
    }

    // ==================== 7. Integration Tests ====================

    @Test
    @DisplayName("모든 체크 통과 시 주문 승인")
    void testAllChecks_Pass_Approve() {
        // Given - All checks within limits
        RiskState normalState = RiskState.builder()
            .killSwitchStatus(KillSwitchStatus.OFF)
            .dailyPnl(BigDecimal.valueOf(-10000)) // Small loss
            .openOrderCount(1) // Few orders
            .consecutiveOrderFailures(0)
            .build();

        Order smallOrder = TestFixtures.placeMarketOrderWithPrice(
            "ORDER_SMALL", "ACC_001", "005930",
            Side.BUY, BigDecimal.valueOf(5), BigDecimal.valueOf(70000), "KEY_SMALL"
        ); // 5 * 70,000 = 350,000

        // When
        RiskDecision decision = riskEngine.evaluatePreTrade(smallOrder, testRule, normalState);

        // Then
        assertThat(decision.isApproved()).isTrue();
    }
}
