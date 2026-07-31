-- 작업 데이터베이스 변경
USE shopdb;

-- select는 데이터를 조회하는 명령으로 독립적 사용 가능
SELECT NOW(), CURRENT_TIMESTAMP();

-- 테이블의 모든 데이터 조회
SELECT category_id, category_name
FROM category;

SELECT * /*  * : 모든 컬럼을 테이블을 만들 때 지정한 순서대로 사용   */
FROM category;

-- 컬럼을 지정해서 조회

DESC product; -- 컬럼이 7개인 것을 확인

SELECT product_name, price, stock
FROM product;

-- 조회 컬럼에 별칭 사용
SELECT
   product_name  AS 상품명,
   price         AS 판매가격,
   stock            재고수량 -- AS는 생략 가능
FROM product;

-- 컬럼명 대신 수식 사용
SELECT
   product_name           AS 상품명,
   price                  AS 판매가격,
   ROUND(price * 1.1)     AS 부가세포함가격,
   stock                  AS 재고수량,
   price * stock          AS 재고자산
FROM product;

-- 주문 실적이 있는 고객 찾기 ( 중복을 제거하고 조회 )
DESC orders;

SELECT DISTINCT customer_id 
FROM orders;

-- where : 조건에 맞는 데이터만 조회

SELECT product_name, price
FROM product
WHERE price >= 30000;

-- where + 논리곱(AND) 연산
SELECT product_name, price
FROM product
WHERE category_id = 1
  AND price >= 30000;

-- where + 논리합(OR) 연산
SELECT order_id, customer_id, status
FROM orders
WHERE status = '배송완료'
   OR status = '배송중';
   
-- where + 논리부정(NOT) 연산
SELECT product_name, category_id
FROM product
-- WHERE category_id <> 1;
WHERE NOT category_id = 1;

SELECT order_id, customer_id, status
FROM orders
WHERE NOT (status = '배송완료' OR STATUS = '배송중');

-- 범위를 조건으로하는 조회 ( 가격이 20,000원 이상 50,000원 이하인 상품 )
SELECT product_name, price
FROM product
-- WHERE price >= 20000 AND price <= 50000;
WHERE price BETWEEN 20000 AND 50000;

-- 범위를 조건으로하는 조회 ( 2024년 1분기(1~3월) 주문 조회 )
-- 날짜는 작은따옴표로 표시
SELECT order_id, order_date, status
FROM orders
WHERE order_date BETWEEN '2024-01-01' AND '2024-03-31';

-- 목록에 포함되는 데이터 조회 
-- ( 전자기기(1), 도서(3), 스포츠용품(4) 카테고리 상품 조회 )
SELECT product_name, category_id, price
FROM product
-- WHERE category_id = 1 OR category_id = 3 OR category_id = 4;
WHERE category_id IN (1, 3, 4);

-- 목록에 포함되지 않는 데이터 조회 
-- ( 전자기기(1), 도서(3), 스포츠용품(4) 카테고리 상품 조회 )
SELECT product_name, category_id, price
FROM product
-- WHERE category_id != 1 AND category_id != 3 AND category_id != 4;
WHERE category_id NOT IN (1, 3, 4);

-- 부분일치 검색 ( 상품명에 '마우스'가 포함된 상품 )
SELECT product_name, price
FROM product
WHERE product_name LIKE '%마우스%';

-- 부분일치 검색 ( 이메일이 'kim'으로 시작하는 고객 )
SELECT name, email
FROM customer
WHERE email LIKE 'kim%';

-- 부분일치 검색 ( 이름이 두 글자인 고객 - 드문 경우이나 연습용 )
SELECT name FROM customer WHERE name LIKE '__';

-- NULL 비교 ( 전화번호가 없는(NULL) 고객 조회 )
SELECT name, email, phone
FROM customer
-- WHERE phone = NULL; -- 비교 불가능 (항상 False)
WHERE phone IS NULL;

-- NULL 비교 ( 상품 설명이 있는 상품만 조회 )
SELECT product_name, description
FROM product
-- WHERE DESCRIPTION != NULL; -- 비교 불가능 ( 항상 False )
WHERE description IS NOT NULL;

-- 정렬 ( 가격 높은 순으로 상품 조회 )
SELECT product_name, price
FROM product
-- ORDER BY price; -- 정렬 방향을 표시하지 않으면 ASC로 정렬
-- ORDER BY price ASC;
ORDER BY price DESC;

-- 다중 정렬 ( 카테고리 오름차순, 같은 카테고리 안에서는 가격 내림차순 )
SELECT category_id, product_name, price
FROM product
ORDER BY category_id ASC, price DESC;


-- 행 개수 제한 ( 가장 비싼 상품 3개 )
SELECT product_name, price
FROM product
ORDER BY price DESC
-- LIMIT 3;
LIMIT 0, 3;

-- 집계함수 ( product 테이블 전체 통계 )
SELECT
    COUNT(*)          AS 전체상품수,
    SUM(price)        AS 가격합계,
    AVG(price)        AS 평균가격,
    MAX(price)        AS 최고가격,
    MIN(price)        AS 최저가격
FROM product;

-- Group By ( 카테고리별 상품 수와 평균 가격 )
SELECT
    category_id, -- group by에 지정된 컬럼만 select에 포함될 수 있습니다.
    COUNT(*)       AS 상품수,
    AVG(price)     AS 평균가격,
    MAX(price)     AS 최고가격
FROM product
GROUP BY category_id;

-- Group By + Order By ( 주문별 결제 금액 합산 (많은 순) )
SELECT
    order_id,
    SUM(quantity * unit_price) AS 총결제금액
FROM order_detail
GROUP BY order_id
ORDER BY 총결제금액 DESC;


-- -- 상품별 갯수가 2 이상인 카테고리만 조회
SELECT
    category_id,
    COUNT(*) AS 상품수
FROM product
-- WHERE COUNT(*) >= 2 -- 오류 : 아직 GROUP BY가 실행되지 않아서 집계 불가능
GROUP BY category_id
HAVING COUNT(*) >= 2;




