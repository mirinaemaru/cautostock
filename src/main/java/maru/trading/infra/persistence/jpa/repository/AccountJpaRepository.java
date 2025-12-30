package maru.trading.infra.persistence.jpa.repository;

import maru.trading.domain.account.AccountStatus;
import maru.trading.domain.shared.Environment;
import maru.trading.infra.persistence.jpa.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountJpaRepository extends JpaRepository<AccountEntity, String> {

	Optional<AccountEntity> findByBrokerAndEnvironmentAndCanoAndAcntPrdtCdAndDelyn(
			String broker,
			Environment environment,
			String cano,
			String acntPrdtCd,
			String delyn
	);

	List<AccountEntity> findByEnvironmentAndStatusAndDelyn(Environment environment, AccountStatus status, String delyn);

	List<AccountEntity> findByStatusAndDelyn(AccountStatus status, String delyn);

	List<AccountEntity> findByDelyn(String delyn);

	@Query("SELECT a FROM AccountEntity a WHERE a.accountId = :accountId AND a.delyn = 'N'")
	Optional<AccountEntity> findByIdAndNotDeleted(String accountId);
}
