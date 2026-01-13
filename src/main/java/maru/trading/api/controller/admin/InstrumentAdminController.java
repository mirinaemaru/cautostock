package maru.trading.api.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.api.dto.request.InstrumentSyncRequest;
import maru.trading.api.dto.request.UpdateInstrumentStatusRequest;
import maru.trading.api.dto.response.AckResponse;
import maru.trading.api.dto.response.InstrumentListResponse;
import maru.trading.api.dto.response.InstrumentResponse;
import maru.trading.application.usecase.instrument.GetInstrumentUseCase;
import maru.trading.application.usecase.instrument.ListInstrumentsUseCase;
import maru.trading.application.usecase.instrument.SyncInstrumentsUseCase;
import maru.trading.application.usecase.instrument.UpdateInstrumentStatusUseCase;
import maru.trading.infra.persistence.jpa.entity.InstrumentEntity;
import maru.trading.infra.persistence.jpa.repository.InstrumentJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin controller for instrument management.
 *
 * Endpoints:
 * - POST /api/v1/admin/instruments/sync - Manual sync trigger
 * - GET /api/v1/admin/instruments - List instruments with filters
 * - GET /api/v1/admin/instruments/{symbol} - Get instrument details
 * - PUT /api/v1/admin/instruments/{symbol}/status - Update status
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/instruments")
@RequiredArgsConstructor
public class InstrumentAdminController {

    private final GetInstrumentUseCase getInstrumentUseCase;
    private final ListInstrumentsUseCase listInstrumentsUseCase;
    private final UpdateInstrumentStatusUseCase updateInstrumentStatusUseCase;
    private final SyncInstrumentsUseCase syncInstrumentsUseCase;
    private final InstrumentJpaRepository instrumentJpaRepository;

    /**
     * Trigger manual instrument sync.
     *
     * POST /api/v1/admin/instruments/sync
     * Body: {"markets": ["KOSPI", "KOSDAQ"]}
     *
     * Syncs instruments from KRX (Korea Exchange) API.
     */
    @PostMapping("/sync")
    public ResponseEntity<AckResponse> triggerSync(
            @Valid @RequestBody InstrumentSyncRequest request) {

        log.info("Manual instrument sync triggered for markets: {}", request.getMarkets());

        try {
            SyncInstrumentsUseCase.SyncResult result;

            if (request.getMarkets() == null || request.getMarkets().isEmpty()) {
                // 전체 시장 동기화
                result = syncInstrumentsUseCase.syncAll();
            } else {
                // 특정 시장만 동기화
                result = syncInstrumentsUseCase.syncByMarkets(request.getMarkets());
            }

            log.info("Instrument sync completed: {}", result.toMessage());

            return ResponseEntity.ok(AckResponse.builder()
                    .ok(true)
                    .message(result.toMessage())
                    .build());

        } catch (Exception e) {
            log.error("Instrument sync failed", e);
            return ResponseEntity.internalServerError()
                    .body(AckResponse.builder()
                            .ok(false)
                            .message("Sync failed: " + e.getMessage())
                            .build());
        }
    }

    /**
     * List instruments with optional filters.
     *
     * GET /api/v1/admin/instruments?market=KOSPI&status=LISTED&tradable=true&search=삼성
     */
    @GetMapping
    public ResponseEntity<InstrumentListResponse> listInstruments(
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean tradable,
            @RequestParam(required = false) String search) {

        log.debug("List instruments: market={}, status={}, tradable={}, search={}",
                market, status, tradable, search);

        List<InstrumentEntity> instruments;

        // Apply filters
        if (search != null && !search.isBlank()) {
            instruments = instrumentJpaRepository.searchByNameKr(search);
        } else if (market != null && tradable != null && tradable) {
            instruments = instrumentJpaRepository.findByMarketAndTradableTrue(market);
        } else if (market != null) {
            instruments = instrumentJpaRepository.findByMarket(market);
        } else if (status != null) {
            instruments = instrumentJpaRepository.findByStatus(status);
        } else if (tradable != null && tradable) {
            instruments = instrumentJpaRepository.findByTradableTrue();
        } else {
            instruments = instrumentJpaRepository.findAll();
        }

        List<InstrumentResponse> items = instruments.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        InstrumentListResponse response = InstrumentListResponse.builder()
                .items(items)
                .total(items.size())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get single instrument by symbol.
     *
     * GET /api/v1/admin/instruments/005930
     */
    @GetMapping("/{symbol}")
    public ResponseEntity<InstrumentResponse> getInstrument(@PathVariable String symbol) {
        log.debug("Get instrument: {}", symbol);

        InstrumentEntity instrument = instrumentJpaRepository.findById(symbol)
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found: " + symbol));

        return ResponseEntity.ok(toResponse(instrument));
    }

    /**
     * Update instrument status.
     *
     * PUT /api/v1/admin/instruments/005930/status
     * Body: {"status": "SUSPENDED", "tradable": false, "halted": true}
     */
    @PutMapping("/{symbol}/status")
    public ResponseEntity<InstrumentResponse> updateStatus(
            @PathVariable String symbol,
            @Valid @RequestBody UpdateInstrumentStatusRequest request) {

        log.info("Update instrument status: symbol={}, status={}, tradable={}, halted={}",
                symbol, request.getStatus(), request.getTradable(), request.getHalted());

        updateInstrumentStatusUseCase.execute(
                symbol,
                request.getStatus(),
                request.getTradable(),
                request.getHalted()
        );

        InstrumentEntity updated = instrumentJpaRepository.findById(symbol)
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found: " + symbol));

        return ResponseEntity.ok(toResponse(updated));
    }

    /**
     * Convert entity to response DTO.
     */
    private InstrumentResponse toResponse(InstrumentEntity entity) {
        return InstrumentResponse.builder()
                .symbol(entity.getSymbol())
                .market(entity.getMarket())
                .nameKr(entity.getNameKr())
                .nameEn(entity.getNameEn())
                .sectorCode(entity.getSectorCode())
                .industry(entity.getIndustry())
                .tickSize(entity.getTickSize())
                .lotSize(entity.getLotSize())
                .listingDate(entity.getListingDate())
                .delistingDate(entity.getDelistingDate())
                .status(entity.getStatus())
                .tradable(entity.getTradable())
                .halted(entity.getHalted())
                .updatedAt(entity.getUpdatedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
