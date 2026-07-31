-- 1. 작업 데이터베이스 변경
USE kamebook;

-- 2. 주문 총액이 10000원 초과 주문의 고객 조회
select distinct m.*
from order_h oh, members m
where oh.memid = m.memid and oh.ordamt > 10000;

select distinct memid from order_h where ordamt > 10000; 
-- 위 조회의 결과는 'hong1', 'hong2'
select *
from members m
where m.memid IN ('hong2', 'hong1'); -- 위 조회 결과 사용

-- 서브쿼리의 결과가 여러 개인 경우 in 사용
select *
from members m
where m.memid IN (select distinct memid 
				  from order_h 
                  where ordamt > 10000);
                  
-- 3. 주문을 취소한 고객 조회
--    서브쿼리의 결과가 단일 값인 경우 =, >, <, ... 사용해서 비교


select *
from members m
where m.memid = (select distinct memid 
				 from order_h 
                 where cancelyn ='Y');
                 
-- 4. 취소되지 않은 주문 중에서 'park' 고객의 주문 조회
select *
from order_h
where cancelyn != 'Y' and memid = 'park';

-- 인라인 뷰 : from 절에서 사용된 서브쿼리
select *
from
(
	select *
	from order_h
	where cancelyn != 'Y' 
) s -- 인라인 뷰는 반드시 별칭을 지정해야 합니다.
where s.memid = 'park';

-- 5. 주문번호, 상품코드, 상품주문총액, 주문총액 조회
--    scalar subquery : select 절에 포함된 subquery
--    상관부속질의 : subquery에서 main query의 데이터를 사용하는 경우 
select 
	orderno, goodscd, amt,
    (select sum(amt) 
     from order_d so 
     where so.orderno = mo.orderno) 주문총액
from order_d mo;

-- 6. EXISTS : 대상 subquery의 조회 결과가 있으면 True 없으면 False
SELECT *
FROM members m
-- WHERE EXISTS (SELECT * FROM order_h WHERE memid = 'abc');
-- WHERE EXISTS (SELECT * FROM order_h WHERE memid = 'hong1');
-- WHERE EXISTS (SELECT * FROM order_h oh WHERE oh.memid = m.memid);
WHERE EXISTS (SELECT 1);

SELECT * FROM order_h oh where memid = 'Taeh';

-- 7. ANY, ALL : subquery의 결과가 여러 개인 경우 비교 연산에 사용하는 도구
--    ANY : subquery 결과 값 중 한 개만 만족하면 True (각 값에 대한 비교를 OR로 연결)
--    ALL : subquery 결과 값 모두에 대해 만족하면 True (각 값에 대한 비교를 AND로 연결)
SELECT ordamt FROM order_h WHERE memid = 'park';

SELECT ordamt FROM order_h WHERE memid = 'park'; -- 5000, 10000
SELECT *
FROM order_h oh
-- WHERE oh.ordamt > ( SELECT ordamt FROM order_h WHERE memid = 'park' ); -- 오류 : 비교연산자는 단일 값에 대해서만 사용 가능
-- WHERE oh.ordamt > ANY ( SELECT ordamt FROM order_h WHERE memid = 'park' );
-- WHERE oh.ordamt > 5000 OR oh.ordamt > 10000; -- 위 ANY 구문과 같은 역할
-- WHERE oh.ordamt > ALL ( SELECT ordamt FROM order_h WHERE memid = 'park' );
WHERE oh.ordamt > 5000 AND oh.ordamt > 10000; -- 위 ALL 구문과 같은 역할