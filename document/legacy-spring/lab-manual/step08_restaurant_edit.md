# 단계 8. 맛집 수정 구현

> **목표** : 등록한 맛집 정보를 수정할 수 있는 기능을 구현합니다. 수정 폼에 기존 값을 미리 채워서 표시합니다.
> **소요 시간** : 약 1.5시간

---

## 8.1 전체 흐름 이해

```
브라우저
  ↓ GET /restaurant/edit?no=1  (수정 폼 요청)
RestaurantController.editForm()
  ↓ 기존 맛집 데이터 조회
  ↓ model 에 restaurant 담기
  ↓ edit.jsp 반환
edit.jsp (기존 값이 채워진 폼)
  ↓ 수정 후 제출
브라우저
  ↓ POST /restaurant/edit
RestaurantController.edit()
  ↓ 본인 글인지 확인
  ↓ restaurantService.modify(restaurant) 호출
RestaurantServiceImpl.modify()
  ↓ restaurantMapper.update(restaurant) 호출 → DB 업데이트
RestaurantController
  ↓ 상세 페이지로 리다이렉트
브라우저 → /restaurant/detail?no=1
```

---

## 8.2 RestaurantMapper 에 update 추가

`RestaurantMapper.java` 에 `update` 메서드를 추가합니다.

```java
package com.food.mapper;

import com.food.domain.Restaurant;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface RestaurantMapper {

    List<Restaurant> selectAll(@Param("memberId") int memberId);
    void insert(Restaurant restaurant);
    Restaurant selectOne(@Param("no") int no);

    // ↓ 아래 메서드 추가
    /**
     * 맛집 정보 수정
     */
    void update(Restaurant restaurant);

}
```

---

## 8.3 RestaurantMapper.xml 에 update SQL 추가

`RestaurantMapper.xml` 에 `<update>` 를 추가합니다.

```xml
<!-- 맛집 수정 -->
<update id="update">
    UPDATE restaurant
       SET name       = #{name},
           category   = #{category},
           address    = #{address},
           memo       = #{memo},
           visit_date = #{visitDate}
     WHERE no = #{no}
</update>
```

---

## 8.4 RestaurantService 에 modify 추가

`RestaurantService.java` 에 `modify` 메서드를 추가합니다.

```java
package com.food.service;

import com.food.domain.Restaurant;
import java.util.List;

public interface RestaurantService {

    List<Restaurant> getList(int memberId);
    void register(Restaurant restaurant);
    Restaurant getDetail(int no);

    // ↓ 아래 메서드 추가
    /**
     * 맛집 정보 수정
     */
    void modify(Restaurant restaurant);

}
```

---

## 8.5 RestaurantServiceImpl 에 modify 구현 추가

`RestaurantServiceImpl.java` 에 `modify()` 메서드를 추가합니다.

```java
// ↓ 아래 메서드 추가
@Override
@Transactional
public void modify(Restaurant restaurant) {
    restaurantMapper.update(restaurant);
}
```

---

## 8.6 RestaurantController 에 edit 추가

`RestaurantController.java` 에 수정 폼과 수정 처리 메서드를 추가합니다.

```java
// ↓ 아래 메서드 추가

// 수정 폼 표시 (기존 데이터를 폼에 채워서 보여줌)
@GetMapping("/edit")
public String editForm(@RequestParam int no, HttpSession session, Model model) {

    Restaurant restaurant = restaurantService.getDetail(no);
    Member loginMember = (Member) session.getAttribute("loginMember");

    // 본인 글이 아니면 목록으로 돌려보냄
    if (restaurant.getMemberId() != loginMember.getId()) {
        return "redirect:/restaurant/list";
    }

    model.addAttribute("restaurant", restaurant);
    return "restaurant/edit";
}

// 수정 처리
@PostMapping("/edit")
public String edit(Restaurant restaurant, HttpSession session) {

    Member loginMember = (Member) session.getAttribute("loginMember");

    // 본인 글이 아니면 목록으로 돌려보냄
    Restaurant saved = restaurantService.getDetail(restaurant.getNo());
    if (saved.getMemberId() != loginMember.getId()) {
        return "redirect:/restaurant/list";
    }

    restaurantService.modify(restaurant);

    return "redirect:/restaurant/detail?no=" + restaurant.getNo();
}
```

---

## 8.7 edit.jsp 작성

`WEB-INF/views/restaurant/` 폴더 안에 `edit.jsp` 파일을 만듭니다.

```jsp
<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>맛집 수정 — 나만의 맛집 노트</title>
</head>
<body>

<h2>맛집 수정</h2>

<form method="post" action="/restaurant/edit">

    <!-- 맛집 번호는 숨김 필드로 전송 (수정 대상 식별) -->
    <input type="hidden" name="no" value="${restaurant.no}">

    <table>
        <tr>
            <td>가게 이름 *</td>
            <td>
                <input type="text" name="name"
                       value="${restaurant.name}" required style="width: 300px;">
            </td>
        </tr>
        <tr>
            <td>카테고리 *</td>
            <td>
                <select name="category" required>
                    <option value="한식"  ${restaurant.category == '한식'  ? 'selected' : ''}>한식</option>
                    <option value="중식"  ${restaurant.category == '중식'  ? 'selected' : ''}>중식</option>
                    <option value="일식"  ${restaurant.category == '일식'  ? 'selected' : ''}>일식</option>
                    <option value="양식"  ${restaurant.category == '양식'  ? 'selected' : ''}>양식</option>
                    <option value="분식"  ${restaurant.category == '분식'  ? 'selected' : ''}>분식</option>
                    <option value="카페"  ${restaurant.category == '카페'  ? 'selected' : ''}>카페</option>
                    <option value="기타"  ${restaurant.category == '기타'  ? 'selected' : ''}>기타</option>
                </select>
            </td>
        </tr>
        <tr>
            <td>주소 *</td>
            <td>
                <input type="text" name="address"
                       value="${restaurant.address}" required style="width: 300px;">
            </td>
        </tr>
        <tr>
            <td>방문일 *</td>
            <td>
                <input type="date" name="visitDate"
                       value="${restaurant.visitDate}" required>
            </td>
        </tr>
        <tr>
            <td>메모</td>
            <td>
                <textarea name="memo" rows="4" cols="40">${restaurant.memo}</textarea>
            </td>
        </tr>
        <tr>
            <td colspan="2">
                <button type="submit">수정하기</button>
                <a href="/restaurant/detail?no=${restaurant.no}">취소</a>
            </td>
        </tr>
    </table>

</form>

</body>
</html>
```

> **기존 값을 폼에 채우는 방법**
>
> - `<input value="${restaurant.name}">` : 텍스트 입력창의 초기값을 설정합니다.
> - `${restaurant.category == '한식' ? 'selected' : ''}` : 기존 카테고리와 일치하는 옵션에 `selected` 를 붙여 선택된 상태로 표시합니다.
> - `<input type="hidden" name="no">` : 어떤 맛집을 수정하는지 번호를 서버로 전송합니다. 화면에는 보이지 않지만 값은 폼과 함께 전송됩니다.

---

## 8.8 동작 확인

### 8.8.1 서버 재시작 후 로그인

### 8.8.2 수정 폼 접속

목록 또는 상세 페이지의 **수정** 링크를 클릭합니다.  
기존 값이 폼에 채워진 상태로 표시되면 성공입니다.

### 8.8.3 수정 테스트

1. 가게 이름이나 메모를 변경합니다.
2. **수정하기** 버튼을 클릭합니다.
3. 상세 페이지로 이동하고 변경된 내용이 반영되면 성공입니다.

### 8.8.4 DB 확인

```sql
SELECT * FROM restaurant WHERE no = 1;
```

변경된 값이 DB 에 저장됐는지 확인합니다.

---

## 자주 발생하는 오류

| 오류 | 원인 | 해결 방법 |
|---|---|---|
| 수정 후 변경 내용이 반영되지 않음 | `no` hidden 필드가 없어 update WHERE 조건 미적용 | `<input type="hidden" name="no">` 가 있는지 확인합니다 |
| 카테고리가 항상 첫 번째 항목으로 표시됨 | `selected` 조건이 누락됨 | `${restaurant.category == '한식' ? 'selected' : ''}` 를 확인합니다 |
| 수정 폼 접속 시 404 | Controller 의 `@GetMapping("/edit")` 누락 | Controller 를 확인합니다 |

---

## ✅ 단계 8 완료 체크리스트

- [ ] `RestaurantMapper.java` 에 `update()` 가 추가됐습니다.
- [ ] `RestaurantMapper.xml` 에 `update` SQL 이 추가됐습니다.
- [ ] `RestaurantService.java` 에 `modify()` 가 추가됐습니다.
- [ ] `RestaurantServiceImpl.java` 에 `modify()` 구현이 추가됐습니다.
- [ ] `RestaurantController.java` 에 `editForm()`, `edit()` 이 추가됐습니다.
- [ ] `WEB-INF/views/restaurant/edit.jsp` 가 작성됐습니다.
- [ ] 수정 폼에 기존 값이 채워져 표시됩니다.
- [ ] 수정 후 상세 페이지에 변경된 내용이 반영됩니다.

모든 항목이 체크됐으면 **단계 9. 맛집 삭제 구현**으로 이동합니다.
