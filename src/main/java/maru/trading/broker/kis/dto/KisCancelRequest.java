package maru.trading.broker.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KIS 주문 취소 요청 DTO.
 *
 * API: POST /uapi/domestic-stock/v1/trading/order-rvsecncl
 * TR_ID: VTTC0803U (모의투자 취소)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KisCancelRequest {

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
     * 한국거래소 전송 주문조직번호
     */
    @JsonProperty("KRX_FWDG_ORD_ORGNO")
    private String krxFwdgOrdOrgno;

    /**
     * 원주문번호 (취소할 주문번호)
     */
    @JsonProperty("ORGN_ODNO")
    private String orgnOdno;

    /**
     * 주문구분
     * - 00: 지정가
     * - 01: 시장가
     */
    @JsonProperty("ORD_DVSN")
    private String ordDvsn;

    /**
     * 정정취소 구분코드
     * - 01: 정정
     * - 02: 취소
     */
    @JsonProperty("RVSE_CNCL_DVSN_CD")
    private String rvseCnclDvsnCd;

    /**
     * 주문수량 (전량 취소 시 원주문 수량)
     */
    @JsonProperty("ORD_QTY")
    private String ordQty;

    /**
     * 주문단가 (취소 시 "0")
     */
    @JsonProperty("ORD_UNPR")
    private String ordUnpr;

    /**
     * 주문가능여부 조회만 할지 여부
     * - Y: 조회만
     * - N: 실제 취소
     */
    @JsonProperty("QTY_ALL_ORD_YN")
    private String qtyAllOrdYn;

    /**
     * 주문 취소 요청 생성
     */
    public static KisCancelRequest of(String accountNo, String accountProduct,
                                       String orgNo, String orderNo, String ordQty) {
        return KisCancelRequest.builder()
                .cano(accountNo)
                .acntPrdtCd(accountProduct)
                .krxFwdgOrdOrgno(orgNo)
                .orgnOdno(orderNo)
                .ordDvsn("00")
                .rvseCnclDvsnCd("02")  // 취소
                .ordQty(ordQty)
                .ordUnpr("0")
                .qtyAllOrdYn("Y")  // 전량 취소
                .build();
    }
}
