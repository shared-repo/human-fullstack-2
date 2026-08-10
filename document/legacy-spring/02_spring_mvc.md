# 2일차 — Spring MVC 구조

> **환경** : Java 21 + Spring Framework 7.0.x + Tomcat 10.x + Maven

---

## 2.1 DispatcherServlet 동작 원리

### 2.1.1 Model2 FrontController 복습

Model2 패턴에서는 모든 요청을 하나의 서블릿이 받아서 분기하는 **FrontController** 패턴을 직접 구현했다.

```java
// 직접 만든 FrontController 서블릿
@WebServlet("*.do")
public class FrontController extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String uri = req.getRequestURI();                         // /board/list.do
        String path = uri.substring(req.getContextPath().length()); // /board/list.do

        // URL을 직접 비교해서 분기 — 컨트롤러가 늘어날수록 이 코드도 늘어난다
        if (path.equals("/board/list.do")) {
            new BoardListController().execute(req, resp);
        } else if (path.equals("/board/detail.do")) {
            new BoardDetailController().execute(req, resp);
        }
        // ...
    }
}
```

이 방식에는 구조적인 한계가 있다.

- URL과 컨트롤러의 매핑 정보가 if-else 코드 안에 하드코딩되어 있다. 기능이 늘어날수록 FrontController가 비대해진다.
- 컨트롤러를 직접 `new`로 생성하므로 스프링 Bean으로 관리할 수 없고, DI도 불가능하다.
- 뷰 이름과 파일 경로의 규칙을 개발자가 직접 관리해야 한다.

**스프링 MVC의 `DispatcherServlet`은 이 FrontController를 프레임워크 차원에서 제공한다.**  
URL-컨트롤러 매핑, 컨트롤러 실행, 뷰 렌더링까지의 전 과정을 처리하는 규칙을 내장하고 있어, 개발자는 컨트롤러 클래스와 설정만 작성하면 된다.

---

### 2.1.2 DispatcherServlet의 전체 요청 처리 흐름

```
클라이언트 요청 (GET /board/list)
        │
        ▼
┌─────────────────────────────────────────────────────────────────┐
│                      DispatcherServlet                          │
│                                                                 │
│  ① HandlerMapping에게 "이 URL 처리할 수 있는 Handler(컨트롤러)  │
│     찾아줘"라고 요청                                             │
│        │                                                        │
│        ▼                                                        │
│  ② HandlerMapping이 적합한 Handler(컨트롤러)와                  │
│     HandlerAdapter를 반환                                        │
│        │                                                        │
│        ▼                                                        │
│  ③ HandlerAdapter가 실제 컨트롤러 메서드를 실행                 │
│     (파라미터 바인딩, 메서드 호출)                               │
│        │                                                        │
│        ▼                                                        │
│  ④ 컨트롤러가 ModelAndView(또는 뷰 이름 String)를 반환         │
│        │                                                        │
│        ▼                                                        │
│  ⑤ ViewResolver에게 "list라는 이름의 뷰 파일이 어디 있어?"     │
│     라고 요청                                                    │
│        │                                                        │
│        ▼                                                        │
│  ⑥ ViewResolver가 실제 뷰 파일 경로를 반환                     │
│     (/WEB-INF/views/board/list.jsp)                             │
│        │                                                        │
│        ▼                                                        │
│  ⑦ View가 Model 데이터를 사용해 HTML 렌더링 후 응답            │
└─────────────────────────────────────────────────────────────────┘
        │
        ▼
클라이언트 응답 (HTML)
```

각 역할이 독립된 컴포넌트로 분리되어 있다는 점이 핵심이다. 개발자가 직접 작성하는 부분은 **③의 컨트롤러 메서드**뿐이며, 나머지는 스프링이 처리한다.

---

### 2.1.3 두 개의 ApplicationContext

스프링 MVC 웹 애플리케이션에는 ApplicationContext가 두 개 존재한다. 이 구조를 이해하지 못하면 Bean을 등록했는데 못 찾는다는 오류로 당황하게 된다.

```
┌──────────────────────────────────────────────────────────┐
│              Root WebApplicationContext                   │
│         (ContextLoaderListener가 생성)                    │
│                                                          │
│  Service, DAO, DataSource, Transaction 등                │
│  웹과 무관한 비즈니스 Bean들                              │
│  설정 파일: applicationContext.xml                        │
└─────────────────────────┬────────────────────────────────┘
                          │  부모 컨텍스트 (자식이 부모 Bean 접근 가능)
┌─────────────────────────▼────────────────────────────────┐
│           Servlet WebApplicationContext                   │
│         (DispatcherServlet이 생성)                        │
│                                                          │
│  Controller, HandlerMapping, ViewResolver 등             │
│  웹 요청 처리에 필요한 Bean들                             │
│  설정 파일: spring-mvc.xml (또는 servlet-context.xml)     │
└──────────────────────────────────────────────────────────┘
```

**부모-자식 관계**이기 때문에 Controller(자식)는 Service(부모)를 주입받아 사용할 수 있지만, Service(부모)는 Controller(자식)의 Bean을 알지 못한다. 이 방향을 역전하면 주입 실패 오류가 발생한다.

소규모 프로젝트에서는 두 컨텍스트를 굳이 분리하지 않고 하나로 합쳐서 사용하기도 한다. 이 교육에서는 실무 구조와 동일하게 두 개로 분리하여 진행한다.

---

## 2.2 HandlerMapping, HandlerAdapter, ViewResolver

### 2.2.1 HandlerMapping

**HandlerMapping**은 들어온 HTTP 요청 URL을 어떤 컨트롤러(Handler)가 처리할지 결정하는 컴포넌트다.

스프링 MVC가 기본으로 등록하는 HandlerMapping 중 가장 중요한 것은 `RequestMappingHandlerMapping`이다. 이것은 `@RequestMapping` 어노테이션을 스캔하여 URL과 컨트롤러 메서드의 매핑 테이블을 만들어 관리한다.

```
RequestMappingHandlerMapping 내부 매핑 테이블 (개념 표현)

GET  /board/list    → BoardController.list()
GET  /board/detail  → BoardController.detail()
POST /board/write   → BoardController.write()
POST /board/delete  → BoardController.delete()
```

개발자가 `@RequestMapping`을 작성하면 이 테이블이 자동으로 구성된다. Model2에서 if-else로 관리하던 URL 매핑을 HandlerMapping이 대체하는 것이다.

---

### 2.2.2 HandlerAdapter

**HandlerAdapter**는 HandlerMapping이 찾아낸 컨트롤러를 실제로 실행하는 역할을 한다. DispatcherServlet이 컨트롤러를 직접 호출하지 않고 HandlerAdapter를 통해 호출하는 이유는, 컨트롤러의 종류에 따라 호출 방식이 다를 수 있기 때문이다.

`@Controller` 어노테이션 기반의 컨트롤러는 `RequestMappingHandlerAdapter`가 처리한다. 이 HandlerAdapter가 하는 일은 다음과 같다.

- 컨트롤러 메서드의 파라미터 타입을 분석하여 `HttpServletRequest`, `Model`, `@RequestParam` 값 등을 자동으로 만들어서 전달한다.
- 컨트롤러 메서드의 반환값을 분석하여 `ModelAndView`로 변환한다.

개발자 입장에서 HandlerAdapter의 존재는 거의 의식하지 않아도 된다. 스프링 MVC를 설정하면 자동으로 등록되기 때문이다.

---

### 2.2.3 ModelAndView

**ModelAndView**는 컨트롤러가 처리 결과를 DispatcherServlet에 전달할 때 사용하는 객체로, **뷰 이름(View Name)** 과 **모델 데이터(Model)** 를 함께 담는다.

```java
// ModelAndView를 직접 생성해서 반환하는 방식
@RequestMapping("/board/list")
public ModelAndView list() {
    ModelAndView mav = new ModelAndView();
    mav.setViewName("board/list");         // 논리 뷰 이름
    mav.addObject("list", boardService.getList()); // 모델 데이터
    return mav;
}
```

실무에서는 `ModelAndView`를 직접 생성하기보다, 메서드 파라미터로 `Model`을 받고 뷰 이름은 `String`으로 반환하는 더 간결한 방식을 선호한다.

```java
// 더 많이 쓰이는 간결한 방식
@RequestMapping("/board/list")
public String list(Model model) {
    model.addAttribute("list", boardService.getList());
    return "board/list";  // 논리 뷰 이름만 String으로 반환
}
```

두 방식 모두 결과는 동일하다. 스프링이 내부적으로 `String` 반환값을 `ModelAndView`로 변환한다.

---

### 2.2.4 ViewResolver

**ViewResolver**는 컨트롤러가 반환한 **논리 뷰 이름**을 실제 뷰 파일 경로로 변환하는 컴포넌트다.

예를 들어 컨트롤러가 `"board/list"` 라는 문자열을 반환했을 때, ViewResolver는 이것을 실제 파일 경로인 `/WEB-INF/views/board/list.jsp`로 변환한다.

```
컨트롤러 반환값:  "board/list"
                      │
                      ▼ ViewResolver 변환
실제 파일 경로:  /WEB-INF/views/ + board/list + .jsp
              = /WEB-INF/views/board/list.jsp
```

스프링 MVC에서 가장 기본적으로 사용하는 ViewResolver는 `InternalResourceViewResolver`다.

```xml
<!-- spring-mvc.xml -->
<bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
    <property name="prefix" value="/WEB-INF/views/"/>
    <property name="suffix" value=".jsp"/>
</bean>
```

`prefix`(앞에 붙이는 경로)와 `suffix`(뒤에 붙이는 확장자)를 설정해 두면, 컨트롤러는 중간 경로 이름(`board/list`)만 반환하면 된다. 뷰 파일의 위치 규칙을 한 곳에서 관리할 수 있는 셈이다.

`/WEB-INF/views/` 경로를 사용하는 이유는, `/WEB-INF/` 하위 파일은 클라이언트가 URL로 직접 접근할 수 없기 때문이다. JSP 파일을 반드시 컨트롤러를 거쳐서만 접근하도록 강제할 수 있다.

---

## 2.3 web.xml 및 스프링 MVC 설정 파일 구성

### 2.3.1 웹 프로젝트 기본 구조

```
spring-day2/
├── pom.xml
└── src/main/
    ├── java/com/spring/
    │   └── board/
    │       └── BoardController.java
    ├── resources/
    │   └── applicationContext.xml        ← Root 컨텍스트 설정
    └── webapp/
        ├── WEB-INF/
        │   ├── web.xml                   ← 서블릿 컨테이너 설정
        │   ├── spring-mvc.xml            ← Servlet 컨텍스트 설정
        │   └── views/
        │       └── board/
        │           ├── list.jsp
        │           └── detail.jsp
        └── index.jsp
```

---

### 2.3.2 pom.xml — 웹 프로젝트 의존성

```xml
<packaging>war</packaging>

<dependencies>
    <!-- Spring MVC (spring-context, spring-web 포함) -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-webmvc</artifactId>
        <version>7.0.7</version>
    </dependency>

    <!-- Jakarta Servlet API (Tomcat이 제공하므로 scope=provided) -->
    <dependency>
        <groupId>jakarta.servlet</groupId>
        <artifactId>jakarta.servlet-api</artifactId>
        <version>6.1.0</version>
        <scope>provided</scope>
    </dependency>

    <!-- JSTL (JSP 태그 라이브러리) -->
    <dependency>
        <groupId>org.glassfish.web</groupId>
        <artifactId>jakarta.servlet.jsp.jstl</artifactId>
        <version>3.0.1</version>
    </dependency>
</dependencies>
```

`spring-webmvc` 하나를 추가하면 `spring-context`, `spring-web`, `spring-beans`, `spring-core`가 모두 포함된다. `jakarta.servlet-api`는 Tomcat이 이미 제공하므로 `scope=provided`로 지정해야 배포 시 충돌이 생기지 않는다.

Spring 7.x는 **Tomcat 10.x 이상**이 필요하다. Tomcat 9.x 이하는 `javax.*` 기반이므로 `jakarta.*`를 사용하는 Spring 7.x와 함께 쓸 수 없다.

---

### 2.3.3 web.xml 설정

`web.xml`은 서블릿 컨테이너(Tomcat)에게 "이 웹 애플리케이션을 어떻게 구성하는지"를 알려주는 배포 기술자(Deployment Descriptor)다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
             https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
         version="6.0">

    <!-- ① Root WebApplicationContext 생성 -->
    <context-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>classpath:applicationContext.xml</param-value>
    </context-param>
    <listener>
        <listener-class>
            org.springframework.web.context.ContextLoaderListener
        </listener-class>
    </listener>

    <!-- ② 한글 인코딩 필터 -->
    <filter>
        <filter-name>encodingFilter</filter-name>
        <filter-class>
            org.springframework.web.filter.CharacterEncodingFilter
        </filter-class>
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

    <!-- ③ DispatcherServlet 등록 -->
    <servlet>
        <servlet-name>dispatcher</servlet-name>
        <servlet-class>
            org.springframework.web.servlet.DispatcherServlet
        </servlet-class>
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>/WEB-INF/spring-mvc.xml</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>dispatcher</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>

</web-app>
```

각 설정의 역할을 자세히 이해하는 것이 중요하다.

**① ContextLoaderListener**

서블릿 컨테이너가 시작될 때 Root WebApplicationContext를 생성한다. `contextConfigLocation`으로 읽을 XML 파일을 지정한다. Service, DAO, DataSource처럼 웹과 무관한 Bean들이 여기에 등록된다.

**② CharacterEncodingFilter**

모든 요청(`/*`)에 대해 UTF-8 인코딩을 강제 적용한다. `forceEncoding=true`를 설정하지 않으면 이미 인코딩이 설정된 요청에서는 적용되지 않으므로 반드시 함께 설정한다. 이 필터가 없으면 한글 폼 데이터가 깨진다.

**③ DispatcherServlet**

모든 요청(`/`)을 받아 처리하는 프론트 컨트롤러다. `url-pattern`을 `/`로 지정하면 `.jsp` 파일을 제외한 모든 요청이 DispatcherServlet으로 들어온다. `load-on-startup=1`은 서버 시작 시 즉시 DispatcherServlet을 초기화하라는 의미다. 값이 클수록 늦게 초기화되고, 음수이면 첫 요청 시 초기화된다.

`url-pattern`으로 `*.do`를 사용하는 예제도 있다. 이 경우 `.do`로 끝나는 요청만 DispatcherServlet이 처리하며, 정적 리소스(css, js, 이미지)는 자동으로 통과된다. 하지만 `/`를 사용하는 방식이 RESTful URL 설계에 유리하고 더 일반적이다.

---

### 2.3.4 spring-mvc.xml 설정

DispatcherServlet이 사용하는 Servlet WebApplicationContext 설정 파일이다. Controller, ViewResolver, 정적 리소스 처리 등 웹 요청 처리에 필요한 Bean을 설정한다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:mvc="http://www.springframework.org/schema/mvc"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="
           http://www.springframework.org/schema/beans
               https://www.springframework.org/schema/beans/spring-beans.xsd
           http://www.springframework.org/schema/mvc
               https://www.springframework.org/schema/mvc/spring-mvc.xsd
           http://www.springframework.org/schema/context
               https://www.springframework.org/schema/context/spring-context.xsd">

    <!-- ① @Controller 등 어노테이션 Bean 자동 스캔 -->
    <context:component-scan base-package="com.spring"/>

    <!-- ② @RequestMapping 등 MVC 어노테이션 활성화 -->
    <mvc:annotation-driven/>

    <!-- ③ 정적 리소스 처리 (css, js, 이미지 등) -->
    <mvc:resources mapping="/resources/**" location="/resources/"/>

    <!-- ④ ViewResolver 설정 -->
    <bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="prefix" value="/WEB-INF/views/"/>
        <property name="suffix" value=".jsp"/>
    </bean>

</beans>
```

**① `<context:component-scan>`**

지정한 패키지(`com.spring`) 하위를 스캔하여 `@Controller`, `@Service`, `@Repository`, `@Component` 어노테이션이 붙은 클래스를 자동으로 Bean으로 등록한다. `applicationContext.xml`에도 같은 설정이 있을 경우 Bean이 중복 등록되므로, 각 컨텍스트에서 스캔할 패키지를 분리하거나 `exclude-filter`로 범위를 제한해야 한다.

**② `<mvc:annotation-driven/>`**

`RequestMappingHandlerMapping`과 `RequestMappingHandlerAdapter`를 자동으로 등록한다. 이 한 줄이 없으면 `@RequestMapping`이 동작하지 않는다. JSON 변환을 위한 `HttpMessageConverter` 설정도 함께 활성화된다.

**③ `<mvc:resources>`**

`url-pattern`을 `/`로 설정한 경우 DispatcherServlet이 CSS, JS, 이미지 요청도 가로채버린다. 이 설정으로 `/resources/**` 경로의 요청은 DispatcherServlet이 처리하지 않고 실제 파일을 직접 서빙하도록 위임한다.

**④ InternalResourceViewResolver**

앞서 설명한 ViewResolver다. prefix와 suffix 사이에 컨트롤러가 반환한 논리 뷰 이름을 끼워 실제 JSP 파일 경로를 만든다.

---

## 2.4 @Controller와 @RequestMapping

### 2.4.1 @Controller

`@Controller`는 해당 클래스가 스프링 MVC의 컨트롤러임을 나타내는 어노테이션이다. `@Component`의 특수화 버전으로, `<context:component-scan>`에 의해 자동으로 Bean으로 등록된다.

```java
package com.spring.board;

import org.springframework.stereotype.Controller;

@Controller
public class BoardController {
    // 이 클래스의 메서드들이 HTTP 요청을 처리한다
}
```

Model2의 컨트롤러와 달리 `HttpServlet`을 상속하지 않는다. 순수 자바 클래스(POJO)이며, 스프링이 요청 처리에 필요한 환경을 모두 주입해준다.

---

### 2.4.2 @RequestMapping

`@RequestMapping`은 URL과 컨트롤러 메서드를 연결하는 어노테이션이다. 클래스 레벨과 메서드 레벨에 모두 적용할 수 있다.

**메서드 레벨 단독 사용**

```java
@Controller
public class BoardController {

    @RequestMapping("/board/list")
    public String list(Model model) {
        // ...
        return "board/list";
    }

    @RequestMapping("/board/detail")
    public String detail(Model model) {
        // ...
        return "board/detail";
    }
}
```

**클래스 레벨 + 메서드 레벨 조합 (권장)**

관련 URL을 묶어서 관리할 수 있어 코드 구조가 명확해진다. 클래스 레벨의 경로와 메서드 레벨의 경로가 합쳐져서 최종 URL이 된다.

```java
@Controller
@RequestMapping("/board")     // 공통 경로
public class BoardController {

    @RequestMapping("/list")   // → 최종 URL: /board/list
    public String list(Model model) { ... }

    @RequestMapping("/detail") // → 최종 URL: /board/detail
    public String detail(Model model) { ... }

    @RequestMapping("/write")  // → 최종 URL: /board/write
    public String writeForm(Model model) { ... }
}
```

---

### 2.4.3 HTTP 메서드 구분

`@RequestMapping`의 `method` 속성으로 처리할 HTTP 메서드를 지정할 수 있다. 같은 URL이라도 GET과 POST를 다른 메서드로 처리할 수 있다.

```java
// GET /board/write → 등록 폼 화면
@RequestMapping(value = "/write", method = RequestMethod.GET)
public String writeForm() {
    return "board/writeForm";
}

// POST /board/write → 등록 처리
@RequestMapping(value = "/write", method = RequestMethod.POST)
public String write(Board board) {
    boardService.register(board);
    return "redirect:/board/list";
}
```

HTTP 메서드별 단축 어노테이션(`@GetMapping`, `@PostMapping` 등)을 사용하면 더 간결하게 표현할 수 있다.

```java
@GetMapping("/write")         // @RequestMapping(method = GET) 축약
public String writeForm() { ... }

@PostMapping("/write")        // @RequestMapping(method = POST) 축약
public String write(Board board) { ... }
```

---

### 2.4.4 컨트롤러 메서드의 파라미터

스프링 MVC는 컨트롤러 메서드의 파라미터 타입을 보고 자동으로 필요한 값을 만들어 전달한다. 개발자가 `request.getParameter()`나 `request.getSession()`을 직접 호출할 필요가 없다.

**Model — 뷰에 데이터를 전달**

```java
@GetMapping("/list")
public String list(Model model) {
    model.addAttribute("list", boardService.getList());
    // JSP에서 ${list}로 접근 가능
    return "board/list";
}
```

**@RequestParam — 쿼리 파라미터나 폼 파라미터 단건 바인딩**

```java
// GET /board/detail?no=5
@GetMapping("/detail")
public String detail(@RequestParam("no") int no, Model model) {
    model.addAttribute("board", boardService.getDetail(no));
    return "board/detail";
}
```

`required` 속성(기본값 `true`)으로 필수 여부를 지정하고, `defaultValue`로 기본값을 설정할 수 있다.

```java
// page 파라미터가 없으면 기본값 1 사용
@GetMapping("/list")
public String list(@RequestParam(value = "page", defaultValue = "1") int page,
                   Model model) { ... }
```

**커맨드 객체(Command Object) — 폼 데이터 일괄 바인딩**

HTTP 폼의 파라미터 이름과 객체의 필드명이 일치하면, 스프링이 자동으로 객체에 값을 채워준다. `@ModelAttribute`를 명시하거나 생략할 수 있다.

```java
// POST /board/write
// 폼 필드 title, content, writer가 Board 객체에 자동 바인딩됨
@PostMapping("/write")
public String write(Board board) {
    boardService.register(board);
    return "redirect:/board/list";
}
```

Model2에서 `request.getParameter("title")`, `request.getParameter("content")`를 일일이 호출하던 코드가 파라미터 하나로 대체된다.

**@PathVariable — URL 경로 변수**

```java
// GET /board/5
@GetMapping("/{no}")
public String detail(@PathVariable int no, Model model) {
    model.addAttribute("board", boardService.getDetail(no));
    return "board/detail";
}
```

**HttpServletRequest / HttpServletResponse — 직접 사용이 필요할 때**

스프링 MVC가 제공하는 파라미터로 해결이 안 되는 경우에만 사용한다. 가능하면 위의 추상화된 파라미터를 쓰는 것이 권장된다.

---

### 2.4.5 컨트롤러 메서드의 반환 타입

| 반환 타입 | 동작 |
|---|---|
| `String` | 논리 뷰 이름. ViewResolver를 통해 실제 뷰 파일로 변환 |
| `"redirect:URL"` | 해당 URL로 리다이렉트 (PRG 패턴에 사용) |
| `"forward:URL"` | 해당 URL로 포워드 |
| `ModelAndView` | 뷰 이름과 모델을 함께 담아서 반환 |
| `void` | 메서드 이름이나 요청 URL을 뷰 이름으로 사용 (드물게 사용) |

`"redirect:"`를 붙이면 ViewResolver를 거치지 않고 브라우저에 302 응답을 보내 지정한 URL로 재요청을 유도한다. POST 요청 처리 후 `"redirect:"`를 사용하는 것은 **PRG(Post-Redirect-Get) 패턴**으로, 브라우저 새로고침 시 폼이 중복 제출되는 문제를 방지한다.

---

### 2.4.6 서비스 Bean 주입

컨트롤러는 POJO이므로 스프링의 DI를 그대로 활용할 수 있다. 1일차에서 학습한 생성자 주입 방식을 그대로 적용한다.

```java
@Controller
@RequestMapping("/board")
public class BoardController {

    private final BoardService boardService;

    // 생성자 주입 — @Autowired 생략 가능 (생성자가 하나일 때)
    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("list", boardService.getList());
        return "board/list";
    }
}
```

`boardService`는 Root WebApplicationContext에 등록된 Bean이고, `BoardController`는 Servlet WebApplicationContext에 등록된 Bean이다. 자식 컨텍스트가 부모 컨텍스트의 Bean을 주입받는 정상적인 방향이므로 문제없이 동작한다.

---

## 2.5 실습 — 게시판 목록·상세 페이지 요청 흐름 구성

### 실습 목표

웹 프로젝트를 구성하고, 게시판 목록과 상세 페이지를 스프링 MVC로 연결한다. 요청이 DispatcherServlet → HandlerMapping → Controller → ViewResolver → JSP 순서로 흐르는 과정을 직접 확인한다.

---

### 실습 환경

```
spring-day2/
├── pom.xml
└── src/main/
    ├── java/com/spring/board/
    │   ├── Board.java
    │   ├── BoardDao.java
    │   ├── BoardDaoImpl.java        ← 1일차 메모리 구현체 재사용
    │   ├── BoardService.java
    │   ├── BoardServiceImpl.java
    │   └── BoardController.java
    ├── resources/
    │   └── applicationContext.xml   ← Service, DAO Bean
    └── webapp/WEB-INF/
        ├── web.xml
        ├── spring-mvc.xml
        └── views/board/
            ├── list.jsp
            └── detail.jsp
```

---

### 실습 순서

**Step 1 — 프로젝트 생성 및 pom.xml 설정**

Maven WAR 프로젝트를 생성하고 `spring-webmvc`, `jakarta.servlet-api`, JSTL 의존성을 추가한다. Tomcat 10.x를 런타임으로 설정한다.

**Step 2 — web.xml 작성**

`ContextLoaderListener`로 Root 컨텍스트를 구성하고, `DispatcherServlet`을 `/` 패턴으로 등록한다. `CharacterEncodingFilter`도 함께 설정한다.

**Step 3 — applicationContext.xml 작성**

Service Bean(`BoardServiceImpl`)과 DAO Bean(`BoardDaoImpl`)을 등록하고 생성자 주입으로 연결한다. Controller는 이 파일에 등록하지 않는다.

**Step 4 — spring-mvc.xml 작성**

`<mvc:annotation-driven/>`, `<context:component-scan/>`, `InternalResourceViewResolver`를 설정한다. component-scan의 대상에서 Service·DAO를 제외하거나, 별도 패키지로 분리하여 중복 스캔을 방지한다.

**Step 5 — BoardController 작성**

`/board/list` — 목록 조회, Model에 리스트 담아 뷰에 전달  
`/board/detail` — `@RequestParam`으로 `no` 파라미터 받아 상세 조회

**Step 6 — JSP 작성**

`list.jsp`에서 `${list}`를 JSTL `<c:forEach>`로 순회하며 출력한다. 각 게시글 제목에 `/board/detail?no=${board.no}` 링크를 건다.

**Step 7 — 서버 실행 및 흐름 확인**

브라우저에서 `/board/list` 요청 후 콘솔 로그와 화면 출력을 확인한다.

`System.out.println`을 Controller → Service → DAO 각 계층에 추가하면 요청이 어떤 순서로 각 객체를 거치는지 눈으로 확인할 수 있다.

**Step 8 — URL 패턴 변경 실험**

`@RequestMapping`의 경로를 바꿔보고, 클래스 레벨과 메서드 레벨 조합 방식으로 변경해본다. `@GetMapping` 단축 어노테이션도 적용해본다.

---

### 요청 흐름 추적 (디버깅 포인트)

실습 중 각 단계에서 다음을 확인한다.

| 단계 | 확인 항목 |
|---|---|
| Tomcat 시작 로그 | `ContextLoaderListener`와 `DispatcherServlet` 초기화 메시지 |
| 컨트롤러 진입 | 콘솔에 추가한 println 출력 여부 |
| Model 데이터 | JSP에서 `${list.size()}`로 데이터 개수 출력 |
| ViewResolver 동작 | 잘못된 뷰 이름 반환 시 404 오류 메시지 확인 |
| redirect 동작 | 브라우저 URL 변경 여부, Network 탭의 302 응답 확인 |

---

## 정리

**DispatcherServlet**은 Model2의 FrontController를 프레임워크 차원에서 대체하는 스프링 MVC의 핵심 서블릿이다. 개발자가 직접 작성하지 않으며, `web.xml`에 등록하는 것만으로 동작한다.

요청 처리 흐름은 DispatcherServlet → **HandlerMapping**(URL→Handler 조회) → **HandlerAdapter**(컨트롤러 실행) → **ViewResolver**(논리 뷰 이름→파일 경로 변환) → View(렌더링) 순서다.

스프링 MVC 웹 애플리케이션에는 **Root WebApplicationContext**(Service·DAO)와 **Servlet WebApplicationContext**(Controller·MVC 설정)가 부모-자식 관계로 공존한다.

`@Controller`와 `@RequestMapping`으로 컨트롤러를 선언하고, 메서드 파라미터(`Model`, `@RequestParam`, 커맨드 객체 등)를 활용하면 `HttpServletRequest`를 직접 다루지 않고도 요청 데이터를 처리할 수 있다.

POST 요청 처리 후에는 `"redirect:"`를 사용하는 **PRG 패턴**으로 폼 중복 제출을 방지한다.

---

## 다음 시간 예고

3일차에서는 DataSource와 MyBatis-Spring 연동 설정을 학습하고, 게시판 CRUD를 실제 데이터베이스와 연결한다. SQL을 XML 매퍼 파일에 분리하여 관리하는 MyBatis 방식과, 스프링의 트랜잭션 관리와의 연동 기초를 살펴본다.
