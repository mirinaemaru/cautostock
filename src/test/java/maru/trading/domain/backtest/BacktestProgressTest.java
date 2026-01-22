package maru.trading.domain.backtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BacktestProgress Test")
class BacktestProgressTest {

    @Nested
    @DisplayName("Status Enum Tests")
    class StatusEnumTests {

        @Test
        @DisplayName("Should have all expected statuses")
        void shouldHaveAllExpectedStatuses() {
            assertThat(BacktestProgress.Status.values()).hasSize(5);
            assertThat(BacktestProgress.Status.QUEUED).isNotNull();
            assertThat(BacktestProgress.Status.RUNNING).isNotNull();
            assertThat(BacktestProgress.Status.COMPLETED).isNotNull();
            assertThat(BacktestProgress.Status.FAILED).isNotNull();
            assertThat(BacktestProgress.Status.CANCELLED).isNotNull();
        }
    }

    @Nested
    @DisplayName("isRunning Tests")
    class IsRunningTests {

        @Test
        @DisplayName("Should return true for QUEUED status")
        void shouldReturnTrueForQueuedStatus() {
            BacktestProgress progress = BacktestProgress.builder()
                    .status(BacktestProgress.Status.QUEUED)
                    .build();

            assertThat(progress.isRunning()).isTrue();
        }

        @Test
        @DisplayName("Should return true for RUNNING status")
        void shouldReturnTrueForRunningStatus() {
            BacktestProgress progress = BacktestProgress.builder()
                    .status(BacktestProgress.Status.RUNNING)
                    .build();

            assertThat(progress.isRunning()).isTrue();
        }

        @Test
        @DisplayName("Should return false for COMPLETED status")
        void shouldReturnFalseForCompletedStatus() {
            BacktestProgress progress = BacktestProgress.builder()
                    .status(BacktestProgress.Status.COMPLETED)
                    .build();

            assertThat(progress.isRunning()).isFalse();
        }

        @Test
        @DisplayName("Should return false for FAILED status")
        void shouldReturnFalseForFailedStatus() {
            BacktestProgress progress = BacktestProgress.builder()
                    .status(BacktestProgress.Status.FAILED)
                    .build();

            assertThat(progress.isRunning()).isFalse();
        }
    }

    @Nested
    @DisplayName("isDone Tests")
    class IsDoneTests {

        @Test
        @DisplayName("Should return true for COMPLETED status")
        void shouldReturnTrueForCompletedStatus() {
            BacktestProgress progress = BacktestProgress.builder()
                    .status(BacktestProgress.Status.COMPLETED)
                    .build();

            assertThat(progress.isDone()).isTrue();
        }

        @Test
        @DisplayName("Should return true for FAILED status")
        void shouldReturnTrueForFailedStatus() {
            BacktestProgress progress = BacktestProgress.builder()
                    .status(BacktestProgress.Status.FAILED)
                    .build();

            assertThat(progress.isDone()).isTrue();
        }

        @Test
        @DisplayName("Should return true for CANCELLED status")
        void shouldReturnTrueForCancelledStatus() {
            BacktestProgress progress = BacktestProgress.builder()
                    .status(BacktestProgress.Status.CANCELLED)
                    .build();

            assertThat(progress.isDone()).isTrue();
        }

        @Test
        @DisplayName("Should return false for RUNNING status")
        void shouldReturnFalseForRunningStatus() {
            BacktestProgress progress = BacktestProgress.builder()
                    .status(BacktestProgress.Status.RUNNING)
                    .build();

            assertThat(progress.isDone()).isFalse();
        }
    }

    @Nested
    @DisplayName("isSuccess Tests")
    class IsSuccessTests {

        @Test
        @DisplayName("Should return true only for COMPLETED status")
        void shouldReturnTrueOnlyForCompletedStatus() {
            BacktestProgress completed = BacktestProgress.builder()
                    .status(BacktestProgress.Status.COMPLETED)
                    .build();

            BacktestProgress failed = BacktestProgress.builder()
                    .status(BacktestProgress.Status.FAILED)
                    .build();

            assertThat(completed.isSuccess()).isTrue();
            assertThat(failed.isSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create initial progress")
        void shouldCreateInitialProgress() {
            BacktestProgress progress = BacktestProgress.initial("JOB001");

            assertThat(progress.getJobId()).isEqualTo("JOB001");
            assertThat(progress.getStatus()).isEqualTo(BacktestProgress.Status.QUEUED);
            assertThat(progress.getProgressPercent()).isEqualTo(0);
            assertThat(progress.getCurrentPhase()).isEqualTo("Queued");
            assertThat(progress.getQueuedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should create running progress")
        void shouldCreateRunningProgress() {
            BacktestProgress progress = BacktestProgress.running(
                    "JOB001", 50, "Processing bars", 1000, 500
            );

            assertThat(progress.getJobId()).isEqualTo("JOB001");
            assertThat(progress.getStatus()).isEqualTo(BacktestProgress.Status.RUNNING);
            assertThat(progress.getProgressPercent()).isEqualTo(50);
            assertThat(progress.getCurrentPhase()).isEqualTo("Processing bars");
            assertThat(progress.getTotalBars()).isEqualTo(1000);
            assertThat(progress.getProcessedBars()).isEqualTo(500);
            assertThat(progress.getStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should create completed progress")
        void shouldCreateCompletedProgress() {
            BacktestProgress progress = BacktestProgress.completed("JOB001");

            assertThat(progress.getJobId()).isEqualTo("JOB001");
            assertThat(progress.getStatus()).isEqualTo(BacktestProgress.Status.COMPLETED);
            assertThat(progress.getProgressPercent()).isEqualTo(100);
            assertThat(progress.getCurrentPhase()).isEqualTo("Completed");
            assertThat(progress.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should create failed progress")
        void shouldCreateFailedProgress() {
            BacktestProgress progress = BacktestProgress.failed("JOB001", "Data not found");

            assertThat(progress.getJobId()).isEqualTo("JOB001");
            assertThat(progress.getStatus()).isEqualTo(BacktestProgress.Status.FAILED);
            assertThat(progress.getCurrentPhase()).isEqualTo("Failed");
            assertThat(progress.getErrorMessage()).isEqualTo("Data not found");
            assertThat(progress.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should create cancelled progress")
        void shouldCreateCancelledProgress() {
            BacktestProgress progress = BacktestProgress.cancelled("JOB001");

            assertThat(progress.getJobId()).isEqualTo("JOB001");
            assertThat(progress.getStatus()).isEqualTo(BacktestProgress.Status.CANCELLED);
            assertThat(progress.getCurrentPhase()).isEqualTo("Cancelled");
            assertThat(progress.getCompletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create progress with all fields")
        void shouldCreateProgressWithAllFields() {
            LocalDateTime queuedAt = LocalDateTime.now().minusMinutes(10);
            LocalDateTime startedAt = LocalDateTime.now().minusMinutes(5);

            BacktestProgress progress = BacktestProgress.builder()
                    .jobId("JOB001")
                    .status(BacktestProgress.Status.RUNNING)
                    .progressPercent(75)
                    .currentPhase("Analyzing")
                    .totalBars(2000)
                    .processedBars(1500)
                    .currentSymbol("005930")
                    .queuedAt(queuedAt)
                    .startedAt(startedAt)
                    .estimatedRemainingSeconds(60L)
                    .build();

            assertThat(progress.getJobId()).isEqualTo("JOB001");
            assertThat(progress.getProgressPercent()).isEqualTo(75);
            assertThat(progress.getCurrentSymbol()).isEqualTo("005930");
            assertThat(progress.getEstimatedRemainingSeconds()).isEqualTo(60L);
        }
    }
}
