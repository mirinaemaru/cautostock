package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Account Permission Entity
 *
 * 계좌별 거래 권한 설정
 */
@Entity
@Table(name = "account_permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AccountPermissionEntity {

    /**
     * 계좌 ID (FK to accounts)
     */
    @Id
    @Column(name = "account_id", length = 26, nullable = false)
    private String accountId;

    /**
     * 매수 허용
     */
    @Column(name = "trade_buy", nullable = false)
    private Boolean tradeBuy = false;

    /**
     * 매도 허용
     */
    @Column(name = "trade_sell", nullable = false)
    private Boolean tradeSell = false;

    /**
     * 자동매매 허용
     */
    @Column(name = "auto_trade", nullable = false)
    private Boolean autoTrade = false;

    /**
     * 수동매매 허용
     */
    @Column(name = "manual_trade", nullable = false)
    private Boolean manualTrade = false;

    /**
     * PAPER 전용 (1이면 LIVE 차단)
     */
    @Column(name = "paper_only", nullable = false)
    private Boolean paperOnly = true;

    /**
     * 수정 시각
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public AccountPermissionEntity(String accountId, Boolean tradeBuy, Boolean tradeSell,
                                   Boolean autoTrade, Boolean manualTrade, Boolean paperOnly,
                                   LocalDateTime updatedAt) {
        this.accountId = accountId;
        this.tradeBuy = tradeBuy != null ? tradeBuy : false;
        this.tradeSell = tradeSell != null ? tradeSell : false;
        this.autoTrade = autoTrade != null ? autoTrade : false;
        this.manualTrade = manualTrade != null ? manualTrade : false;
        this.paperOnly = paperOnly != null ? paperOnly : true;
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    /**
     * 권한 업데이트
     */
    public void updatePermissions(Boolean tradeBuy, Boolean tradeSell, Boolean autoTrade,
                                  Boolean manualTrade, Boolean paperOnly) {
        if (tradeBuy != null) {
            this.tradeBuy = tradeBuy;
        }
        if (tradeSell != null) {
            this.tradeSell = tradeSell;
        }
        if (autoTrade != null) {
            this.autoTrade = autoTrade;
        }
        if (manualTrade != null) {
            this.manualTrade = manualTrade;
        }
        if (paperOnly != null) {
            this.paperOnly = paperOnly;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 기본 권한으로 생성
     */
    public static AccountPermissionEntity createDefault(String accountId) {
        return AccountPermissionEntity.builder()
                .accountId(accountId)
                .tradeBuy(false)
                .tradeSell(false)
                .autoTrade(false)
                .manualTrade(false)
                .paperOnly(true)
                .build();
    }
}
