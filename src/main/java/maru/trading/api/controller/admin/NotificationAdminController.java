package maru.trading.api.controller.admin;

import maru.trading.api.dto.request.CreateNotificationRequest;
import maru.trading.api.dto.request.NotificationSettingsRequest;
import maru.trading.api.dto.response.AckResponse;
import maru.trading.api.dto.response.NotificationResponse;
import maru.trading.api.dto.response.NotificationSettingsResponse;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.NotificationEntity;
import maru.trading.infra.persistence.jpa.entity.NotificationSettingsEntity;
import maru.trading.infra.persistence.jpa.repository.NotificationJpaRepository;
import maru.trading.infra.persistence.jpa.repository.NotificationSettingsJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Notification Admin API.
 *
 * Endpoints:
 * - POST   /api/v1/admin/notifications                     - Create notification
 * - GET    /api/v1/admin/notifications                     - List notifications
 * - GET    /api/v1/admin/notifications/unread              - Get unread notifications
 * - GET    /api/v1/admin/notifications/unread-count        - Get unread count
 * - POST   /api/v1/admin/notifications/{id}/read           - Mark as read
 * - POST   /api/v1/admin/notifications/read-all            - Mark all as read
 * - DELETE /api/v1/admin/notifications/{id}                - Delete notification
 * - GET    /api/v1/admin/notifications/settings            - Get settings
 * - POST   /api/v1/admin/notifications/settings            - Update settings
 */
@RestController
@RequestMapping("/api/v1/admin/notifications")
public class NotificationAdminController {

    private static final Logger log = LoggerFactory.getLogger(NotificationAdminController.class);

    private final NotificationJpaRepository notificationRepository;
    private final NotificationSettingsJpaRepository settingsRepository;

    public NotificationAdminController(
            NotificationJpaRepository notificationRepository,
            NotificationSettingsJpaRepository settingsRepository) {
        this.notificationRepository = notificationRepository;
        this.settingsRepository = settingsRepository;
    }

    /**
     * Create a new notification.
     */
    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(
            @RequestBody CreateNotificationRequest request) {

        log.info("Creating notification: type={}, title={}", request.getNotificationType(), request.getTitle());

        NotificationEntity entity = NotificationEntity.builder()
                .notificationId(UlidGenerator.generate())
                .accountId(request.getAccountId())
                .notificationType(request.getNotificationType())
                .severity(request.getSeverity() != null ? request.getSeverity() : "INFO")
                .title(request.getTitle())
                .message(request.getMessage())
                .refType(request.getRefType())
                .refId(request.getRefId())
                .isRead("N")
                .createdAt(LocalDateTime.now())
                .build();

        NotificationEntity saved = notificationRepository.save(entity);
        log.info("Notification created: id={}", saved.getNotificationId());

        return ResponseEntity.status(HttpStatus.CREATED).body(NotificationResponse.fromEntity(saved));
    }

    /**
     * List notifications with pagination.
     */
    @GetMapping
    public ResponseEntity<NotificationResponse.NotificationList> listNotifications(
            @RequestParam String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Listing notifications for account: {}, page: {}, size: {}", accountId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationEntity> notificationPage = notificationRepository
                .findByAccountIdOrderByCreatedAtDesc(accountId, pageable);

        List<NotificationResponse> notifications = notificationPage.getContent().stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());

        long unreadCount = notificationRepository.countUnreadByAccountId(accountId);

        return ResponseEntity.ok(NotificationResponse.NotificationList.builder()
                .notifications(notifications)
                .totalCount((int) notificationPage.getTotalElements())
                .unreadCount((int) unreadCount)
                .page(page)
                .pageSize(size)
                .build());
    }

    /**
     * Get unread notifications.
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(
            @RequestParam String accountId) {

        log.info("Fetching unread notifications for account: {}", accountId);

        List<NotificationEntity> unread = notificationRepository.findUnreadByAccountId(accountId);
        List<NotificationResponse> responses = unread.stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get unread notification count.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(@RequestParam String accountId) {

        long count = notificationRepository.countUnreadByAccountId(accountId);

        return ResponseEntity.ok(Map.of(
                "accountId", accountId,
                "unreadCount", count
        ));
    }

    /**
     * Mark notification as read.
     */
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<AckResponse> markAsRead(@PathVariable String notificationId) {

        log.info("Marking notification as read: {}", notificationId);

        return notificationRepository.findById(notificationId)
                .map(entity -> {
                    entity.markAsRead();
                    notificationRepository.save(entity);
                    return ResponseEntity.ok(AckResponse.success("Notification marked as read"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Mark all notifications as read.
     */
    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@RequestParam String accountId) {

        log.info("Marking all notifications as read for account: {}", accountId);

        int updated = notificationRepository.markAllAsReadByAccountId(accountId, LocalDateTime.now());

        return ResponseEntity.ok(Map.of(
                "accountId", accountId,
                "markedAsRead", updated
        ));
    }

    /**
     * Delete notification.
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<AckResponse> deleteNotification(@PathVariable String notificationId) {

        log.info("Deleting notification: {}", notificationId);

        if (!notificationRepository.existsById(notificationId)) {
            return ResponseEntity.notFound().build();
        }

        notificationRepository.deleteById(notificationId);
        return ResponseEntity.ok(AckResponse.success("Notification deleted"));
    }

    /**
     * Get notification settings.
     */
    @GetMapping("/settings")
    public ResponseEntity<NotificationSettingsResponse> getSettings(
            @RequestParam(required = false) String accountId) {

        log.info("Fetching notification settings for account: {}", accountId);

        NotificationSettingsEntity settings;
        if (accountId != null) {
            settings = settingsRepository.findByAccountId(accountId)
                    .orElseGet(() -> createDefaultSettings(accountId));
        } else {
            settings = settingsRepository.findGlobalSettings()
                    .orElseGet(() -> createDefaultSettings(null));
        }

        return ResponseEntity.ok(NotificationSettingsResponse.fromEntity(settings));
    }

    /**
     * Update notification settings.
     */
    @PostMapping("/settings")
    public ResponseEntity<NotificationSettingsResponse> updateSettings(
            @RequestParam(required = false) String accountId,
            @RequestBody NotificationSettingsRequest request) {

        log.info("Updating notification settings for account: {}", accountId);

        NotificationSettingsEntity settings;
        if (accountId != null) {
            settings = settingsRepository.findByAccountId(accountId)
                    .orElseGet(() -> createDefaultSettings(accountId));
        } else {
            settings = settingsRepository.findGlobalSettings()
                    .orElseGet(() -> createDefaultSettings(null));
        }

        // Update fields
        if (request.getEmailEnabled() != null) {
            settings.setEmailEnabled(request.getEmailEnabled() ? "Y" : "N");
        }
        if (request.getEmailAddress() != null) {
            settings.setEmailAddress(request.getEmailAddress());
        }
        if (request.getPushEnabled() != null) {
            settings.setPushEnabled(request.getPushEnabled() ? "Y" : "N");
        }
        if (request.getTradeAlerts() != null) {
            settings.setTradeAlerts(request.getTradeAlerts() ? "Y" : "N");
        }
        if (request.getRiskAlerts() != null) {
            settings.setRiskAlerts(request.getRiskAlerts() ? "Y" : "N");
        }
        if (request.getSystemAlerts() != null) {
            settings.setSystemAlerts(request.getSystemAlerts() ? "Y" : "N");
        }
        if (request.getDailySummary() != null) {
            settings.setDailySummary(request.getDailySummary() ? "Y" : "N");
        }

        NotificationSettingsEntity saved = settingsRepository.save(settings);
        log.info("Notification settings updated: settingId={}", saved.getSettingId());

        return ResponseEntity.ok(NotificationSettingsResponse.fromEntity(saved));
    }

    private NotificationSettingsEntity createDefaultSettings(String accountId) {
        return NotificationSettingsEntity.builder()
                .settingId(UlidGenerator.generate())
                .accountId(accountId)
                .emailEnabled("N")
                .pushEnabled("Y")
                .tradeAlerts("Y")
                .riskAlerts("Y")
                .systemAlerts("Y")
                .dailySummary("N")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
