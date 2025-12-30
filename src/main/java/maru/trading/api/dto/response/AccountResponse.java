package maru.trading.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.account.AccountStatus;
import maru.trading.domain.shared.Environment;

import java.time.LocalDateTime;

/**
 * 계좌 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountResponse {
	private String accountId;
	private String broker;
	private Environment environment;
	private String cano;
	private String acntPrdtCd;
	private AccountStatus status;
	private String alias;
	private String delyn;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
