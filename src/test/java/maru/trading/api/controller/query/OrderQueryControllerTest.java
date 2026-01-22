package maru.trading.api.controller.query;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Order Query Controller Test
 *
 * Tests Order Query API endpoints:
 * - GET /api/v1/query/orders - Search orders with filters
 * - GET /api/v1/query/orders/{orderId} - Get order by ID
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Order Query Controller Test")
class OrderQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderJpaRepository orderRepository;

    private static final String BASE_URL = "/api/v1/query/orders";
    private String testAccountId;
    private String testOrderId;

    @BeforeEach
    void setUp() {
        testAccountId = UlidGenerator.generate();
        createTestOrders();
    }

    @Nested
    @DisplayName("GET /api/v1/query/orders - Search Orders")
    class SearchOrders {

        @Test
        @DisplayName("Should return all orders without filters")
        void searchOrders_NoFilters() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items").isNotEmpty());
        }

        @Test
        @DisplayName("Should filter orders by accountId and status")
        void searchOrders_ByAccountIdAndStatus() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("accountId", testAccountId)
                            .param("status", "FILLED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }

        @Test
        @DisplayName("Should filter orders by accountId and date range")
        void searchOrders_ByAccountIdAndDateRange() throws Exception {
            LocalDateTime from = LocalDateTime.now().minusDays(7);
            LocalDateTime to = LocalDateTime.now().plusDays(1);

            mockMvc.perform(get(BASE_URL)
                            .param("accountId", testAccountId)
                            .param("from", from.format(DateTimeFormatter.ISO_DATE_TIME))
                            .param("to", to.format(DateTimeFormatter.ISO_DATE_TIME)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }

        @Test
        @DisplayName("Should filter orders by symbol and date range")
        void searchOrders_BySymbolAndDateRange() throws Exception {
            LocalDateTime from = LocalDateTime.now().minusDays(7);
            LocalDateTime to = LocalDateTime.now().plusDays(1);

            mockMvc.perform(get(BASE_URL)
                            .param("symbol", "005930")
                            .param("from", from.format(DateTimeFormatter.ISO_DATE_TIME))
                            .param("to", to.format(DateTimeFormatter.ISO_DATE_TIME)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }

        @Test
        @DisplayName("Should apply limit to results")
        void searchOrders_WithLimit() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("limit", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items.length()").value(
                            org.hamcrest.Matchers.lessThanOrEqualTo(2)));
        }

        @Test
        @DisplayName("Should return order details in response")
        void searchOrders_OrderDetails() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[0].orderId").exists())
                    .andExpect(jsonPath("$.items[0].accountId").exists())
                    .andExpect(jsonPath("$.items[0].symbol").exists())
                    .andExpect(jsonPath("$.items[0].side").exists())
                    .andExpect(jsonPath("$.items[0].orderType").exists())
                    .andExpect(jsonPath("$.items[0].qty").exists())
                    .andExpect(jsonPath("$.items[0].status").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/query/orders/{orderId} - Get Order by ID")
    class GetOrderById {

        @Test
        @DisplayName("Should return order by ID")
        void getOrder_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + testOrderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(testOrderId))
                    .andExpect(jsonPath("$.accountId").exists())
                    .andExpect(jsonPath("$.symbol").exists())
                    .andExpect(jsonPath("$.side").exists())
                    .andExpect(jsonPath("$.orderType").exists())
                    .andExpect(jsonPath("$.qty").exists())
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.idempotencyKey").exists())
                    .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        @DisplayName("Should return 404 for non-existent order")
        void getOrder_NotFound() throws Exception {
            String nonExistentId = UlidGenerator.generate();

            mockMvc.perform(get(BASE_URL + "/" + nonExistentId))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== Helper Methods ====================

    private void createTestOrders() {
        String strategyId = UlidGenerator.generate();
        String[][] orderData = {
                {"005930", "BUY", "LIMIT", "NEW"},
                {"005930", "SELL", "MARKET", "FILLED"},
                {"035720", "BUY", "LIMIT", "REJECTED"},
                {"000660", "SELL", "LIMIT", "CANCELLED"}
        };

        for (int i = 0; i < orderData.length; i++) {
            String orderId = UlidGenerator.generate();
            if (i == 0) {
                testOrderId = orderId;
            }

            OrderEntity order = OrderEntity.builder()
                    .orderId(orderId)
                    .accountId(testAccountId)
                    .strategyId(strategyId)
                    .strategyVersionId(UlidGenerator.generate())
                    .symbol(orderData[i][0])
                    .side(Side.valueOf(orderData[i][1]))
                    .orderType(OrderType.valueOf(orderData[i][2]))
                    .ordDvsn("00")
                    .qty(BigDecimal.valueOf(100))
                    .price(BigDecimal.valueOf(70000))
                    .status(OrderStatus.valueOf(orderData[i][3]))
                    .idempotencyKey(UlidGenerator.generate())
                    .brokerOrderNo("KIS" + String.format("%06d", i))
                    .createdAt(LocalDateTime.now().minusHours(i))
                    .updatedAt(LocalDateTime.now().minusHours(i))
                    .build();
            orderRepository.save(order);
        }
    }
}
