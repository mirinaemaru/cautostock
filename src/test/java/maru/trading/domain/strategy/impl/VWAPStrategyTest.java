package maru.trading.domain.strategy.impl;

import maru.trading.domain.market.MarketBar;
import maru.trading.domain.signal.SignalDecision;
import maru.trading.domain.signal.SignalType;
import maru.trading.domain.strategy.StrategyContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * VWAPStrategy 테스트
 *
 * 테스트 범위:
 * 1. 가격 상향 교차 (VWAP 위로) → BUY 시그널
 * 2. 가격 하향 교차 (VWAP 아래로) → SELL 시그널
 * 3. 교차 없음 → HOLD 시그널
 * 4. crossoverThreshold 파라미터 테스트
 * 5. 최소 바 부족 시 예외
 * 6. TTL 설정 확인
 */
@DisplayName("VWAPStrategy 도메인 테스트")
class VWAPStrategyTest {

    private VWAPStrategy strategy;
    private Map<String, Object> params;

    @BeforeEach
    void setUp() {
        params = new HashMap<>();
        params.put("crossoverThreshold", 0.0);
        params.put("ttlSeconds", 300);

        strategy = new VWAPStrategy();
    }

    // ==================== 1. Upward Crossover Tests ====================

    @Nested
    @DisplayName("상향 교차 테스트")
    class UpwardCrossoverTests {

        @Test
        @DisplayName("가격이 VWAP 위로 상향 교차 - BUY 시그널")
        void testUpwardCrossover_BuySignal() {
            // Given - 가격이 VWAP 아래에서 위로 교차
            List<MarketBar> bars = createBarsForUpwardCrossover();
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then
            assertThat(decision).isNotNull();
            assertThat(decision.getSignalType()).isEqualTo(SignalType.BUY);
            assertThat(decision.getTargetType()).isEqualTo("QTY");
            assertThat(decision.getTargetValue()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(decision.getReason()).contains("crossed above VWAP");
            assertThat(decision.getTtlSeconds()).isEqualTo(300);
        }

        @Test
        @DisplayName("BUY 신호 이유에 가격과 VWAP 값 포함")
        void testUpwardCrossover_ReasonContainsValues() {
            // Given
            List<MarketBar> bars = createBarsForUpwardCrossover();
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then
            assertThat(decision.getReason()).contains("price=");
            assertThat(decision.getReason()).contains("VWAP=");
        }
    }

    // ==================== 2. Downward Crossover Tests ====================

    @Nested
    @DisplayName("하향 교차 테스트")
    class DownwardCrossoverTests {

        @Test
        @DisplayName("가격이 VWAP 아래로 하향 교차 - SELL 시그널")
        void testDownwardCrossover_SellSignal() {
            // Given - 가격이 VWAP 위에서 아래로 교차
            List<MarketBar> bars = createBarsForDownwardCrossover();
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then
            assertThat(decision).isNotNull();
            assertThat(decision.getSignalType()).isEqualTo(SignalType.SELL);
            assertThat(decision.getTargetType()).isEqualTo("QTY");
            assertThat(decision.getTargetValue()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(decision.getReason()).contains("crossed below VWAP");
            assertThat(decision.getTtlSeconds()).isEqualTo(300);
        }
    }

    // ==================== 3. Hold Signal Tests ====================

    @Nested
    @DisplayName("HOLD 시그널 테스트")
    class HoldSignalTests {

        @Test
        @DisplayName("교차 없음 - HOLD 시그널")
        void testNoCrossover_HoldSignal() {
            // Given - 가격이 계속 VWAP 위에 있음 (교차 없음)
            List<MarketBar> bars = createBarsWithNoCrossover();
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then
            assertThat(decision).isNotNull();
            assertThat(decision.getSignalType()).isEqualTo(SignalType.HOLD);
            assertThat(decision.getTargetType()).isNull();
            assertThat(decision.getTargetValue()).isNull();
            assertThat(decision.getReason()).contains("No VWAP crossover");
        }
    }

    // ==================== 4. Parameter Tests ====================

    @Nested
    @DisplayName("파라미터 테스트")
    class ParameterTests {

        @Test
        @DisplayName("crossoverThreshold 적용 테스트")
        void testCrossoverThreshold() {
            // Given - 5% threshold 설정
            params.put("crossoverThreshold", 5.0);
            List<MarketBar> bars = createBarsWithNoCrossover();
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then - threshold로 인해 HOLD
            assertThat(decision.getSignalType()).isEqualTo(SignalType.HOLD);
        }

        @Test
        @DisplayName("파라미터 검증 - 음수 threshold 예외")
        void testValidateParams_NegativeThreshold() {
            // Given
            Map<String, Object> invalidParams = new HashMap<>();
            invalidParams.put("crossoverThreshold", -1.0);

            // When & Then
            assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be negative");
        }

        @Test
        @DisplayName("파라미터 검증 - 음수 TTL 예외")
        void testValidateParams_NegativeTTL() {
            // Given
            Map<String, Object> invalidParams = new HashMap<>();
            invalidParams.put("ttlSeconds", -100);

            // When & Then
            assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be positive");
        }
    }

    // ==================== 5. Minimum Bars Tests ====================

    @Nested
    @DisplayName("최소 바 검증 테스트")
    class MinimumBarsTests {

        @Test
        @DisplayName("최소 바 부족 시 예외 - 2개 필요")
        void testInsufficientBars_ThrowsException() {
            // Given - 1개 바만 제공
            List<MarketBar> bars = new ArrayList<>();
            bars.add(createBar("005930", LocalDateTime.now(),
                    BigDecimal.valueOf(100), BigDecimal.valueOf(105),
                    BigDecimal.valueOf(98), BigDecimal.valueOf(103), 1000L));
            StrategyContext context = createContext(bars, params);

            // When & Then
            assertThatThrownBy(() -> strategy.evaluate(context))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insufficient bars")
                    .hasMessageContaining("need 2, got 1");
        }

        @Test
        @DisplayName("최소 바 충족 시 정상 실행")
        void testExactMinimumBars_Success() {
            // Given - 정확히 2개 바
            List<MarketBar> bars = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            bars.add(createBar("005930", now.minusMinutes(1),
                    BigDecimal.valueOf(100), BigDecimal.valueOf(105),
                    BigDecimal.valueOf(98), BigDecimal.valueOf(103), 1000L));
            bars.add(createBar("005930", now,
                    BigDecimal.valueOf(103), BigDecimal.valueOf(108),
                    BigDecimal.valueOf(101), BigDecimal.valueOf(106), 1500L));
            StrategyContext context = createContext(bars, params);

            // When & Then
            assertThatCode(() -> strategy.evaluate(context))
                    .doesNotThrowAnyException();
        }
    }

    // ==================== 6. TTL Tests ====================

    @Nested
    @DisplayName("TTL 설정 테스트")
    class TTLTests {

        @Test
        @DisplayName("TTL 파라미터로 설정")
        void testTTL_FromParams() {
            // Given
            params.put("ttlSeconds", 600);
            List<MarketBar> bars = createBarsForUpwardCrossover();
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then
            assertThat(decision.getTtlSeconds()).isEqualTo(600);
        }

        @Test
        @DisplayName("TTL 기본값 300초")
        void testTTL_Default() {
            // Given
            params.remove("ttlSeconds");
            List<MarketBar> bars = createBarsForUpwardCrossover();
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then
            assertThat(decision.getTtlSeconds()).isEqualTo(300);
        }
    }

    // ==================== 7. Strategy Type Test ====================

    @Test
    @DisplayName("전략 타입 확인")
    void testGetStrategyType() {
        assertThat(strategy.getStrategyType()).isEqualTo("VWAP");
    }

    // ==================== Helper Methods ====================

    private StrategyContext createContext(List<MarketBar> bars, Map<String, Object> params) {
        return StrategyContext.builder()
                .strategyId("STR_VWAP_001")
                .symbol("005930")
                .accountId("ACC_001")
                .bars(bars)
                .params(params)
                .timeframe("1m")
                .build();
    }

    /**
     * 상향 교차 발생하는 바 데이터 생성.
     * 첫 번째 바: 가격 < VWAP
     * 두 번째 바: 가격 > VWAP
     */
    private List<MarketBar> createBarsForUpwardCrossover() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 첫 번째 바: 가격이 낮고 거래량이 많아 VWAP가 높음
        // VWAP = (105 + 100 + 102) / 3 = 102.33, close = 102
        bars.add(createBar("005930", now.minusMinutes(1),
                BigDecimal.valueOf(100), BigDecimal.valueOf(105),
                BigDecimal.valueOf(100), BigDecimal.valueOf(102), 10000L));

        // 두 번째 바: 가격이 높아져 VWAP 위로 교차
        // Typical = (120 + 110 + 118) / 3 = 116
        // Cumulative VWAP ≈ 110, close = 118 > VWAP
        bars.add(createBar("005930", now,
                BigDecimal.valueOf(110), BigDecimal.valueOf(120),
                BigDecimal.valueOf(110), BigDecimal.valueOf(118), 5000L));

        return bars;
    }

    /**
     * 하향 교차 발생하는 바 데이터 생성.
     * 첫 번째 바: 가격 > VWAP
     * 두 번째 바: 가격 < VWAP
     */
    private List<MarketBar> createBarsForDownwardCrossover() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 첫 번째 바: 가격이 높고 거래량이 많아 VWAP가 낮음
        // VWAP = (105 + 95 + 103) / 3 = 101, close = 103 > VWAP
        bars.add(createBar("005930", now.minusMinutes(1),
                BigDecimal.valueOf(95), BigDecimal.valueOf(105),
                BigDecimal.valueOf(95), BigDecimal.valueOf(103), 10000L));

        // 두 번째 바: 가격이 낮아져 VWAP 아래로 교차
        // Typical = (90 + 80 + 82) / 3 = 84
        // close = 82 < cumulative VWAP
        bars.add(createBar("005930", now,
                BigDecimal.valueOf(85), BigDecimal.valueOf(90),
                BigDecimal.valueOf(80), BigDecimal.valueOf(82), 5000L));

        return bars;
    }

    /**
     * 교차 없는 바 데이터 생성 (가격이 계속 VWAP 근처에서 유지).
     */
    private List<MarketBar> createBarsWithNoCrossover() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 두 바 모두 비슷한 가격대 유지
        bars.add(createBar("005930", now.minusMinutes(1),
                BigDecimal.valueOf(100), BigDecimal.valueOf(102),
                BigDecimal.valueOf(99), BigDecimal.valueOf(101), 1000L));

        bars.add(createBar("005930", now,
                BigDecimal.valueOf(101), BigDecimal.valueOf(103),
                BigDecimal.valueOf(100), BigDecimal.valueOf(102), 1000L));

        return bars;
    }

    private MarketBar createBar(String symbol, LocalDateTime timestamp,
                                 BigDecimal open, BigDecimal high,
                                 BigDecimal low, BigDecimal close, long volume) {
        return MarketBar.restore(symbol, "1m", timestamp, open, high, low, close, volume, true);
    }
}
