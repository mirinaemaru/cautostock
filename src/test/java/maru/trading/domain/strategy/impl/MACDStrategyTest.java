package maru.trading.domain.strategy.impl;

import maru.trading.domain.market.MarketBar;
import maru.trading.domain.signal.SignalDecision;
import maru.trading.domain.signal.SignalType;
import maru.trading.domain.strategy.StrategyContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * MACDStrategy 테스트
 *
 * 테스트 범위:
 * 1. MACD 상향 돌파 (MACD > Signal) → BUY 시그널
 * 2. MACD 하향 돌파 (MACD < Signal) → SELL 시그널
 * 3. 교차 없음 → HOLD 시그널
 * 4. 파라미터 검증
 * 5. 최소 바 부족 시 예외
 */
@DisplayName("MACDStrategy 도메인 테스트")
class MACDStrategyTest {

    private MACDStrategy strategy;
    private Map<String, Object> params;

    @BeforeEach
    void setUp() {
        params = new HashMap<>();
        params.put("fastPeriod", 12);
        params.put("slowPeriod", 26);
        params.put("signalPeriod", 9);
        params.put("ttlSeconds", 300);

        strategy = new MACDStrategy();
    }

    // ==================== 1. Bullish Crossover Tests ====================

    @Test
    @DisplayName("MACD 상향 돌파 - BUY 시그널 생성")
    void testBullishCrossover_BuySignal() {
        // Given - MACD 라인이 시그널 라인을 상향 돌파
        // 짧은 기간 파라미터 사용 (크로스오버 유발을 위해)
        Map<String, Object> shortParams = new HashMap<>();
        shortParams.put("fastPeriod", 3);
        shortParams.put("slowPeriod", 6);
        shortParams.put("signalPeriod", 3);
        shortParams.put("ttlSeconds", 300);

        List<MarketBar> bars = createBarsForBullishCrossover();
        StrategyContext context = StrategyContext.builder()
                .strategyId("STR_001")
                .symbol("005930")
                .accountId("ACC_001")
                .bars(bars)
                .params(shortParams)
                .timeframe("1m")
                .build();

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision).isNotNull();
        assertThat(decision.getSignalType()).isEqualTo(SignalType.BUY);
        assertThat(decision.getTargetType()).isEqualTo("QTY");
        assertThat(decision.getTargetValue()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(decision.getReason()).contains("bullish crossover");
        assertThat(decision.getReason()).contains("MACD");
        assertThat(decision.getTtlSeconds()).isEqualTo(300);
    }

    // ==================== 2. Bearish Crossover Tests ====================

    @Test
    @DisplayName("MACD 하향 돌파 - SELL 시그널 생성")
    void testBearishCrossover_SellSignal() {
        // Given - MACD 라인이 시그널 라인을 하향 돌파
        // 짧은 기간 파라미터 사용 (크로스오버 유발을 위해)
        Map<String, Object> shortParams = new HashMap<>();
        shortParams.put("fastPeriod", 3);
        shortParams.put("slowPeriod", 6);
        shortParams.put("signalPeriod", 3);
        shortParams.put("ttlSeconds", 300);

        List<MarketBar> bars = createBarsForBearishCrossover();
        StrategyContext context = StrategyContext.builder()
                .strategyId("STR_001")
                .symbol("005930")
                .accountId("ACC_001")
                .bars(bars)
                .params(shortParams)
                .timeframe("1m")
                .build();

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision).isNotNull();
        assertThat(decision.getSignalType()).isEqualTo(SignalType.SELL);
        assertThat(decision.getReason()).contains("bearish crossover");
        assertThat(decision.getReason()).contains("MACD");
    }

    // ==================== 3. No Crossover Tests ====================

    @Test
    @DisplayName("교차 없음 - HOLD 시그널 생성")
    void testNoCrossover_HoldSignal() {
        // Given - MACD 라인과 시그널 라인이 평행
        List<MarketBar> bars = createBarsWithNoCrossover();
        StrategyContext context = createContext(bars, params);

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision).isNotNull();
        assertThat(decision.getSignalType()).isEqualTo(SignalType.HOLD);
        assertThat(decision.getReason()).contains("No MACD crossover");
    }

    @Test
    @DisplayName("MACD가 시그널 위에 있지만 교차 없음 - HOLD")
    void testMACDAboveSignalNoCross_HoldSignal() {
        // Given - MACD > Signal이지만 이전에도 MACD > Signal
        List<MarketBar> bars = createBarsWithMACDAboveSignal();
        StrategyContext context = createContext(bars, params);

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision.getSignalType()).isEqualTo(SignalType.HOLD);
    }

    // ==================== 4. Parameter Validation Tests ====================

    @Test
    @DisplayName("파라미터 검증 - fastPeriod 누락")
    void testValidateParams_MissingFastPeriod() {
        // Given
        Map<String, Object> invalidParams = new HashMap<>();
        invalidParams.put("slowPeriod", 26);
        invalidParams.put("signalPeriod", 9);

        // When & Then
        assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fastPeriod");
    }

    @Test
    @DisplayName("파라미터 검증 - slowPeriod 누락")
    void testValidateParams_MissingSlowPeriod() {
        // Given
        Map<String, Object> invalidParams = new HashMap<>();
        invalidParams.put("fastPeriod", 12);
        invalidParams.put("signalPeriod", 9);

        // When & Then
        assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("slowPeriod");
    }

    @Test
    @DisplayName("파라미터 검증 - signalPeriod 누락")
    void testValidateParams_MissingSignalPeriod() {
        // Given
        Map<String, Object> invalidParams = new HashMap<>();
        invalidParams.put("fastPeriod", 12);
        invalidParams.put("slowPeriod", 26);

        // When & Then
        assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signalPeriod");
    }

    @Test
    @DisplayName("파라미터 검증 - fastPeriod >= slowPeriod")
    void testValidateParams_FastPeriodTooLarge() {
        // Given
        Map<String, Object> invalidParams = new HashMap<>();
        invalidParams.put("fastPeriod", 30);
        invalidParams.put("slowPeriod", 26);
        invalidParams.put("signalPeriod", 9);

        // When & Then
        assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fastPeriod must be less than slowPeriod");
    }

    @Test
    @DisplayName("파라미터 검증 - 음수 period")
    void testValidateParams_NegativePeriod() {
        // Given
        Map<String, Object> invalidParams = new HashMap<>();
        invalidParams.put("fastPeriod", -12);
        invalidParams.put("slowPeriod", 26);
        invalidParams.put("signalPeriod", 9);

        // When & Then
        assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("파라미터 검증 - 정상 파라미터")
    void testValidateParams_ValidParams() {
        // When & Then
        assertThatCode(() -> strategy.validateParams(params))
                .doesNotThrowAnyException();
    }

    // ==================== 5. Minimum Bars Validation Tests ====================

    @Test
    @DisplayName("최소 바 부족 - 예외 발생")
    void testEvaluate_InsufficientBars() {
        // Given - slowPeriod + signalPeriod + 1보다 적은 바
        List<MarketBar> bars = createBars(30); // need 26 + 9 + 1 = 36
        StrategyContext context = createContext(bars, params);

        // When & Then
        assertThatThrownBy(() -> strategy.evaluate(context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient bars");
    }

    @Test
    @DisplayName("최소 바 정확히 일치 - 정상 동작")
    void testEvaluate_ExactMinimumBars() {
        // Given - slowPeriod + signalPeriod + 1 바
        List<MarketBar> bars = createBars(36); // 26 + 9 + 1
        StrategyContext context = createContext(bars, params);

        // When & Then
        assertThatCode(() -> strategy.evaluate(context))
                .doesNotThrowAnyException();
    }

    // ==================== 6. Strategy Type Test ====================

    @Test
    @DisplayName("getStrategyType - MACD 반환")
    void testGetStrategyType() {
        // When
        String type = strategy.getStrategyType();

        // Then
        assertThat(type).isEqualTo("MACD");
    }

    // ==================== Helper Methods ====================

    private StrategyContext createContext(List<MarketBar> bars, Map<String, Object> params) {
        return StrategyContext.builder()
                .strategyId("STR_001")
                .symbol("005930")
                .accountId("ACC_001")
                .bars(bars)
                .params(params)
                .timeframe("1m")
                .build();
    }

    /**
     * 짧은 기간 파라미터(fast=3, slow=6, signal=3)에 최적화된 Bullish Crossover 데이터
     * 마지막 바에서 MACD가 Signal을 상향 돌파
     *
     * 데이터: [100, 100, 100, 100, 100, 100, 50, 50, 50, 51]
     * - Prev: MACD=-11.97 <= Signal=-9.94
     * - Curr: MACD=-9.68 > Signal=-9.81
     */
    private List<MarketBar> createBarsForBullishCrossover() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime time = LocalDateTime.now();

        // 안정 구간 (6개)
        for (int i = 0; i < 6; i++) {
            bars.add(createBar(time.minusMinutes(10 - i), 100));
        }

        // 바닥 유지 (3개)
        for (int i = 0; i < 3; i++) {
            bars.add(createBar(time.minusMinutes(4 - i), 50));
        }

        // 마지막 바: 살짝 상승으로 크로스오버 발생
        bars.add(createBar(time.minusMinutes(1), 51));

        return bars;
    }

    /**
     * 짧은 기간 파라미터(fast=3, slow=6, signal=3)에 최적화된 Bearish Crossover 데이터
     * 마지막 바에서 MACD가 Signal을 하향 돌파
     *
     * 데이터: [100, 100, 100, 100, 100, 100, 150, 150, 150, 100]
     * - Prev: MACD=11.97 >= Signal=9.94
     * - Curr: MACD=-0.82 < Signal=4.56
     */
    private List<MarketBar> createBarsForBearishCrossover() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime time = LocalDateTime.now();

        // 안정 구간 (6개)
        for (int i = 0; i < 6; i++) {
            bars.add(createBar(time.minusMinutes(10 - i), 100));
        }

        // 고점 유지 (3개)
        for (int i = 0; i < 3; i++) {
            bars.add(createBar(time.minusMinutes(4 - i), 150));
        }

        // 마지막 바: 급락으로 크로스오버 발생
        bars.add(createBar(time.minusMinutes(1), 100));

        return bars;
    }

    // 기존 테스트용 데이터 (표준 파라미터 12/26/9 용)
    private List<MarketBar> createBarsWithBullishCrossover() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime time = LocalDateTime.now();
        for (int i = 0; i < 40; i++) {
            bars.add(createBar(time.minusMinutes(40 - i), 100));
        }
        return bars;
    }

    private List<MarketBar> createBarsWithBearishCrossover() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime time = LocalDateTime.now();
        for (int i = 0; i < 40; i++) {
            bars.add(createBar(time.minusMinutes(40 - i), 100));
        }
        return bars;
    }

    private List<MarketBar> createBarsWithNoCrossover() {
        // 평행 추세: 교차 없음
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime time = LocalDateTime.now();

        // 일정한 가격 유지 (40개)
        for (int i = 0; i < 40; i++) {
            bars.add(createBar(time.minusMinutes(40 - i), 100));
        }

        return bars;
    }

    private List<MarketBar> createBarsWithMACDAboveSignal() {
        // MACD가 시그널 위에 있지만 교차 없음
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime time = LocalDateTime.now();

        // 꾸준한 상승 추세 (40개) - MACD가 계속 시그널 위
        for (int i = 0; i < 40; i++) {
            bars.add(createBar(time.minusMinutes(40 - i), 50 + i * 1.0));
        }

        return bars;
    }

    private List<MarketBar> createBars(int count) {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime time = LocalDateTime.now();

        for (int i = 0; i < count; i++) {
            bars.add(createBar(time.minusMinutes(count - i), 100));
        }

        return bars;
    }

    private MarketBar createBar(LocalDateTime timestamp, double price) {
        BigDecimal priceDecimal = BigDecimal.valueOf(price);
        return MarketBar.restore(
                "005930",
                "1m",
                timestamp,
                priceDecimal,  // open
                priceDecimal,  // high
                priceDecimal,  // low
                priceDecimal,  // close
                1000L,         // volume
                true           // closed
        );
    }
}
