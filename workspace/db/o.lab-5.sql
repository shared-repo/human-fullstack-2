-- 1. 작업 데이터베이스 변경
USE labdb;

-- 고객별 (고객이름 같이 조회) 구매액 합계
SELECT c.custid, c.name, SUM(o.saleprice) 구매액합계
FROM customer c, orders o
WHERE c.custid = o.custid
GROUP BY c.custid, c.name;

SELECT c.custid, c.name, COALESCE(SUM(o.saleprice), 0) 구매액합계
FROM customer c
LEFT OUTER JOIN orders o
ON c.custid = o.custid
GROUP BY c.custid, c.name;

-- 고객아이디, 고객이름, 도서명, 주문 정보 ( customer, book, orders )
SELECT c.custid, c.name, b.bookname, o.saleprice, o.orderdate
FROM customer c, book b, orders o
WHERE c.custid = o.custid AND b.bookid = o.bookid;

SELECT c.custid, c.name, b.bookname, o.saleprice, o.orderdate
FROM customer c
INNER JOIN orders o
ON c.custid = o.custid
INNER JOIN book b
ON b.bookid = o.bookid;

-- 박지성이 구매한 도서의 출판사 수 ( customer, orders, book )
-- 집계함수에서 DISTINCT 사용 : 중복을 제거하고 집계
SELECT c.name, COUNT(DISTINCT b.publisher) 
FROM customer c, book b, orders o
WHERE c.custid = o.custid AND b.bookid = o.bookid AND c.name = '박지성'
GROUP BY c.name;

SELECT c.name, COUNT(DISTINCT b.publisher)
FROM customer c
INNER JOIN orders o
ON c.custid = o.custid
INNER JOIN book b
ON b.bookid = o.bookid
WHERE c.name = '박지성'
GROUP BY c.name;

-- 박지성이 구매한 도서의 이름, 가격, 정가와 판매가격의 차이 ( customer, orders, book )
SELECT b.bookname, b.price, b.price - o.saleprice 할인액
FROM customer c, book b, orders o
WHERE c.custid = o.custid AND b.bookid = o.bookid AND c.name = '박지성';

SELECT b.bookname, b.price, b.price - o.saleprice 할인액
FROM customer c
INNER JOIN orders o
ON c.custid = o.custid
INNER JOIN book b
ON b.bookid = o.bookid
WHERE c.name = '박지성';

-- 박지성이 구매하지 않은 도서의 이름 ( customer, orders, book )
SELECT DISTINCT b.*
FROM book b
LEFT OUTER JOIN orders o
ON b.bookid = o.bookid
LEFT OUTER JOIN customer c
ON c.custid = o.custid
WHERE c.name != '박지성' or c.custid is null;

-- 고객의 이름과 고객별 구매액 합계( customer, orders )
SELECT c.name, COALESCE(SUM(o.saleprice), 0) 구매액합계
FROM customer c, orders o
WHERE c.custid = o.custid
GROUP BY c.name;