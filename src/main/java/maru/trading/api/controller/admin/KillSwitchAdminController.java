package maru.trading.api.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.api.dto.request.KillSwitchToggleRequest;
import maru.trading.api.dto.response.KillSwitchResponse;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.RiskStateEntity;
import maru.trading.infra.persistence.jpa.repository.RiskStateJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Kill Switch 관리 Admin Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/kill-switch")
@RequiredArgsConstructor
public class KillSwitchAdminController {

	private final RiskStateJpaRepository riskStateRepository;

	/**
	 * Kill Switch 상태 조회
	 */
	@GetMapping
	public ResponseEntity<KillSwitchResponse> getKillSwitchState(
			@RequestParam(required = false) String accountId
	) {
		RiskStateEntity riskState;

		if (accountId != null) {
			riskState = riskStateRepository.findByScopeAndAccountId("ACCOUNT", accountId)
					.orElse(null);
		} else {
			riskState = riskStateRepository.findByScope("GLOBAL")
					.orElse(null);
		}

		if (riskState == null) {
			// 없으면 기본값 반환
			return ResponseEntity.ok(KillSwitchResponse.builder()
					.accountId(accountId)
					.status(maru.trading.domain.risk.KillSwitchStatus.OFF)
					.reason(null)
					.updatedAt(java.time.LocalDateTime.now())
					.build());
		}

		return ResponseEntity.ok(toResponse(riskState));
	}

	/**
	 * Kill Switch 토글
	 */
	@PostMapping
	public ResponseEntity<KillSwitchResponse> toggleKillSwitch(
			@Valid @RequestBody KillSwitchToggleRequest request
	) {
		log.info("Toggle kill switch: accountId={}, status={}, reason={}",
				request.getAccountId(), request.getStatus(), request.getReason());

		String scope = request.getAccountId() != null ? "ACCOUNT" : "GLOBAL";
		String accountId = request.getAccountId();

		RiskStateEntity riskState = riskStateRepository
				.findByScopeAndAccountId(scope, accountId)
				.orElseGet(() -> {
					// 없으면 생성
					return RiskStateEntity.builder()
							.riskStateId(UlidGenerator.generate())
							.scope(scope)
							.accountId(accountId)
							.killSwitchStatus(maru.trading.domain.risk.KillSwitchStatus.OFF)
							.build();
				});

		riskState.toggleKillSwitch(request.getStatus(), request.getReason());
		RiskStateEntity saved = riskStateRepository.save(riskState);

		return ResponseEntity.ok(toResponse(saved));
	}

	private KillSwitchResponse toResponse(RiskStateEntity entity) {
		return KillSwitchResponse.builder()
				.accountId(entity.getAccountId())
				.status(entity.getKillSwitchStatus())
				.reason(entity.getKillSwitchReason())
				.updatedAt(entity.getUpdatedAt())
				.build();
	}
}
