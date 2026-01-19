-- strategies 테이블에 소프트 삭제 컬럼 추가
ALTER TABLE strategies ADD COLUMN delyn CHAR(1) NOT NULL DEFAULT 'N' COMMENT '삭제여부 (Y/N)';

-- 인덱스 추가 (삭제되지 않은 전략 조회 성능 향상)
CREATE INDEX idx_strategies_delyn ON strategies(delyn);
CREATE INDEX idx_strategies_status_delyn ON strategies(status, delyn);
