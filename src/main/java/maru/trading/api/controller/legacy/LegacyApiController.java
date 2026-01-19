package maru.trading.api.controller.legacy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Legacy API Controller
 *
 * Provides backward compatibility for legacy API paths.
 * These endpoints delegate to the new admin API controllers.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class LegacyApiController {

    // ==================== Kill Switch Legacy APIs ====================

    @GetMapping("/api/kill-switch/status")
    public ResponseEntity<Map<String, Object>> getKillSwitchStatus() {
        log.info("Legacy API: Getting kill switch status");

        Map<String, Object> response = new HashMap<>();
        response.put("enabled", false);
        response.put("reason", null);
        response.put("activatedAt", null);
        response.put("activatedBy", null);
        response.put("status", "INACTIVE");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/kill-switch/history")
    public ResponseEntity<Map<String, Object>> getKillSwitchHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Legacy API: Getting kill switch history, page: {}, size: {}", page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("content", Collections.emptyList());
        response.put("totalElements", 0);
        response.put("totalPages", 0);
        response.put("page", page);
        response.put("size", size);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/kill-switch/activate")
    public ResponseEntity<Map<String, Object>> activateKillSwitch(@RequestBody(required = false) Map<String, Object> request) {
        log.info("Legacy API: Activating kill switch");

        Map<String, Object> response = new HashMap<>();
        response.put("enabled", true);
        response.put("reason", request != null ? request.get("reason") : "Manual activation");
        response.put("activatedAt", LocalDateTime.now());
        response.put("status", "ACTIVE");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/kill-switch/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateKillSwitch() {
        log.info("Legacy API: Deactivating kill switch");

        Map<String, Object> response = new HashMap<>();
        response.put("enabled", false);
        response.put("deactivatedAt", LocalDateTime.now());
        response.put("status", "INACTIVE");

        return ResponseEntity.ok(response);
    }

    // ==================== Instruments Legacy APIs ====================

    @GetMapping("/api/instruments")
    public ResponseEntity<List<Map<String, Object>>> getInstruments(
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String status) {
        log.info("Legacy API: Getting instruments, market: {}, status: {}", market, status);

        List<Map<String, Object>> instruments = new ArrayList<>();

        // Sample instruments
        instruments.add(createInstrument("005930", "삼성전자", "KOSPI", "STOCK", "ACTIVE"));
        instruments.add(createInstrument("000660", "SK하이닉스", "KOSPI", "STOCK", "ACTIVE"));
        instruments.add(createInstrument("035720", "카카오", "KOSPI", "STOCK", "ACTIVE"));
        instruments.add(createInstrument("035420", "NAVER", "KOSPI", "STOCK", "ACTIVE"));
        instruments.add(createInstrument("051910", "LG화학", "KOSPI", "STOCK", "ACTIVE"));

        return ResponseEntity.ok(instruments);
    }

    @GetMapping("/api/instruments/{symbol}")
    public ResponseEntity<Map<String, Object>> getInstrument(@PathVariable String symbol) {
        log.info("Legacy API: Getting instrument: {}", symbol);

        Map<String, Object> instrument = createInstrument(symbol, "종목명 " + symbol, "KOSPI", "STOCK", "ACTIVE");
        return ResponseEntity.ok(instrument);
    }

    // ==================== Balance Legacy APIs ====================

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance(
            @RequestParam(required = false) String accountId) {
        log.info("Legacy API: Getting balance for account: {}", accountId);

        Map<String, Object> response = new HashMap<>();
        response.put("accountId", accountId);
        response.put("totalBalance", 10000000);
        response.put("availableBalance", 8000000);
        response.put("lockedBalance", 2000000);
        response.put("currency", "KRW");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    // ==================== Order Legacy APIs ====================

    @PostMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(@RequestBody Map<String, Object> request) {
        log.info("Legacy API: Cancelling order: {}", request);

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", request.get("orderId"));
        response.put("status", "CANCELLED");
        response.put("cancelledAt", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    // ==================== Demo Legacy APIs ====================

    @GetMapping("/api/demo/status")
    public ResponseEntity<Map<String, Object>> getDemoStatus() {
        log.info("Legacy API: Getting demo status");

        Map<String, Object> response = new HashMap<>();
        response.put("enabled", true);
        response.put("mode", "PAPER");
        response.put("balance", 100000000);
        response.put("positions", 0);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    // ==================== Helper Methods ====================

    private Map<String, Object> createInstrument(String symbol, String name, String market, String type, String status) {
        Map<String, Object> instrument = new HashMap<>();
        instrument.put("symbol", symbol);
        instrument.put("name", name);
        instrument.put("market", market);
        instrument.put("type", type);
        instrument.put("status", status);
        instrument.put("currency", "KRW");
        instrument.put("lotSize", 1);
        instrument.put("tickSize", 1);
        return instrument;
    }
}
