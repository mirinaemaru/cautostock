package maru.trading.infra.persistence.jpa.repository;

import maru.trading.domain.order.OrderStatus;
import maru.trading.infra.persistence.jpa.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, String> {

	Optional<OrderEntity> findByIdempotencyKey(String idempotencyKey);

	Optional<OrderEntity> findByBrokerOrderNo(String brokerOrderNo);

	List<OrderEntity> findByAccountIdAndCreatedAtBetween(
			String accountId,
			LocalDateTime from,
			LocalDateTime to
	);

	List<OrderEntity> findBySymbolAndCreatedAtBetween(
			String symbol,
			LocalDateTime from,
			LocalDateTime to
	);

	List<OrderEntity> findByAccountIdAndStatus(String accountId, OrderStatus status);

	@Query("SELECT o FROM OrderEntity o WHERE o.accountId = :accountId " +
			"AND o.status IN ('NEW', 'SENT', 'ACCEPTED', 'PART_FILLED')")
	List<OrderEntity> findOpenOrdersByAccountId(@Param("accountId") String accountId);

	@Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.accountId = :accountId " +
			"AND o.status IN ('NEW', 'SENT', 'ACCEPTED', 'PART_FILLED')")
	long countOpenOrdersByAccountId(@Param("accountId") String accountId);

	@Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.createdAt >= :from AND o.createdAt < :to")
	long countByCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	@Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.accountId = :accountId " +
			"AND o.createdAt >= :from AND o.createdAt < :to")
	long countByAccountIdAndCreatedAtBetween(
			@Param("accountId") String accountId,
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);

	List<OrderEntity> findTop20ByOrderByCreatedAtDesc();

	@Query("SELECT o FROM OrderEntity o WHERE o.accountId = :accountId ORDER BY o.createdAt DESC")
	List<OrderEntity> findRecentByAccountId(@Param("accountId") String accountId, org.springframework.data.domain.Pageable pageable);
}
