package maru.trading.domain.backtest.data;

import maru.trading.infra.adapter.data.CsvDataSourceAdapter;
import maru.trading.infra.adapter.data.DbDataSourceAdapter;
import maru.trading.infra.persistence.jpa.repository.HistoricalBarJpaRepository;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Factory for creating data source instances.
 *
 * Creates appropriate DataSource implementation based on configuration.
 */
@Component
public class DataSourceFactory {

    private final HistoricalBarJpaRepository historicalBarRepository;

    public DataSourceFactory(HistoricalBarJpaRepository historicalBarRepository) {
        this.historicalBarRepository = historicalBarRepository;
    }

    /**
     * Create data source from configuration.
     *
     * @param config Data source configuration
     * @return DataSource instance
     */
    public DataSource create(DataSourceConfig config) {
        if (config == null || config.getType() == null) {
            // Default to database
            return new DbDataSourceAdapter(historicalBarRepository);
        }

        switch (config.getType()) {
            case DATABASE:
                return new DbDataSourceAdapter(historicalBarRepository);

            case CSV:
                Path dataPath = config.getCsvPath() != null
                        ? Paths.get(config.getCsvPath())
                        : Paths.get("data/backtest");

                CsvFormat format = config.getCsvFormat() != null
                        ? config.getCsvFormat()
                        : CsvFormat.standard();

                return new CsvDataSourceAdapter(dataPath, format);

            case REALTIME:
                throw new UnsupportedOperationException("Realtime data source not yet implemented");

            default:
                return new DbDataSourceAdapter(historicalBarRepository);
        }
    }

    /**
     * Create database data source.
     */
    public DataSource createDbSource() {
        return new DbDataSourceAdapter(historicalBarRepository);
    }

    /**
     * Create CSV data source with standard format.
     *
     * @param dataDirectory Directory containing CSV files
     */
    public DataSource createCsvSource(String dataDirectory) {
        return new CsvDataSourceAdapter(Paths.get(dataDirectory));
    }

    /**
     * Create CSV data source with specified format.
     *
     * @param dataDirectory Directory containing CSV files
     * @param format CSV format configuration
     */
    public DataSource createCsvSource(String dataDirectory, CsvFormat format) {
        return new CsvDataSourceAdapter(Paths.get(dataDirectory), format);
    }

    /**
     * Create CSV data source for Yahoo Finance format.
     *
     * @param dataDirectory Directory containing CSV files
     */
    public DataSource createYahooCsvSource(String dataDirectory) {
        return new CsvDataSourceAdapter(Paths.get(dataDirectory), CsvFormat.yahoo());
    }

    /**
     * Create CSV data source for Investing.com format.
     *
     * @param dataDirectory Directory containing CSV files
     */
    public DataSource createInvestingCsvSource(String dataDirectory) {
        return new CsvDataSourceAdapter(Paths.get(dataDirectory), CsvFormat.investing());
    }
}
