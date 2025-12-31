package maru.trading.application.ports.broker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 브로커 주문 확인 응답
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrokerAck {
	private boolean success;
	private String brokerOrderNo;
	private String message;
	private String errorCode;

	public static BrokerAck success(String brokerOrderNo) {
		return BrokerAck.builder()
				.success(true)
				.brokerOrderNo(brokerOrderNo)
				.message("Order accepted")
				.build();
	}

	public static BrokerAck failure(String errorCode, String message) {
		return BrokerAck.builder()
				.success(false)
				.errorCode(errorCode)
				.message(message)
				.build();
	}
}
