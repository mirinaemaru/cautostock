package maru.trading.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.risk.KillSwitchStatus;

import java.time.LocalDateTime;

/**
 * Kill Switch 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KillSwitchResponse {
	private String accountId;
	private KillSwitchStatus status;
	private String reason;
	private LocalDateTime updatedAt;
}
