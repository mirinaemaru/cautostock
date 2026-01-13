package maru.trading.application.ports.repo;

import maru.trading.domain.market.Instrument;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Port for instrument repository.
 * Domain-agnostic interface for instrument persistence.
 */
public interface InstrumentRepository {

    /**
     * Save or update an instrument.
     */
    Instrument save(Instrument instrument);

    /**
     * Bulk save/update instruments.
     * Optimized for batch operations during sync.
     */
    List<Instrument> saveAll(List<Instrument> instruments);

    /**
     * Find instrument by symbol.
     */
    Optional<Instrument> findBySymbol(String symbol);

    /**
     * Find all instruments by market (KOSPI/KOSDAQ).
     */
    List<Instrument> findByMarket(String market);

    /**
     * Find all tradable instruments.
     */
    List<Instrument> findTradable();

    /**
     * Find tradable instruments by market.
     */
    List<Instrument> findTradableByMarket(String market);

    /**
     * Find instruments by status.
     */
    List<Instrument> findByStatus(String status);

    /**
     * Find all instruments.
     */
    List<Instrument> findAll();

    /**
     * Find instruments updated after a specific time.
     */
    List<Instrument> findUpdatedSince(LocalDateTime since);

    /**
     * Search instruments by name.
     */
    List<Instrument> searchByName(String keyword);
}
