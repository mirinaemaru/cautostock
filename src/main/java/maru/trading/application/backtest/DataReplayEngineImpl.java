package maru.trading.application.backtest;

import maru.trading.domain.backtest.BacktestConfig;
import maru.trading.domain.backtest.DataReplayEngine;
import maru.trading.infra.persistence.jpa.entity.HistoricalBarEntity;
import maru.trading.infra.persistence.jpa.repository.HistoricalBarJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Data Replay Engine implementation.
 *
 * Replays historical market data in chronological order using Iterator pattern.
 * Prevents lookahead bias by only providing data up to current timestamp.
 */
@Component
public class DataReplayEngineImpl implements DataReplayEngine {

    private static final Logger log = LoggerFactory.getLogger(DataReplayEngineImpl.class);

    private final HistoricalBarJpaRepository historicalBarRepository;

    private Iterator<HistoricalBarEntity> iterator;
    private List<HistoricalBarEntity> allBars;
    private int currentIndex;
    private LocalDateTime currentTime;

    public DataReplayEngineImpl(HistoricalBarJpaRepository historicalBarRepository) {
        this.historicalBarRepository = historicalBarRepository;
    }

    @Override
    public void loadData(BacktestConfig config) {
        log.info("Loading historical data for backtest: {}", config.getBacktestId());
        log.info("Date range: {} to {}", config.getStartDate(), config.getEndDate());
        log.info("Symbols: {}", config.getSymbols());
        log.info("Timeframe: {}", config.getTimeframe());

        // Convert LocalDate to LocalDateTime
        LocalDateTime startDateTime = config.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = config.getEndDate().atTime(23, 59, 59);

        // Load data from database
        if (config.getSymbols().size() == 1) {
            // Single symbol
            String symbol = config.getSymbols().get(0);
            allBars = historicalBarRepository.findBySymbolAndTimeframeAndDateRange(
                    symbol,
                    config.getTimeframe(),
                    startDateTime,
                    endDateTime
            );
        } else {
            // Multiple symbols
            allBars = historicalBarRepository.findBySymbolsAndTimeframeAndDateRange(
                    config.getSymbols(),
                    config.getTimeframe(),
                    startDateTime,
                    endDateTime
            );
        }

        log.info("Loaded {} bars for replay", allBars.size());

        if (allBars.isEmpty()) {
            log.warn("No historical data found for the specified criteria");
        }

        // Initialize iterator
        iterator = allBars.iterator();
        currentIndex = -1;
        currentTime = null;
    }

    @Override
    public boolean hasNext() {
        return iterator != null && iterator.hasNext();
    }

    @Override
    public HistoricalBarEntity next() {
        if (!hasNext()) {
            throw new IllegalStateException("No more data available");
        }

        HistoricalBarEntity bar = iterator.next();
        currentIndex++;
        currentTime = bar.getBarTimestamp();

        if (currentIndex % 1000 == 0 && currentIndex > 0) {
            log.debug("Replayed {} / {} bars ({} %)",
                    currentIndex, allBars.size(),
                    String.format("%.1f", (currentIndex * 100.0) / allBars.size()));
        }

        return bar;
    }

    @Override
    public void reset() {
        if (allBars != null) {
            iterator = allBars.iterator();
            currentIndex = -1;
            currentTime = null;
            log.info("Data replay reset");
        }
    }

    @Override
    public LocalDateTime getCurrentTime() {
        return currentTime;
    }

    @Override
    public int getTotalBars() {
        return allBars != null ? allBars.size() : 0;
    }

    @Override
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Get progress percentage.
     *
     * @return Progress percentage (0-100)
     */
    public double getProgress() {
        if (allBars == null || allBars.isEmpty()) {
            return 0.0;
        }
        return (currentIndex + 1) * 100.0 / allBars.size();
    }

    /**
     * Get all bars loaded (for testing).
     *
     * @return All bars
     */
    List<HistoricalBarEntity> getAllBars() {
        return new ArrayList<>(allBars);
    }
}
