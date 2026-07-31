# Chapter 4 DDL — 데이터 정의

> **학습 시간**: 약 4시간  
> **학습 대상**: 데이터베이스를 처음 접하는 학습자  
> **실습 DB**: 온라인 쇼핑몰 (Chapter 3에서 설계)

---

## 학습 목표

이 장을 마치면 다음을 할 수 있다.

- DDL의 개념과 구성 명령어를 설명할 수 있다.
- 데이터베이스를 생성·선택·삭제할 수 있다.
- MariaDB의 주요 데이터 유형을 이해하고 적절히 선택할 수 있다.
- 테이블을 생성·삭제하고 구조를 확인할 수 있다.
- ALTER TABLE로 테이블 구조를 변경할 수 있다.
- 주요 제약 조건(PK, FK, NOT NULL, UNIQUE, DEFAULT)을 테이블에 적용할 수 있다.

---

## DDL이란?

**DDL(Data Definition Language, 데이터 정의 언어)**은 데이터베이스와 테이블의 **구조를 정의**하는 SQL 명령어 집합이다.

| 명령어 | 기능 |
|---|---|
| `CREATE` | 데이터베이스·테이블 생성 |
| `ALTER` | 테이블 구조 변경 |
| `DROP` | 데이터베이스·테이블 삭제 |
| `TRUNCATE` | 테이블의 모든 데이터 삭제 (구조는 유지) |

> **DML과의 차이**: DDL은 **구조(틀)**를 다루고, DML(Chapter 6)은 **데이터(내용)**를 다룬다.  
> 집에 비유하면 DDL은 집의 뼈대·방 구조를 만드는 것이고, DML은 그 방에 가구를 넣고 옮기고 치우는 것이다.

---

## 1. 데이터베이스 생성 · 삭제

### 1.1 데이터베이스 생성 — CREATE DATABASE

```sql
CREATE DATABASE 데이터베이스명;
```

쇼핑몰 실습 데이터베이스를 생성한다.

```sql
CREATE DATABASE shopdb;
```

**문자 인코딩을 명시하여 생성 (권장)**

한글 데이터를 올바르게 저장하려면 문자 인코딩을 `utf8mb4`로 설정하는 것이 좋다.

```sql
CREATE DATABASE shopdb
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;
```

| 옵션 | 설명 |
|---|---|
| `CHARACTER SET utf8mb4` | 한글·이모지 등 다국어를 지원하는 인코딩 |
| `COLLATE utf8mb4_general_ci` | 문자열 비교 시 대소문자를 구분하지 않음 (`ci` = case insensitive) |

> **utf8 vs utf8mb4**: MariaDB의 `utf8`은 3바이트만 지원하여 일부 특수 문자(이모지 등)가 깨질 수 있다. `utf8mb4`는 4바이트까지 지원하므로 실무에서는 `utf8mb4` 사용을 권장한다.

---

### 1.2 데이터베이스 목록 조회 — SHOW DATABASES

```sql
SHOW DATABASES;
```

실행 결과:
```
+--------------------+
| Database           |
+--------------------+
| information_schema |
| mysql              |
| performance_schema |
| shopdb             |
| sys                |
+--------------------+
```

---

### 1.3 데이터베이스 선택 — USE

생성한 데이터베이스를 사용하려면 먼저 선택해야 한다.

```sql
USE shopdb;
```

실행 결과:
```
Database changed
```

HeidiSQL에서는 왼쪽 트리에서 데이터베이스를 더블클릭하면 자동으로 선택된다.

현재 선택된 데이터베이스 확인:

```sql
SELECT DATABASE();
```

실행 결과:
```
+------------+
| DATABASE() |
+------------+
| shopdb     |
+------------+
```

---

### 1.4 데이터베이스 삭제 — DROP DATABASE

```sql
DROP DATABASE 데이터베이스명;
```

```sql
DROP DATABASE shopdb;
```

> ⚠️ **주의**: `DROP DATABASE`는 데이터베이스 안의 모든 테이블과 데이터를 **영구적으로 삭제**한다. 복구할 수 없으므로 반드시 신중하게 실행한다.

데이터베이스가 존재하는 경우에만 삭제(오류 방지):

```sql
DROP DATABASE IF EXISTS shopdb;
```

---

### ✅ 확인 문제 1

1. 한글 데이터를 저장할 수 있도록 `mydb`라는 데이터베이스를 `utf8mb4` 인코딩으로 생성하는 SQL을 작성하시오.
2. `USE` 명령의 역할은 무엇인가?
3. `DROP DATABASE`와 `DROP DATABASE IF EXISTS`의 차이는 무엇인가?

> **정답**:
> ```sql
> 1.
> CREATE DATABASE mydb
>   CHARACTER SET utf8mb4
>   COLLATE utf8mb4_general_ci;
> ```
> 2. 이후 실행하는 SQL이 어느 데이터베이스를 대상으로 하는지 지정한다. USE를 실행하지 않으면 테이블 이름 앞에 매번 데이터베이스명을 붙여야 한다.  
> 3. `DROP DATABASE`는 해당 데이터베이스가 없으면 오류가 발생하고, `DROP DATABASE IF EXISTS`는 없어도 오류 없이 정상 종료된다.

---

## 2. 데이터 유형

테이블의 각 컬럼에는 저장할 수 있는 데이터의 종류(데이터 유형)를 지정해야 한다.  
데이터 유형을 잘 선택하면 저장 공간을 절약하고 잘못된 데이터 입력을 막을 수 있다.

---

### 2.1 숫자형

| 유형 | 저장 공간 | 범위 | 사용 예 |
|---|---|---|---|
| `TINYINT` | 1바이트 | -128 ~ 127 (UNSIGNED: 0 ~ 255) | 나이, 등급 |
| `SMALLINT` | 2바이트 | -32,768 ~ 32,767 | 소규모 수량 |
| `INT` | 4바이트 | -21억 ~ 21억 | 일반 정수, ID |
| `BIGINT` | 8바이트 | 약 -922경 ~ 922경 | 매우 큰 수, 주민번호 |
| `DECIMAL(p, s)` | 가변 | 정밀한 소수 | 금액, 비율 |
| `FLOAT` | 4바이트 | 근사값 소수 | 과학적 계산 |
| `DOUBLE` | 8바이트 | 근사값 소수 | 정밀한 부동소수점 |

**DECIMAL 사용법**

```sql
DECIMAL(10, 2)
-- 전체 자릿수 10자리, 소수점 이하 2자리
-- 예: 99999999.99
```

> **금액은 반드시 DECIMAL 사용**: `FLOAT`이나 `DOUBLE`은 근사값이므로 계산 시 미세한 오차가 발생할 수 있다. 금융 데이터는 정확한 값을 저장하는 `DECIMAL`을 사용해야 한다.

**UNSIGNED**

음수가 필요 없는 컬럼(수량, ID 등)에 `UNSIGNED`를 붙이면 양수 범위가 두 배로 늘어난다.

```sql
INT UNSIGNED   -- 0 ~ 4,294,967,295
```

---

### 2.2 문자형

| 유형 | 최대 길이 | 특징 | 사용 예 |
|---|---|---|---|
| `CHAR(n)` | 255자 | 고정 길이. 남은 공간은 공백으로 채움 | 국가코드, 성별코드 |
| `VARCHAR(n)` | 65,535자 | 가변 길이. 실제 데이터 크기만 사용 | 이름, 이메일, 주소 |
| `TEXT` | 65,535자 | 긴 텍스트, 인덱스 제한 있음 | 게시글, 상품설명 |
| `MEDIUMTEXT` | 16MB | 매우 긴 텍스트 | 소설, 로그 |
| `LONGTEXT` | 4GB | 초대용량 텍스트 | 특수 목적 |

**CHAR vs VARCHAR 비교**

| 항목 | CHAR(10) | VARCHAR(10) |
|---|---|---|
| 저장 데이터 | `'홍'` (1자) | `'홍'` (1자) |
| 실제 저장 크기 | 10자 (나머지 9자 공백) | 1자 + 길이 정보 |
| 속도 | 고정 길이라 검색이 빠름 | 길이가 가변적이라 약간 느림 |
| 적합한 데이터 | 항상 길이가 일정한 코드값 | 길이가 다양한 일반 문자열 |

```sql
phone  CHAR(13)     -- '010-1234-5678' 항상 13자리
email  VARCHAR(100) -- 길이가 다양한 이메일
```

---

### 2.3 날짜·시간형

| 유형 | 형식 | 범위 | 사용 예 |
|---|---|---|---|
| `DATE` | YYYY-MM-DD | 1000-01-01 ~ 9999-12-31 | 생년월일, 주문일 |
| `TIME` | HH:MM:SS | -838:59:59 ~ 838:59:59 | 운영 시간 |
| `DATETIME` | YYYY-MM-DD HH:MM:SS | 1000-01-01 ~ 9999-12-31 | 등록일시, 수정일시 |
| `TIMESTAMP` | YYYY-MM-DD HH:MM:SS | 1970-01-01 ~ 2038-01-19 | 자동 갱신 시간 기록 |
| `YEAR` | YYYY | 1901 ~ 2155 | 연도만 저장 |

**DATETIME vs TIMESTAMP**

| 항목 | DATETIME | TIMESTAMP |
|---|---|---|
| 저장 방식 | 입력값 그대로 저장 | UTC로 변환하여 저장 |
| 시간대 영향 | 없음 | 있음 (서버 시간대 따름) |
| 자동 갱신 | 불가 | `ON UPDATE CURRENT_TIMESTAMP` 가능 |
| 표현 범위 | 넓음 | 2038년까지 |
| 권장 사용처 | 생년월일, 역사적 날짜 | 최근 수정 시간 자동 기록 |

---

### 2.4 기타 유형

| 유형 | 설명 | 사용 예 |
|---|---|---|
| `BOOLEAN` | TRUE(1) / FALSE(0) | 활성화 여부, 공개 여부 |
| `ENUM('값1','값2',...)` | 지정한 값 중 하나만 저장 가능 | 성별('M','F'), 상태값 |
| `JSON` | JSON 형식 데이터 저장 | 설정 정보, 부가 속성 |

```sql
-- ENUM 사용 예
gender  ENUM('M', 'F', 'N')
status  ENUM('주문완료', '배송중', '배송완료', '취소')
```

---

### ✅ 확인 문제 2

다음 각 데이터를 저장하기에 가장 적합한 데이터 유형을 고르시오.

| 저장할 데이터 | 보기 |
|---|---|
| 1. 상품 가격 (최대 1억 원, 원 단위) | INT / DECIMAL(10,0) / VARCHAR(20) |
| 2. 고객 이메일 주소 | CHAR(100) / VARCHAR(100) / TEXT |
| 3. 성별 코드 ('M' 또는 'F') | VARCHAR(1) / CHAR(1) / INT |
| 4. 주문 등록 일시 | DATE / DATETIME / TIMESTAMP |
| 5. 회원 탈퇴 여부 (탈퇴/미탈퇴) | INT / BOOLEAN / VARCHAR(10) |

> **정답**:  
> 1. `DECIMAL(10,0)` — 금액은 정확한 값을 저장하는 DECIMAL 권장  
> 2. `VARCHAR(100)` — 이메일 길이가 가변적이므로 VARCHAR  
> 3. `CHAR(1)` — 항상 1자리 고정값이므로 CHAR  
> 4. `DATETIME` — 날짜와 시간이 함께 필요하고 2038년 이후도 저장해야 하므로  
> 5. `BOOLEAN` — 참/거짓 두 가지 값만 가지므로

---

## 3. 테이블 생성 · 삭제

### 3.1 테이블 생성 — CREATE TABLE

```sql
CREATE TABLE 테이블명 (
    컬럼명1  데이터유형  [제약조건],
    컬럼명2  데이터유형  [제약조건],
    ...
    [테이블 레벨 제약조건]
);
```

**실습: 쇼핑몰 테이블 생성**

먼저 데이터베이스를 선택한다.

```sql
USE shopdb;
```

**① category 테이블**

```sql
CREATE TABLE category (
    category_id    INT             NOT NULL AUTO_INCREMENT,
    category_name  VARCHAR(50)     NOT NULL,
    PRIMARY KEY (category_id),
    UNIQUE KEY uq_category_name (category_name)
);
```

**② product 테이블**

```sql
CREATE TABLE product (
    product_id    INT              NOT NULL AUTO_INCREMENT,
    category_id   INT              NOT NULL,
    product_name  VARCHAR(100)     NOT NULL,
    price         DECIMAL(10, 0)   NOT NULL,
    stock         INT              NOT NULL DEFAULT 0,
    description   TEXT,
    created_at    DATETIME         DEFAULT NOW(),
    PRIMARY KEY (product_id),
    FOREIGN KEY (category_id) REFERENCES category (category_id)
);
```

**③ customer 테이블**

```sql
CREATE TABLE customer (
    customer_id  INT           NOT NULL AUTO_INCREMENT,
    name         VARCHAR(50)   NOT NULL,
    email        VARCHAR(100)  NOT NULL,
    password     VARCHAR(255)  NOT NULL,
    phone        CHAR(13),
    address      VARCHAR(200),
    created_at   DATETIME      DEFAULT NOW(),
    PRIMARY KEY (customer_id),
    UNIQUE KEY uq_customer_email (email)
);
```

**④ orders 테이블**

```sql
CREATE TABLE orders (
    order_id          INT           NOT NULL AUTO_INCREMENT,
    customer_id       INT           NOT NULL,
    order_date        DATETIME      DEFAULT NOW(),
    shipping_address  VARCHAR(200)  NOT NULL,
    payment_method    VARCHAR(20)   NOT NULL,
    status            VARCHAR(20)   DEFAULT '주문완료',
    PRIMARY KEY (order_id),
    FOREIGN KEY (customer_id) REFERENCES customer (customer_id)
);
```

**⑤ order_detail 테이블**

```sql
CREATE TABLE order_detail (
    detail_id   INT              NOT NULL AUTO_INCREMENT,
    order_id    INT              NOT NULL,
    product_id  INT              NOT NULL,
    quantity    INT              NOT NULL,
    unit_price  DECIMAL(10, 0)   NOT NULL,
    PRIMARY KEY (detail_id),
    FOREIGN KEY (order_id)   REFERENCES orders  (order_id),
    FOREIGN KEY (product_id) REFERENCES product (product_id)
);
```

> **테이블 생성 순서**: 외래 키(FK)로 참조하는 테이블을 먼저 생성해야 한다.  
> `product`는 `category`를 참조하므로 `category` → `product` 순서로 생성한다.  
> 위 순서: `category` → `product` → `customer` → `orders` → `order_detail`

---

### 3.2 테이블 목록 조회 — SHOW TABLES

```sql
SHOW TABLES;
```

실행 결과:
```
+------------------+
| Tables_in_shopdb |
+------------------+
| category         |
| customer         |
| order_detail     |
| orders           |
| product          |
+------------------+
```

---

### 3.3 테이블 구조 확인 — DESC

```sql
DESC 테이블명;
```

```sql
DESC product;
```

실행 결과:
```
+--------------+---------------+------+-----+---------+----------------+
| Field        | Type          | Null | Key | Default | Extra          |
+--------------+---------------+------+-----+---------+----------------+
| product_id   | int(11)       | NO   | PRI | NULL    | auto_increment |
| category_id  | int(11)       | NO   | MUL | NULL    |                |
| product_name | varchar(100)  | NO   |     | NULL    |                |
| price        | decimal(10,0) | NO   |     | NULL    |                |
| stock        | int(11)       | NO   |     | 0       |                |
| description  | text          | YES  |     | NULL    |                |
| created_at   | datetime      | YES  |     | NULL    |                |
+--------------+---------------+------+-----+---------+----------------+
```

| 컬럼 | 설명 |
|---|---|
| `Field` | 컬럼명 |
| `Type` | 데이터 유형 |
| `Null` | NULL 허용 여부 (YES/NO) |
| `Key` | PRI(기본 키), UNI(고유), MUL(외래 키) |
| `Default` | 기본값 |
| `Extra` | 추가 정보 (auto_increment 등) |

---

### 3.4 테이블 생성 SQL 확인 — SHOW CREATE TABLE

```sql
SHOW CREATE TABLE product;
```

테이블을 생성할 때 사용한 SQL 전체를 확인할 수 있다. 다른 서버에 동일한 테이블을 만들 때 유용하다.

---

### 3.5 테이블 삭제 — DROP TABLE

```sql
DROP TABLE 테이블명;
```

```sql
DROP TABLE IF EXISTS order_detail;
```

> ⚠️ **주의**: 외래 키로 참조되는 테이블은 삭제할 수 없다.  
> `order_detail`이 `orders`를 참조하므로, `orders`를 먼저 삭제하려면 `order_detail`을 먼저 삭제해야 한다.  
> 삭제 순서: `order_detail` → `orders` → `customer`, `product` → `category`

---

### ✅ 확인 문제 3

1. 아래 조건으로 `member` 테이블을 생성하는 SQL을 작성하시오.
   - 회원번호 (INT, 기본 키, 자동 증가)
   - 이름 (VARCHAR 30자, NULL 불가)
   - 이메일 (VARCHAR 100자, NULL 불가, 중복 불가)
   - 가입일 (DATE, 기본값: 현재 날짜)

2. 외래 키가 있는 테이블을 삭제할 때 주의해야 할 점은 무엇인가?

> **정답**:
> ```sql
> 1.
> CREATE TABLE member (
>     member_id   INT           NOT NULL AUTO_INCREMENT,
>     name        VARCHAR(30)   NOT NULL,
>     email       VARCHAR(100)  NOT NULL,
>     joined_date DATE          DEFAULT (CURRENT_DATE),
>     PRIMARY KEY (member_id),
>     UNIQUE KEY uq_member_email (email)
> );
> ```
> 2. 다른 테이블에서 외래 키로 참조되는 테이블은 바로 삭제할 수 없다. 참조하는 테이블(자식)을 먼저 삭제한 뒤, 참조되는 테이블(부모)을 삭제해야 한다.

---

## 4. 테이블 구조 변경 — ALTER TABLE

테이블을 생성한 후 컬럼을 추가·수정·삭제하거나 테이블 이름을 변경할 수 있다.

### 4.1 컬럼 추가 — ADD COLUMN

```sql
ALTER TABLE 테이블명
    ADD COLUMN 컬럼명 데이터유형 [제약조건] [AFTER 기존컬럼명];
```

`customer` 테이블에 포인트 컬럼을 추가한다.

```sql
ALTER TABLE customer
    ADD COLUMN point INT NOT NULL DEFAULT 0 AFTER address;
```

여러 컬럼을 한 번에 추가할 수도 있다.

```sql
ALTER TABLE customer
    ADD COLUMN gender    CHAR(1)     AFTER name,
    ADD COLUMN birthdate DATE        AFTER gender;
```

---

### 4.2 컬럼 속성 변경 — MODIFY COLUMN

데이터 유형이나 제약 조건을 변경한다. 컬럼명은 유지된다.

```sql
ALTER TABLE 테이블명
    MODIFY COLUMN 컬럼명 새데이터유형 [새제약조건];
```

`product` 테이블의 `description` 컬럼을 `TEXT`에서 `MEDIUMTEXT`로 변경한다.

```sql
ALTER TABLE product
    MODIFY COLUMN description MEDIUMTEXT;
```

> ⚠️ **주의**: 기존 데이터가 있을 때 데이터 유형을 변경하면 데이터가 손실될 수 있다. 예를 들어 `VARCHAR(100)`을 `VARCHAR(10)`으로 줄이면 10자를 초과하는 기존 데이터가 잘린다.

---

### 4.3 컬럼명과 속성 동시 변경 — CHANGE COLUMN

컬럼명과 데이터 유형을 동시에 변경한다.

```sql
ALTER TABLE 테이블명
    CHANGE COLUMN 기존컬럼명 새컬럼명 새데이터유형 [새제약조건];
```

`customer` 테이블의 `phone`을 `phone_number`로 이름을 바꾸고 유형도 변경한다.

```sql
ALTER TABLE customer
    CHANGE COLUMN phone phone_number VARCHAR(20);
```

컬럼명만 변경하고 싶을 때는 `RENAME COLUMN`을 사용할 수 있다.

```sql
ALTER TABLE customer
    RENAME COLUMN phone_number TO phone;
```

---

### 4.4 컬럼 삭제 — DROP COLUMN

```sql
ALTER TABLE 테이블명
    DROP COLUMN 컬럼명;
```

`customer` 테이블에서 `gender` 컬럼을 삭제한다.

```sql
ALTER TABLE customer
    DROP COLUMN gender;
```

> ⚠️ **주의**: `DROP COLUMN`으로 삭제된 컬럼과 그 데이터는 복구할 수 없다.

---

### 4.5 테이블 이름 변경 — RENAME TABLE

```sql
RENAME TABLE 기존테이블명 TO 새테이블명;
```

```sql
RENAME TABLE member TO users;
```

`ALTER TABLE`로도 가능하다.

```sql
ALTER TABLE users RENAME TO member;
```

---

### 4.6 ALTER TABLE 요약

| 작업 | SQL |
|---|---|
| 컬럼 추가 | `ALTER TABLE t ADD COLUMN c 유형;` |
| 컬럼 속성 변경 | `ALTER TABLE t MODIFY COLUMN c 새유형;` |
| 컬럼명+속성 변경 | `ALTER TABLE t CHANGE COLUMN 구c 신c 새유형;` |
| 컬럼명만 변경 | `ALTER TABLE t RENAME COLUMN 구c TO 신c;` |
| 컬럼 삭제 | `ALTER TABLE t DROP COLUMN c;` |
| 테이블명 변경 | `RENAME TABLE 구t TO 신t;` |

---

### ✅ 확인 문제 4

`product` 테이블에 대해 아래 작업을 수행하는 SQL을 작성하시오.

1. `discount_rate` 컬럼을 `DECIMAL(5,2)`, 기본값 0으로 `price` 뒤에 추가하시오.
2. `stock` 컬럼의 데이터 유형을 `SMALLINT`로 변경하시오.
3. `description` 컬럼을 삭제하시오.

> **정답**:
> ```sql
> 1.
> ALTER TABLE product
>     ADD COLUMN discount_rate DECIMAL(5,2) DEFAULT 0 AFTER price;
>
> 2.
> ALTER TABLE product
>     MODIFY COLUMN stock SMALLINT NOT NULL DEFAULT 0;
>
> 3.
> ALTER TABLE product
>     DROP COLUMN description;
> ```

---

## 5. 제약 조건 (Constraints)

**제약 조건**이란 테이블에 저장되는 데이터의 **무결성(정확성·일관성)**을 보장하기 위한 규칙이다.  
잘못된 데이터가 입력되는 것을 데이터베이스 수준에서 막아 준다.

---

### 5.1 PRIMARY KEY (기본 키)

- 각 행을 유일하게 식별하는 컬럼
- **NULL 불가**, **중복 불가**
- 테이블당 하나만 설정 가능 (복합 기본 키는 예외)

```sql
-- 컬럼 레벨 정의
CREATE TABLE category (
    category_id  INT  NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ...
);

-- 테이블 레벨 정의 (복합 기본 키 시 필수)
CREATE TABLE order_detail (
    order_id    INT  NOT NULL,
    product_id  INT  NOT NULL,
    ...
    PRIMARY KEY (order_id, product_id)
);
```

**AUTO_INCREMENT**

기본 키에 자주 사용하며, 새 행이 삽입될 때마다 값이 자동으로 1씩 증가한다.

```sql
customer_id  INT  NOT NULL AUTO_INCREMENT
-- 첫 번째 삽입: 1
-- 두 번째 삽입: 2
-- 세 번째 삽입: 3 ...
```

---

### 5.2 FOREIGN KEY (외래 키)

- 다른 테이블의 기본 키를 참조하는 컬럼
- 참조 무결성: 존재하지 않는 값을 외래 키로 입력할 수 없다

```sql
FOREIGN KEY (컬럼명) REFERENCES 참조테이블명 (참조컬럼명)
    [ON DELETE 옵션]
    [ON UPDATE 옵션]
```

**참조 동작 옵션**

부모 테이블의 데이터가 삭제·변경될 때 자식 테이블을 어떻게 처리할지 결정한다.

| 옵션 | 삭제/변경 시 동작 |
|---|---|
| `RESTRICT` (기본값) | 자식 데이터가 있으면 부모 삭제·변경 불가 |
| `CASCADE` | 부모 삭제 시 자식도 함께 삭제, 부모 변경 시 자식도 변경 |
| `SET NULL` | 부모 삭제·변경 시 자식의 FK 컬럼을 NULL로 변경 |
| `NO ACTION` | RESTRICT와 동일 |

```sql
-- 주문이 삭제되면 주문상세도 함께 삭제
CREATE TABLE order_detail (
    ...
    FOREIGN KEY (order_id) REFERENCES orders (order_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);
```

---

### 5.3 NOT NULL

컬럼에 NULL 값이 입력되는 것을 금지한다.  
필수 입력 항목에 사용한다.

```sql
name   VARCHAR(50)  NOT NULL,   -- 이름은 반드시 입력
phone  CHAR(13),                -- 전화번호는 없어도 됨 (NULL 허용)
```

---

### 5.4 UNIQUE

동일한 값이 두 번 이상 저장되는 것을 금지한다.  
NULL은 중복으로 간주하지 않으므로 여러 행에 NULL이 있어도 UNIQUE 위반이 아니다.

```sql
-- 컬럼 레벨
email  VARCHAR(100)  NOT NULL  UNIQUE,

-- 테이블 레벨 (이름 지정 권장)
UNIQUE KEY uq_customer_email (email)
```

---

### 5.5 DEFAULT

컬럼에 값을 입력하지 않았을 때 자동으로 채워지는 기본값을 지정한다.

```sql
stock       INT       NOT NULL  DEFAULT 0,
created_at  DATETIME            DEFAULT NOW(),
status      VARCHAR(20)         DEFAULT '주문완료'
```

---

### 5.6 CHECK

컬럼에 입력할 수 있는 값의 범위나 조건을 지정한다. (MariaDB 10.2.1 이상)

```sql
-- 가격은 0보다 커야 함
price    DECIMAL(10,0)  NOT NULL  CHECK (price > 0),

-- 수량은 1 이상이어야 함
quantity INT            NOT NULL  CHECK (quantity >= 1)
```

---

### 5.7 제약 조건 조회

테이블에 설정된 제약 조건을 확인한다.

```sql
SELECT
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE,
    TABLE_NAME
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'shopdb';
```

---

### 5.8 제약 조건 추가·삭제 (ALTER TABLE)

이미 생성된 테이블에 제약 조건을 추가하거나 삭제할 수 있다.

```sql
-- UNIQUE 제약 조건 추가
ALTER TABLE customer
    ADD UNIQUE KEY uq_customer_email (email);

-- FOREIGN KEY 추가
ALTER TABLE product
    ADD FOREIGN KEY (category_id) REFERENCES category (category_id);

-- 제약 조건 삭제 (이름으로 삭제)
ALTER TABLE customer
    DROP INDEX uq_customer_email;

-- 외래 키 삭제
ALTER TABLE product
    DROP FOREIGN KEY fk_product_category;
```

---

### 5.9 제약 조건 요약

| 제약 조건 | NULL 허용 | 중복 허용 | 설명 |
|:---:|:---:|:---:|---|
| `PRIMARY KEY` | ❌ | ❌ | 행의 유일 식별자 |
| `FOREIGN KEY` | ✅ | ✅ | 다른 테이블의 PK 참조 |
| `NOT NULL` | ❌ | ✅ | NULL 입력 금지 |
| `UNIQUE` | ✅ | ❌ | 중복 입력 금지 (NULL 예외) |
| `DEFAULT` | — | — | 값 미입력 시 기본값 |
| `CHECK` | — | — | 입력값 조건 검사 |
| `AUTO_INCREMENT` | ❌ | ❌ | 자동 증가 (PK와 함께 사용) |

---

### ✅ 확인 문제 5

1. `PRIMARY KEY`와 `UNIQUE`의 차이점을 설명하시오.
2. `FOREIGN KEY`의 `ON DELETE CASCADE` 옵션은 어떤 동작을 하는가?
3. 아래 테이블 생성 SQL의 오류를 찾고 수정하시오.

```sql
CREATE TABLE product (
    product_id    INT,
    product_name  VARCHAR(100),
    price         DECIMAL(10,0)  DEFAULT -1,
    stock         INT            DEFAULT NULL,
    PRIMARY KEY (product_name)   -- 오류?
);
```

> **정답**:  
> 1. PRIMARY KEY는 NULL을 허용하지 않고 테이블당 하나만 설정 가능하다. UNIQUE는 NULL을 허용하며 한 테이블에 여러 개 설정 가능하다.  
> 2. 부모 테이블의 행이 삭제될 때 그 행을 참조하는 자식 테이블의 모든 행도 함께 자동으로 삭제된다.  
> 3. 문제점이 두 가지다.
>    - `price`의 DEFAULT -1: 가격이 음수가 되어 데이터 의미상 오류 (CHECK 제약으로 방지 권장)
>    - `PRIMARY KEY (product_name)`: 상품명은 중복될 수 있으므로 PK로 부적절. `product_id`를 PK로 설정해야 한다.
>    ```sql
>    CREATE TABLE product (
>        product_id    INT            NOT NULL AUTO_INCREMENT,
>        product_name  VARCHAR(100)   NOT NULL,
>        price         DECIMAL(10,0)  NOT NULL CHECK (price >= 0),
>        stock         INT            NOT NULL DEFAULT 0,
>        PRIMARY KEY (product_id)
>    );
>    ```

---

## 장 요약

| 명령어 | 용도 |
|---|---|
| `CREATE DATABASE` | 데이터베이스 생성 |
| `USE` | 작업할 데이터베이스 선택 |
| `DROP DATABASE` | 데이터베이스 삭제 |
| `CREATE TABLE` | 테이블 생성 |
| `DESC` | 테이블 구조 확인 |
| `SHOW TABLES` | 테이블 목록 조회 |
| `DROP TABLE` | 테이블 삭제 |
| `ALTER TABLE ... ADD COLUMN` | 컬럼 추가 |
| `ALTER TABLE ... MODIFY COLUMN` | 컬럼 속성 변경 |
| `ALTER TABLE ... CHANGE COLUMN` | 컬럼명·속성 변경 |
| `ALTER TABLE ... DROP COLUMN` | 컬럼 삭제 |
| `RENAME TABLE` | 테이블명 변경 |

**주요 제약 조건**

| 제약 조건 | 핵심 |
|---|---|
| `PRIMARY KEY` | 유일 식별자, NULL·중복 불가 |
| `AUTO_INCREMENT` | PK 값 자동 증가 |
| `FOREIGN KEY` | 참조 무결성, 부모 테이블의 PK 참조 |
| `NOT NULL` | 필수 입력 보장 |
| `UNIQUE` | 중복 방지 |
| `DEFAULT` | 미입력 시 기본값 |
| `CHECK` | 입력 범위·조건 검사 |

---

## 다음 장 예고

Chapter 5에서는 생성된 테이블에서 데이터를 조회하는 **SELECT** 문을 학습한다.  
기본 조회부터 시작하여 조건 검색(WHERE), 집계 함수, GROUP BY, JOIN, 서브쿼리까지 다양한 조회 기법을 실습한다.  
실습에는 이번 장에서 생성한 쇼핑몰 DB(shopdb)를 사용한다.
