-- Add missing columns to signals table for signal execution tracking

ALTER TABLE signals
    ADD COLUMN executed_at DATETIME(3) NULL COMMENT '신호 실행 시각',
    ADD COLUMN expired BOOLEAN NOT NULL DEFAULT FALSE COMMENT '만료 여부';

-- Add index for expired signals query
CREATE INDEX idx_signals_expired ON signals(expired, created_at);
