package maru.trading.domain.market;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MarketHoursPolicy 도메인 테스트
 *
 * 테스트 범위:
 * 1. 각 거래 세션별 시간 검증
 * 2. 주말 폐장 검증
 * 3. 공휴일 폐장 검증
 * 4. 세션 경계값 테스트
 * 5. 복수 세션 허용 시나리오
 */
@DisplayName("MarketHoursPolicy 도메인 테스트")
class MarketHoursPolicyTest {

    private MarketHoursPolicy policy;
    private Set<LocalDate> publicHolidays;

    @BeforeEach
    void setUp() {
        policy = new MarketHoursPolicy();
        publicHolidays = Set.of(
            LocalDate.of(2025, 1, 1),  // 신정
            LocalDate.of(2025, 1, 29), // 설날
            LocalDate.of(2025, 12, 25) // 성탄절
        );
    }

    // ==================== 1. Regular Session Tests ====================

    @Test
    @DisplayName("정규장 시간 내 (10:00) - 개장")
    void testRegularSession_MidDay_Open() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 10, 0); // Thursday 10:00
        Set<TradingSession> allowedSessions = Set.of(TradingSession.REGULAR);

        // When
        boolean isOpen = policy.isMarketOpen(time, allowedSessions, Set.of());

        // Then
        assertThat(isOpen).isTrue();
    }

    @Test
    @DisplayName("정규장 개장 시간 (09:00) - 개장")
    void testRegularSession_OpenTime_Open() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 9, 0, 0); // Thursday 09:00:00
        Set<TradingSession> allowedSessions = Set.of(TradingSession.REGULAR);

        // When
        boolean isOpen = policy.isMarketOpen(time, allowedSessions, Set.of());

        // Then
        assertThat(isOpen).isTrue();
    }

    @Test
    @DisplayName("정규장 폐장 시간 (15:30) - 개장")
    void testRegularSession_CloseTime_Open() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 15, 30, 0); // Thursday 15:30:00
        Set<TradingSession> allowedSessions = Set.of(TradingSession.REGULAR);

        // When
        boolean isOpen = policy.isMarketOpen(time, allowedSessions, Set.of());

        // Then
        assertThat(isOpen).isTrue();
    }

    @Test
    @DisplayName("정규장 이전 시간 (08:59) - 폐장")
    void testRegularSession_BeforeOpen_Closed() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 8, 59, 59); // Thursday 08:59:59
        Set<TradingSession> allowedSessions = Set.of(TradingSession.REGULAR);

        // When
        boolean isOpen = policy.isMarketOpen(time, allowedSessions, Set.of());

        // Then
        assertThat(isOpen).isFalse();
    }

    @Test
    @DisplayName("정규장 이후 시간 (15:31) - 폐장")
    void testRegularSession_AfterClose_Closed() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 15, 31, 0); // Thursday 15:31:00
        Set<TradingSession> allowedSessions = Set.of(TradingSession.REGULAR);

        // When
        boolean isOpen = policy.isMarketOpen(time, allowedSessions, Set.of());

        // Then
        assertThat(isOpen).isFalse();
    }

    // ==================== 2. Pre-Market Session Tests ====================

    @Test
    @DisplayName("시간외 단일가 장전 시간 내 (08:35) - 개장")
    void testPreMarket_MidSession_Open() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 8, 35); // Thursday 08:35
        Set<TradingSession> allowedSessions = Set.of(TradingSession.PRE_MARKET);

        // When
        boolean isOpen = policy.isMarketOpen(time, allowedSessions, Set.of());

        // Then
        assertThat(isOpen).isTrue();
    }

    @Test
    @DisplayName("시간외 단일가 장전 이전 시간 (08:29) - 폐장")
    void testPreMarket_BeforeOpen_Closed() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 8, 29, 59); // Thursday 08:29:59
        Set<TradingSession> allowedSessions = Set.of(TradingSession.PRE_MARKET);

        // When
        boolean isOpen = policy.isMarketOpen(time, allowedSessions, Set.of());

        // Then
        assertThat(isOpen).isFalse();
    }

    @Test
    @DisplayName("시간외 단일가 장전 이후 시간 (08:41) - 폐장")
    void testPreMarket_AfterClose_Closed() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 8, 41, 0); // Thursday 08:41:00
        Set<TradingSession> allowedSessions = Set.of(TradingSession.PRE_MARKET);

        // When
        boolean isOpen = policy.isMarketOpen(time, allowedSessions, Set.of());

        // Then
        assertThat(isOpen).isFalse();
    }

    // ==================== 3. After-Hours Closing Session Tests ====================

    @Test
    @DisplayName("시간외 종가 시간 내 (15:50) - 개장")
    void testAfterHoursClosing_MidSession_Open() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 15, 50); // Thursday 15:50
        Set<TradingSession> allowedSessions = Set.of(TradingSession.AFTER_HOURS_CLOSING);

        // When
        boolean isOpen = policy.isMarketOpen(time, allowedSessions, Set.of());

        // Then
        assertThat(isOpen).isTrue();
    }

    // ==================== 4. After-Hours Session Tests ====================

    @Test
    @DisplayName("시간외 단일가 장후 시간 내 (17:00) - 개장")
    void testAfterHours_MidSession_Open() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 17, 0); // Thursday 17:00
        Set<TradingSession> allowedSessions = Set.of(TradingSession.AFTER_HOURS);

        // When
        boolean isOpen = policy.isMarketOpen(time, allowedSessions, Set.of());

        // Then
        assertThat(isOpen).isTrue();
    }

    @Test
    @DisplayName("시간외 단일가 장후 이후 시간 (18:01) - 폐장")
    void testAfterHours_AfterClose_Closed() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 18, 1, 0); // Thursday 18:01:00
        Set<TradingSession> allowedSessions = Set.of(TradingSession.AFTER_HOURS);

        // When
        boolean isOpen = policy.isMarketOpen(time, allowedSessions, Set.of());

        // Then
        assertThat(isOpen).isFalse();
    }

    // ==================== 5. Weekend Tests ====================

    @Test
    @DisplayName("토요일 정규장 시간 - 폐장 (주말)")
    void testWeekend_Saturday_Closed() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 4, 10, 0); // Saturday 10:00
        Set<TradingSession> allowedSessions = Set.of(TradingSession.REGULAR);

        // When
        boolean isOpen = policy.isMarketOpen(time, allowedSessions, Set.of());

        // Then
        assertThat(isOpen).isFalse();
    }

    @Test
    @DisplayName("일요일 정규장 시간 - 폐장 (주말)")
    void testWeekend_Sunday_Closed() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 5, 10, 0); // Sunday 10:00
        Set<TradingSession> allowedSessions = Set.of(TradingSession.REGULAR);

        // When
        boolean isOpen = policy.isMarketOpen(time, allowedSessions, Set.of());

        // Then
        assertThat(isOpen).isFalse();
    }

    // ==================== 6. Public Holiday Tests ====================

    @Test
    @DisplayName("공휴일 정규장 시간 - 폐장 (신정)")
    void testPublicHoliday_NewYear_Closed() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 10, 0); // New Year's Day 10:00
        Set<TradingSession> allowedSessions = Set.of(TradingSession.REGULAR);

        // When
        boolean isOpen = policy.isMarketOpen(time, allowedSessions, publicHolidays);

        // Then
        assertThat(isOpen).isFalse();
    }

    @Test
    @DisplayName("공휴일 정규장 시간 - 폐장 (설날)")
    void testPublicHoliday_LunarNewYear_Closed() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 29, 10, 0); // Lunar New Year 10:00
        Set<TradingSession> allowedSessions = Set.of(TradingSession.REGULAR);

        // When
        boolean isOpen = policy.isMarketOpen(time, allowedSessions, publicHolidays);

        // Then
        assertThat(isOpen).isFalse();
    }

    @Test
    @DisplayName("공휴일 정규장 시간 - 폐장 (성탄절)")
    void testPublicHoliday_Christmas_Closed() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 12, 25, 10, 0); // Christmas 10:00
        Set<TradingSession> allowedSessions = Set.of(TradingSession.REGULAR);

        // When
        boolean isOpen = policy.isMarketOpen(time, allowedSessions, publicHolidays);

        // Then
        assertThat(isOpen).isFalse();
    }

    // ==================== 7. Multiple Sessions Tests ====================

    @Test
    @DisplayName("복수 세션 허용 - 정규장 시간에 개장")
    void testMultipleSessions_RegularTime_Open() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 10, 0); // Thursday 10:00
        Set<TradingSession> allowedSessions = Set.of(
            TradingSession.REGULAR,
            TradingSession.AFTER_HOURS_CLOSING
        );

        // When
        boolean isOpen = policy.isMarketOpen(time, allowedSessions, Set.of());

        // Then
        assertThat(isOpen).isTrue();
    }

    @Test
    @DisplayName("복수 세션 허용 - 시간외 종가 시간에 개장")
    void testMultipleSessions_AfterHoursClosingTime_Open() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 15, 50); // Thursday 15:50
        Set<TradingSession> allowedSessions = Set.of(
            TradingSession.REGULAR,
            TradingSession.AFTER_HOURS_CLOSING
        );

        // When
        boolean isOpen = policy.isMarketOpen(time, allowedSessions, Set.of());

        // Then
        assertThat(isOpen).isTrue();
    }

    @Test
    @DisplayName("복수 세션 허용 - 세션 사이 시간에 폐장")
    void testMultipleSessions_BetweenSessions_Closed() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 15, 35); // Thursday 15:35 (between REGULAR and AFTER_HOURS_CLOSING)
        Set<TradingSession> allowedSessions = Set.of(
            TradingSession.REGULAR,
            TradingSession.AFTER_HOURS_CLOSING
        );

        // When
        boolean isOpen = policy.isMarketOpen(time, allowedSessions, Set.of());

        // Then
        assertThat(isOpen).isFalse();
    }

    // ==================== 8. Session Boundary Parameterized Tests ====================

    @ParameterizedTest
    @MethodSource("provideSessionBoundaries")
    @DisplayName("각 세션별 경계값 테스트")
    void testSessionBoundaries(LocalTime time, TradingSession session, boolean expected) {
        // When
        boolean result = policy.isWithinSession(time, session);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> provideSessionBoundaries() {
        return Stream.of(
            // REGULAR session (09:00-15:30)
            Arguments.of(LocalTime.of(8, 59, 59), TradingSession.REGULAR, false),
            Arguments.of(LocalTime.of(9, 0, 0), TradingSession.REGULAR, true),
            Arguments.of(LocalTime.of(12, 0, 0), TradingSession.REGULAR, true),
            Arguments.of(LocalTime.of(15, 30, 0), TradingSession.REGULAR, true),
            Arguments.of(LocalTime.of(15, 30, 1), TradingSession.REGULAR, false),

            // PRE_MARKET session (08:30-08:40)
            Arguments.of(LocalTime.of(8, 29, 59), TradingSession.PRE_MARKET, false),
            Arguments.of(LocalTime.of(8, 30, 0), TradingSession.PRE_MARKET, true),
            Arguments.of(LocalTime.of(8, 35, 0), TradingSession.PRE_MARKET, true),
            Arguments.of(LocalTime.of(8, 40, 0), TradingSession.PRE_MARKET, true),
            Arguments.of(LocalTime.of(8, 40, 1), TradingSession.PRE_MARKET, false),

            // AFTER_HOURS_CLOSING session (15:40-16:00)
            Arguments.of(LocalTime.of(15, 39, 59), TradingSession.AFTER_HOURS_CLOSING, false),
            Arguments.of(LocalTime.of(15, 40, 0), TradingSession.AFTER_HOURS_CLOSING, true),
            Arguments.of(LocalTime.of(15, 50, 0), TradingSession.AFTER_HOURS_CLOSING, true),
            Arguments.of(LocalTime.of(16, 0, 0), TradingSession.AFTER_HOURS_CLOSING, true),
            Arguments.of(LocalTime.of(16, 0, 1), TradingSession.AFTER_HOURS_CLOSING, false),

            // AFTER_HOURS session (16:00-18:00)
            Arguments.of(LocalTime.of(15, 59, 59), TradingSession.AFTER_HOURS, false),
            Arguments.of(LocalTime.of(16, 0, 0), TradingSession.AFTER_HOURS, true),
            Arguments.of(LocalTime.of(17, 0, 0), TradingSession.AFTER_HOURS, true),
            Arguments.of(LocalTime.of(18, 0, 0), TradingSession.AFTER_HOURS, true),
            Arguments.of(LocalTime.of(18, 0, 1), TradingSession.AFTER_HOURS, false)
        );
    }

    // ==================== 9. getCurrentSession Tests ====================

    @Test
    @DisplayName("getCurrentSession - 정규장 시간")
    void testGetCurrentSession_Regular() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 10, 0); // Thursday 10:00

        // When
        TradingSession session = policy.getCurrentSession(time);

        // Then
        assertThat(session).isEqualTo(TradingSession.REGULAR);
    }

    @Test
    @DisplayName("getCurrentSession - 시간외 단일가 장전")
    void testGetCurrentSession_PreMarket() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 8, 35); // Thursday 08:35

        // When
        TradingSession session = policy.getCurrentSession(time);

        // Then
        assertThat(session).isEqualTo(TradingSession.PRE_MARKET);
    }

    @Test
    @DisplayName("getCurrentSession - 시간외 종가")
    void testGetCurrentSession_AfterHoursClosing() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 15, 50); // Thursday 15:50

        // When
        TradingSession session = policy.getCurrentSession(time);

        // Then
        assertThat(session).isEqualTo(TradingSession.AFTER_HOURS_CLOSING);
    }

    @Test
    @DisplayName("getCurrentSession - 시간외 단일가 장후")
    void testGetCurrentSession_AfterHours() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 17, 0); // Thursday 17:00

        // When
        TradingSession session = policy.getCurrentSession(time);

        // Then
        assertThat(session).isEqualTo(TradingSession.AFTER_HOURS);
    }

    @Test
    @DisplayName("getCurrentSession - 세션 외 시간 (null 반환)")
    void testGetCurrentSession_OutsideAllSessions() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 8, 0); // Thursday 08:00 (before all sessions)

        // When
        TradingSession session = policy.getCurrentSession(time);

        // Then
        assertThat(session).isNull();
    }

    @Test
    @DisplayName("getCurrentSession - 주말 (null 반환)")
    void testGetCurrentSession_Weekend() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 4, 10, 0); // Saturday 10:00

        // When
        TradingSession session = policy.getCurrentSession(time);

        // Then
        assertThat(session).isNull();
    }

    // ==================== 10. getNextOpeningTime Tests ====================

    @Test
    @DisplayName("getNextOpeningTime - 정규장 이전 시간")
    void testGetNextOpeningTime_BeforeRegular() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 8, 0); // Thursday 08:00
        TradingSession targetSession = TradingSession.REGULAR;

        // When
        LocalDateTime nextOpening = policy.getNextOpeningTime(time, targetSession);

        // Then
        assertThat(nextOpening).isEqualTo(LocalDateTime.of(2025, 1, 2, 9, 0));
    }

    @Test
    @DisplayName("getNextOpeningTime - 정규장 이후 시간 (다음날)")
    void testGetNextOpeningTime_AfterRegular_NextDay() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 16, 0); // Thursday 16:00
        TradingSession targetSession = TradingSession.REGULAR;

        // When
        LocalDateTime nextOpening = policy.getNextOpeningTime(time, targetSession);

        // Then
        assertThat(nextOpening).isEqualTo(LocalDateTime.of(2025, 1, 3, 9, 0)); // Friday
    }

    @Test
    @DisplayName("getNextOpeningTime - 금요일 이후 (주말 건너뛰기)")
    void testGetNextOpeningTime_Friday_SkipWeekend() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 3, 16, 0); // Friday 16:00
        TradingSession targetSession = TradingSession.REGULAR;

        // When
        LocalDateTime nextOpening = policy.getNextOpeningTime(time, targetSession);

        // Then
        assertThat(nextOpening).isEqualTo(LocalDateTime.of(2025, 1, 6, 9, 0)); // Monday
    }
}
