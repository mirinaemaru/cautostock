# API 명세서(OpenAPI) – MVP (B안: KIS 연계 자동매매 시스템)
문서 형식: **Markdown + OpenAPI 3.0 YAML 포함**

> 목적: 운영/데모/조회에 필요한 **내부(Admin) API**와 **조회(Query) API**를 MVP 범위로 정의한다.  
> ※ 증권사(KIS) REST/WS 호출은 서버 내부 Adapter에서 수행되며, 본 API는 “운영자/내부 서비스”용이다.

---

## 1. OpenAPI 3.0 (YAML)

```yaml
openapi: 3.0.3
info:
  title: Trading System MVP API (B)
  version: 0.1.0
  description: |
    KIS 연계 자동매매 MVP용 내부 API
    - Admin: 계좌/전략/리스크/Kill Switch 제어
    - Query: 주문/체결/포지션/손익 조회
    - Ops: Health/metrics 최소
servers:
  - url: http://localhost:8080
tags:
  - name: Health
  - name: Admin-Accounts
  - name: Admin-Strategies
  - name: Admin-Risk
  - name: Admin-KillSwitch
  - name: Query-Orders
  - name: Query-Fills
  - name: Query-Positions
  - name: Query-PnL
  - name: Demo

security:
  - AdminApiKey: []

paths:
  /health:
    get:
      tags: [Health]
      summary: Health check
      description: 시스템/연동 상태 요약
      security: []
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/HealthResponse"

  /api/v1/admin/accounts:
    post:
      tags: [Admin-Accounts]
      summary: Register account
      description: 모의/실전 계좌 등록
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/AccountRegisterRequest"
      responses:
        "201":
          description: Created
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/AccountResponse"
        "409":
          $ref: "#/components/responses/Conflict"
    get:
      tags: [Admin-Accounts]
      summary: List accounts
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/AccountListResponse"

  /api/v1/admin/accounts/{accountId}:
    get:
      tags: [Admin-Accounts]
      summary: Get account
      parameters:
        - $ref: "#/components/parameters/AccountId"
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/AccountResponse"
        "404":
          $ref: "#/components/responses/NotFound"

  /api/v1/admin/accounts/{accountId}/status:
    put:
      tags: [Admin-Accounts]
      summary: Update account status
      parameters:
        - $ref: "#/components/parameters/AccountId"
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/AccountStatusUpdateRequest"
      responses:
        "200":
          description: Updated
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/AccountResponse"
        "400":
          $ref: "#/components/responses/BadRequest"
        "404":
          $ref: "#/components/responses/NotFound"

  /api/v1/admin/accounts/{accountId}/permissions:
    put:
      tags: [Admin-Accounts]
      summary: Upsert account permissions
      parameters:
        - $ref: "#/components/parameters/AccountId"
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/AccountPermissionsUpsertRequest"
      responses:
        "200":
          description: Updated
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/AccountPermissionsResponse"
        "404":
          $ref: "#/components/responses/NotFound"

  /api/v1/admin/strategies:
    post:
      tags: [Admin-Strategies]
      summary: Create strategy (v1)
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/StrategyCreateRequest"
      responses:
        "201":
          description: Created
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/StrategyResponse"
    get:
      tags: [Admin-Strategies]
      summary: List strategies
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/StrategyListResponse"

  /api/v1/admin/strategies/{strategyId}:
    get:
      tags: [Admin-Strategies]
      summary: Get strategy
      parameters:
        - $ref: "#/components/parameters/StrategyId"
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/StrategyResponse"
        "404":
          $ref: "#/components/responses/NotFound"

  /api/v1/admin/strategies/{strategyId}/activate:
    post:
      tags: [Admin-Strategies]
      summary: Activate strategy version
      description: 전략 활성(실행 가능 상태)
      parameters:
        - $ref: "#/components/parameters/StrategyId"
      requestBody:
        required: false
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/StrategyActivateRequest"
      responses:
        "200":
          description: Activated
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/StrategyResponse"
        "404":
          $ref: "#/components/responses/NotFound"

  /api/v1/admin/strategies/{strategyId}/deactivate:
    post:
      tags: [Admin-Strategies]
      summary: Deactivate strategy
      parameters:
        - $ref: "#/components/parameters/StrategyId"
      responses:
        "200":
          description: Deactivated
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/StrategyResponse"
        "404":
          $ref: "#/components/responses/NotFound"

  /api/v1/admin/strategies/{strategyId}/params:
    put:
      tags: [Admin-Strategies]
      summary: Update strategy params (creates new version)
      parameters:
        - $ref: "#/components/parameters/StrategyId"
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/StrategyParamsUpdateRequest"
      responses:
        "200":
          description: Updated (new version)
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/StrategyResponse"
        "400":
          $ref: "#/components/responses/BadRequest"
        "404":
          $ref: "#/components/responses/NotFound"

  /api/v1/admin/risk/rules:
    put:
      tags: [Admin-Risk]
      summary: Upsert risk rules
      description: 계좌별/전역 리스크 룰 저장
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/RiskRulesUpsertRequest"
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/RiskRulesResponse"

  /api/v1/admin/risk/state:
    get:
      tags: [Admin-Risk]
      summary: Get risk state
      parameters:
        - in: query
          name: accountId
          schema: { type: string }
          required: false
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/RiskStateResponse"

  /api/v1/admin/kill-switch:
    get:
      tags: [Admin-KillSwitch]
      summary: Get kill switch state
      parameters:
        - in: query
          name: accountId
          schema: { type: string }
          required: false
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/KillSwitchResponse"
    post:
      tags: [Admin-KillSwitch]
      summary: Toggle kill switch
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/KillSwitchToggleRequest"
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/KillSwitchResponse"

  /api/v1/query/orders:
    get:
      tags: [Query-Orders]
      summary: Search orders
      parameters:
        - in: query
          name: accountId
          schema: { type: string }
        - in: query
          name: symbol
          schema: { type: string }
        - in: query
          name: status
          schema:
            $ref: "#/components/schemas/OrderStatus"
        - in: query
          name: from
          schema: { type: string, format: date-time }
        - in: query
          name: to
          schema: { type: string, format: date-time }
        - in: query
          name: limit
          schema: { type: integer, default: 50, minimum: 1, maximum: 200 }
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/OrderListResponse"

  /api/v1/query/orders/{orderId}:
    get:
      tags: [Query-Orders]
      summary: Get order
      parameters:
        - $ref: "#/components/parameters/OrderId"
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/OrderResponse"
        "404":
          $ref: "#/components/responses/NotFound"

  /api/v1/query/fills:
    get:
      tags: [Query-Fills]
      summary: Search fills
      parameters:
        - in: query
          name: accountId
          schema: { type: string }
        - in: query
          name: orderId
          schema: { type: string }
        - in: query
          name: symbol
          schema: { type: string }
        - in: query
          name: from
          schema: { type: string, format: date-time }
        - in: query
          name: to
          schema: { type: string, format: date-time }
        - in: query
          name: limit
          schema: { type: integer, default: 50, minimum: 1, maximum: 200 }
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/FillListResponse"

  /api/v1/query/positions:
    get:
      tags: [Query-Positions]
      summary: List positions
      parameters:
        - in: query
          name: accountId
          schema: { type: string }
          required: true
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/PositionListResponse"

  /api/v1/query/pnl/snapshot:
    get:
      tags: [Query-PnL]
      summary: Latest portfolio snapshot
      parameters:
        - in: query
          name: accountId
          schema: { type: string }
          required: true
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/PortfolioSnapshotResponse"

  /api/v1/demo/signal:
    post:
      tags: [Demo]
      summary: Demo - inject signal (optional)
      description: 데모 재현용 수동 신호 트리거(운영 비활성 권장)
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/DemoSignalRequest"
      responses:
        "202":
          description: Accepted
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/AckResponse"

  /api/v1/demo/failpoint:
    post:
      tags: [Demo]
      summary: Demo - toggle failpoint (optional)
      description: 네트워크/WS/DB 실패를 모의하기 위한 failpoint 토글
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/DemoFailpointRequest"
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/AckResponse"

components:
  securitySchemes:
    AdminApiKey:
      type: apiKey
      in: header
      name: X-ADMIN-API-KEY

  parameters:
    AccountId:
      in: path
      name: accountId
      required: true
      schema: { type: string }
    StrategyId:
      in: path
      name: strategyId
      required: true
      schema: { type: string }
    OrderId:
      in: path
      name: orderId
      required: true
      schema: { type: string }

  responses:
    BadRequest:
      description: Bad Request
      content:
        application/json:
          schema: { $ref: "#/components/schemas/ErrorResponse" }
    NotFound:
      description: Not Found
      content:
        application/json:
          schema: { $ref: "#/components/schemas/ErrorResponse" }
    Conflict:
      description: Conflict
      content:
        application/json:
          schema: { $ref: "#/components/schemas/ErrorResponse" }

  schemas:
    # -------- Common --------
    AckResponse:
      type: object
      properties:
        ok: { type: boolean, example: true }
        message: { type: string, example: accepted }
        requestId: { type: string, example: "req_01H..." }
      required: [ok]

    ErrorResponse:
      type: object
      properties:
        code: { type: string, example: "ORDER_003" }
        message: { type: string, example: "Invalid order price" }
        detail: { type: string }
        requestId: { type: string }
        timestamp: { type: string, format: date-time }
      required: [code, message, timestamp]

    Environment:
      type: string
      enum: [PAPER, LIVE, BACKTEST]

    # -------- Health --------
    HealthResponse:
      type: object
      properties:
        status: { type: string, example: UP }
        components:
          type: object
          properties:
            kisRest: { type: string, example: UP }
            kisWs: { type: string, example: UP }
            token: { type: string, example: VALID }
            db: { type: string, example: UP }
        timestamp: { type: string, format: date-time }
      required: [status, timestamp]

    # -------- Accounts --------
    AccountStatus:
      type: string
      enum: [ACTIVE, INACTIVE, SUSPENDED]

    PermissionCode:
      type: string
      enum: [TRADE_BUY, TRADE_SELL, AUTO_TRADE, MANUAL_TRADE, BACKTEST, PAPER_ONLY]

    AccountRegisterRequest:
      type: object
      properties:
        broker: { type: string, example: KIS }
        environment: { $ref: "#/components/schemas/Environment" }
        cano: { type: string, description: "계좌번호 앞 8자리", example: "12345678" }
        acntPrdtCd: { type: string, example: "01" }
        alias: { type: string, example: "paper-main" }
      required: [broker, environment, cano, acntPrdtCd]

    AccountStatusUpdateRequest:
      type: object
      properties:
        status: { $ref: "#/components/schemas/AccountStatus" }
      required: [status]

    AccountResponse:
      type: object
      properties:
        accountId: { type: string, example: "acct_01H..." }
        broker: { type: string, example: KIS }
        environment: { $ref: "#/components/schemas/Environment" }
        cano: { type: string, example: "12345678" }
        acntPrdtCd: { type: string, example: "01" }
        status: { $ref: "#/components/schemas/AccountStatus" }
        alias: { type: string }
        createdAt: { type: string, format: date-time }
        updatedAt: { type: string, format: date-time }
      required: [accountId, broker, environment, cano, acntPrdtCd, status]

    AccountListResponse:
      type: object
      properties:
        items:
          type: array
          items: { $ref: "#/components/schemas/AccountResponse" }
      required: [items]

    AccountPermissionsUpsertRequest:
      type: object
      properties:
        permissions:
          type: array
          items:
            type: object
            properties:
              code: { $ref: "#/components/schemas/PermissionCode" }
              enabled: { type: boolean }
            required: [code, enabled]
      required: [permissions]

    AccountPermissionsResponse:
      type: object
      properties:
        accountId: { type: string }
        permissions:
          type: array
          items:
            type: object
            properties:
              code: { $ref: "#/components/schemas/PermissionCode" }
              enabled: { type: boolean }
      required: [accountId, permissions]

    # -------- Strategies --------
    StrategyStatus:
      type: string
      enum: [ACTIVE, INACTIVE]

    StrategyCreateRequest:
      type: object
      properties:
        name: { type: string, example: "DEMO_MA_CROSS_1M" }
        description: { type: string }
        mode: { $ref: "#/components/schemas/Environment" }
        params:
          type: object
          additionalProperties: true
      required: [name, mode, params]

    StrategyActivateRequest:
      type: object
      properties:
        versionId: { type: string, description: "지정 시 해당 버전 활성화(옵션)" }
      required: []

    StrategyParamsUpdateRequest:
      type: object
      properties:
        params:
          type: object
          additionalProperties: true
      required: [params]

    StrategyResponse:
      type: object
      properties:
        strategyId: { type: string, example: "str_01H..." }
        name: { type: string }
        description: { type: string }
        status: { $ref: "#/components/schemas/StrategyStatus" }
        activeVersionId: { type: string, example: "strv_01H..." }
        mode: { $ref: "#/components/schemas/Environment" }
        params:
          type: object
          additionalProperties: true
        createdAt: { type: string, format: date-time }
        updatedAt: { type: string, format: date-time }
      required: [strategyId, name, status, activeVersionId, mode, params]

    StrategyListResponse:
      type: object
      properties:
        items:
          type: array
          items: { $ref: "#/components/schemas/StrategyResponse" }
      required: [items]

    # -------- Risk --------
    RiskRulesUpsertRequest:
      type: object
      properties:
        accountId: { type: string, description: "없으면 전역 룰로 취급(옵션)" }
        rules:
          type: object
          properties:
            maxPositionValuePerSymbol: { type: number, example: 100000 }
            maxOpenOrders: { type: integer, example: 1 }
            maxOrdersPerMinute: { type: integer, example: 2 }
            dailyLossLimit: { type: number, example: 3000 }
            consecutiveOrderFailuresLimit: { type: integer, example: 3 }
          additionalProperties: false
      required: [rules]

    RiskRulesResponse:
      type: object
      properties:
        accountId: { type: string }
        rules:
          type: object
          additionalProperties: true
        updatedAt: { type: string, format: date-time }
      required: [rules]

    KillSwitchStatus:
      type: string
      enum: [OFF, ARMED, ON]

    RiskStateResponse:
      type: object
      properties:
        accountId: { type: string }
        dailyPnl: { type: number, example: -1200.5 }
        exposure: { type: number, example: 520000 }
        killSwitchStatus: { $ref: "#/components/schemas/KillSwitchStatus" }
        updatedAt: { type: string, format: date-time }
      required: [killSwitchStatus, updatedAt]

    KillSwitchToggleRequest:
      type: object
      properties:
        accountId: { type: string, description: "없으면 전역 토글(옵션)" }
        status: { $ref: "#/components/schemas/KillSwitchStatus" }
        reason: { type: string, example: "MANUAL" }
      required: [status, reason]

    KillSwitchResponse:
      type: object
      properties:
        accountId: { type: string }
        status: { $ref: "#/components/schemas/KillSwitchStatus" }
        reason: { type: string }
        updatedAt: { type: string, format: date-time }
      required: [status, updatedAt]

    # -------- Orders / Fills / Positions / PnL --------
    Side:
      type: string
      enum: [BUY, SELL]

    OrderType:
      type: string
      enum: [LIMIT, MARKET]

    OrderStatus:
      type: string
      enum: [NEW, SENT, ACCEPTED, PART_FILLED, FILLED, CANCELLED, REJECTED, ERROR]

    OrderResponse:
      type: object
      properties:
        orderId: { type: string }
        accountId: { type: string }
        strategyId: { type: string }
        strategyVersionId: { type: string }
        symbol: { type: string, example: "005930" }
        side: { $ref: "#/components/schemas/Side" }
        orderType: { $ref: "#/components/schemas/OrderType" }
        ordDvsn: { type: string, example: "00", description: "KIS ORD_DVSN" }
        qty: { type: number, example: 1 }
        price: { type: number, example: 72000 }
        status: { $ref: "#/components/schemas/OrderStatus" }
        idempotencyKey: { type: string }
        brokerOrderNo: { type: string }
        createdAt: { type: string, format: date-time }
        updatedAt: { type: string, format: date-time }
      required: [orderId, accountId, symbol, side, orderType, qty, status, createdAt]

    OrderListResponse:
      type: object
      properties:
        items:
          type: array
          items: { $ref: "#/components/schemas/OrderResponse" }
      required: [items]

    FillResponse:
      type: object
      properties:
        fillId: { type: string }
        orderId: { type: string }
        accountId: { type: string }
        symbol: { type: string }
        side: { $ref: "#/components/schemas/Side" }
        fillPrice: { type: number }
        fillQty: { type: number }
        fee: { type: number }
        tax: { type: number }
        fillTs: { type: string, format: date-time }
      required: [fillId, orderId, accountId, symbol, side, fillPrice, fillQty, fillTs]

    FillListResponse:
      type: object
      properties:
        items:
          type: array
          items: { $ref: "#/components/schemas/FillResponse" }
      required: [items]

    PositionResponse:
      type: object
      properties:
        accountId: { type: string }
        symbol: { type: string }
        qty: { type: number }
        avgPrice: { type: number }
        realizedPnl: { type: number }
        updatedAt: { type: string, format: date-time }
      required: [accountId, symbol, qty, avgPrice, updatedAt]

    PositionListResponse:
      type: object
      properties:
        items:
          type: array
          items: { $ref: "#/components/schemas/PositionResponse" }
      required: [items]

    PortfolioSnapshotResponse:
      type: object
      properties:
        accountId: { type: string }
        totalValue: { type: number }
        cash: { type: number }
        unrealizedPnl: { type: number }
        realizedPnl: { type: number }
        snapshotTs: { type: string, format: date-time }
      required: [accountId, totalValue, snapshotTs]

    # -------- Demo --------
    DemoSignalRequest:
      type: object
      properties:
        accountId: { type: string }
        symbol: { type: string, example: "005930" }
        side: { $ref: "#/components/schemas/Side" }
        targetType: { type: string, enum: [QTY, WEIGHT], default: QTY }
        targetValue: { type: number, example: 1 }
        ttlSeconds: { type: integer, default: 60 }
      required: [symbol, side]

    DemoFailpointRequest:
      type: object
      properties:
        name:
          type: string
          enum: [KIS_REST_TIMEOUT, KIS_WS_DROP, DB_WRITE_FAIL]
        enabled: { type: boolean }
      required: [name, enabled]
