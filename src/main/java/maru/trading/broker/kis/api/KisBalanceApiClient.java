package maru.trading.broker.kis.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.broker.kis.auth.KisTokenManager;
import maru.trading.broker.kis.config.KisProperties;
import maru.trading.broker.kis.dto.KisBalanceResponse;
import maru.trading.domain.shared.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * KIS 잔고 조회 API 클라이언트.
 *
 * 실제 KIS OpenAPI를 호출하여 계좌 잔고를 조회합니다.
 * PAPER(모의투자)와 LIVE(실전투자) 환경을 지원합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KisBalanceApiClient {

    private final RestTemplate kisRestTemplate;
    private final KisProperties kisProperties;
    private final KisTokenManager tokenManager;

    // API 경로
    private static final String BALANCE_PATH = "/uapi/domestic-stock/v1/trading/inquire-balance";
    private static final String PAPER_BALANCE_PATH = "/uapi/domestic-stock/v1/trading/inquire-psbl-order";

    // TR_ID constants
    private static final String TR_ID_PAPER = "VTTC8434R";   // 모의투자 잔고조회
    private static final String TR_ID_LIVE = "TTTC8434R";    // 실전투자 잔고조회

    /**
     * 계좌 잔고 조회.
     *
     * @param accountNo 계좌번호
     * @param accountProduct 계좌상품코드
     * @param environment 환경 (PAPER/LIVE)
     * @return 잔고 정보
     * @throws KisApiException if balance inquiry fails
     */
    public KisBalanceResponse getBalance(String accountNo, String accountProduct, Environment environment)
            throws KisApiException {

        log.info("[KIS API] Get balance: accountNo={}, env={}", accountNo, environment);

        try {
            // 환경에 따라 설정 선택
            KisProperties.EnvironmentConfig config = (environment == Environment.LIVE)
                    ? kisProperties.getLive()
                    : kisProperties.getPaper();

            String trId = (environment == Environment.LIVE) ? TR_ID_LIVE : TR_ID_PAPER;
            String path = (environment == Environment.LIVE) ? BALANCE_PATH : PAPER_BALANCE_PATH;

            // URL 구성
            String baseUrl = config.getBaseUrl();
            String url = String.format("%s%s?CANO=%s&ACNT_PRDT_CD=%s&AFHR_FLPR_YN=N&OFL_YN=&INQR_DVSN=02&UNPR_DVSN=01&FUND_STTL_ICLD_YN=N&FNCG_AMT_AUTO_RDPT_YN=N&PRCS_DVSN=01&CTX_AREA_FK100=&CTX_AREA_NK100=",
                    baseUrl, path, accountNo, accountProduct);

            // 인증 토큰 가져오기
            String accessToken = tokenManager.getAccessToken();

            // HTTP 헤더 구성
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authorization", "Bearer " + accessToken);
            headers.set("appkey", config.getAppKey());
            headers.set("appsecret", config.getAppSecret());
            headers.set("tr_id", trId);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // API 호출
            log.debug("[KIS API] GET {} (TR_ID: {})", url, trId);
            ResponseEntity<KisBalanceResponse> response = kisRestTemplate.exchange(
                    url, HttpMethod.GET, entity, KisBalanceResponse.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new KisApiException("Balance API failed: HTTP " + response.getStatusCode(),
                        KisApiException.ErrorType.SERVER_ERROR);
            }

            KisBalanceResponse balanceResponse = response.getBody();

            if (!balanceResponse.isSuccess()) {
                log.error("[KIS API] Balance inquiry failed: rtCd={}, msg={}",
                        balanceResponse.getRtCd(), balanceResponse.getMsg1());
                throw new KisApiException("Balance inquiry failed: " + balanceResponse.getMsg1(),
                        KisApiException.ErrorType.SERVER_ERROR);
            }

            log.info("[KIS API] Balance inquiry success: cashBalance={}, stockValue={}, totalAssets={}",
                    balanceResponse.getCashBalance(),
                    balanceResponse.getStockValue(),
                    balanceResponse.getTotalAssets());

            return balanceResponse;

        } catch (KisApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("[KIS API] Failed to get balance", e);
            throw new KisApiException("Failed to call KIS balance API: " + e.getMessage(), e,
                    KisApiException.ErrorType.UNKNOWN);
        }
    }

    /**
     * PAPER 환경 잔고 조회 편의 메서드.
     */
    public KisBalanceResponse getPaperBalance(String accountNo, String accountProduct) throws KisApiException {
        return getBalance(accountNo, accountProduct, Environment.PAPER);
    }

    /**
     * LIVE 환경 잔고 조회 편의 메서드.
     */
    public KisBalanceResponse getLiveBalance(String accountNo, String accountProduct) throws KisApiException {
        return getBalance(accountNo, accountProduct, Environment.LIVE);
    }
}
