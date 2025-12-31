package maru.trading.domain.risk;

import maru.trading.domain.shared.DomainException;
import maru.trading.domain.shared.ErrorCode;

public class RiskLimitExceededException extends DomainException {
	public RiskLimitExceededException(String detail) {
		super(ErrorCode.RISK_001, detail);
	}
}
