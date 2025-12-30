package maru.trading.domain.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.shared.Environment;

/**
 * 계좌 도메인 모델
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {
	private String accountId;
	private String broker;
	private Environment environment;
	private String cano;
	private String acntPrdtCd;
	private AccountStatus status;
	private String alias;
	@Builder.Default
	private String delyn = "N";

	public boolean isActive() {
		return status == AccountStatus.ACTIVE && !"Y".equals(delyn);
	}

	public boolean isPaper() {
		return environment == Environment.PAPER;
	}

	public boolean isDeleted() {
		return "Y".equals(delyn);
	}
}
