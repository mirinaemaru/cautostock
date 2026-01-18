package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettingsEntity {

    @Id
    @Column(name = "setting_id", columnDefinition = "CHAR(26)")
    private String settingId;

    @Column(name = "account_id", columnDefinition = "CHAR(26)", unique = true)
    private String accountId;

    @Column(name = "email_enabled", length = 1, nullable = false)
    @Builder.Default
    private String emailEnabled = "N";

    @Column(name = "email_address", length = 255)
    private String emailAddress;

    @Column(name = "push_enabled", length = 1, nullable = false)
    @Builder.Default
    private String pushEnabled = "Y";

    @Column(name = "trade_alerts", length = 1, nullable = false)
    @Builder.Default
    private String tradeAlerts = "Y";

    @Column(name = "risk_alerts", length = 1, nullable = false)
    @Builder.Default
    private String riskAlerts = "Y";

    @Column(name = "system_alerts", length = 1, nullable = false)
    @Builder.Default
    private String systemAlerts = "Y";

    @Column(name = "daily_summary", length = 1, nullable = false)
    @Builder.Default
    private String dailySummary = "N";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
