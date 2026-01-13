package maru.trading.domain.signal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * SignalPolicy 테스트
 *
 * 테스트 범위:
 * 1. 시그널 검증 (SignalDecision 유효성)
 * 2. TTL 체크 (시그널 만료)
 * 3. 쿨다운 체크 (최소 시간 간격)
 * 4. 중복 시그널 체크
 * 5. 실행 전 검증
 */
@DisplayName("SignalPolicy 도메인 테스트")
class SignalPolicyTest {

    private SignalPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new SignalPolicy();
    }

    // ==================== 1. Signal Validation Tests ====================

    @Test
    @DisplayName("시그널 검증 - 유효한 BUY 시그널")
    void testValidateSignal_ValidBuySignal() {
        // Given
        SignalDecision decision = SignalDecision.buy(
                BigDecimal.TEN, "Test reason", 300);

        // When & Then - 예외 발생 안 함
        assertThatCode(() -> policy.validateSignal(decision))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("시그널 검증 - 유효한 SELL 시그널")
    void testValidateSignal_ValidSellSignal() {
        // Given
        SignalDecision decision = SignalDecision.sell(
                BigDecimal.ONE, "Test reason", 600);

        // When & Then - 예외 발생 안 함
        assertThatCode(() -> policy.validateSignal(decision))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("시그널 검증 - 유효한 HOLD 시그널")
    void testValidateSignal_ValidHoldSignal() {
        // Given
        SignalDecision decision = SignalDecision.hold("No action");

        // When & Then - 예외 발생 안 함
        assertThatCode(() -> policy.validateSignal(decision))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("시그널 검증 - Null 시그널 예외")
    void testValidateSignal_NullSignal_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> policy.validateSignal(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Signal decision cannot be null");
    }

    // ==================== 2. TTL Validation Tests ====================

    @Test
    @DisplayName("TTL 체크 - 유효한 시그널 (만료 전)")
    void testIsSignalValid_WithinTTL_ReturnsTrue() {
        // Given
        LocalDateTime signalCreatedAt = LocalDateTime.of(2026, 1, 1, 9, 30, 0);
        Integer ttlSeconds = 300; // 5분
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 9, 32, 0); // 2분 후

        // When
        boolean isValid = policy.isSignalValid(signalCreatedAt, ttlSeconds, now);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("TTL 체크 - 만료된 시그널 (TTL 초과)")
    void testIsSignalValid_ExpiredTTL_ReturnsFalse() {
        // Given
        LocalDateTime signalCreatedAt = LocalDateTime.of(2026, 1, 1, 9, 30, 0);
        Integer ttlSeconds = 300; // 5분
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 9, 36, 0); // 6분 후 (만료)

        // When
        boolean isValid = policy.isSignalValid(signalCreatedAt, ttlSeconds, now);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("TTL 체크 - 정확히 만료 시점")
    void testIsSignalValid_ExactTTL_ReturnsFalse() {
        // Given
        LocalDateTime signalCreatedAt = LocalDateTime.of(2026, 1, 1, 9, 30, 0);
        Integer ttlSeconds = 300; // 5분
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 9, 35, 0); // 정확히 5분 후

        // When
        boolean isValid = policy.isSignalValid(signalCreatedAt, ttlSeconds, now);

        // Then - 정확히 만료 시점은 invalid
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("TTL 체크 - TTL null이면 영구 유효")
    void testIsSignalValid_NoTTL_AlwaysValid() {
        // Given
        LocalDateTime signalCreatedAt = LocalDateTime.of(2026, 1, 1, 9, 0, 0);
        Integer ttlSeconds = null; // TTL 없음
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 12, 0, 0); // 3시간 후

        // When
        boolean isValid = policy.isSignalValid(signalCreatedAt, ttlSeconds, now);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("TTL 체크 - TTL이 0이면 영구 유효")
    void testIsSignalValid_ZeroTTL_AlwaysValid() {
        // Given
        LocalDateTime signalCreatedAt = LocalDateTime.of(2026, 1, 1, 9, 0, 0);
        Integer ttlSeconds = 0;
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 12, 0, 0);

        // When
        boolean isValid = policy.isSignalValid(signalCreatedAt, ttlSeconds, now);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("TTL 체크 - Null 타임스탬프 예외")
    void testIsSignalValid_NullTimestamp_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> policy.isSignalValid(null, 300, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Timestamps cannot be null");

        assertThatThrownBy(() -> policy.isSignalValid(LocalDateTime.now(), 300, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Timestamps cannot be null");
    }

    // ==================== 3. Cooldown Validation Tests ====================

    @Test
    @DisplayName("쿨다운 체크 - 쿨다운 통과 (충분한 시간 경과)")
    void testIsCooldownPassed_EnoughTimePassed_ReturnsTrue() {
        // Given
        LocalDateTime lastSignalTime = LocalDateTime.of(2026, 1, 1, 9, 30, 0);
        int cooldownSeconds = 60; // 1분
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 9, 31, 30); // 1분 30초 후

        // When
        boolean passed = policy.isCooldownPassed(lastSignalTime, cooldownSeconds, now);

        // Then
        assertThat(passed).isTrue();
    }

    @Test
    @DisplayName("쿨다운 체크 - 쿨다운 미통과 (시간 부족)")
    void testIsCooldownPassed_NotEnoughTime_ReturnsFalse() {
        // Given
        LocalDateTime lastSignalTime = LocalDateTime.of(2026, 1, 1, 9, 30, 0);
        int cooldownSeconds = 60; // 1분
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 9, 30, 30); // 30초 후 (부족)

        // When
        boolean passed = policy.isCooldownPassed(lastSignalTime, cooldownSeconds, now);

        // Then
        assertThat(passed).isFalse();
    }

    @Test
    @DisplayName("쿨다운 체크 - 정확히 쿨다운 시점 (통과)")
    void testIsCooldownPassed_ExactCooldown_ReturnsTrue() {
        // Given
        LocalDateTime lastSignalTime = LocalDateTime.of(2026, 1, 1, 9, 30, 0);
        int cooldownSeconds = 60; // 1분
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 9, 31, 0); // 정확히 1분 후

        // When
        boolean passed = policy.isCooldownPassed(lastSignalTime, cooldownSeconds, now);

        // Then - 정확히 쿨다운 시점은 통과
        assertThat(passed).isTrue();
    }

    @Test
    @DisplayName("쿨다운 체크 - 이전 시그널 없으면 항상 통과")
    void testIsCooldownPassed_NoLastSignal_AlwaysTrue() {
        // Given
        LocalDateTime lastSignalTime = null; // 이전 시그널 없음
        int cooldownSeconds = 60;
        LocalDateTime now = LocalDateTime.now();

        // When
        boolean passed = policy.isCooldownPassed(lastSignalTime, cooldownSeconds, now);

        // Then
        assertThat(passed).isTrue();
    }

    @Test
    @DisplayName("쿨다운 체크 - 쿨다운 0이면 항상 통과")
    void testIsCooldownPassed_ZeroCooldown_AlwaysTrue() {
        // Given
        LocalDateTime lastSignalTime = LocalDateTime.now();
        int cooldownSeconds = 0; // 쿨다운 없음
        LocalDateTime now = LocalDateTime.now();

        // When
        boolean passed = policy.isCooldownPassed(lastSignalTime, cooldownSeconds, now);

        // Then
        assertThat(passed).isTrue();
    }

    @Test
    @DisplayName("쿨다운 체크 - 음수 쿨다운이면 항상 통과")
    void testIsCooldownPassed_NegativeCooldown_AlwaysTrue() {
        // Given
        LocalDateTime lastSignalTime = LocalDateTime.now();
        int cooldownSeconds = -60;
        LocalDateTime now = LocalDateTime.now();

        // When
        boolean passed = policy.isCooldownPassed(lastSignalTime, cooldownSeconds, now);

        // Then
        assertThat(passed).isTrue();
    }

    @Test
    @DisplayName("쿨다운 체크 - Null 현재 시간 예외")
    void testIsCooldownPassed_NullNow_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> policy.isCooldownPassed(LocalDateTime.now(), 60, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Current time cannot be null");
    }

    // ==================== 4. Duplicate Signal Detection Tests ====================

    @Test
    @DisplayName("중복 체크 - 중복 발견 (같은 SignalType)")
    void testIsDuplicate_SameSignalType_ReturnsTrue() {
        // Given
        List<Signal> recentSignals = new ArrayList<>();
        recentSignals.add(Signal.builder()
                .signalId("SIG_001")
                .signalType(SignalType.BUY)
                .build());
        recentSignals.add(Signal.builder()
                .signalId("SIG_002")
                .signalType(SignalType.SELL)
                .build());

        SignalType newSignalType = SignalType.BUY;
        int lookbackSeconds = 300;
        LocalDateTime now = LocalDateTime.now();

        // When
        boolean isDuplicate = policy.isDuplicate(recentSignals, newSignalType, lookbackSeconds, now);

        // Then
        assertThat(isDuplicate).isTrue();
    }

    @Test
    @DisplayName("중복 체크 - 중복 없음 (다른 SignalType)")
    void testIsDuplicate_DifferentSignalType_ReturnsFalse() {
        // Given
        List<Signal> recentSignals = new ArrayList<>();
        recentSignals.add(Signal.builder()
                .signalId("SIG_001")
                .signalType(SignalType.BUY)
                .build());

        SignalType newSignalType = SignalType.SELL;
        int lookbackSeconds = 300;
        LocalDateTime now = LocalDateTime.now();

        // When
        boolean isDuplicate = policy.isDuplicate(recentSignals, newSignalType, lookbackSeconds, now);

        // Then
        assertThat(isDuplicate).isFalse();
    }

    @Test
    @DisplayName("중복 체크 - 최근 시그널 없으면 중복 없음")
    void testIsDuplicate_NoRecentSignals_ReturnsFalse() {
        // Given
        List<Signal> recentSignals = new ArrayList<>(); // 비어있음
        SignalType newSignalType = SignalType.BUY;
        int lookbackSeconds = 300;
        LocalDateTime now = LocalDateTime.now();

        // When
        boolean isDuplicate = policy.isDuplicate(recentSignals, newSignalType, lookbackSeconds, now);

        // Then
        assertThat(isDuplicate).isFalse();
    }

    @Test
    @DisplayName("중복 체크 - lookbackSeconds가 0이면 중복 체크 안 함")
    void testIsDuplicate_ZeroLookback_ReturnsFalse() {
        // Given
        List<Signal> recentSignals = new ArrayList<>();
        recentSignals.add(Signal.builder()
                .signalId("SIG_001")
                .signalType(SignalType.BUY)
                .build());

        SignalType newSignalType = SignalType.BUY;
        int lookbackSeconds = 0; // 중복 체크 안 함
        LocalDateTime now = LocalDateTime.now();

        // When
        boolean isDuplicate = policy.isDuplicate(recentSignals, newSignalType, lookbackSeconds, now);

        // Then
        assertThat(isDuplicate).isFalse();
    }

    @Test
    @DisplayName("중복 체크 - Null recentSignals는 중복 없음 반환")
    void testIsDuplicate_NullRecentSignals_ReturnsFalse() {
        // Given
        List<Signal> recentSignals = null;
        SignalType newSignalType = SignalType.BUY;
        int lookbackSeconds = 300;
        LocalDateTime now = LocalDateTime.now();

        // When
        boolean isDuplicate = policy.isDuplicate(recentSignals, newSignalType, lookbackSeconds, now);

        // Then
        assertThat(isDuplicate).isFalse();
    }

    @Test
    @DisplayName("중복 체크 - Null SignalType 예외")
    void testIsDuplicate_NullSignalType_ThrowsException() {
        // Given - recentSignals에 요소가 있어야 null 체크까지 도달
        List<Signal> recentSignals = new ArrayList<>();
        recentSignals.add(Signal.builder()
                .signalId("SIG_001")
                .signalType(SignalType.BUY)
                .build());

        // When & Then
        assertThatThrownBy(() -> policy.isDuplicate(
                recentSignals, null, 300, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Signal type and current time cannot be null");
    }

    @Test
    @DisplayName("중복 체크 - Null 현재 시간 예외")
    void testIsDuplicate_NullNow_ThrowsException() {
        // Given - recentSignals에 요소가 있어야 null 체크까지 도달
        List<Signal> recentSignals = new ArrayList<>();
        recentSignals.add(Signal.builder()
                .signalId("SIG_001")
                .signalType(SignalType.BUY)
                .build());

        // When & Then
        assertThatThrownBy(() -> policy.isDuplicate(
                recentSignals, SignalType.BUY, 300, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Signal type and current time cannot be null");
    }

    // ==================== 5. Execution Validation Tests ====================

    @Test
    @DisplayName("실행 검증 - 유효한 BUY 시그널")
    void testValidateForExecution_ValidBuySignal() {
        // Given
        Signal signal = Signal.builder()
                .signalId("SIG_001")
                .signalType(SignalType.BUY)
                .targetValue(BigDecimal.TEN)
                .build();
        LocalDateTime now = LocalDateTime.now();

        // When & Then - 예외 발생 안 함
        assertThatCode(() -> policy.validateForExecution(signal, now))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("실행 검증 - 유효한 SELL 시그널")
    void testValidateForExecution_ValidSellSignal() {
        // Given
        Signal signal = Signal.builder()
                .signalId("SIG_002")
                .signalType(SignalType.SELL)
                .targetValue(BigDecimal.ONE)
                .build();
        LocalDateTime now = LocalDateTime.now();

        // When & Then - 예외 발생 안 함
        assertThatCode(() -> policy.validateForExecution(signal, now))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("실행 검증 - HOLD 시그널 예외")
    void testValidateForExecution_HoldSignal_ThrowsException() {
        // Given
        Signal signal = Signal.builder()
                .signalId("SIG_003")
                .signalType(SignalType.HOLD)
                .build();
        LocalDateTime now = LocalDateTime.now();

        // When & Then
        assertThatThrownBy(() -> policy.validateForExecution(signal, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot execute HOLD signal");
    }

    @Test
    @DisplayName("실행 검증 - Null 시그널 예외")
    void testValidateForExecution_NullSignal_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> policy.validateForExecution(null, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Signal cannot be null");
    }

    @Test
    @DisplayName("실행 검증 - targetValue가 null이면 예외")
    void testValidateForExecution_NullTargetValue_ThrowsException() {
        // Given
        Signal signal = Signal.builder()
                .signalId("SIG_004")
                .signalType(SignalType.BUY)
                .targetValue(null) // Null
                .build();
        LocalDateTime now = LocalDateTime.now();

        // When & Then
        assertThatThrownBy(() -> policy.validateForExecution(signal, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("target value must be positive");
    }

    @Test
    @DisplayName("실행 검증 - targetValue가 0이면 예외")
    void testValidateForExecution_ZeroTargetValue_ThrowsException() {
        // Given
        Signal signal = Signal.builder()
                .signalId("SIG_005")
                .signalType(SignalType.BUY)
                .targetValue(BigDecimal.ZERO)
                .build();
        LocalDateTime now = LocalDateTime.now();

        // When & Then
        assertThatThrownBy(() -> policy.validateForExecution(signal, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("target value must be positive");
    }

    @Test
    @DisplayName("실행 검증 - targetValue가 음수이면 예외")
    void testValidateForExecution_NegativeTargetValue_ThrowsException() {
        // Given
        Signal signal = Signal.builder()
                .signalId("SIG_006")
                .signalType(SignalType.BUY)
                .targetValue(BigDecimal.valueOf(-10))
                .build();
        LocalDateTime now = LocalDateTime.now();

        // When & Then
        assertThatThrownBy(() -> policy.validateForExecution(signal, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("target value must be positive");
    }
}
