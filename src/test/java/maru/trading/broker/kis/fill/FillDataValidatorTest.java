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
 * FillDataValidator 테스트
 *
 * 테스트 범위:
 * 1. Null 체크
 * 2. Fill ID / Order ID 검증
 * 3. 타임스탬프 검증
 * 4. 체결 가격 범위 검증
 * 5. 체결 수량 범위 검증
 * 6. 주문 대조 검증
 */
@DisplayName("FillDataValidator 테스트")
class FillDataValidatorTest {

    private FillDataValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FillDataValidator();
    }

    // ==================== 1. Null 체크 ====================

    @Test
    @DisplayName("validate - Null Fill은 invalid")
    void testValidate_NullFill() {
        // When
        FillDataValidator.ValidationResult result = validator.validate(null);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Fill is null");
    }

    // ==================== 2. Fill ID / Order ID 검증 ====================

    @Test
    @DisplayName("validate - Null Fill ID는 invalid")
    void testValidate_NullFillId() {
        // Given
        Fill fill = new Fill(
                null,  // fillId
                "ORDER_001",
                "ACC_001",
                "005930",
                Side.BUY,
                BigDecimal.valueOf(70000),
                10,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(50),
                LocalDateTime.now(),
                "BROKER_001"
        );

        // When
        FillDataValidator.ValidationResult result = validator.validate(fill);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Fill ID is null");
    }

    @Test
    @DisplayName("validate - Empty Fill ID는 invalid")
    void testValidate_EmptyFillId() {
        // Given
        Fill fill = new Fill(
                "",  // empty fillId
                "ORDER_001",
                "ACC_001",
                "005930",
                Side.BUY,
                BigDecimal.valueOf(70000),
                10,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(50),
                LocalDateTime.now(),
                "BROKER_001"
        );

        // When
        FillDataValidator.ValidationResult result = validator.validate(fill);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Fill ID is null or empty");
    }

    @Test
    @DisplayName("validate - Null Order ID는 invalid")
    void testValidate_NullOrderId() {
        // Given
        Fill fill = new Fill(
                "FILL_001",
                null,  // orderId
                "ACC_001",
                "005930",
                Side.BUY,
                BigDecimal.valueOf(70000),
                10,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(50),
                LocalDateTime.now(),
                "BROKER_001"
        );

        // When
        FillDataValidator.ValidationResult result = validator.validate(fill);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Order ID is null");
    }

    @Test
    @DisplayName("validate - Empty Order ID는 invalid")
    void testValidate_EmptyOrderId() {
        // Given
        Fill fill = new Fill(
                "FILL_001",
                "",  // empty orderId
                "ACC_001",
                "005930",
                Side.BUY,
                BigDecimal.valueOf(70000),
                10,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(50),
                LocalDateTime.now(),
                "BROKER_001"
        );

        // When
        FillDataValidator.ValidationResult result = validator.validate(fill);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Order ID is null or empty");
    }

    // ==================== 3. 타임스탬프 검증 ====================

    @Test
    @DisplayName("validate - Null 타임스탬프는 invalid")
    void testValidate_NullTimestamp() {
        // Given
        Fill fill = new Fill(
                "FILL_001",
                "ORDER_001",
                "ACC_001",
                "005930",
                Side.BUY,
                BigDecimal.valueOf(70000),
                10,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(50),
                null,  // fillTimestamp
                "BROKER_001"
        );

        // When
        FillDataValidator.ValidationResult result = validator.validate(fill);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Timestamp is null");
    }

    @Test
    @DisplayName("validate - 미래 타임스탬프는 invalid")
    void testValidate_FutureTimestamp() {
        // Given
        Fill fill = new Fill(
                "FILL_001",
                "ORDER_001",
                "ACC_001",
                "005930",
                Side.BUY,
                BigDecimal.valueOf(70000),
                10,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(50),
                LocalDateTime.now().plusMinutes(2),  // future
                "BROKER_001"
        );

        // When
        FillDataValidator.ValidationResult result = validator.validate(fill);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Timestamp is in future");
    }

    @Test
    @DisplayName("validate - 현재 타임스탬프는 valid")
    void testValidate_CurrentTimestamp() {
        // Given
        Fill fill = createValidFill();

        // When
        FillDataValidator.ValidationResult result = validator.validate(fill);

        // Then
        assertThat(result.isValid()).isTrue();
    }

    // ==================== 4. 체결 가격 범위 검증 ====================

    @Test
    @DisplayName("validate - Null 체결 가격은 invalid")
    void testValidate_NullFillPrice() {
        // Given
        Fill fill = new Fill(
                "FILL_001",
                "ORDER_001",
                "ACC_001",
                "005930",
                Side.BUY,
                null,  // fillPrice
                10,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(50),
                LocalDateTime.now(),
                "BROKER_001"
        );

        // When
        FillDataValidator.ValidationResult result = validator.validate(fill);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Fill price is null");
    }

    @Test
    @DisplayName("validate - 0원은 invalid")
    void testValidate_ZeroFillPrice() {
        // Given
        Fill fill = createFillWithPrice(BigDecimal.ZERO);

        // When
        FillDataValidator.ValidationResult result = validator.validate(fill);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Fill price is zero or negative");
    }

    @Test
    @DisplayName("validate - 음수 가격은 invalid")
    void testValidate_NegativeFillPrice() {
        // Given
        Fill fill = createFillWithPrice(BigDecimal.valueOf(-1000));

        // When
        FillDataValidator.ValidationResult result = validator.validate(fill);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Fill price is zero or negative");
    }

    @Test
    @DisplayName("validate - 최소 가격 미만은 invalid")
    void testValidate_FillPriceTooLow() {
        // Given (100원 미만)
        Fill fill = createFillWithPrice(BigDecimal.valueOf(50));

        // When
        FillDataValidator.ValidationResult result = validator.validate(fill);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Fill price too low");
    }

    @Test
    @DisplayName("validate - 최대 가격 초과는 invalid")
    void testValidate_FillPriceTooHigh() {
        // Given (10,000,000원 초과)
        Fill fill = createFillWithPrice(BigDecimal.valueOf(15_000_000));

        // When
        FillDataValidator.ValidationResult result = validator.validate(fill);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Fill price too high");
    }

    @Test
    @DisplayName("validate - 정상 가격 범위는 valid")
    void testValidate_ValidFillPriceRange() {
        // Given
        Fill fill1 = createFillWithPrice(BigDecimal.valueOf(100));
        Fill fill2 = createFillWithPrice(BigDecimal.valueOf(50000));
        Fill fill3 = createFillWithPrice(BigDecimal.valueOf(10_000_000));

        // When & Then
        assertThat(validator.validate(fill1).isValid()).isTrue();
        assertThat(validator.validate(fill2).isValid()).isTrue();
        assertThat(validator.validate(fill3).isValid()).isTrue();
    }

    // ==================== 5. 체결 수량 범위 검증 ====================

    @Test
    @DisplayName("validate - 0 수량은 invalid")
    void testValidate_ZeroFillQty() {
        // Given
        Fill fill = createFillWithQty(0);

        // When
        FillDataValidator.ValidationResult result = validator.validate(fill);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Filled quantity too low");
    }

    @Test
    @DisplayName("validate - 음수 수량은 invalid")
    void testValidate_NegativeFillQty() {
        // Given
        Fill fill = createFillWithQty(-10);

        // When
        FillDataValidator.ValidationResult result = validator.validate(fill);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Filled quantity too low");
    }

    @Test
    @DisplayName("validate - 최대 수량 초과는 invalid")
    void testValidate_FillQtyTooHigh() {
        // Given (1,000,000 초과)
        Fill fill = createFillWithQty(2_000_000);

        // When
        FillDataValidator.ValidationResult result = validator.validate(fill);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Filled quantity too high");
    }

    @Test
    @DisplayName("validate - 정상 수량 범위는 valid")
    void testValidate_ValidFillQtyRange() {
        // Given
        Fill fill1 = createFillWithQty(1);
        Fill fill2 = createFillWithQty(500);
        Fill fill3 = createFillWithQty(1_000_000);

        // When & Then
        assertThat(validator.validate(fill1).isValid()).isTrue();
        assertThat(validator.validate(fill2).isValid()).isTrue();
        assertThat(validator.validate(fill3).isValid()).isTrue();
    }

    // ==================== 6. 주문 대조 검증 ====================

    @Test
    @DisplayName("validateAgainstOrder - Order ID 일치 검증")
    void testValidateAgainstOrder_OrderIdMatch() {
        // Given
        Fill fill = createValidFill();

        // When
        FillDataValidator.ValidationResult result =
                validator.validateAgainstOrder(fill, "ORDER_001", null);

        // Then
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("validateAgainstOrder - Order ID 불일치는 invalid")
    void testValidateAgainstOrder_OrderIdMismatch() {
        // Given
        Fill fill = createValidFill();  // ORDER_001

        // When
        FillDataValidator.ValidationResult result =
                validator.validateAgainstOrder(fill, "ORDER_999", null);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Fill order ID mismatch");
        assertThat(result.getErrorMessage()).contains("expected=ORDER_999");
        assertThat(result.getErrorMessage()).contains("actual=ORDER_001");
    }

    @Test
    @DisplayName("validateAgainstOrder - 심볼 일치 검증")
    void testValidateAgainstOrder_SymbolMatch() {
        // Given
        Fill fill = createValidFill();

        // When
        FillDataValidator.ValidationResult result =
                validator.validateAgainstOrder(fill, null, "005930");

        // Then
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("validateAgainstOrder - 심볼 불일치는 invalid")
    void testValidateAgainstOrder_SymbolMismatch() {
        // Given
        Fill fill = createValidFill();  // 005930

        // When
        FillDataValidator.ValidationResult result =
                validator.validateAgainstOrder(fill, null, "000660");

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Fill symbol mismatch");
        assertThat(result.getErrorMessage()).contains("expected=000660");
        assertThat(result.getErrorMessage()).contains("actual=005930");
    }

    @Test
    @DisplayName("validateAgainstOrder - Order ID와 심볼 모두 일치")
    void testValidateAgainstOrder_BothMatch() {
        // Given
        Fill fill = createValidFill();

        // When
        FillDataValidator.ValidationResult result =
                validator.validateAgainstOrder(fill, "ORDER_001", "005930");

        // Then
        assertThat(result.isValid()).isTrue();
    }

    // ==================== 7. 통합 시나리오 ====================

    @Test
    @DisplayName("validate - 완전히 유효한 Fill은 valid")
    void testValidate_CompletelyValid() {
        // Given
        Fill fill = createValidFill();

        // When
        FillDataValidator.ValidationResult result = validator.validate(fill);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("ValidationResult - valid 생성")
    void testValidationResult_Valid() {
        // When
        FillDataValidator.ValidationResult result = FillDataValidator.ValidationResult.valid();

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("ValidationResult - invalid 생성")
    void testValidationResult_Invalid() {
        // When
        FillDataValidator.ValidationResult result =
                FillDataValidator.ValidationResult.invalid("Test error");

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Test error");
    }

    // ==================== Helper Methods ====================

    private Fill createValidFill() {
        return new Fill(
                "FILL_001",
                "ORDER_001",
                "ACC_001",
                "005930",
                Side.BUY,
                BigDecimal.valueOf(70000),
                10,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(50),
                LocalDateTime.now(),
                "BROKER_001"
        );
    }

    private Fill createFillWithPrice(BigDecimal price) {
        return new Fill(
                "FILL_001",
                "ORDER_001",
                "ACC_001",
                "005930",
                Side.BUY,
                price,
                10,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(50),
                LocalDateTime.now(),
                "BROKER_001"
        );
    }

    private Fill createFillWithQty(int qty) {
        return new Fill(
                "FILL_001",
                "ORDER_001",
                "ACC_001",
                "005930",
                Side.BUY,
                BigDecimal.valueOf(70000),
                qty,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(50),
                LocalDateTime.now(),
                "BROKER_001"
        );
    }
}
