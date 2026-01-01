# MaruWeb ↔ Trading System 통합 가이드

## 프로젝트 개요

### Trading System (CAutoStock)
- **위치**: `/Users/changsupark/projects/cautostock`
- **프레임워크**: Spring Boot 3.2.1
- **포트**: 8099
- **역할**: REST API 서버 (자동매매 시스템 백엔드)
- **데이터베이스**: MariaDB `trading_mvp`

### MaruWeb
- **위치**: `/Users/changsupark/projects/maruweb`
- **프레임워크**: Spring Boot 2.7.18 + Thymeleaf
- **포트**: 8080
- **역할**: 웹 프론트엔드 (여러 기능 통합 대시보드)
- **데이터베이스**: MariaDB `maruweb`

---

## 통합 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                    브라우저 (User)                       │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP
                       ▼
┌─────────────────────────────────────────────────────────┐
│              MaruWeb (Frontend Server)                  │
│              http://localhost:8080                      │
├─────────────────────────────────────────────────────────┤
│  Spring Boot 2.7.18 + Thymeleaf                         │
│  ├─ TodoController                                      │
│  ├─ CalendarController                                  │
│  ├─ NoteController                                      │
│  └─ TradingController ← 새로 추가                       │
│                ↓                                         │
│          TradingService (RestTemplate/WebClient)        │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP REST API Call
                       ▼
┌─────────────────────────────────────────────────────────┐
│         Trading System (Backend API Server)             │
│              http://localhost:8099                      │
├─────────────────────────────────────────────────────────┤
│  Spring Boot 3.2.1 REST API                             │
│  ├─ GET  /health                                        │
│  ├─ GET  /api/v1/admin/accounts                        │
│  ├─ POST /api/v1/admin/accounts                        │
│  ├─ GET  /api/v1/admin/strategies                      │
│  ├─ GET  /api/v1/admin/kill-switch                     │
│  ├─ POST /api/v1/admin/kill-switch                     │
│  ├─ GET  /api/v1/query/orders                          │
│  └─ GET  /api/v1/query/positions                       │
└─────────────────────────────────────────────────────────┘
```

---

## MaruWeb에서 Trading System API 호출 방법

### 방법 1: RestTemplate (Spring Boot 2.x 호환) ⭐ 추천

MaruWeb은 Spring Boot 2.7.18을 사용하므로 `RestTemplate`이 가장 호환성이 좋습니다.

#### 1단계: RestTemplate Bean 설정

`maruweb/src/main/java/com/maru/config/RestTemplateConfig.java` 생성:

```java
package com.maru.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate tradingApiRestTemplate(RestTemplateBuilder builder) {
        return builder
            .rootUri("http://localhost:8099")  // Trading System API 주소
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
    }
}
```

#### 2단계: application.properties에 설정 추가

`maruweb/src/main/resources/application.properties`:

```properties
# Trading System API Configuration
trading.api.base-url=http://localhost:8099
trading.api.connect-timeout=5000
trading.api.read-timeout=10000
```

#### 3단계: Trading Service 생성

`maruweb/src/main/java/com/maru/trading/service/TradingApiService.java`:

```java
package com.maru.trading.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradingApiService {

    private final RestTemplate tradingApiRestTemplate;

    @Value("${trading.api.base-url:http://localhost:8099}")
    private String baseUrl;

    /**
     * Health Check
     */
    public Map<String, Object> getHealthStatus() {
        String url = baseUrl + "/health";
        try {
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get health status", e);
            throw new RuntimeException("Trading System API is unavailable", e);
        }
    }

    /**
     * 계좌 목록 조회
     */
    public Map<String, Object> getAccounts() {
        String url = baseUrl + "/api/v1/admin/accounts";
        try {
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get accounts", e);
            throw new RuntimeException("Failed to fetch accounts", e);
        }
    }

    /**
     * Kill Switch 상태 조회
     */
    public Map<String, Object> getKillSwitchStatus() {
        String url = baseUrl + "/api/v1/admin/kill-switch";
        try {
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get kill switch status", e);
            throw new RuntimeException("Failed to fetch kill switch status", e);
        }
    }

    /**
     * 주문 목록 조회
     */
    public Map<String, Object> getOrders(String accountId) {
        String url = baseUrl + "/api/v1/query/orders?accountId=" + accountId;
        try {
            ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get orders for account: {}", accountId, e);
            throw new RuntimeException("Failed to fetch orders", e);
        }
    }
}
```

#### 4단계: Controller 생성

`maruweb/src/main/java/com/maru/trading/controller/TradingController.java`:

```java
package com.maru.trading.controller;

import com.maru.trading.service.TradingApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/trading")
@RequiredArgsConstructor
public class TradingController {

    private final TradingApiService tradingApiService;

    /**
     * Trading Dashboard
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            // Health Check
            Map<String, Object> health = tradingApiService.getHealthStatus();
            model.addAttribute("health", health);

            // Kill Switch Status
            Map<String, Object> killSwitch = tradingApiService.getKillSwitchStatus();
            model.addAttribute("killSwitch", killSwitch);

            // Accounts
            Map<String, Object> accounts = tradingApiService.getAccounts();
            model.addAttribute("accounts", accounts);

            return "trading/dashboard";
        } catch (Exception e) {
            log.error("Failed to load trading dashboard", e);
            model.addAttribute("error", "Trading System에 연결할 수 없습니다.");
            return "trading/error";
        }
    }

    /**
     * 계좌 관리
     */
    @GetMapping("/accounts")
    public String accounts(Model model) {
        try {
            Map<String, Object> accounts = tradingApiService.getAccounts();
            model.addAttribute("accounts", accounts);
            return "trading/accounts";
        } catch (Exception e) {
            log.error("Failed to load accounts", e);
            model.addAttribute("error", e.getMessage());
            return "trading/error";
        }
    }

    /**
     * 주문 조회
     */
    @GetMapping("/orders")
    public String orders(@RequestParam(required = false) String accountId, Model model) {
        try {
            if (accountId != null && !accountId.isEmpty()) {
                Map<String, Object> orders = tradingApiService.getOrders(accountId);
                model.addAttribute("orders", orders);
            }

            // 계좌 목록도 함께 전달 (필터용)
            Map<String, Object> accounts = tradingApiService.getAccounts();
            model.addAttribute("accounts", accounts);

            return "trading/orders";
        } catch (Exception e) {
            log.error("Failed to load orders", e);
            model.addAttribute("error", e.getMessage());
            return "trading/error";
        }
    }
}
```

#### 5단계: Thymeleaf 템플릿 생성

`maruweb/src/main/resources/templates/trading/dashboard.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Trading Dashboard</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .status-box {
            border: 1px solid #ddd;
            padding: 15px;
            margin: 10px 0;
            border-radius: 5px;
        }
        .status-up { background-color: #d4edda; }
        .status-down { background-color: #f8d7da; }
        .kill-switch { font-size: 24px; font-weight: bold; }
        .kill-switch.off { color: green; }
        .kill-switch.on { color: red; }
    </style>
</head>
<body>
    <h1>📊 Trading System Dashboard</h1>

    <!-- System Health -->
    <div class="status-box" th:classappend="${health.status == 'UP'} ? 'status-up' : 'status-down'">
        <h2>System Status: <span th:text="${health.status}">UP</span></h2>
        <ul>
            <li>Database: <span th:text="${health.components?.db}">UP</span></li>
            <li>KIS REST: <span th:text="${health.components?.kisRest}">UP</span></li>
            <li>KIS WebSocket: <span th:text="${health.components?.kisWs}">UP</span></li>
            <li>Token: <span th:text="${health.components?.token}">VALID</span></li>
        </ul>
    </div>

    <!-- Kill Switch -->
    <div class="status-box">
        <h2>⚡ Kill Switch</h2>
        <p class="kill-switch" th:classappend="${killSwitch.status == 'OFF'} ? 'off' : 'on'">
            Status: <span th:text="${killSwitch.status}">OFF</span>
        </p>
    </div>

    <!-- Accounts Summary -->
    <div class="status-box">
        <h2>💼 Accounts</h2>
        <p>Total Accounts: <span th:text="${accounts?.items?.size() ?: 0}">0</span></p>
        <ul>
            <li th:each="account : ${accounts.items}">
                <span th:text="${account.alias}">Account</span>
                (<span th:text="${account.environment}">PAPER</span>)
                - Status: <span th:text="${account.status}">ACTIVE</span>
            </li>
        </ul>
    </div>

    <hr>
    <p>
        <a href="/trading/accounts">계좌 관리</a> |
        <a href="/trading/orders">주문 조회</a> |
        <a href="/">홈으로</a>
    </p>
</body>
</html>
```

---

### 방법 2: WebClient (비동기 방식)

Spring WebFlux의 `WebClient`를 사용하는 방법 (선택사항):

#### pom.xml에 의존성 추가

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

#### WebClient Bean 설정

```java
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient tradingApiWebClient() {
        return WebClient.builder()
            .baseUrl("http://localhost:8099")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
}
```

---

## 환경별 설정

### 개발 환경 (application-dev.properties)

```properties
# Trading System API (로컬 개발)
trading.api.base-url=http://localhost:8099
trading.api.connect-timeout=5000
trading.api.read-timeout=10000
```

### 프로덕션 환경 (application-prod.properties)

```properties
# Trading System API (프로덕션 - 다른 서버)
trading.api.base-url=http://trading-api.maru.com:8099
trading.api.connect-timeout=3000
trading.api.read-timeout=5000
```

---

## 메뉴 구조 제안

MaruWeb의 네비게이션에 Trading System 메뉴 추가:

```
MaruWeb
├─ 홈 (Dashboard)
├─ Todo
├─ Calendar
├─ Note
├─ Shortcut
├─ D-Day
├─ Habit
└─ Trading ← 새로 추가
   ├─ Dashboard (시스템 상태 + Kill Switch)
   ├─ 계좌 관리
   ├─ 전략 관리
   ├─ 주문 조회
   └─ 포지션/손익
```

---

## 실행 순서

### 1. Trading System 실행

```bash
cd /Users/changsupark/projects/cautostock
./run-with-env.sh

# 또는
export JAVA_HOME=/Users/changsupark/Library/Java/JavaVirtualMachines/corretto-17.0.5/Contents/Home
export SPRING_DATASOURCE_USERNAME=nextman
export SPRING_DATASOURCE_PASSWORD=1111
mvn spring-boot:run
```

**확인**: http://localhost:8099/health

### 2. MaruWeb 실행

```bash
cd /Users/changsupark/projects/maruweb
mvn spring-boot:run
```

**확인**: http://localhost:8080

### 3. Trading 페이지 접속

http://localhost:8080/trading/dashboard

---

## 에러 처리

### Connection Refused

```java
@ControllerAdvice
public class TradingExceptionHandler {

    @ExceptionHandler(ResourceAccessException.class)
    public String handleConnectionError(Model model) {
        model.addAttribute("error", "Trading System API에 연결할 수 없습니다. API 서버가 실행 중인지 확인하세요.");
        return "trading/error";
    }
}
```

### Timeout

RestTemplate 설정에서 타임아웃 조정:

```java
.setConnectTimeout(Duration.ofSeconds(5))
.setReadTimeout(Duration.ofSeconds(10))
```

---

## CORS 문제 해결 (필요시)

만약 JavaScript에서 직접 API를 호출한다면 Trading System에 CORS 설정 필요:

`cautostock/src/main/java/maru/trading/infra/config/WebConfig.java`:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:8080")  // MaruWeb 주소
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

---

## 보안 고려사항

### 1. API 키 인증 (향후 추가)

Trading System에 API 키 인증 추가:

```java
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler) {
        String apiKey = request.getHeader("X-API-Key");
        if (!"your-secure-api-key".equals(apiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        return true;
    }
}
```

MaruWeb에서 API 호출 시 헤더 추가:

```java
HttpHeaders headers = new HttpHeaders();
headers.set("X-API-Key", "your-secure-api-key");
HttpEntity<String> entity = new HttpEntity<>(headers);
```

### 2. HTTPS 사용 (프로덕션)

프로덕션에서는 반드시 HTTPS 사용

---

## 개발 체크리스트

- [ ] RestTemplate Bean 설정
- [ ] application.properties에 Trading API URL 설정
- [ ] TradingApiService 생성
- [ ] TradingController 생성
- [ ] Thymeleaf 템플릿 생성 (dashboard.html)
- [ ] 네비게이션 메뉴에 Trading 링크 추가
- [ ] Trading System 실행 확인 (8099 포트)
- [ ] MaruWeb 실행 확인 (8080 포트)
- [ ] 통합 테스트 (http://localhost:8080/trading/dashboard)

---

## 참고 자료

- Trading System API 명세: `/Users/changsupark/projects/cautostock/md/docs/04_API_OPENAPI.md`
- Trading System 실행 가이드: `/Users/changsupark/projects/cautostock/RUN_GUIDE.md`
- Trading System 테스트 시나리오: `/Users/changsupark/projects/cautostock/TEST_SCENARIOS.md`
