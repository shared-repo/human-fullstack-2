# ============================================================
# 웹 애플리케이션 컨테이너 (Spring Boot + Gradle)
# ------------------------------------------------------------
# 1단계(builder): GitHub에서 소스 코드를 clone 한 뒤 Gradle로 빌드
# 2단계(runtime): 빌드된 JAR 파일만 JRE 이미지로 복사하여 실행
# ============================================================

# ---------- 1단계: 빌드 ----------
FROM gradle:8.10-jdk21 AS builder

# GitHub 저장소 주소와 브랜치 (빌드 시 --build-arg 로 변경 가능)
ARG GIT_REPO_URL=https://github.com/shared-repo/deploy.git
ARG GIT_BRANCH=master

# git 설치 확인 (이미지에 포함되어 있지 않은 경우 대비)
RUN apt-get update \
    && apt-get install -y --no-install-recommends git \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

# 소스 코드 clone
RUN git clone --depth 1 --branch ${GIT_BRANCH} ${GIT_REPO_URL} app

WORKDIR /workspace/app/imageboard

# Gradle 빌드 (gradlew 가 있으면 wrapper 사용, 없으면 이미지의 gradle 사용)
# 테스트는 DB 연결이 필요할 수 있으므로 실습에서는 제외(-x test)
RUN if [ -f ./gradlew ]; then \
        chmod +x ./gradlew && ./gradlew clean bootJar -x test --no-daemon; \
    else \
        gradle clean bootJar -x test --no-daemon; \
    fi

# 실행 가능한 JAR 파일만 고정된 이름으로 복사 (*-plain.jar 제외)
RUN find build/libs -name "*.jar" ! -name "*-plain.jar" -exec cp {} /workspace/app.jar \;


# ---------- 2단계: 실행 ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

# 빌드 단계에서 생성된 JAR 파일 복사
COPY --from=builder /workspace/app.jar app.jar

# 데이터베이스 접속 정보 (docker run -e 옵션으로 변경 가능)
# 호스트명 imageboard-db 는 데이터베이스 컨테이너 이름
ENV SPRING_DATASOURCE_URL=jdbc:mariadb://imageboard-db:3306/imageboard?characterEncoding=UTF-8 \
    SPRING_DATASOURCE_USERNAME=human \
    SPRING_DATASOURCE_PASSWORD=human \
    SERVER_PORT=8080 \
    TZ=Asia/Seoul

# 웹 애플리케이션 포트
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
