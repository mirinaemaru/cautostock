package maru.trading.domain.shared;

/**
 * 환경 구분 (모의/실전/백테스트)
 */
public enum Environment {
	PAPER,      // 모의투자
	LIVE,       // 실전
	BACKTEST    // 백테스트
}
