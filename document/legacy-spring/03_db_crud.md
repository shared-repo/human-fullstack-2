# 3일차 — 데이터베이스 연동과 CRUD 구현

> **환경** : Java 21 + Spring Framework 7.0.x + MyBatis 3.5.x + mybatis-spring 3.0.x + MariaDB

---

## 3.1 DataSource 설정과 JDBC 연동 방식 비교

### 3.1.1 DataSource란

웹 애플리케이션은 DB에 쿼리를 날릴 때마다 연결(Connection)을 맺고 해제한다. 연결을 맺는 과정은 TCP 핸드셰이크, DB 인증, 세션 초기화 등을 포함하는 비용이 큰 작업이다. 요청마다 이 과정을 반복하면 응답 속도가 크게 저하된다.

**DataSource**는 이 문제를 **커넥션 풀(Connection Pool)** 로 해결하는 표준 인터페이스(`javax.sql.DataSource` → Spring 7.x: `javax.sql.DataSource` 그대로 사용, 이 인터페이스는 Java SE 표준이므로 `jakarta`로 변경되지 않음)다. 미리 일정 수의 Connection을 만들어 풀에 보관해 두고, 요청이 들어오면 풀에서 꺼내 주고, 사용이 끝나면 풀로 반납하는 방식으로 동작한다.

```
커넥션 풀 동작 방식

애플리케이션 시작 시
  → DB Connection 5개를 미리 생성해 풀에 보관

요청 발생 시
  → 풀에서 Connection 1개를 꺼내 사용
  → 쿼리 실행
  → 풀에 Connection 반납 (close 해도 실제로는 풀로 돌아감)

결과: DB 연결 비용 없이 즉시 쿼리 실행 가능
```

---

### 3.1.2 JDBC 연동 방식 비교

스프링 환경에서 DB를 연동하는 방식은 세 가지로 나뉜다.

**① 순수 JDBC**

```java
// Connection 획득 → PreparedStatement 생성 → 실행 → ResultSet 처리 → 자원 해제
// 개발자가 모든 과정을 직접 작성한다
Connection conn = dataSource.getConnection();
PreparedStatement ps = conn.prepareStatement("SELECT * FROM board");
ResultSet rs = ps.executeQuery();
while (rs.next()) {
    // 컬럼 하나하나 꺼내서 객체에 담기
}
rs.close(); ps.close(); conn.close(); // 자원 해제 누락 시 장애 발생
```

쿼리 하나를 실행하기 위해 반복되는 코드가 많고, 자원 해제 누락 시 커넥션 풀이 고갈되는 장애가 발생한다. 학습 목적 외에는 사용하지 않는다.

**② Spring JDBC (JdbcTemplate)**

스프링이 제공하는 JDBC 추상화 도구다. Connection 획득·해제, 예외 처리를 스프링이 담당하고, 개발자는 SQL과 결과 매핑만 작성한다.

```java
// 자원 관리, 예외 처리는 스프링이 처리 — 개발자는 SQL과 결과 매핑에만 집중
List<Board> list = jdbcTemplate.query(
    "SELECT * FROM board",
    (rs, rowNum) -> new Board(rs.getInt("no"), rs.getString("title"))
);
```

간단한 쿼리에는 적합하지만, 복잡한 동적 쿼리(검색 조건이 가변적인 경우)를 다루기 어렵고, 쿼리가 자바 코드 안에 섞인다는 단점이 있다.

**③ MyBatis**

SQL을 자바 코드가 아닌 **XML 파일**에 분리하여 관리하는 SQL 매핑 프레임워크다. 복잡한 동적 쿼리 작성, SQL과 비즈니스 코드의 분리, 결과 자동 매핑이 강점이다. 국내 기업 환경에서 가장 널리 사용된다.

| 항목 | 순수 JDBC | Spring JdbcTemplate | MyBatis |
|---|---|---|---|
| 자원 관리 | 개발자 직접 | 스프링 자동 | 스프링 자동 |
| SQL 위치 | 자바 코드 안 | 자바 코드 안 | XML 파일 분리 |
| 동적 쿼리 | 복잡 | 복잡 | `<if>`, `<choose>` 태그로 간결 |
| 결과 매핑 | 수동 | 수동/람다 | 자동 (컬럼명-필드명 규칙) |
| 학습 난이도 | 낮음 | 낮음 | 중간 |
| **국내 사용 빈도** | 거의 없음 | 일부 | **매우 높음** |

이번 교육에서는 MyBatis를 사용한다.

---

### 3.1.3 DataSource 종류

스프링 프로젝트에서 사용할 수 있는 DataSource 구현체는 다음과 같다.

**DriverManagerDataSource (스프링 제공)**

커넥션 풀이 없다. 요청마다 새 Connection을 만들고 사용 후 실제로 닫는다. 개발·테스트 단계에서 빠르게 설정할 때만 사용하며, 운영 환경에서는 절대 사용하지 않는다.

```xml
<bean id="dataSource"
      class="org.springframework.jdbc.datasource.DriverManagerDataSource">
    <property name="driverClassName" value="org.mariadb.jdbc.Driver"/>
    <property name="url"             value="jdbc:mariadb://localhost:3306/boarddb"/>
    <property name="username"        value="root"/>
    <property name="password"        value="1234"/>
</bean>
```

**HikariCP (운영 환경 권장)**

현재 가장 빠르고 가벼운 커넥션 풀 구현체로, Spring Boot의 기본 DataSource이기도 하다. 스프링 프레임워크에서 사용하려면 별도 의존성 추가가 필요하다.

```xml
<bean id="dataSource" class="com.zaxxer.hikari.HikariDataSource">
    <property name="driverClassName" value="org.mariadb.jdbc.Driver"/>
    <property name="jdbcUrl"         value="jdbc:mariadb://localhost:3306/boarddb"/>
    <property name="username"        value="root"/>
    <property name="password"        value="1234"/>
    <property name="maximumPoolSize" value="10"/>
    <property name="minimumIdle"     value="5"/>
    <property name="connectionTimeout" value="30000"/>
</bean>
```

이번 실습에서는 HikariCP를 사용한다.

---

### 3.1.4 DB 연결 정보 외부화

DB 연결 정보(URL, 계정, 비밀번호)를 XML에 직접 쓰면 운영 환경과 개발 환경을 전환할 때마다 XML을 수정해야 하고, 형상 관리(Git)에 민감 정보가 노출된다. `.properties` 파일로 분리하는 것이 좋다.

`src/main/resources/db.properties`

```properties
db.driverClassName=org.mariadb.jdbc.Driver
db.url=jdbc:mariadb://localhost:3306/boarddb
db.username=root
db.password=1234
db.maximumPoolSize=10
```

`applicationContext.xml`에서 properties 파일을 불러와 `${...}`로 참조한다.

```xml
<!-- properties 파일 로드 -->
<context:property-placeholder location="classpath:db.properties"/>

<bean id="dataSource" class="com.zaxxer.hikari.HikariDataSource">
    <property name="driverClassName" value="${db.driverClassName}"/>
    <property name="jdbcUrl"         value="${db.url}"/>
    <property name="username"        value="${db.username}"/>
    <property name="password"        value="${db.password}"/>
    <property name="maximumPoolSize" value="${db.maximumPoolSize}"/>
</bean>
```

---

## 3.2 MyBatis-Spring 연동 설정

### 3.2.1 MyBatis란

MyBatis는 자바 객체와 SQL을 연결해주는 **SQL 매핑 프레임워크**다. JPA처럼 SQL을 자동 생성하는 ORM이 아니라, 개발자가 직접 SQL을 작성하되 그 SQL을 XML 파일에 분리해두고 자바 객체와의 매핑을 자동화한다.

```
MyBatis의 역할

자바 코드 (Mapper 인터페이스)
    ↕ SQL 실행 + 결과 매핑 자동화
XML 파일 (SQL 정의)
    ↕
DB
```

국내 엔터프라이즈 환경에서 선호하는 이유는 SQL을 개발자가 완전히 통제할 수 있기 때문이다. 복잡한 비즈니스 쿼리, 레거시 DB의 특수한 SQL, 정밀한 성능 튜닝이 필요한 환경에 적합하다.

---

### 3.2.2 MyBatis-Spring 연동 구조

MyBatis를 단독으로 사용할 때는 `SqlSession`을 직접 관리해야 한다. **mybatis-spring** 라이브러리는 MyBatis와 스프링을 통합하여 다음을 자동화한다.

- `SqlSession`을 스프링 Bean으로 관리
- 스프링의 트랜잭션 관리와 자동 연동
- Mapper 인터페이스를 스프링 Bean으로 자동 등록

```
연동 구조

스프링 컨테이너
 ├── DataSource          ← DB 연결 풀
 ├── SqlSessionFactory   ← SqlSession 생성 공장 (DataSource 사용)
 ├── MapperScannerConfigurer ← Mapper 인터페이스를 스캔해 Bean으로 등록
 └── BoardMapper (Bean)  ← 개발자가 작성한 Mapper 인터페이스
          ↑ 주입
     BoardDaoImpl
```

---

### 3.2.3 pom.xml — MyBatis 관련 의존성

```xml
<!-- MyBatis 코어 -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.16</version>
</dependency>

<!-- MyBatis-Spring 통합 (Spring 6/7 은 3.0 이상 필요) -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis-spring</artifactId>
    <version>3.0.3</version>
</dependency>

<!-- HikariCP 커넥션 풀 -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.1.0</version>
</dependency>

<!-- MariaDB JDBC 드라이버 -->
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
    <version>3.3.3</version>
</dependency>

<!-- Spring JDBC (트랜잭션 관리를 위해 필요) -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>7.0.7</version>
</dependency>
```

`mybatis-spring` 버전과 Spring 버전의 호환성을 반드시 확인해야 한다. Spring 6.x/7.x는 `jakarta.*` 기반이므로 **mybatis-spring 3.0 이상**이 필요하다. mybatis-spring 2.x는 Spring 5.x 이하와 함께 사용한다.

---

### 3.2.4 SqlSessionFactory 설정

`SqlSessionFactory`는 `SqlSession`(DB 작업을 수행하는 객체)을 생성하는 팩토리 Bean이다. `DataSource`와 MyBatis 설정, Mapper XML 파일의 위치를 알고 있어야 한다.

```xml
<!-- applicationContext.xml -->

<bean id="sqlSessionFactory"
      class="org.mybatis.spring.SqlSessionFactoryBean">

    <!-- 어떤 DB를 쓸지 -->
    <property name="dataSource" ref="dataSource"/>

    <!-- MyBatis 전역 설정 파일 위치 -->
    <property name="configLocation" value="classpath:mybatis-config.xml"/>

    <!-- Mapper XML 파일 위치 (** 는 하위 디렉터리 포함) -->
    <property name="mapperLocations" value="classpath:mapper/**/*.xml"/>
</bean>
```

---

### 3.2.5 Mapper 자동 스캔 설정

`MapperScannerConfigurer`는 지정한 패키지를 스캔하여 `@Mapper` 어노테이션이 붙은 인터페이스를 찾아 스프링 Bean으로 자동 등록한다. 개발자가 `BoardMapper` Bean을 수동으로 등록할 필요가 없다.

```xml
<bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
    <property name="basePackage" value="com.spring.board.mapper"/>
    <!-- @Mapper 어노테이션이 붙은 인터페이스만 스캔 -->
    <property name="annotationClass" value="org.apache.ibatis.annotations.Mapper"/>
</bean>
```

---

### 3.2.6 mybatis-config.xml

MyBatis의 전역 설정 파일이다. 별칭(typeAliases), 전역 동작 설정(settings), 플러그인 등을 지정한다.

```xml
<!-- src/main/resources/mybatis-config.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration
    PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
    "https://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>

    <settings>
        <!-- DB 컬럼의 snake_case → 자바 필드의 camelCase 자동 변환 -->
        <!-- board_no → boardNo, created_date → createdDate -->
        <setting name="mapUnderscoreToCamelCase" value="true"/>

        <!-- null 파라미터를 SQL에 넘길 때 JDBC 타입을 NULL로 설정 -->
        <setting name="jdbcTypeForNull" value="NULL"/>
    </settings>

    <typeAliases>
        <!-- com.spring.board.Board 클래스를 XML에서 'Board'로 줄여 쓸 수 있다 -->
        <package name="com.spring.board"/>
    </typeAliases>

</configuration>
```

`mapUnderscoreToCamelCase` 설정이 중요하다. DB 컬럼명은 관례적으로 `board_no`, `created_date` 같은 스네이크 케이스를 쓰고, 자바 필드명은 `boardNo`, `createdDate` 같은 카멜 케이스를 쓴다. 이 설정을 켜면 두 이름 규칙의 차이를 MyBatis가 자동으로 변환해준다.

---

## 3.3 Mapper 인터페이스와 XML 매퍼 작성

### 3.3.1 Mapper 인터페이스

Mapper 인터페이스는 SQL과 자바 메서드를 연결하는 다리다. 인터페이스만 정의하면 MyBatis가 런타임에 구현 클래스를 자동으로 생성한다. 개발자가 `BoardMapperImpl.java` 같은 구현 클래스를 직접 만들 필요가 없다.

```java
package com.spring.board.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface BoardMapper {
    List<Board> selectAll(Map<String, Object> params); // 목록 (페이징)
    int         selectCount(Map<String, Object> params); // 전체 건수 (페이징용)
    Board       selectOne(int no);                     // 상세
    void        insert(Board board);                   // 등록
    void        update(Board board);                   // 수정
    void        delete(int no);                        // 삭제
}
```

메서드명은 XML 매퍼의 `id`와 반드시 일치해야 한다. 이 연결이 MyBatis가 어떤 SQL을 실행할지 결정하는 기준이다.

---

### 3.3.2 XML 매퍼 파일 구조

```xml
<!-- src/main/resources/mapper/board/BoardMapper.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
    PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<!-- namespace 는 Mapper 인터페이스의 전체 경로와 반드시 일치해야 한다 -->
<mapper namespace="com.spring.board.mapper.BoardMapper">

    <!-- SQL 조각 재사용 -->
    <sql id="boardColumns">
        no, title, writer, content, hit, created_date
    </sql>

    <!-- 목록 조회 (페이징) -->
    <select id="selectAll" parameterType="map" resultType="Board">
        SELECT <include refid="boardColumns"/>
        FROM board
        ORDER BY no DESC
        LIMIT #{offset}, #{pageSize}
    </select>

    <!-- 전체 건수 -->
    <select id="selectCount" parameterType="map" resultType="int">
        SELECT COUNT(*) FROM board
    </select>

    <!-- 상세 조회 -->
    <select id="selectOne" parameterType="int" resultType="Board">
        SELECT <include refid="boardColumns"/>
        FROM board
        WHERE no = #{no}
    </select>

    <!-- 등록 -->
    <insert id="insert" parameterType="Board"
            useGeneratedKeys="true" keyProperty="no">
        INSERT INTO board (title, writer, content)
        VALUES (#{title}, #{writer}, #{content})
    </insert>

    <!-- 수정 -->
    <update id="update" parameterType="Board">
        UPDATE board
        SET title = #{title}, content = #{content}
        WHERE no = #{no}
    </update>

    <!-- 삭제 -->
    <delete id="delete" parameterType="int">
        DELETE FROM board WHERE no = #{no}
    </delete>

</mapper>
```

---

### 3.3.3 파라미터 바인딩 — #{} vs ${}

MyBatis에서 SQL에 값을 넣는 방법은 두 가지다.

**`#{}` — PreparedStatement 파라미터 바인딩 (권장)**

`?`를 사용하는 PreparedStatement로 변환되어 실행된다. SQL 인젝션이 원천 차단된다.

```xml
<!-- 실제 실행: SELECT * FROM board WHERE no = ? (값: 5) -->
<select id="selectOne" parameterType="int" resultType="Board">
    SELECT * FROM board WHERE no = #{no}
</select>
```

**`${}` — 문자열 직접 치환 (주의해서 사용)**

값이 SQL 문자열에 그대로 삽입된다. 정렬 컬럼명(`ORDER BY`)처럼 SQL 구조 자체를 동적으로 바꿀 때만 사용한다. 사용자 입력값에 `${}`를 쓰면 SQL 인젝션에 취약해진다.

```xml
<!-- 실제 실행: SELECT * FROM board ORDER BY no DESC -->
<!-- 사용자 입력이 아닌 내부적으로 결정된 값에만 사용 -->
SELECT * FROM board ORDER BY ${sortColumn} ${sortDirection}
```

**원칙**: 사용자 입력이 들어오는 곳은 반드시 `#{}`, SQL 구조를 동적으로 바꾸는 곳에만 `${}`를 제한적으로 사용한다.

---

### 3.3.4 resultType vs resultMap

**resultType**

쿼리 결과를 특정 클래스에 자동으로 매핑한다. `mybatis-config.xml`의 `mapUnderscoreToCamelCase` 설정이 켜져 있으면 컬럼명과 필드명이 규칙적으로 대응될 때 자동 매핑된다.

```xml
<!-- 컬럼 board_no → 필드 boardNo 자동 변환 -->
<select id="selectOne" resultType="Board">
    SELECT board_no, title, writer FROM board WHERE board_no = #{no}
</select>
```

**resultMap**

컬럼명과 필드명의 규칙이 다르거나, 1:N 조인 결과를 객체 그래프로 매핑해야 할 때 사용한다.

```xml
<resultMap id="boardResultMap" type="Board">
    <id     property="no"      column="board_no"/>
    <result property="title"   column="board_title"/>
    <result property="writer"  column="member_name"/>
    <!-- 조인으로 가져온 댓글 목록을 List<Comment>에 매핑 -->
    <collection property="comments" ofType="Comment">
        <id     property="commentNo" column="comment_no"/>
        <result property="content"   column="comment_content"/>
    </collection>
</resultMap>

<select id="selectWithComments" resultMap="boardResultMap">
    SELECT b.board_no, b.board_title, m.member_name,
           c.comment_no, c.comment_content
    FROM board b
    JOIN member m ON b.member_no = m.member_no
    LEFT JOIN comment c ON b.board_no = c.board_no
    WHERE b.board_no = #{no}
</select>
```

컬럼명과 필드명이 규칙적으로 대응된다면 `resultType`이 간결하다. 복잡한 조인이 있거나 명시적 매핑이 필요하면 `resultMap`을 사용한다.

---

### 3.3.5 resultMap의 association과 collection

실제 서비스에서는 하나의 테이블만 조회하는 경우보다 관련 테이블을 함께 조회해야 하는 경우가 많다. MyBatis는 조인 결과를 객체 그래프로 변환하기 위해 `association`(N:1)과 `collection`(1:N) 두 가지 매핑을 제공한다.

#### 관계 유형 이해

```
Board (게시글)
 ├── association → Member (작성자 정보, N:1 관계)
 │                 게시글 여러 개가 하나의 회원에 속함
 └── collection  → List<Attach> (첨부파일 목록, 1:N 관계)
                   게시글 하나에 첨부파일이 여러 개
```

#### 도메인 클래스 준비

```java
// Board.java — 연관 객체 필드 추가
public class Board {
    private int no;
    private String title;
    private String content;
    private int hit;
    private LocalDateTime createdDate;

    private Member member;           // association: 작성자 정보 (N:1)
    private List<Attach> attaches;   // collection: 첨부파일 목록 (1:N)
}

// Member.java
public class Member {
    private String memberId;
    private String name;
}

// Attach.java
public class Attach {
    private int attachNo;
    private int boardNo;
    private String originalName;
    private String savedName;
}
```

---

#### 방법 1 — JOIN + resultMap (단일 쿼리)

JOIN으로 관련 데이터를 한 번에 가져온 뒤, resultMap이 컬럼을 객체 그래프로 분배한다.

**association 예시 — Board와 Member를 JOIN**

```xml
<resultMap id="boardWithMemberMap" type="Board">
    <id     property="no"          column="no"/>
    <result property="title"       column="title"/>
    <result property="hit"         column="hit"/>
    <result property="createdDate" column="created_date"/>

    <!-- association: N:1 관계. member 필드에 Member 객체를 채운다 -->
    <association property="member" javaType="Member">
        <id     property="memberId" column="member_id"/>
        <result property="name"     column="member_name"/>
    </association>
</resultMap>

<select id="selectBoardWithMember" parameterType="int"
        resultMap="boardWithMemberMap">
    SELECT b.no, b.title, b.hit, b.created_date,
           m.member_id, m.name AS member_name
    FROM   board b
    JOIN   member m ON b.writer = m.member_id
    WHERE  b.no = #{no}
</select>
```

**collection 예시 — Board와 Attach를 LEFT JOIN**

```xml
<resultMap id="boardWithAttachMap" type="Board">
    <id     property="no"          column="no"/>
    <result property="title"       column="title"/>
    <result property="hit"         column="hit"/>
    <result property="createdDate" column="created_date"/>

    <!-- collection: 1:N 관계. attaches 필드에 Attach 객체 목록을 채운다 -->
    <collection property="attaches" ofType="Attach">
        <id     property="attachNo"      column="attach_no"/>
        <result property="originalName"  column="original_name"/>
        <result property="savedName"     column="saved_name"/>
    </collection>
</resultMap>

<select id="selectBoardWithAttaches" parameterType="int"
        resultMap="boardWithAttachMap">
    SELECT b.no, b.title, b.hit, b.created_date,
           a.attach_no, a.original_name, a.saved_name
    FROM   board b
    LEFT JOIN attach a ON b.no = a.board_no
    WHERE  b.no = #{no}
</select>
```

LEFT JOIN을 사용하는 이유는 첨부파일이 없는 게시글도 조회해야 하기 때문이다. INNER JOIN을 쓰면 첨부파일이 없는 게시글이 결과에서 제외된다.

MyBatis는 `<id>`로 지정한 컬럼 값을 기준으로 같은 게시글에 속하는 여러 행을 하나의 `Board` 객체로 합치고, `attaches` 리스트에 각 행의 첨부파일 데이터를 추가한다.

```
쿼리 결과 (4행)            MyBatis 변환 결과
no   title   attach_no    Board {
5    공지사항   10           no=5, title="공지사항",
5    공지사항   11           attaches=[
5    공지사항   12              Attach{attachNo=10},
5    공지사항   (null)          Attach{attachNo=11},
                               Attach{attachNo=12}
                           ]
                          }
```

**association + collection 동시 사용**

```xml
<resultMap id="boardFullMap" type="Board">
    <id     property="no"          column="no"/>
    <result property="title"       column="title"/>
    <result property="createdDate" column="created_date"/>

    <association property="member" javaType="Member">
        <id     property="memberId" column="member_id"/>
        <result property="name"     column="member_name"/>
    </association>

    <collection property="attaches" ofType="Attach">
        <id     property="attachNo"     column="attach_no"/>
        <result property="originalName" column="original_name"/>
    </collection>
</resultMap>

<select id="selectBoardFull" parameterType="int" resultMap="boardFullMap">
    SELECT b.no, b.title, b.created_date,
           m.member_id, m.name AS member_name,
           a.attach_no, a.original_name
    FROM   board b
    JOIN   member m ON b.writer = m.member_id
    LEFT JOIN attach a ON b.no = a.board_no
    WHERE  b.no = #{no}
</select>
```

---

#### 방법 2 — select 속성 (중첩 select)

resultMap 내부에서 `select` 속성으로 별도의 쿼리를 지정하면, 첫 쿼리 실행 후 관련 데이터를 가져오기 위한 추가 쿼리가 자동으로 실행된다.

```xml
<!-- 첨부파일을 별도 쿼리로 조회 -->
<resultMap id="boardWithAttachSelectMap" type="Board">
    <id     property="no"    column="no"/>
    <result property="title" column="title"/>

    <!-- board.no 값을 column으로 넘겨 selectAttachesByBoardNo 쿼리를 추가 실행 -->
    <collection property="attaches"
                ofType="Attach"
                select="selectAttachesByBoardNo"
                column="no"/>
</resultMap>

<select id="selectBoard" parameterType="int"
        resultMap="boardWithAttachSelectMap">
    SELECT no, title FROM board WHERE no = #{no}
</select>

<select id="selectAttachesByBoardNo" parameterType="int" resultType="Attach">
    SELECT attach_no, original_name, saved_name
    FROM   attach
    WHERE  board_no = #{no}
</select>
```

**N+1 문제 주의**

중첩 select 방식은 목록 조회 시 심각한 성능 문제를 유발할 수 있다.

```
목록 조회 (10건)
  쿼리 1: SELECT * FROM board LIMIT 0, 10  →  10건 반환
  쿼리 2~11: 각 게시글의 첨부파일 조회 (10번 추가 실행)
  총 쿼리 실행 횟수: 1 + 10 = 11회  ← N+1 문제
```

목록 조회처럼 여러 건을 한꺼번에 가져올 때는 JOIN 방식을, 단건 상세 조회처럼 하나의 레코드만 가져올 때는 중첩 select 방식을 선택적으로 사용한다.

---

#### association vs collection 선택 기준

| 관계 | 태그 | 자바 타입 | 예시 |
|------|------|-----------|------|
| N:1 (다대일) | `<association>` | 단일 객체 | 게시글 → 작성자 |
| 1:N (일대다) | `<collection>` | `List<T>` | 게시글 → 첨부파일 목록 |
| 1:1 (일대일) | `<association>` | 단일 객체 | 회원 → 프로필 |

---

### 3.3.6 어노테이션 기반 SQL

XML 매퍼 대신 Mapper 인터페이스 메서드에 직접 어노테이션으로 SQL을 작성할 수 있다. 간단한 쿼리에서 XML 파일 없이 빠르게 작성할 수 있다는 장점이 있다.

#### 기본 어노테이션

```java
@Mapper
public interface MemberMapper {

    // SELECT
    @Select("SELECT member_id, password, name, created_date FROM member WHERE member_id = #{memberId}")
    Member selectById(String memberId);

    // INSERT
    @Insert("INSERT INTO member (member_id, password, name) VALUES (#{memberId}, #{password}, #{name})")
    void insert(Member member);

    // UPDATE
    @Update("UPDATE member SET name = #{name} WHERE member_id = #{memberId}")
    void update(Member member);

    // DELETE
    @Delete("DELETE FROM member WHERE member_id = #{memberId}")
    void deleteById(String memberId);
}
```

어노테이션 내부 SQL에서도 `#{파라미터명}` 바인딩은 XML과 동일하게 동작한다.

---

#### INSERT 후 자동 생성 키 처리

XML의 `useGeneratedKeys`와 `keyProperty`에 해당하는 어노테이션은 `@Options`다.

```java
@Insert("INSERT INTO board (title, writer, content) VALUES (#{title}, #{writer}, #{content})")
@Options(useGeneratedKeys = true, keyProperty = "no")
void insert(Board board);
```

`@Options`는 SQL 실행 옵션을 지정하는 어노테이션으로, INSERT 후 생성된 PK를 `board.no` 필드에 자동으로 반영한다.

---

#### 결과 매핑 — @Results와 @Result

컬럼명과 필드명이 다를 때, XML의 `<resultMap>`에 해당하는 어노테이션은 `@Results`와 `@Result`다.

```java
@Select("SELECT attach_no, board_no, original_name, saved_name, file_size FROM attach WHERE board_no = #{boardNo}")
@Results(id = "attachResultMap", value = {
    @Result(property = "attachNo",     column = "attach_no",     id = true),
    @Result(property = "boardNo",      column = "board_no"),
    @Result(property = "originalName", column = "original_name"),
    @Result(property = "savedName",    column = "saved_name"),
    @Result(property = "fileSize",     column = "file_size")
})
List<Attach> selectByBoardNo(int boardNo);
```

`mapUnderscoreToCamelCase=true` 설정이 되어 있다면 `@Results` 없이 `resultType`에 해당하는 자동 매핑만으로도 충분한 경우가 많다.

---

#### 어노테이션 방식의 한계 — 동적 SQL

동적 SQL(`<if>`, `<where>`, `<set>`)은 어노테이션으로 표현하기 매우 불편하다. MyBatis가 제공하는 `@SelectProvider`, `@InsertProvider` 등의 Provider 방식으로 동적 SQL을 구현할 수 있지만, 가독성이 떨어지고 유지보수가 어렵다.

```java
// 어노테이션으로 동적 SQL을 작성하면 코드가 복잡해진다
@SelectProvider(type = BoardSqlProvider.class, method = "selectAll")
List<Board> selectAll(Map<String, Object> params);

// SQL을 별도 클래스에 문자열로 조합해야 한다
public class BoardSqlProvider {
    public String selectAll(Map<String, Object> params) {
        return new SQL() {{
            SELECT("no, title, writer, hit, created_date");
            FROM("board");
            if (params.get("searchKeyword") != null) {
                WHERE("title LIKE CONCAT('%', #{searchKeyword}, '%')");
            }
            ORDER_BY("no DESC");
        }}.toString();
    }
}
```

XML 방식에 비해 코드가 훨씬 복잡하고 SQL을 한눈에 파악하기 어렵다.

---

#### XML vs 어노테이션 선택 기준

| 상황 | 권장 방식 |
|------|-----------|
| 단순 CRUD (고정 SQL) | 어노테이션 |
| 동적 SQL (`<if>`, `<where>` 등) | XML |
| 복잡한 JOIN, resultMap | XML |
| SQL을 코드와 분리하여 관리하고 싶을 때 | XML |
| 빠른 프로토타이핑 | 어노테이션 |

실무에서는 **XML 방식이 표준**으로 사용된다. 동적 쿼리가 대부분이고, SQL을 별도 파일로 분리해 DBA나 다른 팀원이 검토하기 쉽기 때문이다. 어노테이션은 단순한 보조 쿼리나 소규모 프로젝트에서 제한적으로 사용한다.

---

## 3.4 게시글 목록 조회 (페이징 포함) / 상세 조회

### 3.4.1 기본 목록 조회

DAO는 Mapper 인터페이스를 주입받아 사용한다. 별도의 SQL 호출 코드 없이 메서드 하나로 쿼리가 실행된다.

```java
@Repository
public class BoardDaoImpl implements BoardDao {

    private final BoardMapper boardMapper;

    public BoardDaoImpl(BoardMapper boardMapper) {
        this.boardMapper = boardMapper;
    }

    @Override
    public List<Board> selectAll() {
        return boardMapper.selectAll(null);
    }
}
```

---

### 3.4.2 페이징 처리

페이징은 한 화면에 표시할 게시글 수를 제한하고, 이전/다음 페이지로 이동하는 기능이다. MariaDB/MySQL에서는 `LIMIT offset, pageSize` 구문으로 구현한다.

**페이징 계산 로직**

```
전체 게시글 수  : totalCount
한 페이지 크기  : pageSize   (예: 10)
현재 페이지    : currentPage (예: 3)
시작 행 번호   : offset = (currentPage - 1) * pageSize
              → (3 - 1) × 10 = 20번째 행부터 10개

전체 페이지 수  : totalPages = Math.ceil(totalCount / pageSize)
블록 내 페이지  : 한 번에 보여줄 페이지 번호 개수 (예: [1][2][3][4][5])
```

페이징 관련 정보를 하나의 객체로 묶어서 관리하면 컨트롤러와 뷰 코드가 깔끔해진다.

```java
public class PageInfo {
    private int currentPage;   // 현재 페이지
    private int pageSize;      // 한 페이지 게시글 수
    private int totalCount;    // 전체 게시글 수
    private int totalPages;    // 전체 페이지 수
    private int offset;        // SQL LIMIT의 시작 행 번호
    private int blockSize;     // 페이지 블록 크기 (한 번에 표시할 페이지 번호 수)
    private int startPage;     // 현재 블록의 시작 페이지 번호
    private int endPage;       // 현재 블록의 끝 페이지 번호

    public PageInfo(int currentPage, int pageSize, int totalCount, int blockSize) {
        this.currentPage = currentPage;
        this.pageSize    = pageSize;
        this.totalCount  = totalCount;
        this.blockSize   = blockSize;

        // 계산
        this.totalPages = (int) Math.ceil((double) totalCount / pageSize);
        this.offset     = (currentPage - 1) * pageSize;

        // 페이지 블록 계산 (예: blockSize=5, currentPage=7 → startPage=6, endPage=10)
        this.startPage  = ((currentPage - 1) / blockSize) * blockSize + 1;
        this.endPage    = Math.min(startPage + blockSize - 1, totalPages);
    }
}
```

컨트롤러에서 페이징 처리 흐름은 다음과 같다.

```java
@GetMapping("/list")
public String list(@RequestParam(defaultValue = "1") int page, Model model) {

    int pageSize   = 10;
    int totalCount = boardService.getTotalCount();  // SELECT COUNT(*)
    PageInfo pageInfo = new PageInfo(page, pageSize, totalCount, 5);

    Map<String, Object> params = new HashMap<>();
    params.put("offset",   pageInfo.getOffset());
    params.put("pageSize", pageInfo.getPageSize());

    model.addAttribute("list",     boardService.getList(params));
    model.addAttribute("pageInfo", pageInfo);
    return "board/list";
}
```

JSP에서는 `pageInfo`의 `startPage`부터 `endPage`까지 반복문으로 페이지 번호를 출력하고, 각 번호에 `/board/list?page=N` 링크를 건다.

---

### 3.4.3 상세 조회와 조회수 증가

게시글 상세 조회 시 조회수를 1 증가시키는 것이 일반적이다. UPDATE와 SELECT를 순서대로 실행한다.

```java
// Service
@Override
public Board getDetail(int no) {
    boardMapper.increaseHit(no);   // 조회수 +1
    return boardMapper.selectOne(no);
}
```

```xml
<!-- Mapper XML -->
<update id="increaseHit" parameterType="int">
    UPDATE board SET hit = hit + 1 WHERE no = #{no}
</update>
```

두 SQL이 하나의 논리적 작업 단위를 이루므로 트랜잭션으로 묶어야 한다. 트랜잭션 관리는 4일차에서 다루며, 여기서는 동작 구조만 이해한다.

---

## 3.5 게시글 등록 — 폼 데이터 바인딩과 @ModelAttribute

### 3.5.1 등록 폼 표시와 등록 처리 분리

같은 URL(`/board/write`)에 대해 GET 요청은 폼 화면을, POST 요청은 등록 처리를 담당한다. HTTP 메서드로 역할을 구분하는 것이 RESTful 설계의 기본이다.

```java
@GetMapping("/write")
public String writeForm() {
    return "board/writeForm";  // 빈 폼 화면 표시
}

@PostMapping("/write")
public String write(Board board) {
    boardService.register(board);
    return "redirect:/board/list";  // PRG 패턴
}
```

---

### 3.5.2 @ModelAttribute

커맨드 객체(폼 파라미터를 담는 VO)에 `@ModelAttribute`를 명시하면 두 가지 일이 일어난다.

**① 폼 데이터 자동 바인딩**

HTTP 요청 파라미터의 이름과 객체 필드명이 일치하면 자동으로 값이 채워진다.

```java
// POST /board/write
// 폼 파라미터: title=스프링공부&writer=홍길동&content=내용입니다
@PostMapping("/write")
public String write(@ModelAttribute Board board) {
    // board.getTitle()   → "스프링공부"
    // board.getWriter()  → "홍길동"
    // board.getContent() → "내용입니다"
    boardService.register(board);
    return "redirect:/board/list";
}
```

`@ModelAttribute`는 파라미터 타입이 커맨드 객체이면 생략할 수 있다. 단, 명시하는 쪽이 의도가 명확해 가독성이 좋다.

**② Model에 자동 등록**

`@ModelAttribute`가 붙은 객체는 자동으로 `Model`에 추가된다. 기본 키는 클래스명의 첫 글자를 소문자로 바꾼 이름이다(`Board` → `board`). 등록 실패 시 폼 화면으로 돌아갈 때 입력했던 값을 다시 채워주는 데 활용된다.

---

### 3.5.3 자동 생성 키 처리 (useGeneratedKeys)

INSERT 후 DB가 자동으로 생성한 기본키(auto_increment) 값을 객체에 돌려받을 수 있다.

```xml
<insert id="insert" parameterType="Board"
        useGeneratedKeys="true" keyProperty="no">
    INSERT INTO board (title, writer, content)
    VALUES (#{title}, #{writer}, #{content})
</insert>
```

`useGeneratedKeys="true"` — DB의 자동 생성 키를 사용하겠다는 선언  
`keyProperty="no"` — 생성된 키 값을 `Board` 객체의 `no` 필드에 저장한다

INSERT 실행 후 `board.getNo()`를 호출하면 DB가 할당한 PK 값을 얻을 수 있다. 등록 후 상세 페이지로 바로 이동할 때 유용하다.

```java
@PostMapping("/write")
public String write(Board board) {
    boardService.register(board);
    // board.getNo() 에 DB가 생성한 PK가 담겨 있다
    return "redirect:/board/detail?no=" + board.getNo();
}
```

---

## 3.6 게시글 수정·삭제와 PRG 패턴

### 3.6.1 수정 흐름

수정은 세 단계로 구성된다.

```
① GET /board/edit?no=5    → 기존 데이터를 폼에 채워서 표시
② POST /board/edit        → 수정 처리 (UPDATE 실행)
③ redirect → /board/detail?no=5  → 수정된 상세 화면으로 이동
```

```java
@GetMapping("/edit")
public String editForm(@RequestParam int no, Model model) {
    model.addAttribute("board", boardService.getDetail(no));
    return "board/editForm";  // 기존 데이터가 채워진 폼 화면
}

@PostMapping("/edit")
public String edit(Board board) {
    boardService.modify(board);
    return "redirect:/board/detail?no=" + board.getNo();
}
```

수정 폼 JSP에서는 `${board.no}`를 `<input type="hidden">`으로 넣어 PK 값을 함께 전송해야 한다.

```jsp
<form action="/board/edit" method="post">
    <input type="hidden" name="no" value="${board.no}"/>
    <input type="text"   name="title"   value="${board.title}"/>
    <textarea name="content">${board.content}</textarea>
    <button type="submit">수정</button>
</form>
```

---

### 3.6.2 삭제 흐름

삭제는 데이터를 변경하는 작업이므로 GET이 아닌 POST를 사용한다. GET 방식으로 삭제를 구현하면 브라우저 캐시, 크롤러 등으로 인해 의도치 않은 삭제가 발생할 수 있다.

```java
@PostMapping("/delete")
public String delete(@RequestParam int no) {
    boardService.remove(no);
    return "redirect:/board/list";
}
```

삭제 버튼은 일반 링크(`<a>`)가 아니라 폼 형태로 만들어야 POST 요청이 가능하다.

```jsp
<form action="/board/delete" method="post">
    <input type="hidden" name="no" value="${board.no}"/>
    <button type="submit"
            onclick="return confirm('삭제하시겠습니까?')">삭제</button>
</form>
```

---

### 3.6.3 PRG(Post-Redirect-Get) 패턴

POST 요청 처리 후 뷰를 직접 렌더링하면 브라우저 새로고침(F5) 시 POST 요청이 재전송되어 등록/수정/삭제가 중복 실행된다.

```
PRG 패턴 적용 전 (문제)
  POST /board/write → 등록 처리 → list.jsp 직접 렌더링
  [새로고침] → POST /board/write 재전송 → 게시글 중복 등록!

PRG 패턴 적용 후 (해결)
  POST /board/write → 등록 처리 → redirect → GET /board/list
  [새로고침] → GET /board/list 재전송 → 안전
```

스프링 MVC에서 PRG 패턴은 컨트롤러에서 `"redirect:URL"` 문자열을 반환하는 것으로 구현한다. 등록, 수정, 삭제를 처리하는 모든 POST 핸들러 메서드는 반드시 redirect로 응답해야 한다.

---

### 3.6.4 동적 쿼리 — `<if>`, `<where>`, `<set>` 태그

검색 조건이 있을 수도 있고 없을 수도 있는 경우, MyBatis의 동적 SQL 태그를 사용하면 조건에 따라 SQL이 자동으로 구성된다.

**`<if>` — 조건이 참일 때만 SQL 조각 포함**

```xml
<select id="selectAll" parameterType="map" resultType="Board">
    SELECT * FROM board
    <where>
        <!-- searchKeyword 값이 있을 때만 WHERE 조건 추가 -->
        <if test="searchKeyword != null and searchKeyword != ''">
            title LIKE CONCAT('%', #{searchKeyword}, '%')
        </if>
    </where>
    ORDER BY no DESC
    LIMIT #{offset}, #{pageSize}
</select>
```

`<where>` 태그는 내부에 포함된 조건이 하나 이상 있을 때만 `WHERE` 키워드를 붙이고, 앞에 오는 `AND`/`OR`를 자동으로 제거한다.

**`<set>` — UPDATE 시 수정할 컬럼 동적 선택**

```xml
<update id="update" parameterType="Board">
    UPDATE board
    <set>
        <if test="title   != null">title   = #{title},</if>
        <if test="content != null">content = #{content},</if>
    </set>
    WHERE no = #{no}
</update>
```

`<set>` 태그는 내부 조건 중 참인 것들을 `,`로 연결하고, 마지막 `,`는 자동으로 제거한다.

---

## 3.7 실습 — 완전한 게시판 CRUD 완성

### 실습 목표

2일차에 구성한 Spring MVC 프로젝트에 MyBatis-Spring 연동을 추가하고, 실제 MariaDB를 연동한 완전한 게시판 CRUD를 구현한다.

---

### 실습 전 DB 준비

```sql
CREATE DATABASE boarddb DEFAULT CHARACTER SET utf8mb4;
USE boarddb;

CREATE TABLE board (
    no           INT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    writer       VARCHAR(50)  NOT NULL,
    content      TEXT,
    hit          INT          DEFAULT 0,
    created_date DATETIME     DEFAULT CURRENT_TIMESTAMP
);

-- 테스트 데이터
INSERT INTO board (title, writer, content)
VALUES ('첫 번째 게시글', '홍길동', '스프링 MVC 학습 중입니다.'),
       ('두 번째 게시글', '이순신', 'MyBatis 연동 완료!'),
       ('세 번째 게시글', '강감찬', 'CRUD 구현 중입니다.');
```

---

### 실습 환경

```
spring-day3/
├── pom.xml
└── src/main/
    ├── java/com/spring/board/
    │   ├── Board.java
    │   ├── PageInfo.java
    │   ├── BoardDao.java / BoardDaoImpl.java
    │   ├── BoardService.java / BoardServiceImpl.java
    │   ├── BoardController.java
    │   └── mapper/
    │       └── BoardMapper.java
    ├── resources/
    │   ├── db.properties
    │   ├── mybatis-config.xml
    │   ├── applicationContext.xml
    │   └── mapper/board/
    │       └── BoardMapper.xml
    └── webapp/WEB-INF/
        ├── web.xml
        ├── spring-mvc.xml
        └── views/board/
            ├── list.jsp        ← 목록 + 페이징
            ├── detail.jsp      ← 상세 + 조회수
            ├── writeForm.jsp   ← 등록 폼
            └── editForm.jsp    ← 수정 폼
```

---

### 실습 순서

**Step 1 — pom.xml에 MyBatis 관련 의존성 추가**

mybatis, mybatis-spring, HikariCP, MariaDB 드라이버, spring-jdbc를 추가한다.

**Step 2 — DB 준비 및 db.properties 작성**

MariaDB에 `boarddb` 데이터베이스와 `board` 테이블을 생성하고, DB 연결 정보를 `db.properties`에 작성한다.

**Step 3 — applicationContext.xml 에 DataSource, SqlSessionFactory, MapperScan 설정 추가**

`context:property-placeholder`로 properties 파일을 로드하고, HikariCP DataSource, SqlSessionFactoryBean, MapperScannerConfigurer를 순서대로 등록한다.

**Step 4 — mybatis-config.xml 작성**

`mapUnderscoreToCamelCase=true`와 `typeAliases`를 설정한다.

**Step 5 — BoardMapper 인터페이스와 BoardMapper.xml 작성**

CRUD 메서드를 인터페이스에 선언하고, XML 매퍼에 대응하는 SQL을 작성한다. `namespace`가 인터페이스 경로와 정확히 일치하는지 확인한다.

**Step 6 — BoardDaoImpl 수정**

메모리 데이터 대신 BoardMapper를 주입받아 사용하도록 변경한다.

**Step 7 — CRUD 기능 순서대로 완성 및 테스트**

목록 → 상세(조회수 증가 확인) → 등록(PRG 확인) → 수정 → 삭제 순서로 완성하고 각 기능을 브라우저에서 테스트한다.

**Step 8 — 페이징 추가**

`PageInfo` 클래스를 작성하고, 목록 컨트롤러와 JSP에 페이징 UI를 추가한다. 10건 이상의 테스트 데이터를 INSERT하고 페이지 이동을 확인한다.

**Step 9 — 동적 쿼리로 검색 기능 추가**

제목 검색 입력창을 목록 JSP에 추가하고, `<if>`와 `<where>` 태그를 사용해 검색 조건이 있을 때만 `LIKE` 조건이 추가되도록 구현한다.

---

### 주요 확인 포인트

| 항목 | 확인 방법 |
|---|---|
| DataSource 연결 | Tomcat 시작 시 HikariCP 풀 생성 로그 확인 |
| Mapper 스캔 | 잘못된 `namespace`→ `Invalid bound statement` 예외 확인 |
| 자동 생성 키 | INSERT 후 `board.getNo()` 값 출력으로 확인 |
| camelCase 변환 | `created_date` 컬럼이 `createdDate` 필드에 정상 매핑 확인 |
| PRG 패턴 | 등록 후 브라우저 URL이 `/board/list`로 변경되는지 확인 |
| 중복 제출 방지 | 등록 처리 후 새로고침 시 재등록 안 됨 확인 |

---

## 정리

**DataSource**는 커넥션 풀을 통해 DB 연결 비용을 줄이는 표준 인터페이스다. 개발 단계에서는 `DriverManagerDataSource`, 운영 환경에서는 **HikariCP**를 사용한다. DB 연결 정보는 `.properties` 파일로 외부화하여 관리한다.

**MyBatis-Spring** 연동의 핵심은 세 가지 Bean이다. DataSource를 참조하는 **SqlSessionFactory**, Mapper 인터페이스를 자동 Bean으로 등록하는 **MapperScannerConfigurer**, 개발자가 직접 작성하는 **Mapper 인터페이스와 XML 매퍼**다.

SQL 파라미터 바인딩은 SQL 인젝션 방지를 위해 반드시 **`#{}`** 을 사용하고, SQL 구조를 동적으로 바꿔야 할 때만 `${}`를 제한적으로 사용한다.

연관 객체를 함께 조회할 때는 **resultMap의 `<association>`(N:1)과 `<collection>`(1:N)** 을 사용한다. JOIN 방식(단일 쿼리)은 성능이 좋고, 중첩 select 방식은 코드가 단순하지만 목록 조회 시 N+1 문제가 발생하므로 주의해야 한다.

단순한 고정 SQL은 **`@Select/@Insert/@Update/@Delete`** 어노테이션으로 XML 없이 작성할 수 있다. 그러나 동적 SQL이 필요하거나 SQL을 코드와 분리해 관리해야 하는 실무 환경에서는 **XML 방식이 표준**이다.

POST 요청을 처리하는 모든 컨트롤러 메서드는 **PRG 패턴**을 적용하여 폼 중복 제출을 방지한다. 페이징은 전체 건수 조회 + `LIMIT offset, pageSize` SQL을 조합하며, `PageInfo` 객체로 페이지 계산 로직을 캡슐화한다.

---

## 다음 시간 예고

4일차에서는 **AOP**로 로깅·예외 처리 공통 관심사를 분리하고, **@Transactional**로 조회수 증가·수정 같은 복합 DB 작업에 트랜잭션을 적용한다. 또한 **HandlerInterceptor**로 로그인 체크를 구현하고, 세션 기반 로그인 및 파일 업로드를 게시판에 추가한다.
