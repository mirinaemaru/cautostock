package maru.trading.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import maru.trading.api.dto.request.InstrumentSyncRequest;
import maru.trading.api.dto.request.UpdateInstrumentStatusRequest;
import maru.trading.infra.persistence.jpa.entity.InstrumentEntity;
import maru.trading.infra.persistence.jpa.repository.InstrumentJpaRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Instrument Admin Controller Test
 *
 * Tests Instrument Admin API endpoints:
 * - POST /api/v1/admin/instruments/sync - Manual sync trigger
 * - GET /api/v1/admin/instruments - List instruments with filters
 * - GET /api/v1/admin/instruments/{symbol} - Get instrument details
 * - PUT /api/v1/admin/instruments/{symbol}/status - Update status
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Instrument Admin Controller Test")
class InstrumentAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InstrumentJpaRepository instrumentRepository;

    private static final String BASE_URL = "/api/v1/admin/instruments";

    @BeforeEach
    void setUp() {
        createTestInstruments();
    }

    @Nested
    @DisplayName("POST /api/v1/admin/instruments/sync - Sync Instruments")
    class SyncInstruments {

        @Test
        @DisplayName("Should sync all markets")
        void syncAll_Success() throws Exception {
            InstrumentSyncRequest request = InstrumentSyncRequest.builder()
                    .markets(List.of("KOSPI", "KOSDAQ"))
                    .build();

            mockMvc.perform(post(BASE_URL + "/sync")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Should sync specific markets")
        void syncSpecificMarkets_Success() throws Exception {
            InstrumentSyncRequest request = InstrumentSyncRequest.builder()
                    .markets(List.of("KOSPI"))
                    .build();

            mockMvc.perform(post(BASE_URL + "/sync")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/instruments - List Instruments")
    class ListInstruments {

        @Test
        @DisplayName("Should return all instruments without filters")
        void listAll_Success() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.total").isNumber());
        }

        @Test
        @DisplayName("Should filter by market")
        void filterByMarket_Success() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("market", "KOSPI"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }

        @Test
        @DisplayName("Should filter by tradable")
        void filterByTradable_Success() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("tradable", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }

        @Test
        @DisplayName("Should search by name")
        void searchByName_Success() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("search", "삼성"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }

        @Test
        @DisplayName("Should filter by market and tradable")
        void filterByMarketAndTradable_Success() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("market", "KOSPI")
                            .param("tradable", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/instruments/{symbol} - Get Instrument")
    class GetInstrument {

        @Test
        @DisplayName("Should return instrument by symbol")
        void getInstrument_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/005930"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.symbol").value("005930"))
                    .andExpect(jsonPath("$.market").exists())
                    .andExpect(jsonPath("$.nameKr").exists())
                    .andExpect(jsonPath("$.tradable").exists());
        }

        @Test
        @DisplayName("Should return 400 for non-existent symbol")
        void getInstrument_NotFound() throws Exception {
            mockMvc.perform(get(BASE_URL + "/999999"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/instruments/{symbol}/status - Update Status")
    class UpdateStatus {

        @Test
        @DisplayName("Should update instrument status")
        void updateStatus_Success() throws Exception {
            UpdateInstrumentStatusRequest request = UpdateInstrumentStatusRequest.builder()
                    .status("SUSPENDED")
                    .tradable(false)
                    .halted(true)
                    .build();

            mockMvc.perform(put(BASE_URL + "/005930/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.symbol").value("005930"))
                    .andExpect(jsonPath("$.status").value("SUSPENDED"))
                    .andExpect(jsonPath("$.tradable").value(false))
                    .andExpect(jsonPath("$.halted").value(true));
        }

        @Test
        @DisplayName("Should return 400 for non-existent symbol")
        void updateStatus_NotFound() throws Exception {
            UpdateInstrumentStatusRequest request = UpdateInstrumentStatusRequest.builder()
                    .status("SUSPENDED")
                    .tradable(false)
                    .halted(true)
                    .build();

            mockMvc.perform(put(BASE_URL + "/999999/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== Helper Methods ====================

    private void createTestInstruments() {
        String[][] instrumentData = {
                {"005930", "KOSPI", "삼성전자", "Samsung Electronics", "LISTED"},
                {"035720", "KOSPI", "카카오", "Kakao", "LISTED"},
                {"000660", "KOSPI", "SK하이닉스", "SK Hynix", "LISTED"},
                {"035420", "KOSDAQ", "NAVER", "NAVER", "LISTED"}
        };

        for (String[] data : instrumentData) {
            InstrumentEntity instrument = InstrumentEntity.builder()
                    .symbol(data[0])
                    .market(data[1])
                    .nameKr(data[2])
                    .nameEn(data[3])
                    .sectorCode("IT")
                    .industry("Technology")
                    .tickSize(BigDecimal.ONE)
                    .lotSize(1)
                    .listingDate(LocalDate.of(2000, 1, 1))
                    .status(data[4])
                    .tradable(true)
                    .halted(false)
                    .updatedAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();
            instrumentRepository.save(instrument);
        }
    }
}
