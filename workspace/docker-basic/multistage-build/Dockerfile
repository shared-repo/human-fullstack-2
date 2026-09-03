# =====================
# Stage 1: Build
# =====================
FROM eclipse-temurin:21-jdk-alpine AS builder

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
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
