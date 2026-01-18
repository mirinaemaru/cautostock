package maru.trading.api.controller.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.api.dto.request.StrategyCreateRequest;
import maru.trading.api.dto.request.StrategyParamsUpdateRequest;
import maru.trading.api.dto.request.StrategyStatusUpdateRequest;
import maru.trading.api.dto.request.StrategyUpdateRequest;
import maru.trading.api.dto.response.StrategyResponse;
import maru.trading.domain.shared.DomainException;
import maru.trading.domain.shared.ErrorCode;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.StrategyEntity;
import maru.trading.infra.persistence.jpa.entity.StrategyVersionEntity;
import maru.trading.infra.persistence.jpa.repository.StrategyJpaRepository;
import maru.trading.infra.persistence.jpa.repository.StrategyVersionJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 전략 관리 Admin Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/strategies")
@RequiredArgsConstructor
public class StrategyAdminController {

	private final StrategyJpaRepository strategyRepository;
	private final StrategyVersionJpaRepository strategyVersionRepository;
	private final ObjectMapper objectMapper;

	/**
	 * 전략 생성
	 */
	@PostMapping
	public ResponseEntity<StrategyResponse> createStrategy(
			@Valid @RequestBody StrategyCreateRequest request
	) {
		log.info("Create strategy: name={}, mode={}", request.getName(), request.getMode());

		// 중복 체크
		strategyRepository.findByName(request.getName())
				.ifPresent(existing -> {
					throw new IllegalStateException("Strategy already exists: " + existing.getName());
				});

		String strategyId = UlidGenerator.generate();
		String versionId = UlidGenerator.generate();

		// Strategy 먼저 생성 (FK 제약조건 때문에 strategy_versions보다 먼저)
		StrategyEntity strategy = StrategyEntity.builder()
				.strategyId(strategyId)
				.name(request.getName())
				.description(request.getDescription())
				.status("INACTIVE")
				.mode(request.getMode())
				.activeVersionId(versionId)
				.build();

		StrategyEntity saved = strategyRepository.save(strategy);

		// Strategy Version 생성 (strategy_id FK 참조)
		StrategyVersionEntity version = StrategyVersionEntity.builder()
				.strategyVersionId(versionId)
				.strategyId(strategyId)
				.versionNo(1)
				.paramsJson(toJson(request.getParams()))
				.build();

		strategyVersionRepository.save(version);

		StrategyResponse response = toResponse(saved, request.getParams());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * 전략 목록 조회
	 */
	@GetMapping
	public ResponseEntity<Map<String, List<StrategyResponse>>> listStrategies() {
		List<StrategyEntity> strategies = strategyRepository.findAll();

		List<StrategyResponse> items = strategies.stream()
				.map(s -> {
					StrategyVersionEntity version = strategyVersionRepository
							.findById(s.getActiveVersionId())
							.orElse(null);
					Map<String, Object> params = version != null ? fromJson(version.getParamsJson()) : new HashMap<>();
					return toResponse(s, params);
				})
				.collect(Collectors.toList());

		Map<String, List<StrategyResponse>> response = new HashMap<>();
		response.put("items", items);

		return ResponseEntity.ok(response);
	}

	/**
	 * 전략 조회
	 */
	@GetMapping("/{strategyId}")
	public ResponseEntity<StrategyResponse> getStrategy(@PathVariable String strategyId) {
		StrategyEntity strategy = strategyRepository.findById(strategyId)
				.orElseThrow(() -> new DomainException(ErrorCode.STRATEGY_001, "Strategy not found: " + strategyId));

		StrategyVersionEntity version = strategyVersionRepository
				.findById(strategy.getActiveVersionId())
				.orElse(null);

		Map<String, Object> params = version != null ? fromJson(version.getParamsJson()) : new HashMap<>();

		return ResponseEntity.ok(toResponse(strategy, params));
	}

	/**
	 * 전략 활성화
	 */
	@PostMapping("/{strategyId}/activate")
	public ResponseEntity<StrategyResponse> activateStrategy(@PathVariable String strategyId) {
		StrategyEntity strategy = strategyRepository.findById(strategyId)
				.orElseThrow(() -> new DomainException(ErrorCode.STRATEGY_001, "Strategy not found: " + strategyId));

		strategy.activate(strategy.getActiveVersionId());
		StrategyEntity updated = strategyRepository.save(strategy);

		StrategyVersionEntity version = strategyVersionRepository
				.findById(updated.getActiveVersionId())
				.orElse(null);

		Map<String, Object> params = version != null ? fromJson(version.getParamsJson()) : new HashMap<>();

		return ResponseEntity.ok(toResponse(updated, params));
	}

	/**
	 * 전략 파라미터 수정 (새 버전 생성)
	 */
	@PutMapping("/{strategyId}/params")
	public ResponseEntity<StrategyResponse> updateStrategyParams(
			@PathVariable String strategyId,
			@Valid @RequestBody StrategyParamsUpdateRequest request
	) {
		log.info("Update strategy params: strategyId={}", strategyId);

		StrategyEntity strategy = strategyRepository.findById(strategyId)
				.orElseThrow(() -> new DomainException(ErrorCode.STRATEGY_001, "Strategy not found: " + strategyId));

		// 현재 최대 버전 번호 조회
		Integer maxVersionNo = strategyVersionRepository.findMaxVersionNoByStrategyId(strategyId)
				.orElse(0);

		// 새 버전 생성
		String newVersionId = UlidGenerator.generate();
		StrategyVersionEntity newVersion = StrategyVersionEntity.builder()
				.strategyVersionId(newVersionId)
				.strategyId(strategyId)
				.versionNo(maxVersionNo + 1)
				.paramsJson(toJson(request.getParams()))
				.build();

		strategyVersionRepository.save(newVersion);

		// 전략의 activeVersionId 업데이트
		strategy.setActiveVersionId(newVersionId);
		StrategyEntity updated = strategyRepository.save(strategy);

		log.info("Created new strategy version: strategyId={}, versionId={}, versionNo={}",
				strategyId, newVersionId, maxVersionNo + 1);

		return ResponseEntity.ok(toResponse(updated, request.getParams()));
	}

	/**
	 * 전략 비활성화
	 */
	@PostMapping("/{strategyId}/deactivate")
	public ResponseEntity<StrategyResponse> deactivateStrategy(@PathVariable String strategyId) {
		StrategyEntity strategy = strategyRepository.findById(strategyId)
				.orElseThrow(() -> new DomainException(ErrorCode.STRATEGY_001, "Strategy not found: " + strategyId));

		strategy.deactivate();
		StrategyEntity updated = strategyRepository.save(strategy);

		StrategyVersionEntity version = strategyVersionRepository
				.findById(updated.getActiveVersionId())
				.orElse(null);

		Map<String, Object> params = version != null ? fromJson(version.getParamsJson()) : new HashMap<>();

		return ResponseEntity.ok(toResponse(updated, params));
	}

	/**
	 * 전략 수정 (이름, 설명, 모드, 상태, 파라미터, 자동매매 설정)
	 */
	@PutMapping("/{strategyId}")
	public ResponseEntity<StrategyResponse> updateStrategy(
			@PathVariable String strategyId,
			@RequestBody StrategyUpdateRequest request
	) {
		log.info("Update strategy: strategyId={}, request={}", strategyId, request);

		StrategyEntity strategy = strategyRepository.findById(strategyId)
				.orElseThrow(() -> new DomainException(ErrorCode.STRATEGY_001, "Strategy not found: " + strategyId));

		// 이름 수정
		if (request.getName() != null && !request.getName().isBlank()) {
			strategy.setName(request.getName());
		}

		// 설명 수정
		if (request.getDescription() != null) {
			strategy.setDescription(request.getDescription());
		}

		// 모드 수정
		if (request.getMode() != null) {
			strategy.setMode(request.getMode());
		}

		// 상태 수정
		if (request.getStatus() != null && !request.getStatus().isBlank()) {
			if ("ACTIVE".equalsIgnoreCase(request.getStatus())) {
				strategy.activate(strategy.getActiveVersionId());
			} else if ("INACTIVE".equalsIgnoreCase(request.getStatus())) {
				strategy.deactivate();
			} else {
				strategy.setStatus(request.getStatus().toUpperCase());
			}
		}

		// ========== 자동매매 설정 수정 ==========

		// 거래 설정
		if (request.getAccountId() != null) {
			strategy.setAccountId(request.getAccountId());
		}
		if (request.getAssetType() != null) {
			strategy.setAssetType(request.getAssetType());
		}
		if (request.getSymbol() != null) {
			strategy.setSymbol(request.getSymbol());
		}

		// 진입/청산 조건
		if (request.getEntryConditions() != null) {
			strategy.setEntryConditions(request.getEntryConditions());
		}
		if (request.getExitConditions() != null) {
			strategy.setExitConditions(request.getExitConditions());
		}

		// 리스크 관리
		if (request.getStopLossType() != null) {
			strategy.setStopLossType(request.getStopLossType());
		}
		if (request.getStopLossValue() != null) {
			strategy.setStopLossValue(request.getStopLossValue());
		}
		if (request.getTakeProfitType() != null) {
			strategy.setTakeProfitType(request.getTakeProfitType());
		}
		if (request.getTakeProfitValue() != null) {
			strategy.setTakeProfitValue(request.getTakeProfitValue());
		}

		// 포지션 크기
		if (request.getPositionSizeType() != null) {
			strategy.setPositionSizeType(request.getPositionSizeType());
		}
		if (request.getPositionSizeValue() != null) {
			strategy.setPositionSizeValue(request.getPositionSizeValue());
		}
		if (request.getMaxPositions() != null) {
			strategy.setMaxPositions(request.getMaxPositions());
		}

		// 파라미터 수정 (새 버전 생성)
		Map<String, Object> params;
		if (request.getParams() != null && !request.getParams().isEmpty()) {
			Integer maxVersionNo = strategyVersionRepository.findMaxVersionNoByStrategyId(strategyId)
					.orElse(0);

			String newVersionId = UlidGenerator.generate();
			StrategyVersionEntity newVersion = StrategyVersionEntity.builder()
					.strategyVersionId(newVersionId)
					.strategyId(strategyId)
					.versionNo(maxVersionNo + 1)
					.paramsJson(toJson(request.getParams()))
					.build();

			strategyVersionRepository.save(newVersion);
			strategy.setActiveVersionId(newVersionId);
			params = request.getParams();

			log.info("Created new strategy version: strategyId={}, versionId={}, versionNo={}",
					strategyId, newVersionId, maxVersionNo + 1);
		} else {
			// 파라미터 변경 없으면 기존 버전 유지
			StrategyVersionEntity version = strategyVersionRepository
					.findById(strategy.getActiveVersionId())
					.orElse(null);
			params = version != null ? fromJson(version.getParamsJson()) : new HashMap<>();
		}

		StrategyEntity updated = strategyRepository.save(strategy);

		return ResponseEntity.ok(toResponse(updated, params));
	}

	/**
	 * 전략 삭제
	 */
	@DeleteMapping("/{strategyId}")
	@Transactional
	public ResponseEntity<Map<String, Object>> deleteStrategy(@PathVariable String strategyId) {
		log.info("Delete strategy: strategyId={}", strategyId);

		StrategyEntity strategy = strategyRepository.findById(strategyId)
				.orElseThrow(() -> new DomainException(ErrorCode.STRATEGY_001, "Strategy not found: " + strategyId));

		// 관련 버전들 삭제
		strategyVersionRepository.deleteByStrategyId(strategyId);

		// 전략 삭제
		strategyRepository.delete(strategy);

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("message", "Strategy deleted successfully");
		response.put("strategyId", strategyId);

		return ResponseEntity.ok(response);
	}

	/**
	 * 전략 상태 변경
	 */
	@PatchMapping("/{strategyId}/status")
	public ResponseEntity<StrategyResponse> updateStrategyStatus(
			@PathVariable String strategyId,
			@Valid @RequestBody StrategyStatusUpdateRequest request
	) {
		log.info("Update strategy status: strategyId={}, status={}", strategyId, request.getStatus());

		StrategyEntity strategy = strategyRepository.findById(strategyId)
				.orElseThrow(() -> new DomainException(ErrorCode.STRATEGY_001, "Strategy not found: " + strategyId));

		String newStatus = request.getStatus().toUpperCase();
		if ("ACTIVE".equals(newStatus)) {
			strategy.activate(strategy.getActiveVersionId());
		} else if ("INACTIVE".equals(newStatus)) {
			strategy.deactivate();
		} else {
			strategy.setStatus(newStatus);
		}

		StrategyEntity updated = strategyRepository.save(strategy);

		StrategyVersionEntity version = strategyVersionRepository
				.findById(updated.getActiveVersionId())
				.orElse(null);

		Map<String, Object> params = version != null ? fromJson(version.getParamsJson()) : new HashMap<>();

		return ResponseEntity.ok(toResponse(updated, params));
	}

	private StrategyResponse toResponse(StrategyEntity entity, Map<String, Object> params) {
		return StrategyResponse.builder()
				.strategyId(entity.getStrategyId())
				.name(entity.getName())
				.description(entity.getDescription())
				.status(entity.getStatus())
				.activeVersionId(entity.getActiveVersionId())
				.mode(entity.getMode())
				.params(params)
				.createdAt(entity.getCreatedAt())
				.updatedAt(entity.getUpdatedAt())
				// 자동매매 설정 필드
				.accountId(entity.getAccountId())
				.assetType(entity.getAssetType())
				.symbol(entity.getSymbol())
				.entryConditions(entity.getEntryConditions())
				.exitConditions(entity.getExitConditions())
				.stopLossType(entity.getStopLossType())
				.stopLossValue(entity.getStopLossValue())
				.takeProfitType(entity.getTakeProfitType())
				.takeProfitValue(entity.getTakeProfitValue())
				.positionSizeType(entity.getPositionSizeType())
				.positionSizeValue(entity.getPositionSizeValue())
				.maxPositions(entity.getMaxPositions())
				.build();
	}

	private String toJson(Map<String, Object> map) {
		try {
			return objectMapper.writeValueAsString(map);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to serialize params", e);
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> fromJson(String json) {
		try {
			return objectMapper.readValue(json, Map.class);
		} catch (JsonProcessingException e) {
			log.error("Failed to deserialize params: {}", json, e);
			return new HashMap<>();
		}
	}
}
