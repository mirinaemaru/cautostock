package maru.trading.broker.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KIS 주문 요청 DTO.
 *
 * KIS OpenAPI - 주식주문(현금) API 요청 파라미터.
 *
 * API: POST /uapi/domestic-stock/v1/trading/order-cash
 * TR_ID:
 * - VTTC0802U (모의투자 매수)
 * - VTTC0801U (모의투자 매도)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KisOrderRequest {

    /**
     * 종목코드 (6자리, 예: "005930" = 삼성전자)
     */
    @JsonProperty("PDNO")
    private String pdno;

    /**
     * 주문구분
     * - 00: 지정가
     * - 01: 시장가
     * - 02: 조건부지정가
     * - 등...
     */
    @JsonProperty("ORD_DVSN")
    private String ordDvsn;

    /**
     * 주문수량
     */
    @JsonProperty("ORD_QTY")
    private String ordQty;

    /**
     * 주문단가 (지정가인 경우)
     * 시장가인 경우 "0" 입력
     */
    @JsonProperty("ORD_UNPR")
    private String ordUnpr;

    /**
     * 계좌번호 앞 8자리
     */
    @JsonProperty("CANO")
    private String cano;

    /**
     * 계좌상품코드 (2자리)
     */
    @JsonProperty("ACNT_PRDT_CD")
    private String acntPrdtCd;

    /**
     * 주문가능여부 조회만 할지 여부
     * - Y: 조회만
     * - N: 실제 주문
     */
    @JsonProperty("ORD_SVR_DVSN_CD")
    private String ordSvrDvsnCd;

    // Helper methods

    /**
     * 지정가 매수 주문 생성
     */
    public static KisOrderRequest limitBuy(String accountNo, String accountProduct,
                                            String symbol, int quantity, int price) {
        return KisOrderRequest.builder()
                .cano(accountNo)
                .acntPrdtCd(accountProduct)
                .pdno(symbol)
                .ordDvsn("00")  // 지정가
                .ordQty(String.valueOf(quantity))
                .ordUnpr(String.valueOf(price))
                .ordSvrDvsnCd("N")  // 실제 주문
                .build();
    }

    /**
     * 지정가 매도 주문 생성
     */
    public static KisOrderRequest limitSell(String accountNo, String accountProduct,
                                             String symbol, int quantity, int price) {
        return KisOrderRequest.builder()
                .cano(accountNo)
                .acntPrdtCd(accountProduct)
                .pdno(symbol)
                .ordDvsn("00")  // 지정가
                .ordQty(String.valueOf(quantity))
                .ordUnpr(String.valueOf(price))
                .ordSvrDvsnCd("N")  // 실제 주문
                .build();
    }

    /**
     * 시장가 매수 주문 생성
     */
    public static KisOrderRequest marketBuy(String accountNo, String accountProduct,
                                             String symbol, int quantity) {
        return KisOrderRequest.builder()
                .cano(accountNo)
                .acntPrdtCd(accountProduct)
                .pdno(symbol)
                .ordDvsn("01")  // 시장가
                .ordQty(String.valueOf(quantity))
                .ordUnpr("0")   // 시장가는 가격 0
                .ordSvrDvsnCd("N")
                .build();
    }

    /**
     * 시장가 매도 주문 생성
     */
    public static KisOrderRequest marketSell(String accountNo, String accountProduct,
                                              String symbol, int quantity) {
        return KisOrderRequest.builder()
                .cano(accountNo)
                .acntPrdtCd(accountProduct)
                .pdno(symbol)
                .ordDvsn("01")  // 시장가
                .ordQty(String.valueOf(quantity))
                .ordUnpr("0")
                .ordSvrDvsnCd("N")
                .build();
    }
}
