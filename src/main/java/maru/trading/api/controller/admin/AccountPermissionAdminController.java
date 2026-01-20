package maru.trading.api.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.api.dto.request.AccountPermissionUpdateRequest;
import maru.trading.api.dto.response.AccountPermissionResponse;
import maru.trading.domain.account.AccountNotFoundException;
import maru.trading.infra.persistence.jpa.entity.AccountEntity;
import maru.trading.infra.persistence.jpa.entity.AccountPermissionEntity;
import maru.trading.infra.persistence.jpa.repository.AccountJpaRepository;
import maru.trading.infra.persistence.jpa.repository.AccountPermissionJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 계좌 권한 관리 Admin Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/accounts")
@RequiredArgsConstructor
public class AccountPermissionAdminController {

    private final AccountJpaRepository accountRepository;
    private final AccountPermissionJpaRepository permissionRepository;

    /**
     * 계좌 권한 조회
     */
    @GetMapping("/{accountId}/permissions")
    public ResponseEntity<AccountPermissionResponse> getPermissions(
            @PathVariable String accountId
    ) {
        log.info("Get permissions for account: {}", accountId);

        // 계좌 존재 여부 확인
        accountRepository.findByIdAndNotDeleted(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        // 권한 조회 (없으면 기본값 반환)
        AccountPermissionEntity permission = permissionRepository.findById(accountId)
                .orElse(AccountPermissionEntity.createDefault(accountId));

        AccountPermissionResponse response = toResponse(permission);
        return ResponseEntity.ok(response);
    }

    /**
     * 계좌 권한 업데이트
     */
    @PutMapping("/{accountId}/permissions")
    public ResponseEntity<AccountPermissionResponse> updatePermissions(
            @PathVariable String accountId,
            @Valid @RequestBody AccountPermissionUpdateRequest request
    ) {
        log.info("Update permissions for account: {}", accountId);

        // 계좌 존재 여부 확인
        accountRepository.findByIdAndNotDeleted(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        // 권한 조회 또는 생성
        AccountPermissionEntity permission = permissionRepository.findById(accountId)
                .orElse(AccountPermissionEntity.builder()
                        .accountId(accountId)
                        .build());

        // 권한 업데이트
        permission.updatePermissions(
                request.getTradeBuy(),
                request.getTradeSell(),
                request.getAutoTrade(),
                request.getManualTrade(),
                request.getPaperOnly()
        );

        // 저장
        AccountPermissionEntity saved = permissionRepository.save(permission);

        AccountPermissionResponse response = toResponse(saved);
        return ResponseEntity.ok(response);
    }

    private AccountPermissionResponse toResponse(AccountPermissionEntity entity) {
        return AccountPermissionResponse.builder()
                .accountId(entity.getAccountId())
                .tradeBuy(entity.getTradeBuy())
                .tradeSell(entity.getTradeSell())
                .autoTrade(entity.getAutoTrade())
                .manualTrade(entity.getManualTrade())
                .paperOnly(entity.getPaperOnly())
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt() : LocalDateTime.now())
                .build();
    }
}
