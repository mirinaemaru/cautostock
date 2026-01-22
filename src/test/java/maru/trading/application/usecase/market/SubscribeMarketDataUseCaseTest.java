package maru.trading.application.usecase.market;

import maru.trading.application.ports.broker.BrokerStream;
import maru.trading.domain.market.MarketTick;
import maru.trading.infra.cache.MarketDataCache;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.messaging.outbox.OutboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscribeMarketDataUseCase Test")
class SubscribeMarketDataUseCaseTest {

    @Mock
    private BrokerStream brokerStream;

    @Mock
    private MarketDataCache marketDataCache;

    @Mock
    private OutboxService outboxService;

    @Mock
    private UlidGenerator ulidGenerator;

    @InjectMocks
    private SubscribeMarketDataUseCase subscribeMarketDataUseCase;

    @Nested
    @DisplayName("Execute Tests")
    class ExecuteTests {

        @Test
        @DisplayName("Should subscribe to market data successfully")
        void shouldSubscribeToMarketDataSuccessfully() {
            // Given
            List<String> symbols = List.of("005930", "000660");
            String expectedSubscriptionId = "SUB_12345";

            when(brokerStream.subscribeTicks(eq(symbols), any())).thenReturn(expectedSubscriptionId);

            // When
            String result = subscribeMarketDataUseCase.execute(symbols);

            // Then
            assertThat(result).isEqualTo(expectedSubscriptionId);
            verify(brokerStream).subscribeTicks(eq(symbols), any());
        }

        @Test
        @DisplayName("Should subscribe to single symbol")
        void shouldSubscribeToSingleSymbol() {
            // Given
            List<String> symbols = List.of("005930");
            String expectedSubscriptionId = "SUB_SINGLE";

            when(brokerStream.subscribeTicks(eq(symbols), any())).thenReturn(expectedSubscriptionId);

            // When
            String result = subscribeMarketDataUseCase.execute(symbols);

            // Then
            assertThat(result).isEqualTo(expectedSubscriptionId);
            verify(brokerStream).subscribeTicks(eq(symbols), any());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw exception when symbols is null")
        void shouldThrowExceptionWhenSymbolsIsNull() {
            // When & Then
            assertThatThrownBy(() -> subscribeMarketDataUseCase.execute(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception when symbols is empty")
        void shouldThrowExceptionWhenSymbolsIsEmpty() {
            // When & Then
            assertThatThrownBy(() -> subscribeMarketDataUseCase.execute(Collections.emptyList()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception when symbol is null")
        void shouldThrowExceptionWhenSymbolIsNull() {
            // Given
            List<String> symbols = java.util.Arrays.asList("005930", null, "000660");

            // When & Then
            assertThatThrownBy(() -> subscribeMarketDataUseCase.execute(symbols))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw exception when symbol is blank")
        void shouldThrowExceptionWhenSymbolIsBlank() {
            // Given
            List<String> symbols = List.of("005930", "   ", "000660");

            // When & Then
            assertThatThrownBy(() -> subscribeMarketDataUseCase.execute(symbols))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null or blank");
        }
    }

    @Nested
    @DisplayName("Tick Handler Tests")
    class TickHandlerTests {

        @Test
        @DisplayName("Should pass tick handler to broker stream")
        @SuppressWarnings("unchecked")
        void shouldPassTickHandlerToBrokerStream() {
            // Given
            List<String> symbols = List.of("005930");

            when(brokerStream.subscribeTicks(any(), any())).thenAnswer(invocation -> {
                // Verify handler is passed
                Consumer<MarketTick> handler = invocation.getArgument(1);
                assertThat(handler).isNotNull();
                return "SUB_001";
            });

            // When
            subscribeMarketDataUseCase.execute(symbols);

            // Then
            verify(brokerStream).subscribeTicks(eq(symbols), any());
        }
    }

    @Nested
    @DisplayName("Multiple Symbols Tests")
    class MultipleSymbolsTests {

        @Test
        @DisplayName("Should subscribe to multiple symbols at once")
        void shouldSubscribeToMultipleSymbolsAtOnce() {
            // Given
            List<String> symbols = List.of("005930", "000660", "035720", "051910", "006400");
            String expectedSubscriptionId = "SUB_MULTI";

            when(brokerStream.subscribeTicks(eq(symbols), any())).thenReturn(expectedSubscriptionId);

            // When
            String result = subscribeMarketDataUseCase.execute(symbols);

            // Then
            assertThat(result).isEqualTo(expectedSubscriptionId);
            verify(brokerStream).subscribeTicks(eq(symbols), any());
        }
    }
}
