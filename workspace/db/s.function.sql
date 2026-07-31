-- 작업 데이터베이스 변경
USE shopdb;

-- 여러 문자열을 하나로 이어붙인다.
SELECT CONCAT('안녕', '하세요', '!');

SELECT
    CONCAT(name, ' <', email, '>') AS 고객정보
FROM customer;

SELECT CONCAT_WS(' / ', name, email, phone) AS 고객정보
FROM customer;

-- 문자열 길이
SELECT
    '홍길동'              AS 이름,
    LENGTH('홍길동')      AS 바이트수,
    CHAR_LENGTH('홍길동') AS 문자수;
    
-- 이메일 길이가 15자 이하인 고객 조회
SELECT name, email, CHAR_LENGTH(email) AS 이메일길이
FROM customer
WHERE CHAR_LENGTH(email) <= 15
ORDER BY 이메일길이;

-- 부분 문자열 조회
SELECT
    SUBSTRING('2024-01-15', 1, 4) AS 연도,
    SUBSTRING('2024-01-15', 6, 2) AS 월,
    SUBSTRING('2024-01-15', 9, 2) AS 일;
    
SELECT
    email,
    SUBSTRING_INDEX(email, '@', 1)  AS 아이디,
    SUBSTRING_INDEX(email, '@', -1) AS 도메인
FROM customer; 

-- 연락처에서 하이픈(-) 제거
SELECT name, phone, REPLACE(phone, '-', '') AS 하이픈제거
FROM customer;   

-- 상품 ID를 3자리로 맞추기 (001, 002 ...)
SELECT
	 product_id,
    LPAD(product_id, 3, '0') AS 상품코드,
    product_name
FROM product;

-- 년, 월, 일, 시, 분, 초 조회
SELECT
    order_date,
    YEAR(order_date)    AS 연도,
    MONTH(order_date)   AS 월,
    DAY(order_date)     AS 일,
    HOUR(order_date)    AS 시,
    MINUTE(order_date)  AS 분,
    SECOND(order_date)  AS 초
FROM orders
WHERE order_id = 1;

-- 월별 주문 건수 집계
SELECT
    YEAR(order_date)  AS 연도,
    MONTH(order_date) AS 월,
    COUNT(*)          AS 주문건수
FROM orders
GROUP BY YEAR(order_date), MONTH(order_date)
ORDER BY 연도, 월;

-- 오늘 기준 각 주문의 경과 일수
SELECT
    order_id,
    order_date,
    DATEDIFF(CURDATE(), order_date) AS 경과일수,
    status
FROM orders
ORDER BY 경과일수 DESC;

-- 주문일부터 현재까지 경과 개월 수
SELECT
    order_id,
    order_date,
    TIMESTAMPDIFF(WEEK, order_date, CURDATE()) AS 경과주,
    TIMESTAMPDIFF(MONTH, order_date, CURDATE()) AS 경과개월
FROM orders;

-- 날자 연산
SELECT
    '2024-01-15'                                  AS 주문일,
    DATE_ADD('2024-01-15', INTERVAL 30 DAY)       AS 30일후,
    DATE_ADD('2024-01-15', INTERVAL 3 MONTH)      AS 3개월후,
    DATE_SUB('2024-01-15', INTERVAL 7 DAY)        AS 7일전;
    
-- 주문일로부터 3일 후 예상 배송 완료일 계산
SELECT
    order_id,
    order_date,
    DATE_ADD(order_date, INTERVAL 3 DAY) AS 예상배송완료일
FROM orders;    

-- 날짜 표시 형식 지정
SELECT
    order_date,
    DATE_FORMAT(order_date, '%Y년 %m월 %d일')          AS 한국형식,
    DATE_FORMAT(order_date, '%Y/%m/%d %H:%i')          AS 날짜시간,
    DATE_FORMAT(order_date, '%Y년 %c월 %e일 (%W)')     AS 요일포함
FROM orders
WHERE order_id = 1;

-- 조건 활용한 조회
-- 재고 20개 미만이면 '재고부족', 이상이면 '재고충분'
SELECT
    product_name,
    stock,
    IF(stock < 20, '⚠ 재고부족', '재고충분') AS 재고상태
FROM product
ORDER BY stock;


-- 가격대별 상품 등급 분류
SELECT
    product_name,
    price,
    CASE
        WHEN price < 20000            THEN '저가'
        WHEN price BETWEEN 20000 AND 49999 THEN '중가'
        WHEN price >= 50000           THEN '고가'
    END AS 가격등급
FROM product
ORDER BY price;

-- 결제방법 한글 코드를 영문으로 변환
SELECT
    order_id,
    CASE payment_method
        WHEN '신용카드' THEN 'CARD'
        WHEN '계좌이체' THEN 'TRANSFER'
        WHEN '현금'    THEN 'CASH'
        ELSE 'OTHER'
    END AS payment_en
FROM orders;


-- 주문 상태별 건수를 한 행으로 출력
SELECT '전체건수', COUNT(*) 건수
FROM orders
UNION
SELECT `status`, COUNT(*) 건수
FROM orders
GROUP BY STATUS;

SELECT
    COUNT(*)                                      AS 전체,
    SUM(CASE WHEN status = '배송완료' THEN 1 ELSE 0 END) AS 배송완료,
    SUM(CASE WHEN status = '배송중'   THEN 1 ELSE 0 END) AS 배송중,
    SUM(CASE WHEN status = '주문완료' THEN 1 ELSE 0 END) AS 주문완료
FROM orders;






