-- V6: 운영/감사/알림 관련 테이블

CREATE TABLE ops_audit_log (
  audit_id CHAR(26) PRIMARY KEY COMMENT '감사 ID (ULID)',
  actor VARCHAR(64) NOT NULL COMMENT '실행자 (admin/user/system)',
  event_type VARCHAR(32) NOT NULL COMMENT '이벤트 타입 (예: KILL_SWITCH_TOGGLE)',
  scope VARCHAR(16) COMMENT 'GLOBAL/ACCOUNT',
  account_id CHAR(26) COMMENT '계좌 ID',
  details_json JSON COMMENT '상세 정보',
  occurred_at DATETIME(3) NOT NULL COMMENT '발생 시각',
  KEY idx_ops_audit_log_eventtype_occurred (event_type, occurred_at),
  KEY idx_ops_audit_log_account_occurred (account_id, occurred_at),
  CONSTRAINT fk_ops_audit_log_account
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='운영 감사 로그';

CREATE TABLE alert_log (
  alert_id CHAR(26) PRIMARY KEY COMMENT '알림 ID (ULID)',
  severity VARCHAR(8) NOT NULL COMMENT 'INFO/WARN/CRIT',
  category VARCHAR(16) NOT NULL COMMENT 'OPS/RISK/ORDER',
  channel VARCHAR(16) NOT NULL COMMENT 'SLACK/EMAIL',
  message VARCHAR(512) NOT NULL COMMENT '알림 메시지',
  success TINYINT(1) NOT NULL COMMENT '발송 성공 여부',
  related_event_id CHAR(26) COMMENT '연관 이벤트 ID',
  sent_at DATETIME(3) NOT NULL COMMENT '발송 시각',
  KEY idx_alert_log_severity_sent (severity, sent_at),
  KEY idx_alert_log_related_event (related_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='알림 로그';

CREATE TABLE event_outbox (
  outbox_id CHAR(26) PRIMARY KEY COMMENT 'Outbox ID (ULID)',
  event_id VARCHAR(64) NOT NULL UNIQUE COMMENT '이벤트 고유 ID',
  event_type VARCHAR(64) NOT NULL COMMENT '이벤트 타입',
  occurred_at DATETIME(3) NOT NULL COMMENT '발생 시각',
  payload_json JSON NOT NULL COMMENT '이벤트 payload',
  published_at DATETIME(3) COMMENT '발행 완료 시각',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '재시도 횟수',
  last_error VARCHAR(512) COMMENT '마지막 에러',
  created_at DATETIME(3) NOT NULL COMMENT '생성 시각',
  KEY idx_event_outbox_published_occurred (published_at, occurred_at),
  KEY idx_event_outbox_eventtype_occurred (event_type, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='이벤트 Outbox';
