# 2. Docker 이미지

> 권장 시간: 1.5시간  
> 목표: 이미지의 구조를 이해하고 Dockerfile을 작성하여 커스텀 이미지를 빌드한다

---

## 2.1 이미지 구조와 레이어 개념

### 레이어(Layer) 구조

Docker 이미지는 여러 개의 **읽기 전용 레이어**가 쌓인 구조다.
각 레이어는 이전 레이어의 변경 사항만 저장하므로 저장 공간을 효율적으로 사용한다.

```
┌──────────────────────────────────┐  ← 컨테이너 레이어 (읽기/쓰기, 컨테이너 실행 시 추가)
├──────────────────────────────────┤
│  Layer 4: COPY app.jar /app/     │  ← 읽기 전용 (이미지 레이어)
├──────────────────────────────────┤
│  Layer 3: RUN apt-get install    │  ← 읽기 전용
├──────────────────────────────────┤
│  Layer 2: ENV JAVA_HOME=/opt/... │  ← 읽기 전용
├──────────────────────────────────┤
│  Layer 1: FROM eclipse-temurin   │  ← 읽기 전용 (베이스 이미지)
└──────────────────────────────────┘
```

### 레이어 캐싱의 중요성

```bash
# 이미지 레이어 확인
docker history eclipse-temurin:17-jre-alpine

# 출력 예시
# IMAGE          CREATED       CREATED BY                 SIZE
# abc123...      2 weeks ago   /bin/sh -c #(nop) CMD ...  0B
# def456...      2 weeks ago   /bin/sh -c apk add ...     8.5MB
# ...
```

- 레이어가 변경되지 않으면 **캐시를 재사용** → 빌드 속도 향상
- 한 레이어가 변경되면 **그 이하 레이어는 모두 재빌드**
- 따라서 자주 바뀌는 내용(소스 코드)은 Dockerfile 아래쪽에 위치시켜야 함

---

## 2.2 Docker Hub 활용

### Docker Hub란?

Docker가 공식 제공하는 퍼블릭 이미지 레지스트리 (https://hub.docker.com)
- 공식 이미지: `nginx`, `mysql`, `eclipse-temurin` 등
- 사용자/조직 이미지: `username/image-name` 형식

### 주요 명령어

```bash
# 이미지 검색
docker search eclipse-temurin

# 이미지 다운로드 (pull)
docker pull eclipse-temurin:17-jre-alpine

# 특정 태그 지정 pull (태그 생략 시 latest)
docker pull mysql:8.0

# 로컬 이미지 목록
docker images
docker image ls  # 동일 명령어

# 이미지 상세 정보
docker inspect eclipse-temurin:17-jre-alpine

# 이미지 삭제
docker rmi eclipse-temurin:17-jre-alpine

# Docker Hub 로그인 (push 전 필요)
docker login
docker login -u myusername

# 이미지 업로드 (push)
# 먼저 이미지에 본인 계정 태그를 붙여야 함
docker tag myapp:1.0 myusername/myapp:1.0
docker push myusername/myapp:1.0
```

### 이미지 태그 네이밍 규칙

```
[레지스트리 주소/] [사용자명/] 이미지명 [:태그]

예시:
  nginx                        → Docker Hub 공식 이미지, latest 태그
  nginx:1.25                   → 특정 버전 태그
  mysql:8.0                    → MySQL 8.0
  myuser/myapp:1.0             → 사용자 이미지
  myregistry.com/myapp:prod    → Private Registry 이미지
```

> ⚠️ **운영 환경에서는 `latest` 태그 사용을 지양할 것**  
> `latest`는 pull 시점마다 다른 버전이 될 수 있어 재현성이 떨어짐

---

## 2.3 Dockerfile 작성 문법

### Dockerfile이란?

이미지를 만들기 위한 **빌드 명세서(레시피)** 파일.
각 명령어 한 줄이 하나의 레이어가 됨.

### 주요 명령어

#### FROM — 베이스 이미지 지정

```dockerfile
# 항상 첫 번째 명령어
FROM eclipse-temurin:17-jre-alpine

# 별칭(alias) 지정 — 멀티스테이지 빌드에서 활용
FROM eclipse-temurin:17-jdk-alpine AS builder
```

#### RUN — 빌드 시 명령어 실행

```dockerfile
# Shell 형식 (권장: 가독성 우수)
RUN apt-get update && apt-get install -y curl

# && 로 연결하여 레이어 최소화 (중요!)
RUN apt-get update \
    && apt-get install -y curl wget \
    && rm -rf /var/lib/apt/lists/*

# Exec 형식 (Shell 없이 직접 실행)
RUN ["apt-get", "install", "-y", "curl"]
```

#### COPY vs ADD — 파일 복사

```dockerfile
# COPY: 로컬 파일 → 이미지 내부 (단순 복사, 권장)
COPY target/app.jar /app/app.jar
COPY src/ /app/src/

# ADD: COPY 기능 + URL 다운로드 + 압축 자동 해제 (특수한 경우만 사용)
ADD https://example.com/file.tar.gz /app/   # URL 다운로드
ADD archive.tar.gz /app/                    # 압축 해제
```

> 💡 **일반적인 파일 복사는 항상 `COPY` 사용** — `ADD`는 압축 해제나 URL 다운로드가 필요한 경우에만

#### ENV — 환경변수 설정

```dockerfile
# 빌드 시 + 컨테이너 실행 시 모두 유효
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV APP_HOME=/app
ENV SPRING_PROFILES_ACTIVE=prod

# 여러 개를 한 줄에
ENV APP_HOME=/app \
    LOG_PATH=/logs
```

#### EXPOSE — 포트 문서화

```dockerfile
# 컨테이너가 사용하는 포트를 명시 (실제 포트 개방은 docker run -p로 수행)
EXPOSE 8080
EXPOSE 8080/tcp
```

> `EXPOSE`는 실제로 포트를 열지 않음 — 문서화 및 `-P` 옵션과의 연동 목적

#### WORKDIR — 작업 디렉토리 설정

```dockerfile
# 이후 명령어(RUN, COPY, CMD 등)의 기준 디렉토리
WORKDIR /app

# WORKDIR 이후 상대 경로 사용 가능
COPY app.jar .          # /app/app.jar 에 복사됨
RUN ls -la              # /app 에서 실행
```

#### CMD vs ENTRYPOINT — 컨테이너 실행 명령어

```dockerfile
# CMD: 컨테이너 실행 시 기본 명령어 (docker run 에서 덮어쓰기 가능)
CMD ["java", "-jar", "/app/app.jar"]

# ENTRYPOINT: 항상 실행되는 고정 명령어 (덮어쓰기 어려움)
ENTRYPOINT ["java"]
CMD ["-jar", "/app/app.jar"]   # ENTRYPOINT의 기본 인자로 동작
```

| 항목 | CMD | ENTRYPOINT |
|------|-----|-----------|
| 목적 | 기본 실행 명령어 | 항상 실행되어야 할 명령어 |
| `docker run` 인자로 덮어쓰기 | 가능 | 불가능 (--entrypoint 옵션 필요) |
| 주 사용처 | 일반적인 실행 | 실행 파일처럼 동작할 때 |

#### ARG — 빌드 시 인자

```dockerfile
# Dockerfile 내에서 사용하는 빌드 타임 변수 (컨테이너 실행 시에는 없음)
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# 빌드 시 값 전달
# docker build --build-arg JAR_FILE=target/myapp.jar .
```

#### USER — 실행 사용자 변경

```dockerfile
# root 대신 일반 사용자로 실행 (보안)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
```

---

## 2.4 이미지 빌드 및 태그 관리

### 기본 빌드

```bash
# 현재 디렉토리의 Dockerfile로 빌드
docker build .

# 태그 지정 (-t)
docker build -t myapp:1.0 .

# 여러 태그 지정
docker build -t myapp:1.0 -t myapp:latest .

# Dockerfile 경로 지정
docker build -f docker/Dockerfile.prod -t myapp:prod .

# 빌드 인자 전달
docker build --build-arg JAR_FILE=target/myapp.jar -t myapp:1.0 .
```

### 태그 추가 및 변경

```bash
# 기존 이미지에 새 태그 추가 (이미지 복사 아님, 참조 추가)
docker tag myapp:1.0 myapp:latest
docker tag myapp:1.0 myregistry.com/myapp:1.0

# 이미지 목록 확인 — 동일 IMAGE ID에 여러 태그
docker images
```

### 빌드 캐시 관리

```bash
# 캐시 없이 빌드 (항상 새로 빌드)
docker build --no-cache -t myapp:1.0 .

# 빌드 캐시 삭제
docker builder prune
```

---

## 2.5 .dockerignore 활용

### .dockerignore란?

`.gitignore`와 동일한 방식으로, **빌드 컨텍스트에서 제외할 파일/폴더**를 지정.
빌드 컨텍스트는 `docker build` 시 Daemon에 전송되는 디렉토리 전체를 의미.

### Spring Boot 프로젝트 권장 설정

```
# .dockerignore

# Git
.git
.gitignore

# IDE 설정
.idea/
*.iml
.vscode/
*.classpath
*.project
*.settings/

# 빌드 산출물 (Dockerfile에서 target/ 을 직접 COPY하는 경우 제외)
# target/   ← 주의: JAR를 COPY해야 하면 주석 처리

# 테스트 관련
src/test/

# 로컬 환경설정
.env
*.local.properties
application-local.yml

# 로그
*.log
logs/

# 문서
README.md
docs/

# OS 파일
.DS_Store
Thumbs.db
```

### 효과

```bash
# .dockerignore 없을 때
# Sending build context to Docker daemon  450MB  ← node_modules, .git 등 포함

# .dockerignore 적용 후
# Sending build context to Docker daemon  15MB   ← 필요한 파일만
```

---

## 실습

### 실습 2-1: Docker Hub 이미지 탐색 및 pull

```bash
# 1. eclipse-temurin 이미지 검색
docker search eclipse-temurin

# 2. JRE 17 Alpine 이미지 pull
docker pull eclipse-temurin:17-jre-alpine

# 3. 이미지 정보 확인
docker images eclipse-temurin
docker inspect eclipse-temurin:17-jre-alpine

# 4. 이미지 레이어 히스토리 확인
docker history eclipse-temurin:17-jre-alpine

# 5. 이미지로 Java 버전 확인
docker run --rm eclipse-temurin:17-jre-alpine java -version
```

**확인 포인트**
- `docker images`에서 이미지 크기 비교 (Alpine vs 일반)
- `docker history`에서 레이어 구조 확인

---

### 실습 2-2: 첫 번째 Dockerfile 작성 및 빌드

프로젝트 구조:
```
my-first-image/
├── Dockerfile
├── .dockerignore
└── hello.sh
```

**hello.sh**

```bash
#!/bin/sh
echo "==============================="
echo " Hello from Docker Container!"
echo " Date: $(date)"
echo " Hostname: $(hostname)"
echo "==============================="
```

**Dockerfile**

```dockerfile
# 베이스 이미지: Alpine Linux (경량)
FROM alpine:3.19

# 메타데이터
LABEL maintainer="your-email@example.com"
LABEL version="1.0"
LABEL description="My first Docker image"

# 환경변수
ENV APP_HOME=/app

# 작업 디렉토리 생성 및 설정
WORKDIR ${APP_HOME}

# 쉘 스크립트 복사
COPY hello.sh .

# 실행 권한 부여
RUN chmod +x hello.sh

# 컨테이너 실행 명령어
CMD ["./hello.sh"]
```

**.dockerignore**

```
.git
*.md
```

**빌드 및 실행**

```bash
cd my-first-image

# 이미지 빌드
docker build -t my-first-image:1.0 .

# 빌드 결과 확인
docker images my-first-image

# 컨테이너 실행
docker run --rm my-first-image:1.0

# 레이어 확인
docker history my-first-image:1.0
```

---

### 실습 2-3: Spring Boot JAR 이미지 빌드

> **전제**: Spring Boot 프로젝트가 Maven/Gradle로 빌드된 JAR 파일이 있어야 함  
> 없는 경우 아래 샘플 JAR를 사용하거나 강사 제공 파일 활용

프로젝트 구조:
```
spring-docker/
├── Dockerfile
├── .dockerignore
└── target/
    └── demo-0.0.1-SNAPSHOT.jar
```

**Dockerfile (기본 버전)**

```dockerfile
FROM eclipse-temurin:17-jre-alpine

# 작업 디렉토리
WORKDIR /app

# JAR 파일 복사
COPY target/demo-0.0.1-SNAPSHOT.jar app.jar

# 포트 노출
EXPOSE 8080

# 실행
CMD ["java", "-jar", "app.jar"]
```

**.dockerignore**

```
.git
.gitignore
.idea/
*.iml
src/test/
*.md
*.log
logs/
```

**빌드 및 실행**

```bash
# 1. Spring Boot 프로젝트 빌드 (JAR 생성)
./mvnw clean package -DskipTests
# 또는 Gradle
./gradlew bootJar

# 2. Docker 이미지 빌드
docker build -t spring-demo:1.0 .

# 3. 이미지 크기 확인
docker images spring-demo

# 4. 컨테이너 실행
docker run -d -p 8080:8080 --name spring-demo spring-demo:1.0

# 5. 애플리케이션 로그 확인
docker logs -f spring-demo

# 6. 접속 확인
curl http://localhost:8080

# 7. 정리
docker stop spring-demo && docker rm spring-demo
```

---

### 실습 2-4: CMD vs ENTRYPOINT 차이 비교

**Dockerfile-cmd**

```dockerfile
FROM alpine:3.19
CMD ["echo", "기본 메시지입니다"]
```

**Dockerfile-entrypoint**

```dockerfile
FROM alpine:3.19
ENTRYPOINT ["echo"]
CMD ["기본 메시지입니다"]
```

**빌드 및 비교 실행**

```bash
# 빌드
docker build -f Dockerfile-cmd -t test-cmd .
docker build -f Dockerfile-entrypoint -t test-entrypoint .

# --- CMD 테스트 ---
# 기본 실행 (CMD 사용)
docker run --rm test-cmd
# 출력: 기본 메시지입니다

# docker run 인자로 CMD 덮어쓰기
docker run --rm test-cmd echo "덮어쓴 메시지"
# 출력: 덮어쓴 메시지

# --- ENTRYPOINT 테스트 ---
# 기본 실행 (ENTRYPOINT + CMD)
docker run --rm test-entrypoint
# 출력: 기본 메시지입니다

# docker run 인자가 CMD를 대체 (ENTRYPOINT는 유지)
docker run --rm test-entrypoint "인자로 전달한 메시지"
# 출력: 인자로 전달한 메시지

# ENTRYPOINT 자체를 바꾸려면 --entrypoint 필요
docker run --rm --entrypoint sh test-entrypoint -c "echo '완전히 교체'"
```

**확인 포인트**
- CMD는 `docker run` 뒤의 인자로 완전히 교체됨
- ENTRYPOINT는 `docker run` 뒤의 인자가 CMD 부분만 교체
- Spring Boot에서는 `CMD ["java", "-jar", "app.jar"]` 형태가 일반적

---

### 실습 2-5: .dockerignore 효과 확인

```bash
mkdir dockerignore-test && cd dockerignore-test

# 큰 더미 파일 생성
dd if=/dev/zero of=bigfile.bin bs=1M count=50   # 50MB 파일 생성
mkdir node_modules && dd if=/dev/zero of=node_modules/big.bin bs=1M count=30

# Dockerfile 생성
cat > Dockerfile << 'EOF'
FROM alpine:3.19
COPY . /app
CMD ["ls", "-la", "/app"]
EOF

# .dockerignore 없이 빌드 → 빌드 컨텍스트 크기 확인
docker build -t ignore-test:no-ignore .

# .dockerignore 추가
cat > .dockerignore << 'EOF'
*.bin
node_modules/
EOF

# .dockerignore 적용 후 빌드 → 컨텍스트 크기 비교
docker build -t ignore-test:with-ignore .
```

**확인 포인트**
- 빌드 출력에서 `Sending build context to Docker daemon` 크기 비교
- 컨텍스트 크기가 빌드 속도에 미치는 영향 이해

---

## 핵심 정리

| 개념 | 핵심 내용 |
|------|----------|
| 레이어 구조 | 변경분만 저장, 캐시 재사용으로 빌드 속도 향상 |
| FROM | 베이스 이미지 지정, 항상 첫 번째 명령어 |
| RUN | 빌드 시 실행, `&&`로 연결해 레이어 최소화 |
| COPY vs ADD | 단순 복사는 COPY, 압축 해제/URL은 ADD |
| CMD vs ENTRYPOINT | CMD는 덮어쓰기 가능, ENTRYPOINT는 고정 |
| .dockerignore | 빌드 컨텍스트 최소화로 빌드 속도 향상 |
| 태그 | 운영 환경에서는 `latest` 지양, 명시적 버전 사용 |

---

## 참고 자료

- [Dockerfile 공식 레퍼런스](https://docs.docker.com/engine/reference/builder/)
- [Dockerfile 베스트 프랙티스](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)
- [eclipse-temurin 공식 이미지 (Docker Hub)](https://hub.docker.com/_/eclipse-temurin)
- [Alpine Linux 공식 이미지](https://hub.docker.com/_/alpine)
