-- =====================================================
-- V10: Create Backtest Tables (Phase 4)
-- =====================================================
-- Date: 2026-01-01
-- Purpose: Create tables for backtesting engine
-- Tables: historical_bars, backtest_runs, backtest_trades
-- =====================================================

-- =====================================================
-- 1. Historical Bars Table
-- =====================================================
-- Stores historical OHLCV data for backtesting
-- Different from bars table (real-time aggregated data)

CREATE TABLE historical_bars (
    bar_id CHAR(26) PRIMARY KEY COMMENT 'ULID',
    symbol VARCHAR(16) NOT NULL COMMENT 'Stock symbol',
    timeframe VARCHAR(8) NOT NULL COMMENT 'Timeframe: 1m, 5m, 1h, 1d',
    bar_timestamp DATETIME(3) NOT NULL COMMENT 'Bar opening time',

    -- OHLCV data
    open_price DECIMAL(18,4) NOT NULL COMMENT 'Opening price',
    high_price DECIMAL(18,4) NOT NULL COMMENT 'High price',
    low_price DECIMAL(18,4) NOT NULL COMMENT 'Low price',
    close_price DECIMAL(18,4) NOT NULL COMMENT 'Closing price',
    volume BIGINT NOT NULL COMMENT 'Trading volume',

    created_at DATETIME(3) NOT NULL COMMENT 'Record creation time',

    -- Constraints
    CONSTRAINT uk_symbol_timeframe_timestamp UNIQUE (symbol, timeframe, bar_timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Historical market bars for backtesting';

-- Indexes for fast querying
CREATE INDEX idx_symbol_timestamp ON historical_bars(symbol, bar_timestamp);
CREATE INDEX idx_timeframe ON historical_bars(timeframe);

-- =====================================================
-- 2. Backtest Runs Table
-- =====================================================
-- Stores backtest execution metadata and results

CREATE TABLE backtest_runs (
    backtest_id CHAR(26) PRIMARY KEY COMMENT 'ULID',
    strategy_id CHAR(26) NOT NULL COMMENT 'Strategy being tested',
    strategy_version_id CHAR(26) COMMENT 'Specific strategy version',

    -- Configuration
    start_date DATETIME(3) NOT NULL COMMENT 'Backtest start date',
    end_date DATETIME(3) NOT NULL COMMENT 'Backtest end date',
    symbols TEXT NOT NULL COMMENT 'Comma-separated symbols',
    timeframe VARCHAR(8) NOT NULL COMMENT 'Bar timeframe',
    initial_capital DECIMAL(18,2) NOT NULL COMMENT 'Initial capital',
    commission DECIMAL(8,6) NOT NULL DEFAULT 0.001 COMMENT 'Commission rate',
    slippage DECIMAL(8,6) NOT NULL DEFAULT 0.0005 COMMENT 'Slippage rate',
    strategy_params JSON COMMENT 'Strategy parameters',

    -- Execution metadata
    status VARCHAR(16) NOT NULL COMMENT 'RUNNING, COMPLETED, FAILED',
    started_at DATETIME(3) NOT NULL COMMENT 'Execution start time',
    completed_at DATETIME(3) COMMENT 'Execution end time',
    error_message TEXT COMMENT 'Error message if failed',

    -- Results summary
    final_capital DECIMAL(18,2) COMMENT 'Final capital after backtest',
    total_return DECIMAL(10,6) COMMENT 'Total return (%)',
    total_trades INT DEFAULT 0 COMMENT 'Number of trades',
    winning_trades INT DEFAULT 0 COMMENT 'Number of winning trades',
    losing_trades INT DEFAULT 0 COMMENT 'Number of losing trades',

    -- Performance metrics (JSON for extensibility)
    performance_metrics JSON COMMENT 'Detailed performance metrics',
    risk_metrics JSON COMMENT 'Risk analysis metrics',
    equity_curve JSON COMMENT 'Equity curve data points',

    created_at DATETIME(3) NOT NULL COMMENT 'Record creation time',
    updated_at DATETIME(3) NOT NULL COMMENT 'Last update time',

    INDEX idx_strategy_id (strategy_id),
    INDEX idx_status (status),
    INDEX idx_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Backtest execution runs';

-- =====================================================
-- 3. Backtest Trades Table
-- =====================================================
-- Stores individual trades executed during backtest

CREATE TABLE backtest_trades (
    trade_id CHAR(26) PRIMARY KEY COMMENT 'ULID',
    backtest_id CHAR(26) NOT NULL COMMENT 'Parent backtest run',
    symbol VARCHAR(16) NOT NULL COMMENT 'Stock symbol',

    -- Entry
    entry_time DATETIME(3) NOT NULL COMMENT 'Entry timestamp',
    entry_price DECIMAL(18,4) NOT NULL COMMENT 'Entry price',
    entry_qty DECIMAL(18,6) NOT NULL COMMENT 'Entry quantity',
    side VARCHAR(8) NOT NULL COMMENT 'BUY or SELL',

    -- Exit
    exit_time DATETIME(3) COMMENT 'Exit timestamp',
    exit_price DECIMAL(18,4) COMMENT 'Exit price',
    exit_qty DECIMAL(18,6) COMMENT 'Exit quantity',

    -- P&L
    gross_pnl DECIMAL(18,4) COMMENT 'Gross profit/loss',
    commission_paid DECIMAL(18,4) COMMENT 'Total commission paid',
    slippage_cost DECIMAL(18,4) COMMENT 'Slippage cost',
    net_pnl DECIMAL(18,4) COMMENT 'Net profit/loss',
    return_pct DECIMAL(10,6) COMMENT 'Return percentage',

    -- Trade metadata
    signal_reason VARCHAR(500) COMMENT 'Signal reason/description',
    status VARCHAR(16) NOT NULL COMMENT 'OPEN, CLOSED, PARTIAL',

    created_at DATETIME(3) NOT NULL COMMENT 'Record creation time',
    updated_at DATETIME(3) NOT NULL COMMENT 'Last update time',

    FOREIGN KEY (backtest_id) REFERENCES backtest_runs(backtest_id) ON DELETE CASCADE,
    INDEX idx_backtest_id (backtest_id),
    INDEX idx_symbol (symbol),
    INDEX idx_entry_time (entry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Individual trades during backtest';

-- =====================================================
-- End of V10 Migration
-- =====================================================
