-- V9: Create strategy_symbols mapping table
-- This table maps strategies to the symbols they should trade

CREATE TABLE strategy_symbols (
    strategy_symbol_id CHAR(26) PRIMARY KEY,
    strategy_id CHAR(26) NOT NULL,
    symbol VARCHAR(16) NOT NULL,
    account_id CHAR(26) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,

    -- Foreign key to strategies table
    CONSTRAINT fk_strategy_symbols_strategy FOREIGN KEY (strategy_id)
        REFERENCES strategies(strategy_id) ON DELETE CASCADE,

    -- Unique constraint: one strategy-symbol-account combination
    CONSTRAINT uk_strategy_symbol_account UNIQUE (strategy_id, symbol, account_id)
);

-- Index for querying symbols by strategy
CREATE INDEX idx_strategy_symbols_strategy_id ON strategy_symbols(strategy_id);

-- Index for querying strategies by symbol
CREATE INDEX idx_strategy_symbols_symbol ON strategy_symbols(symbol);

-- Index for active strategy-symbol mappings
CREATE INDEX idx_strategy_symbols_active ON strategy_symbols(is_active);
