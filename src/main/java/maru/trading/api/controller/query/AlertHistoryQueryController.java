package maru.trading.api.controller.query;

import maru.trading.infra.persistence.jpa.entity.AlertLogEntity;
import maru.trading.infra.persistence.jpa.repository.AlertLogJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Alert History Query API.
 *
 * Provides endpoints for querying alert history and statistics.
 *
 * Endpoints:
 * - GET /api/v1/query/alerts/history - Alert history with filters
 * - GET /api/v1/query/alerts/stats - Alert statistics
 */
@RestController
@RequestMapping("/api/v1/query/alerts")
public class AlertHistoryQueryController {

    private static final Logger log = LoggerFactory.getLogger(AlertHistoryQueryController.class);

    private final AlertLogJpaRepository alertLogRepository;

    public AlertHistoryQueryController(AlertLogJpaRepository alertLogRepository) {
        this.alertLogRepository = alertLogRepository;
    }

    /**
     * Get alert history with filters.
     *
     * @param severity Filter by severity: INFO, WARN, CRIT
     * @param category Filter by category: OPS, RISK, ORDER
     * @param from Start date
     * @param to End date
     * @param page Page number (0-based)
     * @param size Page size
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getAlertHistory(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        log.info("Getting alert history: severity={}, category={}", severity, category);

        LocalDateTime endDateTime = to != null ? to.plusDays(1).atStartOfDay() : LocalDateTime.now();
        LocalDateTime startDateTime = from != null ? from.atStartOfDay() : endDateTime.minusDays(7);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));

        Page<AlertLogEntity> alertPage;
        if (severity != null && category != null) {
            alertPage = alertLogRepository.findBySeverityAndCategoryAndSentAtBetween(
                    severity, category, startDateTime, endDateTime, pageable);
        } else if (severity != null) {
            alertPage = alertLogRepository.findBySeverityAndSentAtBetween(
                    severity, startDateTime, endDateTime, pageable);
        } else if (category != null) {
            alertPage = alertLogRepository.findByCategoryAndSentAtBetween(
                    category, startDateTime, endDateTime, pageable);
        } else {
            alertPage = alertLogRepository.findBySentAtBetween(
                    startDateTime, endDateTime, pageable);
        }

        List<Map<String, Object>> alerts = alertPage.getContent().stream()
                .map(this::toAlertMap)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("alerts", alerts);
        response.put("totalCount", alertPage.getTotalElements());
        response.put("page", page);
        response.put("pageSize", size);
        response.put("totalPages", alertPage.getTotalPages());
        response.put("fromDate", startDateTime.toLocalDate());
        response.put("toDate", endDateTime.toLocalDate());

        return ResponseEntity.ok(response);
    }

    /**
     * Get alert statistics.
     *
     * @param from Start date
     * @param to End date
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAlertStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        log.info("Getting alert statistics: from={}, to={}", from, to);

        LocalDateTime endDateTime = to != null ? to.plusDays(1).atStartOfDay() : LocalDateTime.now();
        LocalDateTime startDateTime = from != null ? from.atStartOfDay() : endDateTime.minusDays(30);

        List<AlertLogEntity> alerts = alertLogRepository.findBySentAtBetweenOrderBySentAtDesc(
                startDateTime, endDateTime);

        // Count by severity
        Map<String, Long> bySeverity = alerts.stream()
                .collect(Collectors.groupingBy(AlertLogEntity::getSeverity, Collectors.counting()));

        // Count by category
        Map<String, Long> byCategory = alerts.stream()
                .collect(Collectors.groupingBy(AlertLogEntity::getCategory, Collectors.counting()));

        // Count by channel
        Map<String, Long> byChannel = alerts.stream()
                .collect(Collectors.groupingBy(AlertLogEntity::getChannel, Collectors.counting()));

        // Success rate
        long successCount = alerts.stream().filter(a -> Boolean.TRUE.equals(a.getSuccess())).count();
        long failedCount = alerts.size() - successCount;

        // Daily counts
        Map<LocalDate, Long> dailyCounts = alerts.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getSentAt().toLocalDate(),
                        Collectors.counting()));

        Map<String, Object> response = new HashMap<>();
        response.put("fromDate", startDateTime.toLocalDate());
        response.put("toDate", endDateTime.toLocalDate());
        response.put("totalAlerts", alerts.size());
        response.put("bySeverity", bySeverity);
        response.put("byCategory", byCategory);
        response.put("byChannel", byChannel);
        response.put("successCount", successCount);
        response.put("failedCount", failedCount);
        response.put("successRate", alerts.size() > 0
                ? Math.round(successCount * 100.0 / alerts.size() * 100) / 100.0 : 0);
        response.put("dailyCounts", dailyCounts);

        // Recent critical alerts
        List<Map<String, Object>> recentCritical = alerts.stream()
                .filter(a -> "CRIT".equals(a.getSeverity()))
                .limit(5)
                .map(this::toAlertMap)
                .collect(Collectors.toList());
        response.put("recentCritical", recentCritical);

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> toAlertMap(AlertLogEntity alert) {
        Map<String, Object> map = new HashMap<>();
        map.put("alertId", alert.getAlertId());
        map.put("severity", alert.getSeverity());
        map.put("category", alert.getCategory());
        map.put("channel", alert.getChannel());
        map.put("message", alert.getMessage());
        map.put("success", alert.getSuccess());
        map.put("relatedEventId", alert.getRelatedEventId());
        map.put("sentAt", alert.getSentAt());
        return map;
    }
}
