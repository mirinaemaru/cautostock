package maru.trading.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.risk.KillSwitchStatus;

/**
 * Kill Switch 토글 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KillSwitchToggleRequest {

	private String accountId; // null이면 전역

	@NotNull(message = "Status is required")
	private KillSwitchStatus status;

	@NotBlank(message = "Reason is required")
	private String reason;
}
