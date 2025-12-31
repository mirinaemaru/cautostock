package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.shared.Environment;

import java.time.LocalDateTime;

@Entity
@Table(name = "strategies")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StrategyEntity {

	@Id
	@Column(name = "strategy_id", columnDefinition = "CHAR(26)")
	private String strategyId;

	@Column(name = "name", length = 80, nullable = false, unique = true)
	private String name;

	@Column(name = "description", length = 255)
	private String description;

	@Column(name = "status", length = 16, nullable = false)
	private String status; // ACTIVE, INACTIVE

	@Enumerated(EnumType.STRING)
	@Column(name = "mode", length = 16, nullable = false)
	private Environment mode;

	@Column(name = "active_version_id", columnDefinition = "CHAR(26)", nullable = false)
	private String activeVersionId;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public void activate(String versionId) {
		this.status = "ACTIVE";
		this.activeVersionId = versionId;
	}

	public void deactivate() {
		this.status = "INACTIVE";
	}
}
