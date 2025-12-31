package maru.trading.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 에러 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
	private String code;
	private String message;
	private String detail;
	private String requestId;
	private LocalDateTime timestamp;

	public static ErrorResponse of(String code, String message) {
		return ErrorResponse.builder()
				.code(code)
				.message(message)
				.timestamp(LocalDateTime.now())
				.build();
	}

	public static ErrorResponse of(String code, String message, String detail) {
		return ErrorResponse.builder()
				.code(code)
				.message(message)
				.detail(detail)
				.timestamp(LocalDateTime.now())
				.build();
	}

	public static ErrorResponse of(String code, String message, String detail, String requestId) {
		return ErrorResponse.builder()
				.code(code)
				.message(message)
				.detail(detail)
				.requestId(requestId)
				.timestamp(LocalDateTime.now())
				.build();
	}
}
