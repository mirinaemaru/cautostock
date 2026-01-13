package maru.trading.broker.kis.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * KRX (Korea Exchange) 종목 마스터 조회 클라이언트.
 *
 * KRX Open API를 통해 KOSPI/KOSDAQ 전체 상장 종목 정보를 조회합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KrxInstrumentClient {

    private final ObjectMapper objectMapper;
    private RestTemplate krxRestTemplate;

    private static final String KRX_API_URL = "http://data.krx.co.kr/comm/bldAttendant/getJsonData.cmd";

    // Market codes
    public static final String MARKET_KOSPI = "STK";  // 유가증권시장 (KOSPI)
    public static final String MARKET_KOSDAQ = "KSQ"; // 코스닥시장

    @PostConstruct
    public void init() {
        this.krxRestTemplate = new RestTemplate();
        this.krxRestTemplate.getMessageConverters().add(new FormHttpMessageConverter());
    }

    /**
     * 네이버 금융 API를 통한 전체 종목 조회
     * curl을 사용하여 API를 호출합니다 (Java HTTP 클라이언트가 차단됨).
     * 페이지네이션을 사용하여 전체 종목을 조회합니다.
     */
    public List<KrxInstrument> fetchInstrumentsFromNaver(String market) {
        log.info("[NAVER API] Fetching {} instruments with pagination...", market);

        List<KrxInstrument> allInstruments = new ArrayList<>();
        String marketName = "KOSPI".equalsIgnoreCase(market) ? "KOSPI" : "KOSDAQ";
        int page = 1;
        int pageSize = 100; // 네이버 API는 작은 페이지 사이즈에서 더 안정적
        int totalCount = 0;

        try {
            while (true) {
                String url = String.format(
                        "https://m.stock.naver.com/api/stocks/marketValue/%s?page=%d&pageSize=%d",
                        marketName, page, pageSize
                );

                // Use shell to execute curl since ProcessBuilder has issues with URLs
                String curlCmd = String.format(
                        "curl -s -L --max-time 30 '%s' -H 'User-Agent: Mozilla/5.0' -H 'Referer: https://m.stock.naver.com/'",
                        url
                );

                ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", curlCmd);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                String responseBody;
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                    responseBody = reader.lines().collect(java.util.stream.Collectors.joining());
                }

                boolean completed = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);
                if (!completed) {
                    process.destroyForcibly();
                    log.error("[NAVER API] curl timeout on page {}", page);
                    break;
                }

                if (responseBody == null || responseBody.isEmpty()) {
                    log.warn("[NAVER API] Empty response on page {}", page);
                    break;
                }

                // Parse JSON response
                NaverStockResponse naverResponse = objectMapper.readValue(responseBody, NaverStockResponse.class);

                if (naverResponse.getStocks() == null || naverResponse.getStocks().isEmpty()) {
                    log.debug("[NAVER API] No more stocks on page {}", page);
                    break;
                }

                for (NaverStock stock : naverResponse.getStocks()) {
                    // 심볼이 6자리 숫자인지 확인 (DB 제약조건)
                    String symbol = stock.getItemCode();
                    if (symbol == null || !symbol.matches("^[0-9]{6}$")) {
                        log.debug("[NAVER API] Skipping invalid symbol: {}", symbol);
                        continue;
                    }
                    KrxInstrument instrument = new KrxInstrument();
                    instrument.setSymbol(symbol);
                    instrument.setNameKr(stock.getStockName());
                    instrument.setMarket(market.toUpperCase());
                    allInstruments.add(instrument);
                }

                if (naverResponse.getTotalCount() != null && totalCount == 0) {
                    totalCount = naverResponse.getTotalCount();
                }

                log.debug("[NAVER API] Page {} fetched {} stocks (total so far: {})",
                        page, naverResponse.getStocks().size(), allInstruments.size());

                // 모든 종목을 가져왔으면 종료
                if (totalCount > 0 && allInstruments.size() >= totalCount) {
                    break;
                }

                // 다음 페이지가 없으면 종료
                if (naverResponse.getStocks().size() < pageSize) {
                    break;
                }

                page++;
                // Rate limiting을 피하기 위해 약간의 딜레이
                Thread.sleep(100);
            }

            log.info("[NAVER API] Fetched {} {} instruments", allInstruments.size(), market);
            return allInstruments;

        } catch (Exception e) {
            log.error("[NAVER API] Error fetching {} instruments", market, e);
            return allInstruments; // 부분적으로 가져온 것이라도 반환
        }
    }

    @Data
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class NaverStockResponse {
        private List<NaverStock> stocks;
        private Integer totalCount;
    }

    @Data
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class NaverStock {
        private String itemCode;
        private String stockName;
        private String marketName;
    }

    /**
     * KOSPI 전체 종목 조회
     */
    public List<KrxInstrument> fetchKospiInstruments() {
        return fetchInstruments(MARKET_KOSPI, "KOSPI");
    }

    /**
     * KOSDAQ 전체 종목 조회
     */
    public List<KrxInstrument> fetchKosdaqInstruments() {
        return fetchInstruments(MARKET_KOSDAQ, "KOSDAQ");
    }

    /**
     * 전체 종목 조회 (KOSPI + KOSDAQ)
     */
    public List<KrxInstrument> fetchAllInstruments() {
        List<KrxInstrument> all = new ArrayList<>();
        all.addAll(fetchKospiInstruments());
        all.addAll(fetchKosdaqInstruments());
        return all;
    }

    /**
     * 특정 시장의 종목 조회
     */
    private List<KrxInstrument> fetchInstruments(String mktId, String marketName) {
        log.info("[KRX API] Fetching {} instruments...", marketName);

        try {
            String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

            // Request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("User-Agent", "Mozilla/5.0");

            // Request body (form data)
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("bld", "dbms/MDC/STAT/standard/MDCSTAT01501");
            formData.add("mktId", mktId);
            formData.add("trdDd", today);
            formData.add("share", "1");
            formData.add("money", "1");
            formData.add("csvxls_is498", "false");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

            ResponseEntity<String> response = krxRestTemplate.exchange(
                    KRX_API_URL,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("[KRX API] Failed to fetch instruments: {}", response.getStatusCode());
                return new ArrayList<>();
            }

            // Parse response
            KrxResponse krxResponse = objectMapper.readValue(response.getBody(), KrxResponse.class);

            List<KrxInstrument> instruments = new ArrayList<>();
            if (krxResponse.getOutBlock1() != null) {
                for (Map<String, Object> item : krxResponse.getOutBlock1()) {
                    KrxInstrument instrument = parseInstrument(item, marketName);
                    if (instrument != null) {
                        instruments.add(instrument);
                    }
                }
            }

            log.info("[KRX API] Fetched {} {} instruments", instruments.size(), marketName);
            return instruments;

        } catch (Exception e) {
            log.error("[KRX API] Error fetching {} instruments", marketName, e);
            return new ArrayList<>();
        }
    }

    /**
     * KRX 응답 데이터를 KrxInstrument로 변환
     */
    private KrxInstrument parseInstrument(Map<String, Object> item, String marketName) {
        try {
            KrxInstrument instrument = new KrxInstrument();
            instrument.setSymbol(getString(item, "ISU_SRT_CD"));       // 종목코드 (단축)
            instrument.setIsin(getString(item, "ISU_CD"));              // ISIN 코드
            instrument.setNameKr(getString(item, "ISU_ABBRV"));         // 종목명 (약어)
            instrument.setMarket(marketName);
            instrument.setSectorCode(getString(item, "MKT_ID"));        // 시장구분

            // 상장주식수
            String listShrs = getString(item, "LIST_SHRS");
            if (listShrs != null && !listShrs.isEmpty()) {
                instrument.setListedShares(Long.parseLong(listShrs.replace(",", "")));
            }

            return instrument;
        } catch (Exception e) {
            log.warn("[KRX API] Failed to parse instrument: {}", item, e);
            return null;
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString().trim() : null;
    }

    /**
     * KRX API 응답 DTO
     */
    @Data
    public static class KrxResponse {
        @JsonProperty("OutBlock_1")
        private List<Map<String, Object>> outBlock1;

        @JsonProperty("CURRENT_DATETIME")
        private String currentDatetime;
    }

    /**
     * KRX 종목 정보 DTO
     */
    @Data
    public static class KrxInstrument {
        private String symbol;       // 종목코드 (6자리)
        private String isin;         // ISIN 코드
        private String nameKr;       // 종목명 (한글)
        private String market;       // 시장 (KOSPI/KOSDAQ)
        private String sectorCode;   // 업종코드
        private Long listedShares;   // 상장주식수
    }
}
