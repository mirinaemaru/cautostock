package maru.trading.api.controller.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.api.dto.request.StrategyCreateRequest;
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

		// Strategy Version 생성
		StrategyVersionEntity version = StrategyVersionEntity.builder()
				.strategyVersionId(versionId)
				.strategyId(strategyId)
				.versionNo(1)
				.paramsJson(toJson(request.getParams()))
				.build();

		strategyVersionRepository.save(version);

		// Strategy 생성
		StrategyEntity strategy = StrategyEntity.builder()
				.strategyId(strategyId)
				.name(request.getName())
				.description(request.getDescription())
				.status("INACTIVE")
				.mode(request.getMode())
				.activeVersionId(versionId)
				.build();

		StrategyEntity saved = strategyRepository.save(strategy);

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
