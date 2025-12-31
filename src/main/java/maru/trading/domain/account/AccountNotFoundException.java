package maru.trading.domain.account;

import maru.trading.domain.shared.DomainException;
import maru.trading.domain.shared.ErrorCode;

public class AccountNotFoundException extends DomainException {
	public AccountNotFoundException(String accountId) {
		super(ErrorCode.ACCOUNT_001, "Account not found: " + accountId);
	}
}
