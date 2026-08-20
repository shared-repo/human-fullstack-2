# 4일차 — AOP, 트랜잭션, Interceptor, 로그인, 파일 업로드

> **환경** : Java 21 + Spring Framework 7.0.x + AspectJ

---

## 4.1 AOP 개념 — 횡단 관심사 분리

### 4.1.1 횡단 관심사란

애플리케이션의 기능은 크게 두 종류로 나뉜다.

**핵심 관심사(Core Concern)**  
비즈니스 목적을 직접 수행하는 기능이다. 게시글 등록, 회원 조회, 주문 처리처럼 각 서비스 클래스가 고유하게 담당하는 로직이다.

**횡단 관심사(Cross-Cutting Concern)**  
핵심 관심사와 무관하게 여러 클래스에 걸쳐 반복적으로 등장하는 기능이다. 로깅, 트랜잭션 관리, 보안 체크, 성능 측정, 예외 처리가 대표적인 예다.

```
횡단 관심사가 코드에 섞이면 어떻게 되는가

BoardService.register()  {
    log.info("register 시작");          // ← 로깅 (횡단)
    txManager.begin();                  // ← 트랜잭션 (횡단)
    // 게시글 등록 (핵심)
    boardDao.insert(board);
    txManager.commit();                 // ← 트랜잭션 (횡단)
    log.info("register 완료");          // ← 로깅 (횡단)
}

MemberService.join()  {
    log.info("join 시작");              // ← 같은 로깅 반복
    txManager.begin();                  // ← 같은 트랜잭션 반복
    // 회원 가입 (핵심)
    memberDao.insert(member);
    txManager.commit();
    log.info("join 완료");
}
```

핵심 로직보다 횡단 관심사 코드가 더 많아지고, 수십 개의 서비스 클래스에 동일한 패턴이 반복된다. 로깅 형식을 바꾸려면 모든 파일을 수정해야 한다.

---

### 4.1.2 AOP의 해결 방식

**AOP(Aspect-Oriented Programming, 관점 지향 프로그래밍)** 는 횡단 관심사를 별도의 모듈(**Aspect**)로 분리하고, 핵심 코드를 수정하지 않고 원하는 시점에 자동으로 끼워 넣는 기법이다.

```
AOP 적용 후

BoardService.register()  {
    boardDao.insert(board);   // 핵심 로직만 남는다
}

MemberService.join()  {
    memberDao.insert(member); // 핵심 로직만 남는다
}

LoggingAspect  {
    // "모든 Service 메서드 실행 전후에 로그를 남긴다"
    // → 스프링이 런타임에 자동으로 끼워 넣는다
}
```

개발자는 비즈니스 코드에 집중하고, 횡단 관심사는 Aspect에 한 번만 작성한다.

---

### 4.1.3 AOP 핵심 용어

| 용어 | 의미 | 예시 |
|---|---|---|
| **Aspect** | 횡단 관심사를 담은 모듈 | `LoggingAspect`, `TransactionAspect` |
| **Advice** | Aspect에서 실행할 실제 동작 코드 | "메서드 실행 전에 로그를 남긴다" |
| **JoinPoint** | Advice가 끼어들 수 있는 지점 | 메서드 실행 시점, 예외 발생 시점 |
| **Pointcut** | Advice를 적용할 JoinPoint의 조건식 | "com.spring 패키지의 모든 Service 메서드" |
| **Weaving** | Advice를 대상 코드에 적용하는 과정 | 스프링은 런타임에 프록시 방식으로 Weaving |
| **Target** | Advice가 적용될 실제 객체 | `BoardServiceImpl` |
| **Proxy** | Target을 감싼 대리 객체 | 스프링이 자동 생성, Target 대신 컨테이너에 등록 |

---

### 4.1.4 스프링 AOP의 동작 방식 — 프록시 패턴

스프링 AOP는 **런타임 프록시** 방식으로 동작한다. 컨테이너가 Bean을 생성할 때, AOP 대상 Bean을 감싸는 프록시 객체를 만들어 원본 Bean 대신 컨테이너에 등록한다.

```
컨테이너에서 boardService를 꺼낼 때

개발자가 기대하는 것: BoardServiceImpl 인스턴스
실제로 받는 것:       BoardServiceImpl을 감싼 Proxy 객체

Proxy 객체의 동작:
  메서드 호출 → Advice 실행(Before) → 원본 메서드 실행 → Advice 실행(After)
```

이 때문에 스프링 AOP에는 두 가지 제약이 있다.

- **인터페이스 기반 프록시(JDK Dynamic Proxy)**: 대상 Bean이 인터페이스를 구현하면 인터페이스 기반 프록시를 생성한다.
- **클래스 기반 프록시(CGLIB)**: 인터페이스가 없으면 CGLIB 라이브러리로 클래스를 상속한 프록시를 생성한다. `final` 클래스나 `final` 메서드에는 CGLIB 프록시를 생성할 수 없다.

또한 스프링 AOP는 **메서드 실행(Method Execution)** JoinPoint만 지원한다. 필드 접근, 생성자 호출 등에는 적용할 수 없다. 이것은 AspectJ 전체 스펙을 사용하는 것과의 차이점이다.

---

## 4.2 어드바이스 유형과 @Aspect 설정

### 4.2.1 @Aspect 선언

`@Aspect`는 해당 클래스가 AOP 모듈임을 선언하는 어노테이션이다. `@Component`와 함께 사용하여 스프링 Bean으로 등록해야 Aspect가 활성화된다.

```java
@Aspect
@Component
public class LoggingAspect {
    // Pointcut + Advice 정의
}
```

`spring-mvc.xml` 또는 `applicationContext.xml`에서 `@Aspect`를 활성화해야 한다.

```xml
<!-- @Aspect 어노테이션 기반 AOP 활성화 -->
<aop:aspectj-autoproxy/>
```

XML 네임스페이스에 `aop` 스키마를 추가해야 한다.

```xml
xmlns:aop="http://www.springframework.org/schema/aop"
xsi:schemaLocation="...
    http://www.springframework.org/schema/aop
    https://www.springframework.org/schema/aop/spring-aop.xsd"
```

---

### 4.2.2 Pointcut 표현식

Pointcut은 Advice를 적용할 메서드의 범위를 지정하는 조건식이다. 스프링 AOP에서 가장 많이 쓰는 표현식은 `execution`이다.

```
execution( [접근제어자] 반환타입 [패키지.클래스.]메서드명(파라미터타입) )
```

| 표현식 | 의미 |
|---|---|
| `execution(* com.spring..*Service.*(..))` | `com.spring` 하위 패키지의 이름이 `Service`로 끝나는 클래스의 모든 메서드 |
| `execution(* com.spring.board.BoardService.*(..))` | `BoardService`의 모든 메서드 |
| `execution(* com.spring..*.get*(..))` | `com.spring` 하위의 모든 `get`으로 시작하는 메서드 |
| `execution(* *(..))` | 모든 메서드 (범위가 너무 넓어 실무에서는 사용하지 않음) |

`*`는 하나의 요소 와일드카드, `..`은 임의 패키지 또는 임의 개수 파라미터를 의미한다.

Pointcut은 `@Pointcut` 어노테이션으로 별도 메서드에 정의한 뒤 재사용할 수 있다.

```java
@Aspect
@Component
public class LoggingAspect {

    // Pointcut 정의 — 메서드 이름이 Pointcut 식별자가 된다
    @Pointcut("execution(* com.spring..*Service.*(..))")
    public void serviceLayer() {}  // 메서드 본문은 비워둔다

    // Pointcut 재사용
    @Before("serviceLayer()")
    public void logBefore(JoinPoint jp) { ... }

    @AfterReturning("serviceLayer()")
    public void logAfter(JoinPoint jp) { ... }
}
```

---

### 4.2.3 어드바이스 유형

**@Before — 메서드 실행 전**

대상 메서드가 실행되기 전에 Advice가 실행된다. 대상 메서드의 실행을 막을 수는 없다.

```java
@Before("serviceLayer()")
public void logBefore(JoinPoint jp) {
    String methodName = jp.getSignature().getName();
    Object[] args     = jp.getArgs();
    log.info("[Before] {} 호출 - 파라미터: {}", methodName, Arrays.toString(args));
}
```

`JoinPoint` 객체에서 실행 중인 메서드 이름(`getSignature().getName()`), 파라미터 값(`getArgs()`), 대상 객체(`getTarget()`) 등의 정보를 꺼낼 수 있다.

**@AfterReturning — 메서드 정상 반환 후**

대상 메서드가 예외 없이 정상적으로 반환된 후에 실행된다. `returning` 속성으로 반환값을 받을 수 있다.

```java
@AfterReturning(pointcut = "serviceLayer()", returning = "result")
public void logAfterReturning(JoinPoint jp, Object result) {
    log.info("[AfterReturning] {} 반환값: {}", jp.getSignature().getName(), result);
}
```

**@AfterThrowing — 예외 발생 후**

대상 메서드에서 예외가 발생했을 때 실행된다. `throwing` 속성으로 발생한 예외 객체를 받을 수 있다.

```java
@AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
public void logAfterThrowing(JoinPoint jp, Exception ex) {
    log.error("[AfterThrowing] {} 예외 발생: {}", jp.getSignature().getName(), ex.getMessage());
}
```

**@After — 메서드 종료 후 (정상/예외 무관)**

정상 반환이든 예외 발생이든 메서드가 종료되면 반드시 실행된다. Java의 `finally`에 해당한다. 자원 해제처럼 결과에 무관하게 실행되어야 하는 작업에 사용한다.

```java
@After("serviceLayer()")
public void logAfter(JoinPoint jp) {
    log.info("[After] {} 종료", jp.getSignature().getName());
}
```

**@Around — 메서드 실행 전체 제어 (가장 강력)**

대상 메서드의 실행 전후를 모두 제어할 수 있는 가장 강력한 어드바이스다. `ProceedingJoinPoint.proceed()`를 직접 호출해야 대상 메서드가 실행된다. 호출하지 않으면 대상 메서드를 실행하지 않고 Advice만 실행된다.

```java
@Around("serviceLayer()")
public Object measureTime(ProceedingJoinPoint pjp) throws Throwable {
    long start = System.currentTimeMillis();

    try {
        Object result = pjp.proceed(); // 실제 메서드 실행
        return result;
    } finally {
        long elapsed = System.currentTimeMillis() - start;
        log.info("[Around] {} 실행시간: {}ms", pjp.getSignature().getName(), elapsed);
    }
}
```

`@Around`는 반환값을 직접 조작하거나, 메서드 실행 여부를 조건에 따라 결정하거나, 실행 시간을 측정하는 데 사용한다.

---

### 4.2.4 어드바이스 실행 순서

하나의 메서드에 여러 어드바이스가 적용될 때 실행 순서는 다음과 같다.

```
메서드 호출
    → @Around (proceed 이전)
        → @Before
            → [대상 메서드 실행]
        → @AfterReturning 또는 @AfterThrowing
        → @After
    → @Around (proceed 이후)
메서드 반환
```

---

### 4.2.5 어드바이스 유형 선택 기준

| 상황 | 권장 어드바이스 |
|---|---|
| 메서드 호출 로깅 | `@Before` + `@AfterReturning` |
| 예외 발생 알림·로깅 | `@AfterThrowing` |
| 자원 해제 (결과 무관) | `@After` |
| 실행 시간 측정 | `@Around` |
| 반환값 가공 | `@Around` |
| 메서드 실행 조건 제어 | `@Around` |
| 트랜잭션 관리 | `@Around` (스프링 내부에서 이 방식으로 구현됨) |

필요 이상으로 강력한 어드바이스를 선택하는 것은 피한다. 실행 전 로깅이 목적이라면 `@Before`로 충분하며, `@Around`를 사용하면 `proceed()` 호출 누락으로 대상 메서드가 실행되지 않는 실수가 생길 수 있다.

---

## 4.3 Spring 트랜잭션 관리

### 4.3.1 트랜잭션이란

**트랜잭션(Transaction)** 은 하나의 논리적 작업 단위를 구성하는 DB 연산의 집합이다. 트랜잭션 안의 모든 작업은 **전부 성공**하거나 **전부 실패(롤백)** 해야 한다.

트랜잭션의 네 가지 특성 ACID는 다음과 같다.

| 특성 | 의미 |
|---|---|
| **Atomicity (원자성)** | 트랜잭션 안의 작업은 모두 성공하거나 모두 실패한다 |
| **Consistency (일관성)** | 트랜잭션 전후로 DB의 제약 조건이 유지된다 |
| **Isolation (격리성)** | 동시에 실행되는 트랜잭션이 서로 간섭하지 않는다 |
| **Durability (지속성)** | 커밋된 데이터는 장애가 발생해도 유지된다 |

조회수 증가 후 게시글을 SELECT하는 작업을 생각해보자. UPDATE가 성공했는데 SELECT 중에 예외가 발생한다면 조회수만 올라가고 화면은 오류가 난다. 이 두 연산을 트랜잭션으로 묶으면, SELECT 실패 시 UPDATE도 롤백되어 일관성이 유지된다.

---

### 4.3.2 스프링 트랜잭션 추상화

스프링은 `PlatformTransactionManager` 인터페이스로 트랜잭션 관리를 추상화한다. 개발자는 이 인터페이스만 사용하면 되고, 실제 구현체는 사용하는 기술에 따라 달라진다.

| 사용 기술 | PlatformTransactionManager 구현체 |
|---|---|
| JDBC / MyBatis | `DataSourceTransactionManager` |
| JPA / Hibernate | `JpaTransactionManager` |
| JTA (분산 트랜잭션) | `JtaTransactionManager` |

MyBatis를 사용하는 이번 교육에서는 `DataSourceTransactionManager`를 사용한다.

```xml
<!-- applicationContext.xml -->
<bean id="transactionManager"
      class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
    <property name="dataSource" ref="dataSource"/>
</bean>

<!-- @Transactional 어노테이션 활성화 -->
<tx:annotation-driven transaction-manager="transactionManager"/>
```

`<tx:annotation-driven/>`이 없으면 `@Transactional`이 동작하지 않는다. 이 설정 역시 AOP 프록시 방식으로 동작한다.

---

### 4.3.3 @Transactional

`@Transactional`은 메서드(또는 클래스) 단위로 트랜잭션 경계를 선언한다. 스프링이 프록시를 통해 메서드 실행 전에 트랜잭션을 시작하고, 정상 반환 시 커밋, 예외 발생 시 롤백한다.

```java
@Service
public class BoardServiceImpl implements BoardService {

    // 클래스 레벨에 선언하면 모든 메서드에 적용된다
    // 메서드 레벨 선언이 클래스 레벨 선언을 덮어쓴다
    @Transactional
    @Override
    public Board getDetail(int no) {
        boardMapper.increaseHit(no);      // UPDATE — 실패 시 롤백 대상
        return boardMapper.selectOne(no); // SELECT
    }

    @Transactional
    @Override
    public void register(Board board) {
        boardMapper.insert(board);         // INSERT — 실패 시 롤백
    }
}
```

---

### 4.3.4 @Transactional 주요 속성

**propagation (전파 속성)**

트랜잭션이 이미 진행 중일 때 새로운 트랜잭션을 어떻게 처리할지 결정한다.

| 전파 속성 | 동작 | 주요 사용처 |
|---|---|---|
| `REQUIRED` (기본값) | 진행 중인 트랜잭션에 합류. 없으면 새로 시작 | 일반적인 서비스 메서드 |
| `REQUIRES_NEW` | 진행 중인 트랜잭션을 일시 정지하고 새 트랜잭션 시작 | 실패해도 독립적으로 커밋되어야 하는 작업 (이력 저장 등) |
| `NESTED` | 중첩 트랜잭션 시작. 내부 트랜잭션 실패 시 세이브포인트로 롤백 | 일부만 롤백할 수 있어야 하는 복잡한 로직 |
| `SUPPORTS` | 트랜잭션 있으면 합류, 없으면 트랜잭션 없이 실행 | 읽기 작업 |
| `NOT_SUPPORTED` | 트랜잭션 없이 실행. 진행 중인 트랜잭션은 일시 정지 | 트랜잭션 오버헤드를 피해야 하는 배치 처리 |
| `NEVER` | 트랜잭션이 있으면 예외 발생 | 트랜잭션 없이 실행해야 하는 작업 |
| `MANDATORY` | 진행 중인 트랜잭션이 없으면 예외 발생 | 반드시 상위 트랜잭션 안에서 호출되어야 하는 메서드 |

```java
// 주문 처리: 결제 실패해도 주문 이력은 남긴다
@Transactional
public void processOrder(Order order) {
    orderMapper.insert(order);   // 주문 저장
    saveOrderHistory(order);     // 이력 저장 — 주문 롤백되어도 이력은 유지
    paymentService.pay(order);   // 결제 — 실패 시 주문과 함께 롤백
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void saveOrderHistory(Order order) {
    // 독립 트랜잭션으로 실행 → 외부 롤백에 영향받지 않음
    historyMapper.insert(order);
}
```

**isolation (격리 수준)**

여러 트랜잭션이 동시에 실행될 때 서로의 작업이 어느 정도 보이는지를 결정한다.

| 격리 수준 | 설명 | 허용하는 문제 |
|---|---|---|
| `DEFAULT` | DB 기본값 사용 (대부분 READ_COMMITTED) | DB 설정에 따름 |
| `READ_UNCOMMITTED` | 커밋되지 않은 데이터도 읽음 | Dirty Read 허용 |
| `READ_COMMITTED` | 커밋된 데이터만 읽음 | Non-Repeatable Read 허용 |
| `REPEATABLE_READ` | 같은 쿼리는 같은 결과 보장 | Phantom Read 허용 |
| `SERIALIZABLE` | 완전한 격리. 모든 트랜잭션 순차 실행 | 없음 (성능 저하 큼) |

격리 수준이 높을수록 데이터 정합성은 높아지지만 동시성 성능은 낮아진다. 대부분의 애플리케이션은 DB 기본값(`READ_COMMITTED`)을 그대로 사용한다.

**readOnly**

읽기 전용 트랜잭션을 선언한다. DB 드라이버와 ORM 레이어에서 최적화가 적용되고, 실수로 수정 쿼리가 실행되는 것을 방지한다.

```java
@Transactional(readOnly = true)
public List<Board> getList(Map<String, Object> params) {
    return boardMapper.selectAll(params);
}
```

**rollbackFor**

기본적으로 `@Transactional`은 **비검사 예외(RuntimeException)** 가 발생했을 때만 롤백한다. 검사 예외(Checked Exception)가 발생해도 커밋한다. `rollbackFor`로 롤백 대상 예외를 추가할 수 있다.

```java
// IOException 발생 시에도 롤백
@Transactional(rollbackFor = Exception.class)
public void registerWithFile(Board board) throws IOException {
    boardMapper.insert(board);
    fileService.save(board.getAttachFile()); // IOException 발생 가능
}
```

---

### 4.3.5 선언적 트랜잭션 vs 프로그래밍 방식 트랜잭션

| 방식 | 특징 | 사용 시점 |
|---|---|---|
| **선언적 (`@Transactional`)** | 코드와 트랜잭션 분리, 간결 | 대부분의 상황 (권장) |
| **프로그래밍 방식 (`TransactionTemplate`)** | 트랜잭션 범위를 메서드 단위보다 세밀하게 제어 | 반복문 안에서 일부만 커밋하는 등 복잡한 케이스 |

실무에서는 거의 모든 경우 `@Transactional`을 사용한다. 단, `@Transactional`은 **같은 클래스 안에서 다른 `@Transactional` 메서드를 호출하면 트랜잭션이 적용되지 않는다**는 점을 주의해야 한다. 프록시 객체를 통한 호출이 아니라 `this`를 통한 직접 호출이 되기 때문이다.

```java
// 이 경우 innerMethod()의 @Transactional은 무시된다
@Transactional
public void outerMethod() {
    this.innerMethod(); // 프록시를 거치지 않는 직접 호출
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void innerMethod() { ... }
```

---

## 4.4 HandlerInterceptor 구조와 Filter와의 차이

### 4.4.1 요청 처리 중간 개입 수단 비교

HTTP 요청이 처리되는 흐름에는 세 가지 중간 개입 수단이 있다.

```
클라이언트 요청
    │
    ▼
┌──────────────────────────────────────────────────┐
│  서블릿 컨테이너 영역                              │
│  ┌──────────────┐                                │
│  │   Filter     │ ← javax/jakarta.servlet.Filter  │
│  └──────┬───────┘   서블릿 컨테이너가 관리          │
└─────────┼────────────────────────────────────────┘
          │
┌─────────▼────────────────────────────────────────┐
│  스프링 MVC 영역                                   │
│  ┌──────────────────┐                            │
│  │  DispatcherServlet│                            │
│  └────────┬─────────┘                            │
│           │                                      │
│  ┌────────▼─────────┐                            │
│  │   Interceptor    │ ← HandlerInterceptor        │
│  └────────┬─────────┘   스프링 컨테이너가 관리     │
│           │                                      │
│  ┌────────▼─────────┐                            │
│  │   Controller     │                            │
│  └────────┬─────────┘                            │
│           │                                      │
│  ┌────────▼─────────┐                            │
│  │  AOP (Advice)    │ ← Service 메서드 레벨 개입   │
│  └──────────────────┘                            │
└──────────────────────────────────────────────────┘
```

| 항목 | Filter | Interceptor | AOP |
|---|---|---|---|
| 관리 주체 | 서블릿 컨테이너 | 스프링 컨테이너 | 스프링 컨테이너 |
| 개입 위치 | DispatcherServlet 앞 | 컨트롤러 앞/뒤 | 서비스 메서드 앞/뒤 |
| 스프링 Bean 접근 | 제한적 | 가능 | 가능 |
| 적용 단위 | URL 패턴 | URL 패턴 | Pointcut 표현식 |
| 주요 사용처 | 인코딩, CORS, 보안 | 로그인 체크, 공통 로깅 | 트랜잭션, 성능 측정 |

---

### 4.4.2 HandlerInterceptor 인터페이스

`HandlerInterceptor`는 세 가지 메서드를 가진 인터페이스다. 스프링 7.x 기준으로 세 메서드 모두 `default` 구현이 있으므로, 필요한 메서드만 오버라이드한다.

```java
public interface HandlerInterceptor {

    // ① 컨트롤러 실행 전 — false 반환 시 요청 처리 중단
    default boolean preHandle(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler) throws Exception {
        return true;
    }

    // ② 컨트롤러 실행 후, 뷰 렌더링 전
    // 컨트롤러가 예외를 던지면 호출되지 않는다
    default void postHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler,
                            ModelAndView modelAndView) throws Exception {
    }

    // ③ 뷰 렌더링 완료 후 — 예외 발생 여부와 무관하게 항상 호출
    default void afterCompletion(HttpServletRequest request,
                                 HttpServletResponse response,
                                 Object handler,
                                 Exception ex) throws Exception {
    }
}
```

**preHandle**

컨트롤러가 실행되기 전에 호출된다. 반환값이 `true`이면 요청 처리를 계속하고, `false`이면 이후의 모든 처리(컨트롤러 포함)를 중단한다. 로그인 여부 확인, 권한 체크, 중복 요청 방지 등에 사용한다.

**postHandle**

컨트롤러가 정상적으로 실행된 후, ViewResolver가 뷰를 렌더링하기 전에 호출된다. `ModelAndView` 객체에 접근하여 모든 뷰에서 공통으로 필요한 데이터를 추가할 수 있다. 컨트롤러에서 예외가 발생하면 호출되지 않는다.

**afterCompletion**

요청 전체 처리가 완료된 후(뷰 렌더링 포함) 호출된다. 예외 발생 여부와 무관하게 반드시 호출되는 것이 `postHandle`과의 차이다. 요청 처리 중 확보한 자원 해제, 요청 처리 시간 측정 완료에 사용한다.

---

### 4.4.3 인터셉터 등록

인터셉터는 스프링 Bean이므로 스프링 컨테이너가 관리한다. `spring-mvc.xml`에서 `<mvc:interceptors>`로 등록하고 적용 URL 패턴을 설정한다.

```xml
<!-- spring-mvc.xml -->
<mvc:interceptors>

    <!-- 인터셉터 ① — 모든 요청에 적용 -->
    <bean class="com.spring.common.LoggingInterceptor"/>

    <!-- 인터셉터 ② — 특정 URL 패턴에만 적용 -->
    <mvc:interceptor>
        <mvc:mapping path="/board/**"/>      <!-- 적용 URL -->
        <mvc:exclude-mapping path="/board/list"/> <!-- 제외 URL -->
        <bean class="com.spring.common.LoginCheckInterceptor"/>
    </mvc:interceptor>

</mvc:interceptors>
```

여러 인터셉터가 등록된 경우 선언 순서대로 `preHandle`이 실행되고, 역순으로 `postHandle`과 `afterCompletion`이 실행된다.

---

## 4.5 세션 기반 회원 로그인/로그아웃과 로그인 체크 인터셉터

### 4.5.1 세션 기반 로그인의 흐름

```
① 로그인 폼에서 아이디/비밀번호 입력 후 POST 요청
② 컨트롤러가 DB에서 회원 정보 조회 및 비밀번호 검증
③ 검증 성공 시 회원 객체를 HttpSession에 저장
④ 이후 요청에서 세션에서 회원 정보를 꺼내 로그인 상태 확인
⑤ 로그아웃 시 세션 무효화
```

---

### 4.5.2 로그인 컨트롤러

```java
@Controller
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/login")
    public String loginForm() {
        return "member/loginForm";
    }

    @PostMapping("/login")
    public String login(String userId, String password,
                        HttpSession session,
                        RedirectAttributes redirectAttr) {

        Member member = memberService.login(userId, password);

        if (member == null) {
            // 로그인 실패 — 폼으로 돌아가며 오류 메시지 전달
            redirectAttr.addFlashAttribute("errorMsg", "아이디 또는 비밀번호가 틀렸습니다.");
            return "redirect:/member/login";
        }

        // 로그인 성공 — 세션에 회원 정보 저장
        session.setAttribute("loginMember", member);
        return "redirect:/board/list";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // 세션 전체 무효화
        return "redirect:/board/list";
    }
}
```

`RedirectAttributes.addFlashAttribute()`는 리다이렉트 후 한 번만 읽히는 임시 데이터를 전달하는 방법이다. 세션이 아닌 플래시 스코프에 저장되어 리다이렉트 후 자동으로 삭제된다. 로그인 실패 메시지를 폼 화면에 전달할 때 유용하다.

---

### 4.5.3 JSP에서 로그인 상태 확인

JSP에서 세션에 저장된 회원 정보를 꺼내 로그인 여부를 판단한다.

```jsp
<%-- EL로 세션 속성 접근 --%>
<c:choose>
    <c:when test="${not empty sessionScope.loginMember}">
        <span>${sessionScope.loginMember.name}님 환영합니다.</span>
        <a href="/member/logout">로그아웃</a>
    </c:when>
    <c:otherwise>
        <a href="/member/login">로그인</a>
    </c:otherwise>
</c:choose>
```

---

### 4.5.4 로그인 체크 인터셉터

로그인이 필요한 모든 컨트롤러 메서드에 세션 체크 코드를 넣으면 중복이 심해진다. 인터셉터의 `preHandle`에서 한 번만 처리하는 것이 올바른 설계다.

```java
@Component
public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        HttpSession session = request.getSession(false); // 세션이 없으면 null 반환

        // 세션이 없거나 로그인 정보가 없으면
        if (session == null || session.getAttribute("loginMember") == null) {

            // 로그인 후 원래 요청 URL로 돌아오기 위해 현재 URL을 파라미터로 전달
            String redirectUrl = request.getRequestURI();
            response.sendRedirect(request.getContextPath()
                + "/member/login?redirectUrl=" + redirectUrl);

            return false; // 요청 처리 중단
        }

        return true; // 로그인 상태 확인됨 → 컨트롤러 실행 허용
    }
}
```

`request.getSession(false)`는 세션이 없을 때 새 세션을 생성하지 않고 `null`을 반환한다. `getSession()` 또는 `getSession(true)`는 세션이 없으면 새로 생성하므로, 로그인 체크 시에는 반드시 `false`를 사용해야 한다.

```xml
<!-- spring-mvc.xml 에서 로그인 체크 인터셉터 등록 -->
<mvc:interceptors>
    <mvc:interceptor>
        <!-- 게시판 쓰기, 수정, 삭제에만 로그인 체크 적용 -->
        <mvc:mapping path="/board/write"/>
        <mvc:mapping path="/board/edit"/>
        <mvc:mapping path="/board/delete"/>
        <bean class="com.spring.common.LoginCheckInterceptor"/>
    </mvc:interceptor>
</mvc:interceptors>
```

---

## 4.6 MultipartResolver를 이용한 파일 업로드

### 4.6.1 Multipart 요청이란

일반 HTML 폼은 텍스트 데이터만 전송할 수 있다. 파일을 함께 전송하려면 폼의 `enctype`을 `multipart/form-data`로 설정해야 하며, 이 형식으로 전송된 요청을 **Multipart 요청**이라고 한다.

```html
<!-- 파일 업로드가 포함된 폼 -->
<form action="/board/write" method="post" enctype="multipart/form-data">
    <input type="text"   name="title"/>
    <textarea name="content"></textarea>
    <input type="file"   name="attachFile"/>  <!-- 파일 선택 -->
    <button type="submit">등록</button>
</form>
```

스프링 MVC는 `MultipartResolver`를 통해 Multipart 요청을 파싱하고, 파일 데이터를 `MultipartFile` 객체로 변환해준다.

---

### 4.6.2 MultipartResolver 설정

Spring 7.x(Servlet 6.x 기반)에서는 Servlet 컨테이너(Tomcat)의 Multipart 파싱 기능을 사용하는 `StandardServletMultipartResolver`를 권장한다.

**`web.xml` — Multipart 설정 추가**

DispatcherServlet 안에 `<multipart-config>`를 추가한다.

```xml
<servlet>
    <servlet-name>dispatcher</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
    <init-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>/WEB-INF/spring-mvc.xml</param-value>
    </init-param>
    <load-on-startup>1</load-on-startup>
    <multipart-config>
        <!-- 업로드 임시 저장 디렉토리 (비어 있으면 시스템 기본 경로 사용) -->
        <location></location>
        <!-- 파일 하나의 최대 크기: 10MB -->
        <max-file-size>10485760</max-file-size>
        <!-- 요청 전체의 최대 크기: 50MB -->
        <max-request-size>52428800</max-request-size>
    </multipart-config>
</servlet>
```

**`spring-mvc.xml` — MultipartResolver Bean 등록**

```xml
<bean id="multipartResolver"
      class="org.springframework.web.multipart.support.StandardServletMultipartResolver"/>
```

Bean의 `id`가 반드시 `multipartResolver`여야 한다. DispatcherServlet이 이 이름으로 Bean을 찾기 때문이다.

---

### 4.6.3 파일 업로드 컨트롤러

```java
@PostMapping("/write")
public String write(@ModelAttribute Board board,
                    @RequestParam(required = false) MultipartFile attachFile)
        throws IOException {

    // 파일이 첨부된 경우에만 처리
    if (attachFile != null && !attachFile.isEmpty()) {

        String originalName = attachFile.getOriginalFilename(); // 원본 파일명
        long   fileSize     = attachFile.getSize();             // 파일 크기(바이트)
        String contentType  = attachFile.getContentType();      // MIME 타입

        // 저장 파일명 — 동일 파일명 충돌 방지를 위해 UUID 사용
        String savedName = UUID.randomUUID().toString()
                         + "_" + originalName;

        // 실제 파일 저장 경로 (운영 환경에서는 외부 경로 사용 권장)
        String uploadPath = "C:/uploads/";
        attachFile.transferTo(new File(uploadPath + savedName));

        // DB에 파일 정보 저장
        board.setOriginalFileName(originalName);
        board.setSavedFileName(savedName);
    }

    boardService.register(board);
    return "redirect:/board/detail?no=" + board.getNo();
}
```

`MultipartFile`의 주요 메서드는 다음과 같다.

| 메서드 | 반환 타입 | 설명 |
|---|---|---|
| `getOriginalFilename()` | `String` | 클라이언트가 업로드한 원본 파일명 |
| `getSize()` | `long` | 파일 크기 (바이트) |
| `getContentType()` | `String` | MIME 타입 (예: `image/jpeg`) |
| `isEmpty()` | `boolean` | 파일이 비어 있는지 여부 |
| `getBytes()` | `byte[]` | 파일 내용을 바이트 배열로 반환 |
| `getInputStream()` | `InputStream` | 파일 내용을 스트림으로 반환 |
| `transferTo(File dest)` | `void` | 지정한 File 경로로 파일 저장 |

---

### 4.6.4 파일 저장 전략

**저장 파일명 충돌 방지**

같은 이름의 파일을 여러 번 업로드하면 이전 파일이 덮어써진다. UUID를 접두어로 붙여 파일명을 고유하게 만드는 것이 일반적이다.

```java
String savedName = UUID.randomUUID().toString() + "_" + originalName;
```

**저장 경로 분리**

업로드 파일을 웹 애플리케이션 폴더(`webapp/`) 안에 저장하면 배포 시 삭제된다. 운영 환경에서는 웹 애플리케이션 외부 경로에 저장하고, 별도 파일 서버를 두는 것이 표준이다.

**파일 종류 검증**

업로드 허용 파일 형식을 제한하지 않으면 악성 파일이 서버에 저장될 수 있다.

```java
// 허용 확장자 목록으로 검증
List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif", "pdf");
String extension = originalName.substring(originalName.lastIndexOf(".") + 1)
                               .toLowerCase();
if (!allowedExtensions.contains(extension)) {
    throw new IllegalArgumentException("허용되지 않는 파일 형식입니다: " + extension);
}
```

---

### 4.6.5 첨부파일 DB 테이블 설계

첨부파일 정보를 게시글 테이블에 직접 넣으면 파일이 여러 개인 경우 처리하기 어렵다. 별도 테이블로 분리하는 것이 유연하다.

```sql
CREATE TABLE attach (
    attach_no      INT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    board_no       INT         NOT NULL,                          -- 게시글 FK
    original_name  VARCHAR(255) NOT NULL,                         -- 원본 파일명
    saved_name     VARCHAR(255) NOT NULL,                         -- 저장 파일명 (UUID)
    file_size      BIGINT       DEFAULT 0,                        -- 파일 크기
    created_date   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (board_no) REFERENCES board(no) ON DELETE CASCADE
);
```

`ON DELETE CASCADE`를 설정하면 게시글 삭제 시 연결된 첨부파일 레코드도 자동으로 삭제된다. 단, 실제 파일 시스템의 파일은 별도로 삭제 처리해야 한다.

---

## 4.7 Custom View를 이용한 파일 다운로드

### 4.7.1 Spring MVC의 View 처리 구조

Spring MVC에서 컨트롤러가 반환하는 것은 뷰 이름(String)이고, ViewResolver가 그 이름을 실제 View 객체로 변환한다. View 객체는 `render()` 메서드를 통해 HTTP 응답을 생성한다.

```
Controller
  ↓ "board/list" (뷰 이름) 또는 ModelAndView 반환
ViewResolver
  ↓ View 객체 조회
View.render(model, request, response)
  ↓ HTTP 응답 생성
```

`View` 인터페이스의 핵심 메서드는 다음과 같다.

```java
public interface View {
    String getContentType();
    void render(Map<String, ?> model, HttpServletRequest request,
                HttpServletResponse response) throws Exception;
}
```

기본 제공되는 주요 View 구현체는 다음과 같다.

| View 구현체 | 역할 |
|---|---|
| `InternalResourceView` | JSP 포워딩 (InternalResourceViewResolver가 생성) |
| `RedirectView` | HTTP 리다이렉트 (`redirect:` 접두어) |
| `MappingJackson2JsonView` | JSON 응답 |
| `AbstractView` | **Custom View의 기반 클래스** |

`AbstractView`를 상속하면 어떤 형태의 응답(파일, Excel, PDF 등)도 View로 구현할 수 있다.

---

### 4.7.2 기존 방식의 문제 — 컨트롤러에서 직접 응답 처리

파일 다운로드를 컨트롤러에서 `HttpServletResponse`를 직접 조작하는 방식으로 구현하면 다음과 같다.

```java
// BoardController.java — 직접 처리 방식
@GetMapping("/download")
public void download(@RequestParam int attachNo,
                     HttpServletResponse response) throws IOException {
    Attach attach = boardService.getAttach(attachNo);

    String encodedName = URLEncoder.encode(attach.getOriginalName(), StandardCharsets.UTF_8)
                                   .replaceAll("\\+", "%20");
    response.setContentType("application/octet-stream");
    response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
    response.setContentLengthLong(file.length());

    try (FileInputStream fis = new FileInputStream(file);
         OutputStream out  = response.getOutputStream()) {
        byte[] buf = new byte[8192];
        int len;
        while ((len = fis.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
    }
}
```

이 방식의 문제점은 다음과 같다.

- 파일 스트리밍이라는 **응답 생성 책임**이 Controller 안에 섞인다
- 다운로드 형태가 다른 경우(파일 경로 기반, 바이트 배열 기반 등) 동일한 스트리밍 코드가 여러 컨트롤러에 중복된다
- 반환 타입이 `void`여서 다른 핸들러 메서드와 일관성이 없다

---

### 4.7.3 Custom View 구현 — AbstractView 상속

`AbstractView`를 상속하여 파일 다운로드 전용 View 클래스를 만든다. Controller는 View가 필요한 데이터만 Model에 담아주면 되고, 실제 스트리밍 코드는 View가 담당한다.

```java
// com/spring/common/FileDownloadView.java
public class FileDownloadView extends AbstractView {

    public FileDownloadView() {
        setContentType("application/octet-stream");
    }

    @Override
    protected void renderMergedOutputModel(
            Map<String, Object> model,
            HttpServletRequest  request,
            HttpServletResponse response) throws Exception {

        // Controller가 Model에 담은 Attach 객체를 꺼낸다
        Attach attach    = (Attach) model.get("attach");
        String uploadDir = (String) model.get("uploadDir");

        File file = new File(uploadDir + attach.getSavedName());
        if (!file.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "파일이 존재하지 않습니다.");
            return;
        }

        // 한글 파일명 인코딩
        String encodedName = URLEncoder.encode(attach.getOriginalName(), StandardCharsets.UTF_8)
                                       .replaceAll("\\+", "%20");

        // HTTP 응답 헤더 설정
        response.setContentType(getContentType());
        response.setHeader("Content-Disposition",
                           "attachment; filename*=UTF-8''" + encodedName);
        response.setContentLengthLong(file.length());

        // 파일 스트리밍 — I/O 책임은 View가 전담
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream     out = response.getOutputStream()) {
            FileCopyUtils.copy(fis, out);  // Spring 제공 유틸리티
        }
    }
}
```

`FileCopyUtils.copy()`는 Spring이 제공하는 유틸리티로, 직접 반복문을 작성하지 않고 InputStream → OutputStream 복사를 처리한다.

---

### 4.7.4 BeanNameViewResolver 등록

Custom View를 사용하려면 뷰 이름과 View Bean을 연결하는 ViewResolver가 필요하다. `BeanNameViewResolver`는 뷰 이름과 같은 이름의 Bean을 View로 사용한다.

**spring-mvc.xml 설정**

```xml
<!-- BeanNameViewResolver: 뷰 이름 = Bean 이름으로 View를 찾는다 -->
<bean class="org.springframework.web.servlet.view.BeanNameViewResolver">
    <!-- InternalResourceViewResolver보다 먼저 시도하도록 우선순위를 높인다 -->
    <property name="order" value="1"/>
</bean>

<!-- InternalResourceViewResolver: 우선순위를 낮춘다 -->
<bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
    <property name="prefix" value="/WEB-INF/views/"/>
    <property name="suffix" value=".jsp"/>
    <property name="order"  value="2"/>
</bean>

<!-- FileDownloadView를 Bean으로 등록 — id가 뷰 이름이 된다 -->
<bean id="fileDownloadView" class="com.spring.common.FileDownloadView"/>
```

ViewResolver는 `order` 값이 낮을수록 먼저 시도된다. `BeanNameViewResolver`가 `fileDownloadView` Bean을 찾으면 `InternalResourceViewResolver`는 시도하지 않는다.

```
컨트롤러가 "fileDownloadView" 반환
  → BeanNameViewResolver 시도 → fileDownloadView Bean 발견 → render() 호출
  → InternalResourceViewResolver는 시도하지 않음
```

---

### 4.7.5 방법 1 — BeanNameViewResolver + 뷰 이름 반환

앞서 4.7.4에서 설정한 `BeanNameViewResolver`를 활용하는 방법입니다. Controller는 뷰 이름 문자열(`"fileDownloadView"`)을 `ModelAndView`에 담아 반환하고, Spring이 해당 이름의 Bean을 찾아 View를 실행합니다.

```java
// BoardController.java — 방법 1: BeanNameViewResolver 사용
private static final String UPLOAD_DIR = "C:/uploads/";

@GetMapping("/download")
public ModelAndView download(@RequestParam int attachNo) {
    Attach attach = boardService.getAttach(attachNo);

    // 뷰 이름 "fileDownloadView" → BeanNameViewResolver가 해당 Bean을 찾아 실행
    ModelAndView mav = new ModelAndView("fileDownloadView");
    mav.addObject("attach",    attach);
    mav.addObject("uploadDir", UPLOAD_DIR);
    return mav;
}
```

처리 흐름은 다음과 같습니다.

```
Controller → ModelAndView("fileDownloadView") 반환
  → DispatcherServlet → BeanNameViewResolver
  → id="fileDownloadView" Bean 조회
  → FileDownloadView.renderMergedOutputModel() 실행
```

Controller는 뷰의 구체적인 클래스를 전혀 알 필요가 없습니다. View 구현체를 다른 클래스로 교체해도 Controller 코드는 수정하지 않아도 됩니다.

---

### 4.7.6 방법 2 — Controller에서 View 객체를 직접 생성하여 반환

`BeanNameViewResolver` 설정 없이 Controller가 `FileDownloadView` 객체를 직접 생성하여 `ModelAndView`에 전달하는 방법입니다.

```java
// BoardController.java — 방법 2: View 객체 직접 생성
private static final String UPLOAD_DIR = "C:/uploads/";

@GetMapping("/download")
public ModelAndView download(@RequestParam int attachNo) {
    Attach attach = boardService.getAttach(attachNo);

    // View 객체를 직접 생성하여 ModelAndView에 설정
    FileDownloadView view = new FileDownloadView();

    ModelAndView mav = new ModelAndView(view);  // 뷰 이름 대신 View 객체 전달
    mav.addObject("attach",    attach);
    mav.addObject("uploadDir", UPLOAD_DIR);
    return mav;
}
```

`ModelAndView` 생성자에 뷰 이름(String) 대신 View 객체를 직접 전달하면 ViewResolver 단계를 거치지 않고 해당 View가 즉시 실행됩니다.

```
Controller → ModelAndView(new FileDownloadView()) 반환
  → DispatcherServlet
  → ViewResolver 조회 없이 바로 FileDownloadView.renderMergedOutputModel() 실행
```

`FileDownloadView`를 스프링 Bean으로 등록하지 않아도 되므로 `spring-mvc.xml` 설정 변경이 필요 없습니다. 단, Controller가 `FileDownloadView` 클래스를 직접 `import`해야 합니다.

`@Autowired`로 주입받는 방법을 사용하면 직접 생성의 단점을 일부 보완할 수 있습니다.

```java
@Controller
@RequestMapping("/board")
public class BoardController {

    @Autowired
    private FileDownloadView fileDownloadView;  // Bean으로 등록한 경우 주입 가능

    @GetMapping("/download")
    public ModelAndView download(@RequestParam int attachNo) {
        Attach attach = boardService.getAttach(attachNo);

        ModelAndView mav = new ModelAndView(fileDownloadView);
        mav.addObject("attach",    attach);
        mav.addObject("uploadDir", UPLOAD_DIR);
        return mav;
    }
}
```

이 경우 `FileDownloadView`를 `spring-mvc.xml`에 Bean으로 등록하되 `BeanNameViewResolver`는 추가하지 않아도 됩니다.

---

### 4.7.7 두 방법의 장단점 비교

**방법 1 — BeanNameViewResolver + 뷰 이름 반환**

장점은 다음과 같습니다.

첫째, Controller가 View 구현 클래스를 전혀 알지 못하므로 결합도가 낮습니다. View 클래스를 다른 구현체로 교체해도 Controller 코드 수정이 필요 없습니다. 둘째, Spring MVC의 표준 ViewResolver 메커니즘을 따르기 때문에 프레임워크 설계 원칙에 부합합니다. 셋째, 하나의 View Bean을 여러 컨트롤러에서 이름으로 공유하여 사용할 수 있습니다.

단점은 다음과 같습니다.

`BeanNameViewResolver`를 추가 설정해야 하며, `InternalResourceViewResolver`와의 `order` 우선순위를 반드시 조정해야 합니다. 뷰 이름이 문자열이므로 오타가 있어도 컴파일 시점에 오류가 발생하지 않습니다.

**방법 2 — Controller에서 View 객체 직접 생성**

장점은 다음과 같습니다.

첫째, `BeanNameViewResolver` 설정이 필요 없어 초기 설정이 간단합니다. 둘째, 컴파일러가 클래스 존재 여부를 검증하므로 뷰 이름 오타 문제가 없습니다. 셋째, 특정 요청 조건에 따라 다른 View 객체를 선택해서 반환하는 동적 처리가 자연스럽습니다.

단점은 다음과 같습니다.

Controller가 `FileDownloadView` 클래스를 직접 `import`해야 하므로 결합도가 높아집니다. View를 교체할 때 Controller 코드도 함께 수정해야 합니다. `new FileDownloadView()`로 직접 생성하면 스프링 컨테이너가 해당 객체를 관리하지 않으므로 `@Autowired` 등의 DI가 적용되지 않습니다.

**비교표**

| 항목 | 방법 1 — BeanNameViewResolver | 방법 2 — View 직접 생성 |
|---|---|---|
| ViewResolver 설정 | 필요 (`order` 조정 포함) | 불필요 |
| Controller-View 결합도 | 낮음 (이름으로만 참조) | 높음 (클래스 직접 참조) |
| View 교체 시 Controller 수정 | 불필요 | 필요 |
| 컴파일 타임 오류 검출 | 불가 (문자열 뷰 이름) | 가능 (클래스 참조) |
| 스프링 DI 적용 | 가능 (Bean으로 관리됨) | 직접 생성 시 불가 |
| 다수 컨트롤러 공유 | 용이 | 각자 생성 필요 |
| 프레임워크 설계 원칙 부합 | 높음 | 보통 |

**권장 사항**: 실무에서는 방법 1(BeanNameViewResolver)을 선호합니다. View를 Bean으로 등록하여 스프링이 관리하게 하면 DI, AOP, 테스트 등 스프링 생태계의 이점을 온전히 활용할 수 있습니다. 방법 2는 별도 설정 없이 빠르게 적용해야 하거나 요청 조건에 따라 View를 동적으로 선택해야 할 때 유용합니다.

---

### 4.7.8 전체 방식 비교 — 직접 응답 처리 vs Custom View

| 항목 | 직접 응답 처리 방식 | Custom View 방식 |
|---|---|---|
| Controller 반환 타입 | `void` | `ModelAndView` |
| 스트리밍 코드 위치 | Controller 내부 | View 클래스 |
| Controller 책임 | 조회 + 헤더 설정 + 스트리밍 | 조회 + 데이터 전달 |
| 재사용성 | 낮음 (컨트롤러마다 중복) | 높음 (View 하나로 여러 다운로드 처리) |
| 설정 복잡도 | 없음 | BeanNameViewResolver 추가 필요 (방법 1) |

다운로드 기능이 하나뿐이고 단순하다면 직접 처리 방식이 간편합니다. 다운로드 유형이 여러 가지이거나(파일/PDF/Excel), 다운로드 로직이 복잡하다면 Custom View로 분리하는 것이 유지보수에 유리합니다.

---

## 4.8 실습 — 게시판에 AOP, 트랜잭션, 로그인 인터셉터, 파일 업로드 적용

### 실습 목표

3일차 게시판에 다음 기능을 순서대로 추가한다.
- AOP 로깅 Aspect로 Service 메서드 호출 추적
- @Transactional로 조회수 증가 + 상세 조회 묶기
- 세션 기반 로그인/로그아웃
- 로그인 체크 인터셉터로 쓰기/수정/삭제 보호
- 게시글 등록 시 파일 첨부

---

### 실습 환경

```
spring-day4/
├── pom.xml
└── src/main/
    ├── java/com/spring/
    │   ├── board/
    │   │   ├── Board.java  /  Attach.java
    │   │   ├── BoardDao.java / BoardDaoImpl.java
    │   │   ├── BoardService.java / BoardServiceImpl.java  ← @Transactional 추가
    │   │   └── BoardController.java                       ← 파일 업로드 추가
    │   ├── member/
    │   │   ├── Member.java
    │   │   ├── MemberDao.java / MemberDaoImpl.java
    │   │   ├── MemberService.java / MemberServiceImpl.java
    │   │   └── MemberController.java                      ← 로그인/로그아웃
    │   └── common/
    │       ├── LoggingAspect.java                         ← AOP
    │       └── LoginCheckInterceptor.java                 ← 인터셉터
    ├── resources/
    │   ├── applicationContext.xml  ← TX 설정, AOP 활성화 추가
    │   ├── mybatis-config.xml
    │   ├── db.properties
    │   └── mapper/
    │       ├── board/BoardMapper.xml
    │       ├── board/AttachMapper.xml
    │       └── member/MemberMapper.xml
    └── webapp/WEB-INF/
        ├── web.xml                ← multipart-config 추가
        ├── spring-mvc.xml         ← 인터셉터, AOP, MultipartResolver 추가
        └── views/
            ├── board/
            │   ├── list.jsp / detail.jsp
            │   └── writeForm.jsp  ← 파일 첨부 추가
            └── member/
                └── loginForm.jsp
```

---

### 실습 순서

**Step 1 — AOP 로깅 Aspect 추가**

`applicationContext.xml`에 `<aop:aspectj-autoproxy/>`를 추가하고, `LoggingAspect` 클래스를 작성한다. `execution(* com.spring..*Service.*(..))` Pointcut으로 Service 레이어의 모든 메서드 진입/종료를 로그로 출력한다. 게시글 목록 조회 후 콘솔에서 로그를 확인한다.

**Step 2 — @Transactional 적용**

`applicationContext.xml`에 `DataSourceTransactionManager`와 `<tx:annotation-driven/>`을 추가한다. `BoardServiceImpl.getDetail()`에 `@Transactional`을 적용하여 조회수 증가와 게시글 조회를 하나의 트랜잭션으로 묶는다. 목록 조회에는 `@Transactional(readOnly = true)`를 적용한다.

**Step 3 — Member 테이블 및 로그인 기능 추가**

```sql
CREATE TABLE member (
    member_id  VARCHAR(50)  NOT NULL PRIMARY KEY,
    password   VARCHAR(100) NOT NULL,
    name       VARCHAR(50)  NOT NULL,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO member VALUES ('admin', '1234', '관리자', NOW());
```

`MemberMapper`, `MemberDao`, `MemberService`, `MemberController`를 작성하고 로그인/로그아웃을 구현한다.

**Step 4 — 로그인 체크 인터셉터 적용**

`LoginCheckInterceptor`를 작성하고 `spring-mvc.xml`에 등록한다. 쓰기(`/board/write`), 수정(`/board/edit`), 삭제(`/board/delete`) URL에 로그인 체크를 적용한다. 로그아웃 상태에서 쓰기 URL 접근 시 로그인 페이지로 이동하는 것을 확인한다.

**Step 5 — 파일 업로드 기능 추가**

`web.xml`에 `<multipart-config>`를 추가하고, `spring-mvc.xml`에 `StandardServletMultipartResolver`를 등록한다. `attach` 테이블을 생성하고 `AttachMapper`와 XML 매퍼를 작성한다. `BoardController.write()`에 `MultipartFile` 파라미터를 추가하고 파일 저장 로직을 구현한다. 게시글 등록 후 `detail.jsp`에서 첨부파일 링크를 확인한다.

---

### 주요 확인 포인트

| 항목 | 확인 방법 |
|---|---|
| AOP Pointcut 범위 | 다른 레이어(Controller, DAO)에는 로그가 찍히지 않는 것 확인 |
| 트랜잭션 롤백 | `getDetail()` 안에 강제 예외를 발생시켜 조회수가 증가하지 않는 것 확인 |
| Self-invocation 문제 | 같은 Service 클래스 안에서 `@Transactional` 메서드를 this로 호출하면 적용 안 됨 확인 |
| 인터셉터 적용 순서 | 여러 인터셉터 등록 시 선언 순서대로 preHandle 실행 확인 |
| 세션 null 처리 | `getSession(false)` vs `getSession(true)` 차이 확인 |
| 파일명 중복 방지 | 같은 파일명으로 두 번 업로드 후 저장된 파일명이 다른 것 확인 |
| multipart-config 누락 | 설정 없이 파일 업로드 시 `MultipartException` 발생 확인 |

---

## 정리

**AOP**는 횡단 관심사를 Aspect로 분리하여 핵심 코드를 오염시키지 않고 공통 기능을 적용하는 기법이다. 스프링 AOP는 런타임 프록시 방식으로 동작하며 메서드 실행 JoinPoint만 지원한다. 어드바이스는 목적에 맞게 선택한다. 단순 로깅은 `@Before`/`@AfterReturning`, 예외 추적은 `@AfterThrowing`, 실행 시간 측정·반환값 조작·실행 제어는 `@Around`를 사용한다.

**@Transactional**은 선언적 트랜잭션의 표준 방법이다. 기본적으로 `RuntimeException` 발생 시 롤백하며, `propagation`, `isolation`, `readOnly`, `rollbackFor` 속성으로 세밀하게 제어할 수 있다. 같은 클래스 내 Self-invocation에서는 AOP 프록시를 거치지 않아 `@Transactional`이 무시된다는 점을 반드시 기억해야 한다.

**Filter vs Interceptor vs AOP**는 개입 위치가 다르다. 인코딩·CORS는 Filter, 로그인 체크·공통 데이터 추가는 Interceptor, 트랜잭션·성능 측정은 AOP를 사용한다. Interceptor의 `preHandle`이 `false`를 반환하면 이후 모든 처리가 중단된다.

**파일 업로드**는 `StandardServletMultipartResolver`와 `web.xml`의 `<multipart-config>`를 조합한다. 저장 파일명은 UUID로 고유하게 만들고, 파일 확장자 검증, 외부 경로 저장, 첨부파일 별도 테이블 분리를 실무 표준으로 적용한다.

**파일 다운로드**는 컨트롤러에서 직접 응답을 처리하는 방식과 `AbstractView`를 상속한 Custom View로 분리하는 방식이 있다. Custom View 방식은 `BeanNameViewResolver`와 조합하여 응답 생성 책임을 View로 위임하고 컨트롤러를 단순하게 유지한다.

---

## 다음 시간 예고

5일차에서는 전체 아키텍처를 복습하고, 예외 처리(`@ExceptionHandler`, `SimpleMappingExceptionResolver`)를 적용한다. XML 설정과 Java Config 방식을 비교하며 스프링 부트로의 전환 포인트를 살펴보고, 최종 게시판 프로젝트를 완성한다.
