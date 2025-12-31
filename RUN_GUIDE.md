# 애플리케이션 실행 가이드

## 방법 3: 환경 변수 사용 (권장)

### 1. 사전 준비

#### DB 사용자 생성 (한 번만 실행)
```bash
# MariaDB root 계정으로 접속
sudo mysql

# 또는
mysql -u root -p
```

MariaDB 콘솔에서 다음 명령어 실행:
```sql
CREATE USER IF NOT EXISTS 'trading_user'@'localhost' IDENTIFIED BY 'trading_pass';
GRANT ALL PRIVILEGES ON trading_mvp.* TO 'trading_user'@'localhost';
FLUSH PRIVILEGES;
quit;
```

또는 제공된 스크립트 사용:
```bash
mysql -u root -p < setup-db-user.sql
```

#### DB 확인
```bash
# trading_user로 접속 테스트
mysql -u trading_user -ptrading_pass -e "SELECT 1"

# trading_mvp DB 확인
mysql -u trading_user -ptrading_pass -e "SHOW DATABASES LIKE 'trading%'"
```

---

### 2. 환경 변수로 실행

#### 방법 A: 제공된 스크립트 사용 (가장 간단)
```bash
./run-with-env.sh
```

#### 방법 B: 수동으로 환경 변수 설정
```bash
# Java 17 설정
export JAVA_HOME=/Users/changsupark/Library/Java/JavaVirtualMachines/corretto-17.0.5/Contents/Home

# DB 연결 정보
export SPRING_DATASOURCE_URL="jdbc:mariadb://localhost:3306/trading_mvp?useUnicode=true&characterEncoding=utf8mb4"
export SPRING_DATASOURCE_USERNAME="trading_user"
export SPRING_DATASOURCE_PASSWORD="trading_pass"

# 애플리케이션 실행
mvn spring-boot:run
```

#### 방법 C: 한 줄 명령어
```bash
export JAVA_HOME=/Users/changsupark/Library/Java/JavaVirtualMachines/corretto-17.0.5/Contents/Home && \
export SPRING_DATASOURCE_USERNAME=trading_user && \
export SPRING_DATASOURCE_PASSWORD=trading_pass && \
mvn spring-boot:run
```

---

### 3. 애플리케이션 확인

#### Health Check
```bash
# 커스텀 Health 엔드포인트
curl http://localhost:8099/health

# 예상 결과:
{
  "components": {
    "kisWs": "UP",
    "kisRest": "UP",
    "db": "UP",
    "token": "VALID"
  },
  "status": "UP",
  "timestamp": "2025-12-29T13:31:47.819421"
}

# Spring Actuator Health
curl http://localhost:8099/actuator/health

# 예상 결과:
{"status":"UP"}
```

#### 계좌 등록 테스트
```bash
curl -X POST http://localhost:8099/api/v1/admin/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "broker": "KIS",
    "environment": "PAPER",
    "cano": "50068923",
    "acntPrdtCd": "01",
    "alias": "test-account"
  }'

# 예상 결과:
{
  "accountId": "01KDM61M1YDZJ4XW1E95YYM2SC",
  "broker": "KIS",
  "environment": "PAPER",
  "cano": "50068923",
  "acntPrdtCd": "01",
  "status": "ACTIVE",
  "alias": "test-account",
  "createdAt": "2025-12-29T13:32:13.899832",
  "updatedAt": "2025-12-29T13:32:13.899844"
}
```

#### 계좌 목록 조회
```bash
curl http://localhost:8099/api/v1/admin/accounts

# 예상 결과:
{
  "items": [
    {
      "accountId": "01KDM61M1YDZJ4XW1E95YYM2SC",
      "broker": "KIS",
      "environment": "PAPER",
      "cano": "50068923",
      "acntPrdtCd": "01",
      "status": "ACTIVE",
      "alias": "test-account",
      "createdAt": "2025-12-29T13:32:13.899",
      "updatedAt": "2025-12-29T13:32:13.899"
    }
  ]
}
```

#### Kill Switch 상태 확인
```bash
curl http://localhost:8099/api/v1/admin/kill-switch

# 예상 결과:
{
  "status": "OFF",
  "updatedAt": "2025-12-29T13:32:40.664211"
}
```

#### 주문 조회
```bash
curl "http://localhost:8099/api/v1/query/orders?accountId=01KDM61M1YDZJ4XW1E95YYM2SC"

# 예상 결과:
{"items":[]}
```

---

### 4. 애플리케이션 중지

터미널에서 `Ctrl + C` 또는:

```bash
# 실행 중인 Java 프로세스 찾기
lsof -i :8099

# PID로 종료
kill <PID>
```

---

## 환경 변수 방식의 장점

1. **보안**: DB 비밀번호가 코드에 하드코딩되지 않음
2. **유연성**: 환경마다 다른 설정 사용 가능
3. **Git 안전**: 민감한 정보가 Git에 커밋되지 않음
4. **표준**: Spring Boot의 표준 설정 오버라이드 방식

---

## 문제 해결

### MariaDB 연결 오류
```bash
# MariaDB 실행 확인
brew services list | grep mariadb

# MariaDB 시작
brew services start mariadb

# DB 존재 확인
mysql -u trading_user -ptrading_pass -e "SHOW DATABASES"
```

### Java 버전 오류
```bash
# 현재 Java 버전 확인
java -version

# 설치된 Java 버전 목록
/usr/libexec/java_home -V

# Java 17 설정
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

### 포트 충돌 (8099)
```bash
# 포트 사용 중인 프로세스 확인
lsof -i :8099

# 프로세스 종료
kill <PID>

# 또는 application.yml에서 포트 변경
server:
  port: 8100  # 8099 → 8100 (다른 사용 가능한 포트)
```

---

## 테스트 시나리오

전체 E2E 테스트는 `TEST_SCENARIOS.md` 참조

---

## 참고 문서

- [README.md](README.md) - 프로젝트 개요
- [TEST_SCENARIOS.md](TEST_SCENARIOS.md) - 테스트 시나리오
- [md/docs/04_API_OPENAPI.md](md/docs/04_API_OPENAPI.md) - API 명세
