-- Add soft delete column to accounts table
ALTER TABLE accounts ADD COLUMN delyn CHAR(1) NOT NULL DEFAULT 'N' COMMENT '삭제여부 (Y/N)';

-- Add index for better query performance
CREATE INDEX idx_accounts_delyn ON accounts(delyn);

-- Add comment to the table
ALTER TABLE accounts COMMENT = '계좌 정보 (소프트 삭제 지원)';
