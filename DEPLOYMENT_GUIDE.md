# Trading System - 프로덕션 배포 가이드

**Version**: 1.0.0
**Last Updated**: 2026-01-01
**Status**: ✅ Production Ready

---

## 📋 목차

1. [배포 개요](#배포-개요)
2. [사전 요구사항](#사전-요구사항)
3. [환경 설정](#환경-설정)
4. [데이터베이스 설정](#데이터베이스-설정)
5. [애플리케이션 빌드](#애플리케이션-빌드)
6. [배포 단계](#배포-단계)
7. [모니터링 및 운영](#모니터링-및-운영)
8. [보안 고려사항](#보안-고려사항)
9. [백업 및 복구](#백업-및-복구)
10. [트러블슈팅](#트러블슈팅)

---

## 배포 개요

### 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                    Load Balancer (Optional)              │
└─────────────────────┬───────────────────────────────────┘
                      │
         ┌────────────┴────────────┐
         │                         │
    ┌────▼─────┐            ┌─────▼────┐
    │  App 1   │            │  App 2   │  (Optional - HA)
    │ (Primary)│            │(Standby) │
    └────┬─────┘            └─────┬────┘
         │                        │
         └────────────┬───────────┘
                      │
         ┌────────────▼────────────┐
         │      MariaDB (Primary)   │
         │   + Replica (Optional)   │
         └──────────────────────────┘
```

### 배포 모드

| 모드 | 설명 | 용도 |
|------|------|------|
| **PAPER** | KIS 모의투자 계좌 | 테스트, 검증 |
| **LIVE** | KIS 실전 계좌 | 프로덕션 (⚠️ 주의) |

> ⚠️ **중요**: 처음 배포 시 반드시 **PAPER 모드**로 시작하여 충분히 검증 후 LIVE 모드로 전환하세요.

---

## 사전 요구사항

### 하드웨어 요구사항

**최소 사양**:
- CPU: 2 cores
- RAM: 4GB
- Disk: 20GB (SSD 권장)
- Network: 100Mbps 이상

**권장 사양** (프로덕션):
- CPU: 4+ cores
- RAM: 8GB+
- Disk: 100GB SSD
- Network: 1Gbps 이상

### 소프트웨어 요구사항

| 소프트웨어 | 버전 | 필수 여부 |
|----------|------|----------|
| Java | 17+ | ✅ 필수 |
| MariaDB | 10.x+ | ✅ 필수 |
| Gradle | 8.x | ✅ 필수 |
| systemd | - | 권장 (자동 시작) |
| nginx | - | 선택 (리버스 프록시) |

### KIS API 요구사항

1. **KIS 계좌 개설**
   - 한국투자증권 계좌 필요
   - 모의투자(PAPER) 계좌 또는 실전(LIVE) 계좌

2. **KIS OpenAPI 등록**
   - KIS Developers 사이트 접속: https://apiportal.koreainvestment.com
   - APP KEY 발급 (PAPER/LIVE 각각)
   - APP SECRET 발급
   - IP 화이트리스트 등록 (필요 시)

---

## 환경 설정

### 1. Java 17 설치

**macOS**:
```bash
# Homebrew 사용
brew install openjdk@17

# 환경 변수 설정
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
source ~/.zshrc

# 확인
java -version
```

**Linux (Ubuntu/Debian)**:
```bash
sudo apt update
sudo apt install openjdk-17-jdk -y

# 환경 변수 설정
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
source ~/.bashrc

# 확인
java -version
```

**Linux (CentOS/RHEL)**:
```bash
sudo yum install java-17-openjdk-devel -y

# 환경 변수 설정
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk' >> ~/.bashrc
source ~/.bashrc
```

### 2. Gradle 설치

```bash
# macOS
brew install gradle

# Linux - SDKMAN 사용 권장
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install gradle 8.5

# 확인
gradle --version
```

### 3. MariaDB 설치

**macOS**:
```bash
brew install mariadb
brew services start mariadb

# 초기 보안 설정
mysql_secure_installation
```

**Linux (Ubuntu/Debian)**:
```bash
sudo apt install mariadb-server -y
sudo systemctl start mariadb
sudo systemctl enable mariadb

# 초기 보안 설정
sudo mysql_secure_installation
```

**보안 설정 권장 사항**:
- Root 비밀번호 설정: ✅ Yes
- Anonymous user 제거: ✅ Yes
- Root 원격 로그인 차단: ✅ Yes
- Test 데이터베이스 제거: ✅ Yes

---

## 데이터베이스 설정

### 1. 데이터베이스 및 사용자 생성

```sql
-- MariaDB 접속
mysql -u root -p

-- 데이터베이스 생성
CREATE DATABASE trading_mvp
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- 사용자 생성 (로컬)
CREATE USER 'trading_user'@'localhost'
  IDENTIFIED BY 'SECURE_PASSWORD_HERE';

-- 사용자 생성 (원격 - 필요 시)
CREATE USER 'trading_user'@'%'
  IDENTIFIED BY 'SECURE_PASSWORD_HERE';

-- 권한 부여
GRANT ALL PRIVILEGES ON trading_mvp.*
  TO 'trading_user'@'localhost';

GRANT ALL PRIVILEGES ON trading_mvp.*
  TO 'trading_user'@'%';

FLUSH PRIVILEGES;

-- 확인
SHOW GRANTS FOR 'trading_user'@'localhost';
```

> ⚠️ **보안**: `SECURE_PASSWORD_HERE`를 강력한 비밀번호로 변경하세요 (최소 16자, 대소문자+숫자+특수문자).

### 2. MariaDB 설정 최적화

**/etc/mysql/mariadb.conf.d/50-server.cnf** (Linux) 또는 **/usr/local/etc/my.cnf** (macOS):

```ini
[mysqld]
# Basic Settings
max_connections = 200
max_allowed_packet = 64M
thread_cache_size = 16
query_cache_size = 32M

# InnoDB Settings
innodb_buffer_pool_size = 2G  # 시스템 RAM의 50-70%
innodb_log_file_size = 256M
innodb_flush_log_at_trx_commit = 2  # 성능 우선 (1=안전성 우선)
innodb_flush_method = O_DIRECT

# Character Set
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci

# Logging
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow-query.log
long_query_time = 2

# Binary Logging (백업/복제 필요 시)
log_bin = /var/log/mysql/mysql-bin.log
expire_logs_days = 7
max_binlog_size = 100M
```

**재시작**:
```bash
# macOS
brew services restart mariadb

# Linux
sudo systemctl restart mariadb
```

### 3. Flyway 마이그레이션 실행

애플리케이션 시작 시 자동으로 실행되지만, 수동으로 실행하려면:

```bash
./gradlew flywayMigrate \
  -Dflyway.url=jdbc:mariadb://localhost:3306/trading_mvp \
  -Dflyway.user=trading_user \
  -Dflyway.password=SECURE_PASSWORD_HERE
```

**마이그레이션 확인**:
```sql
USE trading_mvp;
SHOW TABLES;

-- 총 12개 테이블 확인:
-- accounts, strategies, strategy_versions, strategy_symbols
-- risk_rules, risk_states, orders, fills, positions
-- pnl_ledgers, alerts, outbox
```

---

## 애플리케이션 빌드

### 1. 소스 코드 다운로드

```bash
# Git clone (프로젝트가 Git 저장소인 경우)
git clone <repository-url>
cd cautostock

# 또는 압축 파일 업로드 후 압축 해제
unzip cautostock.zip
cd cautostock
```

### 2. 설정 파일 작성

**application-prod.yml** 생성:

```yaml
# src/main/resources/application-prod.yml

spring:
  application:
    name: trading-system

  datasource:
    url: jdbc:mariadb://localhost:3306/trading_mvp?useUnicode=true&characterEncoding=utf8mb4
    username: trading_user
    password: ${DB_PASSWORD}  # 환경 변수에서 읽음
    driver-class-name: org.mariadb.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  jpa:
    hibernate:
      ddl-auto: validate  # 프로덕션에서는 validate만 사용
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MariaDBDialect
        format_sql: false
        show_sql: false
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true

  flyway:
    enabled: true
    baseline-on-migrate: true
    validate-on-migrate: true

# Trading Configuration
trading:
  broker:
    kis:
      paper:
        enabled: true
        app-key: ${KIS_PAPER_APP_KEY}
        app-secret: ${KIS_PAPER_APP_SECRET}
        base-url: https://openapi.koreainvestment.com:9443
      live:
        enabled: false  # LIVE 모드는 비활성화 (프로덕션 준비 완료 전까지)
        app-key: ${KIS_LIVE_APP_KEY}
        app-secret: ${KIS_LIVE_APP_SECRET}
        base-url: https://openapi.koreainvestment.com:9443

  demo:
    enabled: false  # 프로덕션에서는 데모 비활성화

  scheduler:
    strategy-execution:
      enabled: true
      cron: "0 * * * * ?"  # 매분 실행
    outbox-publisher:
      enabled: true
      fixed-delay-ms: 5000  # 5초마다 실행

# Logging
logging:
  level:
    root: INFO
    maru.trading: INFO
    org.hibernate: WARN
    org.springframework: WARN
  file:
    name: /var/log/trading-system/application.log
    max-size: 100MB
    max-history: 30
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true

# Server
server:
  port: 8080
  shutdown: graceful
  tomcat:
    threads:
      max: 200
      min-spare: 10
```

### 3. 환경 변수 설정

**배포 스크립트 생성** (`deploy.sh`):

```bash
#!/bin/bash

# 환경 변수 설정
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export DB_PASSWORD="SECURE_PASSWORD_HERE"
export KIS_PAPER_APP_KEY="your_paper_app_key"
export KIS_PAPER_APP_SECRET="your_paper_app_secret"

# LIVE 모드 사용 시 (⚠️ 주의)
# export KIS_LIVE_APP_KEY="your_live_app_key"
# export KIS_LIVE_APP_SECRET="your_live_app_secret"

# Spring Profile 설정
export SPRING_PROFILES_ACTIVE=prod

# JVM 옵션
export JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

echo "Environment variables set successfully"
```

**실행 권한 부여**:
```bash
chmod +x deploy.sh
chmod 600 deploy.sh  # 보안: 소유자만 읽기 가능
```

### 4. 빌드 실행

```bash
# 환경 변수 로드
source deploy.sh

# 테스트 포함 빌드
./gradlew clean build

# 테스트 제외 빌드 (프로덕션 배포 시)
./gradlew clean build -x test

# 빌드 결과 확인
ls -lh build/libs/
# trading-system-0.1.0-SNAPSHOT.jar 확인
```

---

## 배포 단계

### 옵션 1: 직접 실행 (개발/테스트)

```bash
# 환경 변수 로드
source deploy.sh

# JAR 파일 실행
java $JAVA_OPTS -jar build/libs/trading-system-0.1.0-SNAPSHOT.jar \
  --spring.profiles.active=prod

# 또는 Gradle로 실행
./gradlew bootRun --args='--spring.profiles.active=prod'
```

### 옵션 2: systemd 서비스 (권장)

**1. Systemd 서비스 파일 생성**:

```bash
sudo vim /etc/systemd/system/trading-system.service
```

**내용**:
```ini
[Unit]
Description=Trading System - KIS Automated Trading
After=network.target mariadb.service
Wants=mariadb.service

[Service]
Type=simple
User=trading
Group=trading
WorkingDirectory=/opt/trading-system

# 환경 변수
Environment="JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64"
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="JAVA_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 민감 정보는 EnvironmentFile로 분리
EnvironmentFile=/opt/trading-system/config/env.conf

# 실행 명령
ExecStart=/usr/bin/java $JAVA_OPTS \
  -jar /opt/trading-system/trading-system.jar \
  --spring.profiles.active=prod

# 재시작 정책
Restart=always
RestartSec=10

# 리소스 제한
LimitNOFILE=65536
MemoryLimit=6G

# 로그
StandardOutput=journal
StandardError=journal
SyslogIdentifier=trading-system

[Install]
WantedBy=multi-user.target
```

**2. 환경 변수 파일 생성**:

```bash
sudo mkdir -p /opt/trading-system/config
sudo vim /opt/trading-system/config/env.conf
```

**내용** (`env.conf`):
```bash
DB_PASSWORD=SECURE_PASSWORD_HERE
KIS_PAPER_APP_KEY=your_paper_app_key
KIS_PAPER_APP_SECRET=your_paper_app_secret
# KIS_LIVE_APP_KEY=your_live_app_key
# KIS_LIVE_APP_SECRET=your_live_app_secret
```

**보안 설정**:
```bash
sudo chmod 600 /opt/trading-system/config/env.conf
sudo chown trading:trading /opt/trading-system/config/env.conf
```

**3. 전용 사용자 생성**:

```bash
# trading 사용자 생성
sudo useradd -r -s /bin/false trading

# 디렉토리 생성 및 권한 설정
sudo mkdir -p /opt/trading-system
sudo mkdir -p /var/log/trading-system

sudo chown -R trading:trading /opt/trading-system
sudo chown -R trading:trading /var/log/trading-system
```

**4. JAR 파일 배포**:

```bash
# JAR 파일 복사
sudo cp build/libs/trading-system-0.1.0-SNAPSHOT.jar \
  /opt/trading-system/trading-system.jar

# 설정 파일 복사
sudo cp src/main/resources/application-prod.yml \
  /opt/trading-system/config/

# 권한 설정
sudo chown trading:trading /opt/trading-system/trading-system.jar
sudo chmod 500 /opt/trading-system/trading-system.jar
```

**5. 서비스 시작**:

```bash
# systemd 데몬 리로드
sudo systemctl daemon-reload

# 서비스 활성화 (부팅 시 자동 시작)
sudo systemctl enable trading-system

# 서비스 시작
sudo systemctl start trading-system

# 상태 확인
sudo systemctl status trading-system

# 로그 확인
sudo journalctl -u trading-system -f
```

### 옵션 3: Docker 배포 (선택)

**Dockerfile 생성**:

```dockerfile
FROM openjdk:17-jdk-slim

# 작업 디렉토리
WORKDIR /app

# JAR 파일 복사
COPY build/libs/trading-system-0.1.0-SNAPSHOT.jar app.jar

# 포트 노출
EXPOSE 8080

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**docker-compose.yml 생성**:

```yaml
version: '3.8'

services:
  trading-app:
    build: .
    container_name: trading-system
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JAVA_OPTS=-Xms2g -Xmx4g
      - DB_PASSWORD=${DB_PASSWORD}
      - KIS_PAPER_APP_KEY=${KIS_PAPER_APP_KEY}
      - KIS_PAPER_APP_SECRET=${KIS_PAPER_APP_SECRET}
    volumes:
      - ./logs:/var/log/trading-system
    depends_on:
      - mariadb
    restart: unless-stopped
    networks:
      - trading-network

  mariadb:
    image: mariadb:10.11
    container_name: trading-db
    environment:
      - MYSQL_ROOT_PASSWORD=${DB_ROOT_PASSWORD}
      - MYSQL_DATABASE=trading_mvp
      - MYSQL_USER=trading_user
      - MYSQL_PASSWORD=${DB_PASSWORD}
    volumes:
      - db-data:/var/lib/mysql
    ports:
      - "3306:3306"
    restart: unless-stopped
    networks:
      - trading-network

volumes:
  db-data:

networks:
  trading-network:
    driver: bridge
```

**실행**:
```bash
# 빌드 및 시작
docker-compose up -d

# 로그 확인
docker-compose logs -f trading-app

# 중지
docker-compose down
```

---

## 모니터링 및 운영

### 1. Health Check

**엔드포인트**:
```bash
# 기본 헬스 체크
curl http://localhost:8080/actuator/health

# 상세 헬스 체크 (인증 필요 시)
curl http://localhost:8080/actuator/health -H "Authorization: Bearer <token>"
```

**응답 예시**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MariaDB",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

### 2. Metrics (Prometheus)

**Prometheus 설정** (`prometheus.yml`):

```yaml
scrape_configs:
  - job_name: 'trading-system'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

**주요 메트릭**:
- `jvm_memory_used_bytes` - JVM 메모리 사용량
- `jvm_gc_pause_seconds` - GC 시간
- `http_server_requests_seconds` - HTTP 요청 응답 시간
- `hikaricp_connections_active` - DB 커넥션 수
- `logback_events_total` - 로그 이벤트 수

### 3. 로그 모니터링

**로그 레벨 동적 변경**:
```bash
# 특정 패키지 로그 레벨 변경
curl -X POST http://localhost:8080/actuator/loggers/maru.trading \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"DEBUG"}'

# 확인
curl http://localhost:8080/actuator/loggers/maru.trading
```

**로그 파일 로테이션**:

```bash
# logrotate 설정
sudo vim /etc/logrotate.d/trading-system
```

```
/var/log/trading-system/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 0640 trading trading
    sharedscripts
    postrotate
        systemctl reload trading-system > /dev/null 2>&1 || true
    endscript
}
```

### 4. 알림 설정

**Kill Switch 알림**:

Trading 시스템은 리스크 한도 초과 시 자동으로 Kill Switch를 활성화합니다.
`alerts` 테이블을 주기적으로 조회하여 알림을 전송하세요:

```sql
-- 최근 1시간 Critical 알림 조회
SELECT * FROM alerts
WHERE severity = 'CRITICAL'
  AND created_at > NOW() - INTERVAL 1 HOUR
ORDER BY created_at DESC;
```

**슬랙/이메일 연동** (추가 개발 필요):
- Outbox 패턴으로 `ALERT_TRIGGERED` 이벤트 발행
- 별도 알림 서비스에서 이벤트 구독

---

## 보안 고려사항

### 1. API Key 보안

**절대 하지 말아야 할 것**:
- ❌ Git에 API 키 커밋
- ❌ 코드에 하드코딩
- ❌ 로그에 API 키 출력

**해야 할 것**:
- ✅ 환경 변수로 관리
- ✅ 파일 권한 600 (소유자만 읽기)
- ✅ `.gitignore`에 `env.conf` 추가
- ✅ 주기적으로 API 키 갱신

### 2. 데이터베이스 보안

```sql
-- 강력한 비밀번호 사용 (예시)
ALTER USER 'trading_user'@'localhost'
  IDENTIFIED BY 'aB3$xY9#pQw2!Zr7';

-- 불필요한 권한 제거
REVOKE ALL PRIVILEGES ON *.* FROM 'trading_user'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON trading_mvp.*
  TO 'trading_user'@'localhost';

-- 원격 접속 제한 (필요 시)
DELETE FROM mysql.user WHERE User='trading_user' AND Host='%';
FLUSH PRIVILEGES;
```

### 3. 네트워크 보안

**방화벽 설정** (ufw 예시):

```bash
# 기본 정책: 모든 incoming 차단
sudo ufw default deny incoming
sudo ufw default allow outgoing

# SSH 허용 (22)
sudo ufw allow 22/tcp

# 애플리케이션 포트 (8080) - 로컬만 허용
sudo ufw allow from 127.0.0.1 to any port 8080

# MariaDB (3306) - 로컬만 허용
sudo ufw allow from 127.0.0.1 to any port 3306

# 방화벽 활성화
sudo ufw enable

# 상태 확인
sudo ufw status
```

**nginx 리버스 프록시** (외부 접속 필요 시):

```nginx
server {
    listen 443 ssl http2;
    server_name trading.yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/trading.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/trading.yourdomain.com/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Actuator는 외부 노출 금지
    location /actuator {
        deny all;
    }
}
```

### 4. Kill Switch 보안

```bash
# Kill Switch 상태 확인
curl http://localhost:8080/api/v1/admin/kill-switch

# Kill Switch 활성화 (긴급 상황)
curl -X POST http://localhost:8080/api/v1/admin/kill-switch \
  -H "Content-Type: application/json" \
  -d '{"enabled": true}'
```

> ⚠️ **중요**: Kill Switch는 모든 자동 거래를 즉시 중단합니다. 긴급 상황 발생 시 활성화하세요.

---

## 백업 및 복구

### 1. 데이터베이스 백업

**자동 백업 스크립트** (`backup-db.sh`):

```bash
#!/bin/bash

BACKUP_DIR="/var/backups/trading-system"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/trading_mvp_$TIMESTAMP.sql"

# 디렉토리 생성
mkdir -p $BACKUP_DIR

# MariaDB 덤프
mysqldump -u trading_user -p$DB_PASSWORD \
  --single-transaction \
  --routines \
  --triggers \
  trading_mvp > $BACKUP_FILE

# 압축
gzip $BACKUP_FILE

# 30일 이상 된 백업 삭제
find $BACKUP_DIR -name "*.sql.gz" -mtime +30 -delete

echo "Backup completed: $BACKUP_FILE.gz"
```

**크론탭 설정** (매일 새벽 2시):

```bash
crontab -e

# 추가
0 2 * * * /opt/trading-system/scripts/backup-db.sh >> /var/log/trading-system/backup.log 2>&1
```

### 2. 데이터베이스 복구

```bash
# 백업 파일 압축 해제
gunzip trading_mvp_20260101_020000.sql.gz

# 복구
mysql -u trading_user -p$DB_PASSWORD trading_mvp < trading_mvp_20260101_020000.sql

# 확인
mysql -u trading_user -p$DB_PASSWORD -e "SELECT COUNT(*) FROM trading_mvp.orders;"
```

### 3. 애플리케이션 백업

```bash
# JAR 파일 백업
cp /opt/trading-system/trading-system.jar \
   /var/backups/trading-system/trading-system_$(date +%Y%m%d).jar

# 설정 파일 백업
tar -czf /var/backups/trading-system/config_$(date +%Y%m%d).tar.gz \
  /opt/trading-system/config/
```

---

## 트러블슈팅

### 문제 1: 애플리케이션이 시작되지 않음

**증상**:
```
Application failed to start
```

**해결 방법**:

1. **로그 확인**:
```bash
sudo journalctl -u trading-system -n 100 --no-pager
```

2. **DB 연결 확인**:
```bash
mysql -u trading_user -p$DB_PASSWORD -e "SELECT 1;"
```

3. **포트 충돌 확인**:
```bash
sudo lsof -i :8080
```

4. **Java 버전 확인**:
```bash
java -version  # 17+ 필요
```

### 문제 2: Kill Switch가 자동으로 활성화됨

**원인**:
- 일일 손실 한도 초과
- 연속 실패 제한 초과
- 포지션 노출 한도 초과

**확인**:
```sql
-- 리스크 상태 조회
SELECT * FROM risk_states ORDER BY updated_at DESC LIMIT 1;

-- 최근 알림 조회
SELECT * FROM alerts WHERE severity = 'CRITICAL' ORDER BY created_at DESC LIMIT 10;
```

**해결**:
1. 원인 분석 (손실/포지션/빈도 확인)
2. 리스크 설정 조정 (필요 시)
3. Kill Switch 수동 해제 (충분한 검토 후)

```bash
curl -X POST http://localhost:8080/api/v1/admin/kill-switch \
  -H "Content-Type: application/json" \
  -d '{"enabled": false}'
```

### 문제 3: WebSocket 연결 실패

**증상**:
```
WebSocket connection failed: Connection refused
```

**해결 방법**:

1. **KIS API 상태 확인**:
   - KIS Developers 사이트에서 API 상태 확인
   - API 키 유효성 확인

2. **네트워크 확인**:
```bash
# KIS API 서버 연결 테스트
curl -I https://openapi.koreainvestment.com:9443

# 방화벽 확인
sudo ufw status
```

3. **재연결 로그 확인**:
```bash
grep "Reconnection" /var/log/trading-system/application.log
```

### 문제 4: 메모리 부족 (OutOfMemoryError)

**증상**:
```
java.lang.OutOfMemoryError: Java heap space
```

**해결 방법**:

1. **힙 메모리 증가**:

`/etc/systemd/system/trading-system.service` 수정:
```ini
Environment="JAVA_OPTS=-Xms4g -Xmx8g -XX:+UseG1GC"
```

2. **재시작**:
```bash
sudo systemctl daemon-reload
sudo systemctl restart trading-system
```

3. **메모리 모니터링**:
```bash
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

### 문제 5: 전략이 실행되지 않음

**확인 사항**:

1. **StrategyScheduler 활성화 확인**:
```yaml
trading:
  scheduler:
    strategy-execution:
      enabled: true
```

2. **StrategySymbol 매핑 확인**:
```sql
SELECT s.name, ss.symbol, ss.is_active
FROM strategies s
JOIN strategy_symbols ss ON s.strategy_id = ss.strategy_id
WHERE s.is_active = TRUE;
```

3. **바 데이터 확인** (전략 실행에 필요):
```sql
SELECT symbol, timeframe, COUNT(*)
FROM market_bars
GROUP BY symbol, timeframe;
```

4. **로그 확인**:
```bash
grep "StrategyScheduler" /var/log/trading-system/application.log | tail -20
```

---

## 체크리스트

### 배포 전 체크리스트

- [ ] Java 17 설치 및 환경 변수 설정
- [ ] MariaDB 설치 및 보안 설정
- [ ] 데이터베이스 생성 (`trading_mvp`)
- [ ] 사용자 생성 (`trading_user`) 및 권한 부여
- [ ] Flyway 마이그레이션 실행 및 확인
- [ ] KIS API 키 발급 (PAPER)
- [ ] 환경 변수 파일 작성 (`env.conf`)
- [ ] 파일 권한 설정 (600)
- [ ] 애플리케이션 빌드 및 JAR 파일 생성
- [ ] systemd 서비스 파일 작성
- [ ] 로그 디렉토리 생성 및 권한 설정
- [ ] 방화벽 설정
- [ ] 백업 스크립트 작성 및 크론 설정
- [ ] Health Check 엔드포인트 테스트
- [ ] Kill Switch 테스트
- [ ] 테스트 주문 실행 (PAPER 모드)

### 배포 후 체크리스트

- [ ] 서비스 정상 시작 확인
- [ ] Health Check 응답 확인 (`/actuator/health`)
- [ ] 로그 확인 (에러 없는지)
- [ ] DB 연결 확인
- [ ] WebSocket 연결 확인 (KIS)
- [ ] 전략 실행 확인 (StrategyScheduler 로그)
- [ ] Outbox Publisher 동작 확인
- [ ] 메트릭 수집 확인 (`/actuator/metrics`)
- [ ] 알림 시스템 테스트
- [ ] Kill Switch 동작 테스트
- [ ] 백업 자동 실행 확인 (다음날)
- [ ] 모니터링 대시보드 설정 (Prometheus/Grafana)

---

## 참고 자료

### 공식 문서

- **Spring Boot**: https://spring.io/projects/spring-boot
- **MariaDB**: https://mariadb.com/kb/en/
- **Flyway**: https://flywaydb.org/documentation/
- **KIS OpenAPI**: https://apiportal.koreainvestment.com

### 프로젝트 문서

- `README.md` - 프로젝트 개요
- `PHASE3_COMPLETE.md` - Phase 3 완료 보고서
- `PHASE4_COMPLETE.md` - Phase 4 완료 보고서
- `PHASE5_COMPLETE.md` - Phase 5 완료 보고서
- `PHASE6_COMPLETE.md` - Phase 6 완료 보고서
- `BACKTEST_GUIDE.md` - 백테스팅 가이드
- `TEST_IMPLEMENTATION_STATUS.md` - 테스트 현황

---

## 지원

배포 중 문제가 발생하면 다음을 확인하세요:

1. **로그 파일**: `/var/log/trading-system/application.log`
2. **systemd 로그**: `sudo journalctl -u trading-system -f`
3. **데이터베이스 로그**: `/var/log/mysql/error.log`
4. **테스트 현황**: `TEST_IMPLEMENTATION_STATUS.md`

---

**문서 버전**: 1.0.0
**작성일**: 2026-01-01
**작성자**: Trading System Team
**상태**: ✅ Production Ready
