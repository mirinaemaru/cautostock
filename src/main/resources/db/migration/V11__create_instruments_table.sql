-- V11: Create instruments master table for KOSPI/KOSDAQ stocks
-- Stores instrument metadata, trading rules, and market status

CREATE TABLE instruments (
    -- Primary identifier
    symbol VARCHAR(16) PRIMARY KEY COMMENT '종목코드 (6자리 KIS 코드, e.g., 005930)',

    -- Market classification
    market VARCHAR(10) NOT NULL COMMENT '시장구분: KOSPI, KOSDAQ',

    -- Naming
    name_kr VARCHAR(100) NOT NULL COMMENT '한글 종목명',
    name_en VARCHAR(100) COMMENT '영문 종목명',

    -- Classification
    sector_code VARCHAR(10) COMMENT '섹터코드 (WICS 기준)',
    industry VARCHAR(50) COMMENT '업종 (예: 반도체, 자동차, 금융)',

    -- Trading rules
    tick_size DECIMAL(15,2) NOT NULL DEFAULT 1.00 COMMENT '호가단위 (최소가격변동단위)',
    lot_size INT NOT NULL DEFAULT 1 COMMENT '거래단위 (최소거래수량)',

    -- Listing status
    listing_date DATE COMMENT '상장일',
    delisting_date DATE COMMENT '폐지일 (NULL = 현재 상장)',

    -- Status management
    status VARCHAR(20) NOT NULL DEFAULT 'LISTED' COMMENT '상태: LISTED, DELISTED, SUSPENDED, UNDER_SUPERVISION',
    tradable BOOLEAN NOT NULL DEFAULT TRUE COMMENT '거래 가능 여부',
    halted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '거래 정지 여부 (일시정지)',

    -- Metadata
    updated_at DATETIME(3) NOT NULL COMMENT '마지막 업데이트 시각',
    created_at DATETIME(3) NOT NULL COMMENT '최초 생성 시각',

    -- Indexes for fast queries
    INDEX idx_instruments_market (market),
    INDEX idx_instruments_status (status),
    INDEX idx_instruments_tradable (tradable),
    INDEX idx_instruments_market_tradable (market, tradable),
    INDEX idx_instruments_updated (updated_at)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='종목 마스터 (KOSPI/KOSDAQ)';

-- Add constraint for symbol format validation (6-digit Korean stock code)
ALTER TABLE instruments ADD CONSTRAINT chk_symbol_format
    CHECK (symbol REGEXP '^[0-9]{6}$');
