package maru.trading.domain.shared;

import java.time.LocalDateTime;

/**
 * 도메인 이벤트 마커 인터페이스
 */
public interface DomainEvent {
	String getEventId();
	String getEventType();
	LocalDateTime getOccurredAt();
}
