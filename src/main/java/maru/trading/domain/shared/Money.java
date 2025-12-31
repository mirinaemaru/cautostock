package maru.trading.domain.shared;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 금액 Value Object
 */
@Getter
@EqualsAndHashCode
public class Money {
	private final BigDecimal amount;

	private Money(BigDecimal amount) {
		if (amount == null) {
			throw new IllegalArgumentException("Amount cannot be null");
		}
		this.amount = amount.setScale(2, RoundingMode.HALF_UP);
	}

	public static Money of(BigDecimal amount) {
		return new Money(amount);
	}

	public static Money of(double amount) {
		return new Money(BigDecimal.valueOf(amount));
	}

	public static Money zero() {
		return new Money(BigDecimal.ZERO);
	}

	public Money add(Money other) {
		return new Money(this.amount.add(other.amount));
	}

	public Money subtract(Money other) {
		return new Money(this.amount.subtract(other.amount));
	}

	public Money multiply(BigDecimal multiplier) {
		return new Money(this.amount.multiply(multiplier));
	}

	public boolean isPositive() {
		return amount.compareTo(BigDecimal.ZERO) > 0;
	}

	public boolean isNegative() {
		return amount.compareTo(BigDecimal.ZERO) < 0;
	}

	public boolean isZero() {
		return amount.compareTo(BigDecimal.ZERO) == 0;
	}

	@Override
	public String toString() {
		return amount.toString();
	}
}
