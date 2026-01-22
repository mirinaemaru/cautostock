package maru.trading.domain.market;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TradingSession Enum Test")
class TradingSessionTest {

    @Test
    @DisplayName("Should have REGULAR session")
    void shouldHaveRegularSession() {
        assertThat(TradingSession.REGULAR).isNotNull();
        assertThat(TradingSession.REGULAR.getKoreanName()).isEqualTo("정규장");
        assertThat(TradingSession.REGULAR.getTimeRange()).isEqualTo("09:00-15:30");
    }

    @Test
    @DisplayName("Should have PRE_MARKET session")
    void shouldHavePreMarketSession() {
        assertThat(TradingSession.PRE_MARKET).isNotNull();
        assertThat(TradingSession.PRE_MARKET.getKoreanName()).isEqualTo("시간외 단일가(장전)");
        assertThat(TradingSession.PRE_MARKET.getTimeRange()).isEqualTo("08:30-08:40");
    }

    @Test
    @DisplayName("Should have AFTER_HOURS_CLOSING session")
    void shouldHaveAfterHoursClosingSession() {
        assertThat(TradingSession.AFTER_HOURS_CLOSING).isNotNull();
        assertThat(TradingSession.AFTER_HOURS_CLOSING.getKoreanName()).isEqualTo("시간외 종가");
        assertThat(TradingSession.AFTER_HOURS_CLOSING.getTimeRange()).isEqualTo("15:40-16:00");
    }

    @Test
    @DisplayName("Should have AFTER_HOURS session")
    void shouldHaveAfterHoursSession() {
        assertThat(TradingSession.AFTER_HOURS).isNotNull();
        assertThat(TradingSession.AFTER_HOURS.getKoreanName()).isEqualTo("시간외 단일가(장후)");
        assertThat(TradingSession.AFTER_HOURS.getTimeRange()).isEqualTo("16:00-18:00");
    }

    @Test
    @DisplayName("Should have exactly 4 sessions")
    void shouldHaveExactly4Sessions() {
        assertThat(TradingSession.values()).hasSize(4);
    }

    @Test
    @DisplayName("Should convert from string")
    void shouldConvertFromString() {
        assertThat(TradingSession.valueOf("REGULAR")).isEqualTo(TradingSession.REGULAR);
        assertThat(TradingSession.valueOf("PRE_MARKET")).isEqualTo(TradingSession.PRE_MARKET);
        assertThat(TradingSession.valueOf("AFTER_HOURS_CLOSING")).isEqualTo(TradingSession.AFTER_HOURS_CLOSING);
        assertThat(TradingSession.valueOf("AFTER_HOURS")).isEqualTo(TradingSession.AFTER_HOURS);
    }

    @Test
    @DisplayName("Should have toString with Korean name and time range")
    void shouldHaveToStringWithKoreanNameAndTimeRange() {
        String toString = TradingSession.REGULAR.toString();

        assertThat(toString).contains("정규장");
        assertThat(toString).contains("09:00-15:30");
    }
}
