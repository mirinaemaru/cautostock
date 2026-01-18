package maru.trading.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import maru.trading.api.dto.request.CreateNotificationRequest;
import maru.trading.api.dto.request.NotificationSettingsRequest;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.NotificationEntity;
import maru.trading.infra.persistence.jpa.repository.NotificationJpaRepository;
import maru.trading.infra.persistence.jpa.repository.NotificationSettingsJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Notification Admin Controller Test
 *
 * Tests Notification API endpoints:
 * - POST   /api/v1/admin/notifications - Create notification
 * - GET    /api/v1/admin/notifications - List notifications
 * - GET    /api/v1/admin/notifications/unread - Get unread notifications
 * - POST   /api/v1/admin/notifications/{id}/read - Mark as read
 * - POST   /api/v1/admin/notifications/read-all - Mark all as read
 * - DELETE /api/v1/admin/notifications/{id} - Delete notification
 * - GET    /api/v1/admin/notifications/settings - Get settings
 * - POST   /api/v1/admin/notifications/settings - Update settings
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Notification Admin Controller Test")
class NotificationAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationJpaRepository notificationRepository;

    @Autowired
    private NotificationSettingsJpaRepository settingsRepository;

    private static final String BASE_URL = "/api/v1/admin/notifications";
    private String testAccountId;

    @BeforeEach
    void setUp() {
        testAccountId = UlidGenerator.generate();
        createTestNotifications();
    }

    @Nested
    @DisplayName("POST /api/v1/admin/notifications - Create Notification")
    class CreateNotification {

        @Test
        @DisplayName("Should create notification successfully")
        void createNotification_Success() throws Exception {
            CreateNotificationRequest request = CreateNotificationRequest.builder()
                    .accountId(testAccountId)
                    .notificationType("TRADE")
                    .severity("INFO")
                    .title("Order Filled")
                    .message("Your order has been filled successfully")
                    .refType("ORDER")
                    .refId(UlidGenerator.generate())
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.notificationId").exists())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.notificationType").value("TRADE"))
                    .andExpect(jsonPath("$.title").value("Order Filled"))
                    .andExpect(jsonPath("$.read").value(false));
        }

        @Test
        @DisplayName("Should create notification with default severity")
        void createNotification_DefaultSeverity() throws Exception {
            CreateNotificationRequest request = CreateNotificationRequest.builder()
                    .accountId(testAccountId)
                    .notificationType("SYSTEM")
                    .title("System Update")
                    .message("System maintenance scheduled")
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.severity").value("INFO"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/notifications - List Notifications")
    class ListNotifications {

        @Test
        @DisplayName("Should list notifications for account")
        void listNotifications_Success() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("accountId", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notifications").isArray())
                    .andExpect(jsonPath("$.totalCount").exists())
                    .andExpect(jsonPath("$.unreadCount").exists());
        }

        @Test
        @DisplayName("Should support pagination")
        void listNotifications_Pagination() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("accountId", testAccountId)
                            .param("page", "0")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.pageSize").value(5));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/notifications/unread - Get Unread Notifications")
    class GetUnreadNotifications {

        @Test
        @DisplayName("Should return unread notifications")
        void getUnread_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/unread")
                            .param("accountId", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/notifications/unread-count - Get Unread Count")
    class GetUnreadCount {

        @Test
        @DisplayName("Should return unread count")
        void getUnreadCount_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/unread-count")
                            .param("accountId", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.unreadCount").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/notifications/{id}/read - Mark as Read")
    class MarkAsRead {

        @Test
        @DisplayName("Should mark notification as read")
        void markAsRead_Success() throws Exception {
            // Create unread notification
            String notificationId = createUnreadNotification();

            mockMvc.perform(post(BASE_URL + "/" + notificationId + "/read"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true));

            // Verify it's marked as read
            NotificationEntity updated = notificationRepository.findById(notificationId).orElseThrow();
            assertThat(updated.isUnread()).isFalse();
        }

        @Test
        @DisplayName("Should return 404 for non-existent notification")
        void markAsRead_NotFound() throws Exception {
            String nonExistentId = UlidGenerator.generate();

            mockMvc.perform(post(BASE_URL + "/" + nonExistentId + "/read"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/notifications/read-all - Mark All as Read")
    class MarkAllAsRead {

        @Test
        @DisplayName("Should mark all notifications as read")
        void markAllAsRead_Success() throws Exception {
            mockMvc.perform(post(BASE_URL + "/read-all")
                            .param("accountId", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.markedAsRead").exists());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/notifications/{id} - Delete Notification")
    class DeleteNotification {

        @Test
        @DisplayName("Should delete notification")
        void deleteNotification_Success() throws Exception {
            String notificationId = createUnreadNotification();

            mockMvc.perform(delete(BASE_URL + "/" + notificationId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true));

            assertThat(notificationRepository.existsById(notificationId)).isFalse();
        }

        @Test
        @DisplayName("Should return 404 for non-existent notification")
        void deleteNotification_NotFound() throws Exception {
            String nonExistentId = UlidGenerator.generate();

            mockMvc.perform(delete(BASE_URL + "/" + nonExistentId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Notification Settings")
    class NotificationSettings {

        @Test
        @DisplayName("Should get notification settings")
        void getSettings_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/settings")
                            .param("accountId", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.settingId").exists())
                    .andExpect(jsonPath("$.pushEnabled").exists())
                    .andExpect(jsonPath("$.tradeAlerts").exists());
        }

        @Test
        @DisplayName("Should update notification settings")
        void updateSettings_Success() throws Exception {
            NotificationSettingsRequest request = NotificationSettingsRequest.builder()
                    .emailEnabled(true)
                    .emailAddress("test@example.com")
                    .pushEnabled(true)
                    .tradeAlerts(true)
                    .riskAlerts(true)
                    .systemAlerts(false)
                    .dailySummary(true)
                    .build();

            mockMvc.perform(post(BASE_URL + "/settings")
                            .param("accountId", testAccountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.emailEnabled").value(true))
                    .andExpect(jsonPath("$.emailAddress").value("test@example.com"))
                    .andExpect(jsonPath("$.systemAlerts").value(false))
                    .andExpect(jsonPath("$.dailySummary").value(true));
        }
    }

    // ==================== Helper Methods ====================

    private void createTestNotifications() {
        for (int i = 0; i < 5; i++) {
            NotificationEntity notification = NotificationEntity.builder()
                    .notificationId(UlidGenerator.generate())
                    .accountId(testAccountId)
                    .notificationType("TRADE")
                    .severity("INFO")
                    .title("Test Notification " + i)
                    .message("Test message " + i)
                    .isRead("N")
                    .createdAt(LocalDateTime.now().minusHours(i))
                    .build();
            notificationRepository.save(notification);
        }
    }

    private String createUnreadNotification() {
        NotificationEntity notification = NotificationEntity.builder()
                .notificationId(UlidGenerator.generate())
                .accountId(testAccountId)
                .notificationType("TRADE")
                .severity("INFO")
                .title("Unread Notification")
                .message("This is an unread notification")
                .isRead("N")
                .createdAt(LocalDateTime.now())
                .build();
        return notificationRepository.save(notification).getNotificationId();
    }
}
