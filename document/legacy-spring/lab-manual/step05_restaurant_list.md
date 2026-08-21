# 단계 5. 맛집 목록 조회 구현

> **목표** : 로그인한 회원이 자신이 등록한 맛집 목록을 확인할 수 있는 페이지를 구현합니다.
> **소요 시간** : 약 2시간

---

## 5.1 전체 흐름 이해

```
브라우저
  ↓ GET /restaurant/list
LoginCheckInterceptor.preHandle()
  ↓ 로그인 확인 통과
RestaurantController.list()
  ↓ 세션에서 로그인 회원 id 꺼냄
  ↓ restaurantService.getList(memberId) 호출
RestaurantServiceImpl.getList()
  ↓ restaurantMapper.selectAll(memberId) 호출
RestaurantMapper → DB 조회
  ↓ List<Restaurant> 반환
RestaurantController
  ↓ model 에 list 담기
  ↓ "restaurant/list" 반환
list.jsp
  ↓ 맛집 목록 출력
브라우저
```

---

## 5.2 작성 순서

```
① RestaurantMapper.java    (인터페이스)
② RestaurantMapper.xml     (SQL)
③ RestaurantService.java   (인터페이스)
④ RestaurantServiceImpl.java
⑤ RestaurantController.java
⑥ list.jsp
```

---

## 5.3 RestaurantMapper 인터페이스 작성

`com.food.mapper` 패키지 안에 `RestaurantMapper.java` 파일을 만듭니다.

```java
package com.food.mapper;

import com.food.domain.Restaurant;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface RestaurantMapper {

    /**
     * 특정 회원이 등록한 맛집 목록 조회
     */
    List<Restaurant> selectAll(@Param("memberId") int memberId);

}
```

---

## 5.4 RestaurantMapper.xml 작성

`src/main/resources/mappers/` 폴더 안에 `RestaurantMapper.xml` 파일을 만듭니다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
    PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.food.mapper.RestaurantMapper">

    <!-- 특정 회원의 맛집 목록 조회 (최근 등록 순) -->
    <select id="selectAll" resultType="Restaurant">
        SELECT no, member_id, name, category, address, memo, visit_date, reg_date
          FROM restaurant
         WHERE member_id = #{memberId}
         ORDER BY no DESC
    </select>

</mapper>
```

> `ORDER BY no DESC` : 가장 최근에 등록한 맛집이 목록 맨 위에 표시됩니다.

---

## 5.5 RestaurantService 인터페이스 작성

`com.food.service` 패키지 안에 `RestaurantService.java` 파일을 만듭니다.

```java
package com.food.service;

import com.food.domain.Restaurant;
import java.util.List;

public interface RestaurantService {

    /**
     * 로그인한 회원의 맛집 목록 조회
     */
    List<Restaurant> getList(int memberId);

}
```

---

## 5.6 RestaurantServiceImpl 작성

`com.food.service.impl` 패키지 안에 `RestaurantServiceImpl.java` 파일을 만듭니다.

```java
package com.food.service.impl;

import com.food.domain.Restaurant;
import com.food.mapper.RestaurantMapper;
import com.food.service.RestaurantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class RestaurantServiceImpl implements RestaurantService {

    @Autowired
    private RestaurantMapper restaurantMapper;

    @Override
    @Transactional(readOnly = true)
    public List<Restaurant> getList(int memberId) {
        return restaurantMapper.selectAll(memberId);
    }

}
```

---

## 5.7 RestaurantController 작성

`com.food.controller` 패키지 안에 `RestaurantController.java` 파일을 만듭니다.

```java
package com.food.controller;

import com.food.domain.Member;
import com.food.domain.Restaurant;
import com.food.service.RestaurantService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;

@Controller
@RequestMapping("/restaurant")
public class RestaurantController {

    @Autowired
    private RestaurantService restaurantService;

    // 맛집 목록 조회
    @GetMapping("/list")
    public String list(HttpSession session, Model model) {

        // 세션에서 로그인한 회원 정보를 꺼냄
        Member loginMember = (Member) session.getAttribute("loginMember");

        // 로그인 회원의 맛집 목록 조회
        List<Restaurant> list = restaurantService.getList(loginMember.getId());

        // 조회 결과를 model 에 담아 JSP 로 전달
        model.addAttribute("list", list);
        model.addAttribute("loginMember", loginMember);

        return "restaurant/list";   // /WEB-INF/views/restaurant/list.jsp
    }

}
```

---

## 5.8 JSP 파일 작성

### 5.8.1 views/restaurant 폴더 생성

1. `WEB-INF/views` 를 오른쪽 클릭합니다.
2. **New → Folder** 를 선택합니다.
3. 폴더 이름에 `restaurant` 를 입력하고 **Finish** 를 클릭합니다.

### 5.8.2 list.jsp 작성

`WEB-INF/views/restaurant/` 폴더 안에 `list.jsp` 파일을 만듭니다.

```jsp
<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>맛집 목록 — 나만의 맛집 노트</title>
</head>
<body>

<!-- 상단 네비게이션 -->
<div>
    <strong>${loginMember.name}</strong>님 환영합니다.
    <a href="/restaurant/write">맛집 등록</a>
    <a href="/member/logout">로그아웃</a>
</div>

<hr>

<h2>나의 맛집 목록</h2>

<!-- 목록이 없을 때 -->
<c:if test="${empty list}">
    <p>등록된 맛집이 없습니다. <a href="/restaurant/write">맛집을 등록해 보세요!</a></p>
</c:if>

<!-- 목록이 있을 때 -->
<c:if test="${not empty list}">
    <table border="1">
        <thead>
            <tr>
                <th>번호</th>
                <th>가게 이름</th>
                <th>카테고리</th>
                <th>방문일</th>
                <th>등록일</th>
                <th>관리</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="r" items="${list}">
            <tr>
                <td>${r.no}</td>
                <td>
                    <a href="/restaurant/detail?no=${r.no}">${r.name}</a>
                </td>
                <td>${r.category}</td>
                <td>${r.visitDate}</td>
                <td><fmt:formatDate value="${r.regDate}" pattern="yyyy-MM-dd"/></td>
                <td>
                    <a href="/restaurant/edit?no=${r.no}">수정</a>
                    <a href="/restaurant/delete?no=${r.no}"
                       onclick="return confirm('삭제하시겠습니까?')">삭제</a>
                </td>
            </tr>
            </c:forEach>
        </tbody>
    </table>
</c:if>

</body>
</html>
```

> **JSTL 태그 설명**
>
> - `<c:if test="${empty list}">` : list 가 비어 있을 때만 출력합니다.
> - `<c:forEach var="r" items="${list}">` : list 의 각 항목을 `r` 이라는 이름으로 반복합니다.
> - `${r.name}` : Restaurant 객체의 `getName()` 을 호출한 결과를 출력합니다.
> - `<fmt:formatDate>` : LocalDateTime 을 지정한 형식(`yyyy-MM-dd`)으로 변환합니다.

---

## 5.9 동작 확인

### 5.9.1 서버 재시작 후 로그인

브라우저에서 로그인 페이지에 접속하여 로그인합니다.

### 5.9.2 목록 페이지 확인

로그인 후 자동으로 `/restaurant/list` 로 이동합니다.  
단계 2에서 추가한 테스트 데이터가 표시되면 성공입니다.

```
맛집 목록
──────────────────────────────────────────────────────
번호 | 가게 이름     | 카테고리 | 방문일     | 등록일     | 관리
 2   | 홍콩반점      | 중식     | 2025-03-15 | 2025-03-16 | 수정 삭제
 1   | 맛있는 순두부  | 한식     | 2025-03-10 | 2025-03-16 | 수정 삭제
```

### 5.9.3 콘솔에서 SQL 확인

콘솔 탭에 아래와 같이 실행된 SQL 이 출력되면 정상입니다.

```
SELECT no, member_id, name, category, address, memo, visit_date, reg_date
  FROM restaurant
 WHERE member_id = 1
 ORDER BY no DESC
```

---

## 자주 발생하는 오류

| 오류 | 원인 | 해결 방법 |
|---|---|---|
| `NullPointerException` at `loginMember.getId()` | 인터셉터가 동작하지 않아 비로그인 상태로 접근 | `spring-mvc.xml` 의 인터셉터 경로가 `/restaurant/**` 인지 확인합니다 |
| 목록이 비어 있음 | 로그인한 회원 id 와 DB 의 `member_id` 가 다름 | `SELECT * FROM restaurant` 로 데이터를 확인합니다 |
| `fmt:formatDate` 오류 | `LocalDateTime` 변환 문제 | `regDate` 필드 타입이 `LocalDateTime` 인지 확인합니다 |

---

## ✅ 단계 5 완료 체크리스트

- [ ] `RestaurantMapper.java` 인터페이스가 작성됐습니다.
- [ ] `RestaurantMapper.xml` 이 `src/main/resources/mappers/` 에 작성됐습니다.
- [ ] `RestaurantService.java` 인터페이스가 작성됐습니다.
- [ ] `RestaurantServiceImpl.java` 가 작성됐습니다.
- [ ] `RestaurantController.java` 가 작성됐습니다.
- [ ] `WEB-INF/views/restaurant/list.jsp` 가 작성됐습니다.
- [ ] 로그인 후 맛집 목록 페이지가 정상적으로 표시됩니다.
- [ ] 테스트 데이터가 목록에 출력됩니다.

모든 항목이 체크됐으면 **단계 6. 맛집 등록 구현**으로 이동합니다.
