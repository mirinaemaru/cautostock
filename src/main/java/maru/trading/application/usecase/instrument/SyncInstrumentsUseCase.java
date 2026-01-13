package maru.trading.application.usecase.instrument;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.broker.kis.api.KrxInstrumentClient;
import maru.trading.broker.kis.api.KrxInstrumentClient.KrxInstrument;
import maru.trading.infra.persistence.jpa.entity.InstrumentEntity;
import maru.trading.infra.persistence.jpa.repository.InstrumentJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 종목 동기화 UseCase.
 *
 * KRX에서 전체 상장 종목 정보를 가져와 데이터베이스에 동기화합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncInstrumentsUseCase {

    private final KrxInstrumentClient krxClient;
    private final InstrumentJpaRepository instrumentRepository;

    /**
     * 전체 종목 동기화 (KOSPI + KOSDAQ)
     *
     * @return 동기화 결과 (신규/업데이트/총합)
     */
    @Transactional
    public SyncResult syncAll() {
        log.info("Starting full instrument sync...");

        List<KrxInstrument> instruments = krxClient.fetchAllInstruments();
        return processInstruments(instruments);
    }

    /**
     * KOSPI 종목만 동기화
     */
    @Transactional
    public SyncResult syncKospi() {
        log.info("Starting KOSPI instrument sync...");

        // KRX API 시도, 실패하면 네이버 API 시도
        List<KrxInstrument> instruments = krxClient.fetchKospiInstruments();
        if (instruments.isEmpty()) {
            log.info("KRX API failed, trying Naver API for KOSPI...");
            instruments = krxClient.fetchInstrumentsFromNaver("KOSPI");
        }
        return processInstruments(instruments);
    }

    /**
     * KOSDAQ 종목만 동기화
     */
    @Transactional
    public SyncResult syncKosdaq() {
        log.info("Starting KOSDAQ instrument sync...");

        // KRX API 시도, 실패하면 네이버 API 시도
        List<KrxInstrument> instruments = krxClient.fetchKosdaqInstruments();
        if (instruments.isEmpty()) {
            log.info("KRX API failed, trying Naver API for KOSDAQ...");
            instruments = krxClient.fetchInstrumentsFromNaver("KOSDAQ");
        }
        return processInstruments(instruments);
    }

    /**
     * 특정 시장 동기화
     */
    @Transactional
    public SyncResult syncByMarkets(List<String> markets) {
        log.info("Starting instrument sync for markets: {}", markets);

        int inserted = 0;
        int updated = 0;
        int total = 0;

        for (String market : markets) {
            SyncResult result;
            if ("KOSPI".equalsIgnoreCase(market)) {
                result = syncKospi();
            } else if ("KOSDAQ".equalsIgnoreCase(market)) {
                result = syncKosdaq();
            } else {
                log.warn("Unknown market: {}", market);
                continue;
            }
            inserted += result.inserted;
            updated += result.updated;
            total += result.total;
        }

        return new SyncResult(inserted, updated, total);
    }

    /**
     * 종목 목록 처리 (신규 추가 또는 업데이트)
     */
    private SyncResult processInstruments(List<KrxInstrument> instruments) {
        int inserted = 0;
        int updated = 0;

        for (KrxInstrument krxInstrument : instruments) {
            try {
                Optional<InstrumentEntity> existing = instrumentRepository.findById(krxInstrument.getSymbol());

                if (existing.isPresent()) {
                    // 기존 종목 업데이트
                    updateInstrument(existing.get(), krxInstrument);
                    updated++;
                } else {
                    // 신규 종목 추가
                    insertInstrument(krxInstrument);
                    inserted++;
                }
            } catch (Exception e) {
                log.warn("Failed to process instrument: {}", krxInstrument.getSymbol(), e);
            }
        }

        log.info("Instrument sync completed: inserted={}, updated={}, total={}",
                inserted, updated, instruments.size());

        return new SyncResult(inserted, updated, instruments.size());
    }

    /**
     * 신규 종목 추가
     */
    private void insertInstrument(KrxInstrument krx) {
        InstrumentEntity entity = InstrumentEntity.builder()
                .symbol(krx.getSymbol())
                .market(krx.getMarket())
                .nameKr(krx.getNameKr())
                .nameEn(null) // KRX에서 영문명 미제공
                .sectorCode(krx.getSectorCode())
                .industry(null)
                .tickSize(getDefaultTickSize(krx.getMarket()))
                .lotSize(1)
                .status("LISTED")
                .tradable(true)
                .halted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        instrumentRepository.save(entity);
        log.debug("Inserted instrument: {} - {}", krx.getSymbol(), krx.getNameKr());
    }

    /**
     * 기존 종목 업데이트
     */
    private void updateInstrument(InstrumentEntity entity, KrxInstrument krx) {
        // 종목명이 변경된 경우에만 업데이트
        if (!krx.getNameKr().equals(entity.getNameKr())) {
            entity.updateName(krx.getNameKr(), entity.getNameEn());
            log.debug("Updated instrument name: {} - {}", krx.getSymbol(), krx.getNameKr());
        }

        // 시장이 변경된 경우 (이전 케이스 - 코스닥→코스피 등)
        if (!krx.getMarket().equals(entity.getMarket())) {
            entity.updateMarket(krx.getMarket());
            log.debug("Updated instrument market: {} - {}", krx.getSymbol(), krx.getMarket());
        }
    }

    /**
     * 시장별 기본 호가 단위
     */
    private BigDecimal getDefaultTickSize(String market) {
        // 실제로는 가격대별로 다르지만, 기본값으로 1원 설정
        return BigDecimal.ONE;
    }

    /**
     * 동기화 결과 DTO
     */
    public record SyncResult(int inserted, int updated, int total) {
        public String toMessage() {
            return String.format("Synced %d instruments (new: %d, updated: %d)", total, inserted, updated);
        }
    }
}
