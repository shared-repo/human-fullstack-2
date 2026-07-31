-- 1. 데이터베이스 만들기 ( root 계정으로 실행 )
CREATE DATABASE labdb
	CHARACTER SET utf8mb4
  	COLLATE utf8mb4_general_ci;
  	
-- 2. 권한 부여 ( root 계정으로 실행 )
GRANT ALL PRIVILEGES ON labdb.* TO human@localhost;
GRANT ALL PRIVILEGES ON labdb.* TO human@"%";

FLUSH PRIVILEGES;

-- 3. 작업 데이터베이스 변경 ( human 계정으로 실행 )
USE labdb;

-- 4. 테이블 만들기 ( human 계정으로 실행 )
/*
이름 : tbl_board
컬럼 : 
   boardno 정수형, 필수, 기본키, 자동증가속성
	writer 최대 20개의 가변 문자형, 필수
	title 최대 100개의 가변 문자형, 필수
	content 최대 2000개의 가변 문자형, 필수
	writedate 년월일시분초 표시 자료형, 선택, 기본값 = 현재시간
	modifydate 년월일시분초 표시 자료형, 선택, 기본값 = 현재시간
	readcount 정수형, 선택, 기본값 = 0
	deleted 진위형, 선택, 기본값 = false

이름 : tbl_comment
컬럼 : 
   commentno 정수형, 필수, 기본키, 자동증가속성
   boardno 정수형, 필수, 외래키
	writer 최대 20개의 가변 문자형, 필수	
	content 최대 500개의 가변 문자형, 필수
	writedate 년월일시분초 표시 자료형, 선택, 기본값 = 현재시간
	modifydate 년월일시분초 표시 자료형, 선택, 기본값 = 현재시간
*/

-- CREATE TABLE IF NOT EXISTS tbl_board

CREATE TABLE tbl_board
(
	boardno INT PRIMARY KEY AUTO_INCREMENT, -- 컬럼에 직접 PK 지정
   writer VARCHAR(20) NOT NULL,
	title VARCHAR(100) NOT NULL,
   content VARCHAR(2000) NOT NULL,
   writedate DATETIME NULL DEFAULT NOW(), -- NOW() == CURRENT_TIMESTAMP()
   modifydate DATETIME NULL DEFAULT CURRENT_TIMESTAMP(),
   readcount INT NULL DEFAULT (0),
   deleted BOOLEAN NULL DEFAULT (FALSE)
);

CREATE TABLE tbl_comment
(
	commentno INT AUTO_INCREMENT,
	boardno INT NOT NULL,
   writer VARCHAR(20) NOT NULL,
   content VARCHAR(500) NOT NULL,
   writedate DATETIME NULL DEFAULT NOW(),
   modifydate DATETIME NULL DEFAULT CURRENT_TIMESTAMP(),
   -- PRIMARY KEY (commentno),
   CONSTRAINT pk_tbl_comment PRIMARY KEY (commentno), -- PK에 이름붙이기
	CONSTRAINT fk_comment_to_board foreign key (boardno) references tbl_board (boardno)
);



