package maru.trading.api.controller.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.api.dto.response.PositionResponse;
import maru.trading.infra.persistence.jpa.entity.PositionEntity;
import maru.trading.infra.persistence.jpa.repository.PositionJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 포지션 조회 Query Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/query/positions")
@RequiredArgsConstructor
public class PositionQueryController {

	private final PositionJpaRepository positionRepository;

	/**
	 * 포지션 검색
	 */
	@GetMapping
	public ResponseEntity<Map<String, Object>> searchPositions(
			@RequestParam(required = false) String accountId
	) {
		log.info("Searching positions: accountId={}", accountId);

		List<PositionEntity> positions;

		if (accountId != null && !accountId.isEmpty()) {
			positions = positionRepository.findByAccountId(accountId);
		} else {
			positions = positionRepository.findAll();
		}

		// qty가 0보다 큰 포지션만 필터링 (보유 중인 포지션)
		List<PositionResponse> items = positions.stream()
				.filter(p -> p.getQty().compareTo(BigDecimal.ZERO) > 0)
				.map(this::toResponse)
				.collect(Collectors.toList());

		Map<String, Object> response = new HashMap<>();
		response.put("items", items);
		response.put("total", items.size());

		return ResponseEntity.ok(response);
	}

	/**
	 * 포지션 요약 조회
	 */
	@GetMapping("/summary")
	public ResponseEntity<Map<String, Object>> getPositionSummary(
			@RequestParam(required = false) String accountId
	) {
		log.info("Get position summary: accountId={}", accountId);

		List<PositionEntity> positions;
		if (accountId != null && !accountId.isEmpty()) {
			positions = positionRepository.findByAccountId(accountId);
		} else {
			positions = positionRepository.findAll();
		}

		// 보유 중인 포지션만 필터링
		List<PositionEntity> activePositions = positions.stream()
				.filter(p -> p.getQty().compareTo(BigDecimal.ZERO) > 0)
				.toList();

		// 총 평가액 계산
		BigDecimal totalValue = activePositions.stream()
				.map(p -> p.getAvgPrice().multiply(p.getQty()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// 총 실현손익
		BigDecimal totalRealizedPnl = activePositions.stream()
				.map(PositionEntity::getRealizedPnl)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// 종목 수
		long symbolCount = activePositions.stream()
				.map(PositionEntity::getSymbol)
				.distinct()
				.count();

		Map<String, Object> summary = new HashMap<>();
		summary.put("accountId", accountId);
		summary.put("totalPositions", activePositions.size());
		summary.put("totalValue", totalValue);
		summary.put("totalRealizedPnl", totalRealizedPnl);
		summary.put("totalUnrealizedPnl", BigDecimal.ZERO); // 현재가 없이는 계산 불가
		summary.put("symbolCount", symbolCount);
		summary.put("timestamp", java.time.LocalDateTime.now());

		return ResponseEntity.ok(summary);
	}

	/**
	 * 포지션 조회
	 */
	@GetMapping("/{positionId}")
	public ResponseEntity<PositionResponse> getPosition(@PathVariable String positionId) {
		log.info("Get position: positionId={}", positionId);

		PositionEntity position = positionRepository.findById(positionId)
				.orElseThrow(() -> new RuntimeException("Position not found: " + positionId));

		return ResponseEntity.ok(toResponse(position));
	}

	private PositionResponse toResponse(PositionEntity entity) {
		// TODO: 현재가 조회하여 미실현손익 계산 (추후 구현)
		BigDecimal currentPrice = entity.getAvgPrice(); // 임시로 평단가 사용
		BigDecimal unrealizedPnl = entity.getQty().multiply(currentPrice.subtract(entity.getAvgPrice()));
		BigDecimal totalValue = entity.getQty().multiply(currentPrice);

		return PositionResponse.builder()
				.positionId(entity.getPositionId())
				.accountId(entity.getAccountId())
				.symbol(entity.getSymbol())
				.qty(entity.getQty())
				.avgPrice(entity.getAvgPrice())
				.realizedPnl(entity.getRealizedPnl())
				.currentPrice(currentPrice)
				.unrealizedPnl(unrealizedPnl)
				.totalValue(totalValue)
				.updatedAt(entity.getUpdatedAt())
				.build();
	}
}
