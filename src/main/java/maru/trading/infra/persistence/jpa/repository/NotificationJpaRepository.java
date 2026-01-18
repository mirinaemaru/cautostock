package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, String> {

    List<NotificationEntity> findByAccountIdOrderByCreatedAtDesc(String accountId);

    Page<NotificationEntity> findByAccountIdOrderByCreatedAtDesc(String accountId, Pageable pageable);

    List<NotificationEntity> findByAccountIdAndIsReadOrderByCreatedAtDesc(String accountId, String isRead);

    @Query("SELECT n FROM NotificationEntity n WHERE n.accountId = :accountId AND n.isRead = 'N' ORDER BY n.createdAt DESC")
    List<NotificationEntity> findUnreadByAccountId(@Param("accountId") String accountId);

    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.accountId = :accountId AND n.isRead = 'N'")
    long countUnreadByAccountId(@Param("accountId") String accountId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = 'Y', n.readAt = :readAt WHERE n.accountId = :accountId AND n.isRead = 'N'")
    int markAllAsReadByAccountId(@Param("accountId") String accountId, @Param("readAt") LocalDateTime readAt);

    List<NotificationEntity> findByNotificationTypeAndCreatedAtAfterOrderByCreatedAtDesc(
            String notificationType, LocalDateTime after);

    @Query("SELECT n FROM NotificationEntity n WHERE n.accountId IS NULL ORDER BY n.createdAt DESC")
    List<NotificationEntity> findGlobalNotifications();

    void deleteByAccountIdAndCreatedAtBefore(String accountId, LocalDateTime before);
}
