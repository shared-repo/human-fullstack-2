-- 작업 데이터베이스 변경
USE shopdb;

-- 1. category 테이블에 데이터 삽입
SELECT * FROM category ORDER BY category_id;

INSERT INTO category (category_id, category_name)
VALUES (11, '문구/오피스'); -- category_id = 11이 이미 있다면 실패

SELECT * FROM category ORDER BY category_id;


-- 2. category 테이블에 데이터 삽입
-- category_id를 생략하면 자동으로 12가 입력됨 -- auto_increment
DESC category; -- category_id 컬럼의 auto_increment 속성 확인

INSERT INTO category (category_name)
VALUES ('여행/레저');

SELECT * FROM category ORDER BY category_id;

-- 마지막으로 발급된 자동증가값 조회
SELECT LAST_INSERT_ID();

-- stock 기본값(0), created_at 기본값(NOW()) 활용
INSERT INTO product (category_id, product_name, price)
VALUES (11, '볼펜 10자루 세트', 5900);

SELECT * FROM product;

-- 여러 건의 데이터를 한 번에 저장
INSERT INTO category (category_name)
VALUES
    ('반려식물'),
    ('자동차용품'),
    ('악기');
    
SELECT * FROM category ORDER BY category_id;

-- 데이터 복사를 위해 product 테이블과 유사한 구조의 테이블  만들기
CREATE TABLE product_premium (
    product_id    INT            NOT NULL,
    product_name  VARCHAR(100)   NOT NULL,
    price         DECIMAL(10,0)  NOT NULL,
    copied_at     DATETIME       DEFAULT NOW()
);

-- product 테이블의 데이터를 조회해서 product_premium 테이블에 삽입
INSERT INTO product_premium (product_id, product_name, price)
SELECT product_id, product_name, price
FROM product
WHERE price >= 30000;

SELECT * FROM product_premium;

-- 없으면 삽입, 있으면 수정
INSERT INTO category (category_id, category_name)
VALUES (1, '전자제품')
ON DUPLICATE KEY UPDATE category_name = '전자제품';

SELECT * FROM category ORDER BY category_id;

-- root 계정으로 실행
-- 작업 데이터베이스 변경
USE shopdb;

-- 파일 데이터를 읽어서 테이블에 삽입
LOAD DATA INFILE 'D:/instructor-och/human-fullstack-2/workspace/db/new_products.csv'
INTO TABLE product
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','          -- 컬럼 구분자
OPTIONALLY ENCLOSED BY '"'        -- 문자열 감싸는 기호 (선택)
LINES TERMINATED BY '\n'          -- 행 구분자
IGNORE 1 LINES                    -- 첫 줄(헤더) 무시
(product_id, category_id, product_name, price, stock);

SELECT * FROM product;

-- ---------------------------------------------------------------
-- 여기부터 insert 구문

-- 무선 마우스(product_id=1)의 재고를 60으로 수정
SELECT * FROM product;

UPDATE product
SET stock = 60
WHERE product_id = 1;

SELECT * FROM product;

-- 블루투스 키보드(product_id=3) 가격과 재고를 동시에 수정
SELECT * FROM product WHERE product_id = 3;

UPDATE product
SET price = 49000,
    stock = 35
WHERE product_id = 3;

SELECT * FROM product WHERE product_id = 3;

-- 모든 제품의 가격을 5% 인상
SELECT * FROM product;

START TRANSACTION;
UPDATE product SET price = ROUND(price * 1.05);

SELECT * FROM product;

ROLLBACK; -- START TRANSACTION 이후 발생한 모든 변경 사항을 취소
SELECT * FROM product;

-- 전자기기(category_id=1) 전체 가격을 5% 인상
UPDATE product
SET price = ROUND(price * 1.05)
WHERE category_id = 1;

SELECT * FROM product;

-- 재고가 가장 적은 상품 2개의 재고를 0으로 초기화
UPDATE product
SET stock = 0
ORDER BY stock ASC
LIMIT 2;

SELECT * FROM product ORDER BY stock ASC;

-- --------------------------------------
-- 여기부터 delete
USE shopdb;

SELECT * FROM product;

-- product_id가 11인 상품 삭제
DELETE FROM product
WHERE product_id = 11;

SELECT * FROM product;

-- 재고가 0인 상품 모두 삭제
DELETE FROM product
WHERE stock = 0; -- 실패 : order_detail 테이블에서 삭제 대상 데이터를 참조

SELECT * FROM order_detail;

-- 가장 오래된 주문 3건 삭제
DELETE FROM orders
ORDER BY order_date ASC
LIMIT 3; -- 실패 : order_detail 테이블에서 삭제 대상 데이터를 참조

SELECT * FROM orders ORDER BY order_date ASC LIMIT 3;
SELECT * FROM order_detail WHERE order_id IN (1, 2, 3);


-- product_premium 테이블의 모든 데이터 초기화
SELECT * FROM product_premium;

TRUNCATE TABLE product_premium;

SELECT * FROM product_premium;