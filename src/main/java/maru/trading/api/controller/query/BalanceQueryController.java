package maru.trading.api.controller.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.broker.kis.api.KisApiException;
import maru.trading.broker.kis.api.KisBalanceApiClient;
import maru.trading.broker.kis.dto.KisBalanceResponse;
import maru.trading.infra.persistence.jpa.entity.AccountEntity;
import maru.trading.infra.persistence.jpa.entity.PositionEntity;
import maru.trading.infra.persistence.jpa.repository.AccountJpaRepository;
import maru.trading.infra.persistence.jpa.repository.PositionJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 계좌 잔액 조회 Query Controller
 *
 * Endpoints:
 * - GET /api/v1/query/balance?accountId={accountId} - Get balance by query param
 * - GET /api/v1/query/balance/{accountId} - Get balance by path variable
 * - GET /api/v1/query/balance/summary - Get balance summary
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/query/balance")
@RequiredArgsConstructor
public class BalanceQueryController {

    private final PositionJpaRepository positionRepository;
    private final AccountJpaRepository accountRepository;
    private final KisBalanceApiClient kisBalanceApiClient;

    /**
     * 전체 잔고 요약 조회
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getBalanceSummary() {
        log.info("Get balance summary");

        try {
            // 모든 포지션 조회
            List<PositionEntity> allPositions = positionRepository.findAll();

            // 총 주식 평가액 계산
            BigDecimal totalStockValue = allPositions.stream()
                    .filter(p -> p.getQty().compareTo(BigDecimal.ZERO) > 0)
                    .map(p -> p.getAvgPrice().multiply(p.getQty()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 총 실현 손익
            BigDecimal totalRealizedPnl = allPositions.stream()
                    .map(PositionEntity::getRealizedPnl)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 계좌 수
            long accountCount = allPositions.stream()
                    .map(PositionEntity::getAccountId)
                    .distinct()
                    .count();

            Map<String, Object> response = new HashMap<>();
            response.put("totalStockValue", totalStockValue);
            response.put("totalRealizedPnl", totalRealizedPnl);
            response.put("accountCount", accountCount);
            response.put("positionCount", allPositions.size());
            response.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get balance summary", e);
            throw new RuntimeException("Failed to get balance summary", e);
        }
    }

    /**
     * 계좌 잔액 조회 (Path Variable)
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<Map<String, Object>> getAccountBalanceByPath(
            @PathVariable String accountId
    ) {
        log.info("Get account balance by path: accountId={}", accountId);
        return getBalance(accountId);
    }

    /**
     * 계좌 잔액 조회 (Query Parameter)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAccountBalance(
            @RequestParam(required = true) String accountId
    ) {
        log.info("Get account balance: accountId={}", accountId);
        return getBalance(accountId);
    }

    /**
     * 잔액 조회 공통 로직
     */
    private ResponseEntity<Map<String, Object>> getBalance(String accountId) {
        try {
            // 해당 계좌의 포지션 조회
            List<PositionEntity> positions = positionRepository.findByAccountId(accountId);

            // 보유 중인 포지션만 필터링 (qty > 0)
            List<PositionEntity> activePositions = positions.stream()
                    .filter(p -> p.getQty().compareTo(BigDecimal.ZERO) > 0)
                    .toList();

            // 주식 평가액 계산 (포지션 평균가 * 수량의 합계)
            BigDecimal stockValue = activePositions.stream()
                    .map(p -> p.getAvgPrice().multiply(p.getQty()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 실현 손익 합계
            BigDecimal totalRealizedPnl = positions.stream()
                    .map(PositionEntity::getRealizedPnl)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 미실현 손익 계산 (임시로 0으로 설정 - 실제로는 현재가 필요)
            BigDecimal totalUnrealizedPnl = BigDecimal.ZERO;

            // KIS API를 통한 실제 현금 잔고 조회
            BigDecimal cashBalance = fetchCashBalanceFromKis(accountId);

            // 총 자산 = 현금 + 주식 평가액
            BigDecimal totalAssets = cashBalance.add(stockValue);

            // 총 손익 = 실현 손익 + 미실현 손익
            BigDecimal totalProfitLoss = totalRealizedPnl.add(totalUnrealizedPnl);

            Map<String, Object> response = new HashMap<>();
            response.put("accountId", accountId);
            response.put("totalAssets", totalAssets);
            response.put("cashBalance", cashBalance);
            response.put("stockValue", stockValue);
            response.put("totalProfitLoss", totalProfitLoss);
            response.put("realizedPnl", totalRealizedPnl);
            response.put("unrealizedPnl", totalUnrealizedPnl);

            log.info("Account balance calculated: totalAssets={}, cashBalance={}, stockValue={}",
                    totalAssets, cashBalance, stockValue);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get account balance for accountId: {}", accountId, e);
            throw new RuntimeException("Failed to calculate account balance", e);
        }
    }

    /**
     * KIS API를 통해 실제 현금 잔고 조회
     *
     * @param accountId 계좌 ID
     * @return 현금 잔고 (조회 실패 시 0원 반환)
     */
    private BigDecimal fetchCashBalanceFromKis(String accountId) {
        try {
            // 계좌 정보 조회
            Optional<AccountEntity> accountOpt = accountRepository.findByIdAndNotDeleted(accountId);

            if (accountOpt.isEmpty()) {
                log.warn("[Balance] Account not found or deleted: accountId={}", accountId);
                return BigDecimal.ZERO; // 에러 시 0원
            }

            AccountEntity account = accountOpt.get();

            // KIS API 호출 - PAPER와 LIVE 모두 실제 API 호출
            log.info("[Balance] Calling KIS API: accountId={}, environment={}, cano={}, acntPrdtCd={}",
                    accountId, account.getEnvironment(), account.getCano(), account.getAcntPrdtCd());

            KisBalanceResponse balanceResponse = kisBalanceApiClient.getBalance(
                    account.getCano(),
                    account.getAcntPrdtCd(),
                    account.getEnvironment()
            );

            if (balanceResponse != null && balanceResponse.isSuccess()) {
                BigDecimal cashBalance = balanceResponse.getCashBalance();
                log.info("[Balance] KIS API success: accountId={}, cashBalance={}", accountId, cashBalance);
                return cashBalance;
            } else {
                log.warn("[Balance] KIS API returned failure response: accountId={}, rtCd={}, msg={}",
                        accountId,
                        balanceResponse != null ? balanceResponse.getRtCd() : "null",
                        balanceResponse != null ? balanceResponse.getMsg1() : "null");
                return BigDecimal.ZERO; // 에러 시 0원
            }

        } catch (KisApiException e) {
            log.error("[Balance] KIS API error: accountId={}, errorType={}, message={}",
                    accountId, e.getErrorType(), e.getMessage());
            return BigDecimal.ZERO; // 에러 시 0원
        } catch (Exception e) {
            log.error("[Balance] Unexpected error while fetching KIS balance: accountId={}", accountId, e);
            return BigDecimal.ZERO; // 에러 시 0원
        }
    }
}
