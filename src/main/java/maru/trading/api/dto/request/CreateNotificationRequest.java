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
    private String severity; // INFO, WARNING, ERROR, CRITICAL
    private String title;
    private String message;
    private String refType;
    private String refId;
}
