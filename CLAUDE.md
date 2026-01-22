# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a KIS (Korea Investment & Securities) OpenAPI-based automated trading system (B안). Currently, this repository contains comprehensive design documentation in `/md/docs/`. The actual Java/Spring Boot implementation will be generated based on these design documents.

**Critical**: All `/md/docs` files are the **single source of truth** for implementation. Treat them as contracts - never deviate from them without explicit user approval.

## Technology Stack

- 이 프로젝트의 JVM 버전은 17(매우중요)
- Java 17
- Spring Boot 3.x (Web, Validation, Actuator)
- JPA (Hibernate) with MariaDB
- Flyway for database migrations
- Lombok, Jackson
- Build tool: Gradle or Maven

## Architecture

**Style**: Layered + Hexagonal (Ports & Adapters)

**Core Layers**:
- `domain`: Pure business logic (strategies, signals, risk, positions, PnL) - minimize Spring dependencies
- `application`: Use cases and workflow orchestration
- `infra`: Database (JPA/MyBatis), Outbox pattern, schedulers, caching
- `broker`: KIS OpenAPI adapter (REST/WebSocket) - isolates vendor dependency
- `api`: REST controllers (Admin, Query, Demo), health checks

**Event-Driven**: DB transactions + Outbox pattern for at-least-once delivery

## Package Structure

Root package: `maru.trading`

For multi-module setup (recommended for production):
```
trading-api/         # Spring Boot app, REST/Batch/Scheduler
trading-domain/      # Domain models + services
trading-application/ # Use cases, orchestration
trading-infrastructure/ # DB/JPA, messaging, cache
trading-broker-kis/  # KIS REST/WS adapter
trading-backtest/    # Backtest engine (optional)
trading-common/      # Shared utilities
```

For MVP single-module setup:
```
maru.trading/
├─ api/          # Controllers, health
├─ application/  # Use cases, workflows
├─ domain/       # Entities, policies
├─ infra/        # Persistence, scheduler, cache
└─ broker/kis/   # KIS adapter
```

See `/md/02_PACKAGE_STRUCTURE.md` for detailed package structure.

## Core Domain Modules

1. **account**: Account, Permission, AccountService
2. **market**: Instrument, MarketTick, MarketBar, MarketDataPolicy
3. **strategy**: Strategy, StrategyVersion, StrategyParams, StrategyEngine
4. **signal**: Signal, SignalType, SignalDecision, SignalPolicy
5. **risk**: RiskRule, RiskDecision, RiskEngine, KillSwitch
6. **order**: Order, OrderType, OrderStatus, IdempotencyKey, OrderPolicy
7. **execution**: Fill, Position, PnlLedger, PortfolioSnapshot
8. **ops**: Alert, Severity, AuditEvent

## Key Architectural Patterns

### Ports & Adapters
- `BrokerClient` (port): placeOrder(), cancelOrder(), getOrderStatus(), getFills()
- `BrokerStream` (port): subscribeTicks(), subscribeFills(), onTick(), onFill()
- `KisOrderClient`, `KisWebSocketClient` (adapters): KIS-specific implementations

### Outbox Pattern
- All order creation/status changes/fill reflections happen in DB transactions
- Events written to `outbox` table in same transaction
- Separate publisher reads outbox and publishes events
- Ensures consistency and at-least-once delivery

### Idempotency
- `orders.idempotency_key` with UNIQUE constraint
- On retry with same key → query existing order status instead of creating duplicate

### Workflows
- **TradingWorkflow**: MarketData/Timer → StrategyEngine → SignalPolicy → RiskEngine → BrokerClient
- **ExecutionWorkflow**: FillEvent → ApplyFillUseCase → Position/PnL update → Alert

## Implementation Priority

### Phase 1 (Core MVP)
1. Project scaffolding (Spring Boot setup)
2. Database schema with Flyway migrations
3. JPA entities + repositories
4. OpenAPI-based controllers and DTOs
5. Global exception handler
6. Event outbox storage

### Phase 2 (Business Logic)
7. TradingWorkflow skeleton
8. Risk engine + Kill Switch
9. Demo API endpoints
10. Health checks

### Phase 3 (KIS Integration)
11. KIS broker adapter (stub implementation - logs only, no actual API calls)

## Critical Implementation Rules

1. **Transaction Boundaries**: All state changes must happen inside transactions
2. **Event Publishing**: Save to DB first, then publish events from outbox
3. **Enum Storage**: Store enums as strings in database
4. **DateTime Precision**: Use `DATETIME(3)` for millisecond precision
5. **KIS Safety**: NEVER call actual KIS order APIs - use PAPER/stub mode only
6. **No Arbitrary Decisions**: Follow design docs strictly; ask user if something is unclear
7. **Idempotency**: Always use idempotency keys for order operations
8. **Domain Purity**: Keep domain layer free from Spring/framework dependencies when possible

## Key Documentation Files

- `/md/docs/00_README.md`: Project overview, goals, MVP scope
- `/md/docs/01_ARCHITECTURE.md`: Architectural style and layers
- `/md/02_PACKAGE_STRUCTURE.md`: Detailed package/module structure
- `/md/03_FUNCTIONAL_SPEC_B.md`: Functional requirements (8 core features)
- `/md/docs/04_API_OPENAPI.md`: OpenAPI specifications
- `/md/docs/05_DOMAIN_EVENTS.md`: Domain event contracts
- `/md/docs/06_DB_SCHEMA_MARIADB.md`: Database schema design
- `/md/docs/07_MVP_SCENARIOS.md`: MVP test scenarios and demo flows
- `/md/docs/08_NON_FUNCTIONAL.md`: Non-functional requirements
- `/md/docs/99_CLAUDE_CODE_TASKS.md`: Code generation rules for Claude Code

## Development Workflow

When implementing new features:
1. Read relevant design docs from `/md/docs/` first
2. Follow the package structure in `02_PACKAGE_STRUCTURE.md`
3. Implement from inner layers outward: domain → application → infra → api
4. Use Flyway for database schema changes
5. Add Outbox entries for all state-changing operations
6. Never call real KIS APIs - stub implementations only

## MVP Scope

- **In scope**: Admin API, Query API, Demo API, PAPER trading mode
- **Out of scope**: Real KIS LIVE account trading, backtesting (Phase 3)
- **KIS Adapter**: Stub implementation that logs actions but doesn't make real API calls

## Important Notes

- This is a Korean trading system using KIS (Korea Investment & Securities) OpenAPI
- All documentation is in Korean, but code should follow Java/Spring conventions
- Focus on correctness and consistency over performance for MVP
- Event-driven architecture is central - never skip Outbox pattern
- Risk management (especially Kill Switch) is a critical safety feature
