# 단계 6. 맛집 등록 구현

> **목표** : 가게 이름, 카테고리, 주소, 메모, 방문일을 입력하여 새로운 맛집을 DB에 저장합니다.
> **소요 시간** : 약 1.5시간

---

## 6.1 전체 흐름 이해

```
브라우저
  ↓ GET /restaurant/write  (등록 폼 요청)
RestaurantController.writeForm()
  ↓ write.jsp 반환
브라우저 (폼 입력 후 제출)
  ↓ POST /restaurant/write
RestaurantController.write()
  ↓ 세션에서 로그인 회원 id 꺼내 restaurant 에 설정
  ↓ restaurantService.register(restaurant) 호출
RestaurantServiceImpl.register()
  ↓ restaurantMapper.insert(restaurant) 호출 → DB 저장
RestaurantController
  ↓ 목록 페이지로 리다이렉트
브라우저 → /restaurant/list
```

---

## 6.2 RestaurantMapper 에 insert 추가

`RestaurantMapper.java` 에 `insert` 메서드를 추가합니다.

```java
package com.food.mapper;

import com.food.domain.Restaurant;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface RestaurantMapper {

    List<Restaurant> selectAll(@Param("memberId") int memberId);

    // ↓ 아래 메서드 추가
    /**
     * 맛집 정보를 DB에 저장
     */
    void insert(Restaurant restaurant);

}
```

---

## 6.3 RestaurantMapper.xml 에 insert SQL 추가

`RestaurantMapper.xml` 에 `<insert>` 를 추가합니다.

```xml
<!-- 맛집 등록 -->
<insert id="insert" useGeneratedKeys="true" keyProperty="no">
    INSERT INTO restaurant (member_id, name, category, address, memo, visit_date)
    VALUES (#{memberId}, #{name}, #{category}, #{address}, #{memo}, #{visitDate})
</insert>
```

추가 후 전체 파일 내용은 다음과 같습니다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
    PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.food.mapper.RestaurantMapper">

    <select id="selectAll" resultType="Restaurant">
        SELECT no, member_id, name, category, address, memo, visit_date, reg_date
          FROM restaurant
         WHERE member_id = #{memberId}
         ORDER BY no DESC
    </select>

    <!-- 추가된 SQL -->
    <insert id="insert" useGeneratedKeys="true" keyProperty="no">
        INSERT INTO restaurant (member_id, name, category, address, memo, visit_date)
        VALUES (#{memberId}, #{name}, #{category}, #{address}, #{memo}, #{visitDate})
    </insert>

</mapper>
```

---

## 6.4 RestaurantService 에 register 추가

`RestaurantService.java` 에 `register` 메서드를 추가합니다.

```java
package com.food.service;

import com.food.domain.Restaurant;
import java.util.List;

public interface RestaurantService {

    List<Restaurant> getList(int memberId);

    // ↓ 아래 메서드 추가
    /**
     * 맛집 등록
     */
    void register(Restaurant restaurant);

}
```

---

## 6.5 RestaurantServiceImpl 에 register 구현 추가

`RestaurantServiceImpl.java` 에 `register()` 메서드를 추가합니다.

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

    // ↓ 아래 메서드 추가
    @Override
    @Transactional
    public void register(Restaurant restaurant) {
        restaurantMapper.insert(restaurant);
    }

}
```

---

## 6.6 RestaurantController 에 write 추가

`RestaurantController.java` 에 등록 폼과 등록 처리 메서드를 추가합니다.

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;

@Controller
@RequestMapping("/restaurant")
public class RestaurantController {

    @Autowired
    private RestaurantService restaurantService;

    @GetMapping("/list")
    public String list(HttpSession session, Model model) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        List<Restaurant> list = restaurantService.getList(loginMember.getId());
        model.addAttribute("list", list);
        model.addAttribute("loginMember", loginMember);
        return "restaurant/list";
    }

    // ↓ 아래 메서드 추가

    // 등록 폼 표시
    @GetMapping("/write")
    public String writeForm() {
        return "restaurant/write";
    }

    // 등록 처리
    @PostMapping("/write")
    public String write(Restaurant restaurant, HttpSession session) {

        // 세션에서 로그인한 회원 id 를 꺼내 restaurant 에 설정
        Member loginMember = (Member) session.getAttribute("loginMember");
        restaurant.setMemberId(loginMember.getId());

        restaurantService.register(restaurant);

        return "redirect:/restaurant/list";
    }

}
```

---

## 6.7 write.jsp 작성

`WEB-INF/views/restaurant/` 폴더 안에 `write.jsp` 파일을 만듭니다.

```jsp
<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>맛집 등록 — 나만의 맛집 노트</title>
</head>
<body>

<h2>맛집 등록</h2>

<form method="post" action="/restaurant/write">

    <table>
        <tr>
            <td>가게 이름 *</td>
            <td><input type="text" name="name" required style="width: 300px;"></td>
        </tr>
        <tr>
            <td>카테고리 *</td>
            <td>
                <select name="category" required>
                    <option value="">-- 선택 --</option>
                    <option value="한식">한식</option>
                    <option value="중식">중식</option>
                    <option value="일식">일식</option>
                    <option value="양식">양식</option>
                    <option value="분식">분식</option>
                    <option value="카페">카페</option>
                    <option value="기타">기타</option>
                </select>
            </td>
        </tr>
        <tr>
            <td>주소 *</td>
            <td><input type="text" name="address" required style="width: 300px;"></td>
        </tr>
        <tr>
            <td>방문일 *</td>
            <td><input type="date" name="visitDate" required></td>
        </tr>
        <tr>
            <td>메모</td>
            <td><textarea name="memo" rows="4" cols="40"></textarea></td>
        </tr>
        <tr>
            <td colspan="2">
                <button type="submit">등록하기</button>
                <a href="/restaurant/list">취소</a>
            </td>
        </tr>
    </table>

</form>

</body>
</html>
```

> **폼 필드와 Restaurant 클래스 필드 대응**
>
> | input/select name | Restaurant 필드 |
> |---|---|
> | `name` | `private String name` |
> | `category` | `private String category` |
> | `address` | `private String address` |
> | `visitDate` | `private LocalDate visitDate` |
> | `memo` | `private String memo` |
>
> `memberId` 는 폼에 없습니다. Controller 에서 세션 값으로 직접 설정합니다.

---

## 6.8 동작 확인

### 6.8.1 서버 재시작 후 로그인

서버를 재시작하고 로그인합니다.

### 6.8.2 등록 폼 접속

목록 페이지 상단의 **맛집 등록** 링크를 클릭하거나, 주소창에 직접 입력합니다.

```
http://localhost:8080/food-note/restaurant/write
```

등록 폼이 표시되면 성공입니다.

### 6.8.3 맛집 등록 테스트

1. 가게 이름, 카테고리, 주소, 방문일을 입력합니다.
2. **등록하기** 버튼을 클릭합니다.
3. 맛집 목록 페이지로 이동하고, 방금 등록한 맛집이 목록 맨 위에 표시되면 성공입니다.

### 6.8.4 DB 확인

```sql
SELECT * FROM restaurant ORDER BY no DESC;
```

방금 등록한 데이터가 보이면 성공입니다.

---

## 자주 발생하는 오류

| 오류 | 원인 | 해결 방법 |
|---|---|---|
| `visitDate` 가 null 로 저장됨 | input `type="date"` 의 값 형식 불일치 | `LocalDate` 타입인지, input name 이 `visitDate` 인지 확인합니다 |
| `memberId` 가 0 으로 저장됨 | 세션에서 id 를 꺼내지 않음 | `restaurant.setMemberId(loginMember.getId())` 코드가 있는지 확인합니다 |
| 등록 후 목록에 내 글이 안 보임 | 다른 회원 id 로 조회 중 | 로그아웃 후 재로그인하여 확인합니다 |

---

## ✅ 단계 6 완료 체크리스트

- [ ] `RestaurantMapper.java` 에 `insert()` 가 추가됐습니다.
- [ ] `RestaurantMapper.xml` 에 `insert` SQL 이 추가됐습니다.
- [ ] `RestaurantService.java` 에 `register()` 가 추가됐습니다.
- [ ] `RestaurantServiceImpl.java` 에 `register()` 구현이 추가됐습니다.
- [ ] `RestaurantController.java` 에 `writeForm()`, `write()` 가 추가됐습니다.
- [ ] `WEB-INF/views/restaurant/write.jsp` 가 작성됐습니다.
- [ ] 등록 폼이 정상적으로 표시됩니다.
- [ ] 등록 후 목록 페이지에 새 맛집이 표시됩니다.
- [ ] DB `restaurant` 테이블에 데이터가 저장됐습니다.

모든 항목이 체크됐으면 **단계 7. 맛집 상세 조회 구현**으로 이동합니다.
