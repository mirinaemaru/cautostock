package maru.trading.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNotificationRequest {

    private String accountId;
    private String notificationType; // TRADE, RISK, SYSTEM, ALERT
    private String type; // Alias for notificationType
    private String severity; // INFO, WARNING, ERROR, CRITICAL
    private String priority; // Alias for severity (HIGH, MEDIUM, LOW, CRITICAL)
    private String title;
    private String message;
    private String refType;
    private String refId;
    private Object data; // Additional data

    /**
     * Get notification type, using type alias if notificationType is not set.
     */
    public String getNotificationType() {
        if (notificationType != null && !notificationType.isBlank()) {
            return notificationType;
        }
        return type;
    }

    /**
     * Get severity, using priority alias if severity is not set.
     */
    public String getSeverity() {
        if (severity != null && !severity.isBlank()) {
            return severity;
        }
        if (priority != null && !priority.isBlank()) {
            // Map priority to severity
            return switch (priority.toUpperCase()) {
                case "CRITICAL" -> "CRITICAL";
                case "HIGH" -> "ERROR";
                case "MEDIUM" -> "WARNING";
                case "LOW" -> "INFO";
                default -> "INFO";
            };
        }
        return null;
    }
}
