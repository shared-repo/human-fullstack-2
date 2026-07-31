
-- 작업 데이터베이스 변경
USE shopdb;


-- 1. category 테이블 만들기

-- not null : 필수 입력 항목
-- null : 선택 입력 항목
-- auto_increment : 값을 입력하지 않아도 자동으로 다음 번호를 저장

CREATE TABLE category (
    category_id    INT             NOT NULL AUTO_INCREMENT,
    category_name  VARCHAR(50)     NOT NULL,
    
    PRIMARY KEY (category_id),
    UNIQUE KEY uq_category_name (category_name) -- 중복되지 않는 값만 저장 가능
);

-- 테이블 목록 조회
SHOW TABLES;

-- 2. product 테이블 만들기

-- default : 행을 저장할 때 컬럼의 값이 지정되지 않으면 사용할 값 설정
-- now() : 현재 시간을 반환하는 함수  (년-월-일 시:분:초)

CREATE TABLE product (
    product_id    INT              NOT NULL AUTO_INCREMENT,
    category_id   INT              NOT NULL,
    product_name  VARCHAR(100)     NOT NULL,
    price         DECIMAL(10, 0)   NOT NULL,
    stock         INT              NOT NULL DEFAULT 0,
    description   TEXT,
    created_at    DATETIME         DEFAULT NOW(),
    
    PRIMARY KEY (product_id),
    FOREIGN KEY (category_id) REFERENCES category (category_id)
);

-- 3. customer 테이블 만들기

CREATE TABLE customer (
    customer_id  INT           NOT NULL AUTO_INCREMENT,
    name         VARCHAR(50)   NOT NULL,
    email        VARCHAR(100)  NOT NULL,
    password     VARCHAR(255)  NOT NULL,
    phone        CHAR(13),
    address      VARCHAR(200),
    created_at   DATETIME      DEFAULT NOW(),
    PRIMARY KEY (customer_id),
    UNIQUE KEY uq_customer_email (email)
);

-- 4. orders 테이블 만들기

-- 데이터베이스에서 문자열은 작은 따옴표로 표시

CREATE TABLE orders (
    order_id          INT           NOT NULL AUTO_INCREMENT,
    customer_id       INT           NOT NULL,
    order_date        DATETIME      DEFAULT NOW(),
    shipping_address  VARCHAR(200)  NOT NULL,
    payment_method    VARCHAR(20)   NOT NULL,
    status            VARCHAR(20)   DEFAULT '주문완료',
    PRIMARY KEY (order_id),
    FOREIGN KEY (customer_id) REFERENCES customer (customer_id)
);

-- 5. order_detail 테이블 만들기
CREATE TABLE order_detail (
    detail_id   INT              NOT NULL AUTO_INCREMENT,
    order_id    INT              NOT NULL,
    product_id  INT              NOT NULL,
    quantity    INT              NOT NULL,
    unit_price  DECIMAL(10, 0)   NOT NULL,
    PRIMARY KEY (detail_id),
    FOREIGN KEY (order_id)   REFERENCES orders  (order_id),
    FOREIGN KEY (product_id) REFERENCES product (product_id)
);

-- 테이블 목록 조회
SHOW TABLES;

-- ------------------------------------------------
-- 여기부터 alter 구문

-- 작업 데이터베이스 변경
USE shopdb;

-- customer 테이블에 컬럼 추가
DESC customer; -- 테이블 구조 확인

ALTER TABLE customer
   ADD COLUMN point INT NOT NULL DEFAULT 0 AFTER address;

DESC customer; -- 테이블 구조 확인

-- 여러 컬럼 추가
ALTER TABLE customer
    ADD COLUMN gender    CHAR(1)     AFTER name,
    ADD COLUMN birthdate DATE        AFTER gender;

DESC customer; -- 테이블 구조 확인


-- product 테이블의 description 컬럼을 TEXT에서 MEDIUMTEXT로 변경한다.
DESC product; -- 테이블 구조 확인

ALTER TABLE product
    MODIFY COLUMN description MEDIUMTEXT;

DESC product; -- 테이블 구조 확인

-- customer 테이블의 phone을 phone_number로 이름을 바꾸고 유형도 변경한다.
DESC customer; -- 테이블 구조 확인



ALTER TABLE customer
    CHANGE COLUMN phone phone_number VARCHAR(20);
    
    

DESC customer; -- 테이블 구조 확인


-- 컬럼 이름만  변경
ALTER TABLE customer
    RENAME COLUMN phone_number TO phone;

DESC customer; -- 테이블 구조 확인


-- customer 테이블에서 gender 컬럼을 삭제한다.

ALTER TABLE customer
    DROP COLUMN gender;
    
DESC customer; -- 테이블 구조 확인

-- 테이블 이름 변경 1
RENAME TABLE product_premium TO product_high_price;

SHOW TABLES; -- 테이블 목록 표시 - 위의 변경 확인

-- 테이블 이름 변경 2
ALTER TABLE product_high_price RENAME TO product_premium;

SHOW TABLES; -- 테이블 목록 표시 - 위의 변경 확인

-- ----------------------------------------
-- 여기부터 제약 조건

-- 제약 조건 내용은 위의 CREATE TABLE 구문 참고

-- 가격은 0보다 커야 함
DESC product;

ALTER TABLE product
ADD CONSTRAINT chk_prodcut_price CHECK (price > 0);

-- 수량은 1 이상이어야 함
DESC order_detail;

ALTER TABLE order_detail
ADD CONSTRAINT chk_order_detail_quantity CHECK (quantity >= 1);

-- 제약 조건 조회
SELECT
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE,
    TABLE_NAME
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'shopdb' AND CONSTRAINT_TYPE = 'CHECK';


