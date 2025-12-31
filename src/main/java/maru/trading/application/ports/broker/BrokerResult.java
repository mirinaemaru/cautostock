package maru.trading.application.ports.broker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 브로커 작업 결과
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrokerResult {
	private boolean success;
	private String message;
	private String errorCode;

	public static BrokerResult success(String message) {
		return BrokerResult.builder()
				.success(true)
				.message(message)
				.build();
	}

	public static BrokerResult failure(String errorCode, String message) {
		return BrokerResult.builder()
				.success(false)
				.errorCode(errorCode)
				.message(message)
				.build();
	}
}
