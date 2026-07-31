-- 1. 작업 데이터베이스 변경
use kamebook;

-- 2. 주문번호, 회윈아이디, 회원이름, 주소, 주문일자, 주문수량 조회
SELECT oh.orderno, oh.memid, m.memname, m.addr, oh.orddt, oh.ordamt
FROM members m, order_h oh
WHERE m.memid = oh.memid;

SELECT oh.orderno, oh.memid, m.memname, m.addr, oh.orddt, oh.ordamt
FROM members m
INNER JOIN order_h oh
ON m.memid = oh.memid;

-- 3. 주문번호, 상품코드, 상품명, 가격, 수량 조회
SELECT od.orderno, od.goodscd, gi.goodsname, od.unitprice, od.qty
FROM order_d od, goodsinfo gi
WHERE od.goodscd = gi.goodscd
ORDER BY od.orderno;

SELECT od.orderno, od.goodscd, gi.goodsname, od.unitprice, od.qty
FROM order_d od
INNER JOIN goodsinfo gi
ON od.goodscd = gi.goodscd
ORDER BY od.orderno;

-- 4. 주문번호, 주문일자, 상품코드, 가격, 수량, 취소여부 조회
SELECT oh.orderno, oh.orddt, od.goodscd, od.unitprice, od.qty, oh.cancelyn
FROM order_h oh, order_d od
WHERE oh.orderno = od.orderno
ORDER BY oh.orderno;

SELECT oh.orderno, oh.orddt, od.goodscd, od.unitprice, od.qty, oh.cancelyn
FROM order_h oh
INNER JOIN order_d od
ON oh.orderno = od.orderno
ORDER BY oh.orderno;

-- 5. 주문별 주문 상품 종류의 갯수 조회
SELECT oh.orderno, COUNT(*) 상품종류갯수
FROM order_h oh, order_d od
WHERE oh.orderno = od.orderno
GROUP BY oh.orderno
ORDER BY oh.orderno;

SELECT oh.orderno, COUNT(*) 상품종류갯수
FROM order_h oh
INNER JOIN order_d od
ON oh.orderno = od.orderno
GROUP BY oh.orderno
ORDER BY oh.orderno;

-- 6. 사용자아이디(members), 사용자이름(members), 사용자별 주문 총액(order_d), 평균 주문액(order_d) 조회
--      직접 관계가 없는 테이블을 관계 있는 다른 테이블을 경유해서 조회 -> 3개 이상의 테이블 Join
SELECT m.memid, m.memname, SUM(od.unitprice*od.qty) 주문총액, AVG(od.unitprice*od.qty) 평균주문액
FROM members m, order_h oh, order_d od
WHERE m.memid = oh.memid AND oh.orderno = od.orderno
GROUP BY m.memid, m.memname
ORDER BY m.memid;

SELECT m.memid, m.memname, SUM(od.unitprice*od.qty) 주문총액, AVG(od.unitprice*od.qty) 평균주문액
FROM members m
INNER JOIN order_h oh 
ON m.memid = oh.memid
INNER JOIN order_d od
ON oh.orderno = od.orderno
GROUP BY m.memid, m.memname
ORDER BY m.memid;

-- 7. 사용자아이디, 사용자이름, 사용자별 주문 총액, 평균 주문액 조회
--      members, order_h 테이블 사용

SELECT 
	m.memid, m.memname, 
   IFNULL(SUM(oh.ordamt), 0) 주문총액, -- IFNULL : NULL을 다른 값으로 변경하는 함수
   IFNULL(AVG(oh.ordamt), 0) 평균주문액,
   COALESCE(SUM(oh.ordamt), 0) 주문총액2, -- COALESCE : 목록 중에서 첫 번째 NULL이 아닌 값 선택
   COALESCE(AVG(oh.ordamt), 0) 평균주문액2
FROM members m
LEFT OUTER JOIN order_h oh
ON m.memid = oh.memid
GROUP BY m.memid, m.memname
ORDER BY SUM(oh.ordamt) DESC;