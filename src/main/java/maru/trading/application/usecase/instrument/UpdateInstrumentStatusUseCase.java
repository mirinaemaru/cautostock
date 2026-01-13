package maru.trading.application.usecase.instrument;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.infra.persistence.jpa.entity.InstrumentEntity;
import maru.trading.infra.persistence.jpa.repository.InstrumentJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for updating instrument trading status.
 * Used by admin to manually halt/resume trading for specific instruments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateInstrumentStatusUseCase {

    private final InstrumentJpaRepository instrumentRepository;

    /**
     * Update instrument status.
     *
     * @param symbol Symbol to update
     * @param status New status (LISTED, SUSPENDED, etc.)
     * @param tradable Whether instrument is tradable
     * @param halted Whether instrument is halted
     */
    @Transactional
    public void execute(String symbol, String status, Boolean tradable, Boolean halted) {
        log.info("Updating instrument status: symbol={}, status={}, tradable={}, halted={}",
                symbol, status, tradable, halted);

        InstrumentEntity instrument = instrumentRepository.findById(symbol)
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found: " + symbol));

        instrument.updateStatus(status, tradable, halted);
        instrumentRepository.save(instrument);

        log.info("Instrument status updated successfully: {}", symbol);
    }
}
