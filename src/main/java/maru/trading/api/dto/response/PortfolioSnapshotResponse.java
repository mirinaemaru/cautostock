package maru.trading.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for portfolio snapshot.
 * Maps from PortfolioSnapshot domain model for API responses.
 */
public class PortfolioSnapshotResponse {

    @JsonProperty("snapshotId")
    private String snapshotId;

    @JsonProperty("accountId")
    private String accountId;

    @JsonProperty("totalValue")
    private BigDecimal totalValue;

    @JsonProperty("cash")
    private BigDecimal cash;

    @JsonProperty("realizedPnl")
    private BigDecimal realizedPnl;

    @JsonProperty("unrealizedPnl")
    private BigDecimal unrealizedPnl;

    @JsonProperty("snapshotTs")
    private LocalDateTime snapshotTs;

    public PortfolioSnapshotResponse() {
    }

    public PortfolioSnapshotResponse(
            String snapshotId,
            String accountId,
            BigDecimal totalValue,
            BigDecimal cash,
            BigDecimal realizedPnl,
            BigDecimal unrealizedPnl,
            LocalDateTime snapshotTs) {
        this.snapshotId = snapshotId;
        this.accountId = accountId;
        this.totalValue = totalValue;
        this.cash = cash;
        this.realizedPnl = realizedPnl;
        this.unrealizedPnl = unrealizedPnl;
        this.snapshotTs = snapshotTs;
    }

    // Getters and Setters
    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getCash() {
        return cash;
    }

    public void setCash(BigDecimal cash) {
        this.cash = cash;
    }

    public BigDecimal getRealizedPnl() {
        return realizedPnl;
    }

    public void setRealizedPnl(BigDecimal realizedPnl) {
        this.realizedPnl = realizedPnl;
    }

    public BigDecimal getUnrealizedPnl() {
        return unrealizedPnl;
    }

    public void setUnrealizedPnl(BigDecimal unrealizedPnl) {
        this.unrealizedPnl = unrealizedPnl;
    }

    public LocalDateTime getSnapshotTs() {
        return snapshotTs;
    }

    public void setSnapshotTs(LocalDateTime snapshotTs) {
        this.snapshotTs = snapshotTs;
    }
}
