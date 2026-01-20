package maru.trading.api.controller.demo;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.api.dto.request.DemoSignalRequest;
import maru.trading.api.dto.response.DemoSignalResponse;
import maru.trading.application.orchestration.SignalProcessingResult;
import maru.trading.application.orchestration.TradingWorkflow;
import maru.trading.application.ports.repo.SignalRepository;
import maru.trading.domain.signal.Signal;
import maru.trading.domain.signal.SignalType;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.AccountEntity;
import maru.trading.infra.persistence.jpa.entity.StrategyEntity;
import maru.trading.infra.persistence.jpa.repository.AccountJpaRepository;
import maru.trading.infra.persistence.jpa.repository.StrategyJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Demo Signal Controller
 *
 * 테스트용 수동 신호 주입 API
 * - 시그널을 DB에 저장하고 Trading Workflow 실행
 * - 시그널 조회 기능 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/demo")
@RequiredArgsConstructor
public class DemoSignalController {

	private final TradingWorkflow tradingWorkflow;
	private final SignalRepository signalRepository;
	private final AccountJpaRepository accountRepository;
	private final StrategyJpaRepository strategyRepository;

	/**
	 * 데모 신호 주입
	 * 시그널을 DB에 저장한 후 Trading Workflow 실행
	 *
	 * Note: @Transactional 제거됨 - 각 작업(signalRepository.save, tradingWorkflow.processSignal)이
	 * 자체 트랜잭션을 사용하므로 Risk 거부 시 예외가 정상적으로 catch됨
	 */
	@PostMapping("/signal")
	public ResponseEntity<DemoSignalResponse> injectSignal(
			@Valid @RequestBody DemoSignalRequest request
	) {
		log.info("Demo signal injection: symbol={}, side={}, qty={}",
				request.getSymbol(), request.getSide(), request.getTargetValue());

		// 1. Account ID 확인 (없으면 기본 ACTIVE 계정 사용)
		String accountId = resolveAccountId(request.getAccountId());
		if (accountId == null) {
			log.error("No valid account found for demo signal");
			return ResponseEntity.badRequest()
					.body(DemoSignalResponse.builder()
							.ok(false)
							.message("No valid account found. Please create an ACTIVE account first.")
							.build());
		}

		// 2. 전략 확인 (기존 ACTIVE 전략 사용)
		StrategyEntity strategy = resolveStrategy();
		if (strategy == null) {
			log.error("No active strategy found for demo signal");
			return ResponseEntity.badRequest()
					.body(DemoSignalResponse.builder()
							.ok(false)
							.message("No active strategy found. Please create an ACTIVE strategy first.")
							.build());
		}

		// 3. 신호 생성
		String signalId = UlidGenerator.generate();
		Signal signal = Signal.builder()
				.signalId(signalId)
				.strategyId(strategy.getStrategyId())
				.strategyVersionId(strategy.getActiveVersionId())
				.accountId(accountId)
				.symbol(request.getSymbol())
				.signalType(SignalType.valueOf(request.getSide().name()))
				.targetType(request.getTargetType() != null ? request.getTargetType() : "QTY")
				.targetValue(request.getTargetValue())
				.ttlSeconds(request.getTtlSeconds() != null ? request.getTtlSeconds() : 120)
				.reason("Demo signal injection via API")
				.build();

		// 4. 시그널 DB 저장
		Signal savedSignal = signalRepository.save(signal);
		log.info("Demo signal saved: signalId={}, symbol={}, type={}",
				savedSignal.getSignalId(), savedSignal.getSymbol(), savedSignal.getSignalType());

		// 5. Trading Workflow 실행
		SignalProcessingResult result = tradingWorkflow.processSignal(savedSignal);
		log.info("Demo signal processing result: signalId={}, status={}, orderId={}",
				signalId, result.getStatus(), result.getOrderId());

		// 6. 결과에 따른 응답 상태 매핑
		String orderStatus = mapResultStatus(result);
		boolean ok = result.isSuccess();
		String message = buildResultMessage(result);

		// 7. 응답 생성
		return ResponseEntity.accepted()
				.body(DemoSignalResponse.builder()
						.ok(ok)
						.message(message)
						.signalId(signalId)
						.accountId(accountId)
						.symbol(request.getSymbol())
						.signalType(request.getSide().name())
						.targetValue(request.getTargetValue())
						.orderStatus(orderStatus)
						.orderId(result.getOrderId())
						.build());
	}

	/**
	 * 시그널 조회
	 */
	@GetMapping("/signals")
	public ResponseEntity<Map<String, Object>> getSignals(
			@RequestParam(required = false) String accountId,
			@RequestParam(defaultValue = "20") int limit
	) {
		// accountId 확인
		String resolvedAccountId = resolveAccountId(accountId);
		if (resolvedAccountId == null) {
			return ResponseEntity.ok(Map.of(
					"items", List.of(),
					"message", "No valid account found"
			));
		}

		List<Signal> signals = signalRepository.findByAccountIdOrderByCreatedAtDesc(resolvedAccountId, limit);

		List<Map<String, Object>> items = signals.stream()
				.map(s -> Map.<String, Object>of(
						"signalId", s.getSignalId(),
						"strategyId", s.getStrategyId(),
						"symbol", s.getSymbol(),
						"signalType", s.getSignalType().name(),
						"targetValue", s.getTargetValue(),
						"reason", s.getReason() != null ? s.getReason() : ""
				))
				.collect(Collectors.toList());

		return ResponseEntity.ok(Map.of(
				"items", items,
				"count", items.size(),
				"accountId", resolvedAccountId
		));
	}

	/**
	 * Account ID 확인 및 기본값 설정
	 */
	private String resolveAccountId(String requestAccountId) {
		// 1. 요청에 accountId가 있으면 유효성 확인
		if (requestAccountId != null && !requestAccountId.isBlank()) {
			boolean exists = accountRepository.existsById(requestAccountId);
			if (exists) {
				return requestAccountId;
			}
			log.warn("Requested accountId not found: {}", requestAccountId);
		}

		// 2. 기본 ACTIVE 계정 찾기
		return accountRepository.findFirstActiveAccount()
				.map(AccountEntity::getAccountId)
				.orElse(null);
	}

	/**
	 * 기존 ACTIVE 전략 조회
	 */
	private StrategyEntity resolveStrategy() {
		List<StrategyEntity> activeStrategies = strategyRepository.findByStatusAndDelyn("ACTIVE", "N");
		if (activeStrategies.isEmpty()) {
			return null;
		}
		return activeStrategies.get(0);
	}

	/**
	 * 처리 결과를 orderStatus 문자열로 매핑
	 */
	private String mapResultStatus(SignalProcessingResult result) {
		return switch (result.getStatus()) {
			case SUCCESS -> "SENT";
			case REJECTED -> "REJECTED";
			case SKIPPED -> "SKIPPED";
			case FAILED -> "FAILED";
		};
	}

	/**
	 * 처리 결과에 따른 메시지 생성
	 */
	private String buildResultMessage(SignalProcessingResult result) {
		return switch (result.getStatus()) {
			case SUCCESS -> "Signal processed and order sent successfully";
			case REJECTED -> "Signal processed but order rejected: " + result.getMessage();
			case SKIPPED -> "Signal skipped: " + result.getMessage();
			case FAILED -> "Signal processing failed: " + result.getMessage();
		};
	}
}
