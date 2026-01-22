package maru.trading.infra.persistence.adapter;

import maru.trading.application.ports.repo.AccountRepository;
import maru.trading.domain.account.Account;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.AccountEntity;
import maru.trading.infra.persistence.jpa.repository.AccountJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adapter implementation for AccountRepository.
 * Bridges the domain AccountRepository port with JPA persistence.
 */
@Component
public class AccountRepositoryAdapter implements AccountRepository {

    private final AccountJpaRepository accountJpaRepository;

    public AccountRepositoryAdapter(AccountJpaRepository accountJpaRepository) {
        this.accountJpaRepository = accountJpaRepository;
    }

    @Override
    public Optional<Account> findById(String accountId) {
        return accountJpaRepository.findByIdAndNotDeleted(accountId)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public Account save(Account account) {
        AccountEntity entity;

        if (account.getAccountId() != null) {
            // Update existing account
            Optional<AccountEntity> existingOpt = accountJpaRepository.findById(account.getAccountId());
            if (existingOpt.isPresent()) {
                entity = existingOpt.get();
                // Update mutable fields
                entity.updateStatus(account.getStatus());
                entity.updateAlias(account.getAlias());
            } else {
                // ID provided but not found - create new with given ID
                entity = toEntity(account);
            }
        } else {
            // Create new account with generated ID
            String newId = UlidGenerator.generate();
            entity = toEntity(Account.builder()
                    .accountId(newId)
                    .broker(account.getBroker())
                    .environment(account.getEnvironment())
                    .cano(account.getCano())
                    .acntPrdtCd(account.getAcntPrdtCd())
                    .status(account.getStatus())
                    .alias(account.getAlias())
                    .delyn("N")
                    .build());
        }

        AccountEntity saved = accountJpaRepository.save(entity);
        return toDomain(saved);
    }

    private AccountEntity toEntity(Account domain) {
        return AccountEntity.builder()
                .accountId(domain.getAccountId())
                .broker(domain.getBroker())
                .environment(domain.getEnvironment())
                .cano(domain.getCano())
                .acntPrdtCd(domain.getAcntPrdtCd())
                .status(domain.getStatus())
                .alias(domain.getAlias())
                .delyn(domain.getDelyn() != null ? domain.getDelyn() : "N")
                .build();
    }

    private Account toDomain(AccountEntity entity) {
        return Account.builder()
                .accountId(entity.getAccountId())
                .broker(entity.getBroker())
                .environment(entity.getEnvironment())
                .cano(entity.getCano())
                .acntPrdtCd(entity.getAcntPrdtCd())
                .status(entity.getStatus())
                .alias(entity.getAlias())
                .delyn(entity.getDelyn())
                .build();
    }
}
