# 단계 9. 맛집 삭제 구현

> **목표** : 등록한 맛집을 삭제하는 기능을 구현합니다. 본인이 등록한 맛집만 삭제할 수 있도록 처리합니다.
> **소요 시간** : 약 0.5시간

---

## 9.1 전체 흐름 이해

```
브라우저
  ↓ POST /restaurant/delete  (삭제 요청 — 확인 다이얼로그 후)
RestaurantController.delete()
  ↓ 본인 글인지 확인
  ↓ restaurantService.remove(no) 호출
RestaurantServiceImpl.remove()
  ↓ restaurantMapper.delete(no) 호출 → DB 에서 삭제
RestaurantController
  ↓ 목록 페이지로 리다이렉트
브라우저→ /restaurant/list
```

---

## 9.2 RestaurantMapper 에 delete 추가

`RestaurantMapper.java` 에 `delete` 메서드를 추가합니다.

```java
package com.food.mapper;

import com.food.domain.Restaurant;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface RestaurantMapper {

    List<Restaurant> selectAll(@Param("memberId") int memberId);
    void insert(Restaurant restaurant);
    Restaurant selectOne(@Param("no") int no);
    void update(Restaurant restaurant);

    // ↓ 아래 메서드 추가
    /**
     * 맛집 삭제
     */
    void delete(@Param("no") int no);

}
```

---

## 9.3 RestaurantMapper.xml 에 delete SQL 추가

`RestaurantMapper.xml` 에 `<delete>` 를 추가합니다.

```xml
<!-- 맛집 삭제 -->
<delete id="delete">
    DELETE FROM restaurant
     WHERE no = #{no}
</delete>
```

---

## 9.4 RestaurantService 에 remove 추가

`RestaurantService.java` 에 `remove` 메서드를 추가합니다.

```java
package com.food.service;

import com.food.domain.Restaurant;
import java.util.List;

public interface RestaurantService {

    List<Restaurant> getList(int memberId);
    void register(Restaurant restaurant);
    Restaurant getDetail(int no);
    void modify(Restaurant restaurant);

    // ↓ 아래 메서드 추가
    /**
     * 맛집 삭제
     */
    void remove(int no);

}
```

---

## 9.5 RestaurantServiceImpl 에 remove 구현 추가

`RestaurantServiceImpl.java` 에 `remove()` 메서드를 추가합니다.

```java
// ↓ 아래 메서드 추가
@Override
@Transactional
public void remove(int no) {
    restaurantMapper.delete(no);
}
```

---

## 9.6 RestaurantController 에 delete 추가

`RestaurantController.java` 에 삭제 처리 메서드를 추가합니다.

```java
// ↓ 아래 메서드 추가

// 삭제 처리
@PostMapping("/delete")
public String delete(@RequestParam int no, HttpSession session) {

    Member loginMember = (Member) session.getAttribute("loginMember");

    // 본인 글인지 확인
    Restaurant restaurant = restaurantService.getDetail(no);
    if (restaurant.getMemberId() != loginMember.getId()) {
        return "redirect:/restaurant/list";
    }

    restaurantService.remove(no);

    return "redirect:/restaurant/list";
}
```

---

## 9.7 삭제 링크를 POST 방식으로 수정

현재 `list.jsp` 와 `detail.jsp` 의 삭제 링크는 `GET` 방식입니다.  
보안상 삭제는 반드시 `POST` 방식으로 처리해야 합니다.  
링크(`<a>`) 대신 숨김 필드가 포함된 `<form>` 으로 교체합니다.

### 9.7.1 list.jsp 의 삭제 부분 수정

**수정 전**

```jsp
<a href="/restaurant/delete?no=${r.no}"
   onclick="return confirm('삭제하시겠습니까?')">삭제</a>
```

**수정 후**

```jsp
<form method="post" action="/restaurant/delete"
      style="display: inline;"
      onsubmit="return confirm('삭제하시겠습니까?')">
    <input type="hidden" name="no" value="${r.no}">
    <button type="submit">삭제</button>
</form>
```

### 9.7.2 detail.jsp 의 삭제 부분 수정

**수정 전**

```jsp
<a href="/restaurant/delete?no=${restaurant.no}"
   onclick="return confirm('삭제하시겠습니까?')">삭제</a>
```

**수정 후**

```jsp
<form method="post" action="/restaurant/delete"
      style="display: inline;"
      onsubmit="return confirm('삭제하시겠습니까?')">
    <input type="hidden" name="no" value="${restaurant.no}">
    <button type="submit">삭제</button>
</form>
```

> **GET 대신 POST 를 사용하는 이유**
>
> GET 방식은 URL 에 데이터가 그대로 노출됩니다. `/restaurant/delete?no=1` 을 주소창에 직접 입력하면 삭제가 실행될 수 있습니다. POST 방식은 폼 제출로만 요청할 수 있어 더 안전합니다.

---

## 9.8 동작 확인

### 9.8.1 서버 재시작 후 로그인

### 9.8.2 삭제 테스트

1. 목록 페이지에서 **삭제** 버튼을 클릭합니다.
2. "삭제하시겠습니까?" 확인 다이얼로그가 표시됩니다.
3. **확인** 을 클릭합니다.
4. 목록 페이지로 이동하고, 삭제한 맛집이 목록에서 사라지면 성공입니다.

### 9.8.3 DB 확인

```sql
SELECT * FROM restaurant;
```

삭제한 데이터가 없어졌는지 확인합니다.

---

## ✅ 단계 9 완료 체크리스트

- [ ] `RestaurantMapper.java` 에 `delete()` 가 추가됐습니다.
- [ ] `RestaurantMapper.xml` 에 `delete` SQL 이 추가됐습니다.
- [ ] `RestaurantService.java` 에 `remove()` 가 추가됐습니다.
- [ ] `RestaurantServiceImpl.java` 에 `remove()` 구현이 추가됐습니다.
- [ ] `RestaurantController.java` 에 `delete()` 가 추가됐습니다.
- [ ] `list.jsp`, `detail.jsp` 의 삭제 링크가 POST 폼으로 교체됐습니다.
- [ ] 삭제 확인 다이얼로그가 표시됩니다.
- [ ] 삭제 후 목록에서 해당 맛집이 사라집니다.

모든 항목이 체크됐으면 **단계 10. 카테고리 필터링 및 키워드 검색 추가**로 이동합니다.
