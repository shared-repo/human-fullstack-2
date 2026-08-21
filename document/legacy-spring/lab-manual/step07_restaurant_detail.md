# 단계 7. 맛집 상세 조회 구현

> **목표** : 목록에서 가게 이름을 클릭하면 해당 맛집의 상세 정보를 표시합니다. 본인이 등록한 맛집에만 수정·삭제 버튼이 보이도록 처리합니다.
> **소요 시간** : 약 1시간

---

## 7.1 전체 흐름 이해

```
브라우저
  ↓ GET /restaurant/detail?no=1
RestaurantController.detail()
  ↓ restaurantService.getDetail(no) 호출
RestaurantServiceImpl.getDetail()
  ↓ restaurantMapper.selectOne(no) 호출 → DB 조회
RestaurantController
  ↓ model 에 restaurant 담기
  ↓ "restaurant/detail" 반환
detail.jsp
  ↓ 맛집 상세 정보 출력
  ↓ 본인 글이면 수정·삭제 버튼 표시
브라우저
```

---

## 7.2 RestaurantMapper 에 selectOne 추가

`RestaurantMapper.java` 에 `selectOne` 메서드를 추가합니다.

```java
package com.food.mapper;

import com.food.domain.Restaurant;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface RestaurantMapper {

    List<Restaurant> selectAll(@Param("memberId") int memberId);

    void insert(Restaurant restaurant);

    // ↓ 아래 메서드 추가
    /**
     * 맛집 번호로 단건 조회
     */
    Restaurant selectOne(@Param("no") int no);

}
```

---

## 7.3 RestaurantMapper.xml 에 selectOne SQL 추가

`RestaurantMapper.xml` 에 `<select>` 를 추가합니다.

```xml
<!-- 맛집 단건 조회 -->
<select id="selectOne" resultType="Restaurant">
    SELECT no, member_id, name, category, address, memo, visit_date, reg_date
      FROM restaurant
     WHERE no = #{no}
</select>
```

---

## 7.4 RestaurantService 에 getDetail 추가

`RestaurantService.java` 에 `getDetail` 메서드를 추가합니다.

```java
package com.food.service;

import com.food.domain.Restaurant;
import java.util.List;

public interface RestaurantService {

    List<Restaurant> getList(int memberId);

    void register(Restaurant restaurant);

    // ↓ 아래 메서드 추가
    /**
     * 맛집 상세 조회
     */
    Restaurant getDetail(int no);

}
```

---

## 7.5 RestaurantServiceImpl 에 getDetail 구현 추가

`RestaurantServiceImpl.java` 에 `getDetail()` 메서드를 추가합니다.

```java
// ↓ 아래 메서드 추가
@Override
@Transactional(readOnly = true)
public Restaurant getDetail(int no) {
    return restaurantMapper.selectOne(no);
}
```

---

## 7.6 RestaurantController 에 detail 추가

`RestaurantController.java` 에 상세 조회 메서드를 추가합니다.

```java
// ↓ 아래 메서드 추가
@GetMapping("/detail")
public String detail(@RequestParam int no, HttpSession session, Model model) {

    Restaurant restaurant = restaurantService.getDetail(no);

    // 로그인한 회원 정보
    Member loginMember = (Member) session.getAttribute("loginMember");

    model.addAttribute("restaurant", restaurant);
    model.addAttribute("loginMember", loginMember);

    return "restaurant/detail";
}
```

`@RequestParam` import 를 추가합니다.

```java
import org.springframework.web.bind.annotation.RequestParam;
```

---

## 7.7 detail.jsp 작성

`WEB-INF/views/restaurant/` 폴더 안에 `detail.jsp` 파일을 만듭니다.

```jsp
<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>${restaurant.name} — 나만의 맛집 노트</title>
</head>
<body>

<h2>맛집 상세</h2>

<table border="1">
    <tr>
        <td>가게 이름</td>
        <td>${restaurant.name}</td>
    </tr>
    <tr>
        <td>카테고리</td>
        <td>${restaurant.category}</td>
    </tr>
    <tr>
        <td>주소</td>
        <td>${restaurant.address}</td>
    </tr>
    <tr>
        <td>방문일</td>
        <td>${restaurant.visitDate}</td>
    </tr>
    <tr>
        <td>메모</td>
        <td>${restaurant.memo}</td>
    </tr>
    <tr>
        <td>등록일</td>
        <td>${restaurant.regDate}</td>
    </tr>
</table>

<br>

<!-- 본인이 등록한 맛집일 때만 수정·삭제 버튼 표시 -->
<c:if test="${restaurant.memberId == loginMember.id}">
    <a href="/restaurant/edit?no=${restaurant.no}">수정</a>
    &nbsp;
    <a href="/restaurant/delete?no=${restaurant.no}"
       onclick="return confirm('삭제하시겠습니까?')">삭제</a>
</c:if>

<br><br>
<a href="/restaurant/list">목록으로</a>

</body>
</html>
```

> **본인 확인 로직**
>
> `${restaurant.memberId == loginMember.id}` 는 맛집을 등록한 회원 번호와 현재 로그인한 회원 번호를 비교합니다. 같을 때만 수정·삭제 버튼이 보입니다. 다른 회원의 맛집 URL 을 직접 입력해도 버튼이 표시되지 않습니다.

---

## 7.8 동작 확인

### 7.8.1 서버 재시작 후 로그인

### 7.8.2 상세 페이지 접속

목록 페이지에서 가게 이름 링크를 클릭합니다.  
해당 맛집의 상세 정보가 표시되면 성공입니다.

### 7.8.3 수정·삭제 버튼 확인

- 본인이 등록한 맛집 → 수정, 삭제 버튼이 표시됩니다.
- (단계 2 테스트 데이터로 다른 회원 데이터가 있다면) 다른 회원 맛집 → 버튼이 표시되지 않습니다.

---

## ✅ 단계 7 완료 체크리스트

- [ ] `RestaurantMapper.java` 에 `selectOne()` 이 추가됐습니다.
- [ ] `RestaurantMapper.xml` 에 `selectOne` SQL 이 추가됐습니다.
- [ ] `RestaurantService.java` 에 `getDetail()` 이 추가됐습니다.
- [ ] `RestaurantServiceImpl.java` 에 `getDetail()` 구현이 추가됐습니다.
- [ ] `RestaurantController.java` 에 `detail()` 이 추가됐습니다.
- [ ] `WEB-INF/views/restaurant/detail.jsp` 가 작성됐습니다.
- [ ] 목록에서 가게 이름 클릭 시 상세 페이지가 표시됩니다.
- [ ] 본인 글에만 수정·삭제 버튼이 표시됩니다.

모든 항목이 체크됐으면 **단계 8. 맛집 수정 구현**으로 이동합니다.
