-- V2: 전략 관련 테이블

CREATE TABLE strategies (
  strategy_id CHAR(26) PRIMARY KEY COMMENT '전략 ID (ULID)',
  name VARCHAR(80) NOT NULL UNIQUE COMMENT '전략명',
  description VARCHAR(255) COMMENT '설명',
  status VARCHAR(16) NOT NULL COMMENT 'ACTIVE/INACTIVE',
  mode VARCHAR(16) NOT NULL COMMENT 'PAPER/LIVE/BACKTEST',
  active_version_id CHAR(26) NOT NULL COMMENT '현재 활성 버전 ID',
  created_at DATETIME(3) NOT NULL COMMENT '생성 시각',
  updated_at DATETIME(3) NOT NULL COMMENT '수정 시각',
  KEY idx_strategies_status_mode (status, mode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='전략';

CREATE TABLE strategy_versions (
  strategy_version_id CHAR(26) PRIMARY KEY COMMENT '전략 버전 ID (ULID)',
  strategy_id CHAR(26) NOT NULL COMMENT '전략 ID',
  version_no INT NOT NULL COMMENT '버전 번호 (1부터 증가)',
  params_json JSON NOT NULL COMMENT '전략 파라미터',
  created_at DATETIME(3) NOT NULL COMMENT '생성 시각',
  UNIQUE KEY uq_strategy_versions_strategy_version (strategy_id, version_no),
  KEY idx_strategy_versions_strategy_created (strategy_id, created_at),
  CONSTRAINT fk_strategy_versions_strategy
    FOREIGN KEY (strategy_id) REFERENCES strategies(strategy_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='전략 버전';

CREATE TABLE signals (
  signal_id CHAR(26) PRIMARY KEY COMMENT '신호 ID (ULID)',
  strategy_id CHAR(26) NOT NULL COMMENT '전략 ID',
  strategy_version_id CHAR(26) NOT NULL COMMENT '전략 버전 ID',
  account_id CHAR(26) COMMENT '계좌 ID (옵션)',
  symbol VARCHAR(16) NOT NULL COMMENT '종목코드',
  signal_type VARCHAR(8) NOT NULL COMMENT 'BUY/SELL/HOLD',
  target_type VARCHAR(8) NOT NULL COMMENT 'QTY/WEIGHT',
  target_value DECIMAL(18,6) NOT NULL COMMENT '목표 값',
  ttl_seconds INT NOT NULL COMMENT 'TTL (초)',
  reason VARCHAR(255) COMMENT 'HOLD 사유',
  indicators_json JSON COMMENT '지표 스냅샷',
  correlation_id VARCHAR(64) COMMENT 'runId 등 상관관계 ID',
  created_at DATETIME(3) NOT NULL COMMENT '생성 시각',
  KEY idx_signals_symbol_created (symbol, created_at),
  KEY idx_signals_strategy_created (strategy_id, created_at),
  KEY idx_signals_account_created (account_id, created_at),
  CONSTRAINT fk_signals_strategy
    FOREIGN KEY (strategy_id) REFERENCES strategies(strategy_id),
  CONSTRAINT fk_signals_account
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='신호';
