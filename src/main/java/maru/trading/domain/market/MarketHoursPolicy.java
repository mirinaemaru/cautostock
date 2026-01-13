package maru.trading.domain.market;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Market hours validation policy for Korean stock market.
 *
 * Validates if current time is within allowed trading hours.
 *
 * Features:
 * - Weekend detection (Saturday/Sunday = closed)
 * - Public holiday checking (configurable)
 * - Session-based time validation
 * - Multiple session support
 *
 * Stateless policy class - thread-safe.
 */
public class MarketHoursPolicy {

    /**
     * Check if market is open at given time.
     *
     * @param now Current time
     * @param allowedSessions Set of allowed trading sessions
     * @param publicHolidays Set of public holiday dates
     * @return true if market is open, false otherwise
     */
    public boolean isMarketOpen(
            LocalDateTime now,
            Set<TradingSession> allowedSessions,
            Set<LocalDate> publicHolidays) {

        if (now == null) {
            throw new IllegalArgumentException("Current time cannot be null");
        }

        if (allowedSessions == null || allowedSessions.isEmpty()) {
            throw new IllegalArgumentException("Allowed sessions cannot be null or empty");
        }

        // 1. Check weekend
        if (isWeekend(now)) {
            return false;
        }

        // 2. Check public holiday
        if (isPublicHoliday(now.toLocalDate(), publicHolidays)) {
            return false;
        }

        // 3. Check if current time falls within any allowed session
        LocalTime time = now.toLocalTime();

        for (TradingSession session : allowedSessions) {
            if (isWithinSession(time, session)) {
                return true;
            }
        }

        return false; // Not within any allowed session
    }

    /**
     * Check if market is open for specific session.
     *
     * @param now Current time
     * @param session Trading session to check
     * @param publicHolidays Set of public holiday dates
     * @return true if market is open for this session
     */
    public boolean isMarketOpen(
            LocalDateTime now,
            TradingSession session,
            Set<LocalDate> publicHolidays) {

        if (session == null) {
            throw new IllegalArgumentException("Session cannot be null");
        }

        Set<TradingSession> sessions = new HashSet<>();
        sessions.add(session);

        return isMarketOpen(now, sessions, publicHolidays);
    }

    /**
     * Check if given time is within trading session.
     *
     * @param time Time to check
     * @param session Trading session
     * @return true if time is within session hours
     */
    public boolean isWithinSession(LocalTime time, TradingSession session) {
        if (time == null || session == null) {
            return false;
        }

        return switch (session) {
            case REGULAR -> isRegularSession(time);
            case PRE_MARKET -> isPreMarket(time);
            case AFTER_HOURS_CLOSING -> isAfterHoursClosing(time);
            case AFTER_HOURS -> isAfterHours(time);
        };
    }

    /**
     * Check if regular trading session (09:00 - 15:30).
     */
    private boolean isRegularSession(LocalTime time) {
        return !time.isBefore(LocalTime.of(9, 0))
                && !time.isAfter(LocalTime.of(15, 30));
    }

    /**
     * Check if pre-market session (08:30 - 08:40).
     */
    private boolean isPreMarket(LocalTime time) {
        return !time.isBefore(LocalTime.of(8, 30))
                && !time.isAfter(LocalTime.of(8, 40));
    }

    /**
     * Check if after-hours closing session (15:40 - 16:00).
     */
    private boolean isAfterHoursClosing(LocalTime time) {
        return !time.isBefore(LocalTime.of(15, 40))
                && !time.isAfter(LocalTime.of(16, 0));
    }

    /**
     * Check if after-hours session (16:00 - 18:00).
     */
    private boolean isAfterHours(LocalTime time) {
        return !time.isBefore(LocalTime.of(16, 0))
                && !time.isAfter(LocalTime.of(18, 0));
    }

    /**
     * Check if given date is weekend.
     */
    private boolean isWeekend(LocalDateTime dateTime) {
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * Check if given date is public holiday.
     */
    private boolean isPublicHoliday(LocalDate date, Set<LocalDate> publicHolidays) {
        if (publicHolidays == null || publicHolidays.isEmpty()) {
            return false;
        }
        return publicHolidays.contains(date);
    }

    /**
     * Get current session (if market is open).
     *
     * @param now Current time
     * @return Current trading session, or null if market is closed
     */
    public TradingSession getCurrentSession(LocalDateTime now) {
        if (now == null) {
            return null;
        }

        if (isWeekend(now)) {
            return null;
        }

        LocalTime time = now.toLocalTime();

        // Check in order of session start time
        if (isWithinSession(time, TradingSession.PRE_MARKET)) {
            return TradingSession.PRE_MARKET;
        }
        if (isWithinSession(time, TradingSession.REGULAR)) {
            return TradingSession.REGULAR;
        }
        if (isWithinSession(time, TradingSession.AFTER_HOURS_CLOSING)) {
            return TradingSession.AFTER_HOURS_CLOSING;
        }
        if (isWithinSession(time, TradingSession.AFTER_HOURS)) {
            return TradingSession.AFTER_HOURS;
        }

        return null; // Market is closed
    }

    /**
     * Get next market opening time.
     *
     * @param now Current time
     * @param session Target session
     * @return Next opening time for the session
     */
    public LocalDateTime getNextOpeningTime(LocalDateTime now, TradingSession session) {
        if (now == null || session == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        LocalDate date = now.toLocalDate();
        LocalTime sessionStart = getSessionStartTime(session);

        // If before session today, return today's session start
        if (now.toLocalTime().isBefore(sessionStart)) {
            return LocalDateTime.of(date, sessionStart);
        }

        // Otherwise, return next business day's session start
        LocalDate nextBusinessDay = getNextBusinessDay(date);
        return LocalDateTime.of(nextBusinessDay, sessionStart);
    }

    /**
     * Get session start time.
     */
    private LocalTime getSessionStartTime(TradingSession session) {
        return switch (session) {
            case PRE_MARKET -> LocalTime.of(8, 30);
            case REGULAR -> LocalTime.of(9, 0);
            case AFTER_HOURS_CLOSING -> LocalTime.of(15, 40);
            case AFTER_HOURS -> LocalTime.of(16, 0);
        };
    }

    /**
     * Get next business day (skip weekends).
     */
    private LocalDate getNextBusinessDay(LocalDate date) {
        LocalDate nextDay = date.plusDays(1);

        while (nextDay.getDayOfWeek() == DayOfWeek.SATURDAY
                || nextDay.getDayOfWeek() == DayOfWeek.SUNDAY) {
            nextDay = nextDay.plusDays(1);
        }

        return nextDay;
    }
}
