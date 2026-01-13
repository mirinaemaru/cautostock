package maru.trading.broker.kis.ws.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KIS WebSocket tick message DTO.
 *
 * Represents the structure of real-time tick data from KIS WebSocket API.
 *
 * Example message:
 * {
 *   "header": {
 *     "tr_id": "H0STCNT0",
 *     "encrypt": "N"
 *   },
 *   "body": {
 *     "rt_cd": "0",
 *     "msg_cd": "OPSP0000",
 *     "msg1": "정상처리",
 *     "output": {
 *       "MKSC_SHRN_ISCD": "005930",
 *       "STCK_PRPR": "72000",
 *       "CNTG_VOL": "100",
 *       "STCK_CNTG_HOUR": "153000",
 *       "ASKP1": "72100",
 *       "BIDP1": "72000"
 *     }
 *   }
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KisTickMessage {

    @JsonProperty("header")
    private Header header;

    @JsonProperty("body")
    private Body body;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Header {
        @JsonProperty("tr_id")
        private String trId;

        @JsonProperty("encrypt")
        private String encrypt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Body {
        @JsonProperty("rt_cd")
        private String rtCd;  // Response code ("0" = success)

        @JsonProperty("msg_cd")
        private String msgCd;

        @JsonProperty("msg1")
        private String msg1;  // Response message

        @JsonProperty("output")
        private Output output;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Output {
        /**
         * 종목코드 (Market Short Code)
         */
        @JsonProperty("MKSC_SHRN_ISCD")
        private String MKSC_SHRN_ISCD;

        /**
         * 주식 현재가 (Stock Present Price)
         */
        @JsonProperty("STCK_PRPR")
        private String STCK_PRPR;

        /**
         * 체결 거래량 (Contract Volume)
         */
        @JsonProperty("CNTG_VOL")
        private String CNTG_VOL;

        /**
         * 주식 체결 시간 (Stock Contract Hour) - Format: HHMMSS
         */
        @JsonProperty("STCK_CNTG_HOUR")
        private String STCK_CNTG_HOUR;

        /**
         * 매도호가1 (Ask Price 1)
         */
        @JsonProperty("ASKP1")
        private String ASKP1;

        /**
         * 매수호가1 (Bid Price 1)
         */
        @JsonProperty("BIDP1")
        private String BIDP1;

        /**
         * 누적 거래량 (Accumulated Volume)
         */
        @JsonProperty("ACML_VOL")
        private String ACML_VOL;

        /**
         * 전일 대비 부호 (1:상한, 2:상승, 3:보합, 4:하한, 5:하락)
         */
        @JsonProperty("PRDY_VRSS_SIGN")
        private String PRDY_VRSS_SIGN;

        /**
         * 전일 대비 (Previous Day Versus)
         */
        @JsonProperty("PRDY_VRSS")
        private String PRDY_VRSS;

        /**
         * 전일 대비율 (Previous Day Versus Rate)
         */
        @JsonProperty("PRDY_CTRT")
        private String PRDY_CTRT;
    }

    /**
     * Check if message indicates success.
     */
    public boolean isSuccess() {
        return body != null && "0".equals(body.getRtCd());
    }

    /**
     * Get error message if failed.
     */
    public String getErrorMessage() {
        if (body == null) {
            return "Empty body";
        }
        return body.getMsg1();
    }
}
