# 3. Spring Boot 애플리케이션 컨테이너화

> 권장 시간: 2.5시간  
> 목표: Spring Boot 애플리케이션을 효율적으로 컨테이너 이미지로 패키징하고 최적화한다

---

## 3.1 JAR 패키징과 Dockerfile 작성 전략

### Spring Boot JAR의 특성

Spring Boot는 `bootJar` / `bootRepackage` 태스크를 통해 **실행 가능한 Fat JAR**를 생성한다.
이 JAR는 애플리케이션 클래스, 의존 라이브러리, 내장 톰캣을 모두 포함한다.

```
demo-0.0.1-SNAPSHOT.jar
├── BOOT-INF/
│   ├── classes/          ← 애플리케이션 클래스 (자주 변경됨)
│   └── lib/              ← 의존 라이브러리 (거의 변경 안 됨)
├── META-INF/
└── org/springframework/  ← Spring Boot Loader
```

### 단순 Dockerfile (초기 버전 — 비효율적)

```dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Fat JAR 전체를 하나의 레이어로 복사 → 코드 한 줄 바꿔도 전체 JAR 재전송
COPY target/demo-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
```

**문제점**: 소스 코드만 변경해도 수백 MB의 의존성까지 함께 다시 전송됨

---

## 3.2 멀티스테이지 빌드

### 개념

하나의 Dockerfile 안에서 **여러 단계(Stage)** 로 나눠 빌드한다.
- **빌드 스테이지**: JDK + Maven/Gradle로 JAR 생성
- **런타임 스테이지**: JRE만 있는 경량 이미지로 JAR 실행

최종 이미지에는 빌드 도구(JDK, Maven, Gradle)가 포함되지 않아 **크기를 크게 줄일 수 있다**.

```
┌─────────────────────────────┐     ┌──────────────────────────────┐
│   Stage 1: builder          │     │   Stage 2: runtime           │
│                             │     │                              │
│  eclipse-temurin:17-jdk     │     │  eclipse-temurin:17-jre      │
│  + Maven / Gradle           │────▶│  + app.jar (복사)            │
│  + 소스 코드                │COPY │                              │
│  → JAR 빌드                 │     │  최종 이미지 (경량)          │
└─────────────────────────────┘     └──────────────────────────────┘
     ~600MB                               ~200MB
```

### Maven 프로젝트용 멀티스테이지 Dockerfile

```dockerfile
# =====================
# Stage 1: Build
# =====================
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /workspace

# 의존성 캐싱: pom.xml 먼저 복사 후 dependency 다운로드
# → 소스 코드만 바뀌면 이 레이어는 캐시 재사용
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# 소스 코드 복사 및 빌드
COPY src/ src/
RUN ./mvnw package -DskipTests -B

# =====================
# Stage 2: Runtime
# =====================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# builder 스테이지에서 JAR만 복사
COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
```

### Gradle 프로젝트용 멀티스테이지 Dockerfile

```dockerfile
# =====================
# Stage 1: Build
# =====================
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /workspace

# 의존성 캐싱: Gradle 설정 파일 먼저 복사
COPY build.gradle settings.gradle gradlew ./
COPY gradle/ gradle/
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사 및 빌드
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# =====================
# Stage 2: Runtime
# =====================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
```

---

## 3.3 JVM 옵션 튜닝

### 컨테이너 환경에서의 JVM 메모리 문제

JDK 8u191 이전에는 JVM이 컨테이너의 메모리 제한을 무시하고 **호스트 전체 메모리**를 기준으로 Heap을 설정했다.
현재(JDK 17)는 컨테이너 메모리 제한을 자동 인식하지만, 명시적 설정이 더 안전하다.

```
호스트 메모리: 16GB
컨테이너 메모리 제한: 512MB

❌ JVM 기본 설정 (구버전): 16GB 기준으로 Heap 설정 → OOMKilled
✅ 컨테이너 인식 설정: 512MB 기준으로 Heap 설정
```

### 권장 JVM 옵션

```dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8080

# 환경변수로 JVM 옵션 분리 → docker run -e 로 재정의 가능
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom"

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 주요 JVM 옵션 설명

| 옵션 | 설명 |
|------|------|
| `-XX:+UseContainerSupport` | 컨테이너 메모리 제한 인식 (JDK 10+, 17에서 기본 활성화) |
| `-XX:MaxRAMPercentage=75.0` | 컨테이너 메모리의 75%를 최대 Heap으로 사용 |
| `-XX:InitialRAMPercentage=50.0` | 초기 Heap 크기를 컨테이너 메모리의 50%로 설정 |
| `-XX:+ExitOnOutOfMemoryError` | OOM 발생 시 즉시 종료 (무한 대기 방지) |
| `-Djava.security.egd=...` | 난수 생성 속도 개선 (Spring Boot 기동 시간 단축) |

### 실행 시 JVM 옵션 재정의

```bash
# 기본 실행
docker run -d -p 8080:8080 spring-demo:1.0

# 메모리 제한 + JVM 옵션 재정의
docker run -d -p 8080:8080 \
  --memory=512m \
  -e JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Xss512k" \
  spring-demo:1.0

# Spring Profile 설정
docker run -d -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  spring-demo:1.0
```

---

## 3.4 이미지 경량화 전략

### 베이스 이미지 선택

| 이미지 | 크기 (참고) | 특징 |
|--------|------------|------|
| `eclipse-temurin:17-jdk` | ~480MB | JDK 포함, 빌드용 |
| `eclipse-temurin:17-jre` | ~280MB | JRE만 포함, 런타임용 |
| `eclipse-temurin:17-jre-alpine` | ~185MB | Alpine 기반, 경량 |
| `eclipse-temurin:17-jre-jammy` | ~290MB | Ubuntu 기반, 호환성 우수 |

> ⚠️ Alpine은 `glibc` 대신 `musl libc` 사용 → 일부 네이티브 라이브러리 호환성 문제 가능
> 호환성 이슈가 있다면 `-jammy` (Ubuntu) 사용 권장

### 불필요한 파일 제거

```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /workspace
COPY pom.xml .
COPY mvnw .mvn/ ./
RUN chmod +x mvnw

# 캐시 없이 빌드 (이미지에 Maven 캐시 남기지 않음)
RUN ./mvnw dependency:go-offline -B

COPY src/ src/
# 테스트 제외, 빌드 캐시 정리
RUN ./mvnw package -DskipTests -B \
    && rm -rf ~/.m2/repository

FROM eclipse-temurin:17-jre-alpine

# 패키지 설치 후 캐시 정리
RUN apk add --no-cache curl

WORKDIR /app
COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

---

## 3.5 레이어 캐싱 최적화

### Spring Boot 레이어 분리 (Layered JAR)

Spring Boot 2.3+부터 JAR를 레이어별로 분리하는 기능을 제공한다.
의존성(lib)과 애플리케이션 코드(classes)를 **별도 Docker 레이어**로 분리하여
소스 변경 시 의존성 레이어를 캐시 재사용한다.

```
일반 JAR 방식:
  [변경 안됨] 의존성 150MB ┐
  [변경됨]   앱 코드  5MB  ┘ → 전체 155MB 재전송

레이어 분리 방식:
  [변경 안됨] 의존성 레이어 150MB → 캐시 재사용 ✅
  [변경됨]   앱 코드 레이어   5MB → 5MB만 재전송 ✅
```

### Layered JAR Dockerfile (Maven)

```dockerfile
# =====================
# Stage 1: Build
# =====================
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /workspace

COPY pom.xml .
COPY mvnw .mvn/ ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

COPY src/ src/
RUN ./mvnw package -DskipTests -B

# JAR를 레이어별로 추출
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted

# =====================
# Stage 2: Runtime
# =====================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 레이어 순서 중요: 변경 빈도 낮은 것 → 높은 것 순서로
# 1. 외부 라이브러리 (거의 변경 안됨)
COPY --from=builder /workspace/extracted/dependencies/ ./
# 2. Spring Boot 내부 라이브러리 (거의 변경 안됨)
COPY --from=builder /workspace/extracted/spring-boot-loader/ ./
# 3. 스냅샷 의존성 (가끔 변경됨)
COPY --from=builder /workspace/extracted/snapshot-dependencies/ ./
# 4. 애플리케이션 코드 (자주 변경됨)
COPY --from=builder /workspace/extracted/application/ ./

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
```

### application.yml에서 레이어 활성화 확인

```yaml
# Spring Boot 2.3+ 기본 활성화 (별도 설정 불필요)
# pom.xml에서 확인
# <plugin>
#   <groupId>org.springframework.boot</groupId>
#   <artifactId>spring-boot-maven-plugin</artifactId>
#   <configuration>
#     <layers>
#       <enabled>true</enabled>
#     </layers>
#   </configuration>
# </plugin>
```

---

## 실습

### 실습 3-1: 단순 Dockerfile vs 멀티스테이지 빌드 이미지 크기 비교

**디렉토리 구조**

```
spring-docker/
├── Dockerfile.simple
├── Dockerfile.multi
├── .dockerignore
└── (Spring Boot 프로젝트)
```

**Dockerfile.simple**

```dockerfile
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY . .

RUN chmod +x mvnw && \
    ./mvnw package -DskipTests -B

EXPOSE 8080

CMD ["java", "-jar", "target/*.jar"]
```

**Dockerfile.multi**

```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /workspace

COPY pom.xml .
COPY mvnw .mvn/ ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

COPY src/ src/
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

**빌드 및 비교**

```bash
# 단순 방식 빌드
docker build -f Dockerfile.simple -t spring-simple:1.0 .

# 멀티스테이지 방식 빌드
docker build -f Dockerfile.multi -t spring-multi:1.0 .

# 크기 비교
docker images | grep spring

# 출력 예시:
# spring-simple   1.0   ...   620MB
# spring-multi    1.0   ...   195MB
```

---

### 실습 3-2: JVM 옵션 환경변수 활용

```dockerfile
# Dockerfile.jvm
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom"

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

```bash
# 빌드
docker build -f Dockerfile.jvm -t spring-jvm:1.0 .

# 1) 메모리 제한 없이 실행
docker run -d -p 8080:8080 --name jvm-test spring-jvm:1.0

# JVM 메모리 확인 (actuator 없이)
docker exec jvm-test java -XX:+PrintFlagsFinal -version 2>&1 | grep -E "MaxHeapSize|InitialHeapSize"

docker stop jvm-test && docker rm jvm-test

# 2) 메모리 제한 적용 후 비교
docker run -d -p 8080:8080 --name jvm-limited \
  --memory=512m \
  spring-jvm:1.0

docker exec jvm-limited java -XX:+PrintFlagsFinal -version 2>&1 | grep -E "MaxHeapSize|InitialHeapSize"

docker stop jvm-limited && docker rm jvm-limited

# 3) Spring 프로파일 환경변수로 주입
docker run -d -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local \
  -e JAVA_OPTS="-XX:MaxRAMPercentage=60.0" \
  --name jvm-env spring-jvm:1.0

docker logs jvm-env
docker stop jvm-env && docker rm jvm-env
```

**확인 포인트**
- 메모리 제한 적용 전후 `MaxHeapSize` 값 변화
- `-e SPRING_PROFILES_ACTIVE` 로 프로파일 주입 확인

---

### 실습 3-3: Layered JAR로 빌드 캐시 최적화

```dockerfile
# Dockerfile.layered
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /workspace

COPY pom.xml .
COPY mvnw .mvn/ ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

COPY src/ src/
RUN ./mvnw package -DskipTests -B

# 레이어 추출
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /workspace/extracted/dependencies/ ./
COPY --from=builder /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/extracted/application/ ./

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
```

```bash
# 1) 최초 빌드 (시간 측정)
time docker build -f Dockerfile.layered -t spring-layered:1.0 .

# 2) 소스 코드만 변경 후 재빌드 (시간 비교)
# src/main/java/... 에서 아무 클래스나 공백 추가 후 저장
time docker build -f Dockerfile.layered -t spring-layered:1.1 .

# 레이어 확인
docker history spring-layered:1.0

# 실행 테스트
docker run -d -p 8080:8080 --name layered-test spring-layered:1.0
docker logs layered-test
docker stop layered-test && docker rm layered-test
```

**확인 포인트**
- 소스 변경 후 재빌드 시 의존성 레이어에 `CACHED` 표시 확인
- `application` 레이어만 재빌드됨을 확인

---

### 실습 3-4: 베이스 이미지 종류별 크기 비교

```bash
# 각 베이스 이미지 pull
docker pull eclipse-temurin:17-jdk
docker pull eclipse-temurin:17-jre
docker pull eclipse-temurin:17-jre-alpine
docker pull eclipse-temurin:17-jre-jammy

# 크기 비교
docker images eclipse-temurin

# 각 이미지로 동일한 앱 빌드
for TAG in jre jre-alpine jre-jammy; do
  cat > Dockerfile.${TAG} << EOF
FROM eclipse-temurin:17-${TAG}
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
EOF
  docker build -f Dockerfile.${TAG} -t spring-${TAG}:1.0 .
done

# 최종 이미지 크기 비교
docker images | grep spring-
```

---

## 핵심 정리

| 전략 | 효과 |
|------|------|
| 멀티스테이지 빌드 | JDK/빌드 도구를 최종 이미지에서 제거 → 이미지 크기 50~60% 감소 |
| 의존성 먼저 COPY | `pom.xml` → 의존성 다운로드 → 소스 복사 순서로 캐시 효율 극대화 |
| Layered JAR | 소스 변경 시 의존성 레이어 캐시 재사용 → 빌드/배포 속도 향상 |
| Alpine 베이스 이미지 | 일반 이미지 대비 30~40% 크기 절감 |
| `JAVA_OPTS` 환경변수 | 컨테이너 메모리 제한에 맞는 JVM 튜닝, 런타임 재정의 가능 |
| `MaxRAMPercentage` | 컨테이너 메모리 기준으로 Heap 비율 설정 (절대값보다 유연) |

---

## 참고 자료

- [Spring Boot Docker 공식 가이드](https://spring.io/guides/topicals/spring-boot-docker/)
- [Layered Jars 공식 문서](https://docs.spring.io/spring-boot/docs/current/reference/html/container-images.html#container-images.efficient-images.layering)
- [eclipse-temurin 공식 이미지](https://hub.docker.com/_/eclipse-temurin)
- [JVM 컨테이너 지원 옵션](https://www.baeldung.com/ops/docker-jvm-heap-size)
