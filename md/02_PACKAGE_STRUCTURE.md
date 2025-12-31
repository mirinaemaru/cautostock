# 패키지/모듈 구조 설계 (Java / Spring 기준)
(B안 – KIS OpenAPI 연계 자동매매 시스템)

목표:
- 8대 기능(인증/데이터/전략/신호/리스크/주문/체결/운영)을 **모듈 단위로 분리**
- 증권사(KIS) 의존을 **Adapter 계층으로 격리**
- 실전/모의/백테스트를 **동일 도메인 로직**으로 재사용
- 장애/재시작/정합성 복구를 고려한 **이벤트 중심 구조**

---

## 1. 전체 아키텍처 스타일

- Layered + Hexagonal(Ports & Adapters) 혼합 추천
  - Domain: 순수 비즈니스 규칙(전략/신호/리스크/포지션/손익)
  - Application: 유스케이스(흐름 제어, 트랜잭션)
  - Adapters: KIS API, DB, 메시징, 알림 등 외부 의존
  - Interfaces: REST/CLI/Scheduler/WS Listener

---

## 2. 멀티 모듈(Gradle/Maven) 권장 구성

> MVP는 단일 모듈로도 시작 가능하지만, 장기적으로는 아래처럼 분리 추천

trading-system/
trading-api/ (Spring Boot App, REST/Batch/Scheduler)
trading-domain/ (Domain Model + Domain Service)
trading-application/ (UseCase, Orchestration)
trading-infrastructure/ (DB/JPA/MyBatis, Messaging, Cache)
trading-broker-kis/ (KIS REST/WS Adapter)
trading-backtest/ (Backtest Engine, Simulated Broker)
trading-common/ (공통 유틸, 에러코드, 시간/ID, 로깅)




### 모듈 책임
- `trading-domain`: 엔티티/값객체/도메인 서비스(평단/손익/리스크 규칙)
- `trading-application`: 신호→리스크→주문→체결 반영 유스케이스
- `trading-broker-kis`: KIS 전용 인증/REST/WS/DTO/매핑
- `trading-infrastructure`: DB, 트랜잭션, Outbox, 스케줄러 구현
- `trading-api`: Controller, Admin API, Health, Actuator, Web UI(선택)
- `trading-backtest`: 과거 데이터/체결 시뮬레이터/리포트

---

## 3. 최상위 패키지 네이밍(예시)

- 루트: `maru.trading`
- 모듈별 루트:
  - `maru.trading.domain`
  - `maru.trading.application`
  - `maru.trading.infra`
  - `maru.trading.broker.kis`
  - `maru.trading.api`
  - `maru.trading.backtest`

---

## 4. Domain 패키지 구조(핵심 비즈니스)

trading-domain
└─ maru.trading.domain
├─ account/
│ ├─ Account.java
│ ├─ Permission.java
│ └─ AccountService.java
├─ market/
│ ├─ Instrument.java
│ ├─ MarketTick.java
│ ├─ MarketBar.java
│ └─ MarketDataPolicy.java
├─ strategy/
│ ├─ Strategy.java
│ ├─ StrategyVersion.java
│ ├─ StrategyParams.java
│ └─ StrategyEngine.java (포트/인터페이스)
├─ signal/
│ ├─ Signal.java
│ ├─ SignalType.java
│ ├─ SignalDecision.java
│ └─ SignalPolicy.java (TTL/쿨다운/중복방지)
├─ risk/
│ ├─ RiskRule.java
│ ├─ RiskDecision.java
│ ├─ RiskEngine.java (Pre/In-Trade)
│ └─ KillSwitch.java
├─ order/
│ ├─ Order.java
│ ├─ OrderType.java (LIMIT/MARKET…)
│ ├─ OrderStatus.java
│ ├─ IdempotencyKey.java
│ └─ OrderPolicy.java (ORD_DVSN/ORD_UNPR 규칙)
├─ execution/
│ ├─ Fill.java
│ ├─ Position.java
│ ├─ PnlLedger.java
│ └─ PortfolioSnapshot.java
├─ ops/
│ ├─ Alert.java
│ ├─ Severity.java
│ └─ AuditEvent.java
└─ shared/
├─ Money.java
├─ Clock.java
├─ DomainEvent.java
└─ ErrorCode.java




> Domain 모듈은 Spring 의존 최소화(가능하면 없음)  
> 테스트가 쉬워지고 백테스트 재사용성이 커짐

---

## 5. Application 패키지 구조(유스케이스/흐름)

trading-application
└─ maru.trading.application
├─ ports/
│ ├─ broker/
│ │ ├─ BrokerClient.java (주문/취소/조회)
│ │ └─ BrokerStream.java (실시간 이벤트)
│ ├─ repo/
│ │ ├─ AccountRepository.java
│ │ ├─ OrderRepository.java
│ │ ├─ FillRepository.java
│ │ └─ MarketDataRepository.java
│ ├─ notify/
│ │ └─ Notifier.java
│ └─ time/
│ └─ TimeProvider.java
├─ usecase/
│ ├─ auth/
│ │ ├─ RefreshTokenUseCase.java
│ │ └─ IssueApprovalKeyUseCase.java
│ ├─ market/
│ │ ├─ SubscribeMarketDataUseCase.java
│ │ └─ BuildBarsUseCase.java
│ ├─ trading/
│ │ ├─ GenerateSignalUseCase.java
│ │ ├─ EvaluateRiskUseCase.java
│ │ ├─ PlaceOrderUseCase.java
│ │ ├─ CancelOrderUseCase.java
│ │ └─ SyncOrdersUseCase.java
│ ├─ execution/
│ │ ├─ ApplyFillUseCase.java
│ │ └─ ReconcileFillsUseCase.java
│ └─ ops/
│ ├─ HealthCheckUseCase.java
│ └─ ToggleKillSwitchUseCase.java
├─ orchestration/
│ ├─ TradingWorkflow.java (Signal→Risk→Order)
│ └─ ExecutionWorkflow.java (Fill→Position→PnL)
└─ dto/
├─ commands/
└─ events/




핵심 흐름:
- `TradingWorkflow`: (MarketData/Timer) → StrategyEngine → SignalPolicy → RiskEngine → BrokerClient
- `ExecutionWorkflow`: (FillEvent) → ApplyFillUseCase → Position/PnL 업데이트 → Alert

---

## 6. Infrastructure 패키지 구조(DB/메시징/스케줄러)

trading-infrastructure
└─ maru.trading.infra
├─ persistence/
│ ├─ jpa/
│ │ ├─ entity/
│ │ ├─ repository/
│ │ └─ mapper/ (Domain ↔ Entity 변환)
│ ├─ mybatis/ (선택)
│ └─ migrations/ (Flyway/Liquibase)
├─ messaging/
│ ├─ outbox/ (Outbox Pattern)
│ ├─ publisher/
│ └─ subscriber/
├─ cache/
│ ├─ TokenCache.java
│ └─ MarketCache.java
├─ scheduler/
│ ├─ StrategyScheduler.java (1m/장마감)
│ ├─ TokenRefreshJob.java
│ └─ ReconcileJob.java
├─ observability/
│ ├─ MetricsConfig.java
│ ├─ TracingConfig.java
│ └─ LoggingConfig.java
└─ config/
├─ AppConfig.java
└─ Properties.java




권장:
- 주문/체결/감사로그는 **트랜잭션 경계**가 중요 → Outbox로 이벤트 발행 안정화

---

## 7. Broker Adapter(KIS) 패키지 구조

trading-broker-kis
└─ maru.trading.broker.kis
├─ config/
│ ├─ KisProperties.java (실전/모의 baseUrl 등)
│ └─ KisClientConfig.java (WebClient/RestTemplate)
├─ auth/
│ ├─ KisTokenClient.java (/oauth2/tokenP)
│ ├─ KisApprovalClient.java (/oauth2/Approval)
│ └─ KisHashkeyClient.java (/uapi/hashkey)
├─ rest/
│ ├─ KisOrderClient.java (order-cash, cancel, amend)
│ ├─ KisQueryClient.java (잔고/주문조회/체결조회)
│ └─ dto/
│ ├─ request/
│ └─ response/
├─ ws/
│ ├─ KisWebSocketClient.java
│ ├─ KisSubscriptionManager.java
│ └─ message/
│ ├─ parser/
│ └─ model/
├─ mapper/
│ ├─ KisOrderMapper.java (Domain Order → KIS DTO)
│ └─ KisFillMapper.java (KIS Event → Domain Fill)
└─ error/
├─ KisErrorParser.java
└─ KisException.java




핵심 원칙:
- `application.ports.broker.BrokerClient` 인터페이스를 KIS가 구현
- KIS 전용 DTO/에러/헤더(tr_id 등)는 **broker-kis 모듈에만 존재**

---

## 8. API 모듈(Controller/Admin/Health)

trading-api
└─ maru.trading.api
├─ controller/
│ ├─ admin/
│ │ ├─ StrategyAdminController.java
│ │ ├─ RiskAdminController.java (Kill Switch)
│ │ └─ AccountAdminController.java
│ ├─ query/
│ │ ├─ OrderQueryController.java
│ │ ├─ PositionQueryController.java
│ │ └─ PnlQueryController.java
│ └─ demo/ (선택: 데모용 트리거)
│ ├─ DemoSignalController.java
│ └─ DemoKillSwitchController.java
├─ health/
│ ├─ HealthController.java
│ └─ HealthIndicatorConfig.java
├─ security/ (내부 운영 API 보호)
├─ exception/
│ ├─ ApiExceptionHandler.java
│ └─ ErrorResponse.java
└─ config/
└─ WebConfig.java




---

## 9. Backtest 모듈(선택) 구조

trading-backtest
└─ maru.trading.backtest
├─ engine/
│ ├─ BacktestRunner.java
│ ├─ ExecutionSimulator.java
│ └─ MetricsCalculator.java
├─ datasource/
│ ├─ CsvMarketDataSource.java
│ └─ DbMarketDataSource.java
├─ broker/
│ └─ SimulatedBrokerClient.java (BrokerClient 구현체)
└─ report/
├─ BacktestReport.java
└─ Exporter.java




---

## 10. 핵심 인터페이스(Ports) 정의(요약)

### BrokerClient (주문 실행 포트)
- `placeOrder(Order): BrokerAck`
- `cancelOrder(orderId): BrokerResult`
- `getOrderStatus(brokerOrderNo): BrokerOrderStatus`
- `getFills(...): List<Fill>`

### BrokerStream (실시간 이벤트 포트)
- `subscribeTicks(symbols)`
- `subscribeFills(account)`
- `onTick(handler)`
- `onFill(handler)`

### Repositories
- `OrderRepository`: save/update/findByIdempotencyKey/findOpenOrders
- `FillRepository`: save/findByOrderId
- `PositionRepository`: upsert/findBySymbol
- `RiskRepository`: readRules/updateState/saveEvent

---

## 11. 트랜잭션/이벤트 처리 권장 패턴

### Outbox 패턴(권장)
- 주문 생성/상태 변경/체결 반영은 DB 트랜잭션에서 확정
- 동일 트랜잭션에 Outbox에 이벤트 적재
- 별도 Publisher가 외부(메시지/알림/메트릭)로 발행

### 멱등성 키 처리
- `orders.idempotency_key` UNIQUE
- `placeOrder` 재시도 시:
  - 동일 키 존재 → 기존 주문 상태 조회/동기화로 대체

---

## 12. MVP 구현을 위한 “최소 패키지” (단일 모듈 버전)

> MVP를 빠르게 만들 때는 한 모듈에 아래 패키지 구조로 시작해도 OK

maru.trading
├─ api/ (controller, health)
├─ application/ (usecase, workflow)
├─ domain/ (entity, policy)
├─ infra/ (persistence, scheduler, cache)
└─ broker/
└─ kis/ (KIS adapter)




---

## 13. 권장 개발 순서(모듈 기준)

1) `broker-kis` : token + WS 연결 + 주문 1종(지정가)
2) `domain` : Order/Fill/Position/PnL 기본 모델
3) `application` : TradingWorkflow(신호→리스크→주문)
4) `infra` : DB 저장/상태 이력/동기화 잡
5) `api` : Admin(전략/리스크/킬스위치) + 조회 API
6) `backtest` : SimulatedBroker + 성과 리포트(2차)

---