package maru.trading.infra.scheduler;

import lombok.extern.slf4j.Slf4j;
import maru.trading.application.usecase.instrument.SyncInstrumentsUseCase;
import maru.trading.application.usecase.instrument.SyncInstrumentsUseCase.SyncResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 종목 동기화 스케줄러.
 *
 * 매일 지정된 시간에 KRX/네이버에서 전체 상장 종목 정보를 가져와
 * 데이터베이스에 동기화합니다.
 *
 * 설정:
 * - trading.instrument.sync.enabled: 스케줄러 활성화 여부
 * - trading.instrument.sync.cron: 실행 주기 (cron 표현식)
 * - trading.instrument.sync.markets: 동기화할 시장 (KOSPI,KOSDAQ)
 *
 * 권장 실행 시간: 새벽 2~3시 (장 마감 후, 서버 부하 적은 시간)
 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "trading.instrument.sync",
        name = "enabled",
        havingValue = "true"
)
public class InstrumentSyncScheduler {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SyncInstrumentsUseCase syncInstrumentsUseCase;
    private final List<String> markets;

    public InstrumentSyncScheduler(
            SyncInstrumentsUseCase syncInstrumentsUseCase,
            @Value("${trading.instrument.sync.markets:KOSPI,KOSDAQ}") String marketsConfig) {
        this.syncInstrumentsUseCase = syncInstrumentsUseCase;
        this.markets = Arrays.asList(marketsConfig.split(","));

        log.info("InstrumentSyncScheduler initialized - markets: {}", markets);
    }

    /**
     * 종목 동기화 실행.
     *
     * cron 표현식은 application.yml에서 설정:
     * - "0 0 2 * * *" : 매일 새벽 2시
     * - "0 0 2 * * MON-FRI" : 평일만 새벽 2시
     * - "0 30 8 * * MON-FRI" : 평일 오전 8시 30분 (장 시작 30분 전)
     */
    @Scheduled(cron = "${trading.instrument.sync.cron:0 0 2 * * *}")
    public void syncInstruments() {
        String startTime = LocalDateTime.now().format(DATE_FORMAT);
        log.info("========== Instrument Sync Started at {} ==========", startTime);

        try {
            long startMs = System.currentTimeMillis();

            // 종목 동기화 실행
            SyncResult result = syncInstrumentsUseCase.syncByMarkets(markets);

            long elapsedMs = System.currentTimeMillis() - startMs;

            log.info("========== Instrument Sync Completed ==========");
            log.info("  Result: {}", result.toMessage());
            log.info("  Markets: {}", markets);
            log.info("  Duration: {} ms", elapsedMs);
            log.info("  Finished at: {}", LocalDateTime.now().format(DATE_FORMAT));
            log.info("================================================");

        } catch (Exception e) {
            log.error("========== Instrument Sync FAILED ==========");
            log.error("  Error: {}", e.getMessage());
            log.error("  Markets: {}", markets);
            log.error("  Time: {}", LocalDateTime.now().format(DATE_FORMAT));
            log.error("=============================================", e);

            // 운영 환경에서는 여기서 알림 발송 가능
            // alertService.sendAlert("INSTRUMENT_SYNC_FAILED", e.getMessage());
        }
    }

    /**
     * 수동 트리거용 메서드 (테스트/관리 목적).
     *
     * @return 동기화 결과
     */
    public SyncResult triggerSync() {
        log.info("Manual instrument sync triggered");
        syncInstruments();
        return syncInstrumentsUseCase.syncByMarkets(markets);
    }
}
