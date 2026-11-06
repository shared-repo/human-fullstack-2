-- ============================================================
-- imageboard 데이터베이스 및 human 사용자 초기화 스크립트
-- ============================================================

-- 1. imageboard 데이터베이스 생성 (문자셋 utf8mb4)
CREATE DATABASE IF NOT EXISTS imageboard
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. human 사용자 생성
--    'localhost' : 컨테이너 내부(로컬) 접속용
--    '%'         : 다른 컨테이너 및 외부(원격) 접속용
CREATE USER IF NOT EXISTS 'human'@'localhost' IDENTIFIED BY 'human';
CREATE USER IF NOT EXISTS 'human'@'%'         IDENTIFIED BY 'human';

-- 3. imageboard 데이터베이스에 대한 모든 권한 부여
GRANT ALL PRIVILEGES ON imageboard.* TO 'human'@'localhost';
GRANT ALL PRIVILEGES ON imageboard.* TO 'human'@'%';

FLUSH PRIVILEGES;
