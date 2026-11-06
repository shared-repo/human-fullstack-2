# ============================================================
# 데이터베이스 컨테이너 (MariaDB)
# ------------------------------------------------------------
# - imageboard 데이터베이스 생성 (utf8mb4)
# - human / human 계정 생성 (로컬 및 원격 접속 허용)
# - human 계정에 imageboard 데이터베이스 전체 권한 부여
# ============================================================

FROM mariadb:latest

# root 계정 비밀번호 (실습용 값이므로 운영 환경에서는 반드시 변경)
ENV MARIADB_ROOT_PASSWORD=root \
    TZ=Asia/Seoul

# 서버 기본 문자셋을 utf8mb4로 설정
RUN printf '[mysqld]\n\
character-set-server=utf8mb4\n\
collation-server=utf8mb4_unicode_ci\n\
bind-address=0.0.0.0\n\
\n\
[client]\n\
default-character-set=utf8mb4\n' > /etc/mysql/conf.d/charset.cnf

# 컨테이너 최초 실행 시 자동으로 수행될 초기화 SQL
# (/docker-entrypoint-initdb.d 에 있는 .sql 파일은 데이터 디렉터리가 비어 있을 때 한 번 실행됨)
COPY init.sql /docker-entrypoint-initdb.d/01-init.sql

# 데이터베이스 포트
EXPOSE 3306
