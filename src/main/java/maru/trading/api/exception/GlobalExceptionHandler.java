package maru.trading.api.exception;

import lombok.extern.slf4j.Slf4j;
import maru.trading.api.dto.response.ErrorResponse;
import maru.trading.domain.shared.DomainException;
import maru.trading.domain.shared.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 글로벌 예외 처리 핸들러
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 도메인 예외 처리
	 */
	@ExceptionHandler(DomainException.class)
	public ResponseEntity<ErrorResponse> handleDomainException(
			DomainException ex,
			WebRequest request
	) {
		log.warn("Domain exception: code={}, message={}, detail={}",
				ex.getErrorCode().getCode(),
				ex.getMessage(),
				ex.getDetail());

		ErrorCode errorCode = ex.getErrorCode();
		HttpStatus status = determineHttpStatus(errorCode);

		ErrorResponse response = ErrorResponse.of(
				errorCode.getCode(),
				errorCode.getMessage(),
				ex.getDetail()
		);

		return new ResponseEntity<>(response, status);
	}

	/**
	 * Validation 예외 처리
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(
			MethodArgumentNotValidException ex
	) {
		String detail = ex.getBindingResult()
				.getAllErrors()
				.stream()
				.map(error -> {
					String field = ((FieldError) error).getField();
					String message = error.getDefaultMessage();
					return field + ": " + message;
				})
				.collect(Collectors.joining(", "));

		log.warn("Validation exception: {}", detail);

		ErrorResponse response = ErrorResponse.of(
				"VALIDATION_ERROR",
				"Invalid request parameters",
				detail
		);

		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	/**
	 * IllegalArgumentException 처리
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
			IllegalArgumentException ex
	) {
		log.warn("Illegal argument: {}", ex.getMessage());

		ErrorResponse response = ErrorResponse.of(
				"BAD_REQUEST",
				"Invalid argument",
				ex.getMessage()
		);

		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	/**
	 * IllegalStateException 처리
	 */
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ErrorResponse> handleIllegalStateException(
			IllegalStateException ex
	) {
		log.warn("Illegal state: {}", ex.getMessage());

		ErrorResponse response = ErrorResponse.of(
				"CONFLICT",
				"Invalid state",
				ex.getMessage()
		);

		return new ResponseEntity<>(response, HttpStatus.CONFLICT);
	}

	/**
	 * 404 Not Found - 핸들러 없음
	 */
	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
			NoHandlerFoundException ex
	) {
		log.warn("No handler found: {} {}", ex.getHttpMethod(), ex.getRequestURL());

		ErrorResponse response = ErrorResponse.of(
				"NOT_FOUND",
				"Resource not found",
				"No handler found for " + ex.getHttpMethod() + " " + ex.getRequestURL()
		);

		return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
	}

	/**
	 * 404 Not Found - 리소스 없음 (Spring Boot 3.x)
	 */
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
			NoResourceFoundException ex
	) {
		log.warn("No resource found: {}", ex.getResourcePath());

		ErrorResponse response = ErrorResponse.of(
				"NOT_FOUND",
				"Resource not found",
				"No resource found at " + ex.getResourcePath()
		);

		return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
	}

	/**
	 * 기타 모든 예외 처리
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleAllExceptions(
			Exception ex,
			WebRequest request
	) {
		log.error("Unexpected exception", ex);

		ErrorResponse response = ErrorResponse.of(
				ErrorCode.SYSTEM_001.getCode(),
				ErrorCode.SYSTEM_001.getMessage(),
				"An unexpected error occurred"
		);

		return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * ErrorCode에 따라 적절한 HTTP 상태 코드 결정
	 */
	private HttpStatus determineHttpStatus(ErrorCode errorCode) {
		String code = errorCode.getCode();

		if (code.startsWith("ACCOUNT_001") || code.startsWith("ORDER_001") ||
				code.startsWith("STRATEGY_001") || code.startsWith("FILL_001") ||
				code.startsWith("POSITION_001")) {
			return HttpStatus.NOT_FOUND;
		}

		if (code.startsWith("ACCOUNT_002") || code.startsWith("STRATEGY_002") ||
				code.startsWith("ORDER_004")) {
			return HttpStatus.CONFLICT;
		}

		if (code.startsWith("RISK_")) {
			return HttpStatus.FORBIDDEN;
		}

		if (code.startsWith("ORDER_") || code.startsWith("SIGNAL_")) {
			return HttpStatus.BAD_REQUEST;
		}

		if (code.startsWith("BROKER_")) {
			return HttpStatus.SERVICE_UNAVAILABLE;
		}

		return HttpStatus.INTERNAL_SERVER_ERROR;
	}
}
