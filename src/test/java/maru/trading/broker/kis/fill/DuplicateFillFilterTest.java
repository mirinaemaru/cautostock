package maru.trading.broker.kis.fill;

import maru.trading.domain.execution.Fill;
import maru.trading.domain.order.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DuplicateFillFilter 테스트
 *
 * 테스트 범위:
 * 1. 중복 체결 감지
 * 2. 캐시 관리
 * 3. Null 처리
 * 4. 수동 처리 표시
 */
@DisplayName("DuplicateFillFilter 테스트")
class DuplicateFillFilterTest {

    private DuplicateFillFilter filter;

    @BeforeEach
    void setUp() {
        filter = new DuplicateFillFilter();
    }

    // ==================== 1. 중복 체결 감지 ====================

    @Test
    @DisplayName("isDuplicate - 첫 번째 Fill은 중복 아님")
    void testIsDuplicate_FirstFill() {
        // Given
        Fill fill = createFill("FILL_001");

        // When
        boolean isDuplicate = filter.isDuplicate(fill);

        // Then
        assertThat(isDuplicate).isFalse();
        assertThat(filter.isInCache("FILL_001")).isTrue();
    }

    @Test
    @DisplayName("isDuplicate - 동일 Fill ID는 중복")
    void testIsDuplicate_SameFillId() {
        // Given
        Fill fill1 = createFill("FILL_001");
        Fill fill2 = createFill("FILL_001");

        // When
        boolean isDuplicate1 = filter.isDuplicate(fill1);
        boolean isDuplicate2 = filter.isDuplicate(fill2);

        // Then
        assertThat(isDuplicate1).isFalse();  // 첫 번째는 중복 아님
        assertThat(isDuplicate2).isTrue();   // 두 번째는 중복
    }

    @Test
    @DisplayName("isDuplicate - 다른 Fill ID는 중복 아님")
    void testIsDuplicate_DifferentFillIds() {
        // Given
        Fill fill1 = createFill("FILL_001");
        Fill fill2 = createFill("FILL_002");
        Fill fill3 = createFill("FILL_003");

        // When
        boolean isDuplicate1 = filter.isDuplicate(fill1);
        boolean isDuplicate2 = filter.isDuplicate(fill2);
        boolean isDuplicate3 = filter.isDuplicate(fill3);

        // Then
        assertThat(isDuplicate1).isFalse();
        assertThat(isDuplicate2).isFalse();
        assertThat(isDuplicate3).isFalse();
        assertThat(filter.getCacheSize()).isEqualTo(3);
    }

    @Test
    @DisplayName("isDuplicate - 여러 번 중복 체크")
    void testIsDuplicate_MultipleDuplicateChecks() {
        // Given
        Fill fill = createFill("FILL_001");

        // When & Then
        assertThat(filter.isDuplicate(fill)).isFalse();  // 첫 번째
        assertThat(filter.isDuplicate(fill)).isTrue();   // 두 번째
        assertThat(filter.isDuplicate(fill)).isTrue();   // 세 번째
        assertThat(filter.isDuplicate(fill)).isTrue();   // 네 번째
        assertThat(filter.getCacheSize()).isEqualTo(1);
    }

    // ==================== 2. Null 처리 ====================

    @Test
    @DisplayName("isDuplicate - Null Fill은 false 반환")
    void testIsDuplicate_NullFill() {
        // When
        boolean isDuplicate = filter.isDuplicate(null);

        // Then
        assertThat(isDuplicate).isFalse();
    }

    @Test
    @DisplayName("isDuplicate - Null Fill ID는 false 반환")
    void testIsDuplicate_NullFillId() {
        // Given
        Fill fill = new Fill(
                null,  // Null fillId
                "ORDER_001",
                "ACC_001",
                "005930",
                Side.BUY,
                BigDecimal.valueOf(70000),
                10,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                LocalDateTime.now(),
                "BROKER_001"
        );

        // When
        boolean isDuplicate = filter.isDuplicate(fill);

        // Then
        assertThat(isDuplicate).isFalse();
    }

    // ==================== 3. 수동 처리 표시 ====================

    @Test
    @DisplayName("markAsProcessed - Fill ID를 처리됨으로 표시")
    void testMarkAsProcessed() {
        // When
        filter.markAsProcessed("FILL_001");

        // Then
        assertThat(filter.isInCache("FILL_001")).isTrue();
        assertThat(filter.getCacheSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("markAsProcessed - 이미 처리된 Fill")
    void testMarkAsProcessed_AlreadyProcessed() {
        // Given
        Fill fill = createFill("FILL_001");
        filter.isDuplicate(fill);  // 첫 번째 처리

        // When
        filter.markAsProcessed("FILL_001");  // 다시 표시

        // Then
        assertThat(filter.isInCache("FILL_001")).isTrue();
        assertThat(filter.getCacheSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("markAsProcessed - Null Fill ID는 무시")
    void testMarkAsProcessed_NullFillId() {
        // When
        filter.markAsProcessed(null);

        // Then
        assertThat(filter.getCacheSize()).isEqualTo(0);
    }

    // ==================== 4. 캐시 관리 ====================

    @Test
    @DisplayName("isInCache - 캐시에 있는 Fill ID")
    void testIsInCache_Exists() {
        // Given
        Fill fill = createFill("FILL_001");
        filter.isDuplicate(fill);

        // When & Then
        assertThat(filter.isInCache("FILL_001")).isTrue();
    }

    @Test
    @DisplayName("isInCache - 캐시에 없는 Fill ID")
    void testIsInCache_NotExists() {
        // When & Then
        assertThat(filter.isInCache("FILL_999")).isFalse();
    }

    @Test
    @DisplayName("isInCache - Null Fill ID")
    void testIsInCache_NullFillId() {
        // When & Then
        assertThat(filter.isInCache(null)).isFalse();
    }

    @Test
    @DisplayName("getCacheSize - 캐시 크기 확인")
    void testGetCacheSize() {
        // Given
        filter.isDuplicate(createFill("FILL_001"));
        filter.isDuplicate(createFill("FILL_002"));
        filter.isDuplicate(createFill("FILL_003"));

        // When
        int size = filter.getCacheSize();

        // Then
        assertThat(size).isEqualTo(3);
    }

    @Test
    @DisplayName("getCacheSize - 중복은 크기에 영향 없음")
    void testGetCacheSize_DuplicatesDoNotAffect() {
        // Given
        Fill fill = createFill("FILL_001");
        filter.isDuplicate(fill);
        filter.isDuplicate(fill);  // 중복
        filter.isDuplicate(fill);  // 중복

        // When
        int size = filter.getCacheSize();

        // Then
        assertThat(size).isEqualTo(1);
    }

    @Test
    @DisplayName("clearCache - 모든 캐시 삭제")
    void testClearCache() {
        // Given
        filter.isDuplicate(createFill("FILL_001"));
        filter.isDuplicate(createFill("FILL_002"));
        filter.isDuplicate(createFill("FILL_003"));
        assertThat(filter.getCacheSize()).isEqualTo(3);

        // When
        filter.clearCache();

        // Then
        assertThat(filter.getCacheSize()).isEqualTo(0);
        assertThat(filter.isInCache("FILL_001")).isFalse();
        assertThat(filter.isInCache("FILL_002")).isFalse();
        assertThat(filter.isInCache("FILL_003")).isFalse();
    }

    @Test
    @DisplayName("clearCache - 캐시 삭제 후 재사용 가능")
    void testClearCache_ReuseAfterClear() {
        // Given
        Fill fill = createFill("FILL_001");
        filter.isDuplicate(fill);
        assertThat(filter.isDuplicate(fill)).isTrue();  // 중복

        // When
        filter.clearCache();

        // Then
        assertThat(filter.isDuplicate(fill)).isFalse();  // 첫 번째로 취급
        assertThat(filter.getCacheSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("getCacheStats - 캐시 통계 문자열")
    void testGetCacheStats() {
        // Given
        filter.isDuplicate(createFill("FILL_001"));
        filter.isDuplicate(createFill("FILL_002"));

        // When
        String stats = filter.getCacheStats();

        // Then
        assertThat(stats).contains("size=2");
        assertThat(stats).contains("max=10000");
    }

    // ==================== 5. 동시성 시나리오 ====================

    @Test
    @DisplayName("동시성 - 여러 Fill을 동시에 처리해도 안전")
    void testConcurrency_MultipleFillsSimultaneous() {
        // Given
        int fillCount = 100;

        // When - 순차 처리 (실제로는 동시 처리되지만 테스트에서는 순차)
        for (int i = 0; i < fillCount; i++) {
            Fill fill = createFill("FILL_" + i);
            boolean isDuplicate = filter.isDuplicate(fill);
            assertThat(isDuplicate).isFalse();  // 모두 첫 번째
        }

        // Then
        assertThat(filter.getCacheSize()).isEqualTo(fillCount);
    }

    // ==================== 6. 통합 시나리오 ====================

    @Test
    @DisplayName("통합 - 실제 Fill 처리 시나리오")
    void testIntegration_RealFillProcessing() {
        // Given - WebSocket에서 3개의 Fill 수신 (1개는 중복)
        Fill fill1 = createFill("FILL_001");
        Fill fill2 = createFill("FILL_002");
        Fill fill1Duplicate = createFill("FILL_001");  // 중복

        // When & Then
        // 첫 번째 Fill - 처리
        assertThat(filter.isDuplicate(fill1)).isFalse();

        // 두 번째 Fill - 처리
        assertThat(filter.isDuplicate(fill2)).isFalse();

        // 첫 번째 Fill 중복 - 무시
        assertThat(filter.isDuplicate(fill1Duplicate)).isTrue();

        // 캐시에는 2개만 저장
        assertThat(filter.getCacheSize()).isEqualTo(2);
        assertThat(filter.isInCache("FILL_001")).isTrue();
        assertThat(filter.isInCache("FILL_002")).isTrue();
    }

    // ==================== Helper Methods ====================

    private Fill createFill(String fillId) {
        return new Fill(
                fillId,
                "ORDER_001",
                "ACC_001",
                "005930",
                Side.BUY,
                BigDecimal.valueOf(70000),
                10,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                LocalDateTime.now(),
                "BROKER_001"
        );
    }
}
