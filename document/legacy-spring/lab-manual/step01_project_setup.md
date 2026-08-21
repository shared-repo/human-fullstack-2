# 단계 1. 프로젝트 환경 설정

> **목표** : 스프링 프레임워크 기반의 Maven 웹 프로젝트를 생성하고, DB 연결까지 완료합니다.
> **소요 시간** : 약 2시간

---

## 1.1 프로젝트 생성

### 1.1.1 Eclipse에서 Maven 프로젝트 만들기

1. Eclipse 상단 메뉴에서 **File → New → Other** 를 클릭합니다.
2. 검색창에 `Maven` 을 입력한 뒤 **Maven Project** 를 선택하고 **Next** 를 클릭합니다.
3. **Create a simple project (skip archetype selection)** 에 체크하고 **Next** 를 클릭합니다.
4. 아래와 같이 입력합니다.

   | 항목 | 입력값 |
   |---|---|
   | Group Id | `com.food` |
   | Artifact Id | `food-note` |
   | Packaging | `war` ← 반드시 **war** 로 변경 |

   > ⚠️ Packaging 기본값은 `jar` 입니다. 웹 프로젝트이므로 반드시 `war` 로 바꿔야 합니다.

5. **Finish** 를 클릭합니다.

---

### 1.1.2 프로젝트 기본 구조 확인

프로젝트가 생성되면 왼쪽 Package Explorer에서 아래 구조가 보여야 합니다.

```
food-note
├── src
│   ├── main
│   │   ├── java          ← Java 소스 파일을 작성하는 폴더
│   │   ├── resources     ← 설정 파일(xml, properties)을 작성하는 폴더
│   │   └── webapp        ← JSP, HTML, CSS, 설정파일(web.xml)을 작성하는 폴더
│   └── test
│       ├── java
│       └── resources
└── pom.xml
```

> 폴더가 보이지 않으면 프로젝트를 오른쪽 클릭 → **Maven → Update Project** 를 실행합니다.

---

### 1.1.3 WEB-INF 폴더 및 필수 디렉터리 생성

`src/main/webapp` 안에 아래 구조를 직접 만들어야 합니다.

```
webapp
├── WEB-INF
│   ├── spring          ← 스프링 설정 XML 파일을 저장하는 폴더
│   └── views           ← JSP 파일을 저장하는 폴더
└── resources
    └── css             ← CSS 파일을 저장하는 폴더 (나중에 사용)
```

**폴더 만드는 방법**

1. `src/main/webapp` 를 오른쪽 클릭합니다.
2. **New → Folder** 를 선택합니다.
3. 폴더 이름을 입력하고 **Finish** 를 클릭합니다.
4. 위 구조가 모두 생길 때까지 반복합니다.

---

## 1.2 pom.xml 작성

`pom.xml` 은 프로젝트에서 사용할 라이브러리를 정의하는 파일입니다.  
아래 내용 전체를 복사하여 기존 `pom.xml` 내용과 **완전히 교체**합니다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.food</groupId>
    <artifactId>food-note</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>war</packaging>

    <properties>
        <java.version>21</java.version>
        <spring.version>7.0.7</spring.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>

        <!-- ======================== -->
        <!--  Spring MVC             -->
        <!-- ======================== -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-webmvc</artifactId>
            <version>${spring.version}</version>
        </dependency>

        <!-- 트랜잭션 관리에 필요 -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-jdbc</artifactId>
            <version>${spring.version}</version>
        </dependency>

        <!-- ======================== -->
        <!--  MyBatis                -->
        <!-- ======================== -->
        <dependency>
            <groupId>org.mybatis</groupId>
            <artifactId>mybatis</artifactId>
            <version>3.5.16</version>
        </dependency>
        <dependency>
            <groupId>org.mybatis</groupId>
            <artifactId>mybatis-spring</artifactId>
            <version>3.0.3</version>
        </dependency>

        <!-- ======================== -->
        <!--  DB                     -->
        <!-- ======================== -->
        <!-- 커넥션 풀 -->
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
            <version>5.1.0</version>
        </dependency>
        <!-- MariaDB 드라이버 -->
        <dependency>
            <groupId>org.mariadb.jdbc</groupId>
            <artifactId>mariadb-java-client</artifactId>
            <version>3.3.3</version>
        </dependency>

        <!-- ======================== -->
        <!--  웹 (Servlet, JSP, JSTL) -->
        <!-- ======================== -->
        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
            <version>6.1.0</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>jakarta.servlet.jsp.jstl</groupId>
            <artifactId>jakarta.servlet.jsp.jstl-api</artifactId>
            <version>3.0.0</version>
        </dependency>
        <dependency>
            <groupId>org.glassfish.web</groupId>
            <artifactId>jakarta.servlet.jsp.jstl</artifactId>
            <version>3.0.1</version>
        </dependency>

        <!-- ======================== -->
        <!--  편의 도구               -->
        <!-- ======================== -->
        <!-- Getter/Setter 자동 생성 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.32</version>
            <scope>provided</scope>
        </dependency>

        <!-- ======================== -->
        <!--  로그                   -->
        <!-- ======================== -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>2.0.9</version>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.4.14</version>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-war-plugin</artifactId>
                <version>3.4.0</version>
            </plugin>
        </plugins>
    </build>

</project>
```

**저장 후 확인**

파일을 저장하면 Eclipse가 자동으로 라이브러리를 내려받습니다.  
하단 Progress 창에 다운로드 진행 표시가 사라질 때까지 기다립니다.

> ⚠️ 인터넷 연결이 필요합니다. 다운로드 중에 다른 작업을 진행하지 않습니다.

---

## 1.3 web.xml 작성

`web.xml` 은 웹 애플리케이션의 시작점을 정의하는 파일입니다.  
`src/main/webapp/WEB-INF/` 폴더 안에 `web.xml` 파일을 새로 만듭니다.

**파일 만드는 방법**

1. `WEB-INF` 폴더를 오른쪽 클릭합니다.
2. **New → File** 을 선택합니다.
3. 파일 이름에 `web.xml` 을 입력하고 **Finish** 를 클릭합니다.

아래 내용을 전체 복사하여 붙여넣습니다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
                             https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
         version="6.0">

    <!-- ① 한글 깨짐 방지 필터 -->
    <filter>
        <filter-name>encodingFilter</filter-name>
        <filter-class>org.springframework.web.filter.CharacterEncodingFilter</filter-class>
        <init-param>
            <param-name>encoding</param-name>
            <param-value>UTF-8</param-value>
        </init-param>
        <init-param>
            <param-name>forceEncoding</param-name>
            <param-value>true</param-value>
        </init-param>
    </filter>
    <filter-mapping>
        <filter-name>encodingFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

    <!-- ② 루트 애플리케이션 컨텍스트 설정 (Service, DAO, DB 관련 Bean) -->
    <context-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>/WEB-INF/spring/applicationContext.xml</param-value>
    </context-param>
    <listener>
        <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
    </listener>

    <!-- ③ 스프링 MVC 프론트 컨트롤러 (Controller, View 관련 Bean) -->
    <servlet>
        <servlet-name>dispatcher</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>/WEB-INF/spring/spring-mvc.xml</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>dispatcher</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>

</web-app>
```

> **각 설정의 역할 요약**
>
> - **encodingFilter** : 요청·응답의 문자 인코딩을 UTF-8로 강제합니다. 한글 깨짐을 방지합니다.
> - **ContextLoaderListener** : 서버 시작 시 `applicationContext.xml` 을 읽어 DB 연결, Service, DAO 등의 Bean을 생성합니다.
> - **DispatcherServlet** : 모든 HTTP 요청을 받아 적절한 Controller로 전달하는 스프링 MVC의 핵심입니다.

---

## 1.4 applicationContext.xml 작성

DB 연결, MyBatis, 트랜잭션 관련 Bean을 정의합니다.  
`src/main/webapp/WEB-INF/spring/` 폴더 안에 `applicationContext.xml` 파일을 만듭니다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:tx="http://www.springframework.org/schema/tx"
       xsi:schemaLocation="
           http://www.springframework.org/schema/beans
           http://www.springframework.org/schema/beans/spring-beans.xsd
           http://www.springframework.org/schema/context
           http://www.springframework.org/schema/context/spring-context.xsd
           http://www.springframework.org/schema/tx
           http://www.springframework.org/schema/tx/spring-tx.xsd">

    <!-- Service, DAO 클래스를 자동으로 Bean으로 등록 (Controller 제외) -->
    <context:component-scan base-package="com.food">
        <context:exclude-filter type="annotation"
            expression="org.springframework.stereotype.Controller"/>
    </context:component-scan>

    <!-- ① HikariCP 커넥션 풀 (DB 연결 정보) -->
    <bean id="dataSource" class="com.zaxxer.hikari.HikariDataSource" destroy-method="close">
        <property name="driverClassName" value="org.mariadb.jdbc.Driver"/>
        <property name="jdbcUrl"         value="jdbc:mariadb://localhost:3306/foodnote?characterEncoding=UTF-8"/>
        <property name="username"        value="root"/>
        <property name="password"        value="1234"/>
        <property name="maximumPoolSize" value="10"/>
    </bean>

    <!-- ② MyBatis SqlSessionFactory -->
    <bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
        <property name="dataSource"      ref="dataSource"/>
        <property name="configLocation"  value="classpath:mybatis-config.xml"/>
        <property name="mapperLocations" value="classpath:mappers/**/*.xml"/>
    </bean>

    <!-- ③ Mapper 인터페이스 자동 등록 -->
    <bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
        <property name="basePackage" value="com.food.mapper"/>
    </bean>

    <!-- ④ 트랜잭션 관리자 -->
    <bean id="transactionManager"
          class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        <property name="dataSource" ref="dataSource"/>
    </bean>

    <!-- ⑤ @Transactional 어노테이션 활성화 -->
    <tx:annotation-driven transaction-manager="transactionManager"/>

</beans>
```

> ⚠️ **DB 연결 정보 수정**
>
> 강사가 안내한 실습 환경에 맞게 아래 항목을 수정합니다.
>
> | 항목 | 기본값 | 수정 필요 여부 |
> |---|---|---|
> | `jdbcUrl` 의 포트(3306) | MariaDB 기본 포트 | 변경된 경우만 수정 |
> | `jdbcUrl` 의 DB명(foodnote) | 실습 DB 이름 | **확인 후 수정** |
> | `username` | root | **확인 후 수정** |
> | `password` | 1234 | **확인 후 수정** |

---

## 1.5 spring-mvc.xml 작성

Controller, ViewResolver, Interceptor 관련 Bean을 정의합니다.  
`src/main/webapp/WEB-INF/spring/` 폴더 안에 `spring-mvc.xml` 파일을 만듭니다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:mvc="http://www.springframework.org/schema/mvc"
       xsi:schemaLocation="
           http://www.springframework.org/schema/beans
           http://www.springframework.org/schema/beans/spring-beans.xsd
           http://www.springframework.org/schema/context
           http://www.springframework.org/schema/context/spring-context.xsd
           http://www.springframework.org/schema/mvc
           http://www.springframework.org/schema/mvc/spring-mvc.xsd">

    <!-- Controller 클래스를 자동으로 Bean으로 등록 -->
    <context:component-scan base-package="com.food.controller"/>

    <!-- @RequestMapping, @GetMapping 등 MVC 어노테이션 활성화 -->
    <mvc:annotation-driven/>

    <!-- CSS, JS 같은 정적 파일은 스프링이 처리하지 않고 직접 제공 -->
    <mvc:default-servlet-handler/>

    <!-- JSP 파일 경로 설정 -->
    <!-- "board/list" → /WEB-INF/views/board/list.jsp 로 변환 -->
    <bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="prefix" value="/WEB-INF/views/"/>
        <property name="suffix" value=".jsp"/>
    </bean>

</beans>
```

---

## 1.6 mybatis-config.xml 작성

MyBatis 전역 설정 파일입니다.  
`src/main/resources/` 폴더 안에 `mybatis-config.xml` 파일을 만듭니다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration
    PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>

    <settings>
        <!-- DB 컬럼명 visit_date → visitDate 자동 변환 -->
        <setting name="mapUnderscoreToCamelCase" value="true"/>
        <!-- null 값도 파라미터로 전달 -->
        <setting name="jdbcTypeForNull" value="NULL"/>
    </settings>

    <typeAliases>
        <!-- 도메인 클래스 짧은 이름 등록 (단계 2에서 클래스 작성 후 채워집니다) -->
        <!-- <typeAlias type="com.food.domain.Member"     alias="Member"/> -->
        <!-- <typeAlias type="com.food.domain.Restaurant" alias="Restaurant"/> -->
    </typeAliases>

</configuration>
```

> 주석 처리된 `<typeAlias>` 부분은 단계 2에서 도메인 클래스를 만든 후 주석을 해제합니다.

---

## 1.7 logback.xml 작성

콘솔에 로그를 출력하기 위한 설정입니다.  
`src/main/resources/` 폴더 안에 `logback.xml` 파일을 만듭니다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss} [%level] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 내가 작성한 코드는 DEBUG 수준까지 출력 -->
    <logger name="com.food" level="DEBUG"/>

    <!-- MyBatis SQL 실행 내용 출력 -->
    <logger name="com.food.mapper" level="TRACE"/>

    <!-- 스프링 프레임워크 로그는 WARN 이상만 출력 -->
    <root level="WARN">
        <appender-ref ref="CONSOLE"/>
    </root>

</configuration>
```

---

## 1.8 mappers 폴더 생성

Mapper XML 파일을 저장할 폴더를 미리 만들어 둡니다.  
`src/main/resources/` 폴더 안에 `mappers` 폴더를 생성합니다.

```
src/main/resources
├── mappers           ← 이 폴더를 만듭니다
├── mybatis-config.xml
└── logback.xml
```

---

## 1.9 패키지 구조 생성

`src/main/java` 아래에 아래 패키지를 모두 만듭니다.

| 패키지 | 역할 |
|---|---|
| `com.food.domain` | Member, Restaurant 도메인 클래스 |
| `com.food.mapper` | Mapper 인터페이스 |
| `com.food.service` | Service 인터페이스 |
| `com.food.service.impl` | Service 구현 클래스 |
| `com.food.controller` | Controller 클래스 |
| `com.food.interceptor` | LoginCheckInterceptor |

**패키지 만드는 방법**

1. `src/main/java` 를 오른쪽 클릭합니다.
2. **New → Package** 를 선택합니다.
3. 패키지 이름을 입력하고 **Finish** 를 클릭합니다.
4. 위 6개의 패키지를 모두 만듭니다.

---

## 1.10 DB 및 설정 파일 연결 확인

### 1.10.1 DB 생성

MariaDB에 접속하여 아래 SQL을 실행합니다. (HeidiSQL, DBeaver 등 사용)

```sql
-- DB 생성
CREATE DATABASE foodnote
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

-- 생성된 DB 확인
SHOW DATABASES;
```

`foodnote` 가 목록에 보이면 성공입니다.

### 1.10.2 HomeController 작성

Spring MVC 설정이 올바르게 동작하는지 확인하기 위해 가장 간단한 Controller를 먼저 만들어 봅니다.  
`com.food.controller` 패키지 안에 `HomeController.java` 파일을 만들고 아래 내용을 작성합니다.

```java
package com.food.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "home";   // WEB-INF/views/home.jsp 로 이동
    }
}
```

> **`@Controller` 와 `@GetMapping`**
>
> - `@Controller` : 이 클래스가 요청을 처리하는 컨트롤러임을 스프링에 알립니다.
> - `@GetMapping("/")` : 브라우저에서 `http://localhost:8080/food-note/` 로 접속했을 때 이 메서드가 실행됩니다.
> - `return "home"` : `spring-mvc.xml` 의 ViewResolver 설정에 의해 `/WEB-INF/views/home.jsp` 파일을 화면에 보여줍니다.

---

### 1.10.3 home.jsp 작성

`src/main/webapp/WEB-INF/views/` 폴더 안에 `home.jsp` 파일을 만듭니다.

**파일 만드는 방법**

1. `WEB-INF/views` 폴더를 오른쪽 클릭합니다.
2. **New → File** 을 선택합니다.
3. 파일 이름에 `home.jsp` 를 입력하고 **Finish** 를 클릭합니다.

아래 내용을 작성합니다.

```jsp
<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>나만의 맛집 노트</title>
</head>
<body>

<h1>🍽 나만의 맛집 노트</h1>

<p>나만의 맛집을 기록하고 관리하는 공간입니다.</p>

<a href="/member/login">로그인</a>
<a href="/member/register">회원가입</a>

</body>
</html>
```

---

### 1.10.4 서버 실행 및 브라우저 확인

1. 프로젝트를 오른쪽 클릭합니다.
2. **Run As → Run on Server** 를 선택합니다.
3. Tomcat 서버를 선택하고 **Finish** 를 클릭합니다.

**성공 기준**

| 확인 항목 | 기대 결과 |
|---|---|
| Console 탭 | `Server startup in [xxxx] milliseconds` 메시지 출력 |
| 브라우저에서 `http://localhost:8080/food-note/` 접속 | "🍽 나만의 맛집 노트" 제목과 로그인·회원가입 링크가 보임 |

> ⚠️ **오류가 발생하면 아래를 순서대로 확인합니다.**
>
> 1. `pom.xml` 의 라이브러리가 모두 다운로드됐는지 확인합니다.  
>    (프로젝트 우클릭 → **Maven → Update Project**)
> 2. `applicationContext.xml` 의 DB 접속 정보(사용자명/비밀번호/DB명)가 맞는지 확인합니다.
> 3. MariaDB 서비스가 실행 중인지 확인합니다.
> 4. `web.xml` 의 파일 경로(`/WEB-INF/spring/applicationContext.xml`)가 실제 파일 위치와 일치하는지 확인합니다.

---

## 1.11 최종 디렉터리 구조 확인

단계 1이 완료된 후 프로젝트 구조가 아래와 같은지 확인합니다.

```
food-note
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── food
│   │   │           ├── controller
│   │   │           │   └── HomeController.java     ✅
│   │   │           ├── domain          (패키지, 비어 있음)
│   │   │           ├── interceptor     (패키지, 비어 있음)
│   │   │           ├── mapper          (패키지, 비어 있음)
│   │   │           ├── service         (패키지, 비어 있음)
│   │   │           └── service
│   │   │               └── impl       (패키지, 비어 있음)
│   │   ├── resources
│   │   │   ├── mappers                 (폴더, 비어 있음)
│   │   │   ├── logback.xml             ✅
│   │   │   └── mybatis-config.xml      ✅
│   │   └── webapp
│   │       ├── resources
│   │       │   └── css                 (폴더, 비어 있음)
│   │       └── WEB-INF
│   │           ├── spring
│   │           │   ├── applicationContext.xml  ✅
│   │           │   └── spring-mvc.xml          ✅
│   │           ├── views
│   │           │   └── home.jsp                    ✅
│   │           └── web.xml             ✅
└── pom.xml                             ✅
```

---

## ✅ 단계 1 완료 체크리스트

아래 항목을 하나씩 확인하고 완료된 항목에 체크합니다.

- [ ] Maven 프로젝트가 생성됐고, Packaging이 `war` 입니다.
- [ ] `pom.xml` 을 작성했고 라이브러리 다운로드가 완료됐습니다.
- [ ] `WEB-INF/spring/`, `WEB-INF/views/` 폴더가 존재합니다.
- [ ] `web.xml` 파일이 작성됐습니다.
- [ ] `applicationContext.xml` 파일이 작성됐습니다. (DB 접속 정보 수정 완료)
- [ ] `spring-mvc.xml` 파일이 작성됐습니다.
- [ ] `mybatis-config.xml` 파일이 작성됐습니다.
- [ ] `logback.xml` 파일이 작성됐습니다.
- [ ] `src/main/resources/mappers/` 폴더가 존재합니다.
- [ ] 6개의 패키지가 모두 생성됐습니다.
- [ ] MariaDB에 `foodnote` DB가 생성됐습니다.
- [ ] `HomeController.java` 파일이 작성됐습니다.
- [ ] `WEB-INF/views/home.jsp` 파일이 작성됐습니다.
- [ ] 서버를 시작했을 때 콘솔에 오류 없이 `Server startup` 메시지가 출력됩니다.
- [ ] 브라우저에서 `http://localhost:8080/food-note/` 접속 시 홈 화면이 표시됩니다.

모든 항목이 체크됐으면 **단계 2. DB 테이블 설계 및 도메인 클래스 작성**으로 이동합니다.
