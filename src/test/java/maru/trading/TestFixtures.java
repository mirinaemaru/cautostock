package maru.trading;

import maru.trading.domain.execution.Fill;
import maru.trading.domain.execution.Position;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.OrderStatus;
import maru.trading.domain.order.OrderType;
import maru.trading.domain.order.Side;
import maru.trading.domain.risk.RiskRule;
import maru.trading.domain.risk.RiskRuleScope;
import maru.trading.domain.risk.RiskState;
import maru.trading.domain.risk.KillSwitchStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Test Fixtures - Common test data builders
 */
public class TestFixtures {

    /**
     * Create a market order for testing.
     */
    public static Order placeMarketOrder(
        String orderId,
        String accountId,
        String symbol,
        Side side,
        BigDecimal qty,
        String idempotencyKey
    ) {
        return Order.builder()
            .orderId(orderId)
            .accountId(accountId)
            .symbol(symbol)
            .side(side)
            .orderType(OrderType.MARKET)
            .ordDvsn("01") // Market order code for KIS
            .qty(qty)
            .price(null) // Market order has no price
            .status(OrderStatus.NEW)
            .idempotencyKey(idempotencyKey)
            .build();
    }

    /**
     * Create a market order with price (for testing).
     */
    public static Order placeMarketOrderWithPrice(
        String orderId,
        String accountId,
        String symbol,
        Side side,
        BigDecimal qty,
        BigDecimal price,
        String idempotencyKey
    ) {
        return Order.builder()
            .orderId(orderId)
            .accountId(accountId)
            .symbol(symbol)
            .side(side)
            .orderType(OrderType.MARKET)
            .ordDvsn("01")
            .qty(qty)
            .price(price)
            .status(OrderStatus.NEW)
            .idempotencyKey(idempotencyKey)
            .build();
    }

    /**
     * Create a limit order for testing.
     */
    public static Order placeLimitOrder(
        String orderId,
        String accountId,
        String symbol,
        Side side,
        BigDecimal qty,
        BigDecimal price,
        String idempotencyKey
    ) {
        return Order.builder()
            .orderId(orderId)
            .accountId(accountId)
            .symbol(symbol)
            .side(side)
            .orderType(OrderType.LIMIT)
            .ordDvsn("00") // Limit order code for KIS
            .qty(qty)
            .price(price)
            .status(OrderStatus.NEW)
            .idempotencyKey(idempotencyKey)
            .build();
    }

    /**
     * Create a Position with initial long position.
     */
    public static Position createLongPosition(
        String positionId,
        String accountId,
        String symbol,
        int qty,
        BigDecimal avgPrice
    ) {
        return new Position(positionId, accountId, symbol, qty, avgPrice, BigDecimal.ZERO);
    }

    /**
     * Create a Fill for testing.
     */
    public static Fill createFill(
        String fillId,
        String orderId,
        String accountId,
        String symbol,
        Side side,
        BigDecimal fillPrice,
        int fillQty,
        BigDecimal fee,
        BigDecimal tax
    ) {
        return new Fill(
            fillId,
            orderId,
            accountId,
            symbol,
            side,
            fillPrice,
            fillQty,
            fee,
            tax,
            LocalDateTime.now(),
            null
        );
    }

    /**
     * Create a default RiskRule for testing.
     */
    public static RiskRule createDefaultRiskRule(String riskRuleId) {
        return RiskRule.builder()
            .riskRuleId(riskRuleId)
            .scope(RiskRuleScope.GLOBAL)
            .dailyLossLimit(BigDecimal.valueOf(50000))
            .maxOpenOrders(5)
            .maxOrdersPerMinute(10)
            .maxPositionValuePerSymbol(BigDecimal.valueOf(1000000))
            .consecutiveOrderFailuresLimit(5)
            .build();
    }

    /**
     * Create a relaxed RiskRule for integration testing.
     * Uses very high limits to avoid blocking integration test scenarios.
     */
    public static RiskRule createRelaxedRiskRule(String riskRuleId) {
        return RiskRule.builder()
            .riskRuleId(riskRuleId)
            .scope(RiskRuleScope.GLOBAL)
            .dailyLossLimit(BigDecimal.valueOf(10000000))
            .maxOpenOrders(100)
            .maxOrdersPerMinute(1000)
            .maxPositionValuePerSymbol(BigDecimal.valueOf(100000000))
            .consecutiveOrderFailuresLimit(100)
            .build();
    }

    /**
     * Create a RiskState with Kill Switch ON.
     */
    public static RiskState createRiskStateWithKillSwitchOn(String reason) {
        RiskState state = RiskState.defaultState();
        state.toggleKillSwitch(KillSwitchStatus.ON, reason);
        return state;
    }

    /**
     * Create a RiskState with specific daily PnL.
     */
    public static RiskState createRiskStateWithDailyPnl(BigDecimal dailyPnl) {
        return RiskState.builder()
            .killSwitchStatus(KillSwitchStatus.OFF)
            .dailyPnl(dailyPnl)
            .exposure(BigDecimal.ZERO)
            .consecutiveOrderFailures(0)
            .openOrderCount(0)
            .build();
    }
}
