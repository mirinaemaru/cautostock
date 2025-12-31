# Maven 사용 가이드

프로젝트가 Gradle에서 Maven으로 변경되었습니다.

## 📋 변경 사항

- ✅ `pom.xml` 생성 (기존 `build.gradle`의 모든 의존성 포함)
- ✅ `.gitignore` 업데이트 (Maven target 디렉토리 추가)
- ✅ 빌드 테스트 완료

## 🚀 Maven 명령어

### 빌드

```bash
# Java 17 설정
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# 클린 빌드 (테스트 제외)
mvn clean package -DskipTests

# 테스트 포함 빌드
mvn clean package

# 컴파일만
mvn compile
```

### 실행

```bash
# 방법 1: Maven으로 직접 실행
mvn spring-boot:run

# 방법 2: 프로파일 지정하여 실행
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 방법 3: JAR 파일 실행
java -jar target/trading-system-0.1.0-SNAPSHOT.jar

# 방법 4: 프로파일 지정하여 JAR 실행
java -jar -Dspring.profiles.active=local target/trading-system-0.1.0-SNAPSHOT.jar
```

### 의존성 관리

```bash
# 의존성 트리 확인
mvn dependency:tree

# 의존성 업데이트 확인
mvn versions:display-dependency-updates

# 의존성 다운로드
mvn dependency:resolve
```

### 테스트

```bash
# 모든 테스트 실행
mvn test

# 특정 테스트 클래스 실행
mvn test -Dtest=AccountAdminControllerTest

# 테스트 건너뛰기
mvn package -DskipTests
```

### 정리

```bash
# 빌드 결과물 삭제
mvn clean

# 모든 캐시 포함 정리
mvn clean -Dmaven.clean.failOnError=false
```

## 📂 디렉토리 구조

Maven 빌드 후 생성되는 디렉토리:

```
trading-system/
├── pom.xml                    # Maven 설정 파일
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
│       ├── java/
│       └── resources/
└── target/                    # 빌드 결과물 (Git 무시)
    ├── classes/               # 컴파일된 클래스
    ├── test-classes/          # 테스트 클래스
    ├── trading-system-0.1.0-SNAPSHOT.jar
    └── ...
```

## 🔧 IDE 설정

### IntelliJ IDEA

1. **프로젝트 열기**
   - File → Open → `pom.xml` 선택
   - "Open as Project" 선택

2. **Maven 자동 임포트 활성화**
   - Settings → Build, Execution, Deployment → Build Tools → Maven
   - ✅ "Reload project after changes in the build scripts" 체크

3. **Java SDK 설정**
   - File → Project Structure → Project
   - SDK: Java 17 선택

### VS Code

1. **Extension 설치**
   - Extension Pack for Java
   - Maven for Java

2. **프로젝트 열기**
   - 프로젝트 폴더 열기
   - Maven이 자동으로 `pom.xml` 인식

## 📝 pom.xml 주요 내용

### 프로젝트 정보

```xml
<groupId>maru.trading</groupId>
<artifactId>trading-system</artifactId>
<version>0.1.0-SNAPSHOT</version>
```

### Spring Boot 버전

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.1</version>
</parent>
```

### Java 버전

```xml
<properties>
    <java.version>17</java.version>
</properties>
```

### 주요 의존성

- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Validation
- Spring Boot Starter Actuator
- Spring Boot Starter WebSocket
- Spring Boot Starter WebFlux
- MariaDB JDBC Driver
- Flyway (Core + MySQL)
- Lombok
- Jackson
- ULID Creator

## 🆚 Gradle vs Maven 명령어 비교

| 작업 | Gradle | Maven |
|------|--------|-------|
| 빌드 | `./gradlew build` | `mvn package` |
| 클린 빌드 | `./gradlew clean build` | `mvn clean package` |
| 실행 | `./gradlew bootRun` | `mvn spring-boot:run` |
| 테스트 | `./gradlew test` | `mvn test` |
| 테스트 스킵 | `./gradlew build -x test` | `mvn package -DskipTests` |
| 의존성 확인 | `./gradlew dependencies` | `mvn dependency:tree` |

## ⚙️ 환경별 실행

### 개발 환경 (local)

```bash
# application-local.yml 사용
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 운영 환경 (prod)

```bash
# application-prod.yml 사용
java -jar -Dspring.profiles.active=prod target/trading-system-0.1.0-SNAPSHOT.jar
```

## 🐛 문제 해결

### Maven이 설치되지 않은 경우

```bash
# macOS - Homebrew로 설치
brew install maven

# 설치 확인
mvn -version
```

### Java 17이 감지되지 않는 경우

```bash
# JAVA_HOME 설정
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# 확인
echo $JAVA_HOME
java -version
```

### 의존성 다운로드 실패

```bash
# Maven 로컬 저장소 정리
rm -rf ~/.m2/repository

# 의존성 재다운로드
mvn clean install
```

### 빌드 캐시 문제

```bash
# 완전히 정리 후 재빌드
mvn clean install -U
# -U: 스냅샷 의존성 강제 업데이트
```

## 📦 배포

### JAR 파일 생성

```bash
# 실행 가능한 JAR 생성
mvn clean package -DskipTests

# 생성 위치
# target/trading-system-0.1.0-SNAPSHOT.jar
```

### JAR 파일 실행

```bash
java -jar target/trading-system-0.1.0-SNAPSHOT.jar
```

## 🔄 Gradle과의 호환성

- ✅ 기존 Gradle 파일(`build.gradle`, `settings.gradle`)은 유지됨
- ✅ 두 빌드 도구를 모두 사용 가능
- ⚠️ 한 프로젝트에서는 하나만 사용하는 것을 권장

Gradle로 돌아가려면:

```bash
./gradlew clean build
./gradlew bootRun
```

## ✅ 빠른 시작

```bash
# 1. 의존성 다운로드 및 빌드
mvn clean package -DskipTests

# 2. 애플리케이션 실행
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 3. 확인
curl http://localhost:8080/health
```

모든 기능이 동일하게 작동합니다! 🎉
