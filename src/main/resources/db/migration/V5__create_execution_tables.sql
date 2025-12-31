-- V5: 체결/포지션/손익 관련 테이블

CREATE TABLE fills (
  fill_id CHAR(26) PRIMARY KEY COMMENT '체결 ID (ULID)',
  order_id CHAR(26) NOT NULL COMMENT '주문 ID',
  account_id CHAR(26) NOT NULL COMMENT '계좌 ID',
  broker_order_no VARCHAR(32) COMMENT '브로커 주문번호',
  symbol VARCHAR(16) NOT NULL COMMENT '종목코드',
  side VARCHAR(8) NOT NULL COMMENT 'BUY/SELL',
  fill_price DECIMAL(18,2) NOT NULL COMMENT '체결가',
  fill_qty DECIMAL(18,6) NOT NULL COMMENT '체결 수량',
  fee DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '수수료',
  tax DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '세금',
  fill_ts DATETIME(3) NOT NULL COMMENT '체결 시각 (브로커 기준)',
  created_at DATETIME(3) NOT NULL COMMENT '저장 시각',
  KEY idx_fills_order_fillts (order_id, fill_ts),
  KEY idx_fills_account_symbol_fillts (account_id, symbol, fill_ts),
  KEY idx_fills_broker_no (broker_order_no),
  CONSTRAINT fk_fills_order
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
  CONSTRAINT fk_fills_account
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='체결';

CREATE TABLE positions (
  position_id CHAR(26) PRIMARY KEY COMMENT '포지션 ID (ULID)',
  account_id CHAR(26) NOT NULL COMMENT '계좌 ID',
  symbol VARCHAR(16) NOT NULL COMMENT '종목코드',
  qty DECIMAL(18,6) NOT NULL DEFAULT 0 COMMENT '보유 수량',
  avg_price DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '평단',
  realized_pnl DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '누적 실현 손익',
  updated_at DATETIME(3) NOT NULL COMMENT '갱신 시각',
  UNIQUE KEY uq_positions_account_symbol (account_id, symbol),
  KEY idx_positions_account_updated (account_id, updated_at),
  CONSTRAINT fk_positions_account
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='포지션';

CREATE TABLE pnl_ledger (
  ledger_id CHAR(26) PRIMARY KEY COMMENT '원장 ID (ULID)',
  account_id CHAR(26) NOT NULL COMMENT '계좌 ID',
  symbol VARCHAR(16) COMMENT '종목코드 (계좌 레벨이면 NULL)',
  event_type VARCHAR(16) NOT NULL COMMENT 'FILL/ADJUST/FEE/TAX',
  amount DECIMAL(18,2) NOT NULL COMMENT '금액 (+/-)',
  ref_id CHAR(26) COMMENT 'order_id 또는 fill_id',
  event_ts DATETIME(3) NOT NULL COMMENT '발생 시각',
  created_at DATETIME(3) NOT NULL COMMENT '저장 시각',
  KEY idx_pnl_ledger_account_eventts (account_id, event_ts),
  KEY idx_pnl_ledger_ref (ref_id),
  CONSTRAINT fk_pnl_ledger_account
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='손익 원장';

CREATE TABLE portfolio_snapshots (
  snapshot_id CHAR(26) PRIMARY KEY COMMENT '스냅샷 ID (ULID)',
  account_id CHAR(26) NOT NULL COMMENT '계좌 ID',
  total_value DECIMAL(18,2) NOT NULL COMMENT '총 자산',
  cash DECIMAL(18,2) NOT NULL COMMENT '현금',
  realized_pnl DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '실현 손익',
  unrealized_pnl DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '미실현 손익',
  snapshot_ts DATETIME(3) NOT NULL COMMENT '스냅샷 시각',
  created_at DATETIME(3) NOT NULL COMMENT '저장 시각',
  KEY idx_portfolio_snapshots_account_snapshotts (account_id, snapshot_ts),
  CONSTRAINT fk_portfolio_snapshots_account
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='포트폴리오 스냅샷';
