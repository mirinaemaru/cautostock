package maru.trading.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.order.Side;

import java.math.BigDecimal;

/**
 * 데모 신호 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemoSignalRequest {

	private String accountId;

	@NotBlank(message = "Symbol is required")
	private String symbol;

	@NotNull(message = "Side is required")
	private Side side;

	private String targetType; // QTY, WEIGHT

	@NotNull(message = "Target value is required")
	private BigDecimal targetValue;

	private Integer ttlSeconds;
}
