package maru.trading.broker.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for KIS WebSocket tick message.
 * Maps to KIS H0STCNT0 (real-time tick) message format.
 */
public class KisTickMessage {

    @JsonProperty("MKSC_SHRN_ISCD")
    private String symbol; // Stock code (e.g., "005930")

    @JsonProperty("STCK_PRPR")
    private String price; // Current price

    @JsonProperty("CNTG_VOL")
    private String volume; // Contract volume

    @JsonProperty("STCK_CNTG_HOUR")
    private String time; // Contract time (HHMMSS format)

    @JsonProperty("TRDG_STTS")
    private String tradingStatus; // Trading status

    public KisTickMessage() {
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTradingStatus() {
        return tradingStatus;
    }

    public void setTradingStatus(String tradingStatus) {
        this.tradingStatus = tradingStatus;
    }

    @Override
    public String toString() {
        return "KisTickMessage{" +
                "symbol='" + symbol + '\'' +
                ", price='" + price + '\'' +
                ", volume='" + volume + '\'' +
                ", time='" + time + '\'' +
                ", tradingStatus='" + tradingStatus + '\'' +
                '}';
    }
}
