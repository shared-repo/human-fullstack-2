-- 작업 데이터베이스 변경
USE shopdb;

-- 평균 가격보다 비싼 상품 조회
SELECT AVG(price) -- 평균 가격 조회 -> 44700
FROM product;

SELECT *
FROM product
WHERE price > 44700; -- 위에서 조회한 평균 가격을 사용해서 조건 구성

SELECT *
FROM product
WHERE price > ( SELECT AVG(price) FROM product );


-- '배송완료' 상태 주문을 한 고객의 이름과 이메일 조회
SELECT c.`name`, c.email, o.status
FROM customer c
INNER JOIN orders o
ON c.customer_id = o.customer_id
WHERE o.`status` = '배송완료';


SELECT c.`name`, c.email
FROM customer c
WHERE customer_id IN ( SELECT DISTINCT customer_id 
							  FROM orders o 
							  WHERE o.status = '배송완료' );
							  

-- 주문별 결제금액을 먼저 집계한 뒤, 
-- 그 결과에서 5만원 이상인 주문만 조회

SELECT t.*
FROM ( SELECT 
			order_id,
			SUM(unit_price * quantity) AS amount
		 FROM order_detail
		 GROUP BY order_id ) t
WHERE t.amount > 50000;

-- 작업 데이터베이스 변경
USE kamebook;

-- 생년월일이 2번째로 빠른 직원부터 3명 생년월일 기준 역순 정렬해서 조회
SELECT t.*
FROM (
	SELECT * 
	FROM members
	ORDER BY birthday
	LIMIT 1,3 -- 1번째 부터 3개만 조회 (순서는 0부터 시작)
) t
ORDER BY t.birthday DESC;

-- 작업 데이터베이스 변경
USE shopdb;

-- 각 상품의 가격과 전체 평균 가격 차이 조회

SELECT
    product_name,
    price,
    (SELECT AVG(price) FROM product)       AS 평균가격,
    price - (SELECT AVG(price) FROM product) AS 평균과의차이
FROM product
ORDER BY 평균과의차이 DESC;

-- 주문 이력이 있는 고객만 조회 (EXISTS)
SELECT name, email
FROM customer c
WHERE EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.customer_id = c.customer_id -- 서브쿼리에서 메인쿼리의 컬럼을 사용하는 경우 : 상관부속질의
);

-- 주문 이력이 없는 고객 조회  (NOT EXISTS)
SELECT name, email
FROM customer c
WHERE NOT EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.customer_id = c.customer_id -- 서브쿼리에서 메인쿼리의 컬럼을 사용하는 경우 : 상관부속질의
);










