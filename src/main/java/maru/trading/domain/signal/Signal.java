package maru.trading.domain.signal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 신호 도메인 모델
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Signal {
	private String signalId;
	private String strategyId;
	private String strategyVersionId;
	private String accountId;
	private String symbol;
	private SignalType signalType;
	private String targetType; // QTY, WEIGHT
	private BigDecimal targetValue;
	private Integer ttlSeconds;
	private String reason;
}
