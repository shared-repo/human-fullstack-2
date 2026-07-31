# Chapter 10 JDBC 데이터베이스 연동

> **학습 시간**: 약 8시간 (강의 4시간 + 실습 4시간)  
> **학습 대상**: Java 기초 문법을 익힌 학습자 (DB 연동 경험 없음)  
> **실습 DB**: shopdb (Chapter 6까지 실습 완료 상태)  
> **실습 환경**: JDK 17+, MariaDB 10.6+, IntelliJ IDEA (또는 Eclipse)

---

## 학습 목표

이 장을 마치면 다음을 할 수 있다.

- JDBC의 역할과 동작 원리를 설명할 수 있다.
- MariaDB JDBC 드라이버를 프로젝트에 추가하고 DB에 연결할 수 있다.
- `Statement`와 `PreparedStatement`의 차이를 설명하고 적절히 선택할 수 있다.
- `ResultSet`으로 조회 결과를 읽어 Java 객체로 변환할 수 있다.
- JDBC로 SELECT · INSERT · UPDATE · DELETE를 구현할 수 있다.
- 트랜잭션을 명시적으로 제어할 수 있다.
- HikariCP 커넥션 풀을 설정하고 기존 코드에 적용할 수 있다.

---

## 1. JDBC란?

### 1.1 배경: Java에서 DB를 다루려면?

Java 애플리케이션은 MariaDB, MySQL, Oracle, PostgreSQL 등 다양한 DBMS와 통신해야 한다.  
DBMS마다 통신 방식이 다르기 때문에 DBMS별로 완전히 다른 코드를 작성한다면 유지보수가 매우 어려워진다.

```
[문제 상황]

Java 앱 → MariaDB 전용 코드 작성
Java 앱 → Oracle 전용 코드 작성
Java 앱 → PostgreSQL 전용 코드 작성
→ DBMS가 바뀔 때마다 애플리케이션 코드를 전부 수정해야 한다.
```

### 1.2 JDBC의 해결책

**JDBC(Java Database Connectivity)**는 Java에서 데이터베이스에 접근하는 **표준 API**다.  
Java SE 표준 라이브러리(`java.sql` 패키지)에 포함되어 있으며, 1997년부터 Java의 핵심 기능으로 자리잡았다.

```
[JDBC 아키텍처]

┌─────────────────────────────┐
│       Java 애플리케이션       │  ← 개발자가 작성하는 코드
└─────────────┬───────────────┘
              │  java.sql API (표준)
┌─────────────▼───────────────┐
│        JDBC Driver Manager  │  ← JDK 내장
└──────┬──────────┬───────────┘
       │          │
┌──────▼──┐  ┌───▼──────┐
│ MariaDB │  │  Oracle  │  ← 각 DBMS 제조사가 제공하는 드라이버
│ Driver  │  │  Driver  │
└──────┬──┘  └───┬──────┘
       │          │
┌──────▼──┐  ┌───▼──────┐
│ MariaDB │  │  Oracle  │  ← 실제 DBMS
└─────────┘  └──────────┘
```

> **핵심 포인트**: 개발자는 `java.sql` 패키지의 표준 인터페이스만 사용하면 된다.  
> DBMS를 교체할 때는 드라이버만 바꾸면 애플리케이션 코드 변경이 최소화된다.

### 1.3 핵심 클래스와 인터페이스

| 이름 | 구분 | 역할 |
|---|---|---|
| `DriverManager` | 클래스 | JDBC 드라이버를 관리하고 DB 연결(Connection)을 생성 |
| `Connection` | 인터페이스 | DB와의 연결 세션을 표현 |
| `Statement` | 인터페이스 | 정적 SQL 실행 |
| `PreparedStatement` | 인터페이스 | 파라미터가 있는 SQL 사전 컴파일 및 실행 |
| `ResultSet` | 인터페이스 | SELECT 결과 집합을 커서 방식으로 탐색 |
| `SQLException` | 예외 클래스 | DB 관련 오류를 표현하는 체크 예외 |

---

## 2. 개발 환경 설정

### 2.1 프로젝트 생성

Eclipse에서 새 Java 프로젝트를 생성한다.

1. 메뉴 **File → New → Java Project** 선택
2. **Project name** 입력 (예: `JdbcPractice`)
3. **JRE** 항목에서 설치된 JDK 17 이상을 선택한 뒤 **Finish** 클릭

프로젝트 루트 아래에 외부 라이브러리를 담을 폴더를 만든다.

1. 프로젝트를 우클릭 → **New → Folder**
2. 폴더 이름을 `lib`으로 입력 후 **Finish**

### 2.2 JDBC 드라이버 JAR 추가

MariaDB JDBC 드라이버는 단일 JAR 파일로 배포된다.

**① JAR 파일 다운로드**

아래 주소에서 최신 버전의 JAR 파일을 받는다.  
[https://mariadb.com/downloads/connectors/connectors-data-access/java8-connector](https://mariadb.com/downloads/connectors/connectors-data-access/java8-connector)

- 파일명 예시: `mariadb-java-client-3.3.3.jar`

**② `lib` 폴더에 복사**

다운로드한 JAR 파일을 Eclipse 프로젝트 탐색기의 `lib` 폴더로 드래그하거나,  
파일 탐색기에서 `JdbcPractice/lib/` 경로에 직접 복사한다.

**③ Build Path에 등록**

1. `lib` 폴더 안의 JAR 파일을 우클릭
2. **Build Path → Add to Build Path** 클릭
3. 프로젝트 탐색기에 **Referenced Libraries** 항목이 생기고 JAR가 등록된 것을 확인

> **확인 방법**: 프로젝트 우클릭 → **Properties → Java Build Path → Libraries** 탭에서 JAR가 목록에 있으면 정상이다.

### 2.3 DB 연결 정보 정리

아래 정보를 미리 확인해 둔다.

| 항목 | 예시 값 | 설명 |
|---|---|---|
| 호스트 | `localhost` | DB 서버 주소 |
| 포트 | `3306` | MariaDB 기본 포트 |
| 데이터베이스명 | `shopdb` | 접속할 스키마 이름 |
| 사용자명 | `root` | DB 계정 |
| 비밀번호 | `1234` | DB 비밀번호 |
| JDBC URL | `jdbc:mariadb://localhost:3306/shopdb` | 연결 문자열 |

**JDBC URL 구조**

```
jdbc:mariadb://[호스트]:[포트]/[데이터베이스명][?옵션=값&옵션=값]

예시:
jdbc:mariadb://localhost:3306/shopdb
jdbc:mariadb://localhost:3306/shopdb?useUnicode=true&characterEncoding=UTF-8
```

---

## 3. DB 연결 — Connection

### 3.1 Connection 획득

`DriverManager.getConnection()`으로 DB와의 연결 세션을 얻는다.

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionExample {

    private static final String URL      = "jdbc:mariadb://localhost:3306/shopdb";
    private static final String USER     = "root";
    private static final String PASSWORD = "1234";

    public static void main(String[] args) {
        // try-with-resources: Connection은 사용 후 반드시 닫아야 한다
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {

            System.out.println("DB 연결 성공!");
            System.out.println("연결 정보: " + conn.getMetaData().getURL());

        } catch (SQLException e) {
            System.err.println("DB 연결 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

**실행 결과 (정상)**
```
DB 연결 성공!
연결 정보: jdbc:mariadb://localhost:3306/shopdb
```

### 3.2 Connection 관리 원칙

Connection은 네트워크 소켓을 열고 DB 세션을 생성하는 **무거운 자원**이다.  
사용 후 반드시 닫지 않으면 DB 서버의 연결 한도가 소진되어 장애로 이어진다.

```java
// ❌ 나쁜 예: Connection을 닫지 않음
Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
// ... SQL 실행 ...
// conn.close()를 호출하지 않으면 DB 연결이 누수된다

// ✅ 좋은 예 1: try-with-resources (Java 7+, 권장)
try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
    // ... SQL 실행 ...
} // 블록 종료 시 conn.close() 자동 호출

// ✅ 좋은 예 2: finally 블록 (구버전 호환)
Connection conn = null;
try {
    conn = DriverManager.getConnection(URL, USER, PASSWORD);
    // ... SQL 실행 ...
} catch (SQLException e) {
    e.printStackTrace();
} finally {
    if (conn != null) {
        try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
    }
}
```

> **권장**: Java 7 이상에서는 `try-with-resources`를 사용한다.  
> `Connection`, `Statement`, `ResultSet` 모두 `AutoCloseable`을 구현하므로 동일하게 적용할 수 있다.

### 3.3 연결 실패 시 확인 사항

| 오류 메시지 | 원인 | 해결 방법 |
|---|---|---|
| `Communications link failure` | 호스트/포트 오류, DB 서버 미실행 | URL 확인, MariaDB 서비스 기동 확인 |
| `Access denied for user` | 사용자명/비밀번호 오류 | 계정 정보 확인 |
| `Unknown database` | 데이터베이스명 오류 | 스키마 이름 확인 |
| `No suitable driver found` | 드라이버 JAR 없음 | pom.xml 의존성 확인 후 Maven 업데이트 |

---

## 4. Statement — SQL 실행

### 4.1 Statement

`Connection`에서 `Statement`를 생성하여 SQL을 실행한다.

```java
// Statement 생성
Statement stmt = conn.createStatement();

// SELECT: executeQuery() → ResultSet 반환
ResultSet rs = stmt.executeQuery("SELECT * FROM category");

// INSERT / UPDATE / DELETE: executeUpdate() → 영향받은 행 수 반환
int rows = stmt.executeUpdate("INSERT INTO category VALUES (10, '신상품')");
```

**전체 SELECT 예제**

```java
import java.sql.*;

public class StatementSelectExample {

    private static final String URL      = "jdbc:mariadb://localhost:3306/shopdb";
    private static final String USER     = "root";
    private static final String PASSWORD = "1234";

    public static void main(String[] args) {
        String sql = "SELECT category_id, category_name FROM category ORDER BY category_id";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {

            System.out.printf("%-5s %-20s%n", "ID", "카테고리명");
            System.out.println("-".repeat(26));

            while (rs.next()) {
                int    id   = rs.getInt("category_id");
                String name = rs.getString("category_name");
                System.out.printf("%-5d %-20s%n", id, name);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

### 4.2 Statement의 한계 — SQL 인젝션

사용자 입력을 `Statement`에 그대로 이어 붙이면 **SQL 인젝션(SQL Injection)** 공격에 취약하다.

```java
// ❌ 위험한 코드
String userId = request.getParameter("id"); // 사용자 입력
String sql = "SELECT * FROM member WHERE member_id = '" + userId + "'";
// userId에 "' OR '1'='1"이 들어오면 모든 회원 정보가 노출된다

// 실제 실행되는 SQL:
// SELECT * FROM member WHERE member_id = '' OR '1'='1'
```

> **SQL 인젝션**: 악의적인 SQL 코드를 입력에 삽입하여 의도하지 않은 쿼리를 실행시키는 공격 기법.  
> `PreparedStatement`를 사용하면 이 문제를 근본적으로 해결할 수 있다.

---

## 5. PreparedStatement — 파라미터 바인딩

### 5.1 PreparedStatement란?

`PreparedStatement`는 SQL을 **미리 컴파일**해 두고, 실행 시 파라미터(`?`)만 바인딩하는 방식이다.

```
[Statement 방식]
실행 요청마다: SQL 파싱 → 컴파일 → 실행

[PreparedStatement 방식]
최초 1회: SQL 파싱 → 컴파일 (미리 준비)
실행 요청:             파라미터 바인딩 → 실행  ← 반복 실행 시 빠름
```

**장점 정리**

| 항목 | Statement | PreparedStatement |
|---|---|---|
| SQL 인젝션 방어 | ❌ 취약 | ✅ 안전 |
| 반복 실행 성능 | 낮음 | 높음 (사전 컴파일) |
| 가독성 | 문자열 연결로 복잡 | 파라미터로 명확 |
| 권장 여부 | 비권장 | ✅ 권장 |

### 5.2 SELECT with PreparedStatement

```java
import java.sql.*;

public class PreparedSelectExample {

    public static void main(String[] args) {
        String url  = "jdbc:mariadb://localhost:3306/shopdb";
        String user = "root", password = "1234";

        // 조회 조건을 파라미터(?)로 지정
        String sql = "SELECT product_id, product_name, price "
                   + "FROM product WHERE category_id = ? AND price <= ?";

        int    categoryId = 1;
        int    maxPrice   = 50000;

        try (Connection        conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 파라미터 바인딩: setXxx(순서, 값) — 순서는 1부터 시작
            pstmt.setInt(1, categoryId);
            pstmt.setInt(2, maxPrice);

            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.printf("%-10s %-30s %10s%n", "상품ID", "상품명", "가격");
                System.out.println("-".repeat(52));

                while (rs.next()) {
                    System.out.printf("%-10d %-30s %,10d원%n",
                        rs.getInt("product_id"),
                        rs.getString("product_name"),
                        rs.getInt("price"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

### 5.3 INSERT with PreparedStatement

```java
String sql = "INSERT INTO category (category_id, category_name) VALUES (?, ?)";

try (Connection        conn  = DriverManager.getConnection(url, user, password);
     PreparedStatement pstmt = conn.prepareStatement(sql)) {

    pstmt.setInt(1, 10);
    pstmt.setString(2, "아웃도어");

    int affectedRows = pstmt.executeUpdate();
    System.out.println("삽입된 행 수: " + affectedRows);

} catch (SQLException e) {
    e.printStackTrace();
}
```

### 5.4 UPDATE with PreparedStatement

```java
String sql = "UPDATE product SET price = ? WHERE product_id = ?";

try (Connection        conn  = DriverManager.getConnection(url, user, password);
     PreparedStatement pstmt = conn.prepareStatement(sql)) {

    pstmt.setInt(1, 39000);  // 변경할 가격
    pstmt.setInt(2, 5);      // 대상 product_id

    int affectedRows = pstmt.executeUpdate();
    System.out.println("수정된 행 수: " + affectedRows);

} catch (SQLException e) {
    e.printStackTrace();
}
```

### 5.5 DELETE with PreparedStatement

```java
String sql = "DELETE FROM category WHERE category_id = ?";

try (Connection        conn  = DriverManager.getConnection(url, user, password);
     PreparedStatement pstmt = conn.prepareStatement(sql)) {

    pstmt.setInt(1, 10);

    int affectedRows = pstmt.executeUpdate();
    System.out.println("삭제된 행 수: " + affectedRows);

} catch (SQLException e) {
    e.printStackTrace();
}
```

### 5.6 자주 사용하는 setXxx / getXxx 메서드

| SQL 타입 | setXxx 메서드 | getXxx 메서드 |
|---|---|---|
| INT, BIGINT | `setInt()`, `setLong()` | `getInt()`, `getLong()` |
| VARCHAR, TEXT | `setString()` | `getString()` |
| DOUBLE, FLOAT | `setDouble()` | `getDouble()` |
| DATE | `setDate()` | `getDate()` |
| DATETIME | `setTimestamp()` | `getTimestamp()` |
| NULL 처리 | `setNull(index, Types.XXX)` | `wasNull()` |

---

## 6. ResultSet — 결과 탐색

### 6.1 커서 방식 이해

`ResultSet`은 **커서(cursor)** 방식으로 동작한다.  
최초에는 커서가 첫 번째 행 **이전**에 위치하며, `next()`를 호출할 때마다 한 행씩 앞으로 이동한다.

```
rs.next() 호출 전:   [커서] → | 1행 | 2행 | 3행 |
rs.next() 1회 후:   | 1행 [커서] | 2행 | 3행 |
rs.next() 2회 후:   | 1행 | 2행 [커서] | 3행 |
rs.next() 3회 후:   | 1행 | 2행 | 3행 [커서] |
rs.next() 4회 후:   | 1행 | 2행 | 3행 | → false 반환 (결과 없음)
```

```java
while (rs.next()) {
    // 커서가 가리키는 행에서 컬럼 값을 읽는다
    String name = rs.getString("column_name");  // 컬럼명으로 접근 (권장)
    int    id   = rs.getInt(1);                  // 컬럼 인덱스로 접근 (1부터 시작)
}
```

### 6.2 단건 조회 패턴

```java
String sql = "SELECT * FROM product WHERE product_id = ?";

try (Connection        conn  = DriverManager.getConnection(url, user, password);
     PreparedStatement pstmt = conn.prepareStatement(sql)) {

    pstmt.setInt(1, 3);

    try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
            // 단건이므로 while 대신 if 사용
            System.out.println("상품명: " + rs.getString("product_name"));
            System.out.println("가격  : " + rs.getInt("price"));
        } else {
            System.out.println("해당 상품이 없습니다.");
        }
    }
}
```

### 6.3 Java 객체로 변환 (VO 패턴)

실무에서는 ResultSet에서 읽은 값을 Java 객체(VO: Value Object)에 담아 반환한다.

```java
// Product.java — 상품 정보를 담는 VO 클래스
public class Product {
    private int    productId;
    private String productName;
    private int    categoryId;
    private int    price;
    private int    stock;

    // 생성자, getter, setter, toString 생략
    // IntelliJ: Alt+Insert → Getter and Setter / toString()으로 자동 생성
}
```

```java
// ProductDAO.java — DB 접근 로직을 담는 DAO 클래스
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    private static final String URL  = "jdbc:mariadb://localhost:3306/shopdb";
    private static final String USER = "root", PASSWORD = "1234";

    public List<Product> findAll() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT product_id, product_name, category_id, price, stock "
                   + "FROM product ORDER BY product_id";

        try (Connection        conn  = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet         rs    = pstmt.executeQuery()) {

            while (rs.next()) {
                Product p = new Product();
                p.setProductId(rs.getInt("product_id"));
                p.setProductName(rs.getString("product_name"));
                p.setCategoryId(rs.getInt("category_id"));
                p.setPrice(rs.getInt("price"));
                p.setStock(rs.getInt("stock"));
                products.add(p);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }
}
```

> **DAO (Data Access Object)**: DB 접근 로직만 담당하는 클래스.  
> DB 관련 코드를 한 곳에 모아 관리하면 유지보수가 쉬워진다.

---

## 7. 트랜잭션 제어

### 7.1 JDBC 기본 트랜잭션 동작

JDBC는 기본적으로 **Auto Commit** 모드로 동작한다.  
즉, `executeUpdate()` 호출 즉시 DB에 영구 반영(COMMIT)된다.

```
Auto Commit = true (기본):
executeUpdate() 호출 → 즉시 COMMIT
```

### 7.2 명시적 트랜잭션

여러 SQL을 하나의 트랜잭션으로 묶으려면 Auto Commit을 비활성화한다.

```java
Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
conn.setAutoCommit(false);  // Auto Commit 비활성화 → 트랜잭션 시작

try {
    // 여러 SQL을 순서대로 실행
    PreparedStatement pstmt1 = conn.prepareStatement(
        "UPDATE account SET balance = balance - ? WHERE account_id = ?");
    pstmt1.setInt(1, 10000);
    pstmt1.setInt(2, 1);      // 계좌 1에서 10,000원 출금
    pstmt1.executeUpdate();

    PreparedStatement pstmt2 = conn.prepareStatement(
        "UPDATE account SET balance = balance + ? WHERE account_id = ?");
    pstmt2.setInt(1, 10000);
    pstmt2.setInt(2, 2);      // 계좌 2에 10,000원 입금
    pstmt2.executeUpdate();

    conn.commit();    // 모두 성공 → 영구 반영
    System.out.println("이체 완료");

} catch (SQLException e) {
    conn.rollback();  // 하나라도 실패 → 모두 취소
    System.err.println("이체 실패, 롤백 처리: " + e.getMessage());
    throw e;

} finally {
    conn.setAutoCommit(true);  // Auto Commit 원복
    conn.close();
}
```

> **원자성(Atomicity)**: 트랜잭션에 속한 SQL은 모두 성공하거나, 모두 취소된다.  
> 이체 예시에서 출금만 되고 입금이 실패하면 절대 안 되는 이유다.

### 7.3 트랜잭션 처리 흐름

```
setAutoCommit(false)
        │
   SQL 실행 1
        │
   SQL 실행 2
        │
   SQL 실행 N
        │
   ┌────▼────┐      ┌────────▼────────┐
   │  정상   │      │    예외 발생     │
   └────┬────┘      └────────┬────────┘
    commit()             rollback()
        │                    │
   DB 영구 반영         변경 취소 (원래대로)
```

---

## 8. Connection Pool — HikariCP

### 8.1 Connection 생성 비용 문제

지금까지 실습에서는 SQL을 실행할 때마다 `DriverManager.getConnection()`으로 새 Connection을 생성했다.  
이 방식은 다음과 같은 문제가 있다.

```
사용자 요청 → Connection 생성 (TCP 연결 + 인증: 수십~수백 ms)
                     → SQL 실행 (수 ms)
                           → Connection 종료
```

- Connection 생성 시간이 실제 SQL 실행 시간보다 훨씬 길다.
- 동시 접속자가 많아지면 모든 요청이 Connection 생성에서 지연된다.

### 8.2 커넥션 풀(Connection Pool) 개념

**커넥션 풀**은 미리 여러 개의 Connection을 생성해 두고, 요청이 올 때 빌려주는 패턴이다.

```
[애플리케이션 시작 시]
커넥션 풀 초기화 → Connection 10개 미리 생성

[요청 처리 시]
요청 → 풀에서 Connection 대여 (즉시!)
     → SQL 실행
     → Connection 반납 (close() 호출 → 실제로는 풀에 반납됨)
```

**효과**

| 항목 | DriverManager | Connection Pool |
|---|---|---|
| Connection 생성 빈도 | 매 요청마다 | 애플리케이션 시작 시 1회 |
| 응답 시간 | 느림 | 빠름 |
| 동시 접속 처리 | 불안정 | 안정적 (최대 연결 수 제어) |
| 실무 사용 여부 | 학습/테스트용 | ✅ 필수 |

### 8.3 HikariCP

**HikariCP**는 Java 생태계에서 가장 널리 사용되는 커넥션 풀 라이브러리다.  
Spring Boot는 기본 커넥션 풀로 HikariCP를 채택하고 있다.

**의존성 추가** (`pom.xml`)

```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.1.0</version>
</dependency>
```

### 8.4 HikariCP 설정 및 사용

```java
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.sql.*;

public class HikariExample {

    // DataSource: Connection을 제공하는 팩토리 (표준 인터페이스)
    private static final DataSource dataSource;

    // 애플리케이션 시작 시 한 번만 초기화
    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mariadb://localhost:3306/shopdb");
        config.setUsername("root");
        config.setPassword("1234");

        // 풀 설정
        config.setMaximumPoolSize(10);       // 최대 Connection 수 (기본 10)
        config.setMinimumIdle(5);            // 유휴 Connection 최소 유지 수
        config.setConnectionTimeout(30000);  // Connection 대기 최대 시간 (ms)
        config.setIdleTimeout(600000);       // 유휴 Connection 유지 시간 (ms)
        config.setMaxLifetime(1800000);      // Connection 최대 수명 (ms)

        dataSource = new HikariDataSource(config);
    }

    public static void main(String[] args) {
        // 사용법은 DriverManager와 동일 — conn.close() 시 풀에 반납됨
        String sql = "SELECT COUNT(*) AS cnt FROM product";

        try (Connection conn = dataSource.getConnection();   // 풀에서 대여
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                System.out.println("상품 수: " + rs.getInt("cnt"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        // try 블록 종료 → conn.close() 자동 호출 → 풀에 반납
    }
}
```

### 8.5 DBConnection 유틸리티 클래스 만들기

실제 프로젝트에서는 DataSource를 별도 유틸리티 클래스에 캡슐화한다.

```java
// DBConnection.java
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DBConnection {

    private static final DataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mariadb://localhost:3306/shopdb");
        config.setUsername("root");
        config.setPassword("1234");
        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);
    }

    // 외부에서 new DBConnection() 금지
    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
```

```java
// ProductDAO.java — DBConnection을 사용하도록 수정
public class ProductDAO {

    public List<Product> findAll() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM product ORDER BY product_id";

        try (Connection        conn  = DBConnection.getConnection();  // 풀에서 대여
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet         rs    = pstmt.executeQuery()) {

            while (rs.next()) {
                // ... 동일 ...
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }
}
```

### 8.6 주요 HikariCP 설정 옵션

| 설정 | 기본값 | 설명 |
|---|---|---|
| `maximumPoolSize` | 10 | 최대 Connection 수. CPU 코어 수 × 2 정도를 기준으로 조정 |
| `minimumIdle` | maximumPoolSize와 동일 | 풀에 유지할 최소 유휴 Connection 수 |
| `connectionTimeout` | 30,000ms | Connection 대기 최대 시간. 초과 시 예외 발생 |
| `idleTimeout` | 600,000ms | 유휴 Connection을 풀에서 제거하기까지의 시간 |
| `maxLifetime` | 1,800,000ms | Connection의 최대 수명. DB 서버 설정보다 짧게 설정 |
| `connectionTestQuery` | -  | 연결 유효성 검사 쿼리 (`SELECT 1` 등) |

> **실무 팁**: `maxLifetime`은 MariaDB의 `wait_timeout` 설정보다 몇 초 짧게 설정한다.  
> 그렇지 않으면 DB가 연결을 끊었는데 풀이 그 연결을 재사용하려다 오류가 발생한다.

---

## 9. 종합 실습 — 상품 CRUD 구현

### 9.1 프로젝트 구조

```
src/
└── main/
    └── java/
        ├── db/
        │   └── DBConnection.java       # HikariCP 설정
        ├── vo/
        │   └── Product.java            # VO 클래스
        ├── dao/
        │   └── ProductDAO.java         # CRUD 메서드
        └── main/
            └── Main.java               # 실행 진입점
```

### 9.2 ProductDAO — 전체 CRUD

```java
// dao/ProductDAO.java
package dao;

import db.DBConnection;
import vo.Product;
import java.sql.*;
import java.util.*;

public class ProductDAO {

    // 전체 상품 조회
    public List<Product> findAll() throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT product_id, product_name, category_id, price, stock "
                   + "FROM product ORDER BY product_id";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    // 단건 조회
    public Optional<Product> findById(int productId) throws SQLException {
        String sql = "SELECT product_id, product_name, category_id, price, stock "
                   + "FROM product WHERE product_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, productId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    // 상품 등록
    public int insert(Product p) throws SQLException {
        String sql = "INSERT INTO product (product_name, category_id, price, stock) "
                   + "VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, p.getProductName());
            pstmt.setInt(2, p.getCategoryId());
            pstmt.setInt(3, p.getPrice());
            pstmt.setInt(4, p.getStock());
            pstmt.executeUpdate();

            // AUTO_INCREMENT로 생성된 PK 값 조회
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    // 상품 수정
    public int update(Product p) throws SQLException {
        String sql = "UPDATE product SET product_name = ?, price = ?, stock = ? "
                   + "WHERE product_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, p.getProductName());
            pstmt.setInt(2, p.getPrice());
            pstmt.setInt(3, p.getStock());
            pstmt.setInt(4, p.getProductId());
            return pstmt.executeUpdate();
        }
    }

    // 상품 삭제
    public int delete(int productId) throws SQLException {
        String sql = "DELETE FROM product WHERE product_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, productId);
            return pstmt.executeUpdate();
        }
    }

    // ResultSet → Product 변환 (공통 메서드)
    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getInt("product_id"));
        p.setProductName(rs.getString("product_name"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setPrice(rs.getInt("price"));
        p.setStock(rs.getInt("stock"));
        return p;
    }
}
```

### 9.3 Main — 동작 확인

```java
// main/Main.java
package main;

import dao.ProductDAO;
import vo.Product;
import java.sql.SQLException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws SQLException {
        ProductDAO dao = new ProductDAO();

        // 1. 전체 조회
        System.out.println("=== 전체 상품 ===");
        List<Product> all = dao.findAll();
        all.forEach(System.out::println);

        // 2. 단건 조회
        System.out.println("\n=== 상품 ID=1 조회 ===");
        dao.findById(1).ifPresentOrElse(
            System.out::println,
            () -> System.out.println("상품 없음")
        );

        // 3. 상품 등록
        Product newProduct = new Product();
        newProduct.setProductName("테스트상품");
        newProduct.setCategoryId(1);
        newProduct.setPrice(9900);
        newProduct.setStock(100);
        int newId = dao.insert(newProduct);
        System.out.println("\n=== 등록된 상품 ID: " + newId + " ===");

        // 4. 수정
        newProduct.setProductId(newId);
        newProduct.setPrice(8900);
        int updated = dao.update(newProduct);
        System.out.println("수정된 행 수: " + updated);

        // 5. 삭제
        int deleted = dao.delete(newId);
        System.out.println("삭제된 행 수: " + deleted);
    }
}
```

---

## 10. 실습 문제

### 실습 1 — 기본 연결 확인 ⭐

`ConnectionTest.java`를 작성하여 MariaDB에 성공적으로 연결되는지 확인한다.  
연결 성공 시 DB 버전(`SELECT VERSION()`)을 출력하라.

**기대 출력 예시**
```
연결 성공!
MariaDB 버전: 10.11.6-MariaDB
```

---

### 실습 2 — 카테고리 전체 조회 ⭐

`CategoryDAO.java`에 `findAll()` 메서드를 작성하라.  
`category` 테이블의 모든 행을 `category_id` 오름차순으로 조회하여 출력한다.

---

### 실습 3 — 상품 검색 ⭐⭐

`ProductDAO.java`에 `findByPriceRange(int minPrice, int maxPrice)` 메서드를 추가하라.  
가격 범위(이상 ~ 이하)에 해당하는 상품을 `price` 오름차순으로 반환한다.

---

### 실습 4 — 주문 등록 트랜잭션 ⭐⭐⭐

아래 두 SQL을 하나의 트랜잭션으로 처리하는 `OrderService.placeOrder()` 메서드를 작성하라.  
두 번째 SQL 실패 시 첫 번째 SQL도 롤백되는지 확인한다.

1. `order` 테이블에 주문 행 INSERT
2. `product` 테이블에서 해당 상품의 `stock` 감소 UPDATE (`stock - 주문수량`)

```java
public void placeOrder(int productId, int memberId, int quantity) throws SQLException {
    // TODO: 트랜잭션으로 구현
}
```

> **힌트**: `conn.setAutoCommit(false)` → SQL 실행 → `commit()` or `rollback()`

---

### 실습 5 — HikariCP 적용 ⭐⭐

실습 2~4에서 작성한 DAO가 `DriverManager` 대신 `DBConnection`(HikariCP)을 사용하도록 변경하라.  
`HikariDataSource`의 로그에서 커넥션 풀이 초기화되는 메시지를 확인한다.

---

## 정리

### 핵심 개념 요약

| 개념 | 설명 |
|---|---|
| JDBC | Java에서 DB에 접근하는 표준 API (`java.sql`) |
| Connection | DB와의 연결 세션. 반드시 사용 후 닫아야 한다 |
| Statement | 정적 SQL 실행. SQL 인젝션 위험 → 사용 지양 |
| PreparedStatement | 파라미터 바인딩. SQL 인젝션 방어 + 성능 우수 → **항상 사용** |
| ResultSet | SELECT 결과 커서. `next()`로 행 이동 후 `getXxx()`로 값 읽기 |
| Auto Commit | 기본 `true`. 명시적 트랜잭션이 필요하면 `false`로 변경 |
| Connection Pool | Connection을 미리 생성해 재사용하는 패턴. 실무 필수 |
| HikariCP | Java 표준 커넥션 풀 라이브러리. Spring Boot 기본 채택 |
| DAO 패턴 | DB 접근 코드를 클래스로 분리하여 유지보수성 향상 |

### JDBC 개발 체크리스트

- [ ] `try-with-resources`로 Connection · Statement · ResultSet을 자동 닫기
- [ ] 사용자 입력이 포함된 SQL은 반드시 `PreparedStatement` 사용
- [ ] 여러 SQL을 하나의 작업 단위로 처리할 때 트랜잭션 명시
- [ ] 실무 환경에서는 `DriverManager` 대신 커넥션 풀 사용
- [ ] DB 접근 코드는 DAO 클래스로 분리

### 다음 단계

이 장에서 배운 JDBC는 DB 연동의 기초다.  
실무에서는 JDBC 위에 다음과 같은 추상화 계층을 더 얹어 사용한다.

```
[추상화 수준]

높음  Spring Data JPA / Hibernate (ORM — SQL 없이 객체로 처리)
      ↑
      MyBatis (SQL 매퍼 — SQL은 직접 작성, 매핑만 자동화)
      ↑
낮음  JDBC (이 장에서 학습한 내용)
```

다음 장에서는 MyBatis를 사용하여 반복적인 JDBC 코드를 줄이는 방법을 배운다.
