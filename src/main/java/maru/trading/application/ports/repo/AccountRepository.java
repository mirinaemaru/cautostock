package maru.trading.application.ports.repo;

import maru.trading.domain.account.Account;

import java.util.Optional;

/**
 * 계좌 저장소 포트
 */
public interface AccountRepository {

	Optional<Account> findById(String accountId);

	Account save(Account account);
}
