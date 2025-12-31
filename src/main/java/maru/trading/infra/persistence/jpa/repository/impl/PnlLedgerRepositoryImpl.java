package maru.trading.infra.persistence.jpa.repository.impl;

import maru.trading.application.ports.repo.PnlLedgerRepository;
import maru.trading.domain.execution.PnlLedger;
import maru.trading.infra.persistence.jpa.entity.PnlLedgerEntity;
import maru.trading.infra.persistence.jpa.repository.PnlLedgerJpaRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of PnlLedgerRepository port.
 * Maps between PnlLedger domain model and PnlLedgerEntity.
 */
@Component
public class PnlLedgerRepositoryImpl implements PnlLedgerRepository {

    private final PnlLedgerJpaRepository jpaRepository;

    public PnlLedgerRepositoryImpl(PnlLedgerJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PnlLedger save(PnlLedger ledger) {
        PnlLedgerEntity entity = toEntity(ledger);
        PnlLedgerEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<PnlLedger> saveAll(List<PnlLedger> ledgers) {
        List<PnlLedgerEntity> entities = ledgers.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        List<PnlLedgerEntity> saved = jpaRepository.saveAll(entities);
        return saved.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<PnlLedger> findByAccount(String accountId, LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findByAccountIdAndEventTsBetween(accountId, from, to).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<PnlLedger> findByAccountAndSymbol(String accountId, String symbol, LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findByAccountIdAndSymbolAndEventTsBetween(accountId, symbol, from, to).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal calculateRealizedPnl(String accountId, LocalDateTime from, LocalDateTime to) {
        return jpaRepository.sumAmountByAccountAndDateRange(accountId, from, to);
    }

    @Override
    public BigDecimal calculateRealizedPnlBySymbol(String accountId, String symbol, LocalDateTime from, LocalDateTime to) {
        return jpaRepository.sumAmountByAccountSymbolAndDateRange(accountId, symbol, from, to);
    }

    // Mapping methods
    private PnlLedgerEntity toEntity(PnlLedger domain) {
        PnlLedgerEntity entity = new PnlLedgerEntity();
        entity.setLedgerId(domain.getLedgerId());
        entity.setAccountId(domain.getAccountId());
        entity.setSymbol(domain.getSymbol());
        entity.setEventType(domain.getEventType());
        entity.setAmount(domain.getAmount());
        entity.setRefId(domain.getRefId());
        entity.setEventTs(domain.getEventTimestamp());
        return entity;
    }

    private PnlLedger toDomain(PnlLedgerEntity entity) {
        return new PnlLedger(
                entity.getLedgerId(),
                entity.getAccountId(),
                entity.getSymbol(),
                entity.getEventType(),
                entity.getAmount(),
                entity.getRefId(),
                entity.getEventTs()
        );
    }
}
