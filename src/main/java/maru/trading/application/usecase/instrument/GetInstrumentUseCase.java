package maru.trading.application.usecase.instrument;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.application.ports.repo.InstrumentRepository;
import maru.trading.domain.market.Instrument;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case for retrieving a single instrument by symbol.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetInstrumentUseCase {

    private final InstrumentRepository instrumentRepository;

    /**
     * Get instrument by symbol.
     *
     * @param symbol 6-digit stock code (e.g., "005930")
     * @return Optional instrument
     */
    public Optional<Instrument> execute(String symbol) {
        log.debug("Getting instrument: {}", symbol);
        return instrumentRepository.findBySymbol(symbol);
    }
}
