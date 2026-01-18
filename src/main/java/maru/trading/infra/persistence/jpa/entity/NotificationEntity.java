package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {

    @Id
    @Column(name = "notification_id", columnDefinition = "CHAR(26)")
    private String notificationId;

    @Column(name = "account_id", columnDefinition = "CHAR(26)")
    private String accountId;

    @Column(name = "notification_type", length = 32, nullable = false)
    private String notificationType;

    @Column(name = "severity", length = 16, nullable = false)
    @Builder.Default
    private String severity = "INFO";

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "ref_type", length = 32)
    private String refType;

    @Column(name = "ref_id", columnDefinition = "CHAR(26)")
    private String refId;

    @Column(name = "is_read", length = 1, nullable = false)
    @Builder.Default
    private String isRead = "N";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void markAsRead() {
        this.isRead = "Y";
        this.readAt = LocalDateTime.now();
    }

    public boolean isUnread() {
        return "N".equals(this.isRead);
    }
}
