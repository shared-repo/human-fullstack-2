# Chapter 5 데이터 검색 SELECT

> **학습 시간**: 약 8시간  
> **학습 대상**: 데이터베이스를 처음 접하는 학습자  
> **실습 DB**: shopdb (Chapter 3 설계, Chapter 4 생성, shopdb_sample_data.sql 데이터 삽입 완료)

---

## 학습 목표

이 장을 마치면 다음을 할 수 있다.

- SELECT 문으로 원하는 컬럼과 행을 조회할 수 있다.
- WHERE 절과 다양한 연산자로 조건을 지정할 수 있다.
- ORDER BY·LIMIT으로 결과를 정렬하고 개수를 제한할 수 있다.
- 집계 함수(COUNT·SUM·AVG·MAX·MIN)를 사용할 수 있다.
- GROUP BY·HAVING으로 그룹별 집계를 수행할 수 있다.
- INNER JOIN·LEFT JOIN·RIGHT JOIN으로 두 개 이상의 테이블을 조합할 수 있다.
- 서브쿼리를 WHERE 절과 FROM 절에서 활용할 수 있다.
- UNION·INTERSECT·EXCEPT로 두 쿼리의 결과를 결합할 수 있다.

---

## SELECT 문 전체 구조

```sql
SELECT   컬럼 목록          -- ① 어떤 컬럼을
FROM     테이블명            -- ② 어느 테이블에서
JOIN     ...               -- ③ 어떤 테이블과 합쳐서
WHERE    조건               -- ④ 어떤 조건으로 걸러서
GROUP BY 그룹 기준 컬럼      -- ⑤ 어떤 기준으로 묶어서
HAVING   그룹 조건           -- ⑥ 묶은 것 중 어떤 조건으로 걸러서
ORDER BY 정렬 기준 컬럼      -- ⑦ 어떤 순서로
LIMIT    개수;              -- ⑧ 몇 개만
```

> **실행 순서**: SQL은 작성 순서와 내부 실행 순서가 다르다.  
> 실제 실행 순서: `FROM` → `JOIN` → `WHERE` → `GROUP BY` → `HAVING` → `SELECT` → `ORDER BY` → `LIMIT`  
> 이 순서를 알아두면 어떤 절에서 어떤 값을 쓸 수 있는지 이해하기 쉽다.

---

## 1. SELECT 기본

### 1.1 전체 컬럼 조회

`*`(애스터리스크)는 모든 컬럼을 의미한다.

```sql
SELECT * FROM category;
```

실행 결과:
```
+-------------+------------------+
| category_id | category_name    |
+-------------+------------------+
|           1 | 전자기기          |
|           2 | 주방용품          |
|           3 | 도서             |
|           4 | 스포츠용품        |
|           5 | 의류             |
|           6 | 뷰티/헬스         |
|           7 | 가구/인테리어     |
|           8 | 식품             |
|           9 | 완구/취미         |
|          10 | 반려동물용품       |
+-------------+------------------+
10 rows in set
```

> **실무 팁**: `SELECT *`는 편리하지만, 필요 없는 컬럼까지 가져오므로 성능이 떨어진다. 실무에서는 필요한 컬럼만 명시하는 것이 좋다.

---

### 1.2 원하는 컬럼만 조회

```sql
SELECT product_name, price, stock
FROM product;
```

실행 결과:
```
+------------------------+--------+-------+
| product_name           | price  | stock |
+------------------------+--------+-------+
| 무선 마우스             |  25000 |    50 |
| USB 4포트 허브          |  35000 |    30 |
| 블루투스 키보드          |  55000 |    20 |
| 스테인리스 전기주전자    |  42000 |    15 |
| MariaDB 입문           |  28000 |   100 |
| 요가매트 6mm            |  32000 |    40 |
| 코튼 반팔 티셔츠         |  18000 |   200 |
| 비타민C 1000mg          |  15000 |   150 |
| 원목 1인 책상           | 185000 |    10 |
| 건조 블루베리 200g       |  12000 |    80 |
+------------------------+--------+-------+
10 rows in set
```

---

### 1.3 별칭 — AS

컬럼이나 테이블에 임시 이름(별칭)을 붙여 결과를 읽기 쉽게 만든다.

```sql
SELECT
    product_name  AS 상품명,
    price         AS 판매가격,
    stock         AS 재고수량
FROM product;
```

> **AS는 생략 가능하다**: `price AS 판매가격`과 `price 판매가격`은 동일하다. 그러나 가독성을 위해 AS를 명시하는 것을 권장한다.

---

### 1.4 SELECT에서 수식 사용

컬럼 값을 계산하거나 가공하여 조회할 수 있다.

```sql
SELECT
    product_name           AS 상품명,
    price                  AS 판매가격,
    ROUND(price * 1.1)     AS 부가세포함가격,
    stock                  AS 재고수량,
    price * stock          AS 재고자산
FROM product;
```

실행 결과 (일부):
```
+------------------+--------+----------------+--------+------------+
| 상품명            | 판매가격 | 부가세포함가격  | 재고수량 | 재고자산    |
+------------------+--------+----------------+--------+------------+
| 무선 마우스        |  25000 |          27500 |     50 |    1250000 |
| USB 4포트 허브    |  35000 |          38500 |     30 |    1050000 |
| 블루투스 키보드    |  55000 |          60500 |     20 |    1100000 |
...
+------------------+--------+----------------+--------+------------+
```

---

### 1.5 중복 제거 — DISTINCT

```sql
SELECT DISTINCT customer_id
FROM orders;
```

실행 결과: 10개의 주문 중 실제로 주문한 고객 ID (customer_id 1·2가 2회씩 주문)
```
+-------------+
| customer_id |
+-------------+
|           1 |
|           2 |
|           3 |
|           4 |
|           5 |
|           6 |
|           7 |
|           8 |
+-------------+
8 rows in set
```

---

### ✅ 확인 문제 1

1. `customer` 테이블에서 `name`, `email`, `phone` 컬럼만 조회하되, 컬럼 별칭을 각각 `고객명`, `이메일`, `연락처`로 지정하는 SQL을 작성하시오.
2. `product` 테이블에서 `product_name`, `price`, 그리고 `price`에서 10% 할인된 `할인가격`을 함께 조회하는 SQL을 작성하시오.
3. `orders` 테이블에서 중복 없이 `payment_method` 목록을 조회하는 SQL을 작성하시오.

> **정답**:
> ```sql
> -- 1
> SELECT name AS 고객명, email AS 이메일, phone AS 연락처
> FROM customer;
>
> -- 2
> SELECT product_name, price, price * 0.9 AS 할인가격
> FROM product;
>
> -- 3
> SELECT DISTINCT payment_method
> FROM orders;
> ```

---

## 2. WHERE 절과 연산자

`WHERE` 절은 조건에 맞는 행만 골라낸다.

### 2.1 비교 연산자

| 연산자 | 의미 | 예시 |
|:---:|---|---|
| `=` | 같다 | `price = 25000` |
| `!=` 또는 `<>` | 다르다 | `status != '배송완료'` |
| `>` | 크다 | `price > 30000` |
| `>=` | 크거나 같다 | `price >= 30000` |
| `<` | 작다 | `stock < 20` |
| `<=` | 작거나 같다 | `stock <= 20` |

```sql
-- 가격이 30,000원 이상인 상품 조회
SELECT product_name, price
FROM product
WHERE price >= 30000;
```

실행 결과:
```
+------------------------+--------+
| product_name           | price  |
+------------------------+--------+
| USB 4포트 허브          |  35000 |
| 블루투스 키보드          |  55000 |
| 스테인리스 전기주전자    |  42000 |
| 요가매트 6mm            |  32000 |
| 원목 1인 책상           | 185000 |
+------------------------+--------+
5 rows in set
```

---

### 2.2 논리 연산자 — AND, OR, NOT

**AND**: 두 조건 모두 참인 행만 선택

```sql
-- 전자기기(category_id=1) 중 가격이 30,000원 이상인 상품
SELECT product_name, price
FROM product
WHERE category_id = 1
  AND price >= 30000;
```

실행 결과:
```
+------------------+-------+
| product_name     | price |
+------------------+-------+
| USB 4포트 허브   | 35000 |
| 블루투스 키보드   | 55000 |
+------------------+-------+
```

**OR**: 두 조건 중 하나라도 참인 행 선택

```sql
-- 배송완료이거나 배송중인 주문 조회
SELECT order_id, customer_id, status
FROM orders
WHERE status = '배송완료'
   OR status = '배송중';
```

**NOT**: 조건의 반대

```sql
-- 전자기기가 아닌 상품 조회
SELECT product_name, category_id
FROM product
WHERE NOT category_id = 1;
```

> **우선순위**: `NOT` > `AND` > `OR` 순서로 적용된다. 복잡한 조건은 괄호`()`로 명확히 묶는 것이 좋다.

---

### 2.3 범위 — BETWEEN

```sql
BETWEEN 최솟값 AND 최댓값
```

최솟값과 최댓값을 **포함**한 범위의 행을 선택한다.

```sql
-- 가격이 20,000원 이상 50,000원 이하인 상품
SELECT product_name, price
FROM product
WHERE price BETWEEN 20000 AND 50000;
```

실행 결과:
```
+------------------------+-------+
| product_name           | price |
+------------------------+-------+
| 무선 마우스             | 25000 |
| USB 4포트 허브          | 35000 |
| 스테인리스 전기주전자    | 42000 |
| MariaDB 입문           | 28000 |
| 요가매트 6mm            | 32000 |
+------------------------+-------+
5 rows in set
```

날짜 범위에도 사용할 수 있다.

```sql
-- 2024년 1분기(1~3월) 주문 조회
SELECT order_id, order_date, status
FROM orders
WHERE order_date BETWEEN '2024-01-01' AND '2024-03-31';
```

---

### 2.4 목록 — IN

여러 값 중 하나와 일치하는 행을 선택한다. `OR`을 여러 번 쓰는 것보다 간결하다.

```sql
-- 전자기기(1), 도서(3), 스포츠용품(4) 카테고리 상품 조회
SELECT product_name, category_id, price
FROM product
WHERE category_id IN (1, 3, 4);
```

실행 결과:
```
+------------------+-------------+-------+
| product_name     | category_id | price |
+------------------+-------------+-------+
| 무선 마우스       |           1 | 25000 |
| USB 4포트 허브   |           1 | 35000 |
| 블루투스 키보드   |           1 | 55000 |
| MariaDB 입문     |           3 | 28000 |
| 요가매트 6mm     |           4 | 32000 |
+------------------+-------------+-------+
5 rows in set
```

`NOT IN`을 사용하면 목록에 없는 행을 선택한다.

```sql
SELECT product_name, category_id
FROM product
WHERE category_id NOT IN (1, 2, 3);
```

---

### 2.5 패턴 검색 — LIKE

문자열 패턴으로 검색할 때 사용한다.

| 와일드카드 | 의미 | 예시 |
|:---:|---|---|
| `%` | 0개 이상의 임의 문자 | `'마우스%'` → 마우스로 시작하는 값 |
| `_` | 정확히 1개의 임의 문자 | `'홍_동'` → 홍과 동 사이에 한 글자 |

```sql
-- 상품명에 '마우스'가 포함된 상품
SELECT product_name, price
FROM product
WHERE product_name LIKE '%마우스%';
```

실행 결과:
```
+--------------+-------+
| product_name | price |
+--------------+-------+
| 무선 마우스   | 25000 |
+--------------+-------+
1 row in set
```

```sql
-- 이메일이 'kim'으로 시작하는 고객
SELECT name, email
FROM customer
WHERE email LIKE 'kim%';
```

```sql
-- 이름이 두 글자인 고객 (드문 경우이나 연습용)
SELECT name FROM customer WHERE name LIKE '__';
```

> **주의**: LIKE 패턴 검색은 컬럼 전체를 스캔하므로 데이터가 많을 때 속도가 느릴 수 있다. 특히 `'%검색어'`처럼 앞에 `%`가 오는 패턴은 인덱스를 사용하지 못한다.

---

### 2.6 NULL 검사 — IS NULL / IS NOT NULL

NULL은 `=`로 비교할 수 없다. 반드시 `IS NULL` 또는 `IS NOT NULL`을 사용한다.

```sql
-- 전화번호가 없는(NULL) 고객 조회
SELECT name, email, phone
FROM customer
WHERE phone IS NULL;
```

```sql
-- 상품 설명이 있는 상품만 조회
SELECT product_name, description
FROM product
WHERE description IS NOT NULL;
```

> **흔한 실수**: `WHERE phone = NULL`은 항상 결과가 없다. NULL과의 비교는 항상 `IS NULL` / `IS NOT NULL`을 사용한다.

---

### ✅ 확인 문제 2

1. `product` 테이블에서 재고(`stock`)가 20개 이하이고 가격이 30,000원 이상인 상품의 `product_name`, `price`, `stock`을 조회하는 SQL을 작성하시오.
2. `orders` 테이블에서 결제방법(`payment_method`)이 `'신용카드'`가 아닌 주문을 조회하시오.
3. `customer` 테이블에서 이름이 '김'으로 시작하는 고객을 조회하시오.
4. `product` 테이블에서 가격이 15,000원 이상 35,000원 이하인 상품을 조회하시오.

> **정답**:
> ```sql
> -- 1
> SELECT product_name, price, stock
> FROM product
> WHERE stock <= 20 AND price >= 30000;
>
> -- 2
> SELECT * FROM orders
> WHERE payment_method != '신용카드';
> -- 또는: WHERE payment_method NOT IN ('신용카드')
>
> -- 3
> SELECT * FROM customer
> WHERE name LIKE '김%';
>
> -- 4
> SELECT product_name, price
> FROM product
> WHERE price BETWEEN 15000 AND 35000;
> ```

---

## 3. ORDER BY · LIMIT

### 3.1 정렬 — ORDER BY

조회 결과를 특정 컬럼 기준으로 정렬한다.

```sql
ORDER BY 컬럼명 [ASC | DESC]
```

- `ASC`: 오름차순 (기본값, 작은 값 → 큰 값)
- `DESC`: 내림차순 (큰 값 → 작은 값)

```sql
-- 가격 높은 순으로 상품 조회
SELECT product_name, price
FROM product
ORDER BY price DESC;
```

실행 결과:
```
+------------------------+--------+
| product_name           | price  |
+------------------------+--------+
| 원목 1인 책상           | 185000 |
| 블루투스 키보드          |  55000 |
| 스테인리스 전기주전자    |  42000 |
| USB 4포트 허브          |  35000 |
| 요가매트 6mm            |  32000 |
| MariaDB 입문           |  28000 |
| 무선 마우스             |  25000 |
| 코튼 반팔 티셔츠         |  18000 |
| 비타민C 1000mg          |  15000 |
| 건조 블루베리 200g       |  12000 |
+------------------------+--------+
10 rows in set
```

**다중 컬럼 정렬**

```sql
-- 카테고리 오름차순, 같은 카테고리 안에서는 가격 내림차순
SELECT category_id, product_name, price
FROM product
ORDER BY category_id ASC, price DESC;
```

---

### 3.2 행 수 제한 — LIMIT

전체 결과 중 일부만 가져올 때 사용한다.

```sql
LIMIT 개수
LIMIT 시작위치, 개수     -- 시작위치는 0부터
```

```sql
-- 가장 비싼 상품 3개
SELECT product_name, price
FROM product
ORDER BY price DESC
LIMIT 3;
```

실행 결과:
```
+------------------+--------+
| product_name     | price  |
+------------------+--------+
| 원목 1인 책상    | 185000 |
| 블루투스 키보드   |  55000 |
| 스테인리스 전기주전자 | 42000 |
+------------------+--------+
3 rows in set
```

**페이지네이션**: 게시판 등에서 페이지 단위로 데이터를 가져올 때 활용한다.

```sql
-- 1페이지: 처음 3개 (0번째부터 3개)
SELECT product_name, price FROM product ORDER BY product_id LIMIT 0, 3;

-- 2페이지: 다음 3개 (3번째부터 3개)
SELECT product_name, price FROM product ORDER BY product_id LIMIT 3, 3;

-- 3페이지: 다음 3개 (6번째부터 3개)
SELECT product_name, price FROM product ORDER BY product_id LIMIT 6, 3;
```

---

### ✅ 확인 문제 3

1. `product` 테이블에서 가격이 낮은 순으로 정렬하여 가장 저렴한 상위 3개 상품을 조회하시오.
2. `customer` 테이블에서 가입일(`created_at`) 기준으로 최신 순 정렬하여 5명을 조회하시오.
3. `orders` 테이블에서 주문일(`order_date`) 기준 오름차순으로 정렬하되, 4번째부터 3개(2페이지 개념)를 조회하시오.

> **정답**:
> ```sql
> -- 1
> SELECT product_name, price
> FROM product
> ORDER BY price ASC
> LIMIT 3;
>
> -- 2
> SELECT name, email, created_at
> FROM customer
> ORDER BY created_at DESC
> LIMIT 5;
>
> -- 3
> SELECT * FROM orders
> ORDER BY order_date ASC
> LIMIT 3, 3;
> ```

---

## 4. 집계 함수

집계 함수는 여러 행의 값을 계산하여 **하나의 결과값**을 반환한다.

| 함수 | 설명 | NULL 처리 |
|:---:|---|---|
| `COUNT(*)` | 전체 행 수 | NULL 포함 |
| `COUNT(컬럼)` | 해당 컬럼에 값이 있는 행 수 | NULL 제외 |
| `SUM(컬럼)` | 합계 | NULL 제외 |
| `AVG(컬럼)` | 평균 | NULL 제외 |
| `MAX(컬럼)` | 최댓값 | NULL 제외 |
| `MIN(컬럼)` | 최솟값 | NULL 제외 |

```sql
-- product 테이블 전체 통계
SELECT
    COUNT(*)          AS 전체상품수,
    SUM(price)        AS 가격합계,
    AVG(price)        AS 평균가격,
    MAX(price)        AS 최고가격,
    MIN(price)        AS 최저가격
FROM product;
```

실행 결과:
```
+----------+----------+----------+----------+----------+
| 전체상품수 | 가격합계  | 평균가격  | 최고가격  | 최저가격  |
+----------+----------+----------+----------+----------+
|       10 |   447000 |  44700.0 |   185000 |    12000 |
+----------+----------+----------+----------+----------+
```

**COUNT(*) vs COUNT(컬럼) 차이**

```sql
-- phone 컬럼에 NULL이 있다면 두 결과가 다를 수 있다
SELECT
    COUNT(*)      AS 전체행수,
    COUNT(phone)  AS 연락처있는_행수
FROM customer;
```

---

### ✅ 확인 문제 4

1. `order_detail` 테이블에서 전체 주문 수량(`quantity`)의 합계를 구하시오.
2. `product` 테이블에서 재고(`stock`)의 평균, 최댓값, 최솟값을 구하시오.
3. `orders` 테이블에서 전체 주문 건수를 구하시오.

> **정답**:
> ```sql
> -- 1
> SELECT SUM(quantity) AS 총주문수량 FROM order_detail;
>
> -- 2
> SELECT AVG(stock) AS 평균재고, MAX(stock) AS 최대재고, MIN(stock) AS 최소재고
> FROM product;
>
> -- 3
> SELECT COUNT(*) AS 총주문건수 FROM orders;
> ```

---

## 5. GROUP BY 절

`GROUP BY`는 지정한 컬럼의 값이 같은 행끼리 묶어 그룹별로 집계를 수행한다.

```sql
SELECT   그룹기준컬럼, 집계함수(...)
FROM     테이블명
GROUP BY 그룹기준컬럼;
```

> **규칙**: SELECT에 집계 함수가 아닌 컬럼이 오면, 반드시 GROUP BY에도 같은 컬럼이 있어야 한다.

```sql
-- 카테고리별 상품 수와 평균 가격
SELECT
    category_id,
    COUNT(*)       AS 상품수,
    AVG(price)     AS 평균가격,
    MAX(price)     AS 최고가격
FROM product
GROUP BY category_id;
```

실행 결과:
```
+-------------+--------+----------+----------+
| category_id | 상품수  | 평균가격  | 최고가격  |
+-------------+--------+----------+----------+
|           1 |      3 |  38333.3 |    55000 |
|           2 |      1 |  42000.0 |    42000 |
|           3 |      1 |  28000.0 |    28000 |
|           4 |      1 |  32000.0 |    32000 |
|           5 |      1 |  18000.0 |    18000 |
|           6 |      1 |  15000.0 |    15000 |
|           7 |      1 | 185000.0 |   185000 |
|           8 |      1 |  12000.0 |    12000 |
+-------------+--------+----------+----------+
8 rows in set
```

**GROUP BY + ORDER BY**

```sql
-- 주문별 결제 금액 합산 (많은 순)
SELECT
    order_id,
    SUM(quantity * unit_price) AS 결제금액
FROM order_detail
GROUP BY order_id
ORDER BY 결제금액 DESC;
```

실행 결과:
```
+----------+----------+
| order_id | 결제금액  |
+----------+----------+
|        7 |   185000 |
|        1 |    85000 |
|        3 |    71000 |
|        5 |    54000 |
|        2 |    55000 |
...
```

---

### ✅ 확인 문제 5

1. `orders` 테이블에서 고객(`customer_id`)별 주문 횟수를 조회하시오.
2. `orders` 테이블에서 주문 상태(`status`)별 주문 건수를 조회하시오.
3. `order_detail` 테이블에서 상품(`product_id`)별 총 판매 수량을 조회하시오.

> **정답**:
> ```sql
> -- 1
> SELECT customer_id, COUNT(*) AS 주문횟수
> FROM orders
> GROUP BY customer_id;
>
> -- 2
> SELECT status, COUNT(*) AS 건수
> FROM orders
> GROUP BY status;
>
> -- 3
> SELECT product_id, SUM(quantity) AS 총판매수량
> FROM order_detail
> GROUP BY product_id
> ORDER BY 총판매수량 DESC;
> ```

---

## 6. HAVING 절

`HAVING`은 `GROUP BY`로 묶인 그룹에 조건을 적용한다.  
`WHERE`가 행(row)을 필터링한다면, `HAVING`은 그룹을 필터링한다.

```
WHERE  → 그룹화 전 개별 행을 필터링
HAVING → 그룹화 후 그룹 전체를 필터링
```

```sql
-- 상품이 2개 이상인 카테고리만 조회
SELECT
    category_id,
    COUNT(*) AS 상품수
FROM product
GROUP BY category_id
HAVING COUNT(*) >= 2;
```

실행 결과:
```
+-------------+--------+
| category_id | 상품수  |
+-------------+--------+
|           1 |      3 |
+-------------+--------+
1 row in set
```

**WHERE와 HAVING 함께 사용**

```sql
-- 가격이 10,000원 이상인 상품(WHERE) 중,
-- 카테고리별 평균 가격이 30,000원 이상인 카테고리(HAVING) 조회
SELECT
    category_id,
    COUNT(*)    AS 상품수,
    AVG(price)  AS 평균가격
FROM product
WHERE price >= 10000
GROUP BY category_id
HAVING AVG(price) >= 30000
ORDER BY 평균가격 DESC;
```

---

### ✅ 확인 문제 6

1. `orders` 테이블에서 주문 횟수가 2회 이상인 고객(`customer_id`)을 조회하시오.
2. `order_detail` 테이블에서 총 판매 수량이 2개 이상인 상품(`product_id`)과 그 수량을 조회하시오.
3. `WHERE`와 `HAVING`의 차이를 설명하시오.

> **정답**:
> ```sql
> -- 1
> SELECT customer_id, COUNT(*) AS 주문횟수
> FROM orders
> GROUP BY customer_id
> HAVING COUNT(*) >= 2;
> -- 결과: customer_id 1(홍길동, 2회), 2(김영희, 2회)
>
> -- 2
> SELECT product_id, SUM(quantity) AS 총판매수량
> FROM order_detail
> GROUP BY product_id
> HAVING SUM(quantity) >= 2;
>
> -- 3
> WHERE는 GROUP BY 전에 실행되어 개별 행을 필터링한다.
> HAVING은 GROUP BY 후에 실행되어 집계 결과(그룹)를 필터링한다.
> 집계 함수(COUNT, SUM 등)를 조건으로 사용하려면 반드시 HAVING을 써야 한다.
> ```

---

## 7. JOIN

`JOIN`은 두 개 이상의 테이블을 연결하여 하나의 결과로 조회한다.  
테이블을 나눠 저장한 데이터를 다시 합쳐서 볼 때 사용한다.

### 7.1 INNER JOIN

두 테이블에서 **ON 조건을 만족하는 행만** 반환한다.  
조건에 맞는 행이 한쪽에만 있으면 결과에 포함되지 않는다.

```sql
SELECT 컬럼 목록
FROM 테이블A
INNER JOIN 테이블B ON 테이블A.컬럼 = 테이블B.컬럼;
```

```sql
-- 상품과 카테고리명을 함께 조회
SELECT
    p.product_id,
    p.product_name,
    c.category_name,
    p.price
FROM product p
INNER JOIN category c ON p.category_id = c.category_id;
```

실행 결과:
```
+------------+------------------------+------------------+--------+
| product_id | product_name           | category_name    | price  |
+------------+------------------------+------------------+--------+
|          1 | 무선 마우스             | 전자기기          |  25000 |
|          2 | USB 4포트 허브          | 전자기기          |  35000 |
|          3 | 블루투스 키보드          | 전자기기          |  55000 |
|          4 | 스테인리스 전기주전자    | 주방용품          |  42000 |
|          5 | MariaDB 입문           | 도서             |  28000 |
|          6 | 요가매트 6mm            | 스포츠용품        |  32000 |
|          7 | 코튼 반팔 티셔츠         | 의류             |  18000 |
|          8 | 비타민C 1000mg          | 뷰티/헬스         |  15000 |
|          9 | 원목 1인 책상           | 가구/인테리어     | 185000 |
|         10 | 건조 블루베리 200g       | 식품             |  12000 |
+------------+------------------------+------------------+--------+
```

> **테이블 별칭 사용**: 조인할 때 테이블명이 길면 별칭(`p`, `c`)을 사용하여 SQL을 간결하게 만든다.

---

### 7.2 LEFT JOIN

왼쪽 테이블의 **모든 행**을 포함하고, 오른쪽 테이블에 일치하는 행이 없으면 NULL로 채운다.

```sql
-- 모든 고객 + 주문 정보 (주문 없는 고객도 포함)
SELECT
    c.customer_id,
    c.name       AS 고객명,
    o.order_id,
    o.order_date,
    o.status
FROM customer c
LEFT JOIN orders o ON c.customer_id = o.customer_id
ORDER BY c.customer_id;
```

실행 결과 (일부):
```
+-------------+--------+----------+---------------------+-----------+
| customer_id | 고객명  | order_id | order_date          | status    |
+-------------+--------+----------+---------------------+-----------+
|           1 | 홍길동  |        1 | 2024-01-15 10:23:00 | 배송완료   |
|           1 | 홍길동  |        3 | 2024-02-20 09:47:00 | 배송완료   |
|           2 | 김영희  |        2 | 2024-02-03 14:05:00 | 배송완료   |
|           2 | 김영희  |        8 | 2024-05-07 15:40:00 | 배송중    |
...
|           9 | 임서연  |     NULL | NULL                | NULL      |  ← 주문 없음
|          10 | 윤도현  |     NULL | NULL                | NULL      |  ← 주문 없음
+-------------+--------+----------+---------------------+-----------+
```

**활용**: 주문한 적 없는 고객 찾기

```sql
SELECT c.name, c.email
FROM customer c
LEFT JOIN orders o ON c.customer_id = o.customer_id
WHERE o.order_id IS NULL;
```

결과: 임서연, 윤도현 (주문 이력 없는 고객)

---

### 7.3 RIGHT JOIN

오른쪽 테이블의 **모든 행**을 포함하고, 왼쪽 테이블에 일치하는 행이 없으면 NULL로 채운다.  
LEFT JOIN의 방향을 뒤집은 것과 같으므로, 실무에서는 LEFT JOIN을 더 자주 사용한다.

```sql
SELECT
    c.name     AS 고객명,
    o.order_id,
    o.status
FROM customer c
RIGHT JOIN orders o ON c.customer_id = o.customer_id;
```

---

### 7.4 세 개 이상 테이블 JOIN

```sql
-- 주문 → 주문상세 → 상품 → 고객 전체 연결
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
ORDER BY o.order_id, p.product_name;
```

실행 결과:
```
+----------+--------+------------------+------+--------+--------+
| order_id | 고객명  | 상품명            | 수량  | 단가   | 소계   |
+----------+--------+------------------+------+--------+--------+
|        1 | 홍길동  | USB 4포트 허브   |    1 |  35000 |  35000 |
|        1 | 홍길동  | 무선 마우스       |    2 |  25000 |  50000 |
|        2 | 김영희  | 블루투스 키보드   |    1 |  55000 |  55000 |
|        3 | 홍길동  | MariaDB 입문     |    2 |  28000 |  56000 |
|        3 | 홍길동  | 비타민C 1000mg   |    1 |  15000 |  15000 |
...
+----------+--------+------------------+------+--------+--------+
```

---

### 7.5 CROSS JOIN

두 테이블의 모든 행을 서로 조합한다(카테시안 곱). ON 조건이 없다.  
결과 행 수 = 테이블A 행 수 × 테이블B 행 수

```sql
-- 모든 카테고리와 상품의 조합 (실습용)
SELECT c.category_name, p.product_name
FROM category c
CROSS JOIN product p
LIMIT 5;
```

> **실무 주의**: 두 테이블이 각각 1000행이면 결과가 100만 행이 된다. 의도치 않게 ON 조건을 생략하면 CROSS JOIN이 발생하여 서버에 부하를 줄 수 있다.

---

### ✅ 확인 문제 7

1. `product` 테이블과 `category` 테이블을 JOIN하여 `category_name`이 `'전자기기'`인 상품의 `product_name`과 `price`를 조회하시오.
2. 모든 고객 중 주문한 적 없는 고객의 `name`과 `email`을 조회하시오.
3. `orders`, `order_detail`, `product` 테이블을 JOIN하여 각 주문의 `order_id`, 상품명, 수량, 소계(수량×단가)를 조회하시오.

> **정답**:
> ```sql
> -- 1
> SELECT p.product_name, p.price
> FROM product p
> JOIN category c ON p.category_id = c.category_id
> WHERE c.category_name = '전자기기';
>
> -- 2
> SELECT c.name, c.email
> FROM customer c
> LEFT JOIN orders o ON c.customer_id = o.customer_id
> WHERE o.order_id IS NULL;
>
> -- 3
> SELECT
>     o.order_id,
>     p.product_name,
>     d.quantity,
>     d.quantity * d.unit_price AS 소계
> FROM orders o
> JOIN order_detail d ON o.order_id   = d.order_id
> JOIN product      p ON d.product_id = p.product_id;
> ```

---

## 8. 서브쿼리

**서브쿼리(Subquery)**는 SQL 문 안에 포함된 또 다른 SELECT 문이다.  
복잡한 조건이나 계산 결과를 동적으로 조회할 때 유용하다.

### 8.1 WHERE 절의 서브쿼리

**단일값 반환 서브쿼리**

```sql
-- 평균 가격보다 비싼 상품 조회
SELECT product_name, price
FROM product
WHERE price > (SELECT AVG(price) FROM product);
```

서브쿼리 `SELECT AVG(price) FROM product`는 `44700`을 반환하고,  
바깥 쿼리는 가격이 44700 초과인 상품을 찾는다.

실행 결과:
```
+------------------+--------+
| product_name     | price  |
+------------------+--------+
| 블루투스 키보드   |  55000 |
| 원목 1인 책상    | 185000 |
+------------------+--------+
2 rows in set
```

**목록 반환 서브쿼리 (IN)**

```sql
-- '배송완료' 상태 주문을 한 고객의 이름과 이메일 조회
SELECT name, email
FROM customer
WHERE customer_id IN (
    SELECT DISTINCT customer_id
    FROM orders
    WHERE status = '배송완료'
);
```

---

### 8.2 FROM 절의 서브쿼리 (인라인 뷰)

서브쿼리를 임시 테이블처럼 FROM 절에 사용한다. 반드시 별칭을 붙여야 한다.

```sql
-- 주문별 결제금액을 먼저 집계한 뒤, 그 결과에서 5만원 이상인 주문만 조회
SELECT *
FROM (
    SELECT
        order_id,
        SUM(quantity * unit_price) AS 결제금액
    FROM order_detail
    GROUP BY order_id
) AS 주문금액
WHERE 결제금액 >= 50000
ORDER BY 결제금액 DESC;
```

실행 결과:
```
+----------+----------+
| order_id | 결제금액  |
+----------+----------+
|        7 |   185000 |
|        1 |    85000 |
|        3 |    71000 |
|        2 |    55000 |
+----------+----------+
4 rows in set
```

---

### 8.3 SELECT 절의 서브쿼리 (스칼라 서브쿼리)

SELECT 절에 서브쿼리를 사용하면 각 행마다 서브쿼리가 실행된다.

```sql
-- 각 상품의 가격과 전체 평균 가격 차이 조회
SELECT
    product_name,
    price,
    (SELECT AVG(price) FROM product)       AS 평균가격,
    price - (SELECT AVG(price) FROM product) AS 평균과의차이
FROM product
ORDER BY 평균과의차이 DESC;
```

---

### 8.4 EXISTS / NOT EXISTS

서브쿼리 결과가 존재하는지 여부를 확인한다.  
`IN`보다 대용량 데이터에서 성능이 좋을 수 있다.

```sql
-- 주문 이력이 있는 고객만 조회
SELECT name, email
FROM customer c
WHERE EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.customer_id = c.customer_id
);

-- 주문 이력이 없는 고객 조회
SELECT name, email
FROM customer c
WHERE NOT EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.customer_id = c.customer_id
);
```

---

### ✅ 확인 문제 8

1. `product` 테이블에서 가격이 전체 최고가격(`MAX`)의 절반 이상인 상품을 조회하시오.
2. `customer` 테이블에서 한 번도 주문하지 않은 고객의 이름을 서브쿼리를 사용하여 조회하시오.
3. FROM 절 서브쿼리와 WHERE 절 서브쿼리는 어떤 상황에서 각각 사용하기 유리한가?

> **정답**:
> ```sql
> -- 1
> SELECT product_name, price
> FROM product
> WHERE price >= (SELECT MAX(price) FROM product) / 2;
>
> -- 2
> SELECT name FROM customer
> WHERE customer_id NOT IN (
>     SELECT DISTINCT customer_id FROM orders
> );
>
> -- 3
> WHERE 절 서브쿼리: 특정 집계값(평균·최댓값 등)과 비교하거나 특정 ID 목록과 비교할 때 사용.
> FROM 절 서브쿼리: 집계한 결과를 다시 한번 필터링하거나 가공할 때 사용.
>   (HAVING으로 해결할 수 없는 집계 후 가공에 유용)
> ```

---

## 9. UNION · INTERSECT · EXCEPT

두 SELECT 결과를 합치거나 교집합·차집합을 구한다.  
두 쿼리의 **컬럼 수와 데이터 유형이 일치**해야 한다.

### 9.1 UNION / UNION ALL

```sql
쿼리1
UNION [ALL]
쿼리2
```

- `UNION`: 두 결과를 합치고 중복 행을 제거한다.
- `UNION ALL`: 두 결과를 합치되 중복을 제거하지 않는다. (더 빠름)

```sql
-- 전자기기 상품과 도서 상품을 합쳐서 조회
SELECT product_name, price, '전자기기' AS 카테고리
FROM product
WHERE category_id = 1

UNION

SELECT product_name, price, '도서' AS 카테고리
FROM product
WHERE category_id = 3;
```

실행 결과:
```
+------------------+-------+-----------+
| product_name     | price | 카테고리   |
+------------------+-------+-----------+
| 무선 마우스       | 25000 | 전자기기   |
| USB 4포트 허브   | 35000 | 전자기기   |
| 블루투스 키보드   | 55000 | 전자기기   |
| MariaDB 입문     | 28000 | 도서       |
+------------------+-------+-----------+
4 rows in set
```

**활용 예**: 서로 다른 기간의 주문을 합쳐서 조회하거나, 서로 다른 테이블의 같은 구조 데이터를 하나로 합칠 때 유용하다.

---

### 9.2 INTERSECT

두 쿼리 결과의 **교집합** (두 결과 모두에 있는 행)을 반환한다.

```sql
-- 1번과 3번 주문 모두에 포함된 상품
SELECT product_id FROM order_detail WHERE order_id = 1
INTERSECT
SELECT product_id FROM order_detail WHERE order_id = 3;
```

> **샘플 데이터 결과**: 주문 1(product 1,2)과 주문 3(product 5,8)은 공통 상품이 없으므로 결과가 없다.

---

### 9.3 EXCEPT

첫 번째 쿼리 결과에는 있지만 두 번째 결과에는 **없는 행**을 반환한다.

```sql
-- 전체 고객 중 주문한 적 있는 고객을 제외한 고객 ID 목록
SELECT customer_id FROM customer
EXCEPT
SELECT DISTINCT customer_id FROM orders;
```

실행 결과:
```
+-------------+
| customer_id |
+-------------+
|           9 |
|          10 |
+-------------+
```

---

### 9.4 세 연산자 비교

```
고객 전체    주문한 고객
┌──────────────────────┐
│  9, 10  │  1,2,3,   │
│         │  4,5,6,   │
│         │  7,8      │
└──────────────────────┘

UNION     : 전체 (중복 제거)
INTERSECT : 겹치는 부분
EXCEPT    : 왼쪽에만 있는 부분 (9, 10)
```

---

### ✅ 확인 문제 9

1. `product` 테이블에서 가격이 30,000원 미만인 상품과 재고가 50개 이상인 상품을 `UNION ALL`로 합쳐서 조회하시오. (중복 허용)
2. `UNION`과 `UNION ALL`의 차이는 무엇이며, 언제 각각 사용하는가?
3. `customer` 테이블에서 `orders`에 주문 기록이 있는 `customer_id`를 `INTERSECT`로 구하시오.

> **정답**:
> ```sql
> -- 1
> SELECT product_name, price, stock FROM product WHERE price < 30000
> UNION ALL
> SELECT product_name, price, stock FROM product WHERE stock >= 50;
>
> -- 2
> UNION은 중복 행을 제거(정렬 과정 필요)하므로 속도가 느리다.
> UNION ALL은 중복을 허용하므로 더 빠르다.
> 중복이 없음이 확실하거나 중복을 포함해야 할 때는 UNION ALL을 사용한다.
>
> -- 3
> SELECT customer_id FROM customer
> INTERSECT
> SELECT DISTINCT customer_id FROM orders;
> ```

---

## 장 요약

| 구문 | 핵심 역할 |
|---|---|
| `SELECT 컬럼 FROM 테이블` | 기본 조회 |
| `SELECT DISTINCT` | 중복 제거 |
| `AS` | 컬럼·테이블 별칭 |
| `WHERE` | 행 필터링 |
| `BETWEEN a AND b` | 범위 조건 |
| `IN (값 목록)` | 목록 조건 |
| `LIKE '%패턴%'` | 문자열 패턴 검색 |
| `IS NULL / IS NOT NULL` | NULL 검사 |
| `ORDER BY 컬럼 DESC` | 정렬 |
| `LIMIT n` | 결과 행 수 제한 |
| `COUNT / SUM / AVG / MAX / MIN` | 집계 함수 |
| `GROUP BY` | 그룹별 집계 |
| `HAVING` | 그룹 필터링 (집계 조건) |
| `INNER JOIN` | 두 테이블 교집합 연결 |
| `LEFT JOIN` | 왼쪽 테이블 전체 + 오른쪽 매칭 |
| `RIGHT JOIN` | 오른쪽 테이블 전체 + 왼쪽 매칭 |
| `서브쿼리 (WHERE)` | 동적 조건값 |
| `서브쿼리 (FROM)` | 임시 집계 테이블 |
| `UNION` | 두 쿼리 결과 합집합 (중복 제거) |
| `UNION ALL` | 두 쿼리 결과 합집합 (중복 허용) |
| `INTERSECT` | 두 쿼리 결과 교집합 |
| `EXCEPT` | 두 쿼리 결과 차집합 |

---

## 다음 장 예고

Chapter 6에서는 데이터를 **추가·수정·삭제**하는 DML 명령어인 `INSERT`, `UPDATE`, `DELETE`, `TRUNCATE`를 학습한다.  
SELECT에서 배운 WHERE 조건과 서브쿼리를 DML에 결합하는 방법도 함께 실습한다.
