package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.InstrumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA Repository for instruments table.
 */
@Repository
public interface InstrumentJpaRepository extends JpaRepository<InstrumentEntity, String> {

    /**
     * Find all instruments by market type.
     */
    List<InstrumentEntity> findByMarket(String market);

    /**
     * Find all instruments by status.
     */
    List<InstrumentEntity> findByStatus(String status);

    /**
     * Find all tradable instruments.
     */
    List<InstrumentEntity> findByTradableTrue();

    /**
     * Find tradable instruments by market.
     */
    List<InstrumentEntity> findByMarketAndTradableTrue(String market);

    /**
     * Find instruments updated after a specific time.
     * Useful for incremental sync.
     */
    List<InstrumentEntity> findByUpdatedAtAfter(LocalDateTime since);

    /**
     * Search instruments by Korean name (partial match).
     */
    @Query("SELECT i FROM InstrumentEntity i WHERE i.nameKr LIKE %:keyword%")
    List<InstrumentEntity> searchByNameKr(@Param("keyword") String keyword);

    /**
     * Find symbols by sector.
     */
    List<InstrumentEntity> findBySectorCode(String sectorCode);
}
