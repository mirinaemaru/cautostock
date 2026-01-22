package maru.trading.domain.signal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Signal Domain Test")
class SignalTest {

    @Test
    @DisplayName("Should create BUY signal with builder")
    void shouldCreateBuySignalWithBuilder() {
        Signal signal = Signal.builder()
                .signalId("SIG-001")
                .strategyId("STR-001")
                .strategyVersionId("SV-001")
                .accountId("ACC-001")
                .symbol("005930")
                .signalType(SignalType.BUY)
                .targetType("QTY")
                .targetValue(BigDecimal.valueOf(10))
                .ttlSeconds(300)
                .reason("MA crossover detected")
                .build();

        assertThat(signal.getSignalId()).isEqualTo("SIG-001");
        assertThat(signal.getStrategyId()).isEqualTo("STR-001");
        assertThat(signal.getStrategyVersionId()).isEqualTo("SV-001");
        assertThat(signal.getAccountId()).isEqualTo("ACC-001");
        assertThat(signal.getSymbol()).isEqualTo("005930");
        assertThat(signal.getSignalType()).isEqualTo(SignalType.BUY);
        assertThat(signal.getTargetType()).isEqualTo("QTY");
        assertThat(signal.getTargetValue()).isEqualTo(BigDecimal.valueOf(10));
        assertThat(signal.getTtlSeconds()).isEqualTo(300);
        assertThat(signal.getReason()).isEqualTo("MA crossover detected");
    }

    @Test
    @DisplayName("Should create SELL signal with builder")
    void shouldCreateSellSignalWithBuilder() {
        Signal signal = Signal.builder()
                .signalId("SIG-002")
                .signalType(SignalType.SELL)
                .targetType("QTY")
                .targetValue(BigDecimal.valueOf(5))
                .reason("RSI overbought")
                .build();

        assertThat(signal.getSignalType()).isEqualTo(SignalType.SELL);
        assertThat(signal.getTargetType()).isEqualTo("QTY");
        assertThat(signal.getTargetValue()).isEqualTo(BigDecimal.valueOf(5));
    }

    @Test
    @DisplayName("Should create HOLD signal with builder")
    void shouldCreateHoldSignalWithBuilder() {
        Signal signal = Signal.builder()
                .signalId("SIG-003")
                .signalType(SignalType.HOLD)
                .reason("No clear signal")
                .build();

        assertThat(signal.getSignalType()).isEqualTo(SignalType.HOLD);
        assertThat(signal.getTargetType()).isNull();
        assertThat(signal.getTargetValue()).isNull();
    }

    @Test
    @DisplayName("Should create signal with WEIGHT target type")
    void shouldCreateSignalWithWeightTargetType() {
        Signal signal = Signal.builder()
                .signalType(SignalType.BUY)
                .targetType("WEIGHT")
                .targetValue(BigDecimal.valueOf(0.1)) // 10% of portfolio
                .build();

        assertThat(signal.getTargetType()).isEqualTo("WEIGHT");
        assertThat(signal.getTargetValue()).isEqualTo(BigDecimal.valueOf(0.1));
    }
}
