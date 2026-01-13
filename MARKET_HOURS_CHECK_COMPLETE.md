# 주문 가능 시간 체크 기능 구현 완료

**작성일**: 2026-01-01
**구현 방식**: A+C 조합 (Domain Policy + 설정 기반)
**상태**: ✅ **완료**

---

## 📊 구현 요약

한국 주식시장의 **거래시간 검증 기능**을 리스크 엔진에 통합했습니다.

### ✅ 주요 기능

1. **거래 세션 검증**: 정규장, 시간외 단일가, 시간외 종가 등 세션별 시간 체크
2. **주말/공휴일 감지**: 토요일, 일요일, 공휴일 자동 차단
3. **설정 기반 유연성**: application.yml로 허용 세션 및 공휴일 관리
4. **7번째 리스크 체크**: 기존 6개 체크에 거래시간 검증 추가

---

## 🎯 구현 내용

### 1단계: TradingSession enum 생성

**파일**: `src/main/java/maru/trading/domain/market/TradingSession.java`

```java
public enum TradingSession {
    REGULAR,              // 정규장 (09:00-15:30)
    PRE_MARKET,          // 시간외 단일가 장전 (08:30-08:40)
    AFTER_HOURS_CLOSING, // 시간외 종가 (15:40-16:00)
    AFTER_HOURS          // 시간외 단일가 장후 (16:00-18:00)
}
```

**특징**:
- 한국명, 시간범위 포함
- toString() 오버라이드로 가독성 향상

---

### 2단계: MarketHoursPolicy 도메인 클래스

**파일**: `src/main/java/maru/trading/domain/market/MarketHoursPolicy.java`

**주요 메서드**:
```java
// 시장 개장 여부 체크
public boolean isMarketOpen(
    LocalDateTime now,
    Set<TradingSession> allowedSessions,
    Set<LocalDate> publicHolidays);

// 특정 세션 시간 체크
public boolean isWithinSession(LocalTime time, TradingSession session);

// 현재 세션 조회
public TradingSession getCurrentSession(LocalDateTime now);

// 다음 개장 시간 계산
public LocalDateTime getNextOpeningTime(LocalDateTime now, TradingSession session);
```

**검증 로직**:
1. 주말 체크 (토요일, 일요일)
2. 공휴일 체크 (설정 기반)
3. 세션 시간 체크 (각 세션별 시간 범위)

**Stateless Design**: 스레드 안전, 인스턴스 재사용 가능

---

### 3단계: MarketHoursConfig 설정 클래스

**파일**: `src/main/java/maru/trading/infra/config/MarketHoursConfig.java`

**매핑**: `trading.market.*` properties

**주요 메서드**:
```java
// 설정 로드
public boolean isCheckEnabled();
public Set<TradingSession> getAllowedSessionsAsEnum();
public Set<LocalDate> getPublicHolidaysAsDate();

// 검증
public boolean isSessionAllowed(TradingSession session);
```

**파싱 로직**:
- `allowed-sessions`: String → TradingSession enum 변환
- `public-holidays`: "yyyy-MM-dd" → LocalDate 변환
- 잘못된 값은 자동으로 스킵 (에러 메시지 출력)

---

### 4단계: application.yml 설정 추가

**위치**: `src/main/resources/application.yml`

```yaml
trading:
  market:
    check-enabled: true  # 거래시간 체크 활성화
    allowed-sessions:
      - REGULAR              # 정규장 (09:00-15:30)
      # - PRE_MARKET         # 시간외 단일가 장전 (08:30-08:40)
      # - AFTER_HOURS_CLOSING # 시간외 종가 (15:40-16:00)
      # - AFTER_HOURS        # 시간외 단일가 장후 (16:00-18:00)
    public-holidays:
      # 2025년 공휴일
      - "2025-01-01"  # 신정
      - "2025-01-28"  # 설날 연휴
      - "2025-01-29"  # 설날
      - "2025-01-30"  # 설날 연휴
      - "2025-03-01"  # 삼일절
      - "2025-05-05"  # 어린이날
      - "2025-06-06"  # 현충일
      - "2025-08-15"  # 광복절
      - "2025-10-03"  # 개천절
      - "2025-10-07"  # 추석
      - "2025-10-09"  # 한글날
      - "2025-12-25"  # 성탄절
```

**기본값**:
- `check-enabled`: `true`
- `allowed-sessions`: `[REGULAR]` (정규장만 허용)
- `public-holidays`: `[]` (비어있음)

---

### 5단계: RiskEngine 업데이트

**파일**: `src/main/java/maru/trading/domain/risk/RiskEngine.java`

**7번째 체크 추가**:
```java
// 7. 거래시간 체크 (Market Hours Check)
RiskDecision marketHoursCheck = checkMarketHours(
    order, marketHoursEnabled, allowedSessions, publicHolidays);
if (!marketHoursCheck.isApproved()) {
    return marketHoursCheck;
}
```

**checkMarketHours() 메서드**:
```java
private RiskDecision checkMarketHours(
    Order order,
    boolean enabled,
    Set<TradingSession> allowedSessions,
    Set<LocalDate> publicHolidays) {

    if (!enabled) {
        return RiskDecision.approve();
    }

    MarketHoursPolicy policy = new MarketHoursPolicy();
    boolean isOpen = policy.isMarketOpen(now, allowedSessions, publicHolidays);

    if (!isOpen) {
        String reason = "Market is closed...";
        return RiskDecision.reject(reason, "MARKET_CLOSED");
    }

    return RiskDecision.approve();
}
```

**에러 메시지**:
- 세션 내이지만 허용 안 됨: "Market is in AFTER_HOURS session which is not allowed"
- 시장 마감: "Market is closed at 16:30. Allowed sessions: [REGULAR]"

**Backward Compatibility**:
```java
// 기존 메서드 (market hours 체크 없음)
public RiskDecision evaluatePreTrade(Order, RiskRule, RiskState);
public RiskDecision evaluatePreTrade(Order, RiskRule, RiskState, Position);

// 새 메서드 (market hours 체크 있음)
public RiskDecision evaluatePreTrade(
    Order, RiskRule, RiskState, Position,
    boolean marketHoursEnabled,
    Set<TradingSession> allowedSessions,
    Set<LocalDate> publicHolidays);
```

---

### 6단계: EvaluateRiskUseCase 업데이트

**파일**: `src/main/java/maru/trading/application/usecase/trading/EvaluateRiskUseCase.java`

**MarketHoursConfig 주입**:
```java
@RequiredArgsConstructor
public class EvaluateRiskUseCase {
    private final RiskEngine riskEngine;
    private final RiskRuleRepository riskRuleRepository;
    private final RiskStateRepository riskStateRepository;
    private final PositionRepository positionRepository;
    private final MarketHoursConfig marketHoursConfig; // 추가
}
```

**리스크 평가 로직**:
```java
public RiskDecision evaluate(Order order) {
    // Step 1-3: 기존 로직 (rule, state, position 로드)

    // Step 4: Load market hours configuration
    boolean marketHoursEnabled = marketHoursConfig.isCheckEnabled();
    Set<TradingSession> allowedSessions = marketHoursConfig.getAllowedSessionsAsEnum();
    Set<LocalDate> publicHolidays = marketHoursConfig.getPublicHolidaysAsDate();

    // Step 5: Evaluate with market hours
    RiskDecision decision = riskEngine.evaluatePreTrade(
        order, rule, state, currentPosition,
        marketHoursEnabled, allowedSessions, publicHolidays);

    // ...
}
```

---

## 🔍 리스크 체크 전체 흐름

### 7개 체크 순서

1. **Kill Switch**: 수동/자동 긴급 정지
2. **Daily PnL Limit**: 일일 손실 한도
3. **Max Open Orders**: 최대 미체결 주문 수
4. **Order Frequency**: 1분당 주문 빈도
5. **Position Exposure**: 종목당 최대 투자금액 (포지션 + 주문)
6. **Consecutive Failures**: 연속 실패 횟수
7. **Market Hours** ✨ (NEW): 거래시간 검증

### 흐름도

```
PlaceOrderUseCase.execute()
  ↓
EvaluateRiskUseCase.evaluate()
  ↓
Load: RiskRule, RiskState, Position, MarketHoursConfig
  ↓
RiskEngine.evaluatePreTrade()
  ├─ Check 1: Kill Switch
  ├─ Check 2: Daily PnL Limit
  ├─ Check 3: Max Open Orders
  ├─ Check 4: Order Frequency
  ├─ Check 5: Position Exposure
  ├─ Check 6: Consecutive Failures
  └─ Check 7: Market Hours ✨
       ├─ Weekend?
       ├─ Public Holiday?
       └─ Within Allowed Session?
  ↓
RiskDecision.approve() or reject("MARKET_CLOSED")
```

---

## 🚀 사용 방법

### 1. 기본 설정 (정규장만 허용)

```yaml
trading:
  market:
    check-enabled: true
    allowed-sessions:
      - REGULAR
    public-holidays:
      - "2025-01-01"
      - "2025-12-25"
```

**결과**:
- 평일 09:00-15:30: ✅ 주문 허용
- 평일 08:00, 16:00: ❌ `MARKET_CLOSED` (정규장 외)
- 토요일, 일요일: ❌ `MARKET_CLOSED` (주말)
- 2025-01-01: ❌ `MARKET_CLOSED` (공휴일)

---

### 2. 시간외 거래 허용

```yaml
trading:
  market:
    check-enabled: true
    allowed-sessions:
      - REGULAR
      - AFTER_HOURS_CLOSING  # 15:40-16:00 추가
    public-holidays: []
```

**결과**:
- 평일 09:00-15:30: ✅ 주문 허용 (정규장)
- 평일 15:40-16:00: ✅ 주문 허용 (시간외 종가)
- 평일 16:10: ❌ `MARKET_CLOSED`

---

### 3. 거래시간 체크 비활성화

```yaml
trading:
  market:
    check-enabled: false
```

**결과**:
- 모든 시간: ✅ 주문 허용 (체크 안 함)
- 테스트/개발 환경에서 유용

---

## 📋 테스트 시나리오

### 시나리오 1: 정규장 내 주문 성공

```
시간: 2025-01-02 (목) 10:30
설정: check-enabled=true, allowed-sessions=[REGULAR]
기대결과: RiskDecision.approve()
```

### 시나리오 2: 정규장 외 주문 차단

```
시간: 2025-01-02 (목) 16:00
설정: check-enabled=true, allowed-sessions=[REGULAR]
기대결과: RiskDecision.reject("MARKET_CLOSED")
에러코드: MARKET_CLOSED
```

### 시나리오 3: 주말 주문 차단

```
시간: 2025-01-04 (토) 10:00
설정: check-enabled=true, allowed-sessions=[REGULAR]
기대결과: RiskDecision.reject("MARKET_CLOSED")
이유: "Market is closed at 10:00 (weekend)"
```

### 시나리오 4: 공휴일 주문 차단

```
시간: 2025-01-01 (수) 10:00
설정: public-holidays=["2025-01-01"]
기대결과: RiskDecision.reject("MARKET_CLOSED")
이유: "Market is closed (public holiday)"
```

### 시나리오 5: 시간외 종가 허용

```
시간: 2025-01-02 (목) 15:50
설정: allowed-sessions=[REGULAR, AFTER_HOURS_CLOSING]
기대결과: RiskDecision.approve()
현재 세션: AFTER_HOURS_CLOSING
```

### 시나리오 6: 체크 비활성화

```
시간: 2025-01-04 (토) 23:00
설정: check-enabled=false
기대결과: RiskDecision.approve()
이유: Market hours check disabled
```

---

## 📁 신규/수정 파일 목록

### 신규 파일 (3개)

1. **`src/main/java/maru/trading/domain/market/TradingSession.java`**
   - 거래 세션 enum (4개 세션)

2. **`src/main/java/maru/trading/domain/market/MarketHoursPolicy.java`**
   - 도메인 정책 클래스 (stateless)
   - 주말/공휴일/세션 시간 검증

3. **`src/main/java/maru/trading/infra/config/MarketHoursConfig.java`**
   - 설정 클래스 (@ConfigurationProperties)
   - application.yml 매핑

### 수정 파일 (3개)

1. **`src/main/resources/application.yml`**
   - `trading.market.*` 설정 추가
   - 2025년 공휴일 목록 포함

2. **`src/main/java/maru/trading/domain/risk/RiskEngine.java`**
   - 7번째 체크 추가 (checkMarketHours)
   - Backward compatibility 메서드 추가

3. **`src/main/java/maru/trading/application/usecase/trading/EvaluateRiskUseCase.java`**
   - MarketHoursConfig 주입
   - market hours 파라미터 전달

---

## 🎯 설정 관리 팁

### 공휴일 업데이트

매년 초 공휴일 목록 갱신:

```yaml
trading:
  market:
    public-holidays:
      # 2026년 공휴일로 업데이트
      - "2026-01-01"
      - "2026-02-16"  # 설날
      # ...
```

### 환경별 설정

**application-dev.yml** (개발):
```yaml
trading:
  market:
    check-enabled: false  # 개발 중에는 비활성화
```

**application-prod.yml** (운영):
```yaml
trading:
  market:
    check-enabled: true
    allowed-sessions:
      - REGULAR
    public-holidays:
      # 실제 공휴일 목록
```

---

## 🚨 주의사항

1. **공휴일 관리**: 매년 초 `public-holidays` 목록 갱신 필요
2. **임시 휴장일**: 증시 긴급 휴장 시 수동으로 날짜 추가
3. **시간외 거래**: 실제로 시간외 거래를 사용할 경우 `allowed-sessions` 수정
4. **테스트 환경**: 개발/테스트 시 `check-enabled: false` 사용 권장
5. **타임존**: 시스템 시간이 한국 시간(KST)인지 확인 필요

---

## 🎉 구현 완료!

**컴파일 상태**: ✅ BUILD SUCCESS (179 files)
**신규 파일**: 3개
**수정 파일**: 3개
**총 라인 수**: ~600 lines

**다음 단계**:
- 단위 테스트 추가 (MarketHoursPolicy 테스트)
- 통합 테스트 (E2E 시나리오 검증)
- 모니터링 대시보드에 거래시간 상태 표시

---

**작성자**: Claude Sonnet 4.5
**프로젝트**: cautostock - KIS Trading System MVP
**완료일**: 2026-01-01
