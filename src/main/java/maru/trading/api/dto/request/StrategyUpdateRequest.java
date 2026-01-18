package maru.trading.api.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.shared.Environment;

import java.util.Map;

/**
 * 전략 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StrategyUpdateRequest {

	private String name;

	private String description;

	private Environment mode;

	private String status;

	private Map<String, Object> params;

	// ========== 자동매매 설정 필드 ==========

	// 거래 설정
	private String accountId;
	private String assetType;
	private String symbol;

	// 진입/청산 조건 (JSON TEXT)
	private String entryConditions;
	private String exitConditions;

	// 리스크 관리
	private String stopLossType;
	private Double stopLossValue;
	private String takeProfitType;
	private Double takeProfitValue;

	// 포지션 크기
	private String positionSizeType;
	private Double positionSizeValue;
	private Integer maxPositions;
}
