package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.FillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
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

	boolean existsByOrderIdAndFillTsAndFillPriceAndFillQty(
			String orderId,
			LocalDateTime fillTs,
			java.math.BigDecimal fillPrice,
			java.math.BigDecimal fillQty
	);
}
