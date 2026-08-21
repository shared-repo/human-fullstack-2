# 단계 2. DB 테이블 설계 및 도메인 클래스 작성

> **목표** : 맛집 노트 앱에 필요한 DB 테이블 2개를 만들고, 테이블과 대응하는 Java 클래스를 작성합니다.
> **소요 시간** : 약 1시간

---

## 2.1 테이블 설계 이해

이 애플리케이션에서 사용하는 테이블은 2개입니다.

```
member (회원)          restaurant (맛집)
─────────────          ──────────────────────
id (PK)          ←─── member_id (FK)
login_id               no (PK)
password               name
name                   category
reg_date               address
                       memo
                       visit_date
                       reg_date
```

- 회원 한 명은 여러 개의 맛집을 등록할 수 있습니다. (1:N 관계)
- `restaurant.member_id` 는 어느 회원이 등록한 맛집인지 연결하는 외래키입니다.

---

## 2.2 DB 테이블 생성

MariaDB에 접속한 뒤 `foodnote` DB를 선택하고 아래 SQL을 순서대로 실행합니다.

### 2.2.1 member 테이블 생성

```sql
USE foodnote;

CREATE TABLE member (
    id        INT          NOT NULL AUTO_INCREMENT,
    login_id  VARCHAR(50)  NOT NULL,
    password  VARCHAR(100) NOT NULL,
    name      VARCHAR(50)  NOT NULL,
    reg_date  DATETIME     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    UNIQUE KEY uq_login_id (login_id)   -- 같은 아이디 중복 가입 방지
);
```

**컬럼 설명**

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | INT, AUTO_INCREMENT | 회원 번호 (자동 증가) |
| `login_id` | VARCHAR(50) | 로그인 아이디 (중복 불가) |
| `password` | VARCHAR(100) | 비밀번호 |
| `name` | VARCHAR(50) | 회원 이름 |
| `reg_date` | DATETIME | 가입일시 (자동 입력) |

### 2.2.2 restaurant 테이블 생성

```sql
CREATE TABLE restaurant (
    no          INT          NOT NULL AUTO_INCREMENT,
    member_id   INT          NOT NULL,
    name        VARCHAR(100) NOT NULL,
    category    VARCHAR(30)  NOT NULL,
    address     VARCHAR(200) NOT NULL,
    memo        VARCHAR(500),
    visit_date  DATE         NOT NULL,
    reg_date    DATETIME     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (no),
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE
);
```

**컬럼 설명**

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `no` | INT, AUTO_INCREMENT | 맛집 번호 (자동 증가) |
| `member_id` | INT | 등록한 회원 번호 (member.id 참조) |
| `name` | VARCHAR(100) | 가게 이름 |
| `category` | VARCHAR(30) | 음식 카테고리 (한식, 중식, ...) |
| `address` | VARCHAR(200) | 가게 주소 |
| `memo` | VARCHAR(500) | 메모 (선택 입력) |
| `visit_date` | DATE | 방문 날짜 |
| `reg_date` | DATETIME | 등록일시 (자동 입력) |

> `ON DELETE CASCADE` : 회원을 삭제하면 그 회원이 등록한 맛집도 함께 삭제됩니다.

### 2.2.3 테이블 생성 확인

아래 SQL로 테이블이 정상적으로 만들어졌는지 확인합니다.

```sql
SHOW TABLES;
```

아래와 같이 두 테이블이 보이면 성공입니다.

```
+---------------------+
| Tables_in_foodnote  |
+---------------------+
| member              |
| restaurant          |
+---------------------+
```

### 2.2.4 테스트 데이터 입력 (선택)

나중에 목록 조회 기능을 테스트할 때 데이터가 없으면 확인하기 어렵습니다.  
미리 테스트 데이터를 넣어 두면 편리합니다.

```sql
-- 테스트 회원 1명 추가
INSERT INTO member (login_id, password, name)
VALUES ('test', '1234', '홍길동');

-- 테스트 맛집 2개 추가 (member.id = 1 로 가정)
INSERT INTO restaurant (member_id, name, category, address, memo, visit_date)
VALUES (1, '맛있는 순두부', '한식', '서울 강남구 역삼동 123', '순두부찌개가 최고', '2025-03-10');

INSERT INTO restaurant (member_id, name, category, address, memo, visit_date)
VALUES (1, '홍콩반점', '중식', '서울 서초구 방배동 456', '짜장면 맛집', '2025-03-15');

-- 입력 확인
SELECT * FROM member;
SELECT * FROM restaurant;
```

---

## 2.3 도메인 클래스 작성

도메인 클래스는 DB 테이블의 한 행(row)을 Java 객체로 표현한 것입니다.  
테이블의 컬럼이 클래스의 필드에 대응됩니다.

```
member 테이블의 한 행     Member 객체
─────────────────────     ──────────────────────
id = 1                →   int id = 1
login_id = "test"     →   String loginId = "test"
password = "1234"     →   String password = "1234"
name = "홍길동"        →   String name = "홍길동"
reg_date = ...        →   LocalDateTime regDate = ...
```

> DB 컬럼명은 `login_id` (스네이크 케이스)이지만 Java 필드명은 `loginId` (카멜 케이스)입니다.  
> `mybatis-config.xml` 에 설정한 `mapUnderscoreToCamelCase=true` 가 자동으로 변환해 줍니다.

### 2.3.1 Member.java 작성

`com.food.domain` 패키지 안에 `Member.java` 파일을 만듭니다.

**파일 만드는 방법**

1. `com.food.domain` 패키지를 오른쪽 클릭합니다.
2. **New → Class** 를 선택합니다.
3. Name 에 `Member` 를 입력하고 **Finish** 를 클릭합니다.

아래 코드를 전체 복사하여 붙여넣습니다.

```java
package com.food.domain;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data               // Getter, Setter, toString, equals, hashCode 자동 생성
@NoArgsConstructor  // 기본 생성자 자동 생성
public class Member {

    private int id;                  // 회원 번호
    private String loginId;          // 로그인 아이디
    private String password;         // 비밀번호
    private String name;             // 이름
    private LocalDateTime regDate;   // 가입일시

}
```

### 2.3.2 Restaurant.java 작성

`com.food.domain` 패키지 안에 `Restaurant.java` 파일을 만듭니다.

```java
package com.food.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Restaurant {

    private int no;                  // 맛집 번호
    private int memberId;            // 등록한 회원 번호
    private String name;             // 가게 이름
    private String category;         // 카테고리
    private String address;          // 주소
    private String memo;             // 메모
    private LocalDate visitDate;     // 방문일
    private LocalDateTime regDate;   // 등록일시

}
```

> **Lombok 어노테이션 설명**
>
> - `@Data` : `getNo()`, `setNo()`, `getName()`, `setName()` 같은 Getter/Setter를 자동으로 만들어 줍니다. 직접 작성하면 코드가 매우 길어지지만 `@Data` 하나로 해결됩니다.
> - `@NoArgsConstructor` : `new Restaurant()` 처럼 인수 없이 객체를 만들 수 있는 기본 생성자를 자동으로 만들어 줍니다. MyBatis가 결과를 객체로 변환할 때 기본 생성자를 사용합니다.

> ⚠️ **Lombok이 적용되지 않는 경우**
>
> Eclipse에 Lombok 플러그인이 설치되지 않았다면 `@Data` 에 오류가 표시됩니다.  
> 이 경우 강사에게 문의하거나, 아래와 같이 Getter/Setter를 직접 작성합니다.
>
> ```java
> // @Data 없이 직접 작성하는 방식 (Lombok 미사용 시)
> public class Member {
>     private int id;
>     private String loginId;
>     // ... 필드 선언
>
>     public int getId() { return id; }
>     public void setId(int id) { this.id = id; }
>     public String getLoginId() { return loginId; }
>     public void setLoginId(String loginId) { this.loginId = loginId; }
>     // ... 나머지 Getter/Setter
> }
> ```

---

## 2.4 mybatis-config.xml 에 typeAlias 등록

단계 1에서 주석 처리했던 `typeAlias` 부분을 활성화합니다.  
`src/main/resources/mybatis-config.xml` 파일을 열고 아래와 같이 수정합니다.

**수정 전**

```xml
<typeAliases>
    <!-- <typeAlias type="com.food.domain.Member"     alias="Member"/> -->
    <!-- <typeAlias type="com.food.domain.Restaurant" alias="Restaurant"/> -->
</typeAliases>
```

**수정 후** (주석 `<!--` `-->` 제거)

```xml
<typeAliases>
    <typeAlias type="com.food.domain.Member"     alias="Member"/>
    <typeAlias type="com.food.domain.Restaurant" alias="Restaurant"/>
</typeAliases>
```

> typeAlias를 등록하면 Mapper XML 에서 전체 패키지명 대신 짧은 이름을 사용할 수 있습니다.
>
> ```xml
> <!-- 등록 전: 긴 이름 사용 -->
> <select id="selectOne" resultType="com.food.domain.Restaurant">
>
> <!-- 등록 후: 짧은 이름 사용 -->
> <select id="selectOne" resultType="Restaurant">
> ```

---

## 2.5 최종 확인

### 2.5.1 작성된 파일 확인

아래 파일이 모두 존재하는지 확인합니다.

```
src/main/java/com/food/domain
├── Member.java       ✅
└── Restaurant.java   ✅

src/main/resources
├── mybatis-config.xml   ✅ (typeAlias 주석 해제 완료)
└── logback.xml
```

### 2.5.2 서버 재시작 확인

서버를 재시작했을 때 콘솔에 오류가 없는지 확인합니다.

1. Console 탭 하단의 빨간 정지 버튼을 눌러 서버를 중지합니다.
2. 프로젝트를 오른쪽 클릭 → **Run As → Run on Server** 로 재시작합니다.
3. `Server startup in [xxxx] milliseconds` 메시지가 보이면 성공입니다.

---

## ✅ 단계 2 완료 체크리스트

- [ ] MariaDB에 `member` 테이블이 생성됐습니다.
- [ ] MariaDB에 `restaurant` 테이블이 생성됐습니다.
- [ ] `Member.java` 파일이 `com.food.domain` 패키지에 작성됐습니다.
- [ ] `Restaurant.java` 파일이 `com.food.domain` 패키지에 작성됐습니다.
- [ ] `mybatis-config.xml` 의 `<typeAlias>` 주석이 해제됐습니다.
- [ ] 서버를 재시작했을 때 콘솔에 오류가 없습니다.

모든 항목이 체크됐으면 **단계 3. 회원가입 구현**으로 이동합니다.
