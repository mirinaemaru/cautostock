-- V18: Infrastructure Improvements
-- 1. Soft delete audit columns
-- 2. Dead Letter Queue table
-- 3. Outbox index optimization

-- ============================================
-- 1. Soft Delete Audit Trail
-- ============================================

-- Add deleted_at and deleted_by to accounts for audit trail
ALTER TABLE accounts
  ADD COLUMN deleted_at DATETIME(3) NULL COMMENT '삭제 시각',
  ADD COLUMN deleted_by VARCHAR(64) NULL COMMENT '삭제 실행자';

-- Add index for deleted records query
CREATE INDEX idx_accounts_deleted_at ON accounts(deleted_at);

-- Add deleted_at and deleted_by to strategies for audit trail
ALTER TABLE strategies
  ADD COLUMN deleted_at DATETIME(3) NULL COMMENT '삭제 시각',
  ADD COLUMN deleted_by VARCHAR(64) NULL COMMENT '삭제 실행자';

-- Add index for deleted records query
CREATE INDEX idx_strategies_deleted_at ON strategies(deleted_at);

-- ============================================
-- 2. Dead Letter Queue (DLQ) Table
-- ============================================

CREATE TABLE event_dlq (
  dlq_id CHAR(26) PRIMARY KEY COMMENT 'DLQ ID (ULID)',
  original_event_id CHAR(26) NOT NULL COMMENT '원본 이벤트 ID',
  event_type VARCHAR(64) NOT NULL COMMENT '이벤트 타입',
  payload JSON NOT NULL COMMENT '이벤트 페이로드',
  failure_reason TEXT COMMENT '실패 사유',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '재시도 횟수',
  last_retry_at DATETIME(3) COMMENT '마지막 재시도 시각',
  reprocessed_at DATETIME(3) NULL COMMENT '재처리 완료 시각',
  created_at DATETIME(3) NOT NULL COMMENT '생성 시각',
  INDEX idx_dlq_type_created (event_type, created_at),
  INDEX idx_dlq_reprocessed (reprocessed_at),
  INDEX idx_dlq_original_event (original_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Dead Letter Queue - 실패 이벤트 저장소';

-- ============================================
-- 3. Outbox Index Optimization
-- ============================================

-- Add index for unpublished events with retry limit query
CREATE INDEX idx_outbox_unpublished_retry ON event_outbox(published_at, retry_count);

-- Add index for cleanup query (published events before date)
CREATE INDEX idx_outbox_published_created ON event_outbox(published_at, created_at);
