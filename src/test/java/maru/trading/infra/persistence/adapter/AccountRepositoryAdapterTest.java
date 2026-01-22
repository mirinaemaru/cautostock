package maru.trading.infra.persistence.adapter;

import maru.trading.domain.account.Account;
import maru.trading.domain.account.AccountStatus;
import maru.trading.domain.shared.Environment;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.AccountEntity;
import maru.trading.infra.persistence.jpa.repository.AccountJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AccountRepositoryAdapter Integration Test
 *
 * Tests the adapter implementation bridging domain AccountRepository port with JPA persistence.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AccountRepositoryAdapter Test")
class AccountRepositoryAdapterTest {

    @Autowired
    private AccountRepositoryAdapter accountRepositoryAdapter;

    @Autowired
    private AccountJpaRepository accountJpaRepository;

    private String testAccountId;

    @BeforeEach
    void setUp() {
        testAccountId = UlidGenerator.generate();
        createTestAccount(testAccountId);
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Should return account when exists and not deleted")
        void findById_Success() {
            Optional<Account> result = accountRepositoryAdapter.findById(testAccountId);

            assertThat(result).isPresent();
            assertThat(result.get().getAccountId()).isEqualTo(testAccountId);
            assertThat(result.get().getBroker()).isEqualTo("KIS");
            assertThat(result.get().getEnvironment()).isEqualTo(Environment.PAPER);
            assertThat(result.get().getStatus()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should return empty when account does not exist")
        void findById_NotFound() {
            String nonExistentId = UlidGenerator.generate();

            Optional<Account> result = accountRepositoryAdapter.findById(nonExistentId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty when account is soft deleted")
        void findById_SoftDeleted() {
            // Soft delete the account
            AccountEntity entity = accountJpaRepository.findById(testAccountId).orElseThrow();
            entity.softDelete();
            accountJpaRepository.save(entity);

            Optional<Account> result = accountRepositoryAdapter.findById(testAccountId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("Should create new account when ID is null")
        void save_CreateNew() {
            Account newAccount = Account.builder()
                    .broker("KIS")
                    .environment(Environment.PAPER)
                    .cano("12345678")
                    .acntPrdtCd("01")
                    .status(AccountStatus.ACTIVE)
                    .alias("New Test Account")
                    .build();

            Account saved = accountRepositoryAdapter.save(newAccount);

            assertThat(saved.getAccountId()).isNotNull();
            assertThat(saved.getBroker()).isEqualTo("KIS");
            assertThat(saved.getAlias()).isEqualTo("New Test Account");

            // Verify persisted
            Optional<AccountEntity> persisted = accountJpaRepository.findById(saved.getAccountId());
            assertThat(persisted).isPresent();
        }

        @Test
        @DisplayName("Should update existing account when ID exists")
        void save_UpdateExisting() {
            Account existingAccount = accountRepositoryAdapter.findById(testAccountId).orElseThrow();

            Account updatedAccount = Account.builder()
                    .accountId(existingAccount.getAccountId())
                    .broker(existingAccount.getBroker())
                    .environment(existingAccount.getEnvironment())
                    .cano(existingAccount.getCano())
                    .acntPrdtCd(existingAccount.getAcntPrdtCd())
                    .status(AccountStatus.INACTIVE)
                    .alias("Updated Alias")
                    .build();

            Account saved = accountRepositoryAdapter.save(updatedAccount);

            assertThat(saved.getAccountId()).isEqualTo(testAccountId);
            assertThat(saved.getStatus()).isEqualTo(AccountStatus.INACTIVE);
            assertThat(saved.getAlias()).isEqualTo("Updated Alias");
        }

        @Test
        @DisplayName("Should create new account when ID provided but not found")
        void save_CreateWithProvidedId() {
            String providedId = UlidGenerator.generate();

            Account newAccount = Account.builder()
                    .accountId(providedId)
                    .broker("KIS")
                    .environment(Environment.LIVE)
                    .cano("99999999")
                    .acntPrdtCd("02")
                    .status(AccountStatus.ACTIVE)
                    .alias("Account with provided ID")
                    .build();

            Account saved = accountRepositoryAdapter.save(newAccount);

            assertThat(saved.getAccountId()).isEqualTo(providedId);
            assertThat(saved.getEnvironment()).isEqualTo(Environment.LIVE);
        }
    }

    @Nested
    @DisplayName("Domain Mapping")
    class DomainMapping {

        @Test
        @DisplayName("Should correctly map all fields from entity to domain")
        void shouldMapAllFields() {
            Optional<Account> result = accountRepositoryAdapter.findById(testAccountId);

            assertThat(result).isPresent();
            Account account = result.get();

            assertThat(account.getAccountId()).isEqualTo(testAccountId);
            assertThat(account.getBroker()).isEqualTo("KIS");
            assertThat(account.getEnvironment()).isEqualTo(Environment.PAPER);
            assertThat(account.getCano()).isEqualTo("50123456");
            assertThat(account.getAcntPrdtCd()).isEqualTo("01");
            assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(account.getAlias()).isEqualTo("Test Account");
            assertThat(account.getDelyn()).isEqualTo("N");
        }

        @Test
        @DisplayName("Should correctly identify active account")
        void shouldIdentifyActiveAccount() {
            Optional<Account> result = accountRepositoryAdapter.findById(testAccountId);

            assertThat(result).isPresent();
            assertThat(result.get().isActive()).isTrue();
            assertThat(result.get().isPaper()).isTrue();
            assertThat(result.get().isDeleted()).isFalse();
        }
    }

    // ==================== Helper Methods ====================

    private void createTestAccount(String accountId) {
        AccountEntity entity = AccountEntity.builder()
                .accountId(accountId)
                .broker("KIS")
                .environment(Environment.PAPER)
                .cano("50123456")
                .acntPrdtCd("01")
                .status(AccountStatus.ACTIVE)
                .alias("Test Account")
                .delyn("N")
                .build();
        accountJpaRepository.save(entity);
    }
}
