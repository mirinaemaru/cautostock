package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.account.AccountStatus;
import maru.trading.domain.shared.Environment;

import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountEntity {

	@Id
	@Column(name = "account_id", columnDefinition = "CHAR(26)")
	private String accountId;

	@Column(name = "broker", length = 16, nullable = false)
	private String broker;

	@Enumerated(EnumType.STRING)
	@Column(name = "environment", length = 16, nullable = false)
	private Environment environment;

	@Column(name = "cano", length = 16, nullable = false)
	private String cano;

	@Column(name = "acnt_prdt_cd", length = 8, nullable = false)
	private String acntPrdtCd;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 16, nullable = false)
	private AccountStatus status;

	@Column(name = "alias", length = 64)
	private String alias;

	@Column(name = "delyn", length = 1, nullable = false, columnDefinition = "CHAR(1) DEFAULT 'N'")
	@Builder.Default
	private String delyn = "N";

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Column(name = "deleted_by", length = 64)
	private String deletedBy;

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

	public void updateStatus(AccountStatus newStatus) {
		this.status = newStatus;
	}

	public void updateAlias(String newAlias) {
		this.alias = newAlias;
	}

	public void softDelete() {
		this.delyn = "Y";
		this.status = AccountStatus.INACTIVE;
		this.deletedAt = LocalDateTime.now();
	}

	public void softDelete(String deletedBy) {
		this.delyn = "Y";
		this.status = AccountStatus.INACTIVE;
		this.deletedAt = LocalDateTime.now();
		this.deletedBy = deletedBy;
	}

	public boolean isDeleted() {
		return "Y".equals(this.delyn);
	}
}
