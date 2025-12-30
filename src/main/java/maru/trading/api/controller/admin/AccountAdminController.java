package maru.trading.api.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.api.dto.request.AccountRegisterRequest;
import maru.trading.api.dto.response.AccountResponse;
import maru.trading.domain.account.AccountNotFoundException;
import maru.trading.domain.account.AccountStatus;
import maru.trading.domain.shared.Environment;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.AccountEntity;
import maru.trading.infra.persistence.jpa.repository.AccountJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 계좌 관리 Admin Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/accounts")
@RequiredArgsConstructor
public class AccountAdminController {

	private final AccountJpaRepository accountRepository;

	/**
	 * 계좌 등록
	 */
	@PostMapping
	public ResponseEntity<AccountResponse> registerAccount(
			@Valid @RequestBody AccountRegisterRequest request
	) {
		log.info("Register account: broker={}, env={}, cano={}",
				request.getBroker(), request.getEnvironment(), request.getCano());

		// 중복 체크
		accountRepository.findByBrokerAndEnvironmentAndCanoAndAcntPrdtCdAndDelyn(
				request.getBroker(),
				request.getEnvironment(),
				request.getCano(),
				request.getAcntPrdtCd(),
				"N"
		).ifPresent(existing -> {
			throw new IllegalStateException("Account already exists: " + existing.getAccountId());
		});

		// 계좌 생성
		AccountEntity account = AccountEntity.builder()
				.accountId(UlidGenerator.generate())
				.broker(request.getBroker())
				.environment(request.getEnvironment())
				.cano(request.getCano())
				.acntPrdtCd(request.getAcntPrdtCd())
				.status(AccountStatus.ACTIVE)
				.alias(request.getAlias())
				.build();

		AccountEntity saved = accountRepository.save(account);

		AccountResponse response = toResponse(saved);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * 계좌 목록 조회
	 */
	@GetMapping
	public ResponseEntity<Map<String, List<AccountResponse>>> listAccounts() {
		List<AccountEntity> accounts = accountRepository.findByDelyn("N");

		List<AccountResponse> items = accounts.stream()
				.map(this::toResponse)
				.collect(Collectors.toList());

		Map<String, List<AccountResponse>> response = new HashMap<>();
		response.put("items", items);

		return ResponseEntity.ok(response);
	}

	/**
	 * 계좌 조회
	 */
	@GetMapping("/{accountId}")
	public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountId) {
		AccountEntity account = accountRepository.findByIdAndNotDeleted(accountId)
				.orElseThrow(() -> new AccountNotFoundException(accountId));

		return ResponseEntity.ok(toResponse(account));
	}

	/**
	 * 계좌 상태 변경
	 */
	@PutMapping("/{accountId}/status")
	public ResponseEntity<AccountResponse> updateAccountStatus(
			@PathVariable String accountId,
			@Valid @RequestBody Map<String, String> request
	) {
		AccountEntity account = accountRepository.findByIdAndNotDeleted(accountId)
				.orElseThrow(() -> new AccountNotFoundException(accountId));

		String statusStr = request.get("status");
		AccountStatus newStatus = AccountStatus.valueOf(statusStr);

		account.updateStatus(newStatus);
		AccountEntity updated = accountRepository.save(account);

		return ResponseEntity.ok(toResponse(updated));
	}

	/**
	 * 계좌 수정
	 */
	@PutMapping("/{accountId}")
	public ResponseEntity<AccountResponse> updateAccount(
			@PathVariable String accountId,
			@Valid @RequestBody Map<String, String> request
	) {
		AccountEntity account = accountRepository.findByIdAndNotDeleted(accountId)
				.orElseThrow(() -> new AccountNotFoundException(accountId));

		if (request.containsKey("alias")) {
			account.updateAlias(request.get("alias"));
		}
		if (request.containsKey("environment")) {
			// Environment 변경은 보통 허용하지 않지만 요청이 있으면 처리
		}

		AccountEntity updated = accountRepository.save(account);
		return ResponseEntity.ok(toResponse(updated));
	}

	/**
	 * 계좌 삭제 (소프트 삭제)
	 */
	@DeleteMapping("/{accountId}")
	public ResponseEntity<Void> deleteAccount(@PathVariable String accountId) {
		log.info("Soft delete account: accountId={}", accountId);

		AccountEntity account = accountRepository.findByIdAndNotDeleted(accountId)
				.orElseThrow(() -> new AccountNotFoundException(accountId));

		account.softDelete();
		accountRepository.save(account);

		return ResponseEntity.noContent().build();
	}

	private AccountResponse toResponse(AccountEntity entity) {
		return AccountResponse.builder()
				.accountId(entity.getAccountId())
				.broker(entity.getBroker())
				.environment(entity.getEnvironment())
				.cano(entity.getCano())
				.acntPrdtCd(entity.getAcntPrdtCd())
				.status(entity.getStatus())
				.alias(entity.getAlias())
				.delyn(entity.getDelyn())
				.createdAt(entity.getCreatedAt())
				.updatedAt(entity.getUpdatedAt())
				.build();
	}
}
