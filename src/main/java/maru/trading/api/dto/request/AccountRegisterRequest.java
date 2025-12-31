package maru.trading.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.shared.Environment;

/**
 * 계좌 등록 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountRegisterRequest {

	@NotBlank(message = "Broker is required")
	private String broker;

	@NotNull(message = "Environment is required")
	private Environment environment;

	@NotBlank(message = "Cano is required")
	private String cano;

	@NotBlank(message = "AcntPrdtCd is required")
	private String acntPrdtCd;

	private String alias;
}
