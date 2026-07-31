
-- 데이터베이스 만들기 - 관리자 계정으로 실행
CREATE DATABASE shopdb
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;
  
-- 데이터베이스 목록 표시
SHOW DATABASES;

-- 작업 데이터베이스 변경
USE shopdb;

-- 현재 작업중인 데이터베이스 확인(조회)
SELECT DATABASE();

-- 데이터베이스 삭제 1
DROP DATABASE shopdb;

-- 데이터베이스 삭제 2 : 데이터베이스가 있을 때만 삭제
DROP DATABASE IF EXISTS shopdb;

-- 데이터베이스 생성 2. 데이터베이스가 없을 때만 생성
CREATE DATABASE IF NOT EXISTS shopdb
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;
  
-- 사용자에게 데이터베이스 사용 권한 부여 - 관리자 계정으로 실행
GRANT ALL PRIVILEGES ON shopdb.* TO human@localhost ;
GRANT ALL PRIVILEGES ON shopdb.* TO human@"%" ;

FLUSH PRIVILEGES;



