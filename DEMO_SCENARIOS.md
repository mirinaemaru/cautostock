# Demo Scenarios - KIS Trading System

**Date**: 2026-01-01
**Purpose**: Demonstrate autonomous trading pipeline without real market data

---

## 개요

Demo 시스템은 실제 시장 데이터 없이 자율 트레이딩 파이프라인을 시연할 수 있습니다.

**구성 요소**:
- `MarketDataSimulator` - 시장 데이터 시뮬레이터
- `StrategyDemoRunner` - 전략 실행 데모
- `SimulationScenario` - 6가지 시나리오

**데이터 흐름**:
```
MarketDataSimulator → MarketTick 생성
  ↓
MarketDataCache → BarAggregator
  ↓
1분봉 생성 → BarCache + DB
  ↓
StrategyScheduler → 전략 실행
  ↓
Signal 생성 → Order 생성
```

---

## 사용 방법

### 1. Spring Bean 주입

```java
@Autowired
private StrategyDemoRunner demoRunner;
```

### 2. Demo 실행

```java
// Golden Cross 데모 실행
StrategyDemoRunner.DemoResults results = demoRunner.runDemo(
    StrategyDemoRunner.DemoConfig.builder()
        .scenario(SimulationScenario.GOLDEN_CROSS)
        .strategyType("MA_CROSSOVER")
        .symbol("DEMO_005930")  // 심볼명
        .accountId("ACC_DEMO_001")  // 계좌 ID
        .build()
);

// 결과 확인
System.out.println("Signals: " + results.getSignals().size());
System.out.println("Orders: " + results.getOrders().size());
```

### 3. 컨트롤러로 실행 (선택)

```java
@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {

    @Autowired
    private StrategyDemoRunner demoRunner;

    @PostMapping("/golden-cross")
    public DemoResults runGoldenCrossDemo() {
        return demoRunner.runDemo(
            StrategyDemoRunner.DemoConfig.builder()
                .scenario(SimulationScenario.GOLDEN_CROSS)
                .strategyType("MA_CROSSOVER")
                .build()
        );
    }
}
```

---

## 시나리오 상세

### Scenario 1: GOLDEN_CROSS

**목적**: MA Golden Cross 패턴 시연 (BUY 신호)

**패턴**:
```
1. Bars 1-20: 안정적 (70,000원)
2. Bars 21-27: 하락 추세 (56,000원까지)
   → MA5 < MA20 상태 생성
3. Bars 28-35: 강력한 상승 (104,000원까지)
   → MA5 crosses ABOVE MA20 (Golden Cross!)
```

**기대 결과**:
- Signal: BUY (MA5가 MA20을 상향 돌파)
- Order: BUY 주문 생성
- 로그: "Golden cross detected"

**사용 예제**:
```java
demoRunner.runDemo(
    StrategyDemoRunner.DemoConfig.builder()
        .scenario(SimulationScenario.GOLDEN_CROSS)
        .strategyType("MA_CROSSOVER")
        .symbol("TEST_GOLDEN")
        .build()
);
```

**예상 로그**:
```
[INFO] Starting Strategy Demo
[INFO] Scenario: GOLDEN_CROSS
[INFO] Strategy Type: MA_CROSSOVER
...
[INFO] MA Crossover evaluation: shortMA=92000.00, longMA=72400.00
[INFO] BUY signal generated: Golden cross detected
[INFO] Signals Generated: 1
[INFO] Orders Placed: 1
```

---

### Scenario 2: DEATH_CROSS

**목적**: MA Death Cross 패턴 시연 (SELL 신호)

**패턴**:
```
1. Bars 1-20: 안정적 (70,000원)
2. Bars 21-26: 상승 추세 (82,000원까지)
   → MA5 > MA20 상태 생성
3. Bars 27-34: 급락 (36,000원까지)
   → MA5 crosses BELOW MA20 (Death Cross!)
```

**기대 결과**:
- Signal: SELL (MA5가 MA20을 하향 돌파)
- Order: SELL 주문 생성
- 로그: "Death cross detected"

**사용 예제**:
```java
demoRunner.runDemo(
    StrategyDemoRunner.DemoConfig.builder()
        .scenario(SimulationScenario.DEATH_CROSS)
        .strategyType("MA_CROSSOVER")
        .symbol("TEST_DEATH")
        .build()
);
```

---

### Scenario 3: RSI_OVERSOLD

**목적**: RSI 과매도 패턴 시연 (BUY 신호)

**패턴**:
```
1. Bars 1-21: 안정적 (70,000원)
2. Bars 22-25: 하락 (62,000원)
3. Bars 26-32: 강한 반등 (73,500원)
4. Bars 33-35: 조정
5. Bar 36: 급락 (60,000원)
   → RSI crosses BELOW 30 (Oversold!)
```

**기대 결과**:
- Signal: BUY (RSI가 30 아래로 하향 돌파)
- Order: BUY 주문 생성
- 로그: "RSI oversold signal"

**사용 예제**:
```java
demoRunner.runDemo(
    StrategyDemoRunner.DemoConfig.builder()
        .scenario(SimulationScenario.RSI_OVERSOLD)
        .strategyType("RSI")
        .symbol("TEST_RSI_OVER")
        .build()
);
```

---

### Scenario 4: RSI_OVERBOUGHT

**목적**: RSI 과매수 패턴 시연 (SELL 신호)

**패턴**:
```
1. Bars 1-21: 안정적 (70,000원)
2. Bars 22-25: 상승 (78,000원)
3. Bars 26-32: 강한 조정 (67,500원)
4. Bars 33-35: 재상승
5. Bar 36: 급등 (80,000원)
   → RSI crosses ABOVE 70 (Overbought!)
```

**기대 결과**:
- Signal: SELL (RSI가 70 위로 상향 돌파)
- Order: SELL 주문 생성
- 로그: "RSI overbought signal"

**사용 예제**:
```java
demoRunner.runDemo(
    StrategyDemoRunner.DemoConfig.builder()
        .scenario(SimulationScenario.RSI_OVERBOUGHT)
        .strategyType("RSI")
        .symbol("TEST_RSI_UNDER")
        .build()
);
```

---

### Scenario 5: VOLATILE

**목적**: 변동성 높은 시장 환경 시연

**패턴**:
```
50개 바: 랜덤 변동 (±5%)
기준가: 70,000원
범위: 66,500원 ~ 73,500원
```

**기대 결과**:
- Signal: HOLD 가능성 높음 (명확한 패턴 없음)
- Order: 주문 생성 가능성 낮음
- 로그: "No clear signal detected"

**사용 예제**:
```java
demoRunner.runDemo(
    StrategyDemoRunner.DemoConfig.builder()
        .scenario(SimulationScenario.VOLATILE)
        .strategyType("MA_CROSSOVER")
        .symbol("TEST_VOLATILE")
        .build()
);
```

**용도**:
- 노이즈 필터링 테스트
- 잘못된 신호 방지 검증
- 리스크 관리 시스템 테스트

---

### Scenario 6: STABLE

**목적**: 안정적 시장 환경 시연

**패턴**:
```
50개 바: 미세 변동 (±0.1%)
기준가: 70,000원
범위: 69,930원 ~ 70,070원
```

**기대 결과**:
- Signal: HOLD (변동 없음)
- Order: 주문 생성 없음
- 로그: "Market stable, no signal"

**사용 예제**:
```java
demoRunner.runDemo(
    StrategyDemoRunner.DemoConfig.builder()
        .scenario(SimulationScenario.STABLE)
        .strategyType("MA_CROSSOVER")
        .symbol("TEST_STABLE")
        .build()
);
```

**용도**:
- 과매매 방지 검증
- TTL 만료 테스트
- 쿨다운 기간 테스트

---

## 전략 타입

### MA_CROSSOVER

**파라미터**:
```json
{
  "shortPeriod": 5,
  "longPeriod": 20,
  "ttlSeconds": 300
}
```

**신호 조건**:
- BUY: MA5 crosses ABOVE MA20
- SELL: MA5 crosses BELOW MA20
- HOLD: No crossover

**추천 시나리오**:
- GOLDEN_CROSS - BUY 신호 검증
- DEATH_CROSS - SELL 신호 검증
- VOLATILE - 노이즈 필터링 검증

---

### RSI

**파라미터**:
```json
{
  "period": 14,
  "oversold": 30,
  "overbought": 70,
  "ttlSeconds": 300
}
```

**신호 조건**:
- BUY: RSI crosses BELOW 30 (oversold)
- SELL: RSI crosses ABOVE 70 (overbought)
- HOLD: RSI between 30-70

**추천 시나리오**:
- RSI_OVERSOLD - BUY 신호 검증
- RSI_OVERBOUGHT - SELL 신호 검증
- STABLE - 과매매 방지 검증

---

## 결과 해석

### 성공적인 데모 (Golden Cross 예시)

```
========================================
Demo Results
========================================
Strategy ID: 01KDVJ2ABC123XYZ...
Symbol: DEMO_005930
Signals Generated: 1
Orders Placed: 1
----------------------------------------
Signals:
  - Type: BUY, Symbol: DEMO_005930, Reason: Golden cross detected
----------------------------------------
Orders:
  - Side: BUY, Symbol: DEMO_005930, Qty: 10, Price: 104000, Status: SENT
========================================
```

**해석**:
✅ Signal 1개 생성 (BUY)
✅ Order 1개 생성 (SENT)
✅ 골든 크로스 패턴 정상 감지
✅ 자율 트레이딩 파이프라인 정상 작동

---

### 신호 미생성 (Stable 예시)

```
========================================
Demo Results
========================================
Strategy ID: 01KDVJ2DEF456XYZ...
Symbol: DEMO_005930
Signals Generated: 0
Orders Placed: 0
========================================
```

**해석**:
✅ Signal 생성 없음 (안정적 시장)
✅ 과매매 방지 정상 작동
✅ 불필요한 주문 방지

---

## 고급 사용법

### 1. 다중 심볼 데모

```java
// 여러 심볼에 동일 전략 적용
for (String symbol : List.of("DEMO_A", "DEMO_B", "DEMO_C")) {
    demoRunner.runDemo(
        StrategyDemoRunner.DemoConfig.builder()
            .scenario(SimulationScenario.GOLDEN_CROSS)
            .strategyType("MA_CROSSOVER")
            .symbol(symbol)
            .build()
    );
}
```

### 2. 다중 전략 비교

```java
// MA vs RSI 비교
for (String strategyType : List.of("MA_CROSSOVER", "RSI")) {
    demoRunner.runDemo(
        StrategyDemoRunner.DemoConfig.builder()
            .scenario(SimulationScenario.VOLATILE)
            .strategyType(strategyType)
            .symbol("DEMO_COMPARE")
            .build()
    );
}
```

### 3. 순차 시나리오 실행

```java
// 모든 시나리오 검증
for (SimulationScenario scenario : SimulationScenario.values()) {
    System.out.println("=== Testing Scenario: " + scenario + " ===");
    demoRunner.runDemo(
        StrategyDemoRunner.DemoConfig.builder()
            .scenario(scenario)
            .strategyType("MA_CROSSOVER")
            .symbol("DEMO_" + scenario.name())
            .build()
    );
}
```

---

## 문제 해결

### 문제 1: Signal 생성 안됨

**증상**:
```
Signals Generated: 0
Orders Placed: 0
```

**가능한 원인**:
1. 전략 상태가 INACTIVE
2. StrategySymbol 매핑 없음
3. 바 데이터 부족 (최소 21개 필요)
4. 전략 파라미터 오류

**해결 방법**:
```java
// 1. 전략 상태 확인
StrategyEntity strategy = strategyRepository.findById(strategyId);
assert strategy.getStatus().equals("ACTIVE");

// 2. 매핑 확인
List<StrategySymbolEntity> mappings =
    strategySymbolRepository.findActiveByStrategyId(strategyId);
assert !mappings.isEmpty();

// 3. 바 개수 확인
List<BarEntity> bars = barRepository.findAll();
assert bars.size() >= 21;
```

---

### 문제 2: Order REJECTED

**증상**:
```
Orders:
  - Side: BUY, Status: REJECTED
```

**가능한 원인**:
1. Kill Switch ON
2. Risk Limit 초과
3. Broker 오류

**해결 방법**:
```java
// Kill Switch 확인
RiskStateEntity riskState = riskStateRepository.findByAccountId(accountId);
assert riskState.getKillSwitchStatus() == KillSwitchStatus.OFF;

// Risk Rule 확인
RiskRuleEntity rule = riskRuleRepository.findGlobalRule();
// maxOpenOrders, maxPositionValue 등 확인
```

---

## 실행 체크리스트

데모 실행 전 확인:

- [ ] MariaDB 실행 중
- [ ] 애플리케이션 실행 중 (Spring Boot)
- [ ] Risk Rule 설정 (relaxed 권장)
- [ ] Kill Switch OFF
- [ ] 로그 레벨: DEBUG (선택)

데모 실행:

- [ ] Scenario 선택
- [ ] Strategy Type 선택
- [ ] Symbol 지정
- [ ] Account ID 지정
- [ ] runDemo() 호출

결과 확인:

- [ ] Signals 개수 확인
- [ ] Orders 개수 확인
- [ ] Signal 타입 확인 (BUY/SELL)
- [ ] Order 상태 확인 (SENT/ACCEPTED)
- [ ] 로그 메시지 확인

---

## 참고 자료

- **PHASE3_COMPLETE.md** - Phase 3 완료 보고서
- **README.md** - 프로젝트 개요
- **TEST_IMPLEMENTATION_STATUS.md** - 테스트 현황

---

**Last Updated**: 2026-01-01
**Author**: Claude Sonnet 4.5
**Status**: ✅ Demo System Ready
