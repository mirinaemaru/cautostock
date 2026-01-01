package maru.trading.broker.kis.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.broker.kis.api.KisApiException;
import maru.trading.broker.kis.config.KisProperties;
import maru.trading.broker.kis.dto.KisApprovalKeyRequest;
import maru.trading.broker.kis.dto.KisApprovalKeyResponse;
import maru.trading.broker.kis.dto.KisTokenRequest;
import maru.trading.broker.kis.dto.KisTokenResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * KIS 인증 클라이언트 (PAPER 모드 실제 구현).
 *
 * KIS OpenAPI OAuth2 인증을 처리합니다:
 * - POST /oauth2/tokenP (PAPER 환경 토큰 발급)
 * - POST /oauth2/Approval (WebSocket Approval Key 발급)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KisAuthenticationClient {

    private final RestTemplate kisRestTemplate;
    private final KisProperties kisProperties;

    /**
     * OAuth2 액세스 토큰 발급.
     *
     * @param appKey Application key
     * @param appSecret Application secret
     * @param isPaper True for PAPER environment, false for LIVE
     * @return Token response
     * @throws KisApiException if token issuance fails
     */
    public KisTokenResponse issueToken(String appKey, String appSecret, boolean isPaper) throws KisApiException {
        log.info("[KIS AUTH] Issuing token for appKey={}, isPaper={}",
                maskAppKey(appKey), isPaper);

        try {
            // 요청 URL 결정
            String baseUrl = isPaper
                    ? kisProperties.getPaper().getBaseUrl()
                    : kisProperties.getLive().getBaseUrl();
            String tokenPath = isPaper ? "/oauth2/tokenP" : "/oauth2/Approval";
            String url = baseUrl + tokenPath;

            // 요청 본문
            KisTokenRequest request = KisTokenRequest.of(appKey, appSecret);

            // HTTP 헤더
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<KisTokenRequest> entity = new HttpEntity<>(request, headers);

            // API 호출
            log.debug("[KIS AUTH] POST {}", url);
            ResponseEntity<KisTokenResponse> response = kisRestTemplate.postForEntity(
                    url, entity, KisTokenResponse.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new KisApiException("Token issuance failed: HTTP " + response.getStatusCode(),
                        KisApiException.ErrorType.AUTHENTICATION);
            }

            KisTokenResponse tokenResponse = response.getBody();
            log.info("[KIS AUTH] Token issued: expiresIn={} seconds", tokenResponse.getExpiresIn());

            return tokenResponse;

        } catch (KisApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("[KIS AUTH] Failed to issue token", e);
            throw new KisApiException("Failed to issue token: " + e.getMessage(), e,
                    KisApiException.ErrorType.UNKNOWN);
        }
    }

    /**
     * WebSocket Approval Key 발급.
     *
     * @param appKey Application key
     * @param appSecret Application secret
     * @param isLive True for LIVE environment, false for PAPER
     * @return Approval key
     * @throws KisApiException if approval key issuance fails
     */
    public String issueApprovalKey(String appKey, String appSecret, boolean isLive) throws KisApiException {
        log.info("[KIS AUTH] Issuing approval key for appKey={}, isLive={}", maskAppKey(appKey), isLive);

        try {
            // 환경에 따라 base URL 선택
            String baseUrl = isLive
                    ? kisProperties.getLive().getBaseUrl()
                    : kisProperties.getPaper().getBaseUrl();
            String url = baseUrl + "/oauth2/Approval";

            // 요청 본문 (Approval API uses 'secretkey' instead of 'appsecret')
            KisApprovalKeyRequest request = KisApprovalKeyRequest.of(appKey, appSecret);

            // HTTP 헤더
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<KisApprovalKeyRequest> entity = new HttpEntity<>(request, headers);

            // API 호출
            log.debug("[KIS AUTH] POST {} for approval key", url);
            ResponseEntity<KisApprovalKeyResponse> response = kisRestTemplate.postForEntity(
                    url, entity, KisApprovalKeyResponse.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new KisApiException("Approval key issuance failed: HTTP " + response.getStatusCode(),
                        KisApiException.ErrorType.AUTHENTICATION);
            }

            String approvalKey = response.getBody().getApprovalKey();
            log.info("[KIS AUTH] Approval key issued: {}...", approvalKey.substring(0, Math.min(15, approvalKey.length())));

            return approvalKey;

        } catch (KisApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("[KIS AUTH] Failed to issue approval key", e);
            throw new KisApiException("Failed to issue approval key: " + e.getMessage(), e,
                    KisApiException.ErrorType.UNKNOWN);
        }
    }

    /**
     * App Key 마스킹 (로깅용)
     */
    private String maskAppKey(String appKey) {
        if (appKey == null || appKey.length() < 8) {
            return "****";
        }
        return appKey.substring(0, 4) + "****" + appKey.substring(appKey.length() - 4);
    }
}
