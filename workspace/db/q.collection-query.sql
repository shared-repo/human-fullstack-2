-- 작업 데이터베이스 변경
USE shopdb;

-- 전자기기 상품과 도서 상품을 합쳐서 조회
SELECT product_name, price, '전자기기' AS 카테고리
FROM product
WHERE category_id = 1

UNION

SELECT product_name, price, '도서' AS 카테고리
FROM product
WHERE category_id = 3;

-- 1번, 2번 주문의 합과 2번,3번 주문의 합에  모두 포함된 상품
SELECT product_id FROM order_detail WHERE order_id IN (1, 2)
INTERSECT
SELECT product_id FROM order_detail WHERE order_id IN (2, 3);

-- 전체 고객 중 주문한 적 있는 고객을 제외한 고객 ID 목록
SELECT customer_id FROM customer
EXCEPT
SELECT DISTINCT customer_id FROM orders;