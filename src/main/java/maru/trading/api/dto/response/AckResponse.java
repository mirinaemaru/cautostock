package maru.trading.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 간단한 확인 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AckResponse {
	private Boolean ok;
	private String message;
	private String requestId;

	public static AckResponse success() {
		return AckResponse.builder()
				.ok(true)
				.message("accepted")
				.build();
	}

	public static AckResponse success(String message) {
		return AckResponse.builder()
				.ok(true)
				.message(message)
				.build();
	}
}
