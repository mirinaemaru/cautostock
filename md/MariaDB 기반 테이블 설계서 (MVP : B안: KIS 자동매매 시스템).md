MariaDB 기반 테이블 설계서 (MVP / B안: KIS 자동매매 시스템)

목적: MVP 구현에 필요한 핵심 엔티티(계좌/전략/신호/리스크/주문/체결/포지션/손익/운영/이벤트)를 MariaDB(InnoDB) 기준으로 설계한다.
원칙: 정합성(UNIQUE/FOREIGN KEY) + 조회 성능(인덱스) + 감사/이력(append-only).

0. 공통 설계 규칙
0.1 ID 규칙

PK는 문자열(ULID/UUID) 또는 BIGINT 중 택 1

본 문서는 문자열 ULID(CHAR(26)) 기준으로 제시

예: acct_..., ord_... 형태를 쓰면 VARCHAR(64) 권장

ULID만 쓸 경우 CHAR(26) 권장

0.2 시간/타임존

created_at, updated_at, occurred_at 등은 DATETIME(3) 사용(밀리초)

저장 기준은 KST 고정 또는 UTC 고정(운영 정책으로 통일)

0.3 JSON 컬럼

MariaDB 10.2+에서는 JSON이 LONGTEXT + validation 형태일 수 있음

MVP에서는 JSON 타입(또는 LONGTEXT) 사용 가능

인덱스가 필요하면 생성 컬럼(generated column) 로 추출하여 인덱스 생성

0.4 엔진/문자셋

ENGINE = InnoDB

CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci(가능한 경우) 또는 utf8mb4_general_ci

1. 테이블 목록(요약)
Core

accounts

account_permissions

broker_tokens

strategies

strategy_versions

signals

risk_rules

risk_state

orders

order_status_history

fills

positions

pnl_ledger

portfolio_snapshots

Ops / Observability

ops_audit_log

alert_log

Event / Integration (Outbox)

event_outbox

2. 테이블 상세 설계
2.1 accounts (계좌)
목적

KIS 계좌(모의/실전) 등록 및 상태 관리

컬럼
컬럼	타입	NULL	키	설명
account_id	CHAR(26)	N	PK	내부 계좌 ID(ULID)
broker	VARCHAR(16)	N		KIS
environment	VARCHAR(16)	N		PAPER / LIVE
cano	VARCHAR(16)	N		계좌번호 앞 8자리
acnt_prdt_cd	VARCHAR(8)	N		계좌상품코드
status	VARCHAR(16)	N		ACTIVE/INACTIVE/SUSPENDED
alias	VARCHAR(64)	Y		별칭
created_at	DATETIME(3)	N		생성 시각
updated_at	DATETIME(3)	N		수정 시각
제약/인덱스

UNIQUE: (broker, environment, cano, acnt_prdt_cd)

INDEX: (environment, status)

2.2 account_permissions (계좌 권한)
목적

자동매매 on/off, 매수/매도 허용 등 권한 플래그

컬럼
컬럼	타입	NULL	키	설명
account_id	CHAR(26)	N	PK, FK	accounts.account_id
trade_buy	TINYINT(1)	N		1/0
trade_sell	TINYINT(1)	N		1/0
auto_trade	TINYINT(1)	N		1/0
manual_trade	TINYINT(1)	N		1/0
paper_only	TINYINT(1)	N		1이면 LIVE 차단
updated_at	DATETIME(3)	N		수정 시각
제약/인덱스

FK: account_id → accounts(account_id) ON DELETE CASCADE

2.3 broker_tokens (브로커 토큰)
목적

KIS access_token 캐시/만료 관리

컬럼
컬럼	타입	NULL	키	설명
token_id	CHAR(26)	N	PK	ULID
broker	VARCHAR(16)	N		KIS
environment	VARCHAR(16)	N		PAPER/LIVE
access_token	TEXT	N		토큰
issued_at	DATETIME(3)	N		발급
expires_at	DATETIME(3)	N		만료
created_at	DATETIME(3)	N		생성
인덱스

INDEX: (broker, environment, expires_at)

2.4 strategies (전략)
목적

전략 메타와 활성 상태 관리

컬럼
컬럼	타입	NULL	키	설명
strategy_id	CHAR(26)	N	PK	ULID
name	VARCHAR(80)	N	UQ	전략명
description	VARCHAR(255)	Y		설명
status	VARCHAR(16)	N		ACTIVE/INACTIVE
mode	VARCHAR(16)	N		PAPER/LIVE/BACKTEST
active_version_id	CHAR(26)	N		현재 활성 버전
created_at	DATETIME(3)	N		생성
updated_at	DATETIME(3)	N		수정
제약/인덱스

UNIQUE: (name)

INDEX: (status, mode)

2.5 strategy_versions (전략 버전)
목적

파라미터 변경 시 버전 생성(히스토리)

컬럼
컬럼	타입	NULL	키	설명
strategy_version_id	CHAR(26)	N	PK	ULID
strategy_id	CHAR(26)	N	FK	strategies.strategy_id
version_no	INT	N		1부터 증가
params_json	JSON	N		전략 파라미터
created_at	DATETIME(3)	N		생성
제약/인덱스

UNIQUE: (strategy_id, version_no)

INDEX: (strategy_id, created_at)

FK: strategy_id → strategies(strategy_id)

2.6 signals (신호)
목적

전략 실행 결과(신호) 저장

컬럼
컬럼	타입	NULL	키	설명
signal_id	CHAR(26)	N	PK	ULID
strategy_id	CHAR(26)	N	FK	전략
strategy_version_id	CHAR(26)	N	FK	전략 버전
account_id	CHAR(26)	Y	FK	계좌(옵션)
symbol	VARCHAR(16)	N		종목
signal_type	VARCHAR(8)	N		BUY/SELL/HOLD
target_type	VARCHAR(8)	N		QTY/WEIGHT
target_value	DECIMAL(18,6)	N		목표 값
ttl_seconds	INT	N		TTL
reason	VARCHAR(255)	Y		HOLD 사유
indicators_json	JSON	Y		지표 스냅샷
correlation_id	VARCHAR(64)	Y		runId 등
created_at	DATETIME(3)	N		생성
인덱스

INDEX: (symbol, created_at)

INDEX: (strategy_id, created_at)

INDEX: (account_id, created_at)

2.7 risk_rules (리스크 룰)
목적

계좌별 또는 전역 리스크 룰 저장

컬럼
컬럼	타입	NULL	키	설명
risk_rules_id	CHAR(26)	N	PK	ULID
scope	VARCHAR(16)	N		GLOBAL / ACCOUNT
account_id	CHAR(26)	Y	FK	scope=ACCOUNT 일 때
rules_json	JSON	N		룰 묶음
updated_at	DATETIME(3)	N		갱신
제약/인덱스

UNIQUE: (scope, account_id) (GLOBAL은 account_id NULL 1건)

INDEX: (scope)

FK: account_id → accounts(account_id) (NULL 허용)

2.8 risk_state (리스크 상태 / Kill Switch 포함)
목적

일중 손익/노출/킬스위치 상태 등 현재 상태값

컬럼
컬럼	타입	NULL	키	설명
risk_state_id	CHAR(26)	N	PK	ULID
scope	VARCHAR(16)	N		GLOBAL/ACCOUNT
account_id	CHAR(26)	Y	FK	
kill_switch_status	VARCHAR(8)	N		OFF/ARMED/ON
kill_switch_reason	VARCHAR(64)	Y		MANUAL/DAILY_LOSS_LIMIT 등
daily_pnl	DECIMAL(18,2)	N		일중손익
exposure	DECIMAL(18,2)	N		노출
consecutive_order_failures	INT	N		연속 실패
updated_at	DATETIME(3)	N		갱신
제약/인덱스

UNIQUE: (scope, account_id)

INDEX: (kill_switch_status, updated_at)

2.9 orders (주문)
목적

주문 생성/전송/상태 추적(멱등키 포함)

컬럼
컬럼	타입	NULL	키	설명
order_id	CHAR(26)	N	PK	ULID
account_id	CHAR(26)	N	FK	계좌
strategy_id	CHAR(26)	Y	FK	전략(옵션)
strategy_version_id	CHAR(26)	Y	FK	버전(옵션)
signal_id	CHAR(26)	Y	FK	신호(옵션)
symbol	VARCHAR(16)	N		종목
side	VARCHAR(8)	N		BUY/SELL
order_type	VARCHAR(8)	N		LIMIT/MARKET
ord_dvsn	VARCHAR(4)	N		KIS ORD_DVSN
qty	DECIMAL(18,6)	N		주문수량
price	DECIMAL(18,2)	Y		지정가(시장가 null/0)
status	VARCHAR(16)	N		NEW/SENT/...
idempotency_key	VARCHAR(128)	N	UQ	멱등키
broker_order_no	VARCHAR(32)	Y		브로커 주문번호
reject_code	VARCHAR(64)	Y		거부/에러 코드
reject_message	VARCHAR(255)	Y		메시지
created_at	DATETIME(3)	N		생성
updated_at	DATETIME(3)	N		변경
제약/인덱스

UNIQUE: (idempotency_key)

INDEX: (account_id, created_at)

INDEX: (symbol, created_at)

INDEX: (status, updated_at)

INDEX: (broker_order_no)

FK: account_id → accounts(account_id)

2.10 order_status_history (주문 상태 이력)
목적

상태 전이를 append-only로 기록(감사/리플레이)

컬럼
컬럼	타입	NULL	키	설명
order_status_hist_id	CHAR(26)	N	PK	ULID
order_id	CHAR(26)	N	FK	orders.order_id
prev_status	VARCHAR(16)	Y		이전
new_status	VARCHAR(16)	N		신규
reason_code	VARCHAR(64)	Y		에러/거부
reason_message	VARCHAR(255)	Y		메시지
raw_json	JSON	Y		브로커 원문 요약
occurred_at	DATETIME(3)	N		발생 시각
인덱스

INDEX: (order_id, occurred_at)

INDEX: (new_status, occurred_at)

2.11 fills (체결)
목적

체결 누적(부분체결 포함) 저장

컬럼
컬럼	타입	NULL	키	설명
fill_id	CHAR(26)	N	PK	ULID
order_id	CHAR(26)	N	FK	내부 주문
account_id	CHAR(26)	N	FK	계좌
broker_order_no	VARCHAR(32)	Y		브로커 주문번호
symbol	VARCHAR(16)	N		종목
side	VARCHAR(8)	N		BUY/SELL
fill_price	DECIMAL(18,2)	N		체결가
fill_qty	DECIMAL(18,6)	N		체결수량
fee	DECIMAL(18,2)	N		수수료
tax	DECIMAL(18,2)	N		세금
fill_ts	DATETIME(3)	N		체결시각(브로커 기준)
created_at	DATETIME(3)	N		저장시각
제약/인덱스

INDEX: (order_id, fill_ts)

INDEX: (account_id, symbol, fill_ts)

INDEX: (broker_order_no)

(권장) 브로커가 제공하는 체결 고유키가 있으면 UNIQUE로 중복 방지

2.12 positions (포지션)
목적

종목별 보유 수량/평단/실현손익 상태

컬럼
컬럼	타입	NULL	키	설명
position_id	CHAR(26)	N	PK	ULID
account_id	CHAR(26)	N	FK	계좌
symbol	VARCHAR(16)	N		종목
qty	DECIMAL(18,6)	N		보유수량
avg_price	DECIMAL(18,2)	N		평단
realized_pnl	DECIMAL(18,2)	N		누적 실현손익
updated_at	DATETIME(3)	N		갱신
제약/인덱스

UNIQUE: (account_id, symbol)

INDEX: (account_id, updated_at)

2.13 pnl_ledger (손익 원장)
목적

손익/수수료/세금 등 회계성 이벤트를 append-only로 기록(재현 가능)

컬럼
컬럼	타입	NULL	키	설명
ledger_id	CHAR(26)	N	PK	ULID
account_id	CHAR(26)	N	FK	계좌
symbol	VARCHAR(16)	Y		종목(계좌 레벨이면 NULL 허용)
event_type	VARCHAR(16)	N		FILL/ADJUST/FEE/TAX
amount	DECIMAL(18,2)	N		금액(+/-)
ref_id	CHAR(26)	Y		order_id 또는 fill_id
event_ts	DATETIME(3)	N		발생
created_at	DATETIME(3)	N		저장
인덱스

INDEX: (account_id, event_ts)

INDEX: (ref_id)

2.14 portfolio_snapshots (포트폴리오 스냅샷)
목적

주기적 손익/자산 스냅샷 저장(대시보드/리포트)

컬럼
컬럼	타입	NULL	키	설명
snapshot_id	CHAR(26)	N	PK	ULID
account_id	CHAR(26)	N	FK	계좌
total_value	DECIMAL(18,2)	N		총자산
cash	DECIMAL(18,2)	N		현금
realized_pnl	DECIMAL(18,2)	N		실현
unrealized_pnl	DECIMAL(18,2)	N		미실현
snapshot_ts	DATETIME(3)	N		스냅샷 시각(정렬 기준)
created_at	DATETIME(3)	N		저장
인덱스

INDEX: (account_id, snapshot_ts)

2.15 ops_audit_log (운영 감사 로그)
목적

운영 행위(전략 on/off, kill switch, 설정 변경 등)를 추적

컬럼
컬럼	타입	NULL	키	설명
audit_id	CHAR(26)	N	PK	ULID
actor	VARCHAR(64)	N		admin/user/system
event_type	VARCHAR(32)	N		예: KILL_SWITCH_TOGGLE
scope	VARCHAR(16)	Y		GLOBAL/ACCOUNT
account_id	CHAR(26)	Y		
details_json	JSON	Y		상세
occurred_at	DATETIME(3)	N		발생
인덱스

INDEX: (event_type, occurred_at)

INDEX: (account_id, occurred_at)

2.16 alert_log (알림 로그)
목적

알림 발송 결과 기록(중복 방지/감사)

컬럼
컬럼	타입	NULL	키	설명
alert_id	CHAR(26)	N	PK	ULID
severity	VARCHAR(8)	N		INFO/WARN/CRIT
category	VARCHAR(16)	N		OPS/RISK/ORDER
channel	VARCHAR(16)	N		SLACK/EMAIL
message	VARCHAR(512)	N		본문
success	TINYINT(1)	N		성공 여부
related_event_id	CHAR(26)	Y		연관 이벤트
sent_at	DATETIME(3)	N		발송 시각
인덱스

INDEX: (severity, sent_at)

INDEX: (related_event_id)

2.17 event_outbox (Outbox 이벤트)
목적

트랜잭션 커밋과 이벤트 발행을 분리(신뢰성)

컬럼
컬럼	타입	NULL	키	설명
outbox_id	CHAR(26)	N	PK	ULID
event_id	VARCHAR(64)	N	UQ	이벤트 고유 ID
event_type	VARCHAR(64)	N		타입
occurred_at	DATETIME(3)	N		발생
payload_json	JSON	N		전체 envelope 또는 payload
published_at	DATETIME(3)	Y		발행 완료 시각
retry_count	INT	N		재시도 횟수
last_error	VARCHAR(512)	Y		마지막 에러
created_at	DATETIME(3)	N		생성
제약/인덱스

UNIQUE: (event_id)

INDEX: (published_at, occurred_at)

INDEX: (event_type, occurred_at)

3. DDL 템플릿 (MariaDB 예시)

아래 DDL은 “바로 생성 가능한” 형태의 기본 골격이다.
운영 환경/정책(UTC/KST, JSON 타입 지원, FK 정책)에 맞춰 조정.

CREATE TABLE accounts (
  account_id CHAR(26) PRIMARY KEY,
  broker VARCHAR(16) NOT NULL,
  environment VARCHAR(16) NOT NULL,
  cano VARCHAR(16) NOT NULL,
  acnt_prdt_cd VARCHAR(8) NOT NULL,
  status VARCHAR(16) NOT NULL,
  alias VARCHAR(64),
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uq_accounts_broker_env_cano (broker, environment, cano, acnt_prdt_cd),
  KEY idx_accounts_env_status (environment, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE orders (
  order_id CHAR(26) PRIMARY KEY,
  account_id CHAR(26) NOT NULL,
  strategy_id CHAR(26),
  strategy_version_id CHAR(26),
  signal_id CHAR(26),
  symbol VARCHAR(16) NOT NULL,
  side VARCHAR(8) NOT NULL,
  order_type VARCHAR(8) NOT NULL,
  ord_dvsn VARCHAR(4) NOT NULL,
  qty DECIMAL(18,6) NOT NULL,
  price DECIMAL(18,2),
  status VARCHAR(16) NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  broker_order_no VARCHAR(32),
  reject_code VARCHAR(64),
  reject_message VARCHAR(255),
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uq_orders_idem (idempotency_key),
  KEY idx_orders_acct_created (account_id, created_at),
  KEY idx_orders_symbol_created (symbol, created_at),
  KEY idx_orders_status_updated (status, updated_at),
  KEY idx_orders_broker_no (broker_order_no),
  CONSTRAINT fk_orders_account FOREIGN KEY (account_id) REFERENCES accounts(account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

4. 조회/운영 관점의 핵심 인덱스 체크

주문 타임라인: orders(account_id, created_at) + order_status_history(order_id, occurred_at)

체결 조회: fills(order_id, fill_ts) + fills(account_id, symbol, fill_ts)

포지션 조회: positions(account_id, symbol) UNIQUE

스냅샷: portfolio_snapshots(account_id, snapshot_ts)

Outbox 퍼블리셔: event_outbox(published_at, occurred_at)

5. MVP 이후(v2) 확장 포인트(테이블 관점)

종목 마스터/거래정지/호가단위: instruments

마켓데이터 저장: market_ticks, market_bars_1m

정정/분할/배당 등 기업행사: corporate_actions

다전략/다계좌 스케줄: strategy_assignments