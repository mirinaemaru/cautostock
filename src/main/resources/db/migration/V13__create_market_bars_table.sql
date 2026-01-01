-- V13: Create market_bars table for OHLCV data storage

CREATE TABLE market_bars (
    bar_id          CHAR(26)       NOT NULL,
    symbol          VARCHAR(16)    NOT NULL,
    timeframe       VARCHAR(8)     NOT NULL,
    open_price      DECIMAL(18,4)  NOT NULL,
    high_price      DECIMAL(18,4)  NOT NULL,
    low_price       DECIMAL(18,4)  NOT NULL,
    close_price     DECIMAL(18,4)  NOT NULL,
    volume          BIGINT         NOT NULL,
    bar_timestamp   DATETIME(3)    NOT NULL,
    closed          BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at      DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (bar_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Unique index for preventing duplicate bars
CREATE UNIQUE INDEX idx_bars_symbol_timeframe_timestamp
    ON market_bars (symbol, timeframe, bar_timestamp);

-- Index for querying bars by symbol and timestamp
CREATE INDEX idx_bars_symbol_timestamp
    ON market_bars (symbol, bar_timestamp);
