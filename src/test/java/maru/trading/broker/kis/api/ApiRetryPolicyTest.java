package maru.trading.broker.kis.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApiRetryPolicy 테스트
 *
 * 테스트 범위:
 * 1. 지연 계산 (지수 백오프)
 * 2. 재시도 가능 여부 판단
 * 3. 기본 정책 검증
 */
@DisplayName("ApiRetryPolicy 테스트")
class ApiRetryPolicyTest {

    @Test
    @DisplayName("calculateDelay - 지수 백오프 계산")
    void testCalculateDelay_ExponentialBackoff() {
        // Given
        ApiRetryPolicy policy = ApiRetryPolicy.builder()
                .initialDelayMs(1000)
                .backoffMultiplier(2.0)
                .maxDelayMs(60000)
                .build();

        // When & Then
        assertThat(policy.calculateDelay(0)).isEqualTo(1000);   // 1초
        assertThat(policy.calculateDelay(1)).isEqualTo(2000);   // 2초
        assertThat(policy.calculateDelay(2)).isEqualTo(4000);   // 4초
        assertThat(policy.calculateDelay(3)).isEqualTo(8000);   // 8초
    }

    @Test
    @DisplayName("calculateDelay - 최대 지연 시간 제한")
    void testCalculateDelay_MaxDelayLimit() {
        // Given
        ApiRetryPolicy policy = ApiRetryPolicy.builder()
                .initialDelayMs(1000)
                .backoffMultiplier(2.0)
                .maxDelayMs(5000)  // 5초 최대
                .build();

        // When & Then
        assertThat(policy.calculateDelay(0)).isEqualTo(1000);
        assertThat(policy.calculateDelay(1)).isEqualTo(2000);
        assertThat(policy.calculateDelay(2)).isEqualTo(4000);
        assertThat(policy.calculateDelay(3)).isEqualTo(5000);  // 최대
        assertThat(policy.calculateDelay(4)).isEqualTo(5000);  // 최대
    }

    @Test
    @DisplayName("calculateDelay - 음수 attempt는 0 반환")
    void testCalculateDelay_NegativeAttempt() {
        // Given
        ApiRetryPolicy policy = ApiRetryPolicy.defaultOrderPolicy();

        // When & Then
        assertThat(policy.calculateDelay(-1)).isEqualTo(0);
        assertThat(policy.calculateDelay(-10)).isEqualTo(0);
    }

    @Test
    @DisplayName("shouldRetry - 최대 재시도 이내")
    void testShouldRetry_WithinLimit() {
        // Given
        ApiRetryPolicy policy = ApiRetryPolicy.builder()
                .maxRetries(3)
                .build();

        // When & Then
        assertThat(policy.shouldRetry(0)).isTrue();
        assertThat(policy.shouldRetry(1)).isTrue();
        assertThat(policy.shouldRetry(2)).isTrue();
        assertThat(policy.shouldRetry(3)).isFalse();  // 초과
    }

    @Test
    @DisplayName("shouldRetry - 최대 재시도 초과")
    void testShouldRetry_ExceedsLimit() {
        // Given
        ApiRetryPolicy policy = ApiRetryPolicy.builder()
                .maxRetries(5)
                .build();

        // When & Then
        assertThat(policy.shouldRetry(4)).isTrue();
        assertThat(policy.shouldRetry(5)).isFalse();
        assertThat(policy.shouldRetry(10)).isFalse();
    }

    @Test
    @DisplayName("defaultOrderPolicy - 주문 API용 기본 정책")
    void testDefaultOrderPolicy() {
        // When
        ApiRetryPolicy policy = ApiRetryPolicy.defaultOrderPolicy();

        // Then
        assertThat(policy.getMaxRetries()).isEqualTo(3);
        assertThat(policy.getInitialDelayMs()).isEqualTo(1000);
        assertThat(policy.getMaxDelayMs()).isEqualTo(10000);
        assertThat(policy.getBackoffMultiplier()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("defaultQueryPolicy - 조회 API용 기본 정책")
    void testDefaultQueryPolicy() {
        // When
        ApiRetryPolicy policy = ApiRetryPolicy.defaultQueryPolicy();

        // Then
        assertThat(policy.getMaxRetries()).isEqualTo(5);
        assertThat(policy.getInitialDelayMs()).isEqualTo(500);
        assertThat(policy.getMaxDelayMs()).isEqualTo(5000);
        assertThat(policy.getBackoffMultiplier()).isEqualTo(1.5);
    }

    @Test
    @DisplayName("defaultOrderPolicy - 지연 계산 검증")
    void testDefaultOrderPolicy_DelayCalculation() {
        // Given
        ApiRetryPolicy policy = ApiRetryPolicy.defaultOrderPolicy();

        // When & Then
        assertThat(policy.calculateDelay(0)).isEqualTo(1000);   // 1초
        assertThat(policy.calculateDelay(1)).isEqualTo(2000);   // 2초
        assertThat(policy.calculateDelay(2)).isEqualTo(4000);   // 4초
        assertThat(policy.calculateDelay(3)).isEqualTo(8000);   // 8초
        assertThat(policy.calculateDelay(4)).isEqualTo(10000);  // 10초 (최대)
    }

    @Test
    @DisplayName("defaultQueryPolicy - 지연 계산 검증")
    void testDefaultQueryPolicy_DelayCalculation() {
        // Given
        ApiRetryPolicy policy = ApiRetryPolicy.defaultQueryPolicy();

        // When & Then
        assertThat(policy.calculateDelay(0)).isEqualTo(500);    // 0.5초
        assertThat(policy.calculateDelay(1)).isEqualTo(750);    // 0.75초
        assertThat(policy.calculateDelay(2)).isEqualTo(1125);   // 1.125초
        assertThat(policy.calculateDelay(3)).isEqualTo(1687);   // 1.687초
        assertThat(policy.calculateDelay(4)).isEqualTo(2531);   // 2.531초
        assertThat(policy.calculateDelay(5)).isEqualTo(3796);   // 3.796초
        assertThat(policy.calculateDelay(6)).isEqualTo(5000);   // 5초 (최대)
    }

    @Test
    @DisplayName("builder - 커스텀 정책 생성")
    void testBuilder_CustomPolicy() {
        // When
        ApiRetryPolicy policy = ApiRetryPolicy.builder()
                .maxRetries(7)
                .initialDelayMs(2000)
                .maxDelayMs(30000)
                .backoffMultiplier(1.8)
                .build();

        // Then
        assertThat(policy.getMaxRetries()).isEqualTo(7);
        assertThat(policy.getInitialDelayMs()).isEqualTo(2000);
        assertThat(policy.getMaxDelayMs()).isEqualTo(30000);
        assertThat(policy.getBackoffMultiplier()).isEqualTo(1.8);
    }
}
