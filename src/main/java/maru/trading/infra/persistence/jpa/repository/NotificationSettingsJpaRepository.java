package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.NotificationSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationSettingsJpaRepository extends JpaRepository<NotificationSettingsEntity, String> {

    Optional<NotificationSettingsEntity> findByAccountId(String accountId);

    @Query("SELECT s FROM NotificationSettingsEntity s WHERE s.accountId IS NULL")
    Optional<NotificationSettingsEntity> findGlobalSettings();

    boolean existsByAccountId(String accountId);
}
