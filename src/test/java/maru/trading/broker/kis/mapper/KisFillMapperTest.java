package maru.trading.broker.kis.mapper;

import maru.trading.broker.kis.dto.KisFillMessage;
import maru.trading.domain.execution.FeeCalculator;
import maru.trading.domain.execution.Fill;
import maru.trading.domain.order.Side;
import maru.trading.infra.config.UlidGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KisFillMapper Test")
class KisFillMapperTest {

    @Mock
    private UlidGenerator ulidGenerator;

    @Mock
    private FeeCalculator feeCalculator;

    private KisFillMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new KisFillMapper(ulidGenerator, feeCalculator);
    }

    @Nested
    @DisplayName("toDomain Tests")
    class ToDomainTests {

        @Test
        @DisplayName("Should convert KisFillMessage to Fill (BUY)")
        void shouldConvertKisFillMessageToFillBuy() {
            // Given
            when(ulidGenerator.generateInstance()).thenReturn("FILL_001");
            when(feeCalculator.calculateFee(anyString(), any(), anyInt(), any()))
                    .thenReturn(BigDecimal.valueOf(1050));
            when(feeCalculator.calculateTax(anyString(), any(), anyInt(), any()))
                    .thenReturn(BigDecimal.ZERO);

            KisFillMessage message = createFillMessage("BROKER001", "01", "70000", "10", "005930", "093000");

            // When
            Fill result = mapper.toDomain(message, "ORDER_001", "ACC_001");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFillId()).isEqualTo("FILL_001");
            assertThat(result.getOrderId()).isEqualTo("ORDER_001");
            assertThat(result.getAccountId()).isEqualTo("ACC_001");
            assertThat(result.getSymbol()).isEqualTo("005930");
            assertThat(result.getSide()).isEqualTo(Side.BUY);
            assertThat(result.getFillPrice()).isEqualTo(new BigDecimal("70000"));
            assertThat(result.getFillQty()).isEqualTo(10);
            assertThat(result.getFee()).isEqualTo(BigDecimal.valueOf(1050));
            assertThat(result.getTax()).isEqualTo(BigDecimal.ZERO);
            assertThat(result.getBrokerOrderNo()).isEqualTo("BROKER001");
        }

        @Test
        @DisplayName("Should convert KisFillMessage to Fill (SELL)")
        void shouldConvertKisFillMessageToFillSell() {
            // Given
            when(ulidGenerator.generateInstance()).thenReturn("FILL_002");
            when(feeCalculator.calculateFee(anyString(), any(), anyInt(), any()))
                    .thenReturn(BigDecimal.valueOf(1050));
            when(feeCalculator.calculateTax(anyString(), any(), anyInt(), any()))
                    .thenReturn(BigDecimal.valueOf(1610));

            KisFillMessage message = createFillMessage("BROKER002", "02", "71000", "10", "005930", "143000");

            // When
            Fill result = mapper.toDomain(message, "ORDER_002", "ACC_001");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSide()).isEqualTo(Side.SELL);
            assertThat(result.getTax()).isEqualTo(BigDecimal.valueOf(1610));
        }

        @Test
        @DisplayName("Should default to BUY for unknown side code")
        void shouldDefaultToBuyForUnknownSideCode() {
            // Given
            when(ulidGenerator.generateInstance()).thenReturn("FILL_003");
            when(feeCalculator.calculateFee(anyString(), any(), anyInt(), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(feeCalculator.calculateTax(anyString(), any(), anyInt(), any()))
                    .thenReturn(BigDecimal.ZERO);

            KisFillMessage message = createFillMessage("BROKER003", "99", "70000", "10", "005930", "093000");

            // When
            Fill result = mapper.toDomain(message, "ORDER_003", "ACC_001");

            // Then
            assertThat(result.getSide()).isEqualTo(Side.BUY);
        }

        @Test
        @DisplayName("Should throw exception for invalid price format")
        void shouldThrowExceptionForInvalidPriceFormat() {
            // Given
            KisFillMessage message = createFillMessage("BROKER001", "01", "invalid", "10", "005930", "093000");

            // When/Then
            assertThatThrownBy(() -> mapper.toDomain(message, "ORDER_001", "ACC_001"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to map KIS fill message");
        }

        @Test
        @DisplayName("Should throw exception for invalid quantity format")
        void shouldThrowExceptionForInvalidQuantityFormat() {
            // Given
            KisFillMessage message = createFillMessage("BROKER001", "01", "70000", "invalid", "005930", "093000");

            // When/Then
            assertThatThrownBy(() -> mapper.toDomain(message, "ORDER_001", "ACC_001"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to map KIS fill message");
        }
    }

    @Nested
    @DisplayName("Time Parsing Tests")
    class TimeParsingTests {

        @Test
        @DisplayName("Should parse valid fill time")
        void shouldParseValidFillTime() {
            // Given
            when(ulidGenerator.generateInstance()).thenReturn("FILL_001");
            when(feeCalculator.calculateFee(anyString(), any(), anyInt(), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(feeCalculator.calculateTax(anyString(), any(), anyInt(), any()))
                    .thenReturn(BigDecimal.ZERO);

            KisFillMessage message = createFillMessage("BROKER001", "01", "70000", "10", "005930", "143052");

            // When
            Fill result = mapper.toDomain(message, "ORDER_001", "ACC_001");

            // Then
            assertThat(result.getFillTimestamp()).isNotNull();
            assertThat(result.getFillTimestamp().getHour()).isEqualTo(14);
            assertThat(result.getFillTimestamp().getMinute()).isEqualTo(30);
            assertThat(result.getFillTimestamp().getSecond()).isEqualTo(52);
        }

        @Test
        @DisplayName("Should use current time for null fill time")
        void shouldUseCurrentTimeForNullFillTime() {
            // Given
            when(ulidGenerator.generateInstance()).thenReturn("FILL_001");
            when(feeCalculator.calculateFee(anyString(), any(), anyInt(), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(feeCalculator.calculateTax(anyString(), any(), anyInt(), any()))
                    .thenReturn(BigDecimal.ZERO);

            KisFillMessage message = createFillMessage("BROKER001", "01", "70000", "10", "005930", null);

            // When
            Fill result = mapper.toDomain(message, "ORDER_001", "ACC_001");

            // Then
            assertThat(result.getFillTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should use current time for invalid fill time format")
        void shouldUseCurrentTimeForInvalidFillTimeFormat() {
            // Given
            when(ulidGenerator.generateInstance()).thenReturn("FILL_001");
            when(feeCalculator.calculateFee(anyString(), any(), anyInt(), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(feeCalculator.calculateTax(anyString(), any(), anyInt(), any()))
                    .thenReturn(BigDecimal.ZERO);

            KisFillMessage message = createFillMessage("BROKER001", "01", "70000", "10", "005930", "invalid");

            // When
            Fill result = mapper.toDomain(message, "ORDER_001", "ACC_001");

            // Then
            assertThat(result.getFillTimestamp()).isNotNull();
        }
    }

    // ==================== Helper Methods ====================

    private KisFillMessage createFillMessage(String brokerOrderNo, String sideCode, String price,
                                              String qty, String symbol, String time) {
        KisFillMessage message = new KisFillMessage();
        message.setBrokerOrderNo(brokerOrderNo);
        message.setSideCode(sideCode);
        message.setFillPrice(price);
        message.setFillQty(qty);
        message.setSymbol(symbol);
        message.setFillTime(time);
        return message;
    }
}
