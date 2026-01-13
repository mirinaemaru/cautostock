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
 * RSIStrategy 테스트
 *
 * 테스트 범위:
 * 1. RSI 과매도 교차 (RSI < 30) → BUY 시그널
 * 2. RSI 과매수 교차 (RSI > 70) → SELL 시그널
 * 3. RSI 중립 영역 → HOLD 시그널
 * 4. 파라미터 검증 (oversoldThreshold >= overboughtThreshold)
 * 5. 최소 바 부족 시 예외
 * 6. TTL 설정 확인
 */
@DisplayName("RSIStrategy 도메인 테스트")
class RSIStrategyTest {

    private RSIStrategy strategy;
    private Map<String, Object> params;

    @BeforeEach
    void setUp() {
        params = new HashMap<>();
        params.put("period", 14);
        params.put("overboughtThreshold", 70.0);
        params.put("oversoldThreshold", 30.0);
        params.put("ttlSeconds", 300);

        strategy = new RSIStrategy();
    }

    // ==================== 1. Oversold Crossover Tests ====================

    @Test
    @DisplayName("RSI 과매도 교차 - BUY 시그널")
    void testOversoldCrossover_BuySignal() {
        // Given - RSI가 30 아래로 하향 교차
        List<MarketBar> bars = createBarsWithOversoldCrossover();
        StrategyContext context = createContext(bars, params);

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision).isNotNull();
        assertThat(decision.getSignalType()).isEqualTo(SignalType.BUY);
        assertThat(decision.getTargetType()).isEqualTo("QTY");
        assertThat(decision.getTargetValue()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(decision.getReason()).contains("RSI Oversold");
        assertThat(decision.getReason()).contains("crossed below threshold");
        assertThat(decision.getTtlSeconds()).isEqualTo(300);
    }

    @Test
    @DisplayName("RSI가 이미 과매도 영역에 있지만 교차 없음 - HOLD")
    void testRSI_AlreadyOversold_NoNewCross_Hold() {
        // Given - RSI가 이미 30 아래에 있지만 새로운 교차 없음
        List<MarketBar> bars = createBarsWithRSIAlreadyOversold();
        StrategyContext context = createContext(bars, params);

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision.getSignalType()).isEqualTo(SignalType.HOLD);
    }

    // ==================== 2. Overbought Crossover Tests ====================

    @Test
    @DisplayName("RSI 과매수 교차 - SELL 시그널")
    void testOverboughtCrossover_SellSignal() {
        // Given - RSI가 70 위로 상향 교차
        List<MarketBar> bars = createBarsWithOverboughtCrossover();
        StrategyContext context = createContext(bars, params);

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision).isNotNull();
        assertThat(decision.getSignalType()).isEqualTo(SignalType.SELL);
        assertThat(decision.getTargetType()).isEqualTo("QTY");
        assertThat(decision.getTargetValue()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(decision.getReason()).contains("RSI Overbought");
        assertThat(decision.getReason()).contains("crossed above threshold");
        assertThat(decision.getTtlSeconds()).isEqualTo(300);
    }

    @Test
    @DisplayName("RSI가 이미 과매수 영역에 있지만 교차 없음 - HOLD")
    void testRSI_AlreadyOverbought_NoNewCross_Hold() {
        // Given - RSI가 이미 70 위에 있지만 새로운 교차 없음
        List<MarketBar> bars = createBarsWithRSIAlreadyOverbought();
        StrategyContext context = createContext(bars, params);

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision.getSignalType()).isEqualTo(SignalType.HOLD);
    }

    // ==================== 3. Neutral Zone Tests ====================

    @Test
    @DisplayName("RSI 중립 영역 - HOLD 시그널")
    void testRSI_Neutral_HoldSignal() {
        // Given - RSI가 30-70 사이 (중립)
        List<MarketBar> bars = createBarsWithNeutralRSI();
        StrategyContext context = createContext(bars, params);

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision).isNotNull();
        assertThat(decision.getSignalType()).isEqualTo(SignalType.HOLD);
        assertThat(decision.getTargetType()).isNull();
        assertThat(decision.getTargetValue()).isNull();
        assertThat(decision.getReason()).contains("RSI Neutral");
    }

    // ==================== 4. Parameter Validation Tests ====================

    @Test
    @DisplayName("파라미터 검증 - oversoldThreshold >= overboughtThreshold 시 예외")
    void testValidateParams_InvalidThresholds_ThrowsException() {
        // Given
        Map<String, Object> invalidParams = new HashMap<>();
        invalidParams.put("period", 14);
        invalidParams.put("overboughtThreshold", 30.0);
        invalidParams.put("oversoldThreshold", 70.0);

        // When & Then
        assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oversoldThreshold must be less than overboughtThreshold");
    }

    @Test
    @DisplayName("파라미터 검증 - 필수 파라미터 누락 시 예외")
    void testValidateParams_MissingParams_ThrowsException() {
        // Given
        Map<String, Object> incompleteParams = new HashMap<>();
        incompleteParams.put("period", 14);
        incompleteParams.put("overboughtThreshold", 70.0);
        // oversoldThreshold 누락

        // When & Then
        assertThatThrownBy(() -> strategy.validateParams(incompleteParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required parameter: oversoldThreshold");
    }

    @Test
    @DisplayName("파라미터 검증 - 음수 period 시 예외")
    void testValidateParams_NegativePeriod_ThrowsException() {
        // Given
        Map<String, Object> invalidParams = new HashMap<>();
        invalidParams.put("period", -14);
        invalidParams.put("overboughtThreshold", 70.0);
        invalidParams.put("oversoldThreshold", 30.0);

        // When & Then
        assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("period must be positive");
    }

    @Test
    @DisplayName("파라미터 검증 - threshold 범위 초과 시 예외 (> 100)")
    void testValidateParams_ThresholdOutOfRange_ThrowsException() {
        // Given
        Map<String, Object> invalidParams = new HashMap<>();
        invalidParams.put("period", 14);
        invalidParams.put("overboughtThreshold", 110.0);
        invalidParams.put("oversoldThreshold", 30.0);

        // When & Then
        assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overboughtThreshold must be between 0 and 100");
    }

    @Test
    @DisplayName("파라미터 검증 - threshold 범위 초과 시 예외 (< 0)")
    void testValidateParams_ThresholdNegative_ThrowsException() {
        // Given
        Map<String, Object> invalidParams = new HashMap<>();
        invalidParams.put("period", 14);
        invalidParams.put("overboughtThreshold", 70.0);
        invalidParams.put("oversoldThreshold", -10.0);

        // When & Then
        assertThatThrownBy(() -> strategy.validateParams(invalidParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oversoldThreshold must be between 0 and 100");
    }

    // ==================== 5. Minimum Bars Validation Tests ====================

    @Test
    @DisplayName("최소 바 부족 시 예외 - period + 2개 필요")
    void testEvaluate_InsufficientBars_ThrowsException() {
        // Given - period=14이면 16개 바 필요, 15개만 제공
        List<MarketBar> bars = createBarsWithConstantPrice(15, BigDecimal.valueOf(70000));
        StrategyContext context = createContext(bars, params);

        // When & Then
        assertThatThrownBy(() -> strategy.evaluate(context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient bars")
                .hasMessageContaining("need 16, got 15");
    }

    @Test
    @DisplayName("최소 바 충족 시 정상 실행 - 16개")
    void testEvaluate_ExactMinimumBars_Success() {
        // Given - 정확히 16개 바
        List<MarketBar> bars = createBarsWithConstantPrice(16, BigDecimal.valueOf(70000));
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
        List<MarketBar> bars = createBarsWithOversoldCrossover();
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
        List<MarketBar> bars = createBarsWithOversoldCrossover();
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
        assertThat(type).isEqualTo("RSI");
    }

    // ==================== Helper Methods ====================

    private StrategyContext createContext(List<MarketBar> bars, Map<String, Object> params) {
        return StrategyContext.builder()
                .strategyId("STR_RSI_001")
                .symbol("005930")
                .accountId("ACC_001")
                .bars(bars)
                .params(params)
                .timeframe("1m")
                .build();
    }

    /**
     * RSI 과매도 교차 발생하는 바 데이터 생성.
     *
     * RSI 계산을 위해 충분한 바가 필요 (period + 2 = 16개 이상)
     *
     * 전략 (초강력 반등 패턴):
     * - 0-20 (21개): 1000원 (안정) → RSI ~50 유지
     * - 21-24 (4개): 약간 하락 900원 → RSI 약간 하락
     * - 25-31 (7개): 초강력 반등 1050원 → RSI 50 이상으로 회복
     * - 32-34 (3개): 조정 950원 → RSI 30 이상 유지
     * - 35 (1개): 급락 100원 → RSI 30 아래로 교차
     *
     * 마지막 바(35)에서 평가:
     * - rsiPrev (Bar 34): 조정 중 - RSI 30 이상
     * - rsiNow (Bar 35): 급락 - RSI 30 미만
     * → rsiPrev >= 30, rsiNow < 30 교차 발생
     */
    private List<MarketBar> createBarsWithOversoldCrossover() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 0-20: 안정 1000원 (21개 바) → RSI ~50
        for (int i = 0; i < 21; i++) {
            bars.add(createBar("005930", now.minusMinutes(50 - i), BigDecimal.valueOf(1000)));
        }

        // 21-24: 약간 하락 900원 (4개 바)
        for (int i = 21; i < 25; i++) {
            bars.add(createBar("005930", now.minusMinutes(50 - i), BigDecimal.valueOf(900)));
        }

        // 25-31: 초강력 반등 1050원 (7개 바) → 원래 가격보다 높음!
        for (int i = 25; i < 32; i++) {
            bars.add(createBar("005930", now.minusMinutes(50 - i), BigDecimal.valueOf(1050)));
        }

        // 32-34: 조정 950원 (3개 바)
        for (int i = 32; i < 35; i++) {
            bars.add(createBar("005930", now.minusMinutes(50 - i), BigDecimal.valueOf(950)));
        }

        // 35: 급락 100원 (1개 바) → RSI 30 아래로
        bars.add(createBar("005930", now.minusMinutes(50 - 35), BigDecimal.valueOf(100)));

        return bars;
    }

    /**
     * RSI가 이미 과매도 영역에 있는 바 데이터 생성 (새로운 교차 없음).
     *
     * - 0-14: 1000원
     * - 15-35: 지속적 하락으로 RSI가 이미 30 아래
     * → 추가 하락이 계속되어 RSI가 계속 낮은 상태 유지 (새로운 교차 없음)
     */
    private List<MarketBar> createBarsWithRSIAlreadyOversold() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 0-14: 안정
        for (int i = 0; i < 15; i++) {
            bars.add(createBar("005930", now.minusMinutes(45 - i), BigDecimal.valueOf(1000)));
        }

        // 15-40: 지속적 하락 (RSI 이미 30 아래로 떨어졌고 계속 낮은 상태)
        for (int i = 15; i < 41; i++) {
            BigDecimal price = BigDecimal.valueOf(1000 - (i - 14) * 35);
            bars.add(createBar("005930", now.minusMinutes(45 - i), price));
        }

        return bars;
    }

    /**
     * RSI 과매수 교차 발생하는 바 데이터 생성.
     *
     * RSI 계산을 위해 충분한 바가 필요 (period + 2 = 16개 이상)
     *
     * 전략 (초강력 조정 패턴):
     * - 0-20 (21개): 1000원 (안정) → RSI ~50 유지
     * - 21-24 (4개): 약간 상승 1100원 → RSI 약간 상승
     * - 25-31 (7개): 초강력 조정 950원 → RSI 50 미만으로 하락
     * - 32-34 (3개): 상승 1050원 → RSI 70 미만 유지
     * - 35 (1개): 급등 2000원 → RSI 70 위로 교차
     *
     * 마지막 바(35)에서 평가:
     * - rsiPrev (Bar 34): 상승 중 - RSI 70 미만
     * - rsiNow (Bar 35): 급등 - RSI 70 초과
     * → rsiPrev <= 70, rsiNow > 70 교차 발생
     */
    private List<MarketBar> createBarsWithOverboughtCrossover() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 0-20: 안정 1000원 (21개 바) → RSI ~50
        for (int i = 0; i < 21; i++) {
            bars.add(createBar("005930", now.minusMinutes(50 - i), BigDecimal.valueOf(1000)));
        }

        // 21-24: 약간 상승 1100원 (4개 바)
        for (int i = 21; i < 25; i++) {
            bars.add(createBar("005930", now.minusMinutes(50 - i), BigDecimal.valueOf(1100)));
        }

        // 25-31: 초강력 조정 950원 (7개 바) → 원래 가격보다 낮음!
        for (int i = 25; i < 32; i++) {
            bars.add(createBar("005930", now.minusMinutes(50 - i), BigDecimal.valueOf(950)));
        }

        // 32-34: 상승 1050원 (3개 바)
        for (int i = 32; i < 35; i++) {
            bars.add(createBar("005930", now.minusMinutes(50 - i), BigDecimal.valueOf(1050)));
        }

        // 35: 급등 2000원 (1개 바) → RSI 70 위로
        bars.add(createBar("005930", now.minusMinutes(50 - 35), BigDecimal.valueOf(2000)));

        return bars;
    }

    /**
     * RSI가 이미 과매수 영역에 있는 바 데이터 생성 (새로운 교차 없음).
     *
     * - 0-14: 100원
     * - 15-40: 지속적 상승으로 RSI가 이미 70 위
     * → 추가 상승이 계속되어 RSI가 계속 높은 상태 유지 (새로운 교차 없음)
     */
    private List<MarketBar> createBarsWithRSIAlreadyOverbought() {
        List<MarketBar> bars = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 0-14: 안정
        for (int i = 0; i < 15; i++) {
            bars.add(createBar("005930", now.minusMinutes(45 - i), BigDecimal.valueOf(100)));
        }

        // 15-40: 지속적 상승 (RSI 이미 70 위로 올라갔고 계속 높은 상태)
        for (int i = 15; i < 41; i++) {
            BigDecimal price = BigDecimal.valueOf(100 + (i - 14) * 35);
            bars.add(createBar("005930", now.minusMinutes(45 - i), price));
        }

        return bars;
    }

    /**
     * RSI 중립 영역 바 데이터 생성 (30-70 사이).
     * 일정한 가격 → RSI ~50 (중립)
     */
    private List<MarketBar> createBarsWithNeutralRSI() {
        return createBarsWithConstantPrice(25, BigDecimal.valueOf(500));
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
