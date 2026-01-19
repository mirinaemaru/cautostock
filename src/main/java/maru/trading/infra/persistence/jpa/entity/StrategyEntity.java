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

	// ========== 자동매매 설정 컬럼 ==========

	// 거래 설정
	@Column(name = "account_id", columnDefinition = "CHAR(26)")
	private String accountId;

	@Column(name = "asset_type", length = 16)
	private String assetType;

	@Column(name = "symbol", length = 32)
	private String symbol;

	// 진입/청산 조건 (JSON TEXT)
	@Column(name = "entry_conditions", columnDefinition = "TEXT")
	private String entryConditions;

	@Column(name = "exit_conditions", columnDefinition = "TEXT")
	private String exitConditions;

	// 리스크 관리
	@Column(name = "stop_loss_type", length = 16)
	private String stopLossType;

	@Column(name = "stop_loss_value")
	private Double stopLossValue;

	@Column(name = "take_profit_type", length = 16)
	private String takeProfitType;

	@Column(name = "take_profit_value")
	private Double takeProfitValue;

	// 포지션 크기
	@Column(name = "position_size_type", length = 16)
	private String positionSizeType;

	@Column(name = "position_size_value")
	private Double positionSizeValue;

	@Column(name = "max_positions")
	private Integer maxPositions;

	// 소프트 삭제 플래그
	@Column(name = "delyn", length = 1, nullable = false)
	@Builder.Default
	private String delyn = "N";

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

	public void setActiveVersionId(String versionId) {
		this.activeVersionId = versionId;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setMode(Environment mode) {
		this.mode = mode;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	// ========== 자동매매 설정 Setter ==========

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public void setAssetType(String assetType) {
		this.assetType = assetType;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public void setEntryConditions(String entryConditions) {
		this.entryConditions = entryConditions;
	}

	public void setExitConditions(String exitConditions) {
		this.exitConditions = exitConditions;
	}

	public void setStopLossType(String stopLossType) {
		this.stopLossType = stopLossType;
	}

	public void setStopLossValue(Double stopLossValue) {
		this.stopLossValue = stopLossValue;
	}

	public void setTakeProfitType(String takeProfitType) {
		this.takeProfitType = takeProfitType;
	}

	public void setTakeProfitValue(Double takeProfitValue) {
		this.takeProfitValue = takeProfitValue;
	}

	public void setPositionSizeType(String positionSizeType) {
		this.positionSizeType = positionSizeType;
	}

	public void setPositionSizeValue(Double positionSizeValue) {
		this.positionSizeValue = positionSizeValue;
	}

	public void setMaxPositions(Integer maxPositions) {
		this.maxPositions = maxPositions;
	}

	// 소프트 삭제
	public void markDeleted() {
		this.delyn = "Y";
	}

	public boolean isDeleted() {
		return "Y".equals(this.delyn);
	}
}
