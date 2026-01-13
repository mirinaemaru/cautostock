package maru.trading.broker.kis.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.broker.kis.auth.KisTokenManager;
import maru.trading.broker.kis.config.KisProperties;
import maru.trading.broker.kis.dto.*;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * KIS 주문 API 클라이언트 (PAPER 모드).
 *
 * 실제 KIS OpenAPI를 호출하여 주문, 취소, 정정을 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KisOrderApiClient {

    private final RestTemplate kisRestTemplate;
    private final KisProperties kisProperties;
    private final KisTokenManager tokenManager;

    private static final String ORDER_PATH = "/uapi/domestic-stock/v1/trading/order-cash";
    private static final String CANCEL_MODIFY_PATH = "/uapi/domestic-stock/v1/trading/order-rvsecncl";

    // TR_ID constants (PAPER mode)
    private static final String TR_ID_BUY = "VTTC0802U";   // 모의투자 매수
    private static final String TR_ID_SELL = "VTTC0801U";  // 모의투자 매도
    private static final String TR_ID_CANCEL_MODIFY = "VTTC0803U";  // 모의투자 정정취소

    /**
     * 매수 주문 실행.
     *
     * @param request 주문 요청
     * @return 주문 응답
     * @throws KisApiException if order fails
     */
    public KisOrderResponse placeBuyOrder(KisOrderRequest request) throws KisApiException {
        log.info("[KIS API] Place BUY order: symbol={}, qty={}, price={}",
                request.getPdno(), request.getOrdQty(), request.getOrdUnpr());

        return executeOrderApi(ORDER_PATH, TR_ID_BUY, request);
    }

    /**
     * 매도 주문 실행.
     *
     * @param request 주문 요청
     * @return 주문 응답
     * @throws KisApiException if order fails
     */
    public KisOrderResponse placeSellOrder(KisOrderRequest request) throws KisApiException {
        log.info("[KIS API] Place SELL order: symbol={}, qty={}, price={}",
                request.getPdno(), request.getOrdQty(), request.getOrdUnpr());

        return executeOrderApi(ORDER_PATH, TR_ID_SELL, request);
    }

    /**
     * 주문 취소 실행.
     *
     * @param request 취소 요청
     * @return 주문 응답
     * @throws KisApiException if cancel fails
     */
    public KisOrderResponse cancelOrder(KisCancelRequest request) throws KisApiException {
        log.info("[KIS API] Cancel order: orderNo={}", request.getOrgnOdno());

        return executeOrderApi(CANCEL_MODIFY_PATH, TR_ID_CANCEL_MODIFY, request);
    }

    /**
     * 주문 정정 실행.
     *
     * @param request 정정 요청
     * @return 주문 응답
     * @throws KisApiException if modify fails
     */
    public KisOrderResponse modifyOrder(KisModifyRequest request) throws KisApiException {
        log.info("[KIS API] Modify order: orderNo={}, newQty={}, newPrice={}",
                request.getOrgnOdno(), request.getOrdQty(), request.getOrdUnpr());

        return executeOrderApi(CANCEL_MODIFY_PATH, TR_ID_CANCEL_MODIFY, request);
    }

    /**
     * 주문 API 실행 (공통 로직).
     */
    private <T> KisOrderResponse executeOrderApi(String path, String trId, T requestBody) throws KisApiException {
        try {
            // URL 구성
            String baseUrl = kisProperties.getPaper().getBaseUrl();
            String url = baseUrl + path;

            // 인증 토큰 가져오기
            String accessToken = tokenManager.getAccessToken();

            // HTTP 헤더 구성
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authorization", "Bearer " + accessToken);
            headers.set("appkey", kisProperties.getPaper().getAppKey());
            headers.set("appsecret", kisProperties.getPaper().getAppSecret());
            headers.set("tr_id", trId);

            HttpEntity<T> entity = new HttpEntity<>(requestBody, headers);

            // API 호출
            log.debug("[KIS API] POST {} (TR_ID: {})", url, trId);
            ResponseEntity<KisOrderResponse> response = kisRestTemplate.postForEntity(
                    url, entity, KisOrderResponse.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new KisApiException("Order API failed: HTTP " + response.getStatusCode(),
                        KisApiException.ErrorType.SERVER_ERROR);
            }

            KisOrderResponse orderResponse = response.getBody();

            if (!orderResponse.isSuccess()) {
                log.error("[KIS API] Order failed: rtCd={}, msg={}",
                        orderResponse.getRtCd(), orderResponse.getMsg1());
                throw new KisApiException("Order rejected: " + orderResponse.getMsg1(),
                        KisApiException.ErrorType.ORDER_REJECTED);
            }

            log.info("[KIS API] Order success: orderNo={}", orderResponse.getOrderNumber());
            return orderResponse;

        } catch (KisApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("[KIS API] API call failed", e);
            throw new KisApiException("Failed to call KIS API: " + e.getMessage(), e,
                    KisApiException.ErrorType.UNKNOWN);
        }
    }
}
