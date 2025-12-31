package maru.trading.domain.risk;

/**
 * Kill Switch 상태
 */
public enum KillSwitchStatus {
	OFF,     // 비활성 (정상 거래)
	ARMED,   // 대기 (경고 상태)
	ON       // 활성 (거래 차단)
}
