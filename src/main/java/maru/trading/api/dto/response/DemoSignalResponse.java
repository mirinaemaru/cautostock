package maru.trading.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 데모 시그널 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemoSignalResponse {

	private boolean ok;
	private String message;
	private String signalId;
	private String accountId;
	private String symbol;
	private String signalType;
	private BigDecimal targetValue;
	private String orderStatus;
	private String orderId;
}
