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
 * MACrossoverStrategy 테스트
 *
 * 테스트 범위:
 * 1. 골든 크로스 (단기 이평 > 장기 이평) → BUY 시그널
 * 2. 데드 크로스 (단기 이평 < 장기 이평) → SELL 시그널
 * 3. 이평선 평행 (교차 없음) → HOLD 시그널
 * 4. 파라미터 검증 (shortPeriod >= longPeriod)
 * 5. 최소 바 부족 시 예외
 * 6. TTL 설정 확인
 */
@DisplayName("MACrossoverStrategy 도메인 테스트")
class MACrossoverStrategyTest {

    private MACrossoverStrategy strategy;
    private Map<String, Object> params;

    @BeforeEach
    void setUp() {
        params = new HashMap<>();
        params.put("shortPeriod", 5);
        params.put("longPeriod", 20);
        params.put("ttlSeconds", 300);

        strategy = new MACrossoverStrategy();
    }

    // ==================== 1. Golden Cross Tests ====================

    @Test
    @DisplayName("골든 크로스 - BUY 시그널 생성")
    void testGoldenCross_BuySignal() {
        // Given - 단기 이평이 장기 이평을 상향 돌파
        List<MarketBar> bars = createBarsWithGoldenCross();
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
        assertThat(decision.getReason()).contains("Golden Cross");
        assertThat(decision.getTtlSeconds()).isEqualTo(300);
    }

    @Test
    @DisplayName("골든 크로스 - 신호 이유에 MA 값 포함")
    void testGoldenCross_ReasonContainsMAValues() {
        // Given
        List<MarketBar> bars = createBarsWithGoldenCross();
        StrategyContext context = createContext(bars, params);

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision.getReason()).contains("MA(5)");
        assertThat(decision.getReason()).contains("MA(20)");
        assertThat(decision.getReason()).contains("crossed above");
    }

    // ==================== 2. Death Cross Tests ====================

    @Test
    @DisplayName("데드 크로스 - SELL 시그널 생성")
    void testDeadCross_SellSignal() {
        // Given - 단기 이평이 장기 이평을 하향 돌파
        List<MarketBar> bars = createBarsWithDeadCross();
        StrategyContext context = createContext(bars, params);

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision).isNotNull();
        assertThat(decision.getSignalType()).isEqualTo(SignalType.SELL);
        assertThat(decision.getTargetType()).isEqualTo("QTY");
        assertThat(decision.getTargetValue()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(decision.getReason()).contains("Death Cross");
        assertThat(decision.getTtlSeconds()).isEqualTo(300);
    }

    @Test
    @DisplayName("데드 크로스 - 신호 이유에 MA 값 포함")
    void testDeadCross_ReasonContainsMAValues() {
        // Given
        List<MarketBar> bars = createBarsWithDeadCross();
        StrategyContext context = createContext(bars, params);

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision.getReason()).contains("MA(5)");
        assertThat(decision.getReason()).contains("MA(20)");
        assertThat(decision.getReason()).contains("crossed below");
    }

    // ==================== 3. Hold Signal Tests ====================

    @Test
    @DisplayName("이평선 평행 - HOLD 시그널")
    void testParallelMA_HoldSignal() {
        // Given - 이평선이 평행 (교차 없음)
        List<MarketBar> bars = createBarsWithParallelMA();
        StrategyContext context = createContext(bars, params);

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision).isNotNull();
        assertThat(decision.getSignalType()).isEqualTo(SignalType.HOLD);
        assertThat(decision.getTargetType()).isNull();
        assertThat(decision.getTargetValue()).isNull();
        assertThat(decision.getReason()).contains("No crossover");
    }

    @Test
    @DisplayName("단기 이평 > 장기 이평 유지 - HOLD 시그널")
    void testShortAboveLong_NoNewCross_Hold() {
        // Given - 단기가 장기보다 계속 위에 있음 (새로운 교차 없음)
        List<MarketBar> bars = createBarsWithShortAboveLong();
        StrategyContext context = createContext(bars, params);

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision.getSignalType()).isEqualTo(SignalType.HOLD);
    }

    // ==================== 4. Parameter Validation Tests ====================

    @Test
    @DisplayName("파라미터 검증 - shortPeriod >= longPeriod 시 예외")
    void testValidateParams_ShortPeriodGreaterThanLong_ThrowsException() {
        // Given
        Map<String, Object> invalidParams = new HashMap<>();
        invalidParams.put("shortPeriod", 20);
        invalidParams.put("longPeriod", 5);

        // When & Then
        assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("shortPeriod must be less than longPeriod");
    }

    @Test
    @DisplayName("파라미터 검증 - shortPeriod = longPeriod 시 예외")
    void testValidateParams_EqualPeriods_ThrowsException() {
        // Given
        Map<String, Object> invalidParams = new HashMap<>();
        invalidParams.put("shortPeriod", 10);
        invalidParams.put("longPeriod", 10);

        // When & Then
        assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("shortPeriod must be less than longPeriod");
    }

    @Test
    @DisplayName("파라미터 검증 - 필수 파라미터 누락 시 예외")
    void testValidateParams_MissingParams_ThrowsException() {
        // Given
        Map<String, Object> incompleteParams = new HashMap<>();
        incompleteParams.put("shortPeriod", 5);
        // longPeriod 누락

        // When & Then
        assertThatThrownBy(() -> strategy.validateParams(incompleteParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required parameter: longPeriod");
    }

    @Test
    @DisplayName("파라미터 검증 - 음수 period 시 예외")
    void testValidateParams_NegativePeriod_ThrowsException() {
        // Given
        Map<String, Object> invalidParams = new HashMap<>();
        invalidParams.put("shortPeriod", -5);
        invalidParams.put("longPeriod", 20);

        // When & Then
        assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("periods must be positive");
    }

    // ==================== 5. Minimum Bars Validation Tests ====================

    @Test
    @DisplayName("최소 바 부족 시 예외 - longPeriod + 1개 필요")
    void testEvaluate_InsufficientBars_ThrowsException() {
        // Given - longPeriod=20이면 21개 바 필요, 20개만 제공
        List<MarketBar> bars = createBarsWithConstantPrice(20, BigDecimal.valueOf(70000));
        StrategyContext context = createContext(bars, params);

        // When & Then
        assertThatThrownBy(() -> strategy.evaluate(context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient bars")
                .hasMessageContaining("need 21, got 20");
    }

    @Test
    @DisplayName("최소 바 충족 시 정상 실행 - 21개")
    void testEvaluate_ExactMinimumBars_Success() {
        // Given - 정확히 21개 바
        List<MarketBar> bars = createBarsWithConstantPrice(21, BigDecimal.valueOf(70000));
        StrategyContext context = createContext(bars, params);

        // When & Then - 예외 발생 안 함
        assertThatCode(() -> strategy.evaluate(context))
                .doesNotThrowAnyException();
    }

    // ==================== 6. TTL Configuration Tests ====================

    @Test
    @DisplayName("TTL 설정 - 파라미터로 지정")
    void testTTL_FromParams() {
        // Given
        params.put("ttlSeconds", 600);
        List<MarketBar> bars = createBarsWithGoldenCross();
        StrategyContext context = createContext(bars, params);

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision.getTtlSeconds()).isEqualTo(600);
    }

    @Test
    @DisplayName("TTL 설정 - 파라미터 없으면 기본값 300초")
    void testTTL_Default() {
        // Given
        params.remove("ttlSeconds");
        List<MarketBar> bars = createBarsWithGoldenCross();
        StrategyContext context = createContext(bars, params);

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision.getTtlSeconds()).isEqualTo(300); // DEFAULT_TTL_SECONDS
    }

    // ==================== 7. Strategy Type Test ====================

    @Test
    @DisplayName("전략 타입 확인")
    void testGetStrategyType() {
        // When
        String type = strategy.getStrategyType();

        // Then
        assertThat(type).isEqualTo("MA_CROSSOVER");
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
     * 골든 크로스 발생하는 바 데이터 생성.
     *
     * 수동 계산으로 검증된 교차 패턴 (30개 바, 인덱스 0-29):
     * - 0-20 (21개): 200원
     * - 21-27 (7개): 50원
     *   → shortMAPrev = avg(price[24..28]) = (50×4 + 500)/5 = 140
     *   → longMAPrev = avg(price[9..28]) = (200×12 + 50×7 + 500)/20 = 162.5
     * - 28-29 (2개): 500원
     *   → shortMANow = avg(price[25..29]) = (50×3 + 500×2)/5 = 230
     *   → longMANow = avg(price[10..29]) = (200×11 + 50×7 + 500×2)/20 = 177.5
     *
     * 골든 크로스 검증: (shortMAPrev <= longMAPrev) && (shortMANow > longMANow)
     * → (140 <= 162.5) && (230 > 177.5) = TRUE ✓
     */
    private List<MarketBar> createBarsWithGoldenCross() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 0-20: 안정 200원 (21개 바)
        for (int i = 0; i < 21; i++) {
            bars.add(createBar("005930", now.minusMinutes(45 - i), BigDecimal.valueOf(200)));
        }

        // 21-27: 급락 50원 (7개 바) → short MA < long MA 확립
        for (int i = 21; i < 28; i++) {
            bars.add(createBar("005930", now.minusMinutes(45 - i), BigDecimal.valueOf(50)));
        }

        // 28-29: 급등 500원 (2개 바) → short MA가 long MA 추월
        for (int i = 28; i < 30; i++) {
            bars.add(createBar("005930", now.minusMinutes(45 - i), BigDecimal.valueOf(500)));
        }

        return bars;
    }

    /**
     * 데드 크로스 발생하는 바 데이터 생성.
     *
     * 수동 계산으로 검증된 교차 패턴 (30개 바, 인덱스 0-29):
     * - 0-20 (21개): 200원
     * - 21-26 (6개): 500원
     *   → shortMAPrev = avg(price[24..28]) = (500×3 + 10×2)/5 = 304
     *   → longMAPrev = avg(price[9..28]) = (200×12 + 500×6 + 10×2)/20 = 271
     * - 27-29 (3개): 10원
     *   → shortMANow = avg(price[25..29]) = (500×2 + 10×3)/5 = 206
     *   → longMANow = avg(price[10..29]) = (200×11 + 500×6 + 10×3)/20 = 261.5
     *
     * 데드 크로스 검증: (shortMAPrev >= longMAPrev) && (shortMANow < longMANow)
     * → (304 >= 271) && (206 < 261.5) = TRUE ✓
     */
    private List<MarketBar> createBarsWithDeadCross() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 0-20: 안정 200원 (21개 바)
        for (int i = 0; i < 21; i++) {
            bars.add(createBar("005930", now.minusMinutes(45 - i), BigDecimal.valueOf(200)));
        }

        // 21-26: 급등 500원 (6개 바) → short MA > long MA 확립
        for (int i = 21; i < 27; i++) {
            bars.add(createBar("005930", now.minusMinutes(45 - i), BigDecimal.valueOf(500)));
        }

        // 27-29: 급락 10원 (3개 바) → short MA가 long MA 아래로 교차
        for (int i = 27; i < 30; i++) {
            bars.add(createBar("005930", now.minusMinutes(45 - i), BigDecimal.valueOf(10)));
        }

        return bars;
    }

    /**
     * 이평선이 평행한 바 데이터 생성 (교차 없음).
     *
     * 시나리오: 가격이 일정하게 유지
     */
    private List<MarketBar> createBarsWithParallelMA() {
        return createBarsWithConstantPrice(25, BigDecimal.valueOf(70000));
    }

    /**
     * 단기 이평이 장기 이평보다 계속 위에 있는 바 데이터 생성.
     *
     * 시나리오: 초기부터 단기 > 장기로 시작해서 유지
     */
    private List<MarketBar> createBarsWithShortAboveLong() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 전체적으로 상승 추세 유지 (단기가 장기보다 항상 위)
        for (int i = 0; i < 25; i++) {
            BigDecimal price = BigDecimal.valueOf(70000 + i * 200);
            bars.add(createBar("005930", now.minusMinutes(30 - i), price));
        }

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

    /**
     * 단일 바 생성 헬퍼 메서드.
     */
    private MarketBar createBar(String symbol, LocalDateTime timestamp, BigDecimal price) {
        return MarketBar.restore(
                symbol,
                "1m",
                timestamp,
                price,  // open
                price,  // high
                price,  // low
                price,  // close
                100L,   // volume
                true    // closed
        );
    }
}
