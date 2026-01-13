package maru.trading.infra.persistence.jpa.repository.impl;

import maru.trading.application.ports.repo.OrderRepository;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.OrderStatus;
import maru.trading.infra.persistence.jpa.entity.OrderEntity;
import maru.trading.infra.persistence.jpa.repository.OrderJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of OrderRepository port.
 * Maps between Order domain model and OrderEntity.
 */
@Component
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    public OrderRepositoryImpl(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = toEntity(order);
        OrderEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Order> findById(String orderId) {
        return jpaRepository.findById(orderId)
                .map(this::toDomain);
    }

    @Override
    public Optional<Order> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey)
                .map(this::toDomain);
    }

    @Override
    public List<Order> findOpenOrdersByAccountId(String accountId) {
        return jpaRepository.findOpenOrdersByAccountId(accountId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countOpenOrdersByAccountId(String accountId) {
        return jpaRepository.countOpenOrdersByAccountId(accountId);
    }

    @Override
    public void updateStatus(String orderId, OrderStatus newStatus) {
        jpaRepository.findById(orderId).ifPresent(entity -> {
            entity.updateStatus(newStatus);
            jpaRepository.save(entity);
        });
    }

    @Override
    public void updateBrokerOrderNo(String orderId, String brokerOrderNo) {
        jpaRepository.findById(orderId).ifPresent(entity -> {
            entity.updateBrokerOrderNo(brokerOrderNo);
            jpaRepository.save(entity);
        });
    }

    // Mapping methods
    private OrderEntity toEntity(Order domain) {
        return OrderEntity.builder()
                .orderId(domain.getOrderId())
                .accountId(domain.getAccountId())
                .strategyId(domain.getStrategyId())
                .strategyVersionId(domain.getStrategyVersionId())
                .signalId(domain.getSignalId())
                .symbol(domain.getSymbol())
                .side(domain.getSide())
                .orderType(domain.getOrderType())
                .ordDvsn(domain.getOrdDvsn())
                .qty(domain.getQty())
                .price(domain.getPrice())
                .status(domain.getStatus())
                .idempotencyKey(domain.getIdempotencyKey())
                .brokerOrderNo(domain.getBrokerOrderNo())
                .build();
    }

    private Order toDomain(OrderEntity entity) {
        return Order.builder()
                .orderId(entity.getOrderId())
                .accountId(entity.getAccountId())
                .strategyId(entity.getStrategyId())
                .strategyVersionId(entity.getStrategyVersionId())
                .signalId(entity.getSignalId())
                .symbol(entity.getSymbol())
                .side(entity.getSide())
                .orderType(entity.getOrderType())
                .ordDvsn(entity.getOrdDvsn())
                .qty(entity.getQty())
                .price(entity.getPrice())
                .status(entity.getStatus())
                .idempotencyKey(entity.getIdempotencyKey())
                .brokerOrderNo(entity.getBrokerOrderNo())
                .build();
    }
}
