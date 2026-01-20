package maru.trading.broker.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * KIS 잔고 조회 응답 DTO.
 *
 * KIS OpenAPI - 주식잔고조회 API 응답.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KisBalanceResponse {

    /**
     * 응답 코드
     * - 0: 성공
     * - 기타: 실패
     */
    @JsonProperty("rt_cd")
    private String rtCd;

    /**
     * 응답 메시지 코드
     */
    @JsonProperty("msg_cd")
    private String msgCd;

    /**
     * 메시지
     */
    @JsonProperty("msg1")
    private String msg1;

    /**
     * 응답 데이터 1 (계좌별 잔고 요약)
     */
    @JsonProperty("output1")
    private List<BalanceItem> output1;

    /**
     * 응답 데이터 2 (계좌 종합 정보)
     */
    @JsonProperty("output2")
    private List<BalanceSummary> output2;

    /**
     * 개별 종목 잔고 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceItem {
        /**
         * 종목번호
         */
        @JsonProperty("pdno")
        private String pdno;

        /**
         * 종목명
         */
        @JsonProperty("prdt_name")
        private String prdtName;

        /**
         * 보유수량
         */
        @JsonProperty("hldg_qty")
        private String hldgQty;

        /**
         * 매입평균가격
         */
        @JsonProperty("pchs_avg_pric")
        private String pchsAvgPric;

        /**
         * 매입금액
         */
        @JsonProperty("pchs_amt")
        private String pchsAmt;

        /**
         * 현재가
         */
        @JsonProperty("prpr")
        private String prpr;

        /**
         * 평가금액
         */
        @JsonProperty("evlu_amt")
        private String evluAmt;

        /**
         * 평가손익금액
         */
        @JsonProperty("evlu_pfls_amt")
        private String evluPflsAmt;

        /**
         * 평가손익율
         */
        @JsonProperty("evlu_pfls_rt")
        private String evluPflsRt;

        /**
         * 평가수익율
         */
        @JsonProperty("evlu_erng_rt")
        private String evluErngRt;
    }

    /**
     * 계좌 종합 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceSummary {
        /**
         * 예수금총액 (주문가능 현금)
         */
        @JsonProperty("dnca_tot_amt")
        private String dncaTotAmt;

        /**
         * 유가증권 평가금액 (주식 평가액)
         */
        @JsonProperty("scts_evlu_amt")
        private String sctsEvluAmt;

        /**
         * 총평가금액 (총 자산)
         */
        @JsonProperty("tot_evlu_amt")
        private String totEvluAmt;

        /**
         * 순자산금액
         */
        @JsonProperty("nass_amt")
        private String nassAmt;

        /**
         * 총손익금액
         */
        @JsonProperty("fncg_gld_auto_rdpt_yn")
        private String totPflsAmt;

        /**
         * 총수익률
         */
        @JsonProperty("evlu_pfls_smtl_amt")
        private String evluPflsSmtlAmt;

        /**
         * 총평가손익금액
         */
        @JsonProperty("tot_evlu_pfls_amt")
        private String totEvluPflsAmt;
    }

    /**
     * 잔고 조회 성공 여부
     */
    public boolean isSuccess() {
        return "0".equals(rtCd);
    }

    /**
     * 현금 잔고 반환 (예수금총액)
     */
    public BigDecimal getCashBalance() {
        if (output2 != null && !output2.isEmpty()) {
            String amt = output2.get(0).getDncaTotAmt();
            return amt != null ? new BigDecimal(amt) : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    /**
     * 주식 평가액 반환
     */
    public BigDecimal getStockValue() {
        if (output2 != null && !output2.isEmpty()) {
            String amt = output2.get(0).getSctsEvluAmt();
            return amt != null ? new BigDecimal(amt) : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    /**
     * 총 자산 반환
     */
    public BigDecimal getTotalAssets() {
        if (output2 != null && !output2.isEmpty()) {
            String amt = output2.get(0).getTotEvluAmt();
            return amt != null ? new BigDecimal(amt) : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    /**
     * 총 평가손익 반환
     */
    public BigDecimal getTotalProfitLoss() {
        if (output2 != null && !output2.isEmpty()) {
            String amt = output2.get(0).getTotEvluPflsAmt();
            return amt != null ? new BigDecimal(amt) : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }
}
