package maru.trading.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.infra.persistence.jpa.entity.NotificationSettingsEntity;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationSettingsResponse {

    private String settingId;
    private String accountId;
    private boolean emailEnabled;
    private String emailAddress;
    private boolean pushEnabled;
    private boolean tradeAlerts;
    private boolean riskAlerts;
    private boolean systemAlerts;
    private boolean dailySummary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NotificationSettingsResponse fromEntity(NotificationSettingsEntity entity) {
        return NotificationSettingsResponse.builder()
                .settingId(entity.getSettingId())
                .accountId(entity.getAccountId())
                .emailEnabled("Y".equals(entity.getEmailEnabled()))
                .emailAddress(entity.getEmailAddress())
                .pushEnabled("Y".equals(entity.getPushEnabled()))
                .tradeAlerts("Y".equals(entity.getTradeAlerts()))
                .riskAlerts("Y".equals(entity.getRiskAlerts()))
                .systemAlerts("Y".equals(entity.getSystemAlerts()))
                .dailySummary("Y".equals(entity.getDailySummary()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
