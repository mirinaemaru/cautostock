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
 * VolatilityBreakoutStrategy 테스트 (래리 윌리엄스 변동성 돌파)
 *
 * 테스트 범위:
 * 1. 변동성 돌파 - BUY 시그널
 * 2. 장 마감 전 청산 - SELL 시그널
 * 3. 돌파 없음 - HOLD 시그널
 * 4. K 값 파라미터 테스트
 * 5. 최소 바 부족 시 예외
 * 6. TTL 설정 확인
 */
@DisplayName("VolatilityBreakoutStrategy 도메인 테스트")
class VolatilityBreakoutStrategyTest {

    private VolatilityBreakoutStrategy strategy;
    private Map<String, Object> params;

    @BeforeEach
    void setUp() {
        params = new HashMap<>();
        params.put("kFactor", 0.5);
        params.put("exitBeforeClose", true);
        params.put("isExitTime", false);
        params.put("ttlSeconds", 300);

        strategy = new VolatilityBreakoutStrategy();
    }

    // ==================== 1. Breakout BUY Tests ====================

    @Nested
    @DisplayName("변동성 돌파 BUY 테스트")
    class BreakoutBuyTests {

        @Test
        @DisplayName("목표가 돌파 - BUY 시그널")
        void testBreakout_BuySignal() {
            // Given
            // 전일 Range = 120 - 100 = 20
            // 목표가 = 당일 시가(105) + 20 * 0.5 = 115
            // 현재가 = 118 >= 115 → BUY
            List<MarketBar> bars = createBarsForBreakout();
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then
            assertThat(decision).isNotNull();
            assertThat(decision.getSignalType()).isEqualTo(SignalType.BUY);
            assertThat(decision.getTargetType()).isEqualTo("QTY");
            assertThat(decision.getTargetValue()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(decision.getReason()).contains("Breakout detected");
            assertThat(decision.getTtlSeconds()).isEqualTo(300);
        }

        @Test
        @DisplayName("BUY 신호 이유에 계산값 포함")
        void testBreakout_ReasonContainsCalculation() {
            // Given
            List<MarketBar> bars = createBarsForBreakout();
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then
            assertThat(decision.getReason()).contains("price=");
            assertThat(decision.getReason()).contains("target=");
            assertThat(decision.getReason()).contains("open=");
        }

        @Test
        @DisplayName("목표가 정확히 달성 - BUY 시그널")
        void testExactBreakout_BuySignal() {
            // Given
            // 전일 Range = 100 - 80 = 20
            // 목표가 = 90 + 20 * 0.5 = 100
            // 현재가 = 100 == 목표가 → BUY
            List<MarketBar> bars = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            bars.add(createBar("005930", now.minusMinutes(1),
                    BigDecimal.valueOf(85), BigDecimal.valueOf(100),
                    BigDecimal.valueOf(80), BigDecimal.valueOf(95), 1000L));
            bars.add(createBar("005930", now,
                    BigDecimal.valueOf(90), BigDecimal.valueOf(100),
                    BigDecimal.valueOf(88), BigDecimal.valueOf(100), 1500L));
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then
            assertThat(decision.getSignalType()).isEqualTo(SignalType.BUY);
        }
    }

    // ==================== 2. Exit SELL Tests ====================

    @Nested
    @DisplayName("장 마감 전 청산 SELL 테스트")
    class ExitSellTests {

        @Test
        @DisplayName("장 마감 시간 도달 - SELL 시그널")
        void testExitTime_SellSignal() {
            // Given
            params.put("exitBeforeClose", true);
            params.put("isExitTime", true);
            List<MarketBar> bars = createBarsForNoBreakout();
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then
            assertThat(decision).isNotNull();
            assertThat(decision.getSignalType()).isEqualTo(SignalType.SELL);
            assertThat(decision.getReason()).contains("Exit before market close");
        }

        @Test
        @DisplayName("exitBeforeClose=false이면 청산 안함")
        void testNoExitBeforeClose_Hold() {
            // Given
            params.put("exitBeforeClose", false);
            params.put("isExitTime", true);
            List<MarketBar> bars = createBarsForNoBreakout();
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then
            assertThat(decision.getSignalType()).isEqualTo(SignalType.HOLD);
        }

        @Test
        @DisplayName("isExitTime=false이면 청산 안함")
        void testNoExitTime_Hold() {
            // Given
            params.put("exitBeforeClose", true);
            params.put("isExitTime", false);
            List<MarketBar> bars = createBarsForNoBreakout();
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then
            assertThat(decision.getSignalType()).isEqualTo(SignalType.HOLD);
        }
    }

    // ==================== 3. Hold Signal Tests ====================

    @Nested
    @DisplayName("HOLD 시그널 테스트")
    class HoldSignalTests {

        @Test
        @DisplayName("목표가 미달성 - HOLD 시그널")
        void testNoBreakout_HoldSignal() {
            // Given
            // 전일 Range = 120 - 100 = 20
            // 목표가 = 105 + 20 * 0.5 = 115
            // 현재가 = 110 < 115 → HOLD
            List<MarketBar> bars = createBarsForNoBreakout();
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then
            assertThat(decision).isNotNull();
            assertThat(decision.getSignalType()).isEqualTo(SignalType.HOLD);
            assertThat(decision.getTargetType()).isNull();
            assertThat(decision.getTargetValue()).isNull();
            assertThat(decision.getReason()).contains("No breakout");
        }
    }

    // ==================== 4. K Factor Tests ====================

    @Nested
    @DisplayName("K 팩터 파라미터 테스트")
    class KFactorTests {

        @Test
        @DisplayName("K=0.0 - 시가와 동일한 목표가")
        void testKFactor_Zero() {
            // Given - K=0이면 목표가 = 당일 시가
            params.put("kFactor", 0.0);
            List<MarketBar> bars = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            bars.add(createBar("005930", now.minusMinutes(1),
                    BigDecimal.valueOf(90), BigDecimal.valueOf(110),
                    BigDecimal.valueOf(90), BigDecimal.valueOf(100), 1000L));
            bars.add(createBar("005930", now,
                    BigDecimal.valueOf(100), BigDecimal.valueOf(102),
                    BigDecimal.valueOf(99), BigDecimal.valueOf(101), 1000L));
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then - 현재가(101) >= 목표가(100) → BUY
            assertThat(decision.getSignalType()).isEqualTo(SignalType.BUY);
        }

        @Test
        @DisplayName("K=1.0 - 전일 Range 전체 적용")
        void testKFactor_One() {
            // Given - K=1이면 목표가 = 당일 시가 + 전일 Range
            params.put("kFactor", 1.0);
            // 전일 Range = 110 - 90 = 20
            // 목표가 = 100 + 20 * 1 = 120
            // 현재가 = 115 < 120 → HOLD
            List<MarketBar> bars = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            bars.add(createBar("005930", now.minusMinutes(1),
                    BigDecimal.valueOf(90), BigDecimal.valueOf(110),
                    BigDecimal.valueOf(90), BigDecimal.valueOf(100), 1000L));
            bars.add(createBar("005930", now,
                    BigDecimal.valueOf(100), BigDecimal.valueOf(115),
                    BigDecimal.valueOf(99), BigDecimal.valueOf(115), 1000L));
            StrategyContext context = createContext(bars, params);

            // When
            SignalDecision decision = strategy.evaluate(context);

            // Then
            assertThat(decision.getSignalType()).isEqualTo(SignalType.HOLD);
        }

        @Test
        @DisplayName("파라미터 검증 - K > 1 예외")
        void testValidateParams_KFactorTooHigh() {
            // Given
            Map<String, Object> invalidParams = new HashMap<>();
            invalidParams.put("kFactor", 1.5);

            // When & Then
            assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 0 and 1");
        }

        @Test
        @DisplayName("파라미터 검증 - K < 0 예외")
        void testValidateParams_KFactorNegative() {
            // Given
            Map<String, Object> invalidParams = new HashMap<>();
            invalidParams.put("kFactor", -0.1);

            // When & Then
            assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 0 and 1");
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
                    BigDecimal.valueOf(100), BigDecimal.valueOf(110),
                    BigDecimal.valueOf(95), BigDecimal.valueOf(105), 1000L));
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
            List<MarketBar> bars = createBarsForNoBreakout();
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
            List<MarketBar> bars = createBarsForBreakout();
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
            List<MarketBar> bars = createBarsForBreakout();
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
        assertThat(strategy.getStrategyType()).isEqualTo("VOLATILITY_BREAKOUT");
    }

    // ==================== Helper Methods ====================

    private StrategyContext createContext(List<MarketBar> bars, Map<String, Object> params) {
        return StrategyContext.builder()
                .strategyId("STR_VB_001")
                .symbol("005930")
                .accountId("ACC_001")
                .bars(bars)
                .params(params)
                .timeframe("1d")
                .build();
    }

    /**
     * 변동성 돌파 발생하는 바 데이터 생성.
     *
     * 전일: High=120, Low=100 → Range=20
     * 당일: Open=105, Close=118
     * 목표가 = 105 + 20 * 0.5 = 115
     * 현재가 = 118 >= 115 → BUY
     */
    private List<MarketBar> createBarsForBreakout() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 전일 바
        bars.add(createBar("005930", now.minusDays(1),
                BigDecimal.valueOf(105), BigDecimal.valueOf(120),
                BigDecimal.valueOf(100), BigDecimal.valueOf(115), 10000L));

        // 당일 바 - 돌파 발생
        bars.add(createBar("005930", now,
                BigDecimal.valueOf(105), BigDecimal.valueOf(120),
                BigDecimal.valueOf(103), BigDecimal.valueOf(118), 15000L));

        return bars;
    }

    /**
     * 변동성 돌파 미발생 바 데이터 생성.
     *
     * 전일: High=120, Low=100 → Range=20
     * 당일: Open=105, Close=110
     * 목표가 = 105 + 20 * 0.5 = 115
     * 현재가 = 110 < 115 → HOLD
     */
    private List<MarketBar> createBarsForNoBreakout() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 전일 바
        bars.add(createBar("005930", now.minusDays(1),
                BigDecimal.valueOf(105), BigDecimal.valueOf(120),
                BigDecimal.valueOf(100), BigDecimal.valueOf(115), 10000L));

        // 당일 바 - 돌파 미발생
        bars.add(createBar("005930", now,
                BigDecimal.valueOf(105), BigDecimal.valueOf(112),
                BigDecimal.valueOf(103), BigDecimal.valueOf(110), 8000L));

        return bars;
    }

    private MarketBar createBar(String symbol, LocalDateTime timestamp,
                                 BigDecimal open, BigDecimal high,
                                 BigDecimal low, BigDecimal close, long volume) {
        return MarketBar.restore(symbol, "1d", timestamp, open, high, low, close, volume, true);
    }
}
