package maru.trading.domain.shared;

/**
 * 도메인 에러 코드
 */
public enum ErrorCode {
	// Account
	ACCOUNT_001("Account not found"),
	ACCOUNT_002("Account already exists"),
	ACCOUNT_003("Account not active"),
	ACCOUNT_004("Permission denied"),

	// Strategy
	STRATEGY_001("Strategy not found"),
	STRATEGY_002("Strategy already exists"),
	STRATEGY_003("Invalid strategy parameters"),
	STRATEGY_004("Strategy not active"),

	// Signal
	SIGNAL_001("Signal expired"),
	SIGNAL_002("Duplicate signal"),
	SIGNAL_003("Invalid signal type"),

	// Risk
	RISK_001("Risk limit exceeded"),
	RISK_002("Kill switch activated"),
	RISK_003("Daily loss limit exceeded"),
	RISK_004("Max position value exceeded"),
	RISK_005("Max open orders exceeded"),

	// Order
	ORDER_001("Order not found"),
	ORDER_002("Invalid order price"),
	ORDER_003("Invalid order quantity"),
	ORDER_004("Duplicate order (idempotency key)"),
	ORDER_005("Order cannot be cancelled"),
	ORDER_006("Broker order failed"),

	// Fill
	FILL_001("Fill not found"),
	FILL_002("Invalid fill data"),

	// Position
	POSITION_001("Position not found"),
	POSITION_002("Insufficient position quantity"),

	// Broker
	BROKER_001("Token expired"),
	BROKER_002("WebSocket connection failed"),
	BROKER_003("API rate limit exceeded"),
	BROKER_004("Invalid broker response"),

	// System
	SYSTEM_001("Internal error"),
	SYSTEM_002("Database error"),
	SYSTEM_003("Configuration error");

	private final String message;

	ErrorCode(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public String getCode() {
		return this.name();
	}
}
