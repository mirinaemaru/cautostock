package maru.trading.infra.config;

import com.github.f4b6a3.ulid.UlidCreator;
import org.springframework.stereotype.Component;

/**
 * ULID 생성 유틸리티
 *
 * Supports both static methods (for backward compatibility with existing code)
 * and instance methods (for dependency injection in new code).
 */
@Component
public class UlidGenerator {

	// Instance methods (for dependency injection)
	public String generateInstance() {
		return UlidCreator.getUlid().toString();
	}

	public String generateWithPrefixInstance(String prefix) {
		return prefix + "_" + UlidCreator.getUlid().toString();
	}

	// Static methods (for backward compatibility)
	public static String generate() {
		return UlidCreator.getUlid().toString();
	}

	public static String generateWithPrefix(String prefix) {
		return prefix + "_" + UlidCreator.getUlid().toString();
	}
}
