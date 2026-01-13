package maru.trading.infra.persistence.adapter;

import maru.trading.application.ports.repo.InstrumentRepository;
import maru.trading.domain.market.Instrument;
import maru.trading.infra.persistence.jpa.entity.InstrumentEntity;
import maru.trading.infra.persistence.jpa.repository.InstrumentJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementation for InstrumentRepository.
 * Bridges domain Instrument and InstrumentEntity.
 *
 * Note: The current Instrument domain model is minimal (6 fields).
 * InstrumentEntity contains more fields (name, sector, dates).
 * This adapter maps between the two with appropriate null handling.
 */
@Component
public class InstrumentRepositoryAdapter implements InstrumentRepository {

    private final InstrumentJpaRepository jpaRepository;

    public InstrumentRepositoryAdapter(InstrumentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Instrument save(Instrument instrument) {
        // Check if entity already exists to preserve extra fields
        Optional<InstrumentEntity> existing = jpaRepository.findById(instrument.getSymbol());

        InstrumentEntity entity;
        if (existing.isPresent()) {
            // Update existing entity while preserving rich fields
            entity = existing.get();
            updateEntityFromDomain(entity, instrument);
        } else {
            // Create new entity with minimal domain fields
            entity = toEntity(instrument);
        }

        InstrumentEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<Instrument> saveAll(List<Instrument> instruments) {
        List<InstrumentEntity> entities = instruments.stream()
                .map(instrument -> {
                    Optional<InstrumentEntity> existing = jpaRepository.findById(instrument.getSymbol());
                    if (existing.isPresent()) {
                        InstrumentEntity entity = existing.get();
                        updateEntityFromDomain(entity, instrument);
                        return entity;
                    } else {
                        return toEntity(instrument);
                    }
                })
                .collect(Collectors.toList());

        List<InstrumentEntity> saved = jpaRepository.saveAll(entities);

        return saved.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Instrument> findBySymbol(String symbol) {
        return jpaRepository.findById(symbol)
                .map(this::toDomain);
    }

    @Override
    public List<Instrument> findByMarket(String market) {
        return jpaRepository.findByMarket(market).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Instrument> findTradable() {
        return jpaRepository.findByTradableTrue().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Instrument> findTradableByMarket(String market) {
        return jpaRepository.findByMarketAndTradableTrue(market).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Instrument> findByStatus(String status) {
        return jpaRepository.findByStatus(status).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Instrument> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Instrument> findUpdatedSince(LocalDateTime since) {
        return jpaRepository.findByUpdatedAtAfter(since).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Instrument> searchByName(String keyword) {
        return jpaRepository.searchByNameKr(keyword).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Convert domain Instrument to InstrumentEntity (for new entities).
     * Note: Rich fields (nameKr, sectorCode, etc.) are null.
     * These should be populated by SyncInstrumentsUseCase from KIS API.
     */
    private InstrumentEntity toEntity(Instrument domain) {
        return InstrumentEntity.builder()
                .symbol(domain.getSymbol())
                .market(domain.getMarket())
                .nameKr(null) // Not in current Instrument domain model
                .nameEn(null) // Not in current Instrument domain model
                .sectorCode(null) // Not in current Instrument domain model
                .industry(null) // Not in current Instrument domain model
                .tickSize(domain.getTickSize())
                .lotSize(domain.getLotSize())
                .listingDate(null) // Not in current Instrument domain model
                .delistingDate(null) // Not in current Instrument domain model
                .status("LISTED") // Derived from tradable/halted
                .tradable(domain.isTradable())
                .halted(domain.isHalted())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Update existing entity with domain values.
     * Preserves rich fields (name, sector, dates).
     */
    private void updateEntityFromDomain(InstrumentEntity entity, Instrument domain) {
        // Update only fields present in domain model
        entity.updateStatus(
                deriveStatus(domain.isTradable(), domain.isHalted()),
                domain.isTradable(),
                domain.isHalted()
        );
        // Note: tickSize, lotSize, market are typically not updated after creation
        // If needed, add setters to InstrumentEntity
    }

    /**
     * Convert InstrumentEntity to domain Instrument.
     */
    private Instrument toDomain(InstrumentEntity entity) {
        return new Instrument(
                entity.getSymbol(),
                entity.getMarket(),
                entity.getTradable(),
                entity.getHalted(),
                entity.getTickSize(),
                entity.getLotSize()
        );
    }

    /**
     * Derive status string from tradable/halted flags.
     */
    private String deriveStatus(boolean tradable, boolean halted) {
        if (!tradable) {
            return "DELISTED";
        } else if (halted) {
            return "SUSPENDED";
        } else {
            return "LISTED";
        }
    }
}
