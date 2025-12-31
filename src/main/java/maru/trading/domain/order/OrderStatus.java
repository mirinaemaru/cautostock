package maru.trading.domain.order;

/**
 * 주문 상태
 */
public enum OrderStatus {
	NEW,          // 신규 (생성됨)
	SENT,         // 전송됨 (브로커에 전송)
	ACCEPTED,     // 접수됨 (브로커 접수 확인)
	PART_FILLED,  // 부분 체결
	FILLED,       // 완전 체결
	CANCELLED,    // 취소됨
	REJECTED,     // 거부됨
	ERROR         // 에러
}
