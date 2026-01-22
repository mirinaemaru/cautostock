package maru.trading.domain.risk;

import maru.trading.domain.shared.DomainException;
import maru.trading.domain.shared.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RiskLimitExceededException Test")
class RiskLimitExceededExceptionTest {

    @Test
    @DisplayName("Should create exception with detail message")
    void shouldCreateExceptionWithDetailMessage() {
        String detail = "Daily loss limit exceeded: -100000 KRW";

        RiskLimitExceededException exception = new RiskLimitExceededException(detail);

        assertThat(exception.getMessage()).contains(detail);
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RISK_001);
    }

    @Test
    @DisplayName("Should extend DomainException")
    void shouldExtendDomainException() {
        RiskLimitExceededException exception = new RiskLimitExceededException("test");

        assertThat(exception).isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("Should have RISK_001 error code")
    void shouldHaveRisk001ErrorCode() {
        RiskLimitExceededException exception = new RiskLimitExceededException("Max position exceeded");

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RISK_001);
    }
}
