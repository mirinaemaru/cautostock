package maru.trading.domain.backtest.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * CSV format configuration for data loading.
 *
 * Supports various CSV formats:
 * - STANDARD: timestamp,open,high,low,close,volume
 * - YAHOO: Date,Open,High,Low,Close,Adj Close,Volume
 * - INVESTING: Date,Price,Open,High,Low,Vol.
 * - CUSTOM: User-defined column mapping
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsvFormat {

    /**
     * Predefined CSV formats.
     */
    public enum PresetFormat {
        STANDARD,
        YAHOO,
        INVESTING,
        CUSTOM
    }

    @Builder.Default
    private PresetFormat preset = PresetFormat.STANDARD;

    @Builder.Default
    private boolean hasHeader = true;

    @Builder.Default
    private String delimiter = ",";

    @Builder.Default
    private String dateFormat = "yyyy-MM-dd HH:mm:ss";

    // Column indices (0-based)
    @Builder.Default
    private int timestampColumn = 0;

    @Builder.Default
    private int openColumn = 1;

    @Builder.Default
    private int highColumn = 2;

    @Builder.Default
    private int lowColumn = 3;

    @Builder.Default
    private int closeColumn = 4;

    @Builder.Default
    private int volumeColumn = 5;

    /**
     * Create standard format.
     * Columns: timestamp,open,high,low,close,volume
     */
    public static CsvFormat standard() {
        return CsvFormat.builder()
                .preset(PresetFormat.STANDARD)
                .build();
    }

    /**
     * Create Yahoo Finance format.
     * Columns: Date,Open,High,Low,Close,Adj Close,Volume
     */
    public static CsvFormat yahoo() {
        return CsvFormat.builder()
                .preset(PresetFormat.YAHOO)
                .dateFormat("yyyy-MM-dd")
                .timestampColumn(0)
                .openColumn(1)
                .highColumn(2)
                .lowColumn(3)
                .closeColumn(4)
                .volumeColumn(6)
                .build();
    }

    /**
     * Create Investing.com format.
     * Columns: Date,Price,Open,High,Low,Vol.
     */
    public static CsvFormat investing() {
        return CsvFormat.builder()
                .preset(PresetFormat.INVESTING)
                .dateFormat("MMM dd, yyyy")
                .timestampColumn(0)
                .openColumn(2)
                .highColumn(3)
                .lowColumn(4)
                .closeColumn(1)
                .volumeColumn(5)
                .build();
    }

    /**
     * Create custom format with specified column mapping.
     */
    public static CsvFormat custom(String delimiter, String dateFormat,
                                   int timestampCol, int openCol, int highCol,
                                   int lowCol, int closeCol, int volumeCol) {
        return CsvFormat.builder()
                .preset(PresetFormat.CUSTOM)
                .delimiter(delimiter)
                .dateFormat(dateFormat)
                .timestampColumn(timestampCol)
                .openColumn(openCol)
                .highColumn(highCol)
                .lowColumn(lowCol)
                .closeColumn(closeCol)
                .volumeColumn(volumeCol)
                .build();
    }
}
