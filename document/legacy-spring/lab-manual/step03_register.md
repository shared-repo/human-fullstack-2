# 단계 3. 회원가입 구현

> **목표** : 아이디/비밀번호/이름을 입력하면 DB에 저장되는 회원가입 기능을 구현합니다.
> **소요 시간** : 약 1.5시간

---

## 3.1 전체 흐름 이해

회원가입 요청이 처리되는 순서는 다음과 같습니다.

```
브라우저
  ↓ GET /member/register  (가입 폼 요청)
MemberController.registerForm()
  ↓ register.jsp 반환
브라우저 (폼 입력 후 제출)
  ↓ POST /member/register  (가입 데이터 전송)
MemberController.register()
  ↓ memberService.register(member) 호출
MemberServiceImpl.register()
  ↓ memberMapper.existsLoginId() → 아이디 중복 확인
  ↓ memberMapper.insert()        → DB 저장
MemberController
  ↓ 로그인 페이지로 리다이렉트
브라우저 → /member/login
```

---

## 3.2 작성 순서

아래 순서대로 파일을 작성합니다. 순서를 지키면 오류 없이 진행할 수 있습니다.

```
① MemberMapper.java     (인터페이스)
② MemberMapper.xml      (SQL)
③ MemberService.java    (인터페이스)
④ MemberServiceImpl.java
⑤ MemberController.java
⑥ register.jsp
```

---

## 3.3 MemberMapper 인터페이스 작성

`com.food.mapper` 패키지 안에 `MemberMapper.java` 파일을 만듭니다.

```java
package com.food.mapper;

import com.food.domain.Member;
import org.apache.ibatis.annotations.Param;

public interface MemberMapper {

    /**
     * 로그인 아이디 중복 여부 확인
     * @return 이미 존재하면 1, 없으면 0
     */
    int existsLoginId(@Param("loginId") String loginId);

    /**
     * 회원 정보를 DB에 저장
     */
    void insert(Member member);

}
```

---

## 3.4 MemberMapper.xml 작성

`src/main/resources/mappers/` 폴더 안에 `MemberMapper.xml` 파일을 만듭니다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
    PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<!-- namespace 는 반드시 Mapper 인터페이스의 전체 경로와 일치해야 합니다 -->
<mapper namespace="com.food.mapper.MemberMapper">

    <!-- 아이디 중복 확인: 존재하면 1, 없으면 0 반환 -->
    <select id="existsLoginId" resultType="int">
        SELECT COUNT(*)
          FROM member
         WHERE login_id = #{loginId}
    </select>

    <!-- 회원 정보 저장 -->
    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO member (login_id, password, name)
        VALUES (#{loginId}, #{password}, #{name})
    </insert>

</mapper>
```

> **주요 속성 설명**
>
> - `namespace` : 이 XML이 어떤 Mapper 인터페이스와 연결되는지 지정합니다. 오타가 있으면 오류가 발생하므로 정확히 입력합니다.
> - `id` : Mapper 인터페이스의 메서드 이름과 반드시 일치해야 합니다.
> - `useGeneratedKeys="true"` : INSERT 후 DB가 자동 생성한 PK 값을 가져옵니다.
> - `keyProperty="id"` : 가져온 PK 값을 `member.id` 필드에 저장합니다.

---

## 3.5 MemberService 인터페이스 작성

`com.food.service` 패키지 안에 `MemberService.java` 파일을 만듭니다.

```java
package com.food.service;

import com.food.domain.Member;

public interface MemberService {

    /**
     * 회원가입
     * @return true: 가입 성공 / false: 아이디 중복으로 가입 실패
     */
    boolean register(Member member);

}
```

---

## 3.6 MemberServiceImpl 작성

`com.food.service.impl` 패키지 안에 `MemberServiceImpl.java` 파일을 만듭니다.

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

        // 1. 아이디 중복 확인
        int count = memberMapper.existsLoginId(member.getLoginId());
        if (count > 0) {
            return false;  // 중복 아이디 → 가입 실패
        }

        // 2. DB에 저장
        memberMapper.insert(member);
        return true;  // 가입 성공
    }

}
```

> **코드 설명**
>
> - `@Service` : 이 클래스가 서비스 레이어 Bean임을 스프링에 알립니다. 스프링이 자동으로 객체를 만들고 관리합니다.
> - `@Autowired` : 스프링이 `MemberMapper` 객체를 자동으로 주입합니다. 직접 `new` 로 만들지 않아도 됩니다.
> - `@Transactional` : 이 메서드가 실행되는 동안 DB 작업을 하나의 트랜잭션으로 묶습니다. 중간에 오류가 발생하면 자동으로 롤백됩니다.

---

## 3.7 MemberController 작성

`com.food.controller` 패키지 안에 `MemberController.java` 파일을 만듭니다.

```java
package com.food.controller;

import com.food.domain.Member;
import com.food.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/member")
public class MemberController {

    @Autowired
    private MemberService memberService;

    // 회원가입 폼 페이지 표시
    @GetMapping("/register")
    public String registerForm() {
        return "member/register";  // /WEB-INF/views/member/register.jsp
    }

    // 회원가입 처리
    @PostMapping("/register")
    public String register(Member member, Model model) {

        boolean success = memberService.register(member);

        if (!success) {
            // 아이디 중복 → 오류 메시지와 함께 폼으로 돌아가기
            model.addAttribute("errorMsg", "이미 사용 중인 아이디입니다.");
            return "member/register";
        }

        // 가입 성공 → 로그인 페이지로 이동
        return "redirect:/member/login";
    }

}
```

> **코드 설명**
>
> - `@Controller` : 이 클래스가 Controller임을 스프링에 알립니다.
> - `@RequestMapping("/member")` : 이 Controller의 모든 요청 경로 앞에 `/member` 가 붙습니다.
> - `@GetMapping("/register")` : `GET /member/register` 요청을 이 메서드가 처리합니다.
> - `@PostMapping("/register")` : `POST /member/register` 요청을 이 메서드가 처리합니다.
> - `Member member` : 폼에서 전송된 데이터를 스프링이 자동으로 Member 객체에 담아줍니다.
> - `return "redirect:/member/login"` : 브라우저를 `/member/login` 으로 이동시킵니다.

---

## 3.8 JSP 파일 작성

`src/main/webapp/WEB-INF/views/` 아래에 `member` 폴더를 먼저 만들고, 그 안에 JSP 파일을 작성합니다.

### 3.8.1 views/member 폴더 생성

1. `WEB-INF/views` 를 오른쪽 클릭합니다.
2. **New → Folder** 를 선택합니다.
3. 폴더 이름에 `member` 를 입력하고 **Finish** 를 클릭합니다.

### 3.8.2 register.jsp 작성

`WEB-INF/views/member/` 폴더 안에 `register.jsp` 파일을 만듭니다.

```jsp
<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>회원가입 — 나만의 맛집 노트</title>
</head>
<body>

<h2>회원가입</h2>

<!-- 아이디 중복 오류 메시지 -->
<c:if test="${not empty errorMsg}">
    <p style="color: red;">${errorMsg}</p>
</c:if>

<form method="post" action="/member/register">

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
            <td>이름</td>
            <td><input type="text" name="name" required></td>
        </tr>
        <tr>
            <td colspan="2">
                <button type="submit">가입하기</button>
            </td>
        </tr>
    </table>

</form>

<p><a href="/member/login">이미 계정이 있으신가요? 로그인</a></p>

</body>
</html>
```

> **폼 input name 속성 주의**
>
> `<input name="loginId">` 의 `name` 값이 `Member` 클래스의 필드명과 정확히 일치해야 스프링이 자동으로 데이터를 담아줍니다.
>
> | input name | Member 필드 |
> |---|---|
> | `loginId` | `private String loginId` |
> | `password` | `private String password` |
> | `name` | `private String name` |

---

## 3.9 동작 확인

### 3.9.1 서버 재시작

서버를 재시작하고 콘솔에 오류가 없는지 확인합니다.

### 3.9.2 회원가입 페이지 접속

브라우저에서 아래 주소로 접속합니다.

```
http://localhost:8080/food-note/member/register
```

회원가입 폼이 표시되면 성공입니다.

### 3.9.3 회원가입 테스트

1. 아이디, 비밀번호, 이름을 입력하고 **가입하기** 버튼을 클릭합니다.
2. 로그인 페이지(`/member/login`)로 이동하면 가입이 성공한 것입니다.

   > 아직 로그인 페이지가 없어서 404 오류가 표시됩니다. 정상입니다.

3. DB에서 데이터가 저장됐는지 확인합니다.

   ```sql
   SELECT * FROM member;
   ```

   방금 입력한 데이터가 보이면 성공입니다.

### 3.9.4 중복 아이디 테스트

같은 아이디로 다시 가입을 시도합니다.  
"이미 사용 중인 아이디입니다." 메시지가 표시되면 성공입니다.

---

## 자주 발생하는 오류

| 오류 메시지 | 원인 | 해결 방법 |
|---|---|---|
| `Invalid bound statement` | Mapper XML의 `namespace` 또는 `id` 오타 | XML의 namespace와 id를 인터페이스와 정확히 일치시킵니다 |
| `Could not find resource` | Mapper XML 파일 경로가 잘못됨 | `mappers/` 폴더 안에 파일이 있는지 확인합니다 |
| `404 Not Found` | Controller 경로 오타 또는 서버 미재시작 | 경로 확인 후 서버를 재시작합니다 |
| 한글 깨짐 | 인코딩 설정 누락 | `web.xml` 의 `CharacterEncodingFilter` 가 있는지 확인합니다 |

---

## ✅ 단계 3 완료 체크리스트

- [ ] `MemberMapper.java` 인터페이스가 작성됐습니다.
- [ ] `MemberMapper.xml` 이 `src/main/resources/mappers/` 에 작성됐습니다.
- [ ] `MemberService.java` 인터페이스가 작성됐습니다.
- [ ] `MemberServiceImpl.java` 가 작성됐습니다.
- [ ] `MemberController.java` 가 작성됐습니다.
- [ ] `WEB-INF/views/member/register.jsp` 가 작성됐습니다.
- [ ] 브라우저에서 `/member/register` 에 접속했을 때 폼이 표시됩니다.
- [ ] 가입 후 DB `member` 테이블에 데이터가 저장됐습니다.
- [ ] 중복 아이디 입력 시 오류 메시지가 표시됩니다.

모든 항목이 체크됐으면 **단계 4. 로그인 · 로그아웃 구현**으로 이동합니다.
