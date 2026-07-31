-- 1. 작업 데이터베이스 변경
USE kamebook;

-- SELECT 문의 형식
/*
	SELECT 컬럼1, 컬럼2, 컬럼3, ...
    [FROM 테이블이름]
    [WHERE 조건식]
    [GROUP BY 컬럼1, 컬럼2, ...]
    [HAVING 조건식]
    [ORDER BY 컬럼1, 컬럼2, ...]
*/

-- 2. members 테이블의 모든 컬럼의 데이터 조회
SELECT 
	memid, memname, passwd, passwdmdt, 
    jumin, addr, birthday, jobcd, mileage, 
    stat, enterdtm, leavedtm
FROM members;

SELECT * FROM members; -- 모든 컬럼은 *로 대체할 수 있습니다.

-- 3. 회원아이디, 회원이름, 주소, 생일 컬럼 조회 ( 특정 컬럼 조회 )
SELECT memid, memname, addr, birthday
FROM members;

-- 4. 회원아이디, 회원이름, 주소, 생년월일 컬럼을 컬럼명을 변경해서 조회
SELECT 
	memid as 회원아이디, 
    memname 회원이름, 
    addr 주소, 
    birthday 생년월일
FROM members;

-- 5. 회원아이디, 회원이름, 주소, 생년월일 컬럼을 생년월일 순으로 정렬해서 조회
SELECT memid, memname, addr, birthday
FROM members
-- ORDER BY birthday; -- ASC 생략 : 오름차순 정렬
ORDER BY birthday DESC; -- DESC : 내림차순 정렬

-- 6. 회원아이디, 회원이름, 주소, 생년월일 컬럼을 이름순으로 정렬해서 조회
--    (같은 이름은 생년월일로 정렬)
SELECT memid, memname, addr, birthday
FROM members
-- ORDER BY memname, birthday;
ORDER BY memname ASC, birthday DESC;

-- 7. 회원들의 직업코드 조회
-- SELECT jobcd
SELECT DISTINCT jobcd -- DISTINCT : 중복 제거
FROM members
ORDER BY jobcd;

-- 8-1. 생년월일이 빠른 직원 3명 조회
SELECT * 
FROM members
ORDER BY birthday
LIMIT 3; -- 앞에서 3개만 조회

-- 8-2. 생년월일이 2번째로 빠른 직원부터 3명 조회
SELECT * 
FROM members
ORDER BY birthday
LIMIT 1,3; -- 1번째 부터 3개만 조회 (순서는 0부터 시작)

-- 9. "[memid] memname" 형식의 결과 및 mileage를 1000 증가한 결과 조회
--    concat : 문자열을 결합하는 함수
SELECT 
	concat("[" , memid, "] ", memname) 아이디와이름, 
    mileage + 1000 보너스합산마일리지
FROM members; 

-- 10. mileage가 0이 아닌 회원 조회
SELECT * 
FROM members
-- WHERE mileage != 0;
WHERE mileage <> 0;

-- 11. mileage가 0인 회원 조회
SELECT * 
FROM members
WHERE mileage = 0; -- SQL에서 동일성 비교 연산자로 = 사용 ( 자바는 == )

-- 12. 2000년 이후에 출생한 회원 조회
SELECT * 
FROM members
WHERE birthday >= '2000-01-01'; -- 날짜 데이터는 '사용해서 표현

-- 13. 2002년에 출생한 회원 조회
SELECT * 
FROM members
-- WHERE birthday >= '2002-01-01' && birthday <= '2002-12-31';
-- WHERE birthday >= '2002-01-01' AND birthday <= '2002-12-31';
WHERE birthday BETWEEN '2002-01-01' AND '2002-12-31';

-- 14. jobcd가 1, 4, 9인 회원 조회
SELECT * 
FROM members
-- WHERE jobcd = 1 OR jobcd = 4 OR jobcd = 9;
WHERE jobcd IN (1, 4, 9);

-- 15. jobcd가 1, 4, 9가 아닌 회원 조회
SELECT * 
FROM members
-- WHERE jobcd != 1 AND jobcd != 4 AND jobcd != 9;
-- WHERE NOT (jobcd = 1 OR jobcd = 4 OR jobcd = 9); -- NOT : 자바의 !연산자
WHERE jobcd NOT IN (1, 4, 9);

-- 16. 인천에 거주하는 회원 조회
SELECT *
FROM members
WHERE addr LIKE '%인천%'; -- addr 컬럼의 값에 '인천'을 포함하는 경우

-- 17. 이름에 '갑'이 포함된 회원 조회
SELECT *
FROM members
WHERE memname LIKE '%갑%'; -- % : 0개 이상의 문자

-- 18. 이름의 두 번째 글자가 '갑'인 회원 조회
SELECT *
FROM members
WHERE memname LIKE '_갑%'; -- _ : 1개의 문자

-- 19. 주민번호가 없는 회원 조회
select * 
from members
-- where jumin = NULL; -- NULL을 비교연산자로 직접 비교할 수 없습니다.
where jumin IS NULL; -- NULL을 비교할 경우에는 IS NULL 사용

-- 20. 전체 주문 총액, 평균, 최고액, 최저액, 건수 조회 ( 집계함수 )
SELECT SUM(ordamt), AVG(ordamt), MAX(ordamt), MIN(ordamt), COUNT(ordamt)
FROM order_h;

-- 21. 고객별 주문 총액, 평균, 최고액, 최저액, 건수 조회 ( 집계함수 )
SELECT memid, SUM(ordamt), AVG(ordamt), MAX(ordamt), MIN(ordamt), COUNT(ordamt)
FROM order_h
GROUP BY memid;

-- 22. 주문 건수가 2건 이상인 고객의 
--    고객별 주문 총액, 평균, 최고액, 최저액, 건수 조회
SELECT memid, SUM(ordamt), AVG(ordamt), MAX(ordamt), MIN(ordamt), COUNT(ordamt)
FROM order_h
-- WHERE COUNT(ordamt) >= 2 -- 오류 : 집계함수는 group by 이후에 사용 가능
GROUP BY memid
HAVING COUNT(ordamt) >= 2; -- HAVING : group by 이후에 실행하는 조건절    