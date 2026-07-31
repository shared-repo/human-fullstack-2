# Chapter 6 데이터 조작 INSERT · UPDATE · DELETE

> **학습 시간**: 약 5시간  
> **학습 대상**: 데이터베이스를 처음 접하는 학습자  
> **실습 DB**: shopdb (Chapter 5까지 실습 완료 상태)

---

## 학습 목표

이 장을 마치면 다음을 할 수 있다.

- INSERT 문으로 단건·다건 데이터를 삽입할 수 있다.
- CSV 파일에서 데이터를 일괄 입력할 수 있다.
- UPDATE 문으로 조건에 맞는 데이터를 수정할 수 있다.
- DELETE와 TRUNCATE의 차이를 설명하고 적절히 사용할 수 있다.
- INSERT·UPDATE·DELETE 각각에 서브쿼리를 결합하여 활용할 수 있다.

---

## DML이란?

**DML(Data Manipulation Language, 데이터 조작 언어)**은 테이블에 저장된 **데이터를 추가·수정·삭제**하는 SQL 명령어 집합이다.

| 명령어 | 기능 |
|---|---|
| `INSERT` | 새 데이터 추가 |
| `UPDATE` | 기존 데이터 수정 |
| `DELETE` | 특정 데이터 삭제 |
| `TRUNCATE` | 모든 데이터 삭제 (구조 유지) |

> **DDL과의 차이**: DDL(Chapter 4)은 테이블 **구조(틀)**를 다루고, DML은 테이블 **데이터(내용)**를 다룬다.

> ⚠️ **DML 실행 시 공통 주의사항**: `UPDATE`와 `DELETE`는 `WHERE` 조건을 빠뜨리면 테이블의 **모든 행**이 영향을 받는다. 실행 전 반드시 `SELECT`로 대상 행을 확인하는 습관을 들인다.

---

## 1. INSERT — 데이터 입력

### 1.1 기본 문법

```sql
-- 형식 1: 컬럼 목록 명시 (권장)
INSERT INTO 테이블명 (컬럼1, 컬럼2, ...)
VALUES (값1, 값2, ...);

-- 형식 2: 컬럼 목록 생략 (테이블의 모든 컬럼에 순서대로 값 입력)
INSERT INTO 테이블명
VALUES (값1, 값2, ...);
```

> **형식 1을 권장하는 이유**: 컬럼 목록을 명시하면 나중에 테이블 구조가 바뀌어도 SQL이 영향을 받지 않고, 특정 컬럼만 선택적으로 입력할 수 있다.

---

### 1.2 단건 INSERT

**category 테이블에 새 카테고리 추가**

```sql
INSERT INTO category (category_id, category_name)
VALUES (11, '문구/오피스');
```

**결과 확인**

```sql
SELECT * FROM category WHERE category_id = 11;
```

```
+-------------+---------------+
| category_id | category_name |
+-------------+---------------+
|          11 | 문구/오피스    |
+-------------+---------------+
```

---

### 1.3 AUTO_INCREMENT 컬럼 생략

기본 키에 `AUTO_INCREMENT`가 설정되어 있으면 값을 생략하면 자동으로 다음 번호가 입력된다.

```sql
-- category_id를 생략하면 자동으로 12가 입력됨
INSERT INTO category (category_name)
VALUES ('여행/레저');
```

현재 AUTO_INCREMENT 값 확인:

```sql
SELECT LAST_INSERT_ID();
```

```
+------------------+
| LAST_INSERT_ID() |
+------------------+
|               12 |
+------------------+
```

> **LAST_INSERT_ID()**: 직전에 실행한 INSERT 문에서 자동 생성된 기본 키 값을 반환한다. 방금 추가한 행의 ID를 바로 다른 테이블의 FK로 사용할 때 유용하다.

---

### 1.4 DEFAULT 값 활용

`DEFAULT` 키워드를 사용하면 테이블 정의 시 설정한 기본값이 자동으로 입력된다.

```sql
-- stock 기본값(0), created_at 기본값(NOW()) 활용
INSERT INTO product (category_id, product_name, price)
VALUES (11, '볼펜 10자루 세트', 5900);
```

`stock`은 DEFAULT 0, `created_at`은 DEFAULT NOW()로 자동 입력된다.

---

### 1.5 다건 INSERT

한 번의 INSERT 문으로 여러 행을 동시에 삽입한다.  
행마다 INSERT를 실행하는 것보다 **훨씬 빠르다**.

```sql
INSERT INTO category (category_name)
VALUES
    ('반려식물'),
    ('자동차용품'),
    ('악기');
```

---

### 1.6 INSERT ... SELECT

다른 테이블(또는 같은 테이블)의 조회 결과를 그대로 삽입한다.  
데이터 복사·이관·백업에 유용하다.

**실습: 가격이 3만원 이상인 상품을 별도 테이블에 복사**

먼저 대상 테이블을 생성한다.

```sql
CREATE TABLE product_premium (
    product_id    INT            NOT NULL,
    product_name  VARCHAR(100)   NOT NULL,
    price         DECIMAL(10,0)  NOT NULL,
    copied_at     DATETIME       DEFAULT NOW()
);
```

데이터를 복사한다.

```sql
INSERT INTO product_premium (product_id, product_name, price)
SELECT product_id, product_name, price
FROM product
WHERE price >= 30000;
```

결과 확인:

```sql
SELECT * FROM product_premium;
```

```
+------------+------------------------+--------+---------------------+
| product_id | product_name           | price  | copied_at           |
+------------+------------------------+--------+---------------------+
|          2 | USB 4포트 허브          |  35000 | 2024-07-25 10:00:00 |
|          3 | 블루투스 키보드          |  55000 | 2024-07-25 10:00:00 |
|          4 | 스테인리스 전기주전자    |  42000 | 2024-07-25 10:00:00 |
|          6 | 요가매트 6mm            |  32000 | 2024-07-25 10:00:00 |
|          9 | 원목 1인 책상           | 185000 | 2024-07-25 10:00:00 |
+------------+------------------------+--------+---------------------+
5 rows in set
```

---

### 1.7 ON DUPLICATE KEY UPDATE

기본 키 또는 UNIQUE 제약이 중복될 때 INSERT 대신 UPDATE를 수행한다.  
"있으면 수정, 없으면 삽입(Upsert)" 패턴이다.

```sql
INSERT INTO category (category_id, category_name)
VALUES (1, '전자제품')
ON DUPLICATE KEY UPDATE category_name = '전자제품';
```

`category_id = 1`이 이미 존재하므로 INSERT 대신 `category_name`을 `'전자제품'`으로 수정한다.

---

### ✅ 확인 문제 1

1. `customer` 테이블에 아래 정보를 가진 신규 고객 한 명을 삽입하는 SQL을 작성하시오.
   - 이름: 신민아, 이메일: shin@example.com, 비밀번호: `$2b$sample`, 연락처: 010-0000-1111, 주소: 서울특별시 마포구

2. `product` 테이블에서 카테고리가 `'전자기기'`(category_id=1)인 상품을 모두 `product_premium` 테이블에 복사하는 SQL을 작성하시오.

3. 단건 INSERT를 10번 반복하는 것과 다건 INSERT 한 번의 차이를 설명하시오.

> **정답**:
> ```sql
> -- 1
> INSERT INTO customer (name, email, password, phone, address)
> VALUES ('신민아', 'shin@example.com', '$2b$sample', '010-0000-1111', '서울특별시 마포구');
>
> -- 2
> INSERT INTO product_premium (product_id, product_name, price)
> SELECT product_id, product_name, price
> FROM product
> WHERE category_id = 1;
>
> -- 3
> 단건 INSERT를 반복하면 매 실행마다 서버와의 통신, 트랜잭션 처리,
> 인덱스 갱신이 각각 발생한다. 다건 INSERT는 이 과정을 한 번에 처리하므로
> 속도가 훨씬 빠르고 서버 부하도 줄어든다.
> ```

---

## 2. 파일에서 데이터 일괄 입력

### 2.1 왜 파일에서 입력하는가?

현실 업무에서 데이터베이스에 입력해야 할 데이터는 이미 엑셀이나 CSV 파일 형태로 존재하는 경우가 많다.  
수백~수만 건의 데이터를 INSERT 문으로 하나씩 작성하는 것은 비현실적이다.

---

### 2.2 실습용 CSV 파일 준비

메모장(또는 텍스트 에디터)을 열고 아래 내용을 작성한다.  
파일명: `new_products.csv`  
저장 위치: `C:\temp\new_products.csv` (폴더가 없으면 먼저 생성)  
인코딩: **UTF-8**로 저장

```
product_id,category_id,product_name,price,stock
11,9,레고 클래식 블록 1000pcs,48000,25
12,10,강아지 간식 소고기 100g,8500,60
13,11,볼펜 10자루 세트,5900,150
14,4,런닝화 남성용,65000,30
15,6,콜라겐 파우더 30포,32000,45
```

> **주의**: CSV 첫 줄은 컬럼명(헤더)이다. LOAD DATA INFILE에서 헤더를 건너뛰는 옵션을 사용해야 한다.

---

### 2.3 LOAD DATA INFILE

MariaDB에서 CSV 등 텍스트 파일을 테이블로 직접 읽어 들이는 명령이다.

```sql
LOAD DATA INFILE 'C:/temp/new_products.csv'
INTO TABLE product
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','          -- 컬럼 구분자
OPTIONALLY ENCLOSED BY '"'        -- 문자열 감싸는 기호 (선택)
LINES TERMINATED BY '\n'          -- 행 구분자
IGNORE 1 LINES                    -- 첫 줄(헤더) 무시
(product_id, category_id, product_name, price, stock);
```

**주요 옵션 설명**

| 옵션 | 설명 |
|---|---|
| `CHARACTER SET utf8mb4` | 파일 인코딩 지정 (한글이 있으면 필수) |
| `FIELDS TERMINATED BY ','` | 컬럼 사이 구분자 (CSV는 `,`) |
| `OPTIONALLY ENCLOSED BY '"'` | 값을 감싸는 따옴표 처리 |
| `LINES TERMINATED BY '\n'` | 행 구분자 (Windows는 `'\r\n'`) |
| `IGNORE 1 LINES` | 첫 번째 줄(헤더) 건너뜀 |

> **Windows 경로 주의**: 경로 구분자로 역슬래시(`\`) 대신 슬래시(`/`)를 사용한다.  
> `'C:\temp\file.csv'` → `'C:/temp/file.csv'`

---

### 2.4 HeidiSQL에서 CSV 가져오기 (GUI 방법)

커맨드라인 없이 HeidiSQL의 가져오기 기능을 사용할 수 있다.

**순서**

1. HeidiSQL에서 `product` 테이블을 선택한다.
2. 상단 메뉴 → `도구(Tools)` → `CSV 파일 가져오기(Import CSV file)` 클릭
3. 가져올 CSV 파일 선택
4. 옵션 설정
   - **Fields terminated by**: `,`
   - **Lines terminated by**: `\n` 또는 `\r\n` (Windows)
   - **Ignore first line**: 체크 (헤더 행 건너뜀)
   - **Encoding**: UTF-8
5. `Import` 클릭

---

### 2.5 가져오기 결과 확인

```sql
SELECT * FROM product WHERE product_id >= 11;
```

```
+------------+-------------+---------------------------+-------+-------+
| product_id | category_id | product_name              | price | stock |
+------------+-------------+---------------------------+-------+-------+
|         11 |           9 | 레고 클래식 블록 1000pcs   | 48000 |    25 |
|         12 |          10 | 강아지 간식 소고기 100g    |  8500 |    60 |
|         13 |          11 | 볼펜 10자루 세트           |  5900 |   150 |
|         14 |           4 | 런닝화 남성용              | 65000 |    30 |
|         15 |           6 | 콜라겐 파우더 30포         | 32000 |    45 |
+------------+-------------+---------------------------+-------+-------+
5 rows in set
```

---

### ✅ 확인 문제 2

1. `LOAD DATA INFILE`에서 `IGNORE 1 LINES`를 지정하는 이유는 무엇인가?
2. CSV 파일을 저장할 때 인코딩을 UTF-8로 설정해야 하는 이유는 무엇인가?
3. Windows 환경에서 파일 경로를 `'C:\temp\data.csv'`로 입력했더니 오류가 발생했다. 원인과 해결 방법을 설명하시오.

> **정답**:  
> 1. CSV 파일의 첫 번째 줄은 컬럼명(헤더)인 경우가 많다. 이를 건너뛰지 않으면 컬럼명 텍스트가 데이터로 삽입되어 오류가 발생한다.  
> 2. 한글이 포함된 CSV 파일을 다른 인코딩(ANSI 등)으로 저장하면 MariaDB에서 읽을 때 한글이 깨진다. `CHARACTER SET utf8mb4`와 파일 인코딩을 일치시켜야 한다.  
> 3. MariaDB에서 경로 구분자로 역슬래시(`\`)를 사용하면 이스케이프 문자로 해석된다. 슬래시(`/`)로 변경하거나 역슬래시를 두 번(`\\`) 써야 한다.  
>    → `'C:/temp/data.csv'` 또는 `'C:\\temp\\data.csv'`

---

## 3. UPDATE — 데이터 수정

### 3.1 기본 문법

```sql
UPDATE 테이블명
SET 컬럼1 = 값1,
    컬럼2 = 값2,
    ...
WHERE 조건;
```

> ⚠️ **`WHERE`를 반드시 지정한다**: WHERE 조건이 없으면 테이블의 **모든 행**이 수정된다.

---

### 3.2 단일 컬럼 수정

```sql
-- 무선 마우스(product_id=1)의 재고를 60으로 수정
UPDATE product
SET stock = 60
WHERE product_id = 1;
```

수정 전 확인 → 수정 → 수정 후 확인 순서를 습관화한다.

```sql
-- 수정 전 확인
SELECT product_id, product_name, stock FROM product WHERE product_id = 1;

-- 수정
UPDATE product SET stock = 60 WHERE product_id = 1;

-- 수정 후 확인
SELECT product_id, product_name, stock FROM product WHERE product_id = 1;
```

---

### 3.3 여러 컬럼 동시 수정

```sql
-- 블루투스 키보드(product_id=3) 가격과 재고를 동시에 수정
UPDATE product
SET price = 49000,
    stock = 35
WHERE product_id = 3;
```

---

### 3.4 연산을 이용한 수정

현재 값을 기준으로 계산하여 수정할 수 있다.

```sql
-- 전자기기(category_id=1) 전체 가격을 5% 인상
UPDATE product
SET price = ROUND(price * 1.05)
WHERE category_id = 1;
```

수정 결과 확인:

```sql
SELECT product_id, product_name, price
FROM product
WHERE category_id = 1;
```

```
+------------+------------------+-------+
| product_id | product_name     | price |
+------------+------------------+-------+
|          1 | 무선 마우스       | 26250 |
|          2 | USB 4포트 허브   | 36750 |
|          3 | 블루투스 키보드   | 51450 |
+------------+------------------+-------+
```

> **ROUND()**: 반올림 함수. 가격 계산 후 소수점이 생기는 것을 방지한다. 자세한 내용은 Chapter 7(내장 함수)에서 다룬다.

---

### 3.5 ORDER BY · LIMIT와 함께 사용

조건에 맞는 행 중 일부만 수정할 때 사용한다.

```sql
-- 재고가 가장 적은 상품 2개의 재고를 0으로 초기화
UPDATE product
SET stock = 0
ORDER BY stock ASC
LIMIT 2;
```

---

### 3.6 UPDATE 전 SELECT로 대상 확인

UPDATE를 실행하기 전에 WHERE 조건이 맞는지 SELECT로 먼저 확인하는 것이 안전하다.

```sql
-- 1단계: 수정 대상 확인
SELECT product_id, product_name, status
FROM orders
WHERE status = '주문완료';

-- 2단계: 확인 후 UPDATE 실행
UPDATE orders
SET status = '처리중'
WHERE status = '주문완료';
```

---

### ✅ 확인 문제 3

1. `product` 테이블에서 재고(`stock`)가 20개 미만인 모든 상품의 재고를 20으로 수정하는 SQL을 작성하시오.
2. `orders` 테이블에서 `order_id = 9`인 주문의 `status`를 `'배송완료'`로 수정하는 SQL을 작성하시오.
3. `customer` 테이블에서 `customer_id = 1`인 고객의 주소를 `'서울특별시 강남구 역삼동 999'`로, 연락처를 `'010-9999-1234'`로 동시에 수정하는 SQL을 작성하시오.

> **정답**:
> ```sql
> -- 1
> UPDATE product
> SET stock = 20
> WHERE stock < 20;
>
> -- 2
> UPDATE orders
> SET status = '배송완료'
> WHERE order_id = 9;
>
> -- 3
> UPDATE customer
> SET address = '서울특별시 강남구 역삼동 999',
>     phone   = '010-9999-1234'
> WHERE customer_id = 1;
> ```

---

## 4. DELETE · TRUNCATE — 데이터 삭제

### 4.1 DELETE 기본 문법

```sql
DELETE FROM 테이블명
WHERE 조건;
```

> ⚠️ **`WHERE`를 반드시 지정한다**: WHERE 조건이 없으면 테이블의 **모든 행**이 삭제된다.

---

### 4.2 단건 DELETE

```sql
-- product_id가 11인 상품 삭제
DELETE FROM product
WHERE product_id = 11;
```

삭제 전 확인:

```sql
-- 삭제 전 확인 (SELECT로 대상 검증)
SELECT * FROM product WHERE product_id = 11;

-- 확인 후 삭제
DELETE FROM product WHERE product_id = 11;

-- 삭제 결과 확인
SELECT COUNT(*) FROM product;
```

---

### 4.3 조건부 DELETE

```sql
-- 재고가 0인 상품 모두 삭제
DELETE FROM product
WHERE stock = 0;

-- 2024년 1월에 생성된 고객 중 주문이 없는 고객 삭제
-- (서브쿼리와 함께 사용 — 5절에서 상세 학습)
```

---

### 4.4 ORDER BY · LIMIT와 함께 사용

```sql
-- 가장 오래된 주문 3건 삭제
DELETE FROM orders
ORDER BY order_date ASC
LIMIT 3;
```

---

### 4.5 TRUNCATE TABLE

테이블의 **모든 데이터를 한번에 삭제**하고 AUTO_INCREMENT를 1로 초기화한다.

```sql
TRUNCATE TABLE 테이블명;
```

```sql
-- product_premium 테이블의 모든 데이터 초기화
TRUNCATE TABLE product_premium;
```

---

### 4.6 DELETE vs TRUNCATE vs DROP 비교

세 명령어는 비슷해 보이지만 명확히 다르다.

| 구분 | DELETE | TRUNCATE | DROP |
|:---:|---|---|---|
| **대상** | 특정 행 또는 전체 행 | 전체 행 | 테이블 자체 |
| **WHERE 사용** | ✅ 가능 | ❌ 불가 | ❌ 불가 |
| **분류** | DML | DDL | DDL |
| **AUTO_INCREMENT** | 유지 | 1로 초기화 | — (테이블 삭제) |
| **속도** | 행마다 로그 기록, 느림 | 전체 삭제, 빠름 | 즉시 삭제, 가장 빠름 |
| **구조(스키마) 유지** | ✅ 유지 | ✅ 유지 | ❌ 테이블 사라짐 |
| **FK 제약 영향** | 받음 | 받음 | 받음 |

**어떤 것을 써야 하는가?**

| 상황 | 권장 명령 |
|---|---|
| 조건에 맞는 일부 데이터만 삭제 | `DELETE WHERE` |
| 테이블 전체 데이터 초기화 (재사용) | `TRUNCATE TABLE` |
| 테이블 자체를 완전히 제거 | `DROP TABLE` |

---

### 4.7 DELETE와 FK 제약

외래 키로 참조되는 부모 행을 삭제하려면, 먼저 자식 테이블의 관련 행을 삭제해야 한다.

```sql
-- orders를 참조하는 order_detail이 있으므로 아래 순서로 삭제
-- 1단계: 자식 먼저 삭제
DELETE FROM order_detail WHERE order_id = 1;

-- 2단계: 부모 삭제
DELETE FROM orders WHERE order_id = 1;
```

`ON DELETE CASCADE`가 설정된 경우에는 부모 행만 삭제해도 자식 행이 자동으로 삭제된다.

---

### ✅ 확인 문제 4

1. `category` 테이블에서 `category_id`가 12 이상인 카테고리를 모두 삭제하는 SQL을 작성하시오.
2. `DELETE`와 `TRUNCATE`의 차이점 세 가지를 설명하시오.
3. 아래 SQL의 문제점을 찾고 수정하시오.
   ```sql
   DELETE FROM orders;   -- 특정 주문만 삭제하려고 했음
   ```

> **정답**:
> ```sql
> -- 1
> DELETE FROM category WHERE category_id >= 12;
>
> -- 2
> ① WHERE로 특정 행만 삭제 가능한 것은 DELETE이고, TRUNCATE는 전체 삭제만 가능하다.
> ② AUTO_INCREMENT를 유지하는 것은 DELETE이고, TRUNCATE는 1로 초기화한다.
> ③ DELETE는 DML로 행마다 로그를 기록하여 느리고, TRUNCATE는 DDL로 빠르다.
>
> -- 3
> WHERE 조건이 없어 테이블의 모든 주문이 삭제된다.
> 삭제할 주문을 특정하는 WHERE 조건을 반드시 추가해야 한다.
> DELETE FROM orders WHERE order_id = ?;
> ```

---

## 5. INSERT · UPDATE · DELETE에서 서브쿼리 활용

서브쿼리를 DML 명령에 결합하면 복잡한 조건으로 데이터를 처리할 수 있다.

### 5.1 INSERT + SELECT (서브쿼리)

Chapter 1에서 배운 `INSERT ... SELECT`는 본질적으로 SELECT 서브쿼리를 INSERT에 결합한 것이다.

```sql
-- 평균 가격 이상인 상품을 product_premium 테이블에 복사
INSERT INTO product_premium (product_id, product_name, price)
SELECT product_id, product_name, price
FROM product
WHERE price >= (SELECT AVG(price) FROM product);
```

서브쿼리 `SELECT AVG(price) FROM product`가 먼저 실행되어 `44700`을 반환하고,  
`WHERE price >= 44700`인 상품이 `product_premium`에 삽입된다.

---

### 5.2 UPDATE + 서브쿼리 (WHERE 절)

서브쿼리 결과를 UPDATE의 조건으로 사용한다.

**예제 1**: 한 번도 주문되지 않은 상품의 재고를 0으로 설정

```sql
-- 1단계: 주문된 상품 ID 확인
SELECT DISTINCT product_id FROM order_detail;
-- 결과: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10

-- 2단계: 주문된 적 없는 상품 ID 확인 (product_id 11~15)
SELECT product_id FROM product
WHERE product_id NOT IN (SELECT DISTINCT product_id FROM order_detail);

-- 3단계: UPDATE 적용
UPDATE product
SET stock = 0
WHERE product_id NOT IN (
    SELECT DISTINCT product_id FROM order_detail
);
```

**예제 2**: 배송완료 주문에 포함된 상품의 재고를 판매 수량만큼 차감

```sql
-- 배송완료된 주문상세의 판매 수량을 집계
SELECT product_id, SUM(quantity) AS 판매수량
FROM order_detail
WHERE order_id IN (
    SELECT order_id FROM orders WHERE status = '배송완료'
)
GROUP BY product_id;
```

---

### 5.3 UPDATE + 서브쿼리 (SET 절)

SET 절에서도 서브쿼리를 사용할 수 있다.

```sql
-- 각 상품의 가격을 해당 카테고리 평균 가격으로 일괄 변경 (예시용)
UPDATE product p
SET price = (
    SELECT ROUND(AVG(p2.price))
    FROM product p2
    WHERE p2.category_id = p.category_id
)
WHERE category_id = 1;
```

> **참고**: 같은 테이블을 UPDATE 대상과 서브쿼리에서 동시에 참조할 때는 별칭(`p`, `p2`)을 사용한다.

---

### 5.4 DELETE + 서브쿼리

서브쿼리 결과를 DELETE의 조건으로 사용한다.

**예제 1**: 주문 이력이 없는 고객 삭제

```sql
-- 주문 이력이 없는 고객 확인
SELECT customer_id, name FROM customer
WHERE customer_id NOT IN (
    SELECT DISTINCT customer_id FROM orders
);
-- 결과: 임서연(9), 윤도현(10)

-- 삭제 실행
DELETE FROM customer
WHERE customer_id NOT IN (
    SELECT DISTINCT customer_id FROM orders
);
```

**예제 2**: 주문이 취소된 주문의 상세 데이터 정리

```sql
-- 취소 상태 주문의 order_detail 삭제
DELETE FROM order_detail
WHERE order_id IN (
    SELECT order_id FROM orders WHERE status = '취소'
);
```

---

### 5.5 서브쿼리와 같은 테이블 — 주의사항

MariaDB에서는 DELETE·UPDATE의 대상 테이블을 서브쿼리에서 직접 참조할 수 없다.

```sql
-- ❌ 오류: DELETE 대상 테이블을 서브쿼리에서 바로 참조
DELETE FROM product
WHERE product_id IN (
    SELECT product_id FROM product WHERE price < 10000
);
```

해결 방법: 서브쿼리를 한 번 더 감싸거나 임시 테이블을 사용한다.

```sql
-- ✅ 해결: 인라인 뷰(FROM 서브쿼리)로 감싸기
DELETE FROM product
WHERE product_id IN (
    SELECT product_id FROM (
        SELECT product_id FROM product WHERE price < 10000
    ) AS temp
);
```

---

### ✅ 확인 문제 5

1. `order_detail` 테이블에서 `'배송완료'` 상태인 주문(`orders.status`)에 포함된 상세 행만 조회하는 서브쿼리 기반 SELECT를 먼저 작성하고, 이를 DELETE로 변환하시오.

2. `product` 테이블에서 재고가 전체 평균 재고보다 많은 상품의 재고를 평균값으로 낮추는 UPDATE를 서브쿼리를 사용하여 작성하시오.

3. `INSERT ... SELECT`와 `UPDATE + 서브쿼리`의 공통점과 차이점을 설명하시오.

> **정답**:
> ```sql
> -- 1-A: SELECT로 먼저 확인
> SELECT * FROM order_detail
> WHERE order_id IN (
>     SELECT order_id FROM orders WHERE status = '배송완료'
> );
>
> -- 1-B: DELETE로 변환
> DELETE FROM order_detail
> WHERE order_id IN (
>     SELECT order_id FROM orders WHERE status = '배송완료'
> );
>
> -- 2
> UPDATE product
> SET stock = (SELECT ROUND(avg_stock) FROM (SELECT AVG(stock) AS avg_stock FROM product) AS t)
> WHERE stock > (SELECT avg_stock FROM (SELECT AVG(stock) AS avg_stock FROM product) AS t2);
>
> -- 3
> 공통점: 두 방법 모두 SELECT 결과를 DML에 활용한다.
> 차이점:
>   INSERT ... SELECT는 SELECT 결과를 새 행으로 추가한다.
>   UPDATE + 서브쿼리는 기존 행의 값을 서브쿼리 결과로 수정하거나,
>   서브쿼리 결과를 조건으로 삼아 수정할 행을 특정한다.
> ```

---

## 장 요약

| 명령어 | 핵심 사항 |
|---|---|
| `INSERT INTO ... VALUES` | 단건/다건 데이터 삽입 |
| `INSERT INTO ... SELECT` | 조회 결과를 다른 테이블로 복사 |
| `ON DUPLICATE KEY UPDATE` | 중복 시 UPDATE로 전환 (Upsert) |
| `LAST_INSERT_ID()` | 마지막으로 삽입된 AUTO_INCREMENT 값 반환 |
| `LOAD DATA INFILE` | CSV 파일에서 일괄 입력 |
| `IGNORE n LINES` | 파일의 헤더(첫 n 줄) 건너뜀 |
| `UPDATE ... SET ... WHERE` | 조건에 맞는 행 수정 (WHERE 필수) |
| `DELETE FROM ... WHERE` | 조건에 맞는 행 삭제 (WHERE 필수) |
| `TRUNCATE TABLE` | 전체 삭제 + AUTO_INCREMENT 초기화 |

**DELETE vs TRUNCATE vs DROP**

| | DELETE | TRUNCATE | DROP |
|:---:|:---:|:---:|:---:|
| WHERE 사용 | ✅ | ❌ | ❌ |
| 구조 유지 | ✅ | ✅ | ❌ |
| AUTO_INCREMENT | 유지 | 초기화 | — |
| 속도 | 느림 | 빠름 | 가장 빠름 |

**DML + 서브쿼리 패턴**

```sql
-- INSERT + 서브쿼리
INSERT INTO 타겟테이블 (컬럼...)
SELECT 컬럼... FROM 소스테이블 WHERE 조건;

-- UPDATE + 서브쿼리
UPDATE 테이블 SET 컬럼 = 값
WHERE 컬럼 IN (SELECT ... FROM 다른테이블 WHERE 조건);

-- DELETE + 서브쿼리
DELETE FROM 테이블
WHERE 컬럼 IN (SELECT ... FROM 다른테이블 WHERE 조건);
```

---

## ⚠️ DML 실수 방지 체크리스트

실무에서 DML 실행 전 반드시 아래를 확인한다.

- [ ] UPDATE / DELETE 전에 SELECT로 대상 행을 먼저 확인했는가?
- [ ] WHERE 조건을 지정했는가? (없으면 전체 행이 영향을 받음)
- [ ] FK 관계를 고려하여 자식 → 부모 순서로 삭제를 계획했는가?
- [ ] 대량 수정/삭제 전에 백업 또는 트랜잭션을 고려했는가?

---

## 다음 장 예고

Chapter 7에서는 MariaDB에서 제공하는 **내장 함수**를 학습한다.  
문자열을 가공하는 문자 함수, 날짜를 계산하는 날짜 함수, 수치를 처리하는 수치 함수,  
그리고 조건에 따라 값을 바꾸는 제어 흐름 함수(IF, CASE)를 실습한다.
