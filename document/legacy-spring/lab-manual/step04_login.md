# 단계 4. 로그인 · 로그아웃 구현

> **목표** : 아이디/비밀번호로 로그인하고 세션에 회원 정보를 저장합니다. 로그인하지 않은 사용자가 맛집 기능에 접근하면 자동으로 로그인 페이지로 이동하도록 인터셉터를 적용합니다.
> **소요 시간** : 약 1.5시간

---

## 4.1 전체 흐름 이해

### 로그인 흐름

```
브라우저
  ↓ GET /member/login  (로그인 폼 요청)
MemberController.loginForm()
  ↓ login.jsp 반환
브라우저 (아이디/비밀번호 입력 후 제출)
  ↓ POST /member/login
MemberController.login()
  ↓ memberService.login(loginId, password) 호출
MemberServiceImpl.login()
  ↓ memberMapper.selectByLoginId(loginId) → DB 조회
  ↓ 비밀번호 일치 여부 확인
MemberController
  ↓ 성공: 세션에 member 저장 → 맛집 목록으로 이동
  ↓ 실패: 오류 메시지 → 로그인 폼으로 돌아가기
```

### 인터셉터 흐름

```
브라우저 → GET /restaurant/list  (로그인 없이 접근 시도)
  ↓
LoginCheckInterceptor.preHandle()
  ↓ 세션에 loginMember 가 없음 → false 반환
  ↓ /member/login 으로 리다이렉트
브라우저 → 로그인 페이지 표시
```

---

## 4.2 작성 순서

```
① MemberMapper.java    (selectByLoginId 추가)
② MemberMapper.xml     (selectByLoginId SQL 추가)
③ MemberService.java   (login 메서드 추가)
④ MemberServiceImpl.java (login 구현 추가)
⑤ MemberController.java (login, logout 추가)
⑥ login.jsp
⑦ LoginCheckInterceptor.java
⑧ spring-mvc.xml       (인터셉터 등록)
```

---

## 4.3 MemberMapper 수정 — selectByLoginId 추가

`MemberMapper.java` 에 로그인 아이디로 회원을 조회하는 메서드를 추가합니다.

```java
package com.food.mapper;

import com.food.domain.Member;
import org.apache.ibatis.annotations.Param;

public interface MemberMapper {

    int existsLoginId(@Param("loginId") String loginId);

    void insert(Member member);

    // ↓ 아래 메서드 추가
    /**
     * 로그인 아이디로 회원 정보 조회
     * @return 해당 아이디의 회원, 없으면 null
     */
    Member selectByLoginId(@Param("loginId") String loginId);

}
```

---

## 4.4 MemberMapper.xml 수정 — selectByLoginId SQL 추가

`MemberMapper.xml` 에 아래 `<select>` 를 추가합니다.

```xml
<!-- 로그인 아이디로 회원 조회 -->
<select id="selectByLoginId" resultType="Member">
    SELECT id, login_id, password, name, reg_date
      FROM member
     WHERE login_id = #{loginId}
</select>
```

추가 후 전체 파일 내용은 다음과 같습니다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
    PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.food.mapper.MemberMapper">

    <select id="existsLoginId" resultType="int">
        SELECT COUNT(*)
          FROM member
         WHERE login_id = #{loginId}
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO member (login_id, password, name)
        VALUES (#{loginId}, #{password}, #{name})
    </insert>

    <!-- 추가된 SQL -->
    <select id="selectByLoginId" resultType="Member">
        SELECT id, login_id, password, name, reg_date
          FROM member
         WHERE login_id = #{loginId}
    </select>

</mapper>
```

---

## 4.5 MemberService 수정 — login 메서드 추가

`MemberService.java` 에 로그인 메서드를 추가합니다.

```java
package com.food.service;

import com.food.domain.Member;

public interface MemberService {

    boolean register(Member member);

    // ↓ 아래 메서드 추가
    /**
     * 로그인
     * @return 로그인 성공 시 Member 객체, 실패 시 null
     */
    Member login(String loginId, String password);

}
```

---

## 4.6 MemberServiceImpl 수정 — login 구현 추가

`MemberServiceImpl.java` 에 `login()` 메서드를 추가합니다.

```java
package com.food.service.impl;

import com.food.domain.Member;
import com.food.mapper.MemberMapper;
import com.food.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberServiceImpl implements MemberService {

    @Autowired
    private MemberMapper memberMapper;

    @Override
    @Transactional
    public boolean register(Member member) {
        int count = memberMapper.existsLoginId(member.getLoginId());
        if (count > 0) {
            return false;
        }
        memberMapper.insert(member);
        return true;
    }

    // ↓ 아래 메서드 추가
    @Override
    @Transactional(readOnly = true)
    public Member login(String loginId, String password) {

        // 1. 아이디로 회원 조회
        Member member = memberMapper.selectByLoginId(loginId);

        // 2. 회원이 없거나 비밀번호가 다르면 null 반환
        if (member == null || !member.getPassword().equals(password)) {
            return null;
        }

        return member;  // 로그인 성공
    }

}
```

> `@Transactional(readOnly = true)` : 데이터를 읽기만 하고 변경하지 않는 메서드에 사용합니다. 성능 최적화에 도움이 됩니다.

---

## 4.7 MemberController 수정 — login, logout 추가

`MemberController.java` 에 로그인/로그아웃 메서드를 추가합니다.

```java
package com.food.controller;

import com.food.domain.Member;
import com.food.service.MemberService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/member")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @GetMapping("/register")
    public String registerForm() {
        return "member/register";
    }

    @PostMapping("/register")
    public String register(Member member, Model model) {
        boolean success = memberService.register(member);
        if (!success) {
            model.addAttribute("errorMsg", "이미 사용 중인 아이디입니다.");
            return "member/register";
        }
        return "redirect:/member/login";
    }

    // ↓ 아래 메서드 추가

    // 로그인 폼 표시
    @GetMapping("/login")
    public String loginForm() {
        return "member/login";
    }

    // 로그인 처리
    @PostMapping("/login")
    public String login(@RequestParam String loginId,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {

        Member member = memberService.login(loginId, password);

        if (member == null) {
            // 로그인 실패
            model.addAttribute("errorMsg", "아이디 또는 비밀번호가 올바르지 않습니다.");
            return "member/login";
        }

        // 로그인 성공 → 세션에 회원 정보 저장
        session.setAttribute("loginMember", member);
        return "redirect:/restaurant/list";
    }

    // 로그아웃 처리
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();  // 세션 전체 삭제
        return "redirect:/member/login";
    }

}
```

> **세션이란?**
>
> 세션(Session)은 서버가 사용자별로 데이터를 기억하는 저장 공간입니다. 로그인 성공 후 `session.setAttribute("loginMember", member)` 로 회원 정보를 저장하면, 이후 모든 요청에서 `session.getAttribute("loginMember")` 로 꺼내서 "지금 로그인한 사람이 누구인지" 확인할 수 있습니다.

---

## 4.8 login.jsp 작성

`WEB-INF/views/member/` 폴더 안에 `login.jsp` 파일을 만듭니다.

```jsp
<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>로그인 — 나만의 맛집 노트</title>
</head>
<body>

<h2>로그인</h2>

<!-- 로그인 실패 오류 메시지 -->
<c:if test="${not empty errorMsg}">
    <p style="color: red;">${errorMsg}</p>
</c:if>

<form method="post" action="/member/login">

    <table>
        <tr>
            <td>아이디</td>
            <td><input type="text" name="loginId" required></td>
        </tr>
        <tr>
            <td>비밀번호</td>
            <td><input type="password" name="password" required></td>
        </tr>
        <tr>
            <td colspan="2">
                <button type="submit">로그인</button>
            </td>
        </tr>
    </table>

</form>

<p><a href="/member/register">계정이 없으신가요? 회원가입</a></p>

</body>
</html>
```

---

## 4.9 LoginCheckInterceptor 작성

로그인하지 않은 사용자가 맛집 기능에 접근하면 로그인 페이지로 이동시키는 인터셉터를 만듭니다.

`com.food.interceptor` 패키지 안에 `LoginCheckInterceptor.java` 파일을 만듭니다.

```java
package com.food.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // 세션에서 로그인 정보 확인
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("loginMember") == null) {
            // 로그인 정보가 없으면 로그인 페이지로 이동
            response.sendRedirect("/member/login");
            return false;  // 요청 처리 중단
        }

        return true;  // 로그인 상태 → 요청 처리 계속
    }

}
```

> **preHandle 반환값의 의미**
>
> - `return true` : 인터셉터 통과. Controller로 요청이 전달됩니다.
> - `return false` : 요청 처리 중단. Controller가 실행되지 않습니다.

---

## 4.10 spring-mvc.xml 에 인터셉터 등록

`spring-mvc.xml` 을 열고 `</beans>` 태그 바로 앞에 아래 내용을 추가합니다.

```xml
<!-- 로그인 체크 인터셉터 등록 -->
<mvc:interceptors>
    <mvc:interceptor>
        <mvc:interceptor>
        <!-- 인터셉터를 적용할 경로 -->
        <mvc:mapping path="/restaurant/**"/>
        <!-- 인터셉터 클래스 지정 -->
        <bean class="com.food.interceptor.LoginCheckInterceptor"/>
    </mvc:interceptor>
</mvc:interceptors>
```

추가 후 `spring-mvc.xml` 전체 내용은 다음과 같습니다.

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

    <context:component-scan base-package="com.food.controller"/>

    <mvc:annotation-driven/>
    <mvc:default-servlet-handler/>

    <bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="prefix" value="/WEB-INF/views/"/>
        <property name="suffix" value=".jsp"/>
    </bean>

    <!-- 인터셉터 등록 -->
    <mvc:interceptors>
        <mvc:interceptor>
            <mvc:mapping path="/restaurant/**"/>
            <bean class="com.food.interceptor.LoginCheckInterceptor"/>
        </mvc:interceptor>
    </mvc:interceptors>

</beans>
```

> **`/restaurant/**` 의 의미**
>
> `/restaurant/` 로 시작하는 모든 경로에 인터셉터를 적용합니다.  
> `/member/login`, `/member/register` 는 적용 대상이 아니므로 로그인 없이 접근할 수 있습니다.

---

## 4.11 동작 확인

### 4.11.1 서버 재시작

서버를 재시작하고 콘솔에 오류가 없는지 확인합니다.

### 4.11.2 로그인 테스트

1. 브라우저에서 `http://localhost:8080/food-note/member/login` 에 접속합니다.
2. 단계 2에서 추가한 테스트 아이디(`test` / `1234`)로 로그인합니다.
3. `/restaurant/list` 로 이동하면 성공입니다. (아직 404 — 정상)

### 4.11.3 잘못된 비밀번호 테스트

틀린 비밀번호를 입력했을 때 "아이디 또는 비밀번호가 올바르지 않습니다." 메시지가 표시되면 성공입니다.

### 4.11.4 인터셉터 테스트

로그인 없이 `http://localhost:8080/food-note/restaurant/list` 에 직접 접속합니다.  
자동으로 로그인 페이지(`/member/login`)로 이동하면 인터셉터가 정상 동작하는 것입니다.

### 4.11.5 로그아웃 테스트

로그인 후 `http://localhost:8080/food-note/member/logout` 에 접속합니다.  
로그인 페이지로 이동하면 성공입니다.

---

## 자주 발생하는 오류

| 오류 메시지 | 원인 | 해결 방법 |
|---|---|---|
| 인터셉터 적용 후 무한 리다이렉트 | `/member/**` 경로도 인터셉터에 포함됨 | `<mvc:mapping>` 이 `/restaurant/**` 인지 확인합니다 |
| 로그인 후에도 로그인 페이지로 돌아옴 | 세션 저장 코드 누락 | `session.setAttribute("loginMember", member)` 가 있는지 확인합니다 |
| `session.getAttribute` 가 null | `getSession(false)` 가 null 반환 | 로그인 후 세션이 생성됐는지 확인합니다 |

---

## ✅ 단계 4 완료 체크리스트

- [ ] `MemberMapper.java` 에 `selectByLoginId()` 가 추가됐습니다.
- [ ] `MemberMapper.xml` 에 `selectByLoginId` SQL 이 추가됐습니다.
- [ ] `MemberService.java` 에 `login()` 이 추가됐습니다.
- [ ] `MemberServiceImpl.java` 에 `login()` 구현이 추가됐습니다.
- [ ] `MemberController.java` 에 `loginForm()`, `login()`, `logout()` 이 추가됐습니다.
- [ ] `WEB-INF/views/member/login.jsp` 가 작성됐습니다.
- [ ] `LoginCheckInterceptor.java` 가 작성됐습니다.
- [ ] `spring-mvc.xml` 에 인터셉터가 등록됐습니다.
- [ ] 올바른 아이디/비밀번호로 로그인하면 맛집 목록 경로로 이동합니다.
- [ ] 틀린 비밀번호 입력 시 오류 메시지가 표시됩니다.
- [ ] 로그인 없이 `/restaurant/list` 에 접근하면 로그인 페이지로 이동합니다.
- [ ] 로그아웃 후 로그인 페이지로 이동합니다.

모든 항목이 체크됐으면 **단계 5. 맛집 목록 조회 구현**으로 이동합니다.
