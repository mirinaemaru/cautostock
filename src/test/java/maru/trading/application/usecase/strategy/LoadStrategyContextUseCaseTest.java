package maru.trading.application.usecase.strategy;

import maru.trading.application.ports.repo.BarRepository;
import maru.trading.domain.market.MarketBar;
import maru.trading.domain.strategy.Strategy;
import maru.trading.domain.strategy.StrategyContext;
import maru.trading.domain.strategy.StrategyVersion;
import maru.trading.infra.cache.BarCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoadStrategyContextUseCase Test")
class LoadStrategyContextUseCaseTest {

    @Mock
    private BarRepository barRepository;

    @Mock
    private BarCache barCache;

    @InjectMocks
    private LoadStrategyContextUseCase loadStrategyContextUseCase;

    private Strategy testStrategy;
    private StrategyVersion testVersion;
    private List<MarketBar> testBars;

    @BeforeEach
    void setUp() {
        testStrategy = Strategy.builder()
                .strategyId("STRAT_001")
                .name("MA Crossover")
                .status("ACTIVE")
                .build();

        testVersion = StrategyVersion.builder()
                .versionNo(1)
                .paramsJson("{\"shortPeriod\":5,\"longPeriod\":20}")
                .build();

        testBars = createTestBars(35);
    }

    private List<MarketBar> createTestBars(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> MarketBar.restore(
                        "005930",
                        "1m",
                        LocalDateTime.now().minusMinutes(count - i),
                        BigDecimal.valueOf(70000 + i * 100),
                        BigDecimal.valueOf(70500 + i * 100),
                        BigDecimal.valueOf(69500 + i * 100),
                        BigDecimal.valueOf(70200 + i * 100),
                        10000L,
                        true))
                .toList();
    }

    @Nested
    @DisplayName("Execute Tests")
    class ExecuteTests {

        @Test
        @DisplayName("Should load context with bars from cache")
        void shouldLoadContextWithBarsFromCache() {
            // Given
            String symbol = "005930";
            String accountId = "ACC_001";

            when(barCache.getRecentBars(eq(symbol), eq("1m"), anyInt())).thenReturn(testBars);

            // When
            StrategyContext result = loadStrategyContextUseCase.execute(
                    testStrategy, testVersion, symbol, accountId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStrategyId()).isEqualTo("STRAT_001");
            assertThat(result.getSymbol()).isEqualTo(symbol);
            assertThat(result.getAccountId()).isEqualTo(accountId);
            assertThat(result.getBars()).hasSize(35);
            assertThat(result.getTimeframe()).isEqualTo("1m");
            verify(barCache).getRecentBars(eq(symbol), eq("1m"), anyInt());
            verify(barRepository, never()).findRecentClosedBars(any(), any(), anyInt());
        }

        @Test
        @DisplayName("Should fallback to database when cache is empty")
        void shouldFallbackToDatabaseWhenCacheIsEmpty() {
            // Given
            String symbol = "005930";
            String accountId = "ACC_001";

            when(barCache.getRecentBars(any(), any(), anyInt())).thenReturn(Collections.emptyList());
            when(barRepository.findRecentClosedBars(eq(symbol), eq("1m"), anyInt())).thenReturn(testBars);

            // When
            StrategyContext result = loadStrategyContextUseCase.execute(
                    testStrategy, testVersion, symbol, accountId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getBars()).hasSize(35);
            verify(barCache).getRecentBars(eq(symbol), eq("1m"), anyInt());
            verify(barRepository).findRecentClosedBars(eq(symbol), eq("1m"), anyInt());
        }

        @Test
        @DisplayName("Should fallback to database when cache has insufficient bars")
        void shouldFallbackToDatabaseWhenCacheHasInsufficientBars() {
            // Given
            String symbol = "005930";
            String accountId = "ACC_001";
            List<MarketBar> insufficientBars = createTestBars(10);

            when(barCache.getRecentBars(any(), any(), anyInt())).thenReturn(insufficientBars);
            when(barRepository.findRecentClosedBars(eq(symbol), eq("1m"), anyInt())).thenReturn(testBars);

            // When
            StrategyContext result = loadStrategyContextUseCase.execute(
                    testStrategy, testVersion, symbol, accountId);

            // Then
            assertThat(result).isNotNull();
            verify(barRepository).findRecentClosedBars(eq(symbol), eq("1m"), anyInt());
        }

        @Test
        @DisplayName("Should return empty bars when both cache and database fail")
        void shouldReturnEmptyBarsWhenBothCacheAndDatabaseFail() {
            // Given
            String symbol = "005930";
            String accountId = "ACC_001";

            when(barCache.getRecentBars(any(), any(), anyInt())).thenReturn(Collections.emptyList());
            when(barRepository.findRecentClosedBars(any(), any(), anyInt()))
                    .thenThrow(new RuntimeException("Database error"));

            // When
            StrategyContext result = loadStrategyContextUseCase.execute(
                    testStrategy, testVersion, symbol, accountId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getBars()).isEmpty();
        }

        @Test
        @DisplayName("Should parse strategy parameters correctly")
        void shouldParseStrategyParametersCorrectly() {
            // Given
            String symbol = "005930";
            String accountId = "ACC_001";

            when(barCache.getRecentBars(any(), any(), anyInt())).thenReturn(testBars);

            // When
            StrategyContext result = loadStrategyContextUseCase.execute(
                    testStrategy, testVersion, symbol, accountId);

            // Then
            assertThat(result.getParams()).containsEntry("shortPeriod", 5);
            assertThat(result.getParams()).containsEntry("longPeriod", 20);
        }
    }

    @Nested
    @DisplayName("Bar Count Calculation Tests")
    class BarCountCalculationTests {

        @Test
        @DisplayName("Should calculate required bars based on longPeriod")
        void shouldCalculateRequiredBarsBasedOnLongPeriod() {
            // Given
            StrategyVersion longPeriodVersion = StrategyVersion.builder()
                    .versionNo(1)
                    .paramsJson("{\"longPeriod\":50}")
                    .build();

            when(barCache.getRecentBars(any(), any(), anyInt())).thenReturn(testBars);

            // When
            loadStrategyContextUseCase.execute(testStrategy, longPeriodVersion, "005930", "ACC_001");

            // Then
            // longPeriod(50) + buffer(10) + 1 = 61 bars requested
            verify(barCache).getRecentBars(eq("005930"), eq("1m"), eq(61));
        }

        @Test
        @DisplayName("Should calculate required bars based on period parameter")
        void shouldCalculateRequiredBarsBasedOnPeriodParameter() {
            // Given
            StrategyVersion rsiVersion = StrategyVersion.builder()
                    .versionNo(1)
                    .paramsJson("{\"period\":14}")
                    .build();

            when(barCache.getRecentBars(any(), any(), anyInt())).thenReturn(testBars);

            // When
            loadStrategyContextUseCase.execute(testStrategy, rsiVersion, "005930", "ACC_001");

            // Then
            // period(14) + buffer(10) + 1 = 25 bars requested
            verify(barCache).getRecentBars(eq("005930"), eq("1m"), eq(25));
        }

        @Test
        @DisplayName("Should cap required bars at maximum lookback")
        void shouldCapRequiredBarsAtMaximumLookback() {
            // Given
            StrategyVersion hugePeriodVersion = StrategyVersion.builder()
                    .versionNo(1)
                    .paramsJson("{\"longPeriod\":200}")
                    .build();

            when(barCache.getRecentBars(any(), any(), anyInt())).thenReturn(testBars);

            // When
            loadStrategyContextUseCase.execute(testStrategy, hugePeriodVersion, "005930", "ACC_001");

            // Then
            // Capped at DEFAULT_BAR_LOOKBACK (100)
            verify(barCache).getRecentBars(eq("005930"), eq("1m"), eq(100));
        }

        @Test
        @DisplayName("Should handle empty parameters")
        void shouldHandleEmptyParameters() {
            // Given
            StrategyVersion emptyParamsVersion = StrategyVersion.builder()
                    .versionNo(1)
                    .paramsJson("{}")
                    .build();

            when(barCache.getRecentBars(any(), any(), anyInt())).thenReturn(testBars);

            // When
            StrategyContext result = loadStrategyContextUseCase.execute(
                    testStrategy, emptyParamsVersion, "005930", "ACC_001");

            // Then
            assertThat(result.getParams()).isEmpty();
            // 0 + buffer(10) + 1 = 11 bars
            verify(barCache).getRecentBars(eq("005930"), eq("1m"), eq(11));
        }
    }
}
