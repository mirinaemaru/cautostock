package maru.trading.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 포지션 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PositionResponse {
	private String positionId;
	private String accountId;
	private String symbol;
	private BigDecimal qty;
	private BigDecimal avgPrice;
	private BigDecimal realizedPnl;
	private BigDecimal currentPrice;  // 현재가 (별도 조회 필요)
	private BigDecimal unrealizedPnl; // 미실현손익
	private BigDecimal totalValue;    // 평가금액
	private LocalDateTime updatedAt;
}
