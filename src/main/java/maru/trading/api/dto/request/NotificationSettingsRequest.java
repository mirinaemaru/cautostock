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
public class NotificationSettingsRequest {

    private Boolean emailEnabled;
    private String emailAddress;
    private Boolean pushEnabled;
    private Boolean tradeAlerts;
    private Boolean riskAlerts;
    private Boolean systemAlerts;
    private Boolean dailySummary;
}
