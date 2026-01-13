package maru.trading.infra.persistence.adapter;

import maru.trading.application.ports.repo.BarRepository;
import maru.trading.domain.market.MarketBar;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.BarEntity;
import maru.trading.infra.persistence.jpa.repository.BarJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementation for BarRepository.
 * Converts between domain model (MarketBar) and JPA entity (BarEntity).
 */
@Component
public class BarRepositoryAdapter implements BarRepository {

    private final BarJpaRepository barJpaRepository;
    private final UlidGenerator ulidGenerator;

    public BarRepositoryAdapter(BarJpaRepository barJpaRepository, UlidGenerator ulidGenerator) {
        this.barJpaRepository = barJpaRepository;
        this.ulidGenerator = ulidGenerator;
    }

    @Override
    public MarketBar save(MarketBar bar) {
        if (bar == null) {
            throw new IllegalArgumentException("Bar cannot be null");
        }

        bar.validate();

        BarEntity entity = BarEntity.builder()
                .barId(ulidGenerator.generateInstance())
                .symbol(bar.getSymbol())
                .timeframe(bar.getTimeframe())
                .openPrice(bar.getOpen())
                .highPrice(bar.getHigh())
                .lowPrice(bar.getLow())
                .closePrice(bar.getClose())
                .volume(bar.getVolume())
                .barTimestamp(bar.getBarTimestamp())
                .closed(bar.isClosed())
                .build();

        BarEntity savedEntity = barJpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<MarketBar> findBySymbolAndTimeframeAndTimestamp(
            String symbol, String timeframe, LocalDateTime barTimestamp) {

        return barJpaRepository.findBySymbolAndTimeframeAndBarTimestamp(symbol, timeframe, barTimestamp)
                .map(this::toDomain);
    }

    @Override
    public List<MarketBar> findRecentClosedBars(String symbol, String timeframe, int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }

        List<BarEntity> entities = barJpaRepository.findRecentClosedBars(symbol, timeframe, count);

        // Reverse to get oldest-first order
        Collections.reverse(entities);

        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<MarketBar> findBarsInRange(
            String symbol, String timeframe, LocalDateTime startTime, LocalDateTime endTime) {

        return barJpaRepository.findBarsInRange(symbol, timeframe, startTime, endTime).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsBySymbolAndTimeframeAndTimestamp(
            String symbol, String timeframe, LocalDateTime barTimestamp) {

        return barJpaRepository.findBySymbolAndTimeframeAndBarTimestamp(symbol, timeframe, barTimestamp)
                .isPresent();
    }

    /**
     * Convert BarEntity to MarketBar domain model.
     */
    private MarketBar toDomain(BarEntity entity) {
        return MarketBar.restore(
                entity.getSymbol(),
                entity.getTimeframe(),
                entity.getBarTimestamp(),
                entity.getOpenPrice(),
                entity.getHighPrice(),
                entity.getLowPrice(),
                entity.getClosePrice(),
                entity.getVolume(),
                entity.getClosed()
        );
    }
}
