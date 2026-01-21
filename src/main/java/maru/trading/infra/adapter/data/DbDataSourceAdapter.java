package maru.trading.infra.adapter.data;

import maru.trading.domain.backtest.data.BarData;
import maru.trading.domain.backtest.data.DataSource;
import maru.trading.domain.backtest.data.DataSourceType;
import maru.trading.infra.persistence.jpa.entity.HistoricalBarEntity;
import maru.trading.infra.persistence.jpa.repository.HistoricalBarJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Database data source adapter.
 *
 * Loads historical bar data from database (historical_bars table).
 * Default data source for backtests.
 */
@Component
public class DbDataSourceAdapter implements DataSource {

    private static final Logger log = LoggerFactory.getLogger(DbDataSourceAdapter.class);

    private final HistoricalBarJpaRepository historicalBarRepository;

    private List<BarData> allBars;
    private Iterator<BarData> iterator;
    private int currentIndex;

    public DbDataSourceAdapter(HistoricalBarJpaRepository historicalBarRepository) {
        this.historicalBarRepository = historicalBarRepository;
        this.allBars = new ArrayList<>();
        this.currentIndex = -1;
    }

    @Override
    public void initialize(List<String> symbols, LocalDate startDate, LocalDate endDate, String timeframe) {
        log.info("Initializing DB data source");
        log.info("Date range: {} to {}", startDate, endDate);
        log.info("Symbols: {}", symbols);
        log.info("Timeframe: {}", timeframe);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<HistoricalBarEntity> entities;
        if (symbols.size() == 1) {
            entities = historicalBarRepository.findBySymbolAndTimeframeAndDateRange(
                    symbols.get(0), timeframe, startDateTime, endDateTime);
        } else {
            entities = historicalBarRepository.findBySymbolsAndTimeframeAndDateRange(
                    symbols, timeframe, startDateTime, endDateTime);
        }

        // Convert entities to domain objects
        allBars = entities.stream()
                .map(this::toBarData)
                .collect(Collectors.toList());

        log.info("Loaded {} bars from database", allBars.size());

        if (allBars.isEmpty()) {
            log.warn("No historical data found for the specified criteria");
        }

        // Initialize iterator
        iterator = allBars.iterator();
        currentIndex = -1;
    }

    @Override
    public boolean hasNext() {
        return iterator != null && iterator.hasNext();
    }

    @Override
    public BarData next() {
        if (!hasNext()) {
            throw new IllegalStateException("No more data available");
        }

        BarData bar = iterator.next();
        currentIndex++;

        if (currentIndex % 1000 == 0 && currentIndex > 0) {
            log.debug("Processed {} / {} bars ({} %)",
                    currentIndex, allBars.size(),
                    String.format("%.1f", getProgress()));
        }

        return bar;
    }

    @Override
    public void reset() {
        if (allBars != null) {
            iterator = allBars.iterator();
            currentIndex = -1;
            log.info("Data source reset");
        }
    }

    @Override
    public int getTotalBars() {
        return allBars != null ? allBars.size() : 0;
    }

    @Override
    public int getCurrentIndex() {
        return currentIndex;
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.DATABASE;
    }

    @Override
    public void close() {
        // No resources to close for DB source
        allBars = null;
        iterator = null;
        currentIndex = -1;
    }

    @Override
    public List<BarData> getAllBars() {
        return new ArrayList<>(allBars);
    }

    private BarData toBarData(HistoricalBarEntity entity) {
        return BarData.builder()
                .symbol(entity.getSymbol())
                .timeframe(entity.getTimeframe())
                .timestamp(entity.getBarTimestamp())
                .open(entity.getOpenPrice())
                .high(entity.getHighPrice())
                .low(entity.getLowPrice())
                .close(entity.getClosePrice())
                .volume(entity.getVolume())
                .build();
    }
}
