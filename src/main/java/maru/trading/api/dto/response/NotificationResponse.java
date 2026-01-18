package maru.trading.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.infra.persistence.jpa.entity.NotificationEntity;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {

    private String notificationId;
    private String accountId;
    private String notificationType;
    private String severity;
    private String title;
    private String message;
    private String refType;
    private String refId;
    private boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public static NotificationResponse fromEntity(NotificationEntity entity) {
        return NotificationResponse.builder()
                .notificationId(entity.getNotificationId())
                .accountId(entity.getAccountId())
                .notificationType(entity.getNotificationType())
                .severity(entity.getSeverity())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .refType(entity.getRefType())
                .refId(entity.getRefId())
                .isRead(!entity.isUnread())
                .createdAt(entity.getCreatedAt())
                .readAt(entity.getReadAt())
                .build();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationList {
        private List<NotificationResponse> notifications;
        private int totalCount;
        private int unreadCount;
        private int page;
        private int pageSize;
    }
}
