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
 * - GET  /api/v1/admin/risk-rules - List all risk rules
 * - POST /api/v1/admin/risk-rules - Create a new risk rule
 * - GET  /api/v1/admin/risk-rules/{ruleId} - Get a specific risk rule
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
     * List all risk rules.
     *
     * @param page Page number
     * @param size Page size
     * @return List of all risk rules
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listAllRules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Listing all risk rules, page: {}, size: {}", page, size);

        List<RiskRule> allRules = riskRuleRepository.findAll();

        // Simple pagination
        int start = page * size;
        int end = Math.min(start + size, allRules.size());
        List<Map<String, Object>> pageContent = start < allRules.size()
                ? allRules.subList(start, end).stream().map(this::toRuleResponse).collect(Collectors.toList())
                : new java.util.ArrayList<>();

        return ResponseEntity.ok(Map.of(
                "content", pageContent,
                "totalElements", allRules.size(),
                "totalPages", (allRules.size() + size - 1) / size,
                "page", page,
                "size", size
        ));
    }

    /**
     * Create a new risk rule.
     *
     * @param request Risk rule creation request
     * @return Created risk rule
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createRule(@Valid @RequestBody UpdateRiskRuleRequest request) {

        log.info("Creating new risk rule");

        // Default to global rule if no scope specified
        RiskRule rule = updateRiskRuleUseCase.updateGlobalRule(
                request.getMaxPositionValuePerSymbol(),
                request.getMaxOpenOrders(),
                request.getMaxOrdersPerMinute(),
                request.getDailyLossLimit(),
                request.getConsecutiveOrderFailuresLimit()
        );

        log.info("Risk rule created: ruleId={}", rule.getRiskRuleId());

        return ResponseEntity.ok(toRuleResponse(rule));
    }

    /**
     * Get a specific risk rule by ID.
     *
     * @param ruleId Risk rule identifier
     * @return Risk rule details
     */
    @GetMapping("/{ruleId}")
    public ResponseEntity<Map<String, Object>> getRule(@PathVariable String ruleId) {

        log.info("Fetching risk rule: {}", ruleId);

        return riskRuleRepository.findById(ruleId)
                .map(rule -> ResponseEntity.ok(toRuleResponse(rule)))
                .orElse(ResponseEntity.notFound().build());
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
