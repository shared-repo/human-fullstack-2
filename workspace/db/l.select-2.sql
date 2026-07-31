-- 1. 작업 데이터베이스 변경
USE shopdb;

-- 2-1. 상품 목록 조회 --> 카테고리 이름이 결과에 포함되지 않음
SELECT *
FROM product;

-- 2-2. 카테고리 목록 조회 --> 카테고리 아이디와 매핑된 카테고리 이름 있음
SELECT *
FROM category;

-- 2-3. 상품 테이블과 카테고리 테이블을 합쳐서 조회 필요
SELECT product_id, category.category_id, category_name, product_name, price, stock
FROM product, category
WHERE product.category_id = category.category_id;

SELECT product_id, c.category_id, category_name, product_name, price, stock
FROM product p, category c -- 테이블에 별칭 부여
WHERE p.category_id = c.category_id;

SELECT product_id, c.category_id, category_name, product_name, price, stock
FROM product p
INNER JOIN category c
ON p.category_id = c.category_id;

-- 3-1. 주문 정보 조회 --> 고객 이름을 알 수 없음
SELECT * 
FROM orders;

-- 3-2. 고객 정보 조회 --> 고객 아이디와 매핑된 고객 이름 확인
SELECT *
FROM customer;

-- 3-3. 고객이름과 고객별 주문 실적 조회
-- legacy join 구문은 outer join을 지원하지 않습니다.
SELECT c.`name`, COUNT(o.order_id) 주문건수
FROM customer c, orders o
WHERE c.customer_id = o.customer_id
GROUP BY c.name
ORDER BY c.name;

SELECT c.`name`, COUNT(o.order_id) 주문건수
FROM customer c
-- LEFT OUTER JOIN orders o -- customer 테이블의 데이터는 모두 포함
LEFT JOIN orders o
ON c.customer_id = o.customer_id
GROUP BY c.name
ORDER BY c.name;

-- 4. 주문 실적이 없는 고객 조회
SELECT c.name, c.email, o.order_id
FROM customer c
LEFT JOIN orders o 
ON c.customer_id = o.customer_id
WHERE o.order_id IS NULL;

-- 5. 세 개 이상의 테이블 조인 ( 주문번호, 고객명, 상품명, 수량, 단가 조회 )
SELECT 
	o.order_id 주문번호,
	c.`name` 고객명,
	p.product_name 상품명,
	od.quantity 수량,
	od.unit_price 단가,
	od.quantity * od.unit_price 소계
FROM orders o, customer c, product p, order_detail od
WHERE o.customer_id = c.customer_id 
		AND 
		o.order_id = od.order_id
		AND 
		od.product_id = p.product_id
ORDER BY c.name, o.order_id;

SELECT
    o.order_id,
    c.name           AS 고객명,
    p.product_name   AS 상품명,
    d.quantity       AS 수량,
    d.unit_price     AS 단가,
    d.quantity * d.unit_price AS 소계
FROM orders o
JOIN customer     c ON o.customer_id = c.customer_id
JOIN order_detail d ON o.order_id    = d.order_id
JOIN product      p ON d.product_id  = p.product_id
ORDER BY c.name, o.order_id;

-- 5. 모든 카테고리와 상품의 조합
SELECT c.category_name, p.product_name
FROM category c, product p
LIMIT 5, 10;

SELECT c.category_name, p.product_name
FROM category c
CROSS JOIN product p
LIMIT 5, 10;








