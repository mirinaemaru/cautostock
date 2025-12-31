package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "strategy_versions")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StrategyVersionEntity {

	@Id
	@Column(name = "strategy_version_id", columnDefinition = "CHAR(26)")
	private String strategyVersionId;

	@Column(name = "strategy_id", columnDefinition = "CHAR(26)", nullable = false)
	private String strategyId;

	@Column(name = "version_no", nullable = false)
	private Integer versionNo;

	@Column(name = "params_json", columnDefinition = "JSON", nullable = false)
	private String paramsJson;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
