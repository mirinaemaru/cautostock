package maru.trading.api.controller.demo;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.api.dto.request.DemoSignalRequest;
import maru.trading.api.dto.response.AckResponse;
import maru.trading.application.orchestration.TradingWorkflow;
import maru.trading.domain.signal.Signal;
import maru.trading.domain.signal.SignalType;
import maru.trading.infra.config.UlidGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo Signal Controller
 *
 * 테스트용 수동 신호 주입 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/demo")
@RequiredArgsConstructor
public class DemoSignalController {

	private final TradingWorkflow tradingWorkflow;

	/**
	 * 데모 신호 주입
	 */
	@PostMapping("/signal")
	public ResponseEntity<AckResponse> injectSignal(
			@Valid @RequestBody DemoSignalRequest request
	) {
		log.info("Demo signal injection: symbol={}, side={}, qty={}",
				request.getSymbol(), request.getSide(), request.getTargetValue());

		// 신호 생성
		Signal signal = Signal.builder()
				.signalId(UlidGenerator.generate())
				.strategyId("demo-strategy")
				.strategyVersionId("demo-v1")
				.accountId(request.getAccountId())
				.symbol(request.getSymbol())
				.signalType(SignalType.valueOf(request.getSide().name()))
				.targetType(request.getTargetType() != null ? request.getTargetType() : "QTY")
				.targetValue(request.getTargetValue())
				.ttlSeconds(request.getTtlSeconds() != null ? request.getTtlSeconds() : 60)
				.build();

		// Trading Workflow 실행
		try {
			tradingWorkflow.processSignal(signal);

			return ResponseEntity.accepted()
					.body(AckResponse.success("Signal processed successfully"));

		} catch (Exception e) {
			log.error("Failed to process demo signal", e);
			return ResponseEntity.internalServerError()
					.body(AckResponse.builder()
							.ok(false)
							.message("Failed to process signal: " + e.getMessage())
							.build());
		}
	}
}
