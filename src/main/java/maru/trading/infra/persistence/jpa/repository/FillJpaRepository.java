package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.FillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FillJpaRepository extends JpaRepository<FillEntity, String> {

	List<FillEntity> findByOrderIdOrderByFillTsAsc(String orderId);

	List<FillEntity> findByOrderId(String orderId);

	List<FillEntity> findByAccountIdAndSymbolAndFillTsBetween(
			String accountId,
			String symbol,
			LocalDateTime from,
			LocalDateTime to
	);

	List<FillEntity> findByAccountIdAndFillTsBetween(
			String accountId,
			LocalDateTime from,
			LocalDateTime to
	);

	List<FillEntity> findByFillTsBetween(LocalDateTime from, LocalDateTime to);

	boolean existsByOrderIdAndFillTsAndFillPriceAndFillQty(
			String orderId,
			LocalDateTime fillTs,
			java.math.BigDecimal fillPrice,
			java.math.BigDecimal fillQty
	);

	@Query("SELECT COUNT(f) FROM FillEntity f WHERE f.fillTs >= :from AND f.fillTs < :to")
	long countByFillTsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	@Query("SELECT COUNT(f) FROM FillEntity f WHERE f.accountId = :accountId " +
			"AND f.fillTs >= :from AND f.fillTs < :to")
	long countByAccountIdAndFillTsBetween(
			@Param("accountId") String accountId,
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);

	List<FillEntity> findTop20ByOrderByFillTsDesc();

	@Query("SELECT f FROM FillEntity f WHERE f.accountId = :accountId ORDER BY f.fillTs DESC")
	List<FillEntity> findRecentByAccountId(@Param("accountId") String accountId, org.springframework.data.domain.Pageable pageable);
}
