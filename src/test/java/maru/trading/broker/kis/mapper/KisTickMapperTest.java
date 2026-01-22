package maru.trading.broker.kis.mapper;

import maru.trading.broker.kis.dto.KisTickMessage;
import maru.trading.domain.market.MarketTick;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("KisTickMapper Test")
class KisTickMapperTest {

    private KisTickMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new KisTickMapper();
    }

    @Nested
    @DisplayName("toDomain Tests")
    class ToDomainTests {

        @Test
        @DisplayName("Should convert KisTickMessage to MarketTick")
        void shouldConvertKisTickMessageToMarketTick() {
            // Given
            KisTickMessage message = createTickMessage("005930", "70000", "1000", "093000", "NORMAL");

            // When
            MarketTick result = mapper.toDomain(message);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSymbol()).isEqualTo("005930");
            assertThat(result.getPrice()).isEqualTo(new BigDecimal("70000"));
            assertThat(result.getVolume()).isEqualTo(1000L);
            assertThat(result.getTradingStatus()).isEqualTo("NORMAL");
        }

        @Test
        @DisplayName("Should set default trading status when null")
        void shouldSetDefaultTradingStatusWhenNull() {
            // Given
            KisTickMessage message = createTickMessage("005930", "70000", "1000", "093000", null);

            // When
            MarketTick result = mapper.toDomain(message);

            // Then
            assertThat(result.getTradingStatus()).isEqualTo("NORMAL");
        }

        @Test
        @DisplayName("Should handle different trading statuses")
        void shouldHandleDifferentTradingStatuses() {
            // Given
            KisTickMessage message = createTickMessage("005930", "70000", "1000", "093000", "HALT");

            // When
            MarketTick result = mapper.toDomain(message);

            // Then
            assertThat(result.getTradingStatus()).isEqualTo("HALT");
        }

        @Test
        @DisplayName("Should throw exception for invalid price format")
        void shouldThrowExceptionForInvalidPriceFormat() {
            // Given
            KisTickMessage message = createTickMessage("005930", "invalid", "1000", "093000", "NORMAL");

            // When/Then
            assertThatThrownBy(() -> mapper.toDomain(message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to map KIS tick message");
        }

        @Test
        @DisplayName("Should throw exception for invalid volume format")
        void shouldThrowExceptionForInvalidVolumeFormat() {
            // Given
            KisTickMessage message = createTickMessage("005930", "70000", "invalid", "093000", "NORMAL");

            // When/Then
            assertThatThrownBy(() -> mapper.toDomain(message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to map KIS tick message");
        }
    }

    @Nested
    @DisplayName("Time Parsing Tests")
    class TimeParsingTests {

        @Test
        @DisplayName("Should parse valid time format")
        void shouldParseValidTimeFormat() {
            // Given
            KisTickMessage message = createTickMessage("005930", "70000", "1000", "143052", "NORMAL");

            // When
            MarketTick result = mapper.toDomain(message);

            // Then
            assertThat(result.getTimestamp()).isNotNull();
            assertThat(result.getTimestamp().getHour()).isEqualTo(14);
            assertThat(result.getTimestamp().getMinute()).isEqualTo(30);
            assertThat(result.getTimestamp().getSecond()).isEqualTo(52);
        }

        @Test
        @DisplayName("Should use current time for null time")
        void shouldUseCurrentTimeForNullTime() {
            // Given
            KisTickMessage message = createTickMessage("005930", "70000", "1000", null, "NORMAL");
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            // When
            MarketTick result = mapper.toDomain(message);

            // Then
            assertThat(result.getTimestamp()).isAfter(before);
        }

        @Test
        @DisplayName("Should use current time for invalid time format")
        void shouldUseCurrentTimeForInvalidTimeFormat() {
            // Given
            KisTickMessage message = createTickMessage("005930", "70000", "1000", "invalid", "NORMAL");
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            // When
            MarketTick result = mapper.toDomain(message);

            // Then
            assertThat(result.getTimestamp()).isAfter(before);
        }

        @Test
        @DisplayName("Should use current time for short time format")
        void shouldUseCurrentTimeForShortTimeFormat() {
            // Given
            KisTickMessage message = createTickMessage("005930", "70000", "1000", "1430", "NORMAL");
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            // When
            MarketTick result = mapper.toDomain(message);

            // Then
            assertThat(result.getTimestamp()).isAfter(before);
        }
    }

    // ==================== Helper Methods ====================

    private KisTickMessage createTickMessage(String symbol, String price, String volume, String time, String tradingStatus) {
        KisTickMessage message = new KisTickMessage();
        message.setSymbol(symbol);
        message.setPrice(price);
        message.setVolume(volume);
        message.setTime(time);
        message.setTradingStatus(tradingStatus);
        return message;
    }
}
