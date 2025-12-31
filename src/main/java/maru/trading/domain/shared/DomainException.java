package maru.trading.domain.shared;

import lombok.Getter;

/**
 * 도메인 예외 기본 클래스
 */
@Getter
public class DomainException extends RuntimeException {
	private final ErrorCode errorCode;
	private final String detail;

	public DomainException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
		this.detail = null;
	}

	public DomainException(ErrorCode errorCode, String detail) {
		super(errorCode.getMessage() + ": " + detail);
		this.errorCode = errorCode;
		this.detail = detail;
	}

	public DomainException(ErrorCode errorCode, String detail, Throwable cause) {
		super(errorCode.getMessage() + ": " + detail, cause);
		this.errorCode = errorCode;
		this.detail = detail;
	}
}
