package maru.trading.domain.backtest.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Data source configuration for backtest.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataSourceConfig {

    /**
     * Data source type.
     */
    @Builder.Default
    private DataSourceType type = DataSourceType.DATABASE;

    /**
     * CSV file/directory path (for CSV data source).
     */
    private String csvPath;

    /**
     * CSV format configuration (for CSV data source).
     */
    private CsvFormat csvFormat;

    /**
     * Create database data source config.
     */
    public static DataSourceConfig database() {
        return DataSourceConfig.builder()
                .type(DataSourceType.DATABASE)
                .build();
    }

    /**
     * Create CSV data source config with standard format.
     */
    public static DataSourceConfig csv(String path) {
        return DataSourceConfig.builder()
                .type(DataSourceType.CSV)
                .csvPath(path)
                .csvFormat(CsvFormat.standard())
                .build();
    }

    /**
     * Create CSV data source config with custom format.
     */
    public static DataSourceConfig csv(String path, CsvFormat format) {
        return DataSourceConfig.builder()
                .type(DataSourceType.CSV)
                .csvPath(path)
                .csvFormat(format)
                .build();
    }

    /**
     * Create CSV data source config for Yahoo Finance format.
     */
    public static DataSourceConfig yahooCsv(String path) {
        return DataSourceConfig.builder()
                .type(DataSourceType.CSV)
                .csvPath(path)
                .csvFormat(CsvFormat.yahoo())
                .build();
    }

    /**
     * Create CSV data source config for Investing.com format.
     */
    public static DataSourceConfig investingCsv(String path) {
        return DataSourceConfig.builder()
                .type(DataSourceType.CSV)
                .csvPath(path)
                .csvFormat(CsvFormat.investing())
                .build();
    }
}
