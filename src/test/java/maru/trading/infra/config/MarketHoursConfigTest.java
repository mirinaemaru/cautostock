package maru.trading.infra.config;

import maru.trading.domain.market.TradingSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MarketHoursConfig Test")
class MarketHoursConfigTest {

    private MarketHoursConfig config;

    @BeforeEach
    void setUp() {
        config = new MarketHoursConfig();
    }

    @Nested
    @DisplayName("checkEnabled Tests")
    class CheckEnabledTests {

        @Test
        @DisplayName("Should be enabled by default")
        void shouldBeEnabledByDefault() {
            // Then
            assertThat(config.isCheckEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should allow setting checkEnabled")
        void shouldAllowSettingCheckEnabled() {
            // When
            config.setCheckEnabled(false);

            // Then
            assertThat(config.isCheckEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("getAllowedSessionsAsEnum() Tests")
    class GetAllowedSessionsAsEnumTests {

        @Test
        @DisplayName("Should return REGULAR by default when no sessions configured")
        void shouldReturnRegularByDefaultWhenNoSessionsConfigured() {
            // Given - no sessions set

            // When
            Set<TradingSession> sessions = config.getAllowedSessionsAsEnum();

            // Then
            assertThat(sessions).containsExactly(TradingSession.REGULAR);
        }

        @Test
        @DisplayName("Should return REGULAR by default when sessions is null")
        void shouldReturnRegularByDefaultWhenSessionsIsNull() {
            // Given
            config.setAllowedSessions(null);

            // When
            Set<TradingSession> sessions = config.getAllowedSessionsAsEnum();

            // Then
            assertThat(sessions).containsExactly(TradingSession.REGULAR);
        }

        @Test
        @DisplayName("Should return REGULAR by default when sessions is empty")
        void shouldReturnRegularByDefaultWhenSessionsIsEmpty() {
            // Given
            config.setAllowedSessions(new ArrayList<>());

            // When
            Set<TradingSession> sessions = config.getAllowedSessionsAsEnum();

            // Then
            assertThat(sessions).containsExactly(TradingSession.REGULAR);
        }

        @Test
        @DisplayName("Should parse single session")
        void shouldParseSingleSession() {
            // Given
            config.setAllowedSessions(List.of("PRE_MARKET"));

            // When
            Set<TradingSession> sessions = config.getAllowedSessionsAsEnum();

            // Then
            assertThat(sessions).containsExactly(TradingSession.PRE_MARKET);
        }

        @Test
        @DisplayName("Should parse multiple sessions")
        void shouldParseMultipleSessions() {
            // Given
            config.setAllowedSessions(List.of("REGULAR", "PRE_MARKET", "AFTER_HOURS"));

            // When
            Set<TradingSession> sessions = config.getAllowedSessionsAsEnum();

            // Then
            assertThat(sessions).containsExactlyInAnyOrder(
                    TradingSession.REGULAR,
                    TradingSession.PRE_MARKET,
                    TradingSession.AFTER_HOURS
            );
        }

        @Test
        @DisplayName("Should handle case-insensitive session names")
        void shouldHandleCaseInsensitiveSessionNames() {
            // Given
            config.setAllowedSessions(List.of("regular", "pre_market"));

            // When
            Set<TradingSession> sessions = config.getAllowedSessionsAsEnum();

            // Then
            assertThat(sessions).containsExactlyInAnyOrder(
                    TradingSession.REGULAR,
                    TradingSession.PRE_MARKET
            );
        }

        @Test
        @DisplayName("Should skip invalid session names")
        void shouldSkipInvalidSessionNames() {
            // Given
            config.setAllowedSessions(List.of("REGULAR", "INVALID_SESSION", "AFTER_HOURS"));

            // When
            Set<TradingSession> sessions = config.getAllowedSessionsAsEnum();

            // Then
            assertThat(sessions).containsExactlyInAnyOrder(
                    TradingSession.REGULAR,
                    TradingSession.AFTER_HOURS
            );
        }

        @Test
        @DisplayName("Should return REGULAR when all sessions are invalid")
        void shouldReturnRegularWhenAllSessionsAreInvalid() {
            // Given
            config.setAllowedSessions(List.of("INVALID1", "INVALID2"));

            // When
            Set<TradingSession> sessions = config.getAllowedSessionsAsEnum();

            // Then
            assertThat(sessions).containsExactly(TradingSession.REGULAR);
        }

        @Test
        @DisplayName("Should trim whitespace from session names")
        void shouldTrimWhitespaceFromSessionNames() {
            // Given
            config.setAllowedSessions(List.of("  REGULAR  ", "  PRE_MARKET  "));

            // When
            Set<TradingSession> sessions = config.getAllowedSessionsAsEnum();

            // Then
            assertThat(sessions).containsExactlyInAnyOrder(
                    TradingSession.REGULAR,
                    TradingSession.PRE_MARKET
            );
        }
    }

    @Nested
    @DisplayName("getPublicHolidaysAsDate() Tests")
    class GetPublicHolidaysAsDateTests {

        @Test
        @DisplayName("Should return empty set when no holidays configured")
        void shouldReturnEmptySetWhenNoHolidaysConfigured() {
            // When
            Set<LocalDate> holidays = config.getPublicHolidaysAsDate();

            // Then
            assertThat(holidays).isEmpty();
        }

        @Test
        @DisplayName("Should return empty set when holidays is null")
        void shouldReturnEmptySetWhenHolidaysIsNull() {
            // Given
            config.setPublicHolidays(null);

            // When
            Set<LocalDate> holidays = config.getPublicHolidaysAsDate();

            // Then
            assertThat(holidays).isEmpty();
        }

        @Test
        @DisplayName("Should parse valid date formats")
        void shouldParseValidDateFormats() {
            // Given
            config.setPublicHolidays(List.of("2025-01-01", "2025-02-09", "2025-03-01"));

            // When
            Set<LocalDate> holidays = config.getPublicHolidaysAsDate();

            // Then
            assertThat(holidays).containsExactlyInAnyOrder(
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 2, 9),
                    LocalDate.of(2025, 3, 1)
            );
        }

        @Test
        @DisplayName("Should skip invalid date formats")
        void shouldSkipInvalidDateFormats() {
            // Given
            config.setPublicHolidays(List.of("2025-01-01", "invalid-date", "01/01/2025", "2025-02-28"));

            // When
            Set<LocalDate> holidays = config.getPublicHolidaysAsDate();

            // Then
            assertThat(holidays).containsExactlyInAnyOrder(
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 2, 28)
            );
        }

        @Test
        @DisplayName("Should trim whitespace from dates")
        void shouldTrimWhitespaceFromDates() {
            // Given
            config.setPublicHolidays(List.of("  2025-01-01  ", "  2025-12-25  "));

            // When
            Set<LocalDate> holidays = config.getPublicHolidaysAsDate();

            // Then
            assertThat(holidays).containsExactlyInAnyOrder(
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 12, 25)
            );
        }
    }

    @Nested
    @DisplayName("isSessionAllowed() Tests")
    class IsSessionAllowedTests {

        @Test
        @DisplayName("Should return true for allowed session")
        void shouldReturnTrueForAllowedSession() {
            // Given
            config.setAllowedSessions(List.of("REGULAR", "PRE_MARKET"));

            // Then
            assertThat(config.isSessionAllowed(TradingSession.REGULAR)).isTrue();
            assertThat(config.isSessionAllowed(TradingSession.PRE_MARKET)).isTrue();
        }

        @Test
        @DisplayName("Should return false for disallowed session")
        void shouldReturnFalseForDisallowedSession() {
            // Given
            config.setAllowedSessions(List.of("REGULAR"));

            // Then
            assertThat(config.isSessionAllowed(TradingSession.AFTER_HOURS)).isFalse();
            assertThat(config.isSessionAllowed(TradingSession.AFTER_HOURS_CLOSING)).isFalse();
        }

        @Test
        @DisplayName("Should only allow REGULAR by default")
        void shouldOnlyAllowRegularByDefault() {
            // Then
            assertThat(config.isSessionAllowed(TradingSession.REGULAR)).isTrue();
            assertThat(config.isSessionAllowed(TradingSession.PRE_MARKET)).isFalse();
            assertThat(config.isSessionAllowed(TradingSession.AFTER_HOURS)).isFalse();
        }
    }

    @Nested
    @DisplayName("Setter/Getter Tests")
    class SetterGetterTests {

        @Test
        @DisplayName("Should set and get allowed sessions")
        void shouldSetAndGetAllowedSessions() {
            // Given
            List<String> sessions = List.of("REGULAR", "AFTER_HOURS");

            // When
            config.setAllowedSessions(sessions);

            // Then
            assertThat(config.getAllowedSessions()).isEqualTo(sessions);
        }

        @Test
        @DisplayName("Should set and get public holidays")
        void shouldSetAndGetPublicHolidays() {
            // Given
            List<String> holidays = List.of("2025-01-01", "2025-12-25");

            // When
            config.setPublicHolidays(holidays);

            // Then
            assertThat(config.getPublicHolidays()).isEqualTo(holidays);
        }
    }
}
