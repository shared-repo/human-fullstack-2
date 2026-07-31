-- SQL의 주석

-- 1. 사용자 계정 생성 ( DCL ) - 관리자(root) 계정으로 실행
CREATE USER human@localhost IDENTIFIED BY "human";
-- CREATE USER human@"192.168.0.19" IDENTIFIED BY "human";
CREATE USER human@"%" IDENTIFIED BY "human"; -- % : 모든 다른 컴퓨터

FLUSH PRIVILEGES; -- 현재까지 실행한 계정 작업 확정

-- 작업 데이터베이스 변경
USE mysql; 

-- 계정 생성 확인
SELECT USER, HOST FROM USER;