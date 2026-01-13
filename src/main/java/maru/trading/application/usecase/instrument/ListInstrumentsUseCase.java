package maru.trading.application.usecase.instrument;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.application.ports.repo.InstrumentRepository;
import maru.trading.domain.market.Instrument;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for listing instruments with optional filters.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListInstrumentsUseCase {

    private final InstrumentRepository instrumentRepository;

    /**
     * List all instruments.
     */
    public List<Instrument> execute() {
        return instrumentRepository.findAll();
    }

    /**
     * List instruments by market.
     */
    public List<Instrument> executeByMarket(String market) {
        log.debug("Listing instruments for market: {}", market);
        return instrumentRepository.findByMarket(market);
    }

    /**
     * List tradable instruments.
     */
    public List<Instrument> executeTradableOnly() {
        log.debug("Listing tradable instruments");
        return instrumentRepository.findTradable();
    }

    /**
     * List instruments by status.
     */
    public List<Instrument> executeByStatus(String status) {
        log.debug("Listing instruments with status: {}", status);
        return instrumentRepository.findByStatus(status);
    }

    /**
     * Search instruments by name.
     */
    public List<Instrument> executeSearch(String keyword) {
        log.debug("Searching instruments with keyword: {}", keyword);
        return instrumentRepository.searchByName(keyword);
    }
}
