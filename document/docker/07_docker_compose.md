# 7. Docker Compose

> 권장 시간: 3시간  
> 목표: Docker Compose로 Spring Boot + DB + Redis 멀티 컨테이너 환경을 코드로 정의하고 관리한다

---

## 7.1 Compose 파일 구조

### Docker Compose란?

여러 컨테이너로 구성된 애플리케이션을 **하나의 YAML 파일**로 정의하고
단일 명령어로 실행·관리하는 도구.

```bash
# 기존 방식: 컨테이너마다 별도 명령어
docker network create app-network
docker volume create db-data
docker run -d --name db --network app-network -v db-data:/var/lib/mysql ...
docker run -d --name redis --network app-network ...
docker run -d --name app --network app-network -p 8080:8080 ...

# Compose 방식: 파일 하나로 전체 구성
docker compose up -d
```

### 파일 구조

```yaml
# docker-compose.yml
services:          # 컨테이너 정의 (필수)
  서비스명:
    image: ...     # 사용할 이미지 또는
    build: ...     # Dockerfile 빌드 경로

networks:          # 네트워크 정의 (선택, 생략 시 자동 생성)
  네트워크명:

volumes:           # 볼륨 정의 (선택)
  볼륨명:
```

### 주요 서비스 속성

```yaml
services:
  app:
    image: spring-demo:1.0         # 사용할 이미지
    build:                         # 또는 Dockerfile로 빌드
      context: .
      dockerfile: Dockerfile
    container_name: spring-app     # 컨테이너 이름 고정
    ports:
      - "8080:8080"                # 호스트:컨테이너 포트
    environment:                   # 환경변수
      - SPRING_PROFILES_ACTIVE=local
      - SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/mydb
    env_file:                      # 환경변수 파일
      - .env
    volumes:                       # 마운트
      - ./logs:/app/logs
      - app-data:/app/data
    networks:                      # 연결할 네트워크
      - app-network
    depends_on:                    # 의존 서비스
      db:
        condition: service_healthy
    restart: unless-stopped        # 재시작 정책
    healthcheck:                   # 헬스체크
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
```

---

## 7.2 Spring Boot + DB + Redis 멀티 컨테이너 구성

### 전체 구성도

```
사용자
  │ :8080
  ▼
┌─────────────────────────────────────────────────────┐
│                    app-network                      │
│                                                     │
│  ┌──────────────┐   ┌──────────┐   ┌─────────────┐ │
│  │  spring-app  │──▶│  mysql   │   │    redis    │ │
│  │  (:8080)     │   │  (:3306) │   │   (:6379)   │ │
│  └──────────────┘   └──────────┘   └─────────────┘ │
│                          │                         │
└──────────────────────────┼─────────────────────────┘
                           │
                      db-data (volume)
```

### docker-compose.yml

```yaml
services:

  # ── Spring Boot 애플리케이션 ──────────────────────────
  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: spring-app
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=local
      - SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/mydb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      - SPRING_DATASOURCE_USERNAME=appuser
      - SPRING_DATASOURCE_PASSWORD=apppass
      - SPRING_DATA_REDIS_HOST=redis
      - SPRING_DATA_REDIS_PORT=6379
      - JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
    volumes:
      - ./logs:/app/logs
    networks:
      - app-network
    depends_on:
      db:
        condition: service_healthy
      redis:
        condition: service_healthy
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 90s

  # ── MySQL ─────────────────────────────────────────────
  db:
    image: mysql:8.0
    container_name: mysql-db
    environment:
      - MYSQL_ROOT_PASSWORD=rootpass
      - MYSQL_DATABASE=mydb
      - MYSQL_USER=appuser
      - MYSQL_PASSWORD=apppass
    volumes:
      - db-data:/var/lib/mysql
      - ./db/init:/docker-entrypoint-initdb.d  # 초기화 SQL 자동 실행
    networks:
      - app-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-prootpass"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s

  # ── Redis ─────────────────────────────────────────────
  redis:
    image: redis:7-alpine
    container_name: redis-cache
    command: redis-server --appendonly yes  # 데이터 영속성 활성화
    volumes:
      - redis-data:/data
    networks:
      - app-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

# ── 네트워크 ─────────────────────────────────────────────
networks:
  app-network:
    driver: bridge

# ── 볼륨 ─────────────────────────────────────────────────
volumes:
  db-data:
  redis-data:
```

---

## 7.3 환경변수 분리 (.env 파일)

### .env 파일 활용

Compose 파일과 같은 디렉토리의 `.env`는 자동으로 로드된다.

```bash
# .env
COMPOSE_PROJECT_NAME=myapp

# DB 설정
MYSQL_ROOT_PASSWORD=rootpass
MYSQL_DATABASE=mydb
MYSQL_USER=appuser
MYSQL_PASSWORD=apppass

# App 설정
SPRING_PROFILES_ACTIVE=local
APP_PORT=8080
```

```yaml
# docker-compose.yml - 환경변수 파일 참조
services:
  db:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
      - MYSQL_DATABASE=${MYSQL_DATABASE}
      - MYSQL_USER=${MYSQL_USER}
      - MYSQL_PASSWORD=${MYSQL_PASSWORD}

  app:
    ports:
      - "${APP_PORT}:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}
```

> ⚠️ `.env` 파일은 반드시 `.gitignore`에 추가 (패스워드 등 민감 정보 포함)

```
# .gitignore
.env
.env.*
!.env.example   # 예시 파일은 포함
```

```bash
# .env.example (예시 파일, Git에 포함)
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_DATABASE=mydb
MYSQL_USER=appuser
MYSQL_PASSWORD=your_password
SPRING_PROFILES_ACTIVE=local
APP_PORT=8080
```

---

## 7.4 서비스 의존성 제어 (depends_on, healthcheck)

### depends_on 동작 방식

```yaml
# 단순 의존 (컨테이너 시작 순서만 보장, 준비 여부 X)
depends_on:
  - db

# 헬스체크 기반 의존 (서비스 준비 완료 후 시작)
depends_on:
  db:
    condition: service_healthy   # healthcheck 통과 후 시작
  redis:
    condition: service_healthy
```

| condition | 의미 |
|-----------|------|
| `service_started` | 컨테이너 시작만 확인 (기본값) |
| `service_healthy` | healthcheck 통과 확인 |
| `service_completed_successfully` | 컨테이너 정상 종료 확인 (초기화 작업용) |

### healthcheck 설정 패턴

```yaml
# MySQL
healthcheck:
  test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD}"]
  interval: 10s     # 체크 간격
  timeout: 5s       # 응답 대기 시간
  retries: 10       # 실패 허용 횟수
  start_period: 30s # 초기 유예 기간 (이 시간 동안 실패해도 카운트 안 함)

# Redis
healthcheck:
  test: ["CMD", "redis-cli", "ping"]
  interval: 10s
  timeout: 5s
  retries: 5

# Spring Boot (Actuator 필요)
healthcheck:
  test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
  interval: 30s
  timeout: 10s
  retries: 5
  start_period: 90s  # Spring Boot 기동 시간 고려
```

---

## 7.5 Compose 주요 명령어

```bash
# 전체 서비스 시작 (백그라운드)
docker compose up -d

# 특정 서비스만 시작
docker compose up -d db redis

# 이미지 재빌드 후 시작
docker compose up -d --build

# 전체 서비스 중지 (컨테이너·네트워크 제거, 볼륨 유지)
docker compose down

# 볼륨까지 삭제 (데이터 초기화)
docker compose down -v

# 이미지까지 삭제
docker compose down --rmi all

# 서비스 상태 확인
docker compose ps

# 전체 로그 확인
docker compose logs

# 특정 서비스 로그 + 실시간
docker compose logs -f app

# 서비스 재시작
docker compose restart app

# 특정 서비스만 재빌드 + 재시작
docker compose up -d --build app

# 서비스 스케일 (인스턴스 수 조정)
docker compose up -d --scale app=3

# 실행 중인 서비스에 명령 실행
docker compose exec app sh
docker compose exec db mysql -uroot -prootpass

# 서비스 설정 검증
docker compose config
```

---

## 7.6 개발 / 운영 환경별 Compose 파일 분리

### 파일 구성 전략

```
프로젝트/
├── docker-compose.yml          # 공통 기본 설정
├── docker-compose.override.yml # 개발 환경 (자동 병합)
├── docker-compose.prod.yml     # 운영 환경
└── .env
```

### docker-compose.yml (공통)

```yaml
services:
  app:
    build: .
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/mydb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      - SPRING_DATASOURCE_USERNAME=${MYSQL_USER}
      - SPRING_DATASOURCE_PASSWORD=${MYSQL_PASSWORD}
    networks:
      - app-network
    depends_on:
      db:
        condition: service_healthy

  db:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
      - MYSQL_DATABASE=${MYSQL_DATABASE}
      - MYSQL_USER=${MYSQL_USER}
      - MYSQL_PASSWORD=${MYSQL_PASSWORD}
    volumes:
      - db-data:/var/lib/mysql
    networks:
      - app-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s

networks:
  app-network:

volumes:
  db-data:
```

### docker-compose.override.yml (개발 환경 — 자동 적용)

```yaml
# docker compose up 시 자동으로 merge됨
services:
  app:
    ports:
      - "8080:8080"
      - "5005:5005"          # 원격 디버깅 포트
    environment:
      - SPRING_PROFILES_ACTIVE=local
      - JAVA_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
    volumes:
      - ./src:/workspace/src  # 소스 핫 리로드 (Spring DevTools)

  db:
    ports:
      - "3306:3306"          # 개발 시 DB 직접 접속 허용
```

### docker-compose.prod.yml (운영 환경)

```yaml
services:
  app:
    image: myregistry.com/spring-demo:${IMAGE_TAG}  # 사전 빌드된 이미지 사용
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError
    deploy:
      resources:
        limits:
          memory: 512m
          cpus: '1.0'
    restart: unless-stopped
    logging:
      driver: "json-file"
      options:
        max-size: "50m"
        max-file: "5"

  db:
    # 운영에서는 DB 포트 외부 노출 없음
    restart: unless-stopped
    logging:
      driver: "json-file"
      options:
        max-size: "50m"
        max-file: "3"
```

### 환경별 실행 방법

```bash
# 개발 환경 (override 자동 적용)
docker compose up -d

# 운영 환경 (-f 로 파일 명시)
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# 특정 .env 파일 지정
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml up -d
```

---

## 실습

### 실습 7-1: 기본 Compose 파일 작성 및 실행

```
compose-basic/
├── docker-compose.yml
└── .env
```

**.env**
```bash
MYSQL_ROOT_PASSWORD=rootpass
MYSQL_DATABASE=mydb
MYSQL_USER=appuser
MYSQL_PASSWORD=apppass
```

**docker-compose.yml**

```yaml
services:
  db:
    image: mysql:8.0
    container_name: compose-db
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
      - MYSQL_DATABASE=${MYSQL_DATABASE}
      - MYSQL_USER=${MYSQL_USER}
      - MYSQL_PASSWORD=${MYSQL_PASSWORD}
    volumes:
      - db-data:/var/lib/mysql
    networks:
      - app-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s

  redis:
    image: redis:7-alpine
    container_name: compose-redis
    networks:
      - app-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

networks:
  app-network:

volumes:
  db-data:
```

```bash
cd compose-basic

# 1. 서비스 시작
docker compose up -d

# 2. 상태 확인
docker compose ps

# 3. 헬스체크 상태 확인 (healthy 될 때까지 대기)
watch docker compose ps

# 4. 로그 확인
docker compose logs db
docker compose logs redis

# 5. Redis 접속 테스트
docker compose exec redis redis-cli ping
# PONG

# 6. MySQL 접속 테스트
docker compose exec db mysql -uroot -prootpass -e "SHOW DATABASES;"

# 7. 중지 (볼륨 유지)
docker compose down

# 8. 볼륨 확인 (유지됨)
docker volume ls | grep compose

# 9. 볼륨 포함 전체 삭제
docker compose down -v
```

---

### 실습 7-2: Spring Boot + DB + Redis 전체 스택 구성

```
compose-fullstack/
├── docker-compose.yml
├── docker-compose.override.yml
├── Dockerfile
├── .env
└── db/
    └── init/
        └── 01_init.sql
```

**db/init/01_init.sql**

```sql
CREATE TABLE IF NOT EXISTS todos (
  id    BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  done  BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO todos (title) VALUES
  ('Docker 기초 학습'),
  ('Spring Boot 컨테이너화'),
  ('Docker Compose 실습');
```

**docker-compose.yml**

```yaml
services:
  app:
    build: .
    container_name: fullstack-app
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=local
      - SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/mydb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      - SPRING_DATASOURCE_USERNAME=${MYSQL_USER}
      - SPRING_DATASOURCE_PASSWORD=${MYSQL_PASSWORD}
      - SPRING_DATA_REDIS_HOST=redis
      - SPRING_DATA_REDIS_PORT=6379
      - SPRING_JPA_HIBERNATE_DDL_AUTO=validate
      - JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
    volumes:
      - ./logs:/app/logs
    networks:
      - app-network
    depends_on:
      db:
        condition: service_healthy
      redis:
        condition: service_healthy
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 90s

  db:
    image: mysql:8.0
    container_name: fullstack-db
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
      - MYSQL_DATABASE=${MYSQL_DATABASE}
      - MYSQL_USER=${MYSQL_USER}
      - MYSQL_PASSWORD=${MYSQL_PASSWORD}
    volumes:
      - db-data:/var/lib/mysql
      - ./db/init:/docker-entrypoint-initdb.d
    networks:
      - app-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s

  redis:
    image: redis:7-alpine
    container_name: fullstack-redis
    command: redis-server --appendonly yes
    volumes:
      - redis-data:/data
    networks:
      - app-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

networks:
  app-network:
    driver: bridge

volumes:
  db-data:
  redis-data:
```

```bash
# 1. 전체 스택 시작 (이미지 빌드 포함)
docker compose up -d --build

# 2. 서비스별 기동 상태 모니터링
docker compose ps
docker compose logs -f

# 3. 헬스체크 통과 확인
docker compose ps   # STATUS: healthy 확인

# 4. 앱 동작 확인
curl http://localhost:8080/actuator/health
curl http://localhost:8080/todos   # API 확인

# 5. DB 초기화 SQL 적용 확인
docker compose exec db mysql -u${MYSQL_USER} -p${MYSQL_PASSWORD} ${MYSQL_DATABASE} \
  -e "SELECT * FROM todos;"

# 6. Redis 동작 확인
docker compose exec redis redis-cli set testkey "hello"
docker compose exec redis redis-cli get testkey

# 7. 앱만 재빌드 후 재시작
docker compose up -d --build app

# 8. 전체 정리
docker compose down -v
```

---

### 실습 7-3: 개발/운영 환경 파일 분리

```bash
# 개발 환경 실행 (override 자동 병합)
docker compose up -d
docker compose config   # 병합된 설정 확인

# 운영 환경 실행
docker compose -f docker-compose.yml -f docker-compose.prod.yml config  # 설정 확인
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# 환경별 설정 차이 확인
docker compose ps        # 개발: DB 포트 3306 노출
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps  # 운영: DB 포트 미노출
```

---

## 핵심 정리

| 개념 | 핵심 내용 |
|------|----------|
| `docker compose up -d` | 전체 스택 백그라운드 실행 |
| `docker compose down -v` | 볼륨까지 포함한 완전 정리 |
| `docker compose logs -f 서비스명` | 특정 서비스 실시간 로그 |
| `docker compose exec 서비스명 명령어` | 실행 중인 서비스에 명령 실행 |
| `depends_on + condition: service_healthy` | 헬스체크 기반 기동 순서 보장 |
| `healthcheck` | 서비스 준비 여부 자동 감지 |
| `.env` 파일 | 환경변수 분리, Git에서 제외 |
| `override.yml` | 개발 환경 추가 설정 자동 병합 |
| `-f` 옵션 | 운영 등 특정 환경 파일 명시 지정 |

---

## 참고 자료

- [Docker Compose 공식 문서](https://docs.docker.com/compose/)
- [Compose 파일 레퍼런스](https://docs.docker.com/compose/compose-file/)
- [Compose healthcheck](https://docs.docker.com/compose/compose-file/05-services/#healthcheck)
