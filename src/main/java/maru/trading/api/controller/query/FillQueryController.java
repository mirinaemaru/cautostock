package maru.trading.api.controller.query;

import maru.trading.api.dto.response.FillListResponse;
import maru.trading.api.dto.response.FillResponse;
import maru.trading.application.ports.repo.FillRepository;
import maru.trading.domain.execution.Fill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query controller for fill data.
 * Provides endpoints to query fill history.
 */
@RestController
@RequestMapping("/api/v1/query/fills")
public class FillQueryController {

    private static final Logger log = LoggerFactory.getLogger(FillQueryController.class);

    private final FillRepository fillRepository;

    public FillQueryController(FillRepository fillRepository) {
        this.fillRepository = fillRepository;
    }

    /**
     * Query fills with optional filters.
     *
     * @param accountId Account ID (optional)
     * @param orderId Order ID (optional)
     * @param symbol Symbol (optional)
     * @param from Start time (optional)
     * @param to End time (optional)
     * @return List of fills matching criteria
     */
    @GetMapping
    public ResponseEntity<FillListResponse> queryFills(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        log.info("Query fills: accountId={}, orderId={}, symbol={}, from={}, to={}",
                accountId, orderId, symbol, from, to);

        List<Fill> fills;

        // Priority: orderId > (accountId + symbol) > accountId only
        if (orderId != null && !orderId.isBlank()) {
            // Query by order ID
            fills = fillRepository.findByOrderId(orderId);

        } else if (accountId != null && !accountId.isBlank() && symbol != null && !symbol.isBlank()) {
            // Query by account and symbol with time range
            LocalDateTime fromTime = from != null ? from : LocalDateTime.now().minusMonths(1);
            LocalDateTime toTime = to != null ? to : LocalDateTime.now();
            fills = fillRepository.findByAccountAndSymbol(accountId, symbol, fromTime, toTime);

        } else if (accountId != null && !accountId.isBlank()) {
            // Query by account only with time range
            LocalDateTime fromTime = from != null ? from : LocalDateTime.now().minusMonths(1);
            LocalDateTime toTime = to != null ? to : LocalDateTime.now();
            fills = fillRepository.findByAccount(accountId, fromTime, toTime);

        } else {
            // No filters provided - return all fills (limited)
            log.info("No filters provided for fill query, returning all fills");
            fills = fillRepository.findAll();
        }

        log.info("Found {} fills", fills.size());

        // Map to response DTOs
        List<FillResponse> responses = fills.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new FillListResponse(responses));
    }

    /**
     * Get fill statistics.
     */
    @GetMapping("/statistics")
    public ResponseEntity<java.util.Map<String, Object>> getFillStatistics(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        log.info("Get fill statistics: accountId={}", accountId);

        LocalDateTime fromTime = from != null ? from : LocalDateTime.now().minusMonths(1);
        LocalDateTime toTime = to != null ? to : LocalDateTime.now();

        List<Fill> fills;
        if (accountId != null && !accountId.isBlank()) {
            fills = fillRepository.findByAccount(accountId, fromTime, toTime);
        } else {
            fills = fillRepository.findAll();
        }

        // Calculate statistics
        int totalFills = fills.size();
        java.math.BigDecimal totalVolume = fills.stream()
                .map(f -> f.getFillPrice().multiply(java.math.BigDecimal.valueOf(f.getFillQty())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal totalFees = fills.stream()
                .map(f -> f.getFee().add(f.getTax()))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        long buyCount = fills.stream()
                .filter(f -> "BUY".equals(f.getSide().name()))
                .count();
        long sellCount = totalFills - buyCount;

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("accountId", accountId);
        stats.put("fromDate", fromTime.toLocalDate());
        stats.put("toDate", toTime.toLocalDate());
        stats.put("totalFills", totalFills);
        stats.put("totalVolume", totalVolume);
        stats.put("totalFees", totalFees);
        stats.put("buyCount", buyCount);
        stats.put("sellCount", sellCount);
        stats.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(stats);
    }

    /**
     * Get a specific fill by ID.
     *
     * @param fillId Fill ID
     * @return Fill details
     */
    @GetMapping("/{fillId}")
    public ResponseEntity<FillResponse> getFillById(@PathVariable String fillId) {
        log.info("Get fill by ID: {}", fillId);

        return fillRepository.findById(fillId)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Map Fill domain model to FillResponse DTO.
     */
    private FillResponse toResponse(Fill fill) {
        return new FillResponse(
                fill.getFillId(),
                fill.getOrderId(),
                fill.getAccountId(),
                fill.getSymbol(),
                fill.getSide().name(),
                fill.getFillPrice(),
                fill.getFillQty(),
                fill.getFee(),
                fill.getTax(),
                fill.getFillTimestamp(),
                fill.getBrokerOrderNo()
        );
    }
}
