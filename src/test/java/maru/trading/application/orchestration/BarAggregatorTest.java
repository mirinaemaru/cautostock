package maru.trading.application.orchestration;

import maru.trading.application.ports.repo.BarRepository;
import maru.trading.domain.market.MarketBar;
import maru.trading.domain.market.MarketTick;
import maru.trading.infra.cache.BarCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * BarAggregator 테스트
 *
 * 테스트 범위:
 * 1. 틱 수신 → 바 생성
 * 2. 여러 틱 집계 → OHLCV 계산
 * 3. 1분 경계 교차 → 바 닫기 + 새 바 생성
 * 4. 바 닫기 시 저장 및 캐싱
 * 5. 여러 심볼 동시 집계
 * 6. Null 틱 처리
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BarAggregator 도메인 테스트")
class BarAggregatorTest {

    @Mock
    private BarRepository barRepository;

    @Mock
    private BarCache barCache;

    @Captor
    private ArgumentCaptor<MarketBar> barCaptor;

    private BarAggregator barAggregator;

    @BeforeEach
    void setUp() {
        barAggregator = new BarAggregator(barRepository, barCache);
    }

    // ==================== 1. Bar Creation Tests ====================

    @Test
    @DisplayName("첫 틱 수신 → 새 바 생성")
    void testOnTick_FirstTick_CreatesNewBar() {
        // Given
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 9, 30, 15); // 09:30:15
        MarketTick tick = createTick("005930", BigDecimal.valueOf(70000), 100L, now);

        // When
        barAggregator.onTick(tick);

        // Then - 바가 생성되었지만 아직 닫히지 않음 (저장/캐싱 안 됨)
        verify(barRepository, never()).save(any());
        verify(barCache, never()).put(any());

        // Stats 확인 - 현재 진행 중인 바 확인
        Map<String, String> stats = barAggregator.getStats();
        assertThat(stats).hasSize(1);
        assertThat(stats).containsKey("005930:1m");
    }

    @Test
    @DisplayName("Null 틱 수신 → 무시")
    void testOnTick_NullTick_Ignored() {
        // When
        barAggregator.onTick(null);

        // Then
        verify(barRepository, never()).save(any());
        verify(barCache, never()).put(any());

        Map<String, String> stats = barAggregator.getStats();
        assertThat(stats).isEmpty();
    }

    // ==================== 2. OHLCV Calculation Tests ====================

    @Test
    @DisplayName("여러 틱 집계 → OHLCV 계산")
    void testOnTick_MultipleTicks_CalculatesOHLCV() {
        // Given
        LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 9, 30, 0);

        // 같은 분(9:30) 내의 틱 5개
        MarketTick tick1 = createTick("005930", BigDecimal.valueOf(70000), 100L, baseTime);
        MarketTick tick2 = createTick("005930", BigDecimal.valueOf(70500), 150L, baseTime.plusSeconds(10));
        MarketTick tick3 = createTick("005930", BigDecimal.valueOf(70800), 200L, baseTime.plusSeconds(20));
        MarketTick tick4 = createTick("005930", BigDecimal.valueOf(70300), 180L, baseTime.plusSeconds(30));
        MarketTick tick5 = createTick("005930", BigDecimal.valueOf(70600), 120L, baseTime.plusSeconds(40));

        // When
        barAggregator.onTick(tick1);
        barAggregator.onTick(tick2);
        barAggregator.onTick(tick3);
        barAggregator.onTick(tick4);
        barAggregator.onTick(tick5);

        // Then - 아직 바가 닫히지 않음 (1분 경계를 넘지 않음)
        verify(barRepository, never()).save(any());

        // 다음 분(9:31)의 틱으로 바 닫기
        MarketTick nextMinuteTick = createTick("005930", BigDecimal.valueOf(70700), 100L, baseTime.plusMinutes(1));
        barAggregator.onTick(nextMinuteTick);

        // Then - 이전 바가 닫히고 저장됨
        verify(barRepository, times(1)).save(barCaptor.capture());
        verify(barCache, times(1)).put(barCaptor.getValue());

        MarketBar closedBar = barCaptor.getValue();
        assertThat(closedBar.getSymbol()).isEqualTo("005930");
        assertThat(closedBar.getTimeframe()).isEqualTo("1m");
        assertThat(closedBar.getBarTimestamp()).isEqualTo(LocalDateTime.of(2026, 1, 1, 9, 30, 0));
        assertThat(closedBar.getOpen()).isEqualByComparingTo(BigDecimal.valueOf(70000)); // 첫 틱
        assertThat(closedBar.getHigh()).isEqualByComparingTo(BigDecimal.valueOf(70800)); // 최고
        assertThat(closedBar.getLow()).isEqualByComparingTo(BigDecimal.valueOf(70000)); // 최저
        assertThat(closedBar.getClose()).isEqualByComparingTo(BigDecimal.valueOf(70600)); // 마지막 틱
        assertThat(closedBar.getVolume()).isEqualTo(100 + 150 + 200 + 180 + 120); // 750
        assertThat(closedBar.isClosed()).isTrue();
    }

    // ==================== 3. Bar Boundary Crossing Tests ====================

    @Test
    @DisplayName("1분 경계 교차 → 기존 바 닫기 + 새 바 생성")
    void testOnTick_CrossesMinuteBoundary_ClosesOldBarAndCreatesNew() {
        // Given
        LocalDateTime time1 = LocalDateTime.of(2026, 1, 1, 9, 30, 45); // 09:30:45
        LocalDateTime time2 = LocalDateTime.of(2026, 1, 1, 9, 31, 10); // 09:31:10 (다음 분)

        MarketTick tick1 = createTick("005930", BigDecimal.valueOf(70000), 100L, time1);
        MarketTick tick2 = createTick("005930", BigDecimal.valueOf(70500), 150L, time2);

        // When
        barAggregator.onTick(tick1);
        barAggregator.onTick(tick2);

        // Then
        verify(barRepository, times(1)).save(barCaptor.capture());
        verify(barCache, times(1)).put(any());

        // 닫힌 바 확인 (09:30:00 바)
        MarketBar closedBar = barCaptor.getValue();
        assertThat(closedBar.getBarTimestamp()).isEqualTo(LocalDateTime.of(2026, 1, 1, 9, 30, 0));
        assertThat(closedBar.getOpen()).isEqualByComparingTo(BigDecimal.valueOf(70000));
        assertThat(closedBar.getClose()).isEqualByComparingTo(BigDecimal.valueOf(70000));
        assertThat(closedBar.isClosed()).isTrue();

        // 새 바가 생성되었는지 확인 (09:31:00 바)
        Map<String, String> stats = barAggregator.getStats();
        assertThat(stats).hasSize(1);
        assertThat(stats.get("005930:1m")).contains("timestamp=2026-01-01T09:31");
    }

    @Test
    @DisplayName("여러 분 경계 교차 → 각 바마다 저장")
    void testOnTick_MultipleBoundaryCrosses_SavesEachBar() {
        // Given
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 9, 30, 0);

        // When - 3개 분의 틱 (09:30, 09:31, 09:32)
        barAggregator.onTick(createTick("005930", BigDecimal.valueOf(70000), 100L, base));
        barAggregator.onTick(createTick("005930", BigDecimal.valueOf(70100), 100L, base.plusMinutes(1)));
        barAggregator.onTick(createTick("005930", BigDecimal.valueOf(70200), 100L, base.plusMinutes(2)));

        // Then - 2개 바가 닫힘 (09:30, 09:31)
        verify(barRepository, times(2)).save(any());
        verify(barCache, times(2)).put(any());
    }

    // ==================== 4. Bar Closing and Persistence Tests ====================

    @Test
    @DisplayName("바 닫기 시 저장 및 캐싱")
    void testCloseBar_SavesAndCaches() {
        // Given
        LocalDateTime time1 = LocalDateTime.of(2026, 1, 1, 9, 30, 30);
        LocalDateTime time2 = LocalDateTime.of(2026, 1, 1, 9, 31, 0);

        MarketTick tick1 = createTick("005930", BigDecimal.valueOf(70000), 100L, time1);
        MarketTick tick2 = createTick("005930", BigDecimal.valueOf(70500), 150L, time2);

        // When
        barAggregator.onTick(tick1);
        barAggregator.onTick(tick2);

        // Then
        verify(barRepository, times(1)).save(barCaptor.capture());
        verify(barCache, times(1)).put(barCaptor.getValue());

        MarketBar savedBar = barCaptor.getValue();
        assertThat(savedBar.isClosed()).isTrue();
    }

    @Test
    @DisplayName("closeAllBars 호출 시 모든 바 닫기")
    void testCloseAllBars_ClosesAllCurrentBars() {
        // Given - 3개 심볼의 바 생성
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 9, 30, 0);
        barAggregator.onTick(createTick("005930", BigDecimal.valueOf(70000), 100L, now));
        barAggregator.onTick(createTick("000660", BigDecimal.valueOf(50000), 100L, now));
        barAggregator.onTick(createTick("035420", BigDecimal.valueOf(80000), 100L, now));

        // When
        barAggregator.closeAllBars();

        // Then - 3개 바 모두 저장됨
        verify(barRepository, times(3)).save(any());
        verify(barCache, times(3)).put(any());

        // 현재 진행 중인 바 없음
        Map<String, String> stats = barAggregator.getStats();
        assertThat(stats).isEmpty();
    }

    // ==================== 5. Multiple Symbol Tests ====================

    @Test
    @DisplayName("여러 심볼 동시 집계 - 독립적으로 바 생성")
    void testOnTick_MultipleSymbols_IndependentBars() {
        // Given
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 9, 30, 0);

        MarketTick tick1 = createTick("005930", BigDecimal.valueOf(70000), 100L, now);
        MarketTick tick2 = createTick("000660", BigDecimal.valueOf(50000), 150L, now);

        // When
        barAggregator.onTick(tick1);
        barAggregator.onTick(tick2);

        // Then - 2개 심볼의 바가 독립적으로 생성됨
        Map<String, String> stats = barAggregator.getStats();
        assertThat(stats).hasSize(2);
        assertThat(stats).containsKeys("005930:1m", "000660:1m");
    }

    @Test
    @DisplayName("여러 심볼 동시 집계 - 각 심볼 독립적으로 바 닫기")
    void testOnTick_MultipleSymbols_IndependentClosing() {
        // Given
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 9, 30, 0);

        // 005930: 09:30 → 09:31 (바 닫힘)
        barAggregator.onTick(createTick("005930", BigDecimal.valueOf(70000), 100L, base));
        barAggregator.onTick(createTick("005930", BigDecimal.valueOf(70100), 100L, base.plusMinutes(1)));

        // 000660: 09:30만 (바 안 닫힘)
        barAggregator.onTick(createTick("000660", BigDecimal.valueOf(50000), 100L, base));

        // Then - 005930 바만 닫힘
        verify(barRepository, times(1)).save(barCaptor.capture());
        assertThat(barCaptor.getValue().getSymbol()).isEqualTo("005930");

        // 000660 바는 여전히 진행 중
        Map<String, String> stats = barAggregator.getStats();
        assertThat(stats).containsKeys("005930:1m", "000660:1m");
    }

    // ==================== 6. Edge Case Tests ====================

    @Test
    @DisplayName("정확히 분 경계의 틱 - 올바른 바에 포함")
    void testOnTick_ExactMinuteBoundary_CorrectBar() {
        // Given - 정확히 09:30:00와 09:31:00의 틱
        LocalDateTime time1 = LocalDateTime.of(2026, 1, 1, 9, 30, 0);
        LocalDateTime time2 = LocalDateTime.of(2026, 1, 1, 9, 31, 0);

        MarketTick tick1 = createTick("005930", BigDecimal.valueOf(70000), 100L, time1);
        MarketTick tick2 = createTick("005930", BigDecimal.valueOf(70100), 100L, time2);

        // When
        barAggregator.onTick(tick1);
        barAggregator.onTick(tick2);

        // Then
        verify(barRepository, times(1)).save(barCaptor.capture());

        MarketBar closedBar = barCaptor.getValue();
        assertThat(closedBar.getBarTimestamp()).isEqualTo(time1);
        assertThat(closedBar.getOpen()).isEqualByComparingTo(BigDecimal.valueOf(70000));
    }

    @Test
    @DisplayName("단일 틱으로 바 생성 및 닫기")
    void testOnTick_SingleTick_CreatesAndClosesBar() {
        // Given
        LocalDateTime time1 = LocalDateTime.of(2026, 1, 1, 9, 30, 30);
        LocalDateTime time2 = LocalDateTime.of(2026, 1, 1, 9, 31, 30);

        MarketTick tick1 = createTick("005930", BigDecimal.valueOf(70000), 100L, time1);
        MarketTick tick2 = createTick("005930", BigDecimal.valueOf(70100), 100L, time2);

        // When
        barAggregator.onTick(tick1);
        barAggregator.onTick(tick2);

        // Then
        verify(barRepository, times(1)).save(barCaptor.capture());

        MarketBar closedBar = barCaptor.getValue();
        assertThat(closedBar.getOpen()).isEqualByComparingTo(BigDecimal.valueOf(70000));
        assertThat(closedBar.getHigh()).isEqualByComparingTo(BigDecimal.valueOf(70000));
        assertThat(closedBar.getLow()).isEqualByComparingTo(BigDecimal.valueOf(70000));
        assertThat(closedBar.getClose()).isEqualByComparingTo(BigDecimal.valueOf(70000));
        assertThat(closedBar.getVolume()).isEqualTo(100);
    }

    @Test
    @DisplayName("Stats 조회 - 현재 진행 중인 바 정보")
    void testGetStats_ReturnsCurrentBars() {
        // Given
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 9, 30, 15);
        barAggregator.onTick(createTick("005930", BigDecimal.valueOf(70000), 100L, now));
        barAggregator.onTick(createTick("000660", BigDecimal.valueOf(50000), 100L, now));

        // When
        Map<String, String> stats = barAggregator.getStats();

        // Then
        assertThat(stats).hasSize(2);
        assertThat(stats).containsKeys("005930:1m", "000660:1m");
        assertThat(stats.get("005930:1m")).contains("timestamp=2026-01-01T09:30");
        assertThat(stats.get("000660:1m")).contains("timestamp=2026-01-01T09:30");
    }

    // ==================== Helper Methods ====================

    private MarketTick createTick(String symbol, BigDecimal price, long volume, LocalDateTime timestamp) {
        return new MarketTick(symbol, price, volume, timestamp, "NORMAL");
    }
}
