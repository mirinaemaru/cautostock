package maru.trading.infra.persistence.jpa.repository.impl;

import maru.trading.application.ports.repo.FillRepository;
import maru.trading.domain.execution.Fill;
import maru.trading.domain.order.Side;
import maru.trading.infra.persistence.jpa.entity.FillEntity;
import maru.trading.infra.persistence.jpa.repository.FillJpaRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of FillRepository port.
 * Maps between Fill domain model and FillEntity.
 */
@Component
public class FillRepositoryImpl implements FillRepository {

    private final FillJpaRepository jpaRepository;

    public FillRepositoryImpl(FillJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Fill save(Fill fill) {
        FillEntity entity = toEntity(fill);
        FillEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Fill> findById(String fillId) {
        return jpaRepository.findById(fillId)
                .map(this::toDomain);
    }

    @Override
    public List<Fill> findByOrderId(String orderId) {
        return jpaRepository.findByOrderId(orderId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Fill> findByAccountAndSymbol(String accountId, String symbol, LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findByAccountIdAndSymbolAndFillTsBetween(accountId, symbol, from, to).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Fill> findByAccount(String accountId, LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findByAccountIdAndFillTsBetween(accountId, from, to).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByOrderIdAndDetails(String orderId, LocalDateTime fillTimestamp, BigDecimal fillPrice, int fillQty) {
        return jpaRepository.existsByOrderIdAndFillTsAndFillPriceAndFillQty(
                orderId, fillTimestamp, fillPrice, BigDecimal.valueOf(fillQty));
    }

    @Override
    public List<Fill> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // Mapping methods
    private FillEntity toEntity(Fill domain) {
        return FillEntity.builder()
                .fillId(domain.getFillId())
                .orderId(domain.getOrderId())
                .accountId(domain.getAccountId())
                .symbol(domain.getSymbol())
                .side(domain.getSide())
                .fillPrice(domain.getFillPrice())
                .fillQty(BigDecimal.valueOf(domain.getFillQty()))
                .fee(domain.getFee())
                .tax(domain.getTax())
                .fillTs(domain.getFillTimestamp())
                .brokerOrderNo(domain.getBrokerOrderNo())
                .build();
    }

    private Fill toDomain(FillEntity entity) {
        return new Fill(
                entity.getFillId(),
                entity.getOrderId(),
                entity.getAccountId(),
                entity.getSymbol(),
                entity.getSide(),
                entity.getFillPrice(),
                entity.getFillQty().intValue(),
                entity.getFee(),
                entity.getTax(),
                entity.getFillTs(),
                entity.getBrokerOrderNo()
        );
    }
}
