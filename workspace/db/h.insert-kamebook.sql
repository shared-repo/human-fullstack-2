-- 1. 데이터베이스 목록 조회
SHOW DATABASES;

-- 2. 작업 데이터베이스 변경
USE kamebook;

-- 3. 테이블 목록 조회
SHOW TABLES;

-- 4. 테이블 정보 조회
DESCRIBE members;
DESCRIBE goodsinfo;
DESCRIBE order_h;
DESCRIBE order_d;

-- 데이터 삽입 구문 형식 ( DML - Insert )
/*
INSERT INTO 테이블이름 (컬럼1, 컬럼2, ... )
VALUES              (값1,  값2 ... )
*/

-- 5. 데이터 삽입 ( DML )
INSERT  INTO `members` 
(`memid`, `memname`, `addr`, `birthday`, `jobcd`, `mileage`, `enterdtm`) 
VALUES
('hong1', '홍길동', '인천 동구 송림동', '2000-05-08', '2', 500, '2022-03-01 14:10:27');

INSERT  INTO `members` 
(`memid`, `memname`, `addr`, `birthday`, `jobcd`, `mileage`, `enterdtm`) 
VALUES
('hong2', '홍길동', '서울 강남구 신사동', '1990-01-05', '9', 1000,  '2022-03-01 14:11:50');

INSERT  INTO `members` 
(`memid`, `memname`, `addr`, `birthday`, `jobcd`, `enterdtm`) 
VALUES
('kim1', '김갑수', '인천 연수구 연수동', '2003-07-01', '1', '2022-03-01 14:12:39');

INSERT  INTO `members` 
(`memid`, `memname`, `addr`, `birthday`, `jobcd`, `enterdtm`) 
VALUES
('park', '박기자', '경기 부천시', '2002-09-30', '3', '2022-03-01 14:13:16');
 
INSERT  INTO `members` 
(`memid`, `memname`, `addr`, `birthday`, `jobcd`, `enterdtm`) 
VALUES
('seo', '서갑돌',  '인천 동구', '1998-03-10', '1', '2022-03-01 14:08:41');

INSERT  INTO `members` 
(`memid`, `memname`, `addr`, `birthday`, `jobcd`, `enterdtm`) 
VALUES
('Taeh', '태현', '경기 수원시', '2002-10-15', '4', '2022-03-01 14:15:10');

-- 6. goodsinfo 테이블에 데이터 삽입 ( DML )
INSERT INTO `kamebook`.`goodsinfo` 
(`goodscd`, `goodsname`, `unitcd`, `unitprice`, `insdtm`) 
VALUES ('GDS01', '노트', '01', '2000', '2022-03-01 14:42:44');

INSERT INTO `kamebook`.`goodsinfo` 
(`goodscd`, `goodsname`, `unitcd`, `unitprice`, `insdtm`) 
VALUES ('GDS02', '연필', '02', '100', '2022-03-01 14:43:17');

INSERT INTO `kamebook`.`goodsinfo` 
(`goodscd`, `goodsname`, `unitcd`, `unitprice`, `insdtm`) 
VALUES ('GDS03', '복사지', '03', '5000', '2022-03-01 14:43:47');

INSERT INTO `kamebook`.`goodsinfo` 
(`goodscd`, `goodsname`, `unitcd`, `unitprice`, `insdtm`) 
VALUES ('GDS04', '볼펜', '02', '500', '2022-03-01 14:44:13');

INSERT INTO `kamebook`.`goodsinfo` 
(`goodscd`, `goodsname`, `unitcd`, `unitprice`, `insdtm`) 
VALUES ('GDS05', '네임펜', '02', '1000', '2022-03-01 14:44:30');

INSERT INTO kamebook.goodsinfo 
(`goodscd`, `goodsname`, `unitcd`, `unitprice`, `insdtm`) 
VALUES ('GDS06', '크레파스', '02', '1500', '2022-03-01 14:45:30');

-- 7. order_h 테이블에 데이터 삽입 ( DML )
INSERT INTO `kamebook`.`order_h` 
(`orderno`, `orddt`, `memid`, `ordamt`, `insdtm`) 
VALUES
('202201001', '2022-01-24', 'seo', '10000', '2022-03-01 14:49:07');

INSERT INTO `kamebook`.`order_h` 
(`orderno`, `orddt`, `memid`, `ordamt`, `insdtm`) 
VALUES
('202201002', '2022-01-24', 'hong2', '15000', '2022-03-01 14:50:35');

INSERT INTO `kamebook`.`order_h` 
(`orderno`, `orddt`, `memid`, `ordamt`, `insdtm`) 
VALUES
('202201003', '2022-01-25', 'hong1', '20000', '2022-03-01 14:51:19');

INSERT INTO `kamebook`.`order_h` 
(`orderno`, `orddt`, `memid`, `ordamt`, `insdtm`) 
VALUES
('202201004', '2022-01-25', 'kim1', '10000', '2022-03-01 14:51:58');

INSERT INTO `kamebook`.`order_h` 
(`orderno`, `orddt`, `memid`, `ordamt`, `cancelyn`, `canceldtm`, `insdtm`) 
VALUES
('202201005', '2022-01-25', 'park', '5000', 'Y', '2022-01-25 00:00:00', '2022-03-01 14:53:12');

INSERT INTO `kamebook`.`order_h` 
(`orderno`, `orddt`, `memid`, `ordamt`, `insdtm`) 
VALUES
('202202001', '2022-02-01', 'hong1', '30000', '2022-03-01 14:54:09');

INSERT INTO `kamebook`.`order_h` 
(`orderno`, `orddt`, `memid`, `ordamt`, `insdtm`) 
VALUES
('202202002', '2022-02-01', 'hong1', '1000', '2022-03-01 14:54:40');

INSERT INTO `kamebook`.`order_h` 
(`orderno`, `orddt`, `memid`, `ordamt`, `insdtm`) 
VALUES
('202202003', '2022-02-02', 'park', '10000', '2022-03-01 14:55:28');

INSERT INTO `kamebook`.`order_h` 
(`orderno`, `orddt`, `memid`, `ordamt`, `insdtm`) 
VALUES
('202202004', '2022-02-02', 'abcd', '500', '2022-03-01 14:56:03');

-- 8. order_d 테이블에 데이터 삽입 ( DML )
INSERT INTO `kamebook`.`order_d` 
     (`orderno`, `goodscd`, `unitcd`, `unitprice`, `qty`, `amt`, `insdtm`) VALUES
     ('202201001', 'GDS01', '01', '2000', '10', '20000', '2022-03-01 15:10:39');

INSERT INTO `kamebook`.`order_d` 
    (`orderno`, `goodscd`, `unitcd`, `unitprice`, `qty`, `amt`, `insdtm`) VALUES
    ('202201001', 'GDS02', '02', '100', '50', '5000', '2022-03-01 15:11:39');

INSERT INTO `kamebook`.`order_d` 
    (`orderno`, `goodscd`, `unitcd`, `unitprice`, `qty`, `amt`, `insdtm`) VALUES
    ('202201001', 'GDS03', '03', '5000', '1', '5000', '2022-03-01 15:12:23');

INSERT INTO `kamebook`.`order_d` 
    (`orderno`, `goodscd`, `unitcd`, `unitprice`, `qty`, `amt`, `insdtm`) VALUES
    ('202201002', 'GDS01', '01', '1000', '5', '5000', '2022-03-01 15:13:28');

INSERT INTO `kamebook`.`order_d` 
    (`orderno`, `goodscd`, `unitcd`, `unitprice`, `qty`, `amt`, `insdtm`) VALUES
    ('202201002', 'GDS03', '03', '5000', '10', '50000', '2022-03-01 15:14:26');

INSERT INTO `kamebook`.`order_d` 
    (`orderno`, `goodscd`, `unitcd`, `unitprice`, `qty`, `amt`, `insdtm`) VALUES
    ('202201003', 'GDS04', '02', '500', '50', '25000', '2022-03-01 15:15:12');

INSERT INTO `kamebook`.`order_d` 
    (`orderno`, `goodscd`, `unitcd`, `unitprice`, `qty`, `amt`, `insdtm`) VALUES
    ('202201004', 'GDS05', '02', '1000', '10', '10000', '2022-03-01 15:15:59');

INSERT INTO `kamebook`.`order_d` 
    (`orderno`, `goodscd`, `unitcd`, `unitprice`, `qty`, `amt`, `insdtm`) VALUES
    ('202201005', 'GDS02', '02', '100', '50', '5000', '2022-03-01 15:16:45');

INSERT INTO `kamebook`.`order_d` 
    (`orderno`, `goodscd`, `unitcd`, `unitprice`, `qty`, `amt`, `insdtm`) VALUES
    ('202201005', 'GDS03', '03', '5000', '4', '20000', '2022-03-01 15:17:30');

INSERT INTO `kamebook`.`order_d` 
    (`orderno`, `goodscd`, `unitcd`, `unitprice`, `qty`, `amt`, `insdtm`) VALUES
    ('202201006', 'GDS01', '01', '2000', '1', '2000', '2022-03-01 15:18:08');

INSERT INTO `kamebook`.`order_d` 
    (`orderno`, `goodscd`, `unitcd`, `unitprice`, `qty`, `amt`, `insdtm`) VALUES
    ('202202001', 'GDS01', '01', '2000', '10', '20000', '2022-03-01 15:18:59');

INSERT INTO `kamebook`.`order_d` 
    (`orderno`, `goodscd`, `unitcd`, `unitprice`, `qty`, `amt`, `insdtm`) 
    VALUES
    ('202202001', 'GDS03', '03', '5000', '1', '5000', '2022-03-01 15:19:10');

INSERT INTO `kamebook`.`order_d` 
    (`orderno`, `goodscd`, `unitcd`, `unitprice`, `qty`, `amt`, `insdtm`) 
    VALUES
    ('202202001', 'GDS05', '02', '1000', '20', '20000', '2022-03-01 15:19:20');

INSERT INTO `kamebook`.`order_d` 
    (`orderno`, `goodscd`, `unitcd`, `unitprice`, `qty`, `amt`, `insdtm`) VALUES
    ('202202003', 'GDS01', '01', '2000', '10', '20000', '2022-03-01 15:19:30');

INSERT INTO `kamebook`.`order_d` 
    (`orderno`, `goodscd`, `unitcd`, `unitprice`, `qty`, `amt`, `insdtm`) 
    VALUES
    ('202202003', 'GDS07', '03', '2000', '20', '40000', '2022-03-01 15:20:30');

INSERT INTO `kamebook`.`order_d` 
(`orderno`, `goodscd`, `unitcd`, `unitprice`, `qty`, `amt`, `insdtm`) 
VALUES
('202202004', 'GDS01', '01', '2000', '10', '20000', '2022-03-01 15:21:18');