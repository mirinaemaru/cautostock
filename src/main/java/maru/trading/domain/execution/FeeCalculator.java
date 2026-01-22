package maru.trading.domain.execution;

import maru.trading.domain.order.Side;

import java.math.BigDecimal;

/**
 * Fee and tax calculator interface.
 *
 * Responsible for calculating broker fees and taxes based on market rules.
 */
public interface FeeCalculator {

    /**
     * Calculate broker commission fee.
     *
     * @param symbol Stock symbol
     * @param price Fill price
     * @param quantity Fill quantity
     * @param side Buy or Sell
     * @return Commission fee amount
     */
    BigDecimal calculateFee(String symbol, BigDecimal price, int quantity, Side side);

    /**
     * Calculate securities transaction tax.
     *
     * @param symbol Stock symbol
     * @param price Fill price
     * @param quantity Fill quantity
     * @param side Buy or Sell
     * @return Tax amount
     */
    BigDecimal calculateTax(String symbol, BigDecimal price, int quantity, Side side);

    /**
     * Calculate total costs (fee + tax).
     *
     * @param symbol Stock symbol
     * @param price Fill price
     * @param quantity Fill quantity
     * @param side Buy or Sell
     * @return Total cost (fee + tax)
     */
    default BigDecimal calculateTotalCost(String symbol, BigDecimal price, int quantity, Side side) {
        return calculateFee(symbol, price, quantity, side)
                .add(calculateTax(symbol, price, quantity, side));
    }
}
