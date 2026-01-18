-- V15: 자동매매 설정 컬럼 추가

-- 거래 설정
ALTER TABLE strategies ADD COLUMN account_id CHAR(26) COMMENT '매매 계좌 ID';
ALTER TABLE strategies ADD COLUMN asset_type VARCHAR(16) COMMENT '자산 유형 (STOCK/FUTURES/CRYPTO)';
ALTER TABLE strategies ADD COLUMN symbol VARCHAR(32) COMMENT '거래 종목';

-- 진입/청산 조건 (JSON TEXT)
ALTER TABLE strategies ADD COLUMN entry_conditions TEXT COMMENT '진입 조건 JSON';
ALTER TABLE strategies ADD COLUMN exit_conditions TEXT COMMENT '청산 조건 JSON';

-- 리스크 관리
ALTER TABLE strategies ADD COLUMN stop_loss_type VARCHAR(16) COMMENT '손절 유형 (PERCENT/FIXED)';
ALTER TABLE strategies ADD COLUMN stop_loss_value DOUBLE COMMENT '손절 값';
ALTER TABLE strategies ADD COLUMN take_profit_type VARCHAR(16) COMMENT '익절 유형 (PERCENT/FIXED)';
ALTER TABLE strategies ADD COLUMN take_profit_value DOUBLE COMMENT '익절 값';

-- 포지션 크기
ALTER TABLE strategies ADD COLUMN position_size_type VARCHAR(16) COMMENT '포지션 크기 유형 (PERCENT/FIXED)';
ALTER TABLE strategies ADD COLUMN position_size_value DOUBLE COMMENT '포지션 크기 값';
ALTER TABLE strategies ADD COLUMN max_positions INT COMMENT '최대 포지션 수';

-- 인덱스 추가
ALTER TABLE strategies ADD INDEX idx_strategies_account_id (account_id);
ALTER TABLE strategies ADD INDEX idx_strategies_symbol (symbol);
