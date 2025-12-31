package maru.trading.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.shared.Environment;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 전략 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StrategyResponse {
	private String strategyId;
	private String name;
	private String description;
	private String status;
	private String activeVersionId;
	private Environment mode;
	private Map<String, Object> params;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
