package maru.trading.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for fill data.
 * Maps from Fill domain model for API responses.
 */
public class FillResponse {

    @JsonProperty("fillId")
    private String fillId;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("accountId")
    private String accountId;

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("side")
    private String side; // "BUY" or "SELL"

    @JsonProperty("fillPrice")
    private BigDecimal fillPrice;

    @JsonProperty("fillQty")
    private int fillQty;

    @JsonProperty("fee")
    private BigDecimal fee;

    @JsonProperty("tax")
    private BigDecimal tax;

    @JsonProperty("fillTs")
    private LocalDateTime fillTs;

    @JsonProperty("brokerOrderNo")
    private String brokerOrderNo;

    public FillResponse() {
    }

    public FillResponse(
            String fillId,
            String orderId,
            String accountId,
            String symbol,
            String side,
            BigDecimal fillPrice,
            int fillQty,
            BigDecimal fee,
            BigDecimal tax,
            LocalDateTime fillTs,
            String brokerOrderNo) {
        this.fillId = fillId;
        this.orderId = orderId;
        this.accountId = accountId;
        this.symbol = symbol;
        this.side = side;
        this.fillPrice = fillPrice;
        this.fillQty = fillQty;
        this.fee = fee;
        this.tax = tax;
        this.fillTs = fillTs;
        this.brokerOrderNo = brokerOrderNo;
    }

    // Getters and Setters
    public String getFillId() {
        return fillId;
    }

    public void setFillId(String fillId) {
        this.fillId = fillId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public BigDecimal getFillPrice() {
        return fillPrice;
    }

    public void setFillPrice(BigDecimal fillPrice) {
        this.fillPrice = fillPrice;
    }

    public int getFillQty() {
        return fillQty;
    }

    public void setFillQty(int fillQty) {
        this.fillQty = fillQty;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public LocalDateTime getFillTs() {
        return fillTs;
    }

    public void setFillTs(LocalDateTime fillTs) {
        this.fillTs = fillTs;
    }

    public String getBrokerOrderNo() {
        return brokerOrderNo;
    }

    public void setBrokerOrderNo(String brokerOrderNo) {
        this.brokerOrderNo = brokerOrderNo;
    }
}
