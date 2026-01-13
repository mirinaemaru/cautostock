package maru.trading.domain.market;

/**
 * Trading session types for Korean stock market.
 *
 * Defines different trading periods throughout the day:
 * - REGULAR: Main trading hours (09:00 - 15:30)
 * - PRE_MARKET: Pre-market single price auction (08:30 - 08:40)
 * - AFTER_HOURS_CLOSING: After-hours closing auction (15:40 - 16:00)
 * - AFTER_HOURS: After-hours trading (16:00 - 18:00)
 */
public enum TradingSession {

    /**
     * Regular trading session (정규장)
     * 09:00 - 15:30
     */
    REGULAR("정규장", "09:00-15:30"),

    /**
     * Pre-market single price auction (시간외 단일가 - 장 시작 전)
     * 08:30 - 08:40
     */
    PRE_MARKET("시간외 단일가(장전)", "08:30-08:40"),

    /**
     * After-hours closing auction (시간외 종가)
     * 15:40 - 16:00
     */
    AFTER_HOURS_CLOSING("시간외 종가", "15:40-16:00"),

    /**
     * After-hours trading (시간외 단일가 - 장 마감 후)
     * 16:00 - 18:00
     */
    AFTER_HOURS("시간외 단일가(장후)", "16:00-18:00");

    private final String koreanName;
    private final String timeRange;

    TradingSession(String koreanName, String timeRange) {
        this.koreanName = koreanName;
        this.timeRange = timeRange;
    }

    public String getKoreanName() {
        return koreanName;
    }

    public String getTimeRange() {
        return timeRange;
    }

    @Override
    public String toString() {
        return koreanName + " (" + timeRange + ")";
    }
}
