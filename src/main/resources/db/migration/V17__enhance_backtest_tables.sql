-- V17: Enhance backtest tables for advanced features
-- Adds optimization, walk-forward, async jobs, and monte carlo support

-- ==========================================
-- Optimization Runs Table
-- ==========================================
CREATE TABLE IF NOT EXISTS optimization_runs (
    optimization_id CHAR(26) NOT NULL COMMENT 'ULID',
    backtest_config JSON NOT NULL COMMENT 'Base backtest configuration',
    parameter_ranges JSON NOT NULL COMMENT 'Parameter search ranges',
    method VARCHAR(32) NOT NULL DEFAULT 'GRID' COMMENT 'GRID, RANDOM, BAYESIAN',
    objective VARCHAR(32) NOT NULL DEFAULT 'SHARPE' COMMENT 'SHARPE, SORTINO, RETURN, PROFIT_FACTOR',
    best_parameters JSON COMMENT 'Best found parameters',
    best_objective_value DECIMAL(18,6) COMMENT 'Best objective metric value',
    total_iterations INT DEFAULT 0 COMMENT 'Total optimization iterations',
    completed_iterations INT DEFAULT 0 COMMENT 'Completed iterations',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, RUNNING, COMPLETED, FAILED, CANCELLED',
    progress_percent INT DEFAULT 0 COMMENT 'Progress percentage 0-100',
    error_message TEXT COMMENT 'Error message if failed',
    started_at DATETIME(3) COMMENT 'Start timestamp',
    completed_at DATETIME(3) COMMENT 'Completion timestamp',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (optimization_id),
    INDEX idx_opt_status (status),
    INDEX idx_opt_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Parameter optimization runs';

-- ==========================================
-- Walk-Forward Analysis Runs Table
-- ==========================================
CREATE TABLE IF NOT EXISTS walkforward_runs (
    walkforward_id CHAR(26) NOT NULL COMMENT 'ULID',
    base_config JSON NOT NULL COMMENT 'Base backtest configuration',
    mode VARCHAR(16) NOT NULL DEFAULT 'ROLLING' COMMENT 'ROLLING, ANCHORED',
    in_sample_days INT NOT NULL COMMENT 'In-sample period days',
    out_of_sample_days INT NOT NULL COMMENT 'Out-of-sample period days',
    total_windows INT DEFAULT 0 COMMENT 'Total analysis windows',
    completed_windows INT DEFAULT 0 COMMENT 'Completed windows',
    combined_oos_return DECIMAL(18,6) COMMENT 'Combined OOS return',
    avg_oos_sharpe DECIMAL(10,6) COMMENT 'Average OOS Sharpe ratio',
    stability_score DECIMAL(10,6) COMMENT 'Parameter stability score',
    window_results JSON COMMENT 'Per-window results array',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, RUNNING, COMPLETED, FAILED, CANCELLED',
    progress_percent INT DEFAULT 0 COMMENT 'Progress percentage 0-100',
    error_message TEXT COMMENT 'Error message if failed',
    started_at DATETIME(3) COMMENT 'Start timestamp',
    completed_at DATETIME(3) COMMENT 'Completion timestamp',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (walkforward_id),
    INDEX idx_wf_status (status),
    INDEX idx_wf_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Walk-forward analysis runs';

-- ==========================================
-- Backtest Jobs Table (Async Execution)
-- ==========================================
CREATE TABLE IF NOT EXISTS backtest_jobs (
    job_id CHAR(26) NOT NULL COMMENT 'ULID',
    job_type VARCHAR(32) NOT NULL COMMENT 'BACKTEST, OPTIMIZATION, WALKFORWARD, PORTFOLIO, MONTE_CARLO',
    related_id CHAR(26) COMMENT 'Related run ID (backtest_id, optimization_id, etc.)',
    status VARCHAR(16) NOT NULL DEFAULT 'QUEUED' COMMENT 'QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED',
    progress_percent INT DEFAULT 0 COMMENT 'Progress percentage 0-100',
    current_phase VARCHAR(64) COMMENT 'Current execution phase description',
    config JSON NOT NULL COMMENT 'Job configuration',
    result_summary JSON COMMENT 'Result summary when completed',
    error_message TEXT COMMENT 'Error message if failed',
    queued_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'When job was queued',
    started_at DATETIME(3) COMMENT 'When execution started',
    completed_at DATETIME(3) COMMENT 'When execution completed',
    PRIMARY KEY (job_id),
    INDEX idx_job_type (job_type),
    INDEX idx_job_status (status),
    INDEX idx_job_related (related_id),
    INDEX idx_job_queued (queued_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Async backtest job queue';

-- ==========================================
-- Monte Carlo Simulation Runs Table
-- ==========================================
CREATE TABLE IF NOT EXISTS monte_carlo_runs (
    monte_carlo_id CHAR(26) NOT NULL COMMENT 'ULID',
    base_backtest_id CHAR(26) NOT NULL COMMENT 'Reference backtest run',
    num_simulations INT NOT NULL DEFAULT 1000 COMMENT 'Number of simulations',
    method VARCHAR(32) NOT NULL DEFAULT 'BOOTSTRAP' COMMENT 'BOOTSTRAP, PERMUTATION, RANDOM_ENTRY',
    confidence_level DECIMAL(5,4) NOT NULL DEFAULT 0.95 COMMENT 'Confidence level (0-1)',
    mean_return DECIMAL(18,6) COMMENT 'Mean simulated return',
    std_return DECIMAL(18,6) COMMENT 'Standard deviation of returns',
    var_95 DECIMAL(18,6) COMMENT 'Value at Risk (95%)',
    cvar_95 DECIMAL(18,6) COMMENT 'Conditional VaR (95%)',
    probability_of_profit DECIMAL(8,6) COMMENT 'Probability of profit (0-1)',
    median_return DECIMAL(18,6) COMMENT 'Median return',
    percentile_5 DECIMAL(18,6) COMMENT '5th percentile return',
    percentile_95 DECIMAL(18,6) COMMENT '95th percentile return',
    max_drawdown_mean DECIMAL(18,6) COMMENT 'Mean max drawdown',
    max_drawdown_worst DECIMAL(18,6) COMMENT 'Worst case max drawdown',
    return_distribution JSON COMMENT 'Return distribution histogram',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, RUNNING, COMPLETED, FAILED',
    progress_percent INT DEFAULT 0 COMMENT 'Progress percentage 0-100',
    error_message TEXT COMMENT 'Error message if failed',
    started_at DATETIME(3) COMMENT 'Start timestamp',
    completed_at DATETIME(3) COMMENT 'Completion timestamp',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (monte_carlo_id),
    INDEX idx_mc_backtest (base_backtest_id),
    INDEX idx_mc_status (status),
    INDEX idx_mc_created (created_at),
    CONSTRAINT fk_mc_backtest FOREIGN KEY (base_backtest_id)
        REFERENCES backtest_runs(backtest_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Monte Carlo simulation runs';

-- ==========================================
-- Portfolio Backtest Runs Table
-- ==========================================
CREATE TABLE IF NOT EXISTS portfolio_backtest_runs (
    portfolio_backtest_id CHAR(26) NOT NULL COMMENT 'ULID',
    config JSON NOT NULL COMMENT 'Portfolio backtest configuration',
    symbols JSON NOT NULL COMMENT 'List of symbols with weights',
    rebalance_frequency VARCHAR(16) COMMENT 'DAILY, WEEKLY, MONTHLY, QUARTERLY',
    total_return DECIMAL(18,6) COMMENT 'Total portfolio return',
    sharpe_ratio DECIMAL(10,6) COMMENT 'Portfolio Sharpe ratio',
    max_drawdown DECIMAL(18,6) COMMENT 'Portfolio max drawdown',
    correlation_matrix JSON COMMENT 'Symbol correlation matrix',
    individual_results JSON COMMENT 'Per-symbol backtest results',
    combined_equity_curve JSON COMMENT 'Combined equity curve',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, RUNNING, COMPLETED, FAILED',
    progress_percent INT DEFAULT 0 COMMENT 'Progress percentage 0-100',
    error_message TEXT COMMENT 'Error message if failed',
    started_at DATETIME(3) COMMENT 'Start timestamp',
    completed_at DATETIME(3) COMMENT 'Completion timestamp',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (portfolio_backtest_id),
    INDEX idx_pb_status (status),
    INDEX idx_pb_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Portfolio backtest runs';

-- ==========================================
-- Add max_drawdown_duration to backtest_runs
-- ==========================================
ALTER TABLE backtest_runs
ADD COLUMN IF NOT EXISTS max_drawdown_duration_days INT DEFAULT 0 COMMENT 'Max drawdown duration in days';

-- ==========================================
-- Add strategy_type to backtest_runs
-- ==========================================
ALTER TABLE backtest_runs
ADD COLUMN IF NOT EXISTS strategy_type VARCHAR(32) DEFAULT 'MA_CROSSOVER' COMMENT 'Strategy type identifier';

-- ==========================================
-- Add data_source_config to backtest_runs
-- ==========================================
ALTER TABLE backtest_runs
ADD COLUMN IF NOT EXISTS data_source_config JSON COMMENT 'Data source configuration (DB, CSV, etc.)';
