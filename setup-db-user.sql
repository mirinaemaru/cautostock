-- Setup trading_user account for the project
-- Run this script with: mysql -u root -p < setup-db-user.sql

-- Create user if not exists
CREATE USER IF NOT EXISTS 'trading_user'@'localhost' IDENTIFIED BY 'trading_pass';

-- Grant privileges on trading_mvp database
GRANT ALL PRIVILEGES ON trading_mvp.* TO 'trading_user'@'localhost';

-- Apply changes
FLUSH PRIVILEGES;

-- Verify
SELECT user, host FROM mysql.user WHERE user = 'trading_user';
SHOW GRANTS FOR 'trading_user'@'localhost';
