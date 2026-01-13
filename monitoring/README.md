# Trading System - Monitoring Setup Guide

## 개요

이 디렉토리는 Trading System의 모니터링 스택을 포함합니다:
- **Prometheus**: 메트릭 수집 및 저장
- **Grafana**: 메트릭 시각화 및 대시보드

## 빠른 시작

### 1. 사전 요구사항

- Docker 및 Docker Compose 설치
- Trading System 애플리케이션이 8080 포트에서 실행 중
- `/actuator/prometheus` 엔드포인트 활성화

### 2. 모니터링 스택 시작

```bash
# 프로젝트 루트에서 실행
cd /Users/changsupark/projects/cautostock

# 모니터링 스택 시작
docker-compose -f monitoring/docker-compose.yml up -d

# 로그 확인
docker-compose -f monitoring/docker-compose.yml logs -f
```

### 3. 접속 정보

- **Prometheus**: http://localhost:9090
  - 메트릭 쿼리 및 확인

- **Grafana**: http://localhost:3000
  - 기본 계정: `admin` / `admin`
  - 첫 로그인 시 비밀번호 변경 요구됨

### 4. 대시보드 확인

Grafana에 로그인 후:
1. 좌측 메뉴에서 "Dashboards" 클릭
2. "Trading System Dashboard" 선택
3. 실시간 메트릭 확인

## 디렉토리 구조

```
monitoring/
├── docker-compose.yml              # Docker Compose 설정
├── prometheus.yml                  # Prometheus 수집 설정
├── grafana/
│   ├── provisioning/
│   │   ├── datasources/
│   │   │   └── prometheus.yml     # Prometheus 데이터소스 자동 설정
│   │   └── dashboards/
│   │       └── default.yml        # 대시보드 프로비저닝 설정
│   └── dashboards/
│       └── trading-system-dashboard.json  # 메인 대시보드
└── README.md                       # 이 파일
```

## 대시보드 패널 설명

### JVM 메트릭
1. **JVM Memory Usage**: Heap/Non-heap 메모리 사용량
2. **GC Pause Time Rate**: Garbage Collection 일시정지 시간

### HTTP 메트릭
3. **HTTP Request Rate**: API 요청 처리율 (requests/sec)
4. **HTTP Request Latency (P95/P99)**: API 응답 시간 백분위수

### 데이터베이스 메트릭
5. **DB Active Connections**: 활성 DB 커넥션 수 (게이지)
6. **DB Idle Connections**: 유휴 DB 커넥션 수 (게이지)
7. **DB Connection Acquire Time**: 커넥션 획득 시간

### 트레이딩 메트릭
8. **Kill Switch Status**: Kill Switch 활성화 상태 (0=정상, 1=활성화)
9. **Orders (5m)**: 최근 5분간 주문 건수 (상태별)
10. **Total Signals Generated**: 총 생성된 시그널 수
11. **Current Positions (Quantity)**: 현재 포지션 수량 (종목별)
12. **Profit & Loss (PnL)**: 실현/미실현 손익

## Prometheus 메트릭 커스터마이징

### 애플리케이션에서 메트릭 추가하기

Spring Boot 애플리케이션에서 Micrometer를 사용하여 커스텀 메트릭 추가:

```java
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;

@Service
public class TradingMetricsService {
    private final Counter signalCounter;
    private final Gauge killSwitchGauge;

    public TradingMetricsService(MeterRegistry registry) {
        this.signalCounter = Counter.builder("trading.signals.total")
                .description("Total signals generated")
                .tag("application", "trading-system")
                .register(registry);

        this.killSwitchGauge = Gauge.builder("trading.kill_switch.enabled", this,
                service -> service.isKillSwitchEnabled() ? 1.0 : 0.0)
                .description("Kill switch status (0=disabled, 1=enabled)")
                .tag("application", "trading-system")
                .register(registry);
    }

    private boolean isKillSwitchEnabled() {
        // Kill Switch 상태 조회 로직
        return false;
    }

    public void recordSignal() {
        signalCounter.increment();
    }
}
```

### Prometheus 수집 대상 추가

`prometheus.yml` 파일의 `scrape_configs` 섹션에 추가:

```yaml
scrape_configs:
  - job_name: 'new-service'
    metrics_path: '/metrics'
    static_configs:
      - targets: ['localhost:9999']
        labels:
          application: 'new-service'
```

## Grafana 대시보드 커스터마이징

### 새 패널 추가하기

1. Grafana 대시보드 열기
2. 우측 상단 "Add panel" 클릭
3. "Add a new panel" 선택
4. Query 입력 (예: `trading_orders_total`)
5. Visualization 선택 (Time series, Gauge, Stat 등)
6. 패널 제목 및 설정 지정
7. "Apply" 클릭

### 대시보드 변경사항 저장

변경한 대시보드를 JSON으로 저장:

1. 대시보드 설정 (톱니바퀴 아이콘) 클릭
2. "JSON Model" 선택
3. JSON 복사
4. `grafana/dashboards/trading-system-dashboard.json`에 붙여넣기
5. Git에 커밋하여 버전 관리

## 알림 설정 (옵션)

### Prometheus Alertmanager 추가

`docker-compose.yml`에 Alertmanager 서비스 추가:

```yaml
alertmanager:
  image: prom/alertmanager:v0.26.0
  ports:
    - "9093:9093"
  volumes:
    - ./alertmanager.yml:/etc/alertmanager/alertmanager.yml
    - alertmanager-data:/alertmanager
  networks:
    - monitoring
```

### 알림 규칙 정의

`prometheus.yml`에 규칙 파일 추가:

```yaml
rule_files:
  - "alerts/trading-alerts.yml"
```

`alerts/trading-alerts.yml` 생성:

```yaml
groups:
  - name: trading_alerts
    interval: 30s
    rules:
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage detected"
          description: "Heap memory usage is above 90%"

      - alert: KillSwitchActivated
        expr: trading_kill_switch_enabled == 1
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Kill Switch activated"
          description: "Trading has been halted by Kill Switch"
```

## 데이터 보관 정책

### Prometheus 데이터 보관 기간 변경

`docker-compose.yml`의 Prometheus 설정에서:

```yaml
command:
  - '--storage.tsdb.retention.time=90d'  # 30일 → 90일로 변경
```

### 데이터 백업

```bash
# Prometheus 데이터 백업
docker-compose -f monitoring/docker-compose.yml stop prometheus
tar -czf prometheus-backup-$(date +%Y%m%d).tar.gz -C /var/lib/docker/volumes/monitoring_prometheus-data/_data .
docker-compose -f monitoring/docker-compose.yml start prometheus

# Grafana 데이터 백업
docker-compose -f monitoring/docker-compose.yml stop grafana
tar -czf grafana-backup-$(date +%Y%m%d).tar.gz -C /var/lib/docker/volumes/monitoring_grafana-data/_data .
docker-compose -f monitoring/docker-compose.yml start grafana
```

## 모니터링 스택 중지 및 제거

```bash
# 중지 (데이터 유지)
docker-compose -f monitoring/docker-compose.yml stop

# 중지 및 컨테이너 제거 (데이터 유지)
docker-compose -f monitoring/docker-compose.yml down

# 중지 및 모든 데이터 제거 (주의!)
docker-compose -f monitoring/docker-compose.yml down -v
```

## 트러블슈팅

### Prometheus가 Trading System에 연결되지 않음

1. Trading System이 실행 중인지 확인
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

2. Docker 내부에서 호스트 접근 확인
   - macOS/Windows: `host.docker.internal` 사용
   - Linux: `--network host` 또는 실제 IP 사용

### Grafana 대시보드가 표시되지 않음

1. Prometheus 데이터소스 연결 확인
   - Grafana → Configuration → Data sources → Prometheus
   - "Test" 버튼 클릭하여 연결 확인

2. 대시보드 프로비저닝 확인
   ```bash
   docker-compose -f monitoring/docker-compose.yml logs grafana
   ```

### 메트릭이 수집되지 않음

1. Prometheus targets 확인
   - http://localhost:9090/targets
   - Trading System의 상태가 "UP"인지 확인

2. Spring Boot Actuator 설정 확인
   - `application.yml`에 `management.endpoints.web.exposure.include: prometheus` 추가

## 참고 자료

- [Prometheus Documentation](https://prometheus.io/docs/introduction/overview/)
- [Grafana Documentation](https://grafana.com/docs/grafana/latest/)
- [Spring Boot Actuator with Prometheus](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics.export.prometheus)
- [Micrometer Documentation](https://micrometer.io/docs)
