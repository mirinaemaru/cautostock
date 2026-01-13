package maru.trading.broker.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KIS 주문 응답 DTO.
 *
 * KIS OpenAPI - 주식주문(현금) API 응답.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KisOrderResponse {

    /**
     * 응답 코드
     * - 0: 성공
     * - 기타: 실패
     */
    @JsonProperty("rt_cd")
    private String rtCd;

    /**
     * 응답 메시지
     */
    @JsonProperty("msg_cd")
    private String msgCd;

    /**
     * 메시지
     */
    @JsonProperty("msg1")
    private String msg1;

    /**
     * 응답 데이터
     */
    @JsonProperty("output")
    private OrderOutput output;

    /**
     * 주문 응답 데이터
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderOutput {
        /**
         * 한국거래소 전송 주문조직번호
         */
        @JsonProperty("KRX_FWDG_ORD_ORGNO")
        private String krxFwdgOrdOrgno;

        /**
         * 주문번호 (KIS 주문번호)
         */
        @JsonProperty("ODNO")
        private String odno;

        /**
         * 주문시각 (HHMMSS)
         */
        @JsonProperty("ORD_TMD")
        private String ordTmd;
    }

    /**
     * 주문 성공 여부
     */
    public boolean isSuccess() {
        return "0".equals(rtCd);
    }

    /**
     * 주문번호 반환 (없으면 null)
     */
    public String getOrderNumber() {
        return (output != null) ? output.getOdno() : null;
    }
}
