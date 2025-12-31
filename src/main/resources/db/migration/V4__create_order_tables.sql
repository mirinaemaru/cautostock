-- V4: 주문 관련 테이블

CREATE TABLE orders (
  order_id CHAR(26) PRIMARY KEY COMMENT '주문 ID (ULID)',
  account_id CHAR(26) NOT NULL COMMENT '계좌 ID',
  strategy_id CHAR(26) COMMENT '전략 ID (옵션)',
  strategy_version_id CHAR(26) COMMENT '전략 버전 ID (옵션)',
  signal_id CHAR(26) COMMENT '신호 ID (옵션)',
  symbol VARCHAR(16) NOT NULL COMMENT '종목코드',
  side VARCHAR(8) NOT NULL COMMENT 'BUY/SELL',
  order_type VARCHAR(8) NOT NULL COMMENT 'LIMIT/MARKET',
  ord_dvsn VARCHAR(4) NOT NULL COMMENT 'KIS ORD_DVSN (00=지정가, 01=시장가)',
  qty DECIMAL(18,6) NOT NULL COMMENT '주문 수량',
  price DECIMAL(18,2) COMMENT '지정가 (시장가는 null/0)',
  status VARCHAR(16) NOT NULL COMMENT 'NEW/SENT/ACCEPTED/PART_FILLED/FILLED/CANCELLED/REJECTED/ERROR',
  idempotency_key VARCHAR(128) NOT NULL UNIQUE COMMENT '멱등키',
  broker_order_no VARCHAR(32) COMMENT '브로커 주문번호',
  reject_code VARCHAR(64) COMMENT '거부/에러 코드',
  reject_message VARCHAR(255) COMMENT '거부/에러 메시지',
  created_at DATETIME(3) NOT NULL COMMENT '생성 시각',
  updated_at DATETIME(3) NOT NULL COMMENT '변경 시각',
  KEY idx_orders_account_created (account_id, created_at),
  KEY idx_orders_symbol_created (symbol, created_at),
  KEY idx_orders_status_updated (status, updated_at),
  KEY idx_orders_broker_no (broker_order_no),
  CONSTRAINT fk_orders_account
    FOREIGN KEY (account_id) REFERENCES accounts(account_id),
  CONSTRAINT fk_orders_strategy
    FOREIGN KEY (strategy_id) REFERENCES strategies(strategy_id),
  CONSTRAINT fk_orders_signal
    FOREIGN KEY (signal_id) REFERENCES signals(signal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='주문';

CREATE TABLE order_status_history (
  order_status_hist_id CHAR(26) PRIMARY KEY COMMENT '주문 상태 이력 ID (ULID)',
  order_id CHAR(26) NOT NULL COMMENT '주문 ID',
  prev_status VARCHAR(16) COMMENT '이전 상태',
  new_status VARCHAR(16) NOT NULL COMMENT '새 상태',
  reason_code VARCHAR(64) COMMENT '에러/거부 코드',
  reason_message VARCHAR(255) COMMENT '에러/거부 메시지',
  raw_json JSON COMMENT '브로커 원문 요약',
  occurred_at DATETIME(3) NOT NULL COMMENT '발생 시각',
  KEY idx_order_status_history_order_occurred (order_id, occurred_at),
  KEY idx_order_status_history_status_occurred (new_status, occurred_at),
  CONSTRAINT fk_order_status_history_order
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='주문 상태 이력';
