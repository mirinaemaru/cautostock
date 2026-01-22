package maru.trading.domain.backtest.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CsvFormat Test")
class CsvFormatTest {

    @Nested
    @DisplayName("PresetFormat Enum Tests")
    class PresetFormatEnumTests {

        @Test
        @DisplayName("Should have all preset formats")
        void shouldHaveAllPresetFormats() {
            assertThat(CsvFormat.PresetFormat.values()).hasSize(4);
            assertThat(CsvFormat.PresetFormat.STANDARD).isNotNull();
            assertThat(CsvFormat.PresetFormat.YAHOO).isNotNull();
            assertThat(CsvFormat.PresetFormat.INVESTING).isNotNull();
            assertThat(CsvFormat.PresetFormat.CUSTOM).isNotNull();
        }
    }

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should have correct default values")
        void shouldHaveCorrectDefaultValues() {
            CsvFormat format = CsvFormat.builder().build();

            assertThat(format.getPreset()).isEqualTo(CsvFormat.PresetFormat.STANDARD);
            assertThat(format.isHasHeader()).isTrue();
            assertThat(format.getDelimiter()).isEqualTo(",");
            assertThat(format.getDateFormat()).isEqualTo("yyyy-MM-dd HH:mm:ss");
            assertThat(format.getTimestampColumn()).isEqualTo(0);
            assertThat(format.getOpenColumn()).isEqualTo(1);
            assertThat(format.getHighColumn()).isEqualTo(2);
            assertThat(format.getLowColumn()).isEqualTo(3);
            assertThat(format.getCloseColumn()).isEqualTo(4);
            assertThat(format.getVolumeColumn()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create standard format")
        void shouldCreateStandardFormat() {
            CsvFormat format = CsvFormat.standard();

            assertThat(format.getPreset()).isEqualTo(CsvFormat.PresetFormat.STANDARD);
        }

        @Test
        @DisplayName("Should create Yahoo format")
        void shouldCreateYahooFormat() {
            CsvFormat format = CsvFormat.yahoo();

            assertThat(format.getPreset()).isEqualTo(CsvFormat.PresetFormat.YAHOO);
            assertThat(format.getDateFormat()).isEqualTo("yyyy-MM-dd");
            assertThat(format.getTimestampColumn()).isEqualTo(0);
            assertThat(format.getOpenColumn()).isEqualTo(1);
            assertThat(format.getHighColumn()).isEqualTo(2);
            assertThat(format.getLowColumn()).isEqualTo(3);
            assertThat(format.getCloseColumn()).isEqualTo(4);
            assertThat(format.getVolumeColumn()).isEqualTo(6); // Adj Close is column 5
        }

        @Test
        @DisplayName("Should create Investing format")
        void shouldCreateInvestingFormat() {
            CsvFormat format = CsvFormat.investing();

            assertThat(format.getPreset()).isEqualTo(CsvFormat.PresetFormat.INVESTING);
            assertThat(format.getDateFormat()).isEqualTo("MMM dd, yyyy");
            assertThat(format.getTimestampColumn()).isEqualTo(0);
            assertThat(format.getOpenColumn()).isEqualTo(2);
            assertThat(format.getHighColumn()).isEqualTo(3);
            assertThat(format.getLowColumn()).isEqualTo(4);
            assertThat(format.getCloseColumn()).isEqualTo(1); // Price is close
            assertThat(format.getVolumeColumn()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should create custom format")
        void shouldCreateCustomFormat() {
            CsvFormat format = CsvFormat.custom(
                    ";", "dd/MM/yyyy",
                    0, 3, 4, 5, 1, 6
            );

            assertThat(format.getPreset()).isEqualTo(CsvFormat.PresetFormat.CUSTOM);
            assertThat(format.getDelimiter()).isEqualTo(";");
            assertThat(format.getDateFormat()).isEqualTo("dd/MM/yyyy");
            assertThat(format.getTimestampColumn()).isEqualTo(0);
            assertThat(format.getOpenColumn()).isEqualTo(3);
            assertThat(format.getHighColumn()).isEqualTo(4);
            assertThat(format.getLowColumn()).isEqualTo(5);
            assertThat(format.getCloseColumn()).isEqualTo(1);
            assertThat(format.getVolumeColumn()).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should override default values")
        void shouldOverrideDefaultValues() {
            CsvFormat format = CsvFormat.builder()
                    .preset(CsvFormat.PresetFormat.CUSTOM)
                    .hasHeader(false)
                    .delimiter("\t")
                    .dateFormat("yyyyMMdd")
                    .timestampColumn(5)
                    .openColumn(0)
                    .highColumn(1)
                    .lowColumn(2)
                    .closeColumn(3)
                    .volumeColumn(4)
                    .build();

            assertThat(format.isHasHeader()).isFalse();
            assertThat(format.getDelimiter()).isEqualTo("\t");
            assertThat(format.getDateFormat()).isEqualTo("yyyyMMdd");
            assertThat(format.getTimestampColumn()).isEqualTo(5);
        }
    }
}
