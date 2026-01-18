-- V16: Create notification and execution history tables for new API features

-- 알림 테이블
CREATE TABLE IF NOT EXISTS notifications (
    notification_id CHAR(26) PRIMARY KEY,
    account_id CHAR(26),
    notification_type VARCHAR(32) NOT NULL,  -- TRADE, RISK, SYSTEM, ALERT
    severity VARCHAR(16) NOT NULL DEFAULT 'INFO',  -- INFO, WARNING, ERROR, CRITICAL
    title VARCHAR(255) NOT NULL,
    message TEXT,
    ref_type VARCHAR(32),  -- ORDER, FILL, STRATEGY, RISK_RULE
    ref_id CHAR(26),
    is_read CHAR(1) NOT NULL DEFAULT 'N',
    created_at DATETIME(3) NOT NULL,
    read_at DATETIME(3),

    INDEX idx_notifications_account (account_id, created_at DESC),
    INDEX idx_notifications_unread (account_id, is_read, created_at DESC),
    INDEX idx_notifications_type (notification_type, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 알림 설정 테이블
CREATE TABLE IF NOT EXISTS notification_settings (
    setting_id CHAR(26) PRIMARY KEY,
    account_id CHAR(26) UNIQUE,  -- NULL이면 전역 설정
    email_enabled CHAR(1) NOT NULL DEFAULT 'N',
    email_address VARCHAR(255),
    push_enabled CHAR(1) NOT NULL DEFAULT 'Y',
    trade_alerts CHAR(1) NOT NULL DEFAULT 'Y',
    risk_alerts CHAR(1) NOT NULL DEFAULT 'Y',
    system_alerts CHAR(1) NOT NULL DEFAULT 'Y',
    daily_summary CHAR(1) NOT NULL DEFAULT 'N',
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,

    INDEX idx_notification_settings_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 실행 히스토리 테이블
CREATE TABLE IF NOT EXISTS execution_history (
    execution_id CHAR(26) PRIMARY KEY,
    strategy_id CHAR(26) NOT NULL,
    account_id CHAR(26),
    execution_type VARCHAR(32) NOT NULL,  -- SIGNAL_GENERATED, ORDER_PLACED, ORDER_FILLED, RISK_CHECK, ERROR
    status VARCHAR(16) NOT NULL,  -- SUCCESS, FAILED, PENDING
    symbol VARCHAR(16),
    description TEXT,
    details JSON,
    error_message TEXT,
    execution_time_ms INT,
    created_at DATETIME(3) NOT NULL,

    INDEX idx_execution_history_strategy (strategy_id, created_at DESC),
    INDEX idx_execution_history_account (account_id, created_at DESC),
    INDEX idx_execution_history_type (execution_type, created_at DESC),
    INDEX idx_execution_history_status (status, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 일별 성과 요약 테이블 (성과 분석용)
CREATE TABLE IF NOT EXISTS daily_performance (
    performance_id CHAR(26) PRIMARY KEY,
    account_id CHAR(26) NOT NULL,
    strategy_id CHAR(26),
    trade_date DATE NOT NULL,
    total_trades INT NOT NULL DEFAULT 0,
    winning_trades INT NOT NULL DEFAULT 0,
    losing_trades INT NOT NULL DEFAULT 0,
    total_pnl DECIMAL(20, 4) NOT NULL DEFAULT 0,
    realized_pnl DECIMAL(20, 4) NOT NULL DEFAULT 0,
    unrealized_pnl DECIMAL(20, 4) NOT NULL DEFAULT 0,
    total_volume DECIMAL(20, 4) NOT NULL DEFAULT 0,
    total_fees DECIMAL(20, 4) NOT NULL DEFAULT 0,
    max_drawdown DECIMAL(10, 4),
    sharpe_ratio DECIMAL(10, 4),
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,

    UNIQUE KEY uk_daily_performance (account_id, strategy_id, trade_date),
    INDEX idx_daily_performance_date (trade_date DESC),
    INDEX idx_daily_performance_strategy (strategy_id, trade_date DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
