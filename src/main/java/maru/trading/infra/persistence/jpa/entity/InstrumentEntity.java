package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity for instruments master table.
 * Represents KOSPI/KOSDAQ stock metadata and trading rules.
 */
@Entity
@Table(name = "instruments")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstrumentEntity {

    @Id
    @Column(name = "symbol", length = 16)
    private String symbol;

    @Column(name = "market", length = 10, nullable = false)
    private String market;

    @Column(name = "name_kr", length = 100, nullable = false)
    private String nameKr;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(name = "sector_code", length = 10)
    private String sectorCode;

    @Column(name = "industry", length = 50)
    private String industry;

    @Column(name = "tick_size", precision = 15, scale = 2, nullable = false)
    private BigDecimal tickSize;

    @Column(name = "lot_size", nullable = false)
    private Integer lotSize;

    @Column(name = "listing_date")
    private LocalDate listingDate;

    @Column(name = "delisting_date")
    private LocalDate delistingDate;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "tradable", nullable = false)
    private Boolean tradable;

    @Column(name = "halted", nullable = false)
    private Boolean halted;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = "LISTED";
        }
        if (tradable == null) {
            tradable = true;
        }
        if (halted == null) {
            halted = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Update trading status.
     */
    public void updateStatus(String newStatus, Boolean isTradable, Boolean isHalted) {
        this.status = newStatus;
        this.tradable = isTradable;
        this.halted = isHalted;
    }

    /**
     * Mark as delisted.
     */
    public void delist(LocalDate delistingDate) {
        this.status = "DELISTED";
        this.tradable = false;
        this.delistingDate = delistingDate;
    }

    /**
     * Update instrument name.
     */
    public void updateName(String nameKr, String nameEn) {
        this.nameKr = nameKr;
        this.nameEn = nameEn;
    }

    /**
     * Update market.
     */
    public void updateMarket(String market) {
        this.market = market;
    }
}
