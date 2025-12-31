-- V1: 계좌 관련 테이블

CREATE TABLE accounts (
  account_id CHAR(26) PRIMARY KEY COMMENT '내부 계좌 ID (ULID)',
  broker VARCHAR(16) NOT NULL COMMENT '브로커 (KIS)',
  environment VARCHAR(16) NOT NULL COMMENT 'PAPER/LIVE/BACKTEST',
  cano VARCHAR(16) NOT NULL COMMENT '계좌번호 앞 8자리',
  acnt_prdt_cd VARCHAR(8) NOT NULL COMMENT '계좌상품코드',
  status VARCHAR(16) NOT NULL COMMENT 'ACTIVE/INACTIVE/SUSPENDED',
  alias VARCHAR(64) COMMENT '별칭',
  created_at DATETIME(3) NOT NULL COMMENT '생성 시각',
  updated_at DATETIME(3) NOT NULL COMMENT '수정 시각',
  UNIQUE KEY uq_accounts_broker_env_cano (broker, environment, cano, acnt_prdt_cd),
  KEY idx_accounts_env_status (environment, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='계좌';

CREATE TABLE account_permissions (
  account_id CHAR(26) PRIMARY KEY COMMENT '계좌 ID',
  trade_buy TINYINT(1) NOT NULL DEFAULT 0 COMMENT '매수 허용',
  trade_sell TINYINT(1) NOT NULL DEFAULT 0 COMMENT '매도 허용',
  auto_trade TINYINT(1) NOT NULL DEFAULT 0 COMMENT '자동매매 허용',
  manual_trade TINYINT(1) NOT NULL DEFAULT 0 COMMENT '수동매매 허용',
  paper_only TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'PAPER 전용 (1이면 LIVE 차단)',
  updated_at DATETIME(3) NOT NULL COMMENT '수정 시각',
  CONSTRAINT fk_account_permissions_account
    FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='계좌 권한';

CREATE TABLE broker_tokens (
  token_id CHAR(26) PRIMARY KEY COMMENT 'Token ID (ULID)',
  broker VARCHAR(16) NOT NULL COMMENT '브로커 (KIS)',
  environment VARCHAR(16) NOT NULL COMMENT 'PAPER/LIVE',
  access_token TEXT NOT NULL COMMENT 'Access Token',
  issued_at DATETIME(3) NOT NULL COMMENT '발급 시각',
  expires_at DATETIME(3) NOT NULL COMMENT '만료 시각',
  created_at DATETIME(3) NOT NULL COMMENT '생성 시각',
  KEY idx_broker_tokens_env_expires (broker, environment, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='브로커 토큰';
