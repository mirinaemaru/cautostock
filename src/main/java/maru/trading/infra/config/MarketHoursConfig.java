package maru.trading.infra.config;

import maru.trading.domain.market.TradingSession;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Market hours configuration.
 *
 * Maps to trading.market.* properties in application.yml.
 *
 * Provides:
 * - Enabled/disabled flag for market hours checking
 * - Allowed trading sessions
 * - Public holidays list
 */
@Configuration
@ConfigurationProperties(prefix = "trading.market")
public class MarketHoursConfig {

    private boolean checkEnabled = true;
    private List<String> allowedSessions = new ArrayList<>();
    private List<String> publicHolidays = new ArrayList<>();

    public boolean isCheckEnabled() {
        return checkEnabled;
    }

    public void setCheckEnabled(boolean checkEnabled) {
        this.checkEnabled = checkEnabled;
    }

    public List<String> getAllowedSessions() {
        return allowedSessions;
    }

    public void setAllowedSessions(List<String> allowedSessions) {
        this.allowedSessions = allowedSessions;
    }

    public List<String> getPublicHolidays() {
        return publicHolidays;
    }

    public void setPublicHolidays(List<String> publicHolidays) {
        this.publicHolidays = publicHolidays;
    }

    /**
     * Parse allowed sessions from config strings.
     *
     * @return Set of TradingSession enums
     */
    public Set<TradingSession> getAllowedSessionsAsEnum() {
        Set<TradingSession> sessions = new HashSet<>();

        if (allowedSessions == null || allowedSessions.isEmpty()) {
            // Default: REGULAR session only
            sessions.add(TradingSession.REGULAR);
            return sessions;
        }

        for (String sessionStr : allowedSessions) {
            try {
                TradingSession session = TradingSession.valueOf(sessionStr.trim().toUpperCase());
                sessions.add(session);
            } catch (IllegalArgumentException e) {
                // Skip invalid session names
                System.err.println("Invalid trading session in config: " + sessionStr);
            }
        }

        // If parsing failed, default to REGULAR
        if (sessions.isEmpty()) {
            sessions.add(TradingSession.REGULAR);
        }

        return sessions;
    }

    /**
     * Parse public holidays from config strings.
     *
     * Expected format: "2025-01-01", "2025-02-09", etc.
     *
     * @return Set of LocalDate for public holidays
     */
    public Set<LocalDate> getPublicHolidaysAsDate() {
        Set<LocalDate> holidays = new HashSet<>();

        if (publicHolidays == null || publicHolidays.isEmpty()) {
            return holidays; // Empty set = no holidays configured
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (String dateStr : publicHolidays) {
            try {
                LocalDate date = LocalDate.parse(dateStr.trim(), formatter);
                holidays.add(date);
            } catch (Exception e) {
                // Skip invalid dates
                System.err.println("Invalid date format in public holidays config: " + dateStr);
            }
        }

        return holidays;
    }

    /**
     * Check if a specific session is allowed.
     */
    public boolean isSessionAllowed(TradingSession session) {
        return getAllowedSessionsAsEnum().contains(session);
    }
}
