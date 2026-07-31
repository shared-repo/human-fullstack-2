-- 0. 데이터베이스 만들기 ( root 계정으로 실행 )

--     - 이름 : kamebook 
CREATE DATABASE kamebook  
	CHARACTER SET utf8mb4
  	COLLATE utf8mb4_general_ci;
  	
-- 2. 권한 부여 ( root 계정으로 실행 )
GRANT ALL PRIVILEGES ON kamebook.* TO human@localhost;
GRANT ALL PRIVILEGES ON kamebook.* TO human@"%";


-- 2. 작업 데이터베이스 변경 ( human 계정으로 실행 )

USE kamebook;

-- 테이블 만들기 SQL문 형식
/*
CREATE TABLE `테이블이름`
(
	컬럼이름1 자료형 [option_1, option_2, ... option_n],
    컬럼이름2 자료형 [option_1, option_2, ... option_n],
    ...
);
*/

-- 주요 자료형
/*
	문자형 : char(갯수), varchar(갯수), text
    날짜형 : date, datetime
    숫자형 : float, double, int, bigint
    진위형 : boolean
*/

-- primary key 
-- : 각 행의 데이터를 고유하게 식별(구별)할 수 있는 1개 이상의 컬럼 집합
-- : 컬럼의 속성으로 지정하거나 (단, 1개의 컬럼일 때)
--   [constraint pk_name] primary key (컬럼이름) 형식으로 지정

-- option 종류
/*
1. NULL OR NOT NULL : 데이터가 비어 있을 수 있는지 여부 지정
2. DEFAULT (값) : 값이 없으면 자동으로 사용할 값 지정    
*/

-- current_timestamp(), now() 는 현재 시간을 반환하는 함수
SELECT current_timestamp(), now();


-- 2. members 테이블 만들기 ( DDL )
CREATE TABLE `members` 
(
	`memid` VARCHAR(9) NOT NULL,
	`memname` VARCHAR(20) NOT NULL,
	`passwd` VARCHAR(128) NULL,
	`passwdmdt` DATETIME NULL,
	`jumin` VARCHAR(64) NULL,
	`addr` VARCHAR(100) NULL,
	`birthday` DATE NULL,
	`jobcd` CHAR(1) NULL,
	`mileage` DECIMAL(7,0) NULL DEFAULT '0',
	`stat` ENUM('Y','N') NOT NULL DEFAULT 'Y',
	`enterdtm` DATETIME NOT NULL DEFAULT current_timestamp(),
	`leavedtm` DATETIME NULL,
    PRIMARY KEY (`memid`)
);

-- 3. members 테이블 제거 ( DDL )
-- DROP TABLE members;

-- 4. goodsinfo 테이블 만들기 ( DDL )
CREATE TABLE `goodsinfo` (
	`goodscd` CHAR(5) NOT NULL,
	`goodsname` VARCHAR(20) NOT NULL,
	`unitcd` CHAR(2) NULL,
	`unitprice` DECIMAL(5,0) NOT NULL DEFAULT '0',
	`stat` ENUM('Y','N') NOT NULL DEFAULT 'Y',
	`insdtm` DATETIME NOT NULL DEFAULT current_timestamp(),
	`moddtm` DATETIME NULL,
	PRIMARY KEY (`goodscd`)
);

-- 5. order_h 테이블 만들기 ( DDL )
CREATE TABLE `order_h` (
	`orderno` CHAR(9) PRIMARY KEY, -- PRIMARY KEY : 자동으로 NOT NULL
	`orddt` DATE NOT NULL,
	`memid` VARCHAR(9) NOT NULL,
	`ordamt` DECIMAL(7,0) NOT NULL DEFAULT '0',
	`cancelyn` ENUM('Y','N') NOT NULL DEFAULT 'N',
	`canceldtm` DATETIME NULL,
	`insdtm` DATETIME NOT NULL DEFAULT current_timestamp(),
	`moddtm` DATETIME NULL
);

-- 6. order_d 테이블 만들기 ( DDL )

CREATE TABLE `order_d` (
	`orderno` CHAR(9) NOT NULL,
	`goodscd` CHAR(5) NOT NULL,
	`unitcd` CHAR(2) NULL,
	`unitprice` DECIMAL(5,0) NOT NULL DEFAULT '0',
	`qty` DECIMAL(3,0) NOT NULL DEFAULT '0',
	`amt` DECIMAL(7,0) NOT NULL DEFAULT '0',
	`insdtm` DATETIME NOT NULL DEFAULT current_timestamp(),
	`moddtm` DATETIME NULL,
	PRIMARY KEY (`orderno`, `goodscd`) -- 두 개의 컬럼으로 PK 만들기 : 복합 PK
);

-- 테이블에 대해 설명하는 명령
DESCRIBE order_d;
DESC order_d;