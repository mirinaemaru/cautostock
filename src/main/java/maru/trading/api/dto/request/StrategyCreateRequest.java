package maru.trading.api.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.shared.Environment;

import java.util.Map;

/**
 * 전략 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StrategyCreateRequest {

	@NotBlank(message = "Strategy name is required")
	private String name;

	private String description;

	@NotNull(message = "Mode is required")
	private Environment mode;

	@NotNull(message = "Params are required")
	private Map<String, Object> params;
}
