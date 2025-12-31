package maru.trading.broker.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for KIS WebSocket fill notification message.
 * Maps to KIS fill event message format.
 */
public class KisFillMessage {

    @JsonProperty("ODNO")
    private String brokerOrderNo; // Broker order number

    @JsonProperty("SELN_BYOV_CLS")
    private String sideCode; // Side code ("01" = BUY, "02" = SELL)

    @JsonProperty("CNTG_PRCE")
    private String fillPrice; // Fill price

    @JsonProperty("CNTG_QTY")
    private String fillQty; // Fill quantity

    @JsonProperty("ODNO_ORGNO")
    private String orderId; // Internal order ID (if available)

    @JsonProperty("PDNO")
    private String symbol; // Stock code

    @JsonProperty("CNTG_TIME")
    private String fillTime; // Fill time (HHMMSS format)

    public KisFillMessage() {
    }

    public String getBrokerOrderNo() {
        return brokerOrderNo;
    }

    public void setBrokerOrderNo(String brokerOrderNo) {
        this.brokerOrderNo = brokerOrderNo;
    }

    public String getSideCode() {
        return sideCode;
    }

    public void setSideCode(String sideCode) {
        this.sideCode = sideCode;
    }

    public String getFillPrice() {
        return fillPrice;
    }

    public void setFillPrice(String fillPrice) {
        this.fillPrice = fillPrice;
    }

    public String getFillQty() {
        return fillQty;
    }

    public void setFillQty(String fillQty) {
        this.fillQty = fillQty;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getFillTime() {
        return fillTime;
    }

    public void setFillTime(String fillTime) {
        this.fillTime = fillTime;
    }

    @Override
    public String toString() {
        return "KisFillMessage{" +
                "brokerOrderNo='" + brokerOrderNo + '\'' +
                ", sideCode='" + sideCode + '\'' +
                ", fillPrice='" + fillPrice + '\'' +
                ", fillQty='" + fillQty + '\'' +
                ", symbol='" + symbol + '\'' +
                '}';
    }
}
