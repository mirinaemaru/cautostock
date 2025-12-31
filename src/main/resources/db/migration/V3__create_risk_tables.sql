-- V3: 리스크 관련 테이블

CREATE TABLE risk_rules (
  risk_rules_id CHAR(26) PRIMARY KEY COMMENT '리스크 룰 ID (ULID)',
  scope VARCHAR(16) NOT NULL COMMENT 'GLOBAL/ACCOUNT',
  account_id CHAR(26) COMMENT '계좌 ID (scope=ACCOUNT 일 때)',
  rules_json JSON NOT NULL COMMENT '룰 묶음',
  updated_at DATETIME(3) NOT NULL COMMENT '갱신 시각',
  UNIQUE KEY uq_risk_rules_scope_account (scope, account_id),
  KEY idx_risk_rules_scope (scope),
  CONSTRAINT fk_risk_rules_account
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='리스크 룰';

CREATE TABLE risk_state (
  risk_state_id CHAR(26) PRIMARY KEY COMMENT '리스크 상태 ID (ULID)',
  scope VARCHAR(16) NOT NULL COMMENT 'GLOBAL/ACCOUNT',
  account_id CHAR(26) COMMENT '계좌 ID',
  kill_switch_status VARCHAR(8) NOT NULL DEFAULT 'OFF' COMMENT 'OFF/ARMED/ON',
  kill_switch_reason VARCHAR(64) COMMENT 'MANUAL/DAILY_LOSS_LIMIT 등',
  daily_pnl DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '일중 손익',
  exposure DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '노출',
  consecutive_order_failures INT NOT NULL DEFAULT 0 COMMENT '연속 실패 횟수',
  updated_at DATETIME(3) NOT NULL COMMENT '갱신 시각',
  UNIQUE KEY uq_risk_state_scope_account (scope, account_id),
  KEY idx_risk_state_killswitch_updated (kill_switch_status, updated_at),
  CONSTRAINT fk_risk_state_account
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='리스크 상태 / Kill Switch';
