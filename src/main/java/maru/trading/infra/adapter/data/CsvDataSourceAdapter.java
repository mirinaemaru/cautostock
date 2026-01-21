package maru.trading.infra.adapter.data;

import maru.trading.domain.backtest.data.BarData;
import maru.trading.domain.backtest.data.CsvFormat;
import maru.trading.domain.backtest.data.DataSource;
import maru.trading.domain.backtest.data.DataSourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CSV file data source adapter.
 *
 * Loads historical bar data from CSV files.
 * Supports multiple CSV formats (Standard, Yahoo, Investing, Custom).
 *
 * File naming convention: {symbol}_{timeframe}.csv
 * Example: 005930_1d.csv, AAPL_1m.csv
 */
public class CsvDataSourceAdapter implements DataSource {

    private static final Logger log = LoggerFactory.getLogger(CsvDataSourceAdapter.class);

    private final Path dataDirectory;
    private final CsvFormat format;

    private List<BarData> allBars;
    private Iterator<BarData> iterator;
    private int currentIndex;
    private String defaultSymbol;
    private String defaultTimeframe;

    /**
     * Create CSV data source with default format.
     *
     * @param dataDirectory Directory containing CSV files
     */
    public CsvDataSourceAdapter(Path dataDirectory) {
        this(dataDirectory, CsvFormat.standard());
    }

    /**
     * Create CSV data source with specified format.
     *
     * @param dataDirectory Directory containing CSV files
     * @param format CSV format configuration
     */
    public CsvDataSourceAdapter(Path dataDirectory, CsvFormat format) {
        this.dataDirectory = dataDirectory;
        this.format = format;
        this.allBars = new ArrayList<>();
        this.currentIndex = -1;
    }

    /**
     * Create CSV data source from a single file.
     *
     * @param csvFile CSV file path
     * @param symbol Symbol for all bars in the file
     * @param timeframe Timeframe for all bars in the file
     * @param format CSV format configuration
     */
    public CsvDataSourceAdapter(Path csvFile, String symbol, String timeframe, CsvFormat format) {
        this.dataDirectory = csvFile.getParent();
        this.format = format;
        this.allBars = new ArrayList<>();
        this.currentIndex = -1;
        this.defaultSymbol = symbol;
        this.defaultTimeframe = timeframe;
    }

    @Override
    public void initialize(List<String> symbols, LocalDate startDate, LocalDate endDate, String timeframe) {
        log.info("Initializing CSV data source from: {}", dataDirectory);
        log.info("Date range: {} to {}", startDate, endDate);
        log.info("Symbols: {}", symbols);
        log.info("Timeframe: {}", timeframe);

        allBars = new ArrayList<>();

        for (String symbol : symbols) {
            try {
                List<BarData> symbolBars = loadSymbolData(symbol, timeframe, startDate, endDate);
                allBars.addAll(symbolBars);
                log.info("Loaded {} bars for symbol {}", symbolBars.size(), symbol);
            } catch (IOException e) {
                log.warn("Failed to load data for symbol {}: {}", symbol, e.getMessage());
            }
        }

        // Sort by timestamp
        allBars.sort(Comparator.comparing(BarData::getTimestamp));

        log.info("Total bars loaded: {}", allBars.size());

        if (allBars.isEmpty()) {
            log.warn("No CSV data found for the specified criteria");
        }

        // Initialize iterator
        iterator = allBars.iterator();
        currentIndex = -1;
    }

    /**
     * Load data from a single CSV file.
     *
     * @param filePath CSV file path
     * @param symbol Symbol for all bars
     * @param timeframe Timeframe for all bars
     * @param startDate Start date filter (optional)
     * @param endDate End date filter (optional)
     * @return List of bar data
     * @throws IOException if file cannot be read
     */
    public List<BarData> loadFromFile(Path filePath, String symbol, String timeframe,
                                       LocalDate startDate, LocalDate endDate) throws IOException {
        log.info("Loading CSV file: {}", filePath);

        if (!Files.exists(filePath)) {
            throw new IOException("CSV file not found: " + filePath);
        }

        List<BarData> bars = new ArrayList<>();
        DateTimeFormatter dateFormatter = createDateFormatter();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip header if configured
                if (lineNumber == 1 && format.isHasHeader()) {
                    continue;
                }

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    BarData bar = parseLine(line, symbol, timeframe, dateFormatter);
                    if (bar != null && isInDateRange(bar, startDate, endDate)) {
                        bars.add(bar);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse line {}: {} - Error: {}", lineNumber, line, e.getMessage());
                }
            }
        }

        // Sort by timestamp
        bars.sort(Comparator.comparing(BarData::getTimestamp));

        log.info("Loaded {} bars from {}", bars.size(), filePath.getFileName());
        return bars;
    }

    private List<BarData> loadSymbolData(String symbol, String timeframe,
                                          LocalDate startDate, LocalDate endDate) throws IOException {
        // Try different file naming conventions
        List<String> fileNamePatterns = Arrays.asList(
                symbol + "_" + timeframe + ".csv",
                symbol + ".csv",
                symbol.toLowerCase() + "_" + timeframe + ".csv",
                symbol.toLowerCase() + ".csv"
        );

        for (String fileName : fileNamePatterns) {
            Path filePath = dataDirectory.resolve(fileName);
            if (Files.exists(filePath)) {
                return loadFromFile(filePath, symbol, timeframe, startDate, endDate);
            }
        }

        throw new IOException("CSV file not found for symbol: " + symbol +
                " (tried patterns: " + fileNamePatterns + ")");
    }

    private BarData parseLine(String line, String symbol, String timeframe,
                              DateTimeFormatter dateFormatter) {
        String[] columns = line.split(format.getDelimiter());

        if (columns.length <= Math.max(
                Math.max(format.getTimestampColumn(), format.getCloseColumn()),
                Math.max(format.getOpenColumn(), format.getVolumeColumn()))) {
            return null; // Not enough columns
        }

        try {
            LocalDateTime timestamp = parseTimestamp(
                    columns[format.getTimestampColumn()].trim(), dateFormatter);
            BigDecimal open = parseBigDecimal(columns[format.getOpenColumn()].trim());
            BigDecimal high = parseBigDecimal(columns[format.getHighColumn()].trim());
            BigDecimal low = parseBigDecimal(columns[format.getLowColumn()].trim());
            BigDecimal close = parseBigDecimal(columns[format.getCloseColumn()].trim());
            Long volume = parseVolume(columns[format.getVolumeColumn()].trim());

            return BarData.builder()
                    .symbol(symbol)
                    .timeframe(timeframe)
                    .timestamp(timestamp)
                    .open(open)
                    .high(high)
                    .low(low)
                    .close(close)
                    .volume(volume)
                    .build();

        } catch (Exception e) {
            log.debug("Failed to parse line: {} - {}", line, e.getMessage());
            return null;
        }
    }

    private LocalDateTime parseTimestamp(String value, DateTimeFormatter formatter) {
        try {
            // Try parsing as LocalDateTime first
            return LocalDateTime.parse(value, formatter);
        } catch (DateTimeParseException e) {
            // Try parsing as LocalDate and convert to start of day
            try {
                LocalDate date = LocalDate.parse(value, formatter);
                return date.atStartOfDay();
            } catch (DateTimeParseException e2) {
                // Try common date formats
                return tryCommonDateFormats(value);
            }
        }
    }

    private LocalDateTime tryCommonDateFormats(String value) {
        List<DateTimeFormatter> formatters = Arrays.asList(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ISO_LOCAL_DATE
        );

        for (DateTimeFormatter fmt : formatters) {
            try {
                return LocalDateTime.parse(value, fmt);
            } catch (DateTimeParseException e) {
                try {
                    return LocalDate.parse(value, fmt).atStartOfDay();
                } catch (DateTimeParseException e2) {
                    // Continue trying
                }
            }
        }

        throw new IllegalArgumentException("Cannot parse timestamp: " + value);
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty() || value.equals("-")) {
            return BigDecimal.ZERO;
        }
        // Remove commas and currency symbols
        String cleaned = value.replaceAll("[,$%]", "").trim();
        return new BigDecimal(cleaned);
    }

    private Long parseVolume(String value) {
        if (value == null || value.isEmpty() || value.equals("-")) {
            return 0L;
        }

        String cleaned = value.replaceAll("[,]", "").trim();

        // Handle K, M, B suffixes (e.g., "1.5M" = 1,500,000)
        if (cleaned.endsWith("K") || cleaned.endsWith("k")) {
            return (long) (Double.parseDouble(cleaned.substring(0, cleaned.length() - 1)) * 1_000);
        }
        if (cleaned.endsWith("M") || cleaned.endsWith("m")) {
            return (long) (Double.parseDouble(cleaned.substring(0, cleaned.length() - 1)) * 1_000_000);
        }
        if (cleaned.endsWith("B") || cleaned.endsWith("b")) {
            return (long) (Double.parseDouble(cleaned.substring(0, cleaned.length() - 1)) * 1_000_000_000);
        }

        return Long.parseLong(cleaned);
    }

    private DateTimeFormatter createDateFormatter() {
        return DateTimeFormatter.ofPattern(format.getDateFormat());
    }

    private boolean isInDateRange(BarData bar, LocalDate startDate, LocalDate endDate) {
        if (bar.getTimestamp() == null) {
            return false;
        }

        LocalDate barDate = bar.getTimestamp().toLocalDate();

        if (startDate != null && barDate.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && barDate.isAfter(endDate)) {
            return false;
        }

        return true;
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
            log.info("CSV data source reset");
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
        return DataSourceType.CSV;
    }

    @Override
    public void close() {
        allBars = null;
        iterator = null;
        currentIndex = -1;
    }

    @Override
    public List<BarData> getAllBars() {
        return new ArrayList<>(allBars);
    }
}
