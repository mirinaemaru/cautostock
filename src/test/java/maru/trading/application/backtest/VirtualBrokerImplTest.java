package maru.trading.application.backtest;

import maru.trading.domain.execution.Fill;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.OrderStatus;
import maru.trading.domain.order.OrderType;
import maru.trading.domain.order.Side;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.HistoricalBarEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for VirtualBrokerImpl.
 */
class VirtualBrokerImplTest {

    private VirtualBrokerImpl virtualBroker;

    @BeforeEach
    void setUp() {
        virtualBroker = new VirtualBrokerImpl();
        virtualBroker.reset(BigDecimal.valueOf(10_000_000)); // 10M KRW
        virtualBroker.setCommission(BigDecimal.valueOf(0.001)); // 0.1%
        virtualBroker.setSlippage(BigDecimal.valueOf(0.0005)); // 0.05%
    }

    @Test
    @DisplayName("Market BUY order should fill at open price with slippage")
    void testMarketBuyOrder() {
        // Given: Market BUY order for 10 shares
        Order buyOrder = createMarketOrder(Side.BUY, BigDecimal.valueOf(10), "005930");
        virtualBroker.submitOrder(buyOrder);

        // When: Process bar with open=70,000
        HistoricalBarEntity bar = createBar("005930",
                BigDecimal.valueOf(70_000),
                BigDecimal.valueOf(71_000),
                BigDecimal.valueOf(69_000),
                BigDecimal.valueOf(70_500),
                1000L);
        List<Fill> fills = virtualBroker.processBar(bar);

        // Then: Should fill at open + slippage = 70,000 + 35 = 70,035
        assertThat(fills).hasSize(1);
        Fill fill = fills.get(0);
        assertThat(fill.getSide()).isEqualTo(Side.BUY);
        assertThat(fill.getFillQty()).isEqualTo(10);

        BigDecimal expectedPrice = BigDecimal.valueOf(70_000).multiply(BigDecimal.valueOf(1.0005));
        assertThat(fill.getFillPrice()).isEqualByComparingTo(expectedPrice);

        // Commission should be (70,035 * 10) * 0.001 = 700.35
        BigDecimal expectedFee = fill.getFillPrice().multiply(BigDecimal.valueOf(10)).multiply(BigDecimal.valueOf(0.001));
        assertThat(fill.getFee()).isEqualByComparingTo(expectedFee);

        // Cash should decrease by (price * qty + commission)
        BigDecimal fillValue = fill.getFillPrice().multiply(BigDecimal.valueOf(10));
        BigDecimal expectedCash = BigDecimal.valueOf(10_000_000).subtract(fillValue).subtract(fill.getFee());
        assertThat(virtualBroker.getCashBalance()).isEqualByComparingTo(expectedCash);
    }

    @Test
    @DisplayName("Market SELL order should fill at open price with slippage")
    void testMarketSellOrder() {
        // Given: Market SELL order for 10 shares
        Order sellOrder = createMarketOrder(Side.SELL, BigDecimal.valueOf(10), "005930");
        virtualBroker.submitOrder(sellOrder);

        // When: Process bar with open=70,000
        HistoricalBarEntity bar = createBar("005930",
                BigDecimal.valueOf(70_000),
                BigDecimal.valueOf(71_000),
                BigDecimal.valueOf(69_000),
                BigDecimal.valueOf(70_500),
                1000L);
        List<Fill> fills = virtualBroker.processBar(bar);

        // Then: Should fill at open - slippage = 70,000 - 35 = 69,965
        assertThat(fills).hasSize(1);
        Fill fill = fills.get(0);
        assertThat(fill.getSide()).isEqualTo(Side.SELL);
        assertThat(fill.getFillQty()).isEqualTo(10);

        BigDecimal expectedPrice = BigDecimal.valueOf(70_000).multiply(BigDecimal.valueOf(0.9995));
        assertThat(fill.getFillPrice()).isEqualByComparingTo(expectedPrice);

        // Cash should increase by (price * qty - commission)
        BigDecimal fillValue = fill.getFillPrice().multiply(BigDecimal.valueOf(10));
        BigDecimal expectedCash = BigDecimal.valueOf(10_000_000).add(fillValue).subtract(fill.getFee());
        assertThat(virtualBroker.getCashBalance()).isEqualByComparingTo(expectedCash);
    }

    @Test
    @DisplayName("Limit BUY order should fill when low <= limit price")
    void testLimitBuyOrderFill() {
        // Given: Limit BUY order at 70,000
        Order buyOrder = createLimitOrder(Side.BUY, BigDecimal.valueOf(10), "005930", BigDecimal.valueOf(70_000));
        virtualBroker.submitOrder(buyOrder);

        // When: Bar with low=69,500 (below limit)
        HistoricalBarEntity bar = createBar("005930",
                BigDecimal.valueOf(71_000),
                BigDecimal.valueOf(72_000),
                BigDecimal.valueOf(69_500),  // Low touches limit
                BigDecimal.valueOf(71_500),
                1000L);
        List<Fill> fills = virtualBroker.processBar(bar);

        // Then: Should fill at limit price + slippage
        assertThat(fills).hasSize(1);
        Fill fill = fills.get(0);

        BigDecimal expectedPrice = BigDecimal.valueOf(70_000).multiply(BigDecimal.valueOf(1.0005));
        assertThat(fill.getFillPrice()).isEqualByComparingTo(expectedPrice);
    }

    @Test
    @DisplayName("Limit BUY order should NOT fill when low > limit price")
    void testLimitBuyOrderNotFill() {
        // Given: Limit BUY order at 70,000
        Order buyOrder = createLimitOrder(Side.BUY, BigDecimal.valueOf(10), "005930", BigDecimal.valueOf(70_000));
        virtualBroker.submitOrder(buyOrder);

        // When: Bar with low=70,500 (above limit)
        HistoricalBarEntity bar = createBar("005930",
                BigDecimal.valueOf(71_000),
                BigDecimal.valueOf(72_000),
                BigDecimal.valueOf(70_500),  // Low doesn't touch limit
                BigDecimal.valueOf(71_500),
                1000L);
        List<Fill> fills = virtualBroker.processBar(bar);

        // Then: Should NOT fill
        assertThat(fills).isEmpty();
        assertThat(virtualBroker.getPendingOrders()).hasSize(1);
    }

    @Test
    @DisplayName("Limit SELL order should fill when high >= limit price")
    void testLimitSellOrderFill() {
        // Given: Limit SELL order at 70,000
        Order sellOrder = createLimitOrder(Side.SELL, BigDecimal.valueOf(10), "005930", BigDecimal.valueOf(70_000));
        virtualBroker.submitOrder(sellOrder);

        // When: Bar with high=70,500 (above limit)
        HistoricalBarEntity bar = createBar("005930",
                BigDecimal.valueOf(69_000),
                BigDecimal.valueOf(70_500),  // High touches limit
                BigDecimal.valueOf(68_500),
                BigDecimal.valueOf(69_500),
                1000L);
        List<Fill> fills = virtualBroker.processBar(bar);

        // Then: Should fill at limit price - slippage
        assertThat(fills).hasSize(1);
        Fill fill = fills.get(0);

        BigDecimal expectedPrice = BigDecimal.valueOf(70_000).multiply(BigDecimal.valueOf(0.9995));
        assertThat(fill.getFillPrice()).isEqualByComparingTo(expectedPrice);
    }

    @Test
    @DisplayName("Should handle multiple orders for different symbols")
    void testMultipleSymbols() {
        // Given: Orders for two different symbols
        Order buyOrder1 = createMarketOrder(Side.BUY, BigDecimal.valueOf(10), "005930");
        Order buyOrder2 = createMarketOrder(Side.BUY, BigDecimal.valueOf(5), "035720");
        virtualBroker.submitOrder(buyOrder1);
        virtualBroker.submitOrder(buyOrder2);

        // When: Process bar for 005930
        HistoricalBarEntity bar1 = createBar("005930",
                BigDecimal.valueOf(70_000),
                BigDecimal.valueOf(71_000),
                BigDecimal.valueOf(69_000),
                BigDecimal.valueOf(70_500),
                1000L);
        List<Fill> fills1 = virtualBroker.processBar(bar1);

        // Then: Only 005930 order should fill
        assertThat(fills1).hasSize(1);
        assertThat(fills1.get(0).getSymbol()).isEqualTo("005930");
        assertThat(virtualBroker.getPendingOrders()).hasSize(1);
        assertThat(virtualBroker.getPendingOrders().get(0).getSymbol()).isEqualTo("035720");

        // When: Process bar for 035720
        HistoricalBarEntity bar2 = createBar("035720",
                BigDecimal.valueOf(40_000),
                BigDecimal.valueOf(41_000),
                BigDecimal.valueOf(39_000),
                BigDecimal.valueOf(40_500),
                500L);
        List<Fill> fills2 = virtualBroker.processBar(bar2);

        // Then: 035720 order should fill
        assertThat(fills2).hasSize(1);
        assertThat(fills2.get(0).getSymbol()).isEqualTo("035720");
        assertThat(virtualBroker.getPendingOrders()).isEmpty();
    }

    @Test
    @DisplayName("Should track all fills executed")
    void testGetAllFills() {
        // Given: Two orders
        virtualBroker.submitOrder(createMarketOrder(Side.BUY, BigDecimal.valueOf(10), "005930"));
        virtualBroker.submitOrder(createMarketOrder(Side.SELL, BigDecimal.valueOf(5), "005930"));

        // When: Process bars
        HistoricalBarEntity bar1 = createBar("005930", BigDecimal.valueOf(70_000), BigDecimal.valueOf(71_000),
                BigDecimal.valueOf(69_000), BigDecimal.valueOf(70_500), 1000L);
        virtualBroker.processBar(bar1);

        HistoricalBarEntity bar2 = createBar("005930", BigDecimal.valueOf(70_500), BigDecimal.valueOf(71_500),
                BigDecimal.valueOf(69_500), BigDecimal.valueOf(71_000), 1000L);
        virtualBroker.processBar(bar2);

        // Then: Should have 2 fills
        assertThat(virtualBroker.getAllFills()).hasSize(2);
    }

    @Test
    @DisplayName("Should cancel pending order")
    void testCancelOrder() {
        // Given: Pending order
        Order order = createLimitOrder(Side.BUY, BigDecimal.valueOf(10), "005930", BigDecimal.valueOf(70_000));
        virtualBroker.submitOrder(order);
        assertThat(virtualBroker.getPendingOrders()).hasSize(1);

        // When: Cancel order
        boolean cancelled = virtualBroker.cancelOrder(order.getOrderId());

        // Then: Should be removed from pending orders
        assertThat(cancelled).isTrue();
        assertThat(virtualBroker.getPendingOrders()).isEmpty();
    }

    @Test
    @DisplayName("Reset should clear all state")
    void testReset() {
        // Given: Active state
        virtualBroker.submitOrder(createMarketOrder(Side.BUY, BigDecimal.valueOf(10), "005930"));
        HistoricalBarEntity bar = createBar("005930", BigDecimal.valueOf(70_000), BigDecimal.valueOf(71_000),
                BigDecimal.valueOf(69_000), BigDecimal.valueOf(70_500), 1000L);
        virtualBroker.processBar(bar);

        assertThat(virtualBroker.getPendingOrders()).isEmpty();
        assertThat(virtualBroker.getAllFills()).hasSize(1);

        // When: Reset
        virtualBroker.reset(BigDecimal.valueOf(5_000_000));

        // Then: Should clear all state
        assertThat(virtualBroker.getPendingOrders()).isEmpty();
        assertThat(virtualBroker.getAllFills()).isEmpty();
        assertThat(virtualBroker.getCashBalance()).isEqualByComparingTo(BigDecimal.valueOf(5_000_000));
    }

    // ========== Helper Methods ==========

    private Order createMarketOrder(Side side, BigDecimal qty, String symbol) {
        return Order.builder()
                .orderId(UlidGenerator.generate())
                .accountId("ACC_TEST")
                .symbol(symbol)
                .side(side)
                .orderType(OrderType.MARKET)
                .qty(qty)
                .price(BigDecimal.ZERO)
                .status(OrderStatus.SENT)
                .build();
    }

    private Order createLimitOrder(Side side, BigDecimal qty, String symbol, BigDecimal price) {
        return Order.builder()
                .orderId(UlidGenerator.generate())
                .accountId("ACC_TEST")
                .symbol(symbol)
                .side(side)
                .orderType(OrderType.LIMIT)
                .qty(qty)
                .price(price)
                .status(OrderStatus.SENT)
                .build();
    }

    private HistoricalBarEntity createBar(String symbol, BigDecimal open, BigDecimal high,
                                          BigDecimal low, BigDecimal close, Long volume) {
        return HistoricalBarEntity.builder()
                .barId(UlidGenerator.generate())
                .symbol(symbol)
                .timeframe("1d")
                .barTimestamp(LocalDateTime.now())
                .openPrice(open)
                .highPrice(high)
                .lowPrice(low)
                .closePrice(close)
                .volume(volume)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
