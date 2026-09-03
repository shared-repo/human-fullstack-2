FROM eclipse-temurin:21-jre-alpine

# 작업 디렉토리
WORKDIR /app

# JAR 파일 복사
COPY target/imageboard-0.0.1-SNAPSHOT.jar app.jar

# 포트 노출
EXPOSE 8080

# 실행
CMD ["java", "-jar", "app.jar"]
