package maru.trading.api.controller.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.infra.persistence.jpa.entity.PositionEntity;
import maru.trading.infra.persistence.jpa.repository.PositionJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 계좌 잔액 조회 Query Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/query/balance")
@RequiredArgsConstructor
public class BalanceQueryController {

    private final PositionJpaRepository positionRepository;

    /**
     * 계좌 잔액 조회
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAccountBalance(
            @RequestParam(required = true) String accountId
    ) {
        log.info("Get account balance: accountId={}", accountId);

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

            // 현금 잔고 (임시로 1,000,000원으로 설정 - 실제로는 KIS API 호출 필요)
            BigDecimal cashBalance = new BigDecimal("1000000");

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
}
