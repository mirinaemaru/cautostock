package maru.trading.domain.backtest;

import maru.trading.domain.order.Side;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BacktestTrade Test")
class BacktestTradeTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create trade with builder")
        void shouldCreateTradeWithBuilder() {
            LocalDateTime entryTime = LocalDateTime.now().minusHours(1);
            LocalDateTime exitTime = LocalDateTime.now();

            BacktestTrade trade = BacktestTrade.builder()
                    .tradeId("TRADE001")
                    .backtestId("BT001")
                    .symbol("005930")
                    .entryTime(entryTime)
                    .entryPrice(BigDecimal.valueOf(70000))
                    .entryQty(BigDecimal.valueOf(10))
                    .side(Side.BUY)
                    .exitTime(exitTime)
                    .exitPrice(BigDecimal.valueOf(72000))
                    .exitQty(BigDecimal.valueOf(10))
                    .grossPnl(BigDecimal.valueOf(20000))
                    .commissionPaid(BigDecimal.valueOf(100))
                    .slippageCost(BigDecimal.valueOf(50))
                    .netPnl(BigDecimal.valueOf(19850))
                    .returnPct(BigDecimal.valueOf(2.84))
                    .signalReason("MA crossover")
                    .status("CLOSED")
                    .build();

            assertThat(trade.getTradeId()).isEqualTo("TRADE001");
            assertThat(trade.getBacktestId()).isEqualTo("BT001");
            assertThat(trade.getSymbol()).isEqualTo("005930");
            assertThat(trade.getSide()).isEqualTo(Side.BUY);
            assertThat(trade.getNetPnl()).isEqualTo(BigDecimal.valueOf(19850));
            assertThat(trade.getStatus()).isEqualTo("CLOSED");
        }
    }

    @Nested
    @DisplayName("isWinner Tests")
    class IsWinnerTests {

        @Test
        @DisplayName("Should return true when netPnl is positive")
        void shouldReturnTrueWhenNetPnlIsPositive() {
            BacktestTrade trade = BacktestTrade.builder()
                    .netPnl(BigDecimal.valueOf(1000))
                    .build();

            assertThat(trade.isWinner()).isTrue();
        }

        @Test
        @DisplayName("Should return false when netPnl is zero")
        void shouldReturnFalseWhenNetPnlIsZero() {
            BacktestTrade trade = BacktestTrade.builder()
                    .netPnl(BigDecimal.ZERO)
                    .build();

            assertThat(trade.isWinner()).isFalse();
        }

        @Test
        @DisplayName("Should return false when netPnl is negative")
        void shouldReturnFalseWhenNetPnlIsNegative() {
            BacktestTrade trade = BacktestTrade.builder()
                    .netPnl(BigDecimal.valueOf(-500))
                    .build();

            assertThat(trade.isWinner()).isFalse();
        }

        @Test
        @DisplayName("Should return false when netPnl is null")
        void shouldReturnFalseWhenNetPnlIsNull() {
            BacktestTrade trade = BacktestTrade.builder()
                    .build();

            assertThat(trade.isWinner()).isFalse();
        }
    }

    @Nested
    @DisplayName("isLoser Tests")
    class IsLoserTests {

        @Test
        @DisplayName("Should return true when netPnl is negative")
        void shouldReturnTrueWhenNetPnlIsNegative() {
            BacktestTrade trade = BacktestTrade.builder()
                    .netPnl(BigDecimal.valueOf(-500))
                    .build();

            assertThat(trade.isLoser()).isTrue();
        }

        @Test
        @DisplayName("Should return false when netPnl is zero")
        void shouldReturnFalseWhenNetPnlIsZero() {
            BacktestTrade trade = BacktestTrade.builder()
                    .netPnl(BigDecimal.ZERO)
                    .build();

            assertThat(trade.isLoser()).isFalse();
        }

        @Test
        @DisplayName("Should return false when netPnl is positive")
        void shouldReturnFalseWhenNetPnlIsPositive() {
            BacktestTrade trade = BacktestTrade.builder()
                    .netPnl(BigDecimal.valueOf(1000))
                    .build();

            assertThat(trade.isLoser()).isFalse();
        }

        @Test
        @DisplayName("Should return false when netPnl is null")
        void shouldReturnFalseWhenNetPnlIsNull() {
            BacktestTrade trade = BacktestTrade.builder()
                    .build();

            assertThat(trade.isLoser()).isFalse();
        }
    }
}
