package maru.trading.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import maru.trading.api.dto.request.CancelOrderRequest;
import maru.trading.api.dto.request.ModifyOrderRequest;
import maru.trading.domain.order.OrderStatus;
import maru.trading.domain.order.OrderType;
import maru.trading.domain.order.Side;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.OrderEntity;
import maru.trading.infra.persistence.jpa.repository.OrderJpaRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Order Admin Controller Test
 *
 * Tests Order Admin API endpoints:
 * - POST /api/v1/admin/orders - Create order
 * - POST /api/v1/admin/orders/{orderId}/cancel - Cancel order by path
 * - POST /api/v1/admin/orders/cancel - Cancel order by body
 * - POST /api/v1/admin/orders/modify - Modify order
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Order Admin Controller Test")
class OrderAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderJpaRepository orderRepository;

    private static final String BASE_URL = "/api/v1/admin/orders";
    private String testAccountId;
    private String testOrderId;

    @BeforeEach
    void setUp() {
        testAccountId = UlidGenerator.generate();
        testOrderId = UlidGenerator.generate();
        createTestOrder();
    }

    @Nested
    @DisplayName("POST /api/v1/admin/orders - Create Order")
    class CreateOrder {

        @Test
        @DisplayName("Should create order successfully")
        void createOrder_Success() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("accountId", testAccountId);
            request.put("symbol", "005930");
            request.put("side", "BUY");
            request.put("orderType", "LIMIT");
            request.put("quantity", 100);
            request.put("price", 70000);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").exists())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.symbol").value("005930"))
                    .andExpect(jsonPath("$.side").value("BUY"))
                    .andExpect(jsonPath("$.status").value("NEW"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/orders/{orderId}/cancel - Cancel Order by Path")
    class CancelOrderByPath {

        @Test
        @DisplayName("Should cancel order successfully")
        void cancelOrder_Success() throws Exception {
            mockMvc.perform(post(BASE_URL + "/" + testOrderId + "/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(testOrderId))
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent order")
        void cancelOrder_NotFound() throws Exception {
            String nonExistentId = UlidGenerator.generate();

            mockMvc.perform(post(BASE_URL + "/" + nonExistentId + "/cancel"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/orders/cancel - Cancel Order by Body")
    class CancelOrderByBody {

        @Test
        @DisplayName("Should cancel order via request body")
        void cancelOrder_Success() throws Exception {
            CancelOrderRequest request = CancelOrderRequest.builder()
                    .orderId(testOrderId)
                    .reason("Manual cancellation request")
                    .build();

            mockMvc.perform(post(BASE_URL + "/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(testOrderId))
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("Should return 400 for missing orderId")
        void cancelOrder_MissingOrderId() throws Exception {
            String invalidRequest = "{\"reason\": \"test\"}";

            mockMvc.perform(post(BASE_URL + "/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/orders/modify - Modify Order")
    class ModifyOrder {

        @Test
        @DisplayName("Should modify order quantity")
        void modifyOrder_Qty() throws Exception {
            ModifyOrderRequest request = ModifyOrderRequest.builder()
                    .orderId(testOrderId)
                    .newQty(BigDecimal.valueOf(200))
                    .reason("Increase quantity")
                    .build();

            mockMvc.perform(post(BASE_URL + "/modify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(testOrderId))
                    .andExpect(jsonPath("$.qty").value(200));
        }

        @Test
        @DisplayName("Should modify order price")
        void modifyOrder_Price() throws Exception {
            ModifyOrderRequest request = ModifyOrderRequest.builder()
                    .orderId(testOrderId)
                    .newPrice(BigDecimal.valueOf(75000))
                    .reason("Adjust price")
                    .build();

            mockMvc.perform(post(BASE_URL + "/modify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(testOrderId))
                    .andExpect(jsonPath("$.price").value(75000));
        }

        @Test
        @DisplayName("Should return 400 for missing orderId")
        void modifyOrder_MissingOrderId() throws Exception {
            String invalidRequest = "{\"newQty\": 200}";

            mockMvc.perform(post(BASE_URL + "/modify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== Helper Methods ====================

    private void createTestOrder() {
        OrderEntity order = OrderEntity.builder()
                .orderId(testOrderId)
                .accountId(testAccountId)
                .strategyId(UlidGenerator.generate())
                .strategyVersionId(UlidGenerator.generate())
                .symbol("005930")
                .side(Side.BUY)
                .orderType(OrderType.LIMIT)
                .ordDvsn("00")
                .qty(BigDecimal.valueOf(100))
                .price(BigDecimal.valueOf(70000))
                .status(OrderStatus.SENT)
                .idempotencyKey(UlidGenerator.generate())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        orderRepository.save(order);
    }
}
