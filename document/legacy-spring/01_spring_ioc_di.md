# 1일차 — Spring Framework 소개와 IoC/DI

> **환경** : Java 21 + Spring Framework 7.0.x + Maven

---

## 1.1 스프링 프레임워크 개요와 구조

### 1.1.1 스프링이 등장한 배경

서블릿/JSP + Model2 패턴으로 웹 애플리케이션을 개발할 때 반복적으로 겪는 문제가 있다.

```java
// FrontController 또는 각 서블릿에서 직접 객체를 생성
BoardDao     boardDao     = new BoardDaoImpl();
BoardService boardService = new BoardServiceImpl(boardDao);
```

이 방식에는 세 가지 구조적 문제가 있다.

**① 강한 결합(Tight Coupling)**

`BoardServiceImpl`이라는 구체 클래스 이름이 코드 안에 박혀 있다. 예를 들어 `BoardDaoImpl`을 `OracleBoardDaoImpl`로 교체해야 한다면, `new BoardDaoImpl()`이 쓰인 모든 파일을 찾아서 수정해야 한다. 파일이 수십 개라면 수십 군데를 고쳐야 하고, 수정 중 빠뜨리는 곳이 생기면 버그로 이어진다. 이것이 강한 결합의 전형적인 문제다.

**② 싱글톤 관리 부재**

`new`로 생성하면 요청마다 새 객체가 만들어진다. `BoardService`처럼 상태(필드)가 없는 객체를 매 요청마다 생성하는 것은 메모리 낭비다. 싱글톤으로 만들려면 개발자가 직접 `static` 인스턴스와 동기화 코드를 작성해야 하는데, 이는 번거롭고 버그가 생기기 쉽다.

**③ 관심사의 혼재**

서블릿은 HTTP 요청을 받아서 결과를 응답하는 역할에 집중해야 한다. 그런데 `new BoardDaoImpl()`처럼 객체를 직접 만들고 조립하는 코드가 섞이면, 서블릿이 비즈니스 로직 역할과 객체 조립 역할을 동시에 맡게 된다. 단일 책임 원칙(SRP)을 위반하는 것이다.

스프링 프레임워크는 이 세 가지 문제를 **IoC 컨테이너**라는 단일 메커니즘으로 해결한다.

---

### 1.1.2 스프링 프레임워크란

스프링은 **자바 엔터프라이즈 애플리케이션 개발을 위한 오픈소스 경량 프레임워크**다. 2003년 Rod Johnson이 저서 *Expert One-on-One J2EE Design and Development*에서 제시한 아이디어를 토대로 만들어졌으며, 기존 EJB(Enterprise JavaBeans) 기반 개발의 복잡함을 탈피하는 것을 목표로 삼았다.

스프링의 핵심 철학은 두 가지다.

**IoC (Inversion of Control, 제어의 역전)**  
객체의 생성, 의존성 연결, 생명주기 관리의 제어권을 개발자가 아닌 프레임워크가 담당한다.

**AOP (Aspect-Oriented Programming, 관점 지향 프로그래밍)**  
로깅, 트랜잭션, 보안처럼 여러 클래스에 걸쳐 반복되는 공통 관심사를 핵심 비즈니스 코드와 분리한다.

스프링은 POJO(Plain Old Java Object) — 즉 특정 클래스를 상속하거나 인터페이스를 구현할 필요 없는 순수 자바 객체 — 를 기반으로 동작한다는 점에서 EJB와 근본적으로 다르다. 스프링이 관리하는 Bean은 그냥 평범한 자바 클래스다.

---

### 1.1.3 스프링 주요 모듈 구조

스프링은 단일 라이브러리가 아니라 기능별로 나뉜 여러 모듈의 집합이다.

```
┌──────────────────────────────────────────────────────────────┐
│                      Spring Framework                        │
├─────────────────┬────────────────────┬───────────────────────┤
│  Core Container │        Web         │    Data Access        │
│  ─────────────  │  ────────────────  │  ─────────────────    │
│  spring-core    │  spring-webmvc     │  spring-jdbc          │
│  spring-beans   │  spring-webflux    │  spring-orm (JPA)     │
│  spring-context │  spring-websocket  │  spring-tx            │
│  spring-aop     │                    │                       │
├─────────────────┴────────────────────┴───────────────────────┤
│                         Test                                 │
│            spring-test (JUnit, Mockito 통합)                 │
└──────────────────────────────────────────────────────────────┘
```

**Core Container**는 스프링의 심장부다. IoC 컨테이너와 DI 기능을 제공하며, 나머지 모든 모듈이 이 위에서 동작한다. `spring-context` 하나를 의존성에 추가하면 `spring-core`, `spring-beans`, `spring-aop`, `spring-expression`이 함께 포함된다.

이번 교육에서는 Core Container → Spring MVC → Data Access(MyBatis) → AOP/Transactions 순서로 학습한다.

---

### 1.1.4 Spring 7.x 기준 주의사항

이번 교육은 Spring 7.0.x 기준으로 진행한다. 인터넷에 있는 스프링 예제의 대부분은 Spring 5.x 이하를 기준으로 작성되어 있어, 다음 두 가지 차이를 반드시 숙지해야 한다.

| 항목 | Spring 5.x 이하 | Spring 6.x / 7.x |
|---|---|---|
| 서블릿 관련 패키지 | `javax.servlet.*` | **`jakarta.servlet.*`** |
| JPA 관련 패키지 | `javax.persistence.*` | **`jakarta.persistence.*`** |
| 최소 Java 버전 | Java 8 | **Java 17** |

예제를 참고할 때 `javax`가 보이면 `jakarta`로 바꿔야 한다고 기억하면 된다. 패키지 이름만 다를 뿐 클래스 구조와 사용법은 동일하다.

---

## 1.2 IoC (Inversion of Control, 제어의 역전)

### 1.2.1 제어의 역전이란

일반적인 프로그래밍에서 객체는 자신이 필요한 의존 객체를 스스로 생성한다.

```java
// 전통적인 방식: 객체 스스로 의존성을 만든다
public class BoardServiceImpl {
    private BoardDao boardDao = new BoardDaoImpl(); // 직접 생성
}
```

IoC는 이 흐름을 뒤집는다. 객체는 자신이 필요한 것을 요청하지 않는다. 대신 외부(컨테이너)가 필요한 것을 만들어서 밀어 넣어준다.

```
전통적 방식 (개발자가 제어)
  BoardService → new BoardDaoImpl()

IoC 방식 (컨테이너가 제어)
  컨테이너 → BoardDaoImpl 생성
  컨테이너 → BoardServiceImpl 생성
  컨테이너 → BoardServiceImpl 에 BoardDaoImpl 주입
```

"제어의 역전"이라는 이름은 여기서 나온다. 객체를 누가 만드는지(제어권)가 개발자에서 컨테이너로 역전된 것이다.

---

### 1.2.2 스프링 IoC 컨테이너

스프링은 IoC 컨테이너를 두 가지 인터페이스로 제공한다.

**BeanFactory**  
IoC 컨테이너의 최상위 인터페이스로, Bean의 생성과 의존성 주입을 담당하는 가장 기본적인 컨테이너다. **Lazy Loading** 방식으로 동작하여 `getBean()`을 호출하는 시점에 Bean을 생성한다. 기능이 최소화되어 있어 메모리가 극도로 제한된 환경(임베디드 등)에서나 고려할 수 있고, 일반적인 개발에서는 거의 사용하지 않는다.

**ApplicationContext**  
`BeanFactory`를 확장한 인터페이스로, 실무에서 항상 사용하는 컨테이너다. **Eager Loading** 방식으로 동작하여 컨테이너가 시작될 때 모든 싱글톤 Bean을 미리 생성한다. `BeanFactory`의 기본 기능 외에 다음을 추가로 제공한다.

- **국제화(i18n)**: MessageSource를 통한 다국어 메시지 지원
- **이벤트 처리**: ApplicationEvent 발행/구독 메커니즘
- **리소스 추상화**: 파일, 클래스패스, URL 등 다양한 리소스를 통일된 방식으로 접근
- **AOP 자동 적용**: @Transactional 같은 AOP 기반 기능이 자동으로 작동

Eager Loading의 이점은 애플리케이션 시작 시점에 모든 Bean 설정 오류가 드러난다는 것이다. 잘못된 Bean 설정이 있다면 첫 요청 때가 아니라 서버가 뜨는 순간 바로 예외가 발생한다.

`ApplicationContext`의 주요 구현체는 다음과 같다.

| 구현체 | 설명 |
|---|---|
| `ClassPathXmlApplicationContext` | 클래스패스의 XML 파일을 읽어 컨테이너 생성 |
| `FileSystemXmlApplicationContext` | 파일 시스템 경로의 XML 파일을 읽어 컨테이너 생성 |
| `AnnotationConfigApplicationContext` | Java Config(`@Configuration` 클래스)로 컨테이너 생성 |
| `XmlWebApplicationContext` | 웹 환경에서 XML 설정으로 컨테이너 생성 (DispatcherServlet과 함께 사용) |

이번 교육에서는 XML 설정을 먼저 학습하므로 `ClassPathXmlApplicationContext`를 주로 사용한다.

---

### 1.2.3 Bean이란

스프링 IoC 컨테이너가 **생성하고 생명주기를 관리하는 자바 객체**를 **Bean**이라고 한다. 개발자가 `new`로 직접 만드는 일반 객체와 구분하기 위한 용어다.

**Bean의 생명주기**

Bean은 컨테이너 안에서 다음 단계를 거친다.

```
컨테이너 시작
    → Bean 인스턴스 생성 (new)
    → 의존성 주입 (DI)
    → 초기화 콜백 (afterPropertiesSet / init-method)
    → [사용]
    → 소멸 전 콜백 (destroy / destroy-method)
컨테이너 종료
```

초기화 콜백과 소멸 콜백은 DB 연결 초기화나 자원 해제처럼 Bean 생성/소멸 시점에 특정 작업이 필요할 때 사용한다. XML에서는 `<bean>` 태그의 `init-method`, `destroy-method` 속성으로 지정한다.

---

### 1.2.4 Bean 스코프

스코프(Scope)는 **Bean 인스턴스가 언제 생성되고, 얼마나 공유되는지**를 결정하는 설정이다. 스프링은 기본적으로 다섯 가지 스코프를 제공한다.

#### Singleton (기본값)

컨테이너당 Bean 인스턴스가 단 하나만 생성된다. `getBean()`을 몇 번 호출해도 항상 같은 인스턴스가 반환된다.

```xml
<!-- scope 생략 시 자동으로 singleton -->
<bean id="boardService" class="com.spring.BoardServiceImpl"/>

<!-- 명시적 선언 -->
<bean id="boardService" class="com.spring.BoardServiceImpl" scope="singleton"/>
```

```
컨테이너 시작 → BoardServiceImpl 인스턴스 1개 생성
getBean("boardService") → 동일 인스턴스 반환
getBean("boardService") → 동일 인스턴스 반환  ← 항상 같은 객체
```

싱글톤 Bean은 여러 스레드가 동시에 접근할 수 있으므로, **인스턴스 변수(필드)에 상태를 저장해서는 안 된다**. `BoardService`처럼 메서드 안에서만 데이터를 처리하고 필드에 상태를 두지 않는 stateless 설계가 싱글톤과 궁합이 맞다.

#### Prototype

`getBean()`을 호출할 때마다 새 인스턴스가 생성된다.

```xml
<bean id="boardSearch" class="com.spring.BoardSearch" scope="prototype"/>
```

```
getBean("boardSearch") → 인스턴스 A 생성 후 반환
getBean("boardSearch") → 인스턴스 B 생성 후 반환  ← 매번 새 객체
```

사용자별로 다른 상태를 유지해야 하는 객체(검색 조건 객체, 커맨드 객체 등)에 적합하다. 단, 컨테이너는 prototype Bean을 생성하고 주입한 뒤에는 관리하지 않는다. 따라서 소멸 콜백(`destroy-method`)이 자동으로 호출되지 않으며, 메모리 해제는 GC에 맡긴다.

#### Request (웹 환경 전용)

HTTP 요청 하나당 Bean 인스턴스가 하나 생성된다. 동일한 요청 안에서는 같은 인스턴스를 공유하고, 요청이 끝나면 Bean이 소멸된다.

```xml
<bean id="loginForm" class="com.spring.LoginForm" scope="request"/>
```

폼 데이터처럼 하나의 요청 처리 흐름 안에서만 공유하면 되는 데이터에 사용한다. Spring MVC 환경에서만 동작한다.

#### Session (웹 환경 전용)

HTTP 세션 하나당 Bean 인스턴스가 하나 생성된다. 같은 세션을 가진 요청들은 동일한 Bean 인스턴스를 공유하고, 세션이 만료되면 Bean이 소멸된다.

```xml
<bean id="loginUser" class="com.spring.LoginUser" scope="session"/>
```

로그인한 사용자 정보처럼 세션 동안 유지되어야 하는 데이터에 사용한다. Spring MVC 환경에서만 동작한다.

#### Application (웹 환경 전용)

서블릿 컨텍스트(웹 애플리케이션) 하나당 Bean 인스턴스가 하나 생성된다. 싱글톤과 유사하지만, 스프링 컨테이너가 아닌 서블릿 컨텍스트를 기준으로 범위가 결정된다는 점이 다르다. 하나의 웹 애플리케이션 안에 여러 스프링 컨텍스트가 있을 때 의미를 갖는다.

```xml
<bean id="appConfig" class="com.spring.AppConfig" scope="application"/>
```

**스코프 선택 기준 요약**

| 스코프 | 인스턴스 수 | 주요 사용처 |
|---|---|---|
| `singleton` | 컨테이너당 1개 | Service, DAO, Repository (stateless) |
| `prototype` | 호출마다 새 인스턴스 | 상태를 갖는 커맨드/폼 객체 |
| `request` | HTTP 요청당 1개 | 단일 요청 처리 중 공유 데이터 |
| `session` | HTTP 세션당 1개 | 로그인 사용자 정보 |
| `application` | 서블릿 컨텍스트당 1개 | 애플리케이션 전역 공유 설정 |

---

## 1.3 DI (Dependency Injection, 의존성 주입)

### 1.3.1 의존성이란

클래스 A가 동작하기 위해 클래스 B의 기능이 필요할 때, "A는 B에 의존한다"고 표현한다.

```java
public class BoardServiceImpl {
    // BoardServiceImpl은 BoardDao에 의존한다
    private BoardDao boardDao;
}
```

의존 관계 자체는 자연스럽다. 문제는 **의존 객체를 어떻게 얻는가**다. 스스로 `new`로 만들면 강한 결합이 생기고, 외부에서 주입받으면 느슨한 결합(Loose Coupling)이 된다.

---

### 1.3.2 생성자 주입 (Constructor Injection)

생성자의 매개변수를 통해 의존 객체를 전달받는 방식이다.

```java
public class BoardServiceImpl implements BoardService {

    private final BoardDao boardDao; // final → 한 번 주입되면 변경 불가

    public BoardServiceImpl(BoardDao boardDao) {
        this.boardDao = boardDao;
    }
}
```

```xml
<bean id="boardService" class="com.spring.BoardServiceImpl">
    <constructor-arg ref="boardDao"/>  <!-- boardDao Bean을 생성자 인자로 주입 -->
</bean>
```

생성자 주입은 다음 이유로 **스프링 공식 문서도 권장하는 기본 방식**이다.

- `final` 필드를 사용할 수 있어 주입된 의존성이 변경되지 않음을 컴파일 타임에 보장한다.
- 필수 의존성이 누락되면 객체 생성 자체가 실패하므로, 불완전한 상태의 객체가 생성될 수 없다.
- 생성자 인자만 보면 이 클래스가 어떤 의존성을 필요로 하는지 즉시 파악할 수 있다.
- 순환 의존성(A → B → A)이 있으면 컨테이너 시작 시점에 바로 예외가 발생하여 조기에 문제를 발견할 수 있다.

`<constructor-arg>` 태그는 여러 개를 쓸 수 있으며, `index` 속성으로 생성자 인자의 순서를 명시할 수 있다.

```xml
<bean id="dataSource" class="com.spring.SimpleDataSource">
    <constructor-arg index="0" value="jdbc:mysql://localhost/mydb"/>
    <constructor-arg index="1" value="root"/>
    <constructor-arg index="2" value="password"/>
</bean>
```

`ref`는 다른 Bean을 참조할 때, `value`는 문자열이나 숫자 같은 리터럴 값을 주입할 때 사용한다.

---

### 1.3.3 Setter 주입 (Setter Injection)

Setter 메서드를 통해 의존 객체를 전달받는 방식이다.

```java
public class BoardServiceImpl implements BoardService {

    private BoardDao boardDao;

    // 스프링은 setBoardDao() 메서드를 호출하여 boardDao를 주입한다
    public void setBoardDao(BoardDao boardDao) {
        this.boardDao = boardDao;
    }
}
```

```xml
<bean id="boardService" class="com.spring.BoardServiceImpl">
    <!-- name 속성: setter 메서드에서 'set'을 제거하고 첫 글자를 소문자로 -->
    <property name="boardDao" ref="boardDao"/>
</bean>
```

Setter 주입에는 기본 생성자(no-arg constructor)가 반드시 필요하다. 스프링이 먼저 기본 생성자로 객체를 만든 뒤, setter를 호출하여 의존성을 주입하는 순서로 동작하기 때문이다.

Setter 주입은 **선택적 의존성** — 없어도 기본 동작이 가능하지만 있으면 기능이 추가되는 의존성 — 에 적합하다. 예를 들어 캐시 기능이 없어도 조회는 동작하지만, 캐시 Bean이 주입되면 성능이 향상되는 경우가 이에 해당한다.

`<property>` 태그로 기본값(문자열, 숫자, boolean)도 주입할 수 있다.

```xml
<bean id="mailSender" class="com.spring.MailSender">
    <property name="host"  value="smtp.example.com"/>
    <property name="port"  value="587"/>
    <property name="auth"  value="true"/>
</bean>
```

---

### 1.3.4 필드 주입 (Field Injection)

어노테이션 기반 설정에서 `@Autowired`를 필드에 직접 붙이는 방식이다. XML 설정에서는 사용할 수 없고, 어노테이션 기반 설정에서 쓰인다.

```java
public class BoardServiceImpl implements BoardService {

    @Autowired // 필드에 직접 주입 — 권장하지 않음
    private BoardDao boardDao;
}
```

코드가 간결해 보이지만 다음 이유로 사용을 피해야 한다.

- `final` 필드로 선언할 수 없어 불변성 보장이 안 된다.
- 스프링 컨테이너 없이는 의존성 주입이 불가능하여 순수 단위 테스트 작성이 어렵다.
- 어떤 의존성이 필요한지 클래스 외부에서 파악하기 어렵다.

스프링 공식 문서에서도 필드 주입은 권장하지 않는다고 명시하고 있다.

---

### 1.3.5 세 가지 주입 방식 비교

| 항목 | 생성자 주입 | Setter 주입 | 필드 주입 |
|---|---|---|---|
| 필수 의존성 강제 | ✅ 가능 | ❌ 불가 | ❌ 불가 |
| 불변성(`final`) | ✅ 가능 | ❌ 불가 | ❌ 불가 |
| 순환 의존 감지 | 컨테이너 시작 시 | 런타임 | 런타임 |
| 선택적 의존성 표현 | 불편 | ✅ 적합 | ✅ 간단 |
| 단위 테스트 용이성 | ✅ 높음 | 보통 | ❌ 낮음 |
| **권장 여부** | ✅ **기본 권장** | 선택적 의존성에만 | ❌ 지양 |

---

## 1.4 XML 기반 Bean 설정과 ApplicationContext

### 1.4.1 설정 파일 기본 구조

스프링 XML 설정 파일은 `<beans>` 루트 태그 안에 Bean 정의를 작성한다. 파일 이름은 관례적으로 `applicationContext.xml`을 사용하고, `src/main/resources/`에 위치시킨다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="
           http://www.springframework.org/schema/beans
           https://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- Bean 정의 -->
    <bean id="boardDao"     class="com.spring.BoardDaoImpl"/>
    <bean id="boardService" class="com.spring.BoardServiceImpl">
        <constructor-arg ref="boardDao"/>
    </bean>

</beans>
```

`xsi:schemaLocation`은 XML 스키마 위치를 IDE에 알려주기 위한 선언이다. 이것이 있어야 IDE에서 자동완성이 동작하고, 오타가 있을 때 빨간 밑줄로 표시해준다.

---

### 1.4.2 `<bean>` 태그 주요 속성

| 속성 | 필수 여부 | 설명 |
|---|---|---|
| `id` | 권장 | 컨테이너 내 Bean의 고유 식별자. `getBean("id")`로 조회한다 |
| `class` | 필수 | 생성할 클래스의 완전한 경로(패키지명 포함) |
| `scope` | 선택 | Bean 스코프. 기본값은 `singleton` |
| `init-method` | 선택 | 의존성 주입 완료 후 호출할 초기화 메서드명 |
| `destroy-method` | 선택 | 컨테이너 종료 시 호출할 소멸 메서드명 |
| `lazy-init` | 선택 | `true`로 설정하면 해당 Bean만 Lazy Loading으로 전환 |

---

### 1.4.3 여러 값 유형 주입

XML 설정에서 주입할 수 있는 값의 유형은 다음과 같다.

**다른 Bean 참조**
```xml
<property name="boardDao" ref="boardDao"/>
```

**기본 타입 · 문자열**
```xml
<property name="maxPageSize" value="10"/>
<property name="encoding"    value="UTF-8"/>
```

**List**
```xml
<property name="allowedRoles">
    <list>
        <value>ADMIN</value>
        <value>MANAGER</value>
    </list>
</property>
```

**Map**
```xml
<property name="dbProperties">
    <map>
        <entry key="url"      value="jdbc:mysql://localhost/mydb"/>
        <entry key="username" value="root"/>
    </map>
</property>
```

**null 명시적 주입**
```xml
<property name="optionalService"><null/></property>
```

---

### 1.4.4 ApplicationContext 로드와 Bean 조회

```java
// 컨테이너 생성 — XML 파일을 읽어 Bean을 모두 초기화한다
ApplicationContext ctx =
    new ClassPathXmlApplicationContext("applicationContext.xml");

// Bean 조회 방법 ①: id + 타입 지정 (권장 — 캐스팅 불필요)
BoardService boardService = ctx.getBean("boardService", BoardService.class);

// Bean 조회 방법 ②: 타입만 지정 (해당 타입의 Bean이 하나일 때만 사용 가능)
BoardService boardService = ctx.getBean(BoardService.class);

// Bean 조회 방법 ③: id만 지정 (Object 반환 — 명시적 캐스팅 필요, 비권장)
BoardService boardService = (BoardService) ctx.getBean("boardService");

// 컨테이너 종료 — destroy-method 콜백을 실행하고 자원을 해제한다
((ClassPathXmlApplicationContext) ctx).close();
```

`getBean(id, Class)`의 두 번째 인자로 타입을 지정하면 ClassCastException 없이 안전하게 Bean을 가져올 수 있어 가장 권장하는 방식이다.

---

### 1.4.5 설정 파일 분리

프로젝트가 커지면 Bean 설정을 여러 파일로 나누어 관리한다. 웹 애플리케이션에서는 보통 용도별로 분리한다.

```
resources/
├── applicationContext.xml        ← 전체 통합 또는 Service·DAO
├── applicationContext-mvc.xml    ← Spring MVC 관련 Bean
└── applicationContext-db.xml     ← DataSource, MyBatis 관련 Bean
```

여러 파일을 하나의 컨테이너로 로드하려면 배열로 전달하거나, `<import>` 태그를 사용한다.

```java
// 방법 ①: 생성자에 파일 목록 배열로 전달
ApplicationContext ctx = new ClassPathXmlApplicationContext(
    "applicationContext.xml",
    "applicationContext-db.xml"
);
```

```xml
<!-- 방법 ②: applicationContext.xml 안에서 import -->
<import resource="applicationContext-db.xml"/>
<import resource="applicationContext-mvc.xml"/>
```

---

## 1.5 실습 — Model2 코드를 Spring Bean으로 전환하기

### 실습 목표

게시판 서비스를 Model2 방식(직접 `new`)으로 구현한 코드를 스프링 IoC/DI 방식으로 전환한다. 두 방식의 코드를 나란히 실행하고 출력 결과와 구조 차이를 직접 확인한다.

### 실습 환경

```
spring-day1/
├── pom.xml
└── src/main/
    ├── java/com/spring/day1/
    │   ├── Board.java                 ← 게시글 도메인 클래스
    │   ├── BoardDao.java              ← DAO 인터페이스
    │   ├── BoardDaoImpl.java          ← DAO 구현체 (메모리 데이터)
    │   ├── BoardService.java          ← 서비스 인터페이스
    │   ├── BoardServiceImpl.java      ← 서비스 구현체 (생성자·setter 주입 모두 지원)
    │   ├── BeforeMain.java            ← Model2 방식 (직접 new)
    │   └── AfterMain.java             ← Spring IoC/DI 방식
    └── resources/
        └── applicationContext.xml
```

### pom.xml 핵심 의존성

```xml
<properties>
    <java.version>21</java.version>
    <spring.version>7.0.7</spring.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <version>${spring.version}</version>
    </dependency>
</dependencies>
```

`spring-context` 하나만 추가하면 `spring-core`, `spring-beans`, `spring-aop`, `spring-expression`이 자동으로 포함된다. 오늘 실습은 이것만으로 충분하다.

---

### 실습 순서

**Step 1 — BeforeMain 실행**

`BoardDao`와 `BoardService`를 직접 `new`로 생성하고 조립한 코드를 실행한다. 동작은 정상이지만 코드 안에 `BoardDaoImpl`이라는 구체 클래스 이름이 노출되어 있고, 구현체가 바뀌면 이 파일을 수정해야 한다는 점을 확인한다.

**Step 2 — applicationContext.xml 작성**

Bean 두 개(boardDao, boardService)를 XML에 등록하고, 생성자 주입으로 연결한다.

**Step 3 — AfterMain 실행**

`AfterMain`은 `new`를 한 줄도 쓰지 않는다. `getBean()`으로 컨테이너에서 `BoardService`를 꺼내 쓰기만 한다. `AfterMain`은 `BoardDaoImpl`의 존재를 전혀 모른다.

**Step 4 — 구현체 교체 실험**

`BoardDaoImpl`을 `MemoryBoardDaoImpl`로 이름을 바꾸거나, 다른 구현체를 새로 만든다. 그런 다음 `applicationContext.xml`의 `class` 속성 한 줄만 수정하면 `AfterMain` 코드를 전혀 건드리지 않고 동작이 바뀌는 것을 확인한다.

**Step 5 — Setter 주입으로 전환**

`applicationContext.xml`에서 `<constructor-arg>`를 `<property>`로 바꾸고, `BoardServiceImpl`에 기본 생성자를 추가한다. `AfterMain`은 수정 없이 그대로 실행된다.

---

### 실습 결과 정리

| 항목 | Before (Model2) | After (Spring) |
|---|---|---|
| 객체 생성 주체 | 개발자 (`new`) | 스프링 컨테이너 |
| 구체 클래스 의존 | 있음 (직접 `new BoardDaoImpl()`) | 없음 (인터페이스만 사용) |
| 구현체 변경 시 수정 대상 | 자바 소스 코드 | XML 설정 파일 |
| 싱글톤 관리 | 직접 구현 필요 | 컨테이너가 자동 관리 |

---

## 정리

**IoC**는 객체 생성과 의존성 조립의 제어권을 개발자에서 스프링 컨테이너로 넘기는 원칙이고, **DI**는 그 원칙을 실현하는 구체적인 기법이다. 이 둘은 동전의 양면이다.

**Bean**은 스프링 컨테이너가 생성·관리하는 객체이며, 기본 스코프는 싱글톤이다. 스코프는 Bean이 언제, 얼마나 공유되는지를 결정하며 `singleton`, `prototype`, `request`, `session`, `application` 중에서 선택한다.

DI 방식은 **생성자 주입을 기본으로** 사용하고, 선택적 의존성에만 setter 주입을 병행한다. 필드 주입은 테스트 어려움과 불변성 미보장 문제로 사용하지 않는다.

Spring 7.x에서는 모든 `javax.*` 패키지가 `jakarta.*`로 변경되었다. 외부 예제 참고 시 반드시 확인해야 한다.

---

## 다음 시간 예고

2일차에서는 스프링 MVC의 핵심인 **DispatcherServlet** 구조를 학습한다. Model2의 FrontController 서블릿이 어떻게 DispatcherServlet으로 대체되는지, 그리고 HandlerMapping · Controller · ViewResolver가 어떤 역할을 나누어 맡는지 살펴본다.
