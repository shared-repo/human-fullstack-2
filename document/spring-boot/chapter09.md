# 9장. 빌드·배포 및 운영

---

## 학습 목표

- `bootJar`로 실행 가능한 JAR를 패키징하고 서버에서 실행할 수 있다.
- `dev` / `prod` 프로파일을 분리하여 환경별 설정을 관리할 수 있다.
- 비밀번호·API 키 등 민감 정보를 환경변수로 외부화할 수 있다.
- Dockerfile을 작성하고 애플리케이션을 컨테이너로 실행할 수 있다.
- Docker Compose로 애플리케이션과 MariaDB를 함께 구성할 수 있다.

---

## 9.1 Spring Boot 빌드 결과물 이해

### bootJar vs jar

Spring Boot Gradle 플러그인은 두 종류의 JAR를 만들 수 있습니다.

| 태스크 | 결과물 | 설명 |
|---|---|---|
| `./gradlew jar` | 일반 JAR | 애플리케이션 클래스만 포함. 의존성 없어 단독 실행 불가 |
| `./gradlew bootJar` | 실행 가능한 FAT JAR | 의존성·내장 Tomcat 전부 포함. 단독 실행 가능 |
| `./gradlew build` | 테스트 + bootJar | 테스트 통과 후 FAT JAR 생성 |

```
FAT JAR 내부 구조
imageboard-0.0.1-SNAPSHOT.jar
├── BOOT-INF/
│   ├── classes/          ← 우리가 작성한 클래스
│   └── lib/              ← 모든 의존성 JAR (spring-web, hibernate 등)
├── META-INF/
│   └── MANIFEST.MF       ← 진입점(Main-Class) 정보
└── org/springframework/boot/loader/  ← Spring Boot 클래스로더
```

### bootJar 실행

```bash
# 1. 빌드 (테스트 포함)
./gradlew build

# 테스트를 건너뛰고 빌드
./gradlew build -x test

# 2. 생성된 JAR 확인
ls -lh build/libs/
# imageboard-0.0.1-SNAPSHOT.jar  (약 60~100MB)

# 3. 실행
java -jar build/libs/imageboard-0.0.1-SNAPSHOT.jar
```

### build.gradle — JAR 파일명 고정

기본 파일명에 버전이 붙으므로, 배포 스크립트 작성이 편리하도록 고정할 수 있습니다.

```groovy
// build.gradle
bootJar {
    archiveFileName = 'imageboard.jar'   // 버전 없이 고정 파일명
}
```

```bash
# 이후 항상 같은 파일명으로 실행 가능
java -jar build/libs/imageboard.jar
```

---

## 9.2 프로파일 분리 전략

### 파일 구성

```
src/main/resources/
├── application.yml           ← 공통 설정 + 기본 프로파일 지정
├── application-dev.yml       ← 개발 환경
└── application-prod.yml      ← 운영 환경
```

### application.yml — 공통 설정

```yaml
# application.yml
spring:
  application:
    name: imageboard
  profiles:
    active: dev               # 기본값: 개발 환경

  # 공통 JPA 설정
  jpa:
    properties:
      hibernate:
        format_sql: true

  # 공통 메시지 설정
  messages:
    basename: messages
    encoding: UTF-8

  # 공통 파일 업로드
  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB
      max-request-size: 30MB

  # HTTP Method Override (PUT, DELETE 폼 지원)
  mvc:
    hiddenmethod:
      filter:
        enabled: true

server:
  port: 8080
```

### application-dev.yml — 개발 환경

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/imageboard?characterEncoding=UTF-8&serverTimezone=Asia/Seoul
    username: boarduser
    password: board1234
    driver-class-name: org.mariadb.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update          # 스키마 자동 변경
    show-sql: true              # SQL 콘솔 출력

  thymeleaf:
    cache: false                # 템플릿 캐시 비활성화

file:
  upload-dir: ${user.home}/imageboard/uploads
  thumbnail-dir: ${user.home}/imageboard/thumbnails
  allowed-extensions: [jpg, jpeg, png, gif, webp]

logging:
  level:
    com.example.imageboard: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql: TRACE
```

### application-prod.yml — 운영 환경

```yaml
# application-prod.yml
spring:
  datasource:
    url: ${DB_URL}              # 환경변수에서 주입
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.mariadb.jdbc.Driver
    hikari:                     # 커넥션 풀 튜닝
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000

  jpa:
    hibernate:
      ddl-auto: validate        # 운영: 스키마 변경 금지, 불일치 시 시작 실패
    show-sql: false             # 운영: SQL 출력 비활성화

  thymeleaf:
    cache: true                 # 운영: 템플릿 캐시 활성화

file:
  upload-dir: ${FILE_UPLOAD_DIR}
  thumbnail-dir: ${FILE_THUMBNAIL_DIR}
  allowed-extensions: [jpg, jpeg, png, gif, webp]

server:
  port: 8080
  tomcat:
    max-threads: 200            # 최대 스레드 수

logging:
  level:
    com.example.imageboard: INFO
    org.hibernate: WARN
  file:
    name: /var/log/imageboard/app.log     # 파일 로그
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30           # 30일치 보관
```

### 프로파일 활성화 방법

```bash
# 방법 1: JVM 옵션 (가장 일반적)
java -jar imageboard.jar -Dspring.profiles.active=prod

# 방법 2: 커맨드라인 인수
java -jar imageboard.jar --spring.profiles.active=prod

# 방법 3: 환경변수
export SPRING_PROFILES_ACTIVE=prod
java -jar imageboard.jar
```

> **운영 서버에서는 `prod` 프로파일을 반드시 명시하세요.** 기본값인 `dev`로 시작하면 `ddl-auto: update`로 인해 예상치 못한 스키마 변경이 발생할 수 있습니다.

---

## 9.3 민감 정보 외부화

`application.yml`에 DB 비밀번호·API 키 등을 직접 작성하면 Git에 노출될 위험이 있습니다. 민감 정보는 반드시 외부에서 주입합니다.

### 환경변수 참조

```yaml
# application-prod.yml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

file:
  upload-dir: ${FILE_UPLOAD_DIR:/home/ubuntu/imageboard/uploads}  # 기본값 설정
  thumbnail-dir: ${FILE_THUMBNAIL_DIR:/home/ubuntu/imageboard/thumbnails}
```

`${변수명:기본값}` 형식으로 환경변수가 없을 때의 기본값을 지정할 수 있습니다.

### 환경변수 설정 방법

**Linux 서버 — `/etc/environment` 또는 systemd 서비스**

```bash
# /etc/environment 에 추가
DB_URL=jdbc:mariadb://localhost:3306/imageboard?characterEncoding=UTF-8&serverTimezone=Asia/Seoul
DB_USERNAME=boarduser
DB_PASSWORD=실제비밀번호
FILE_UPLOAD_DIR=/home/ubuntu/imageboard/uploads
FILE_THUMBNAIL_DIR=/home/ubuntu/imageboard/thumbnails
```

**systemd 서비스 파일 — 애플리케이션을 서비스로 등록**

```ini
# /etc/systemd/system/imageboard.service
[Unit]
Description=Imageboard Spring Boot Application
After=network.target mariadb.service

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/app

# 환경변수 직접 주입
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="DB_URL=jdbc:mariadb://localhost:3306/imageboard?characterEncoding=UTF-8&serverTimezone=Asia/Seoul"
Environment="DB_USERNAME=boarduser"
Environment="DB_PASSWORD=실제비밀번호"
Environment="FILE_UPLOAD_DIR=/home/ubuntu/imageboard/uploads"
Environment="FILE_THUMBNAIL_DIR=/home/ubuntu/imageboard/thumbnails"

ExecStart=/usr/bin/java -jar /home/ubuntu/app/imageboard.jar
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
# systemd 서비스 등록 및 시작
sudo systemctl daemon-reload
sudo systemctl enable imageboard     # 서버 재시작 시 자동 시작
sudo systemctl start imageboard      # 서비스 시작

# 상태 확인
sudo systemctl status imageboard

# 로그 확인
sudo journalctl -u imageboard -f
```

### .env 파일 + .gitignore

로컬 개발 환경에서는 `.env` 파일에 환경변수를 모아 관리하고, Git에는 올리지 않습니다.

```bash
# .env (프로젝트 루트)
DB_URL=jdbc:mariadb://localhost:3306/imageboard
DB_USERNAME=boarduser
DB_PASSWORD=board1234
FILE_UPLOAD_DIR=/Users/username/imageboard/uploads
FILE_THUMBNAIL_DIR=/Users/username/imageboard/thumbnails
```

```
# .gitignore 에 반드시 추가
.env
*.env
application-prod.yml    # 운영 설정 파일도 Git 제외
```

---

## 9.4 배포 전 체크리스트

애플리케이션을 운영 서버에 배포하기 전에 반드시 확인해야 할 항목입니다.

```
□ 프로파일 설정
  □ spring.profiles.active=prod 명시
  □ ddl-auto=validate 확인 (update ❌)
  □ show-sql=false 확인
  □ thymeleaf.cache=true 확인

□ 보안
  □ DB 비밀번호 환경변수 처리 (yml 하드코딩 ❌)
  □ .env, application-prod.yml Git 미포함 확인
  □ Spring Security 활성화 확인
  □ HTTPS 적용 여부 확인

□ 데이터베이스
  □ 운영 DB 연결 정보 확인
  □ DB 백업 정책 수립
  □ 커넥션 풀 설정 (HikariCP)

□ 파일 저장
  □ 업로드 디렉터리 존재 및 쓰기 권한 확인
  □ 디스크 용량 여유 확인

□ 빌드
  □ ./gradlew build (테스트 포함) 성공 확인
  □ JAR 파일 생성 확인

□ 로깅
  □ 로그 파일 경로 및 권한 확인
  □ 로그 로테이션 정책 설정
```

---

## 9.5 Docker 컨테이너화 기초

Docker를 사용하면 애플리케이션 실행 환경(JDK, 설정 등)을 이미지로 패키징하여 어떤 서버에서도 동일하게 실행할 수 있습니다.

### Docker 핵심 개념

```
Dockerfile   → (build) →   Image   → (run) →   Container
  설계도               실행 가능한 패키지        실제 실행 중인 인스턴스
```

| 개념 | 의미 |
|---|---|
| Image | 실행 환경을 레이어로 쌓아 놓은 읽기 전용 패키지 |
| Container | Image를 실행한 인스턴스. 프로세스처럼 동작 |
| Dockerfile | Image를 만드는 지시서 |
| Docker Compose | 여러 컨테이너를 함께 정의하고 실행하는 도구 |

### Dockerfile 작성

```dockerfile
# Dockerfile (프로젝트 루트)

# ── 1단계: 빌드 ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Gradle Wrapper와 빌드 설정 파일만 먼저 복사 (캐시 최적화)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성 다운로드 (소스 변경 없으면 이 레이어 캐시 재사용)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사 후 빌드
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# ── 2단계: 실행 ────────────────────────────────────────────────────────
# JRE만 포함된 경량 이미지 (JDK 불필요)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 업로드 파일 저장 디렉터리
RUN mkdir -p /app/uploads /app/thumbnails

# 1단계에서 만든 JAR만 복사
COPY --from=builder /app/build/libs/imageboard.jar imageboard.jar

# 컨테이너에서 실행할 사용자 (root 권한 최소화)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "imageboard.jar"]
```

**멀티 스테이지 빌드 (Multi-stage Build)**

Dockerfile이 두 단계(`builder`, `runtime`)로 구성된 이유:

```
builder 단계: JDK 포함 → 빌드에만 사용
runtime 단계: JRE만 포함 → 최종 이미지에만 포함

결과: 최종 이미지 크기가 대폭 감소 (약 500MB → 약 150MB)
```

### .dockerignore

불필요한 파일이 이미지에 포함되지 않도록 합니다.

```
# .dockerignore
.git
.gradle
build
out
*.md
.env
src/test
```

### Docker 이미지 빌드 및 실행

```bash
# 이미지 빌드
docker build -t imageboard:latest .

# 빌드 확인
docker images | grep imageboard

# 컨테이너 실행 (환경변수 전달)
docker run -d \
  --name imageboard-app \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL="jdbc:mariadb://host.docker.internal:3306/imageboard?characterEncoding=UTF-8&serverTimezone=Asia/Seoul" \
  -e DB_USERNAME=boarduser \
  -e DB_PASSWORD=board1234 \
  -e FILE_UPLOAD_DIR=/app/uploads \
  -e FILE_THUMBNAIL_DIR=/app/thumbnails \
  -v /home/ubuntu/uploads:/app/uploads \        # 업로드 파일 볼륨 마운트
  -v /home/ubuntu/thumbnails:/app/thumbnails \
  imageboard:latest

# 실행 중인 컨테이너 확인
docker ps

# 로그 확인
docker logs -f imageboard-app

# 컨테이너 중지 및 제거
docker stop imageboard-app
docker rm imageboard-app
```

> **볼륨 마운트 필수**: 업로드된 이미지 파일을 컨테이너 내부에만 저장하면 컨테이너를 삭제할 때 파일도 함께 사라집니다. `-v` 옵션으로 호스트 디렉터리와 마운트하면 컨테이너가 재시작되어도 파일이 유지됩니다.

---

## 9.6 Docker Compose — 애플리케이션 + MariaDB 통합 구성

Docker Compose를 사용하면 애플리케이션과 데이터베이스를 하나의 파일로 정의하고 함께 실행할 수 있습니다.

```yaml
# docker-compose.yml (프로젝트 루트)
version: '3.8'

services:

  # ── MariaDB ──────────────────────────────────────────────────────────
  db:
    image: mariadb:11
    container_name: imageboard-db
    restart: unless-stopped
    environment:
      MARIADB_ROOT_PASSWORD: ${MARIADB_ROOT_PASSWORD}
      MARIADB_DATABASE: imageboard
      MARIADB_USER: ${DB_USERNAME}
      MARIADB_PASSWORD: ${DB_PASSWORD}
    ports:
      - "3306:3306"
    volumes:
      - db-data:/var/lib/mysql       # DB 데이터 영구 보존
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql  # 초기 SQL (선택)
    healthcheck:
      test: ["CMD", "healthcheck.sh", "--connect"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ── Spring Boot 애플리케이션 ─────────────────────────────────────────
  app:
    build: .                          # 현재 디렉터리 Dockerfile 사용
    container_name: imageboard-app
    restart: unless-stopped
    depends_on:
      db:
        condition: service_healthy    # DB 헬스 체크 통과 후 시작
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:mariadb://db:3306/imageboard?characterEncoding=UTF-8&serverTimezone=Asia/Seoul
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      FILE_UPLOAD_DIR: /app/uploads
      FILE_THUMBNAIL_DIR: /app/thumbnails
    ports:
      - "8080:8080"
    volumes:
      - uploads:/app/uploads
      - thumbnails:/app/thumbnails
      - ./logs:/var/log/imageboard    # 로그 파일 호스트에 마운트

volumes:
  db-data:        # MariaDB 데이터 볼륨
  uploads:        # 업로드 이미지 볼륨
  thumbnails:     # 썸네일 이미지 볼륨
```

Docker Compose와 함께 사용할 `.env` 파일:

```bash
# .env (프로젝트 루트 — .gitignore에 추가)
MARIADB_ROOT_PASSWORD=root_secret
DB_USERNAME=boarduser
DB_PASSWORD=board1234
```

### Docker Compose 주요 명령

```bash
# 전체 서비스 시작 (백그라운드)
docker compose up -d

# 이미지 재빌드 후 시작
docker compose up -d --build

# 전체 서비스 상태 확인
docker compose ps

# 특정 서비스 로그 확인
docker compose logs -f app
docker compose logs -f db

# 특정 서비스만 재시작
docker compose restart app

# 전체 서비스 중지 (데이터 유지)
docker compose down

# 전체 서비스 중지 + 볼륨 삭제 (데이터 초기화)
docker compose down -v
```

### 서비스 간 네트워크

Docker Compose로 묶인 서비스들은 자동으로 같은 네트워크에 속합니다. **호스트 IP 대신 서비스 이름으로 접근**합니다.

```yaml
# ❌ 호스트 IP 사용 — 환경에 따라 달라짐
DB_URL: jdbc:mariadb://192.168.0.10:3306/imageboard

# ✅ 서비스 이름 사용 — Docker 내부 DNS가 자동 해석
DB_URL: jdbc:mariadb://db:3306/imageboard
```

---

## 9.7 운영 환경 로깅 설정

운영 환경에서는 콘솔 출력 대신 파일 로그를 사용하고 로테이션을 설정합니다.

### Logback 설정 파일

```xml
<!-- src/main/resources/logback-spring.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <!-- 프로파일별 설정 분기 -->
    <springProfile name="dev">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="DEBUG">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <springProfile name="prod">
        <!-- 롤링 파일 로그 -->
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>/var/log/imageboard/app.log</file>
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
                <charset>UTF-8</charset>
            </encoder>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <!-- 날짜별 파일 분리 -->
                <fileNamePattern>/var/log/imageboard/app.%d{yyyy-MM-dd}.log.gz</fileNamePattern>
                <maxHistory>30</maxHistory>     <!-- 30일치 보관 -->
                <totalSizeCap>3GB</totalSizeCap> <!-- 최대 3GB -->
            </rollingPolicy>
        </appender>

        <!-- 애플리케이션 패키지는 INFO -->
        <logger name="com.example.imageboard" level="INFO"/>
        <!-- Hibernate는 WARN (SQL 미출력) -->
        <logger name="org.hibernate" level="WARN"/>

        <root level="WARN">
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>

</configuration>
```

---

## 9.8 Actuator — 운영 모니터링

Spring Boot Actuator는 애플리케이션 상태를 HTTP 엔드포인트로 노출합니다.

### 의존성 추가

```groovy
// build.gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
}
```

### application-prod.yml — Actuator 설정

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics  # 노출할 엔드포인트 (운영: 최소한만)
  endpoint:
    health:
      show-details: when-authorized     # 인증된 사용자에게만 상세 정보 표시
  info:
    env:
      enabled: true

# 애플리케이션 정보
info:
  app:
    name: 이미지 게시판
    version: '@project.version@'        # build.gradle의 version 값 자동 삽입
```

### 주요 엔드포인트

```bash
# 애플리케이션 상태 확인 (로드밸런서 헬스 체크에 사용)
curl http://localhost:8080/actuator/health
# {"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"}}}

# 애플리케이션 정보
curl http://localhost:8080/actuator/info

# 메트릭 (JVM, HTTP 요청 통계 등)
curl http://localhost:8080/actuator/metrics
```

> Actuator 엔드포인트는 운영 서버에서 외부에 노출되지 않도록 방화벽이나 Spring Security로 접근을 제한하세요.

---

## 9.9 전체 배포 흐름 정리

### 로컬 개발 → 운영 배포 흐름

```
[개발자 로컬]
  코드 작성 / 테스트
      ↓
  git push origin main
      ↓
[CI 서버 (선택)]
  ./gradlew build         ← 테스트 + JAR 빌드
  docker build            ← 이미지 빌드
  docker push registry    ← 이미지 레지스트리 업로드
      ↓
[운영 서버]
  docker compose pull     ← 최신 이미지 다운로드
  docker compose up -d    ← 무중단 배포
```

### 단순 배포 스크립트 (JAR 직접 배포)

CI/CD 없이 빠르게 배포할 때 사용하는 간단한 셸 스크립트입니다.

```bash
#!/bin/bash
# deploy.sh

set -e   # 오류 발생 시 즉시 중단

APP_NAME="imageboard"
JAR_FILE="build/libs/imageboard.jar"
DEPLOY_DIR="/home/ubuntu/app"
LOG_DIR="/var/log/imageboard"

echo "=== 1. 빌드 시작 ==="
./gradlew build -x test

echo "=== 2. JAR 파일 복사 ==="
mkdir -p $DEPLOY_DIR
cp $JAR_FILE $DEPLOY_DIR/

echo "=== 3. 기존 프로세스 종료 ==="
PID=$(pgrep -f "$APP_NAME" || true)
if [ -n "$PID" ]; then
    kill $PID
    sleep 5
    echo "기존 프로세스 종료: PID=$PID"
fi

echo "=== 4. 애플리케이션 시작 ==="
mkdir -p $LOG_DIR
nohup java -jar $DEPLOY_DIR/imageboard.jar \
    -Dspring.profiles.active=prod \
    > $LOG_DIR/app.log 2>&1 &

echo "=== 5. 시작 확인 (30초 대기) ==="
sleep 30
curl -s http://localhost:8080/actuator/health | grep -q "UP" \
    && echo "✅ 배포 성공" \
    || echo "❌ 배포 실패 — 로그 확인: $LOG_DIR/app.log"
```

```bash
# 실행 권한 부여 후 실행
chmod +x deploy.sh
./deploy.sh
```

---

## 9.10 자주 발생하는 운영 이슈

### ddl-auto 설정 실수

```yaml
# ❌ 운영 서버에서 절대 사용 금지
jpa:
  hibernate:
    ddl-auto: create        # 시작할 때마다 테이블 초기화 → 데이터 전체 삭제
    ddl-auto: create-drop   # 종료 시 테이블 삭제 → 데이터 전체 삭제

# ✅ 운영 환경 권장 설정
jpa:
  hibernate:
    ddl-auto: validate      # 스키마 불일치 시 시작 실패 (안전)
    ddl-auto: none          # JPA 스키마 관리 비활성화 (Flyway 등 별도 관리 시)
```

### 업로드 디렉터리 권한 오류

```bash
# 증상: java.io.IOException: Permission denied
# 원인: 애플리케이션 실행 사용자가 디렉터리에 쓰기 권한 없음
# 해결:
sudo mkdir -p /home/ubuntu/imageboard/uploads
sudo mkdir -p /home/ubuntu/imageboard/thumbnails
sudo chown -R ubuntu:ubuntu /home/ubuntu/imageboard
```

### MariaDB 연결 타임존 오류

```bash
# 증상: The server time zone value 'KST' is unrecognized ...
# 해결: JDBC URL에 serverTimezone 파라미터 추가
jdbc:mariadb://localhost:3306/imageboard?characterEncoding=UTF-8&serverTimezone=Asia/Seoul
```

### 파일 업로드 크기 초과 오류

```bash
# 증상: MaxUploadSizeExceededException
# 해결: application.yml 설정 확인
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 30MB
```

---

## 정리

| 핵심 개념 | 내용 |
|---|---|
| FAT JAR | 모든 의존성과 내장 Tomcat을 포함한 단독 실행 가능한 JAR |
| `./gradlew bootJar` | 실행 가능한 JAR 빌드 |
| 프로파일 분리 | `application-dev.yml` / `application-prod.yml`로 환경별 설정 관리 |
| 환경변수 외부화 | `${ENV_NAME}` 참조로 민감 정보를 코드 밖으로 분리 |
| `ddl-auto: validate` | 운영 환경 필수 — 스키마 변경 방지 |
| Dockerfile 멀티 스테이지 빌드 | 빌드 단계와 실행 단계를 분리하여 최소 이미지 생성 |
| Docker Compose | 앱 + DB 등 여러 컨테이너를 하나의 파일로 통합 관리 |
| `depends_on` + `healthcheck` | DB가 준비된 후 앱 컨테이너 시작 보장 |
| Actuator `/health` | 로드밸런서 헬스 체크 및 운영 모니터링 |

---

## 연습 문제

1. `./gradlew bootJar`로 JAR를 빌드하고 `java -jar`로 실행해 보세요. 콘솔 로그에서 `prod` 프로파일이 아닌 `dev` 프로파일이 활성화되어 있음을 확인하고, `--spring.profiles.active=prod` 옵션으로 전환해 보세요.
2. `application-prod.yml`의 DB 비밀번호를 환경변수 `${DB_PASSWORD}`로 변경하고, 환경변수를 설정하지 않은 채 실행했을 때 어떤 오류가 발생하는지 확인해 보세요.
3. Dockerfile을 빌드하고 Docker 컨테이너로 애플리케이션을 실행해 보세요. `-v` 옵션으로 업로드 디렉터리를 마운트하지 않으면 컨테이너 재시작 후 이미지가 사라지는 현상을 직접 확인해 보세요.
4. `spring-boot-starter-actuator`를 추가하고 `http://localhost:8080/actuator/health`로 DB 연결 상태가 포함된 헬스 정보를 확인해 보세요.

---

## 과정을 마치며

이 교육과정에서 다룬 내용을 돌아봅니다.

| 장 | 주제 | 핵심 성과물 |
|---|---|---|
| 1장 | Spring Boot 개요 및 환경 설정 | 프로젝트 생성 및 첫 실행 |
| 2장 | 프로젝트 구조와 자동 설정 | Auto Configuration 이해, yml 설정 관리 |
| 3장 | 웹 계층 구현 | 게시판 목록·상세·작성 화면 |
| 4장 | JPA 데이터 연동 | MariaDB CRUD 완성 |
| 5장 | 이미지 업로드·페이징·검색 | 이미지 게시판 핵심 기능 완성 |
| 6장 | Spring Security | 로그인·회원가입·권한 제어 |
| 7장 | 예외 처리 및 검증 | 견고한 오류 처리 체계 구축 |
| 8장 | 테스트 | 단위·슬라이스·통합 테스트 작성 |
| 9장 | 빌드·배포·운영 | 실서버 배포 및 Docker 컨테이너화 |

Spring Framework에서 쌓은 기반 위에 Spring Boot가 제공하는 생산성과 운영 편의성을 더해 **이미지 게시판 하나를 처음부터 배포까지** 완성했습니다. 앞으로는 이 프로젝트를 기반으로 REST API 전환, CI/CD 파이프라인 구축, 클라우드 배포 등으로 학습을 확장해 나가시길 바랍니다.
