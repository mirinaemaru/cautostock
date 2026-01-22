package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Alert Log Entity.
 * Maps to the alert_log table for tracking sent alerts.
 */
@Entity
@Table(name = "alert_log", indexes = {
        @Index(name = "idx_alert_log_severity_sent", columnList = "severity, sent_at"),
        @Index(name = "idx_alert_log_related_event", columnList = "related_event_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertLogEntity {

    @Id
    @Column(name = "alert_id", columnDefinition = "CHAR(26)")
    private String alertId;

    @Column(name = "severity", length = 8, nullable = false)
    private String severity;

    @Column(name = "category", length = 16, nullable = false)
    private String category;

    @Column(name = "channel", length = 16, nullable = false)
    private String channel;

    @Column(name = "message", length = 512, nullable = false)
    private String message;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "related_event_id", columnDefinition = "CHAR(26)")
    private String relatedEventId;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
    }

    public boolean isSuccessful() {
        return Boolean.TRUE.equals(success);
    }

    public boolean isCritical() {
        return "CRIT".equals(severity);
    }

    public boolean isWarning() {
        return "WARN".equals(severity);
    }
}
