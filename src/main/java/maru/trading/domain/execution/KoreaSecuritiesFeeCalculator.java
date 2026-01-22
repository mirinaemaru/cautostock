package maru.trading.domain.execution;

import maru.trading.domain.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Korea Securities Fee Calculator.
 *
 * Implements Korean stock market fee and tax rules:
 *
 * 1. Securities Transaction Tax (증권거래세):
 *    - KOSPI: 0.23% on SELL only (2024년 기준)
 *    - KOSDAQ: 0.23% on SELL only
 *    - ETF: 0% (no transaction tax)
 *    - ELW/ETN: 0%
 *
 * 2. Broker Commission (증권사 수수료):
 *    - Varies by broker (configurable via properties)
 *    - Typically 0.015% ~ 0.5%
 *    - Applied on both BUY and SELL
 *
 * Note: Agricultural tax (농어촌특별세) was abolished in 2020.
 */
@Component
public class KoreaSecuritiesFeeCalculator implements FeeCalculator {

    private static final Logger log = LoggerFactory.getLogger(KoreaSecuritiesFeeCalculator.class);

    // Securities Transaction Tax Rates (as of 2024)
    private static final BigDecimal KOSPI_TAX_RATE = new BigDecimal("0.0023");    // 0.23%
    private static final BigDecimal KOSDAQ_TAX_RATE = new BigDecimal("0.0023");   // 0.23%
    private static final BigDecimal ETF_TAX_RATE = BigDecimal.ZERO;               // 0%
    private static final BigDecimal ELW_TAX_RATE = BigDecimal.ZERO;               // 0%
    private static final BigDecimal KONEX_TAX_RATE = new BigDecimal("0.001");     // 0.1%

    // ETF/ELW/ETN symbol prefixes
    private static final String[] ETF_PREFIXES = {"069500", "102110", "148020", "229200", "251340", "252670", "278530"};
    private static final String[] ETF_NAME_PATTERNS = {"ETF", "KODEX", "TIGER", "KINDEX", "ARIRANG", "HANARO", "KOSEF"};

    @Value("${trading.fee.broker-commission-rate:0.00015}")
    private BigDecimal brokerCommissionRate;

    @Value("${trading.fee.minimum-commission:0}")
    private BigDecimal minimumCommission;

    @Override
    public BigDecimal calculateFee(String symbol, BigDecimal price, int quantity, Side side) {
        BigDecimal transactionValue = price.multiply(BigDecimal.valueOf(quantity));
        BigDecimal fee = transactionValue.multiply(brokerCommissionRate);

        // Apply minimum commission if configured
        if (minimumCommission.compareTo(BigDecimal.ZERO) > 0 && fee.compareTo(minimumCommission) < 0) {
            fee = minimumCommission;
        }

        // Round to nearest won (no decimal places for KRW)
        fee = fee.setScale(0, RoundingMode.HALF_UP);

        log.debug("Calculated broker fee: symbol={}, value={}, rate={}, fee={}",
                symbol, transactionValue, brokerCommissionRate, fee);

        return fee;
    }

    @Override
    public BigDecimal calculateTax(String symbol, BigDecimal price, int quantity, Side side) {
        // Tax only applies to SELL
        if (side != Side.SELL) {
            return BigDecimal.ZERO;
        }

        BigDecimal transactionValue = price.multiply(BigDecimal.valueOf(quantity));
        BigDecimal taxRate = determineTaxRate(symbol);
        BigDecimal tax = transactionValue.multiply(taxRate);

        // Round to nearest won
        tax = tax.setScale(0, RoundingMode.HALF_UP);

        log.debug("Calculated transaction tax: symbol={}, value={}, rate={}, tax={}",
                symbol, transactionValue, taxRate, tax);

        return tax;
    }

    /**
     * Determine tax rate based on symbol/market type.
     */
    private BigDecimal determineTaxRate(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            return KOSPI_TAX_RATE; // Default to KOSPI rate
        }

        // Check if ETF (based on known ETF symbols or naming patterns)
        if (isEtf(symbol)) {
            return ETF_TAX_RATE;
        }

        // Check if ELW (typically starts with specific codes)
        if (isElw(symbol)) {
            return ELW_TAX_RATE;
        }

        // Check if KONEX (typically 6-digit codes starting with 9)
        if (isKonex(symbol)) {
            return KONEX_TAX_RATE;
        }

        // KOSPI symbols: 6-digit starting with 0-5
        // KOSDAQ symbols: 6-digit starting with 0-4 (different range)
        // Both have same tax rate currently
        return KOSPI_TAX_RATE;
    }

    /**
     * Check if symbol is an ETF.
     * ETF detection based on:
     * 1. Known ETF symbol codes
     * 2. Name patterns (KODEX, TIGER, etc.)
     */
    private boolean isEtf(String symbol) {
        // Check known ETF prefixes
        for (String prefix : ETF_PREFIXES) {
            if (symbol.startsWith(prefix)) {
                return true;
            }
        }

        // Simple heuristic: ETF codes often start with certain patterns
        // 069xxx, 102xxx, 148xxx, 229xxx, 251xxx, 252xxx, 278xxx are common ETF ranges
        String symbolPrefix = symbol.length() >= 3 ? symbol.substring(0, 3) : symbol;
        return "069".equals(symbolPrefix) ||
               "102".equals(symbolPrefix) ||
               "148".equals(symbolPrefix) ||
               "229".equals(symbolPrefix) ||
               "251".equals(symbolPrefix) ||
               "252".equals(symbolPrefix) ||
               "278".equals(symbolPrefix) ||
               "305".equals(symbolPrefix) ||
               "360".equals(symbolPrefix);
    }

    /**
     * Check if symbol is ELW (Equity Linked Warrant).
     * ELW symbols typically start with 5 and have specific format.
     */
    private boolean isElw(String symbol) {
        return symbol.length() >= 6 && symbol.startsWith("5");
    }

    /**
     * Check if symbol is KONEX market.
     * KONEX symbols typically start with 9.
     */
    private boolean isKonex(String symbol) {
        return symbol.length() >= 6 && symbol.startsWith("9");
    }

    /**
     * Get the current broker commission rate.
     */
    public BigDecimal getBrokerCommissionRate() {
        return brokerCommissionRate;
    }

    /**
     * Set broker commission rate (for runtime configuration).
     */
    public void setBrokerCommissionRate(BigDecimal rate) {
        this.brokerCommissionRate = rate;
        log.info("Broker commission rate updated to: {}", rate);
    }
}
