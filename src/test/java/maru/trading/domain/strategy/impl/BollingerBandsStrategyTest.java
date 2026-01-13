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
 * BollingerBandsStrategy 테스트
 *
 * 테스트 범위:
 * 1. 하단 밴드 터치/돌파 → BUY 시그널
 * 2. 상단 밴드 터치/돌파 → SELL 시그널
 * 3. 밴드 내부 → HOLD 시그널
 * 4. 파라미터 검증
 * 5. 최소 바 부족 시 예외
 */
@DisplayName("BollingerBandsStrategy 도메인 테스트")
class BollingerBandsStrategyTest {

    private BollingerBandsStrategy strategy;
    private Map<String, Object> params;

    @BeforeEach
    void setUp() {
        params = new HashMap<>();
        params.put("period", 20);
        params.put("stdDevMultiplier", 2.0);
        params.put("ttlSeconds", 300);

        strategy = new BollingerBandsStrategy();
    }

    // ==================== 1. Lower Band Touch Tests ====================

    @Test
    @DisplayName("하단 밴드 터치 - BUY 시그널 생성")
    void testLowerBandTouch_BuySignal() {
        // Given - 가격이 하단 밴드에 터치
        List<MarketBar> bars = createBarsWithLowerBandTouch();
        StrategyContext context = StrategyContext.builder()
                .strategyId("STR_001")
                .symbol("005930")
                .accountId("ACC_001")
                .bars(bars)
                .params(params)
                .timeframe("1m")
                .build();

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision).isNotNull();
        assertThat(decision.getSignalType()).isEqualTo(SignalType.BUY);
        assertThat(decision.getTargetType()).isEqualTo("QTY");
        assertThat(decision.getTargetValue()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(decision.getReason()).contains("lower band");
        assertThat(decision.getReason()).contains("oversold");
        assertThat(decision.getTtlSeconds()).isEqualTo(300);
    }

    @Test
    @DisplayName("하단 밴드 아래 - BUY 시그널 생성")
    void testBelowLowerBand_BuySignal() {
        // Given - 가격이 하단 밴드 아래
        List<MarketBar> bars = createBarsWithPriceBelowLowerBand();
        StrategyContext context = createContext(bars, params);

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision.getSignalType()).isEqualTo(SignalType.BUY);
        assertThat(decision.getReason()).contains("lower band");
    }

    // ==================== 2. Upper Band Touch Tests ====================

    @Test
    @DisplayName("상단 밴드 터치 - SELL 시그널 생성")
    void testUpperBandTouch_SellSignal() {
        // Given - 가격이 상단 밴드에 터치
        List<MarketBar> bars = createBarsWithUpperBandTouch();
        StrategyContext context = createContext(bars, params);

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision).isNotNull();
        assertThat(decision.getSignalType()).isEqualTo(SignalType.SELL);
        assertThat(decision.getReason()).contains("upper band");
        assertThat(decision.getReason()).contains("overbought");
    }

    @Test
    @DisplayName("상단 밴드 위 - SELL 시그널 생성")
    void testAboveUpperBand_SellSignal() {
        // Given - 가격이 상단 밴드 위
        List<MarketBar> bars = createBarsWithPriceAboveUpperBand();
        StrategyContext context = createContext(bars, params);

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision.getSignalType()).isEqualTo(SignalType.SELL);
        assertThat(decision.getReason()).contains("upper band");
    }

    // ==================== 3. Within Bands Tests ====================

    @Test
    @DisplayName("밴드 내부 - HOLD 시그널 생성")
    void testWithinBands_HoldSignal() {
        // Given - 가격이 밴드 내부
        List<MarketBar> bars = createBarsWithPriceWithinBands();
        StrategyContext context = createContext(bars, params);

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision).isNotNull();
        assertThat(decision.getSignalType()).isEqualTo(SignalType.HOLD);
        assertThat(decision.getReason()).contains("within bands");
    }

    // ==================== 4. Parameter Validation Tests ====================

    @Test
    @DisplayName("파라미터 검증 - period 누락")
    void testValidateParams_MissingPeriod() {
        // Given
        Map<String, Object> invalidParams = new HashMap<>();
        invalidParams.put("stdDevMultiplier", 2.0);

        // When & Then
        assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("period");
    }

    @Test
    @DisplayName("파라미터 검증 - stdDevMultiplier 누락")
    void testValidateParams_MissingStdDevMultiplier() {
        // Given
        Map<String, Object> invalidParams = new HashMap<>();
        invalidParams.put("period", 20);

        // When & Then
        assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stdDevMultiplier");
    }

    @Test
    @DisplayName("파라미터 검증 - period가 음수")
    void testValidateParams_NegativePeriod() {
        // Given
        Map<String, Object> invalidParams = new HashMap<>();
        invalidParams.put("period", -20);
        invalidParams.put("stdDevMultiplier", 2.0);

        // When & Then
        assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("파라미터 검증 - stdDevMultiplier가 0 이하")
    void testValidateParams_ZeroStdDevMultiplier() {
        // Given
        Map<String, Object> invalidParams = new HashMap<>();
        invalidParams.put("period", 20);
        invalidParams.put("stdDevMultiplier", 0.0);

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
        // Given - period보다 적은 바
        List<MarketBar> bars = createBars(10); // period=20이므로 부족
        StrategyContext context = createContext(bars, params);

        // When & Then
        assertThatThrownBy(() -> strategy.evaluate(context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient bars");
    }

    @Test
    @DisplayName("최소 바 정확히 일치 - 정상 동작")
    void testEvaluate_ExactMinimumBars() {
        // Given - period + 1 바 (최소 요구사항)
        List<MarketBar> bars = createBars(21); // period=20, need 21
        StrategyContext context = createContext(bars, params);

        // When & Then
        assertThatCode(() -> strategy.evaluate(context))
                .doesNotThrowAnyException();
    }

    // ==================== 6. Strategy Type Test ====================

    @Test
    @DisplayName("getStrategyType - BOLLINGER_BANDS 반환")
    void testGetStrategyType() {
        // When
        String type = strategy.getStrategyType();

        // Then
        assertThat(type).isEqualTo("BOLLINGER_BANDS");
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

    private List<MarketBar> createBarsWithLowerBandTouch() {
        // 평균 100, 표준편차 5라면 Lower Band = 100 - 2*5 = 90
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime time = LocalDateTime.now();

        // 20개 바: 평균 100으로 안정
        for (int i = 0; i < 20; i++) {
            bars.add(createBar(time.minusMinutes(20 - i), 100));
        }

        // 마지막 바: 가격 90 (하단 밴드 터치)
        bars.add(createBar(time, 90));

        return bars;
    }

    private List<MarketBar> createBarsWithPriceBelowLowerBand() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime time = LocalDateTime.now();

        // 20개 바: 평균 100
        for (int i = 0; i < 20; i++) {
            bars.add(createBar(time.minusMinutes(20 - i), 100));
        }

        // 마지막 바: 가격 85 (하단 밴드 아래)
        bars.add(createBar(time, 85));

        return bars;
    }

    private List<MarketBar> createBarsWithUpperBandTouch() {
        // Upper Band = 100 + 2*5 = 110
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime time = LocalDateTime.now();

        // 20개 바: 평균 100
        for (int i = 0; i < 20; i++) {
            bars.add(createBar(time.minusMinutes(20 - i), 100));
        }

        // 마지막 바: 가격 110 (상단 밴드 터치)
        bars.add(createBar(time, 110));

        return bars;
    }

    private List<MarketBar> createBarsWithPriceAboveUpperBand() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime time = LocalDateTime.now();

        // 20개 바: 평균 100
        for (int i = 0; i < 20; i++) {
            bars.add(createBar(time.minusMinutes(20 - i), 100));
        }

        // 마지막 바: 가격 115 (상단 밴드 위)
        bars.add(createBar(time, 115));

        return bars;
    }

    private List<MarketBar> createBarsWithPriceWithinBands() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime time = LocalDateTime.now();

        // 20개 바: 가격 변동 (평균 100, 표준편차 > 0)
        double[] prices = {
            95, 98, 102, 100, 97, 103, 99, 101, 96, 104,
            98, 102, 100, 99, 101, 97, 103, 100, 98, 102
        };
        for (int i = 0; i < 20; i++) {
            bars.add(createBar(time.minusMinutes(20 - i), prices[i]));
        }

        // 마지막 바: 가격 100 (밴드 중간)
        bars.add(createBar(time, 100));

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
