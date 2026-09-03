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
COPY hello.sh hello2.sh .

# 실행 권한 부여
RUN chmod +x hello.sh

# 컨테이너 실행 명령어
ENTRYPOINT ["/bin/sh"]
CMD ["./hello.sh"]
