package maru.trading.api.controller.admin;

import jakarta.validation.Valid;
import maru.trading.api.dto.request.UpdateRiskRuleRequest;
import maru.trading.api.dto.response.AckResponse;
import maru.trading.application.usecase.risk.UpdateRiskRuleUseCase;
import maru.trading.domain.risk.RiskRule;
import maru.trading.application.ports.repo.RiskRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin API for managing risk rules.
 *
 * Endpoints:
 * - POST /api/v1/admin/risk-rules/global - Update global risk rule
 * - POST /api/v1/admin/risk-rules/account/{accountId} - Update account-specific rule
 * - POST /api/v1/admin/risk-rules/account/{accountId}/symbol/{symbol} - Update symbol-specific rule
 * - GET /api/v1/admin/risk-rules/account/{accountId} - Get all rules for an account
 * - DELETE /api/v1/admin/risk-rules/{ruleId} - Delete a risk rule
 */
@RestController
@RequestMapping("/api/v1/admin/risk-rules")
public class RiskRuleAdminController {

    private static final Logger log = LoggerFactory.getLogger(RiskRuleAdminController.class);

    private final UpdateRiskRuleUseCase updateRiskRuleUseCase;
    private final RiskRuleRepository riskRuleRepository;

    public RiskRuleAdminController(
            UpdateRiskRuleUseCase updateRiskRuleUseCase,
            RiskRuleRepository riskRuleRepository) {
        this.updateRiskRuleUseCase = updateRiskRuleUseCase;
        this.riskRuleRepository = riskRuleRepository;
    }

    /**
     * Update global risk rule (system-wide defaults).
     *
     * @param request Risk rule parameters
     * @return Updated risk rule details
     */
    @PostMapping("/global")
    public ResponseEntity<Map<String, Object>> updateGlobalRule(
            @Valid @RequestBody UpdateRiskRuleRequest request) {

        log.info("Received request to update global risk rule");

        RiskRule rule = updateRiskRuleUseCase.updateGlobalRule(
                request.getMaxPositionValuePerSymbol(),
                request.getMaxOpenOrders(),
                request.getMaxOrdersPerMinute(),
                request.getDailyLossLimit(),
                request.getConsecutiveOrderFailuresLimit()
        );

        log.info("Global risk rule updated: ruleId={}", rule.getRiskRuleId());

        return ResponseEntity.ok(toRuleResponse(rule));
    }

    /**
     * Update account-specific risk rule (overrides global).
     *
     * @param accountId Account identifier
     * @param request Risk rule parameters
     * @return Updated risk rule details
     */
    @PostMapping("/account/{accountId}")
    public ResponseEntity<Map<String, Object>> updateAccountRule(
            @PathVariable String accountId,
            @Valid @RequestBody UpdateRiskRuleRequest request) {

        log.info("Received request to update risk rule for account: {}", accountId);

        RiskRule rule = updateRiskRuleUseCase.updateAccountRule(
                accountId,
                request.getMaxPositionValuePerSymbol(),
                request.getMaxOpenOrders(),
                request.getMaxOrdersPerMinute(),
                request.getDailyLossLimit(),
                request.getConsecutiveOrderFailuresLimit()
        );

        log.info("Account risk rule updated: ruleId={}, accountId={}", rule.getRiskRuleId(), accountId);

        return ResponseEntity.ok(toRuleResponse(rule));
    }

    /**
     * Update symbol-specific risk rule (overrides account and global).
     *
     * @param accountId Account identifier
     * @param symbol Symbol (e.g., "005930")
     * @param request Risk rule parameters
     * @return Updated risk rule details
     */
    @PostMapping("/account/{accountId}/symbol/{symbol}")
    public ResponseEntity<Map<String, Object>> updateSymbolRule(
            @PathVariable String accountId,
            @PathVariable String symbol,
            @Valid @RequestBody UpdateRiskRuleRequest request) {

        log.info("Received request to update risk rule for account: {}, symbol: {}", accountId, symbol);

        RiskRule rule = updateRiskRuleUseCase.updateSymbolRule(
                accountId,
                symbol,
                request.getMaxPositionValuePerSymbol(),
                request.getMaxOpenOrders(),
                request.getMaxOrdersPerMinute(),
                request.getDailyLossLimit(),
                request.getConsecutiveOrderFailuresLimit()
        );

        log.info("Symbol risk rule updated: ruleId={}, accountId={}, symbol={}",
                rule.getRiskRuleId(), accountId, symbol);

        return ResponseEntity.ok(toRuleResponse(rule));
    }

    /**
     * Get all risk rules for an account (includes global, account-specific, and symbol-specific).
     *
     * @param accountId Account identifier
     * @return List of applicable risk rules
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<Map<String, Object>> getRulesForAccount(@PathVariable String accountId) {

        log.info("Fetching risk rules for account: {}", accountId);

        List<RiskRule> rules = riskRuleRepository.findRulesForAccount(accountId);

        List<Map<String, Object>> ruleResponses = rules.stream()
                .map(this::toRuleResponse)
                .collect(Collectors.toList());

        log.info("Found {} risk rules for account: {}", rules.size(), accountId);

        return ResponseEntity.ok(Map.of(
                "accountId", accountId,
                "rules", ruleResponses,
                "count", ruleResponses.size()
        ));
    }

    /**
     * Delete a risk rule by ID.
     *
     * @param ruleId Risk rule identifier
     * @return Acknowledgment response
     */
    @DeleteMapping("/{ruleId}")
    public ResponseEntity<AckResponse> deleteRule(@PathVariable String ruleId) {

        log.info("Received request to delete risk rule: {}", ruleId);

        updateRiskRuleUseCase.deleteRule(ruleId);

        log.info("Risk rule deleted: {}", ruleId);

        return ResponseEntity.ok(AckResponse.success("Risk rule deleted successfully"));
    }

    /**
     * Convert RiskRule domain object to response map.
     */
    private Map<String, Object> toRuleResponse(RiskRule rule) {
        return Map.of(
                "riskRuleId", rule.getRiskRuleId(),
                "scope", rule.getScope().name(),
                "accountId", rule.getAccountId() != null ? rule.getAccountId() : "",
                "symbol", rule.getSymbol() != null ? rule.getSymbol() : "",
                "maxPositionValuePerSymbol", rule.getMaxPositionValuePerSymbol() != null
                        ? rule.getMaxPositionValuePerSymbol() : "",
                "maxOpenOrders", rule.getMaxOpenOrders() != null ? rule.getMaxOpenOrders() : "",
                "maxOrdersPerMinute", rule.getMaxOrdersPerMinute() != null
                        ? rule.getMaxOrdersPerMinute() : "",
                "dailyLossLimit", rule.getDailyLossLimit() != null ? rule.getDailyLossLimit() : "",
                "consecutiveOrderFailuresLimit", rule.getConsecutiveOrderFailuresLimit() != null
                        ? rule.getConsecutiveOrderFailuresLimit() : ""
        );
    }
}
