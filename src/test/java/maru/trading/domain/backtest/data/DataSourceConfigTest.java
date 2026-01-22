package maru.trading.domain.backtest.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DataSourceConfig Test")
class DataSourceConfigTest {

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should have DATABASE as default type")
        void shouldHaveDatabaseAsDefaultType() {
            DataSourceConfig config = DataSourceConfig.builder().build();

            assertThat(config.getType()).isEqualTo(DataSourceType.DATABASE);
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create database config")
        void shouldCreateDatabaseConfig() {
            DataSourceConfig config = DataSourceConfig.database();

            assertThat(config.getType()).isEqualTo(DataSourceType.DATABASE);
            assertThat(config.getCsvPath()).isNull();
            assertThat(config.getCsvFormat()).isNull();
        }

        @Test
        @DisplayName("Should create CSV config with standard format")
        void shouldCreateCsvConfigWithStandardFormat() {
            DataSourceConfig config = DataSourceConfig.csv("/data/prices.csv");

            assertThat(config.getType()).isEqualTo(DataSourceType.CSV);
            assertThat(config.getCsvPath()).isEqualTo("/data/prices.csv");
            assertThat(config.getCsvFormat()).isNotNull();
            assertThat(config.getCsvFormat().getPreset())
                    .isEqualTo(CsvFormat.PresetFormat.STANDARD);
        }

        @Test
        @DisplayName("Should create CSV config with custom format")
        void shouldCreateCsvConfigWithCustomFormat() {
            CsvFormat customFormat = CsvFormat.custom(
                    ";", "dd/MM/yyyy",
                    0, 1, 2, 3, 4, 5
            );

            DataSourceConfig config = DataSourceConfig.csv("/data/prices.csv", customFormat);

            assertThat(config.getType()).isEqualTo(DataSourceType.CSV);
            assertThat(config.getCsvPath()).isEqualTo("/data/prices.csv");
            assertThat(config.getCsvFormat().getDelimiter()).isEqualTo(";");
        }

        @Test
        @DisplayName("Should create Yahoo CSV config")
        void shouldCreateYahooCsvConfig() {
            DataSourceConfig config = DataSourceConfig.yahooCsv("/data/yahoo.csv");

            assertThat(config.getType()).isEqualTo(DataSourceType.CSV);
            assertThat(config.getCsvPath()).isEqualTo("/data/yahoo.csv");
            assertThat(config.getCsvFormat().getPreset())
                    .isEqualTo(CsvFormat.PresetFormat.YAHOO);
        }

        @Test
        @DisplayName("Should create Investing CSV config")
        void shouldCreateInvestingCsvConfig() {
            DataSourceConfig config = DataSourceConfig.investingCsv("/data/investing.csv");

            assertThat(config.getType()).isEqualTo(DataSourceType.CSV);
            assertThat(config.getCsvPath()).isEqualTo("/data/investing.csv");
            assertThat(config.getCsvFormat().getPreset())
                    .isEqualTo(CsvFormat.PresetFormat.INVESTING);
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create config with builder")
        void shouldCreateConfigWithBuilder() {
            DataSourceConfig config = DataSourceConfig.builder()
                    .type(DataSourceType.REALTIME)
                    .build();

            assertThat(config.getType()).isEqualTo(DataSourceType.REALTIME);
        }

        @Test
        @DisplayName("Should set all CSV properties")
        void shouldSetAllCsvProperties() {
            CsvFormat format = CsvFormat.yahoo();

            DataSourceConfig config = DataSourceConfig.builder()
                    .type(DataSourceType.CSV)
                    .csvPath("/path/to/data.csv")
                    .csvFormat(format)
                    .build();

            assertThat(config.getType()).isEqualTo(DataSourceType.CSV);
            assertThat(config.getCsvPath()).isEqualTo("/path/to/data.csv");
            assertThat(config.getCsvFormat()).isEqualTo(format);
        }
    }
}
