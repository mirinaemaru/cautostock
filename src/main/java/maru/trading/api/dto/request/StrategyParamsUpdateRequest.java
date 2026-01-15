package maru.trading.api.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 전략 파라미터 수정 요청 DTO
 *
 * 새로운 버전을 생성하고 activeVersionId를 업데이트합니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StrategyParamsUpdateRequest {

	@NotNull(message = "Params are required")
	private Map<String, Object> params;
}
