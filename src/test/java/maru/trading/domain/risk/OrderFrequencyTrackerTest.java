package maru.trading.domain.risk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderFrequencyTracker Test")
class OrderFrequencyTrackerTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create empty tracker with default constructor")
        void shouldCreateEmptyTracker() {
            OrderFrequencyTracker tracker = new OrderFrequencyTracker();

            assertThat(tracker.getCount()).isEqualTo(0);
            assertThat(tracker.getTimestamps()).isEmpty();
        }

        @Test
        @DisplayName("Should create tracker with existing timestamps")
        void shouldCreateTrackerWithExistingTimestamps() {
            List<LocalDateTime> timestamps = Arrays.asList(
                    LocalDateTime.now().minusSeconds(30),
                    LocalDateTime.now().minusSeconds(20)
            );

            OrderFrequencyTracker tracker = new OrderFrequencyTracker(timestamps);

            assertThat(tracker.getCount()).isEqualTo(2);
            assertThat(tracker.getTimestamps()).hasSize(2);
        }

        @Test
        @DisplayName("Should handle null timestamps in constructor")
        void shouldHandleNullTimestamps() {
            OrderFrequencyTracker tracker = new OrderFrequencyTracker(null);

            assertThat(tracker.getCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("addOrder Tests")
    class AddOrderTests {

        @Test
        @DisplayName("Should add order and return new tracker")
        void shouldAddOrderAndReturnNewTracker() {
            OrderFrequencyTracker original = new OrderFrequencyTracker();
            LocalDateTime timestamp = LocalDateTime.now();

            OrderFrequencyTracker updated = original.addOrder(timestamp);

            assertThat(original.getCount()).isEqualTo(0); // Original unchanged
            assertThat(updated.getCount()).isEqualTo(1);
            assertThat(updated.getTimestamps()).contains(timestamp);
        }

        @Test
        @DisplayName("Should accumulate multiple orders")
        void shouldAccumulateMultipleOrders() {
            OrderFrequencyTracker tracker = new OrderFrequencyTracker();

            tracker = tracker.addOrder(LocalDateTime.now().minusSeconds(30));
            tracker = tracker.addOrder(LocalDateTime.now().minusSeconds(20));
            tracker = tracker.addOrder(LocalDateTime.now().minusSeconds(10));

            assertThat(tracker.getCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("cleanup Tests")
    class CleanupTests {

        @Test
        @DisplayName("Should remove old timestamps before cutoff")
        void shouldRemoveOldTimestamps() {
            LocalDateTime now = LocalDateTime.now();
            OrderFrequencyTracker tracker = new OrderFrequencyTracker(Arrays.asList(
                    now.minusMinutes(5),  // Old - should be removed
                    now.minusMinutes(2),  // Old - should be removed
                    now.minusSeconds(30)  // Recent - should remain
            ));

            OrderFrequencyTracker cleaned = tracker.cleanup(now.minusMinutes(1));

            assertThat(cleaned.getCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should keep all timestamps if all are recent")
        void shouldKeepAllRecentTimestamps() {
            LocalDateTime now = LocalDateTime.now();
            OrderFrequencyTracker tracker = new OrderFrequencyTracker(Arrays.asList(
                    now.minusSeconds(30),
                    now.minusSeconds(20),
                    now.minusSeconds(10)
            ));

            OrderFrequencyTracker cleaned = tracker.cleanup(now.minusMinutes(1));

            assertThat(cleaned.getCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("countOrdersInWindow Tests")
    class CountOrdersInWindowTests {

        @Test
        @DisplayName("Should count orders in time window")
        void shouldCountOrdersInWindow() {
            LocalDateTime now = LocalDateTime.now();
            OrderFrequencyTracker tracker = new OrderFrequencyTracker(Arrays.asList(
                    now.minusMinutes(2),  // Outside window
                    now.minusSeconds(45), // Inside window
                    now.minusSeconds(30), // Inside window
                    now.minusSeconds(15)  // Inside window
            ));

            long count = tracker.countOrdersInWindow(now.minusMinutes(1), now);

            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return zero for empty window")
        void shouldReturnZeroForEmptyWindow() {
            LocalDateTime now = LocalDateTime.now();
            OrderFrequencyTracker tracker = new OrderFrequencyTracker(Arrays.asList(
                    now.minusHours(1)
            ));

            long count = tracker.countOrdersInWindow(now.minusMinutes(1), now);

            assertThat(count).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("wouldExceedLimit Tests")
    class WouldExceedLimitTests {

        @Test
        @DisplayName("Should return false when under limit")
        void shouldReturnFalseWhenUnderLimit() {
            LocalDateTime now = LocalDateTime.now();
            OrderFrequencyTracker tracker = new OrderFrequencyTracker(Arrays.asList(
                    now.minusSeconds(30),
                    now.minusSeconds(20)
            ));

            boolean wouldExceed = tracker.wouldExceedLimit(now, 60, 5);

            assertThat(wouldExceed).isFalse();
        }

        @Test
        @DisplayName("Should return true when at limit")
        void shouldReturnTrueWhenAtLimit() {
            LocalDateTime now = LocalDateTime.now();
            OrderFrequencyTracker tracker = new OrderFrequencyTracker(Arrays.asList(
                    now.minusSeconds(50),
                    now.minusSeconds(40),
                    now.minusSeconds(30),
                    now.minusSeconds(20),
                    now.minusSeconds(10)
            ));

            boolean wouldExceed = tracker.wouldExceedLimit(now, 60, 5);

            assertThat(wouldExceed).isTrue();
        }

        @Test
        @DisplayName("Should return true when over limit")
        void shouldReturnTrueWhenOverLimit() {
            LocalDateTime now = LocalDateTime.now();
            OrderFrequencyTracker tracker = new OrderFrequencyTracker(Arrays.asList(
                    now.minusSeconds(50),
                    now.minusSeconds(40),
                    now.minusSeconds(30),
                    now.minusSeconds(20),
                    now.minusSeconds(10),
                    now.minusSeconds(5)
            ));

            boolean wouldExceed = tracker.wouldExceedLimit(now, 60, 5);

            assertThat(wouldExceed).isTrue();
        }
    }

    @Nested
    @DisplayName("getTimestamps Tests")
    class GetTimestampsTests {

        @Test
        @DisplayName("Should return immutable copy of timestamps")
        void shouldReturnImmutableCopy() {
            LocalDateTime ts1 = LocalDateTime.now();
            OrderFrequencyTracker tracker = new OrderFrequencyTracker(Arrays.asList(ts1));

            List<LocalDateTime> timestamps = tracker.getTimestamps();

            assertThat(timestamps).hasSize(1);
            assertThat(tracker.getCount()).isEqualTo(1); // Original unchanged
        }
    }
}
