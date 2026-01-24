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
 * PairTradingStrategy 테스트 (단일 종목 평균회귀)
 *
 * 테스트 범위:
 * 1. Z-Score < -entryZScore → BUY 시그널 (평균 이하 이탈)
 * 2. Z-Score > +entryZScore → SELL 시그널 (평균 이상 이탈)
 * 3. |Z-Score| <= entryZScore → HOLD 시그널
 * 4. lookbackPeriod, entryZScore, exitZScore 파라미터 테스트
 * 5. 최소 바 부족 시 예외
 * 6. TTL 설정 확인
 */
@DisplayName("PairTradingStrategy 도메인 테스트")
class PairTradingStrategyTest {

    private PairTradingStrategy strategy;
    private Map<String, Object> params;

    @BeforeEach
    void setUp() {
        params = new HashMap<>();
        params.put("lookbackPeriod", 5);  // 테스트용으로 짧게 설정
        params.put("entryZScore", 1.5);   // 테스트용으로 임계값 낮춤
        params.put("exitZScore", 0.5);
        params.put("ttlSeconds", 300);

        strategy = new PairTradingStrategy();
    }

    // ==================== 1. BUY Signal Tests (Z-Score < -entry) ====================

    @Nested
    @DisplayName("평균 이하 이탈 BUY 테스트")
    class BuySignalTests {

        @Test
        @DisplayName("Z-Score < -entryZScore - BUY 시그널")
        void testNegativeZScore_BuySignal() {
            // Given - 가격이 평균보다 크게 낮음
            List<MarketBar> bars = createBarsForNegativeZScore();
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then
            assertThat(decision).isNotNull();
            assertThat(decision.getSignalType()).isEqualTo(SignalType.BUY);
            assertThat(decision.getTargetType()).isEqualTo("QTY");
            assertThat(decision.getTargetValue()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(decision.getReason()).contains("Mean reversion BUY");
            assertThat(decision.getTtlSeconds()).isEqualTo(300);
        }

        @Test
        @DisplayName("BUY 신호 이유에 Z-Score 값 포함")
        void testBuySignal_ReasonContainsZScore() {
            // Given
            List<MarketBar> bars = createBarsForNegativeZScore();
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then
            assertThat(decision.getReason()).contains("Z-Score=");
            assertThat(decision.getReason()).contains("price=");
            assertThat(decision.getReason()).contains("mean=");
        }
    }

    // ==================== 2. SELL Signal Tests (Z-Score > +entry) ====================

    @Nested
    @DisplayName("평균 이상 이탈 SELL 테스트")
    class SellSignalTests {

        @Test
        @DisplayName("Z-Score > +entryZScore - SELL 시그널")
        void testPositiveZScore_SellSignal() {
            // Given - 가격이 평균보다 크게 높음
            List<MarketBar> bars = createBarsForPositiveZScore();
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then
            assertThat(decision).isNotNull();
            assertThat(decision.getSignalType()).isEqualTo(SignalType.SELL);
            assertThat(decision.getTargetType()).isEqualTo("QTY");
            assertThat(decision.getTargetValue()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(decision.getReason()).contains("Mean reversion SELL");
            assertThat(decision.getTtlSeconds()).isEqualTo(300);
        }
    }

    // ==================== 3. HOLD Signal Tests ====================

    @Nested
    @DisplayName("HOLD 시그널 테스트")
    class HoldSignalTests {

        @Test
        @DisplayName("Z-Score 중립 - HOLD 시그널")
        void testNeutralZScore_HoldSignal() {
            // Given - 가격이 평균 근처
            List<MarketBar> bars = createBarsForNeutralZScore();
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then
            assertThat(decision).isNotNull();
            assertThat(decision.getSignalType()).isEqualTo(SignalType.HOLD);
            assertThat(decision.getTargetType()).isNull();
            assertThat(decision.getTargetValue()).isNull();
            assertThat(decision.getReason()).contains("No mean reversion signal");
        }

        @Test
        @DisplayName("Z-Score = 0 (모든 가격 동일) - HOLD 시그널")
        void testZeroZScore_HoldSignal() {
            // Given - 모든 가격이 동일
            List<MarketBar> bars = createBarsWithConstantPrice(5, BigDecimal.valueOf(100));
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then
            assertThat(decision.getSignalType()).isEqualTo(SignalType.HOLD);
        }
    }

    // ==================== 4. Parameter Tests ====================

    @Nested
    @DisplayName("파라미터 테스트")
    class ParameterTests {

        @Test
        @DisplayName("lookbackPeriod 변경 테스트")
        void testLookbackPeriod() {
            // Given - lookbackPeriod를 3으로 변경
            params.put("lookbackPeriod", 3);
            List<MarketBar> bars = createBarsForNegativeZScore();
            StrategyContext context = createContext(bars, params);

            // When & Then - 정상 실행
            assertThatCode(() -> strategy.evaluate(context))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("entryZScore 변경 테스트 - 높은 임계값으로 HOLD")
        void testEntryZScore_HighThreshold() {
            // Given - entryZScore를 10으로 설정 (일반적인 이탈로는 도달 불가)
            params.put("entryZScore", 10.0);
            List<MarketBar> bars = createBarsForPositiveZScore();
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then - 높은 임계값으로 인해 HOLD
            assertThat(decision.getSignalType()).isEqualTo(SignalType.HOLD);
        }

        @Test
        @DisplayName("파라미터 검증 - 음수 lookbackPeriod 예외")
        void testValidateParams_NegativeLookbackPeriod() {
            // Given
            Map<String, Object> invalidParams = new HashMap<>();
            invalidParams.put("lookbackPeriod", -5);

            // When & Then
            assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be positive");
        }

        @Test
        @DisplayName("파라미터 검증 - 음수 entryZScore 예외")
        void testValidateParams_NegativeEntryZScore() {
            // Given
            Map<String, Object> invalidParams = new HashMap<>();
            invalidParams.put("entryZScore", -1.0);

            // When & Then
            assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be positive");
        }

        @Test
        @DisplayName("파라미터 검증 - exitZScore >= entryZScore 예외")
        void testValidateParams_ExitGreaterThanEntry() {
            // Given
            Map<String, Object> invalidParams = new HashMap<>();
            invalidParams.put("entryZScore", 2.0);
            invalidParams.put("exitZScore", 2.5);

            // When & Then
            assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exitZScore must be less than entryZScore");
        }
    }

    // ==================== 5. Minimum Bars Tests ====================

    @Nested
    @DisplayName("최소 바 검증 테스트")
    class MinimumBarsTests {

        @Test
        @DisplayName("최소 바 부족 시 예외 - lookbackPeriod개 필요")
        void testInsufficientBars_ThrowsException() {
            // Given - lookbackPeriod=5인데 3개 바만 제공
            List<MarketBar> bars = createBarsWithConstantPrice(3, BigDecimal.valueOf(100));
            StrategyContext context = createContext(bars, params);

            // When & Then
            assertThatThrownBy(() -> strategy.evaluate(context))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insufficient bars")
                    .hasMessageContaining("need 5, got 3");
        }

        @Test
        @DisplayName("최소 바 충족 시 정상 실행")
        void testExactMinimumBars_Success() {
            // Given - 정확히 5개 바
            List<MarketBar> bars = createBarsWithConstantPrice(5, BigDecimal.valueOf(100));
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
            List<MarketBar> bars = createBarsForNegativeZScore();
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
            List<MarketBar> bars = createBarsForNegativeZScore();
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
        assertThat(strategy.getStrategyType()).isEqualTo("PAIR_TRADING");
    }

    // ==================== 8. Default Parameter Tests ====================

    @Nested
    @DisplayName("기본값 파라미터 테스트")
    class DefaultParameterTests {

        @Test
        @DisplayName("파라미터 없이 기본값 사용")
        void testDefaultParameters() {
            // Given - 기본값: lookbackPeriod=20, entryZScore=2.0, exitZScore=0.5
            Map<String, Object> emptyParams = new HashMap<>();
            List<MarketBar> bars = createBarsWithConstantPrice(20, BigDecimal.valueOf(100));
            StrategyContext context = StrategyContext.builder()
                    .strategyId("STR_PT_001")
                    .symbol("005930")
                    .accountId("ACC_001")
                    .bars(bars)
                    .params(emptyParams)
                    .timeframe("1m")
                    .build();

            // When & Then - 기본값으로 정상 실행
            assertThatCode(() -> strategy.evaluate(context))
                    .doesNotThrowAnyException();
        }
    }

    // ==================== Helper Methods ====================

    private StrategyContext createContext(List<MarketBar> bars, Map<String, Object> params) {
        return StrategyContext.builder()
                .strategyId("STR_PT_001")
                .symbol("005930")
                .accountId("ACC_001")
                .bars(bars)
                .params(params)
                .timeframe("1m")
                .build();
    }

    /**
     * 음의 Z-Score 발생하는 바 데이터 생성 (가격이 평균보다 크게 낮음).
     *
     * 가격: 100, 100, 100, 100, 60
     * 평균 = (100 + 100 + 100 + 100 + 60) / 5 = 92
     * 현재가 = 60 < 평균
     * 표준편차 계산 후 Z-Score < -2 → BUY
     */
    private List<MarketBar> createBarsForNegativeZScore() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 4개의 높은 가격
        for (int i = 0; i < 4; i++) {
            bars.add(createBar("005930", now.minusMinutes(5 - i), BigDecimal.valueOf(100)));
        }
        // 마지막 가격은 크게 낮음
        bars.add(createBar("005930", now, BigDecimal.valueOf(60)));

        return bars;
    }

    /**
     * 양의 Z-Score 발생하는 바 데이터 생성 (가격이 평균보다 크게 높음).
     *
     * 가격: 100, 100, 100, 100, 140
     * 평균 = (100 + 100 + 100 + 100 + 140) / 5 = 108
     * 현재가 = 140 > 평균
     * 표준편차 계산 후 Z-Score > +2 → SELL
     */
    private List<MarketBar> createBarsForPositiveZScore() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 4개의 낮은 가격
        for (int i = 0; i < 4; i++) {
            bars.add(createBar("005930", now.minusMinutes(5 - i), BigDecimal.valueOf(100)));
        }
        // 마지막 가격은 크게 높음
        bars.add(createBar("005930", now, BigDecimal.valueOf(140)));

        return bars;
    }

    /**
     * 중립 Z-Score 발생하는 바 데이터 생성 (가격이 평균 근처).
     *
     * 가격이 소폭 변동하며 Z-Score가 임계값 내에 있음
     */
    private List<MarketBar> createBarsForNeutralZScore() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 가격이 소폭 변동
        bars.add(createBar("005930", now.minusMinutes(4), BigDecimal.valueOf(99)));
        bars.add(createBar("005930", now.minusMinutes(3), BigDecimal.valueOf(101)));
        bars.add(createBar("005930", now.minusMinutes(2), BigDecimal.valueOf(100)));
        bars.add(createBar("005930", now.minusMinutes(1), BigDecimal.valueOf(102)));
        bars.add(createBar("005930", now, BigDecimal.valueOf(100)));

        return bars;
    }

    /**
     * 일정한 가격으로 바 데이터 생성.
     */
    private List<MarketBar> createBarsWithConstantPrice(int count, BigDecimal price) {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < count; i++) {
            bars.add(createBar("005930", now.minusMinutes(count - i), price));
        }

        return bars;
    }

    private MarketBar createBar(String symbol, LocalDateTime timestamp, BigDecimal price) {
        return MarketBar.restore(symbol, "1m", timestamp, price, price, price, price, 100L, true);
    }
}
