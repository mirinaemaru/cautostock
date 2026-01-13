package maru.trading.broker.kis.marketdata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataQualityMonitor 테스트
 *
 * 테스트 범위:
 * 1. Valid tick 기록
 * 2. Invalid tick 기록
 * 3. Duplicate tick 기록
 * 4. Out-of-sequence tick 기록
 * 5. Error 기록
 * 6. 품질 점수 계산
 * 7. 메트릭 조회 및 리셋
 */
@DisplayName("DataQualityMonitor 테스트")
class DataQualityMonitorTest {

    private DataQualityMonitor monitor;

    @BeforeEach
    void setUp() {
        monitor = new DataQualityMonitor();
    }

    // ==================== 1. Valid Tick 기록 ====================

    @Test
    @DisplayName("recordValidTick - 유효한 틱 카운트 증가")
    void testRecordValidTick() {
        // When
        monitor.recordValidTick("005930");
        monitor.recordValidTick("005930");
        monitor.recordValidTick("005930");

        // Then
        DataQualityMonitor.SymbolQualityMetrics metrics = monitor.getMetrics("005930");
        assertThat(metrics.getValidTickCount().get()).isEqualTo(3);
        assertThat(metrics.getLastTickTimestamp()).isNotNull();
    }

    // ==================== 2. Invalid Tick 기록 ====================

    @Test
    @DisplayName("recordInvalidTick - 무효한 틱 카운트 증가")
    void testRecordInvalidTick() {
        // When
        monitor.recordInvalidTick("005930", "Price too low");
        monitor.recordInvalidTick("005930", "Null timestamp");

        // Then
        DataQualityMonitor.SymbolQualityMetrics metrics = monitor.getMetrics("005930");
        assertThat(metrics.getInvalidTickCount().get()).isEqualTo(2);
        assertThat(metrics.getLastError()).isEqualTo("Null timestamp");
        assertThat(metrics.getLastErrorTimestamp()).isNotNull();
    }

    // ==================== 3. Duplicate Tick 기록 ====================

    @Test
    @DisplayName("recordDuplicateTick - 중복 틱 카운트 증가")
    void testRecordDuplicateTick() {
        // When
        monitor.recordDuplicateTick("005930");
        monitor.recordDuplicateTick("005930");
        monitor.recordDuplicateTick("005930");

        // Then
        DataQualityMonitor.SymbolQualityMetrics metrics = monitor.getMetrics("005930");
        assertThat(metrics.getDuplicateTickCount().get()).isEqualTo(3);
    }

    // ==================== 4. Out-of-Sequence Tick 기록 ====================

    @Test
    @DisplayName("recordOutOfSequenceTick - 순서 오류 틱 카운트 증가")
    void testRecordOutOfSequenceTick() {
        // When
        monitor.recordOutOfSequenceTick("005930");
        monitor.recordOutOfSequenceTick("005930");

        // Then
        DataQualityMonitor.SymbolQualityMetrics metrics = monitor.getMetrics("005930");
        assertThat(metrics.getOutOfSequenceTickCount().get()).isEqualTo(2);
    }

    // ==================== 5. Error 기록 ====================

    @Test
    @DisplayName("recordError - 에러 카운트 증가")
    void testRecordError() {
        // When
        monitor.recordError("005930", "Network timeout");
        monitor.recordError("005930", "Connection lost");

        // Then
        DataQualityMonitor.SymbolQualityMetrics metrics = monitor.getMetrics("005930");
        assertThat(metrics.getErrorCount().get()).isEqualTo(2);
        assertThat(metrics.getLastError()).isEqualTo("Connection lost");
    }

    // ==================== 6. 품질 점수 계산 ====================

    @Test
    @DisplayName("getQualityScore - 100% 유효한 틱")
    void testGetQualityScore_Perfect() {
        // Given
        monitor.recordValidTick("005930");
        monitor.recordValidTick("005930");
        monitor.recordValidTick("005930");

        // When
        DataQualityMonitor.SymbolQualityMetrics metrics = monitor.getMetrics("005930");
        double score = metrics.getQualityScore();

        // Then
        assertThat(score).isEqualTo(100.0);
    }

    @Test
    @DisplayName("getQualityScore - 50% 유효한 틱")
    void testGetQualityScore_Half() {
        // Given
        monitor.recordValidTick("005930");
        monitor.recordValidTick("005930");
        monitor.recordInvalidTick("005930", "Error 1");
        monitor.recordDuplicateTick("005930");

        // When
        DataQualityMonitor.SymbolQualityMetrics metrics = monitor.getMetrics("005930");
        double score = metrics.getQualityScore();

        // Then
        // Total = 2 valid + 1 invalid + 1 duplicate = 4
        // Valid ratio = 2/4 = 0.5 = 50%
        assertThat(score).isEqualTo(50.0);
    }

    @Test
    @DisplayName("getQualityScore - 데이터 없으면 getMetrics는 null 반환")
    void testGetQualityScore_NoData() {
        // When
        DataQualityMonitor.SymbolQualityMetrics metrics = monitor.getMetrics("005930");

        // Then
        assertThat(metrics).isNull();  // 데이터가 없으면 null 반환
    }

    @Test
    @DisplayName("isQualityAcceptable - 95% 이상은 acceptable")
    void testIsQualityAcceptable_Above95() {
        // Given - 95개 valid, 5개 invalid = 95%
        for (int i = 0; i < 95; i++) {
            monitor.recordValidTick("005930");
        }
        for (int i = 0; i < 5; i++) {
            monitor.recordInvalidTick("005930", "Error");
        }

        // When
        DataQualityMonitor.SymbolQualityMetrics metrics = monitor.getMetrics("005930");

        // Then
        assertThat(metrics.getQualityScore()).isEqualTo(95.0);
        assertThat(metrics.isQualityAcceptable()).isTrue();
    }

    @Test
    @DisplayName("isQualityAcceptable - 95% 미만은 not acceptable")
    void testIsQualityAcceptable_Below95() {
        // Given - 94개 valid, 6개 invalid = 94%
        for (int i = 0; i < 94; i++) {
            monitor.recordValidTick("005930");
        }
        for (int i = 0; i < 6; i++) {
            monitor.recordInvalidTick("005930", "Error");
        }

        // When
        DataQualityMonitor.SymbolQualityMetrics metrics = monitor.getMetrics("005930");

        // Then
        assertThat(metrics.getQualityScore()).isEqualTo(94.0);
        assertThat(metrics.isQualityAcceptable()).isFalse();
    }

    // ==================== 7. 메트릭 조회 및 리셋 ====================

    @Test
    @DisplayName("getMetrics - 심볼별 메트릭 조회")
    void testGetMetrics() {
        // Given
        monitor.recordValidTick("005930");
        monitor.recordValidTick("000660");

        // When
        DataQualityMonitor.SymbolQualityMetrics metrics1 = monitor.getMetrics("005930");
        DataQualityMonitor.SymbolQualityMetrics metrics2 = monitor.getMetrics("000660");

        // Then
        assertThat(metrics1.getSymbol()).isEqualTo("005930");
        assertThat(metrics1.getValidTickCount().get()).isEqualTo(1);

        assertThat(metrics2.getSymbol()).isEqualTo("000660");
        assertThat(metrics2.getValidTickCount().get()).isEqualTo(1);
    }

    @Test
    @DisplayName("getAllMetrics - 모든 심볼 메트릭 조회")
    void testGetAllMetrics() {
        // Given
        monitor.recordValidTick("005930");
        monitor.recordValidTick("000660");
        monitor.recordValidTick("035420");

        // When
        Map<String, DataQualityMonitor.SymbolQualityMetrics> allMetrics = monitor.getAllMetrics();

        // Then
        assertThat(allMetrics).hasSize(3);
        assertThat(allMetrics).containsKeys("005930", "000660", "035420");
    }

    @Test
    @DisplayName("resetMetrics - 특정 심볼 메트릭 리셋")
    void testResetMetrics() {
        // Given
        monitor.recordValidTick("005930");
        monitor.recordInvalidTick("005930", "Error");
        assertThat(monitor.getMetrics("005930")).isNotNull();

        // When
        monitor.resetMetrics("005930");

        // Then
        assertThat(monitor.getMetrics("005930")).isNull();
    }

    @Test
    @DisplayName("resetAllMetrics - 모든 메트릭 리셋")
    void testResetAllMetrics() {
        // Given
        monitor.recordValidTick("005930");
        monitor.recordValidTick("000660");
        monitor.recordValidTick("035420");
        assertThat(monitor.getAllMetrics()).hasSize(3);

        // When
        monitor.resetAllMetrics();

        // Then
        assertThat(monitor.getAllMetrics()).isEmpty();
    }

    // ==================== 8. 다중 심볼 시나리오 ====================

    @Test
    @DisplayName("다중 심볼 - 독립적인 메트릭 추적")
    void testMultipleSymbols_IndependentTracking() {
        // Given
        // Samsung: 10 valid
        for (int i = 0; i < 10; i++) {
            monitor.recordValidTick("005930");
        }

        // SK Hynix: 5 valid, 5 invalid
        for (int i = 0; i < 5; i++) {
            monitor.recordValidTick("000660");
            monitor.recordInvalidTick("000660", "Error");
        }

        // When
        DataQualityMonitor.SymbolQualityMetrics samsung = monitor.getMetrics("005930");
        DataQualityMonitor.SymbolQualityMetrics skHynix = monitor.getMetrics("000660");

        // Then
        assertThat(samsung.getValidTickCount().get()).isEqualTo(10);
        assertThat(samsung.getQualityScore()).isEqualTo(100.0);

        assertThat(skHynix.getValidTickCount().get()).isEqualTo(5);
        assertThat(skHynix.getInvalidTickCount().get()).isEqualTo(5);
        assertThat(skHynix.getQualityScore()).isEqualTo(50.0);
    }

    // ==================== 9. SymbolQualityMetrics 생성자 ====================

    @Test
    @DisplayName("SymbolQualityMetrics - 생성 시 초기값 확인")
    void testSymbolQualityMetrics_InitialValues() {
        // When
        DataQualityMonitor.SymbolQualityMetrics metrics =
                new DataQualityMonitor.SymbolQualityMetrics("005930");

        // Then
        assertThat(metrics.getSymbol()).isEqualTo("005930");
        assertThat(metrics.getValidTickCount().get()).isEqualTo(0);
        assertThat(metrics.getInvalidTickCount().get()).isEqualTo(0);
        assertThat(metrics.getDuplicateTickCount().get()).isEqualTo(0);
        assertThat(metrics.getOutOfSequenceTickCount().get()).isEqualTo(0);
        assertThat(metrics.getErrorCount().get()).isEqualTo(0);
        assertThat(metrics.getLastTickTimestamp()).isNull();
        assertThat(metrics.getLastError()).isNull();
        assertThat(metrics.getLastErrorTimestamp()).isNull();
    }
}
