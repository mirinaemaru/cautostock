package maru.trading.application.orchestration;

import lombok.Builder;
import lombok.Getter;

/**
 * Signal 처리 결과
 */
@Getter
@Builder
public class SignalProcessingResult {

	public enum Status {
		SUCCESS,      // 주문 성공적으로 전송됨
		REJECTED,     // Risk 엔진에 의해 거부됨
		SKIPPED,      // HOLD 신호로 스킵됨
		FAILED        // 처리 중 오류 발생
	}

	private final Status status;
	private final String orderId;
	private final String message;

	public static SignalProcessingResult success(String orderId) {
		return SignalProcessingResult.builder()
				.status(Status.SUCCESS)
				.orderId(orderId)
				.message("Order sent successfully")
				.build();
	}

	public static SignalProcessingResult rejected(String reason) {
		return SignalProcessingResult.builder()
				.status(Status.REJECTED)
				.message(reason)
				.build();
	}

	public static SignalProcessingResult skipped(String reason) {
		return SignalProcessingResult.builder()
				.status(Status.SKIPPED)
				.message(reason)
				.build();
	}

	public static SignalProcessingResult failed(String error) {
		return SignalProcessingResult.builder()
				.status(Status.FAILED)
				.message(error)
				.build();
	}

	public boolean isSuccess() {
		return status == Status.SUCCESS;
	}
}
