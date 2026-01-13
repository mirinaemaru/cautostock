# Trading System - 배포 가이드

## 목차

1. [배포 환경 준비](#배포-환경-준비)
2. [로컬 배포](#로컬-배포)
3. [프로덕션 배포](#프로덕션-배포)
4. [CI/CD 파이프라인](#cicd-파이프라인)
5. [모니터링 설정](#모니터링-설정)
6. [트러블슈팅](#트러블슈팅)

---

## 배포 환경 준비

### 시스템 요구사항

- **OS**: Ubuntu 20.04 LTS 이상 / macOS 12 이상
- **Java**: OpenJDK 17 (Amazon Corretto 17 권장)
- **Database**: MariaDB 10.11 이상
- **Memory**: 최소 4GB RAM (권장 8GB)
- **Disk**: 최소 10GB 여유 공간

### 필수 소프트웨어 설치

#### Ubuntu/Debian

```bash
# Java 17 설치
sudo apt update
sudo apt install -y openjdk-17-jdk

# MariaDB 설치
sudo apt install -y mariadb-server

# Maven 설치 (빌드용)
sudo apt install -y maven

# Docker 설치 (모니터링용)
sudo apt install -y docker.io docker-compose
```

#### macOS

```bash
# Homebrew를 통한 설치
brew install openjdk@17
brew install mariadb
brew install maven
brew install docker docker-compose
```

### 데이터베이스 설정

```bash
# MariaDB 시작
sudo systemctl start mariadb  # Ubuntu
brew services start mariadb   # macOS

# 데이터베이스 및 사용자 생성
mysql -u root -p << EOF
CREATE DATABASE IF NOT EXISTS trading_mvp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'trading_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON trading_mvp.* TO 'trading_user'@'localhost';
FLUSH PRIVILEGES;
EOF
```

---

## 로컬 배포

### 1. 환경 변수 설정

```bash
# .env.local 파일 생성
cp .env.example .env.local

# 편집기로 환경 변수 설정
vi .env.local
```

**필수 환경 변수**:
```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
DB_PASSWORD=your_secure_password
KIS_PAPER_APP_KEY=your_paper_app_key
KIS_PAPER_APP_SECRET=your_paper_app_secret
SPRING_PROFILE=local
```

### 2. 빌드 및 실행

#### 방법 A: 배포 스크립트 사용 (권장)

```bash
# 전체 자동 배포
./scripts/deploy.sh local

# 또는 수동 단계별 실행
./scripts/start.sh local
```

#### 방법 B: Maven 직접 사용

```bash
# 빌드
mvn clean package -DskipTests

# 실행
java -jar target/trading-system-1.0.0.jar \
  --spring.profiles.active=local \
  --spring.datasource.url=jdbc:mariadb://localhost:3306/trading_mvp \
  --spring.datasource.username=trading_user \
  --spring.datasource.password=your_secure_password
```

### 3. 애플리케이션 확인

```bash
# 헬스체크
./scripts/health-check.sh

# 또는 curl 직접 사용
curl http://localhost:8080/actuator/health

# 상태 확인
./scripts/status.sh
```

---

## 프로덕션 배포

### 1. 서버 준비

```bash
# 배포 디렉토리 생성
export DEPLOY_DIR=/opt/trading-system
sudo mkdir -p $DEPLOY_DIR
sudo chown $USER:$USER $DEPLOY_DIR

# 로그 디렉토리 생성
mkdir -p $DEPLOY_DIR/logs
```

### 2. 환경 변수 설정

```bash
# .env.prod 파일 생성
cp .env.example $DEPLOY_DIR/.env.prod

# 프로덕션 환경 변수 설정
vi $DEPLOY_DIR/.env.prod
```

**프로덕션 환경 변수**:
```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
JVM_OPTS="-Xms4g -Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
DB_PASSWORD=strong_production_password
KIS_PAPER_APP_KEY=production_paper_app_key
KIS_PAPER_APP_SECRET=production_paper_app_secret
SPRING_PROFILE=prod
DEPLOY_DIR=/opt/trading-system
RUN_TESTS=false
AUTO_RESTART=true
```

### 3. 프로덕션 배포 실행

```bash
# 배포 스크립트 실행
./scripts/deploy.sh prod

# 배포 후 상태 확인
./scripts/status.sh

# 헬스체크
./scripts/health-check.sh
```

### 4. 모니터링 시작

```bash
# 모니터링 스택 시작 (Prometheus + Grafana)
docker-compose -f monitoring/docker-compose.yml up -d

# 모니터링 대시보드 접속
# Grafana: http://<server-ip>:3000
# Prometheus: http://<server-ip>:9090
```

### 5. 지속적 모니터링

```bash
# 연속 모니터링 모드 (60초 간격)
./scripts/monitor.sh --continuous --interval 60

# 또는 cron 작업으로 등록
crontab -e

# 다음 라인 추가 (매 5분마다 모니터링)
*/5 * * * * cd /opt/trading-system && ./scripts/monitor.sh >> logs/monitoring.log 2>&1
```

---

## CI/CD 파이프라인

### GitHub Actions 워크플로우

이 프로젝트는 3개의 GitHub Actions 워크플로우를 제공합니다:

#### 1. CI - Build and Test (`.github/workflows/ci.yml`)

**트리거**:
- `master`, `develop`, `feature/**` 브랜치로 push
- `master`, `develop` 브랜치로 pull request

**작업**:
1. MariaDB 테스트 데이터베이스 시작
2. JDK 17 설정
3. Maven 빌드 및 컴파일
4. 단위 테스트 실행
5. JAR 파일 패키징
6. 테스트 결과 및 커버리지 리포트 업로드
7. 코드 품질 검사 (Checkstyle, SpotBugs)

#### 2. CD - Deploy to Production (`.github/workflows/cd.yml`)

**트리거**:
- `v*.*.*` 형식의 태그 push (예: v1.0.0)
- 수동 트리거 (workflow_dispatch)

**작업**:
1. 프로덕션 빌드
2. 배포 패키지 생성 (JAR + 스크립트 + 환경 변수 예제)
3. SSH를 통한 원격 서버 배포
4. 헬스체크 수행
5. GitHub Release 생성
6. 배포 알림

**필수 Secrets 설정**:
```
DEPLOY_HOST: 배포 서버 호스트명/IP
DEPLOY_USER: SSH 사용자명
DEPLOY_SSH_KEY: SSH 개인 키
DEPLOY_DIR: 배포 디렉토리 경로
HEALTH_CHECK_URL: 헬스체크 URL
```

#### 3. Monitoring - Scheduled Health Checks (`.github/workflows/monitoring.yml`)

**트리거**:
- 15분마다 자동 실행 (cron)
- 수동 트리거

**작업**:
1. 프로덕션 헬스체크
2. Kill Switch 상태 확인
3. Prometheus 메트릭 쿼리
4. 실패 시 알림

**필수 Secrets 설정**:
```
PRODUCTION_HEALTH_URL: 프로덕션 헬스체크 URL
PRODUCTION_KILL_SWITCH_URL: Kill Switch API URL
PROMETHEUS_URL: Prometheus 서버 URL
```

### GitHub Secrets 설정 방법

1. GitHub 저장소 → Settings → Secrets and variables → Actions
2. "New repository secret" 클릭
3. 위의 필수 Secrets를 모두 추가

### 배포 워크플로우 실행

#### 자동 배포 (태그 기반)

```bash
# 새 버전 태그 생성
git tag v1.0.0
git push origin v1.0.0

# GitHub Actions가 자동으로 배포 실행
```

#### 수동 배포

1. GitHub 저장소 → Actions
2. "CD - Deploy to Production" 워크플로우 선택
3. "Run workflow" 클릭
4. 환경 선택 (staging/production)
5. "Run workflow" 버튼 클릭

---

## 모니터링 설정

### Prometheus + Grafana 스택 배포

상세한 모니터링 설정은 [`monitoring/README.md`](monitoring/README.md)를 참조하세요.

**빠른 시작**:

```bash
# 모니터링 스택 시작
docker-compose -f monitoring/docker-compose.yml up -d

# Grafana 접속
open http://localhost:3000  # 기본 계정: admin/admin

# Prometheus 접속
open http://localhost:9090
```

### 제공되는 대시보드

Trading System Dashboard는 다음 메트릭을 제공합니다:
- JVM 메모리 사용량 및 GC 시간
- HTTP 요청 처리율 및 레이턴시
- 데이터베이스 커넥션 풀 상태
- Kill Switch 상태
- 주문 건수 및 상태별 분포
- 포지션 및 손익 (PnL)

---

## 트러블슈팅

### 애플리케이션이 시작되지 않음

**증상**: `./scripts/start.sh` 실행 시 오류

**해결 방법**:
```bash
# 로그 확인
tail -f $DEPLOY_DIR/logs/application.log

# 일반적인 원인:
# 1. Java 버전 불일치
java -version  # Java 17인지 확인

# 2. 데이터베이스 연결 실패
mysql -u trading_user -p trading_mvp  # DB 접속 테스트

# 3. 포트 충돌
lsof -i :8080  # 8080 포트 사용 프로세스 확인
```

### 배포 스크립트 실패

**증상**: `./scripts/deploy.sh` 실행 시 환경 변수 오류

**해결 방법**:
```bash
# 환경 변수 파일 확인
cat .env.local  # 또는 .env.prod

# 필수 변수 설정 확인
grep -E "DB_PASSWORD|KIS_PAPER_APP_KEY" .env.local

# 누락된 변수가 있다면 .env.example을 참고하여 추가
```

### Kill Switch가 자동으로 활성화됨

**증상**: 거래가 차단되고 Kill Switch가 활성화됨

**해결 방법**:
```bash
# Kill Switch 상태 확인
curl http://localhost:8080/api/v1/admin/kill-switch

# Kill Switch 비활성화 (관리자 권한 필요)
curl -X POST http://localhost:8080/api/v1/admin/kill-switch/disable

# 로그에서 원인 확인
grep "Kill Switch" $DEPLOY_DIR/logs/application.log

# 일반적인 원인:
# - 일일 손실 한도 초과
# - 포지션 노출 한도 초과
# - 주문 빈도 제한 초과
```

### 모니터링 대시보드에 데이터가 표시되지 않음

**증상**: Grafana 대시보드가 비어 있음

**해결 방법**:
```bash
# Prometheus가 메트릭을 수집하는지 확인
curl http://localhost:9090/api/v1/targets

# Trading System의 Prometheus 엔드포인트 확인
curl http://localhost:8080/actuator/prometheus

# Prometheus 로그 확인
docker-compose -f monitoring/docker-compose.yml logs prometheus

# Grafana 데이터소스 연결 확인
# Grafana → Configuration → Data sources → Prometheus → Test
```

### 데이터베이스 마이그레이션 실패

**증상**: Flyway 마이그레이션 오류

**해결 방법**:
```bash
# Flyway 마이그레이션 상태 확인
mvn flyway:info

# 실패한 마이그레이션 복구
mvn flyway:repair

# 마이그레이션 재실행
mvn flyway:migrate
```

---

## 롤백 절차

### 애플리케이션 롤백

```bash
# 이전 버전 JAR 파일로 복원
cp $DEPLOY_DIR/trading-system.jar.backup $DEPLOY_DIR/trading-system.jar

# 애플리케이션 재시작
./scripts/restart.sh prod

# 헬스체크
./scripts/health-check.sh
```

### 데이터베이스 롤백

```bash
# Flyway를 사용한 롤백 (특정 버전으로)
mvn flyway:undo -Dflyway.target=V5

# 수동 롤백 (백업에서 복원)
mysql -u trading_user -p trading_mvp < backup/trading_mvp_backup_20260101.sql
```

---

## 보안 체크리스트

배포 전 다음 항목을 확인하세요:

- [ ] `.env.local` 및 `.env.prod` 파일이 `.gitignore`에 포함되어 있음
- [ ] 데이터베이스 비밀번호가 강력함 (최소 16자, 특수문자 포함)
- [ ] KIS API 키가 PAPER 모드임 (LIVE 키 절대 사용 금지)
- [ ] SSH 키가 비밀번호로 보호되어 있음
- [ ] Grafana 기본 비밀번호가 변경됨
- [ ] 방화벽 규칙이 설정됨 (8080, 3000, 9090 포트)
- [ ] HTTPS 사용 (프로덕션 환경)
- [ ] 정기적인 백업 cron 작업 설정

---

## 참고 자료

- [Spring Boot 프로덕션 준비](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html)
- [Flyway 마이그레이션](https://flywaydb.org/documentation/)
- [GitHub Actions 문서](https://docs.github.com/en/actions)
- [Prometheus 베스트 프랙티스](https://prometheus.io/docs/practices/naming/)
- [Grafana 프로비저닝](https://grafana.com/docs/grafana/latest/administration/provisioning/)

---

**문의**: 배포 중 문제가 발생하면 이슈를 생성해주세요.
