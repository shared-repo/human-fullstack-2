# 단계 10. 카테고리 필터링 및 키워드 검색 추가

> **목표** : 목록 페이지에서 카테고리로 필터링하거나 가게 이름으로 검색하는 기능을 추가합니다. MyBatis 동적 SQL 을 사용하여 조건이 있을 때만 WHERE 절에 추가합니다.
> **소요 시간** : 약 2시간

---

## 10.1 동적 SQL 이란

지금까지 작성한 SQL 은 항상 고정된 조건으로 실행됐습니다.

```sql
-- 항상 이 SQL 만 실행
SELECT * FROM restaurant WHERE member_id = ?
```

검색 기능을 추가하려면 **조건이 있을 때만 WHERE 절에 추가**하는 동적 SQL 이 필요합니다.

```sql
-- 카테고리 선택 시
SELECT * FROM restaurant WHERE member_id = ? AND category = ?

-- 키워드 입력 시
SELECT * FROM restaurant WHERE member_id = ? AND name LIKE ?

-- 둘 다 선택 시
SELECT * FROM restaurant WHERE member_id = ? AND category = ? AND name LIKE ?

-- 아무것도 선택 안 했을 때
SELECT * FROM restaurant WHERE member_id = ?
```

MyBatis 의 `<where>`, `<if>` 태그를 사용하면 이 네 가지 SQL 을 하나의 XML 로 처리할 수 있습니다.

---

## 10.2 SearchCondition 클래스 작성

검색 조건을 담는 클래스를 만듭니다.  
`com.food.domain` 패키지 안에 `SearchCondition.java` 파일을 만듭니다.

```java
package com.food.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SearchCondition {

    private int memberId;       // 로그인 회원 번호 (필수)
    private String category;    // 카테고리 필터 (선택)
    private String keyword;     // 가게 이름 검색어 (선택)

}
```

---

## 10.3 RestaurantMapper 수정

### 10.3.1 인터페이스 수정

`RestaurantMapper.java` 의 `selectAll` 메서드 파라미터를 `SearchCondition` 으로 변경합니다.

```java
package com.food.mapper;

import com.food.domain.Restaurant;
import com.food.domain.SearchCondition;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface RestaurantMapper {

    // 수정: int memberId → SearchCondition condition
    List<Restaurant> selectAll(SearchCondition condition);

    void insert(Restaurant restaurant);
    Restaurant selectOne(@Param("no") int no);
    void update(Restaurant restaurant);
    void delete(@Param("no") int no);

}
```

### 10.3.2 XML 수정 — 동적 SQL 적용

`RestaurantMapper.xml` 의 `selectAll` SQL 을 동적 SQL 로 교체합니다.

**수정 전**

```xml
<select id="selectAll" resultType="Restaurant">
    SELECT no, member_id, name, category, address, memo, visit_date, reg_date
      FROM restaurant
     WHERE member_id = #{memberId}
     ORDER BY no DESC
</select>
```

**수정 후**

```xml
<select id="selectAll" resultType="Restaurant">
    SELECT no, member_id, name, category, address, memo, visit_date, reg_date
      FROM restaurant
    <where>
        member_id = #{memberId}

        <!-- category 값이 있을 때만 AND category = ? 추가 -->
        <if test="category != null and category != ''">
            AND category = #{category}
        </if>

        <!-- keyword 값이 있을 때만 AND name LIKE ? 추가 -->
        <if test="keyword != null and keyword != ''">
            AND name LIKE CONCAT('%', #{keyword}, '%')
        </if>
    </where>
    ORDER BY no DESC
</select>
```

> **동적 SQL 태그 설명**
>
> - `<where>` : 내부에 조건이 하나라도 있으면 `WHERE` 키워드를 자동으로 붙입니다. 조건이 없으면 `WHERE` 를 붙이지 않습니다. 맨 앞의 `AND` 도 자동으로 제거합니다.
> - `<if test="...">` : test 조건이 참일 때만 내부 SQL 을 포함시킵니다. `category != null and category != ''` 는 카테고리 값이 null 도 아니고 빈 문자열도 아닐 때를 의미합니다.
> - `LIKE CONCAT('%', #{keyword}, '%')` : `%keyword%` 형태로 가게 이름에 검색어가 포함된 경우를 찾습니다.

---

## 10.4 RestaurantService 수정

### 10.4.1 인터페이스 수정

`RestaurantService.java` 의 `getList` 파라미터를 `SearchCondition` 으로 변경합니다.

```java
package com.food.service;

import com.food.domain.Restaurant;
import com.food.domain.SearchCondition;
import java.util.List;

public interface RestaurantService {

    // 수정: int memberId → SearchCondition condition
    List<Restaurant> getList(SearchCondition condition);

    void register(Restaurant restaurant);
    Restaurant getDetail(int no);
    void modify(Restaurant restaurant);
    void remove(int no);

}
```

### 10.4.2 구현 클래스 수정

`RestaurantServiceImpl.java` 의 `getList` 메서드를 수정합니다.

```java
@Override
@Transactional(readOnly = true)
public List<Restaurant> getList(SearchCondition condition) {
    return restaurantMapper.selectAll(condition);
}
```

---

## 10.5 RestaurantController 수정

`RestaurantController.java` 의 `list` 메서드를 수정합니다.

```java
@GetMapping("/list")
public String list(SearchCondition condition,
                   HttpSession session,
                   Model model) {

    Member loginMember = (Member) session.getAttribute("loginMember");

    // SearchCondition 에 로그인 회원 번호 설정
    condition.setMemberId(loginMember.getId());

    List<Restaurant> list = restaurantService.getList(condition);

    model.addAttribute("list", list);
    model.addAttribute("loginMember", loginMember);
    model.addAttribute("condition", condition);  // 검색 조건을 JSP 에 전달 (선택값 유지)

    return "restaurant/list";
}
```

> `SearchCondition condition` 파라미터를 추가하면 스프링이 요청 파라미터(`category`, `keyword`)를 자동으로 `SearchCondition` 객체에 담아줍니다. 검색 조건이 없으면 필드 값이 `null` 이 됩니다.

---

## 10.6 list.jsp 수정 — 검색 폼 추가

`list.jsp` 상단에 검색 폼을 추가합니다. 기존 `<h2>` 태그 위에 삽입합니다.

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

<!-- 검색 폼 (추가된 부분) -->
<form method="get" action="/restaurant/list">
    <select name="category">
        <option value="">전체 카테고리</option>
        <option value="한식" ${condition.category == '한식' ? 'selected' : ''}>한식</option>
        <option value="중식" ${condition.category == '중식' ? 'selected' : ''}>중식</option>
        <option value="일식" ${condition.category == '일식' ? 'selected' : ''}>일식</option>
        <option value="양식" ${condition.category == '양식' ? 'selected' : ''}>양식</option>
        <option value="분식" ${condition.category == '분식' ? 'selected' : ''}>분식</option>
        <option value="카페" ${condition.category == '카페' ? 'selected' : ''}>카페</option>
        <option value="기타" ${condition.category == '기타' ? 'selected' : ''}>기타</option>
    </select>

    <input type="text" name="keyword"
           value="${condition.keyword}"
           placeholder="가게 이름으로 검색">

    <button type="submit">검색</button>
    <a href="/restaurant/list">전체 보기</a>
</form>

<h2>나의 맛집 목록</h2>

<!-- 목록이 없을 때 -->
<c:if test="${empty list}">
    <p>검색 결과가 없습니다.</p>
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
                    <form method="post" action="/restaurant/delete"
                          style="display: inline;"
                          onsubmit="return confirm('삭제하시겠습니까?')">
                        <input type="hidden" name="no" value="${r.no}">
                        <button type="submit">삭제</button>
                    </form>
                </td>
            </tr>
            </c:forEach>
        </tbody>
    </table>
</c:if>

</body>
</html>
```

> **검색 조건 유지 방법**
>
> 검색 후에도 선택한 카테고리와 입력한 키워드가 폼에 남아 있어야 사용자가 편리합니다. `${condition.category == '한식' ? 'selected' : ''}` 와 `value="${condition.keyword}"` 가 이 역할을 합니다. Controller 에서 `model.addAttribute("condition", condition)` 으로 검색 조건을 JSP 에 전달했기 때문에 사용 가능합니다.

---

## 10.7 동작 확인

### 10.7.1 서버 재시작 후 로그인

### 10.7.2 카테고리 필터링 테스트

1. 목록 페이지에서 카테고리 드롭다운에서 **한식** 을 선택합니다.
2. **검색** 버튼을 클릭합니다.
3. 한식 카테고리의 맛집만 표시되면 성공입니다.
4. URL 을 확인합니다: `/restaurant/list?category=한식`

### 10.7.3 키워드 검색 테스트

1. 검색창에 가게 이름 일부를 입력합니다.
2. **검색** 버튼을 클릭합니다.
3. 입력한 단어가 포함된 가게만 표시되면 성공입니다.

### 10.7.4 복합 검색 테스트

카테고리와 키워드를 함께 입력하고 검색합니다.  
두 조건을 모두 만족하는 맛집만 표시되면 성공입니다.

### 10.7.5 전체 보기 테스트

**전체 보기** 링크를 클릭합니다.  
검색 조건이 초기화되고 전체 목록이 표시되면 성공입니다.

### 10.7.6 콘솔에서 동적 SQL 확인

검색 조건에 따라 실행 SQL 이 달라지는 것을 콘솔에서 확인합니다.

```sql
-- 카테고리만 선택한 경우
SELECT ... FROM restaurant WHERE member_id = 1 AND category = '한식' ORDER BY no DESC

-- 키워드만 입력한 경우
SELECT ... FROM restaurant WHERE member_id = 1 AND name LIKE '%순두부%' ORDER BY no DESC

-- 아무 조건 없는 경우
SELECT ... FROM restaurant WHERE member_id = 1 ORDER BY no DESC
```

---

## 자주 발생하는 오류

| 오류 | 원인 | 해결 방법 |
|---|---|---|
| 검색 후 모든 회원의 맛집이 표시됨 | `memberId` 가 설정되지 않음 | Controller 에서 `condition.setMemberId(loginMember.getId())` 를 확인합니다 |
| 검색 조건이 폼에 유지되지 않음 | `model.addAttribute("condition", condition)` 누락 | Controller 의 `list` 메서드를 확인합니다 |
| `<if test>` 가 동작하지 않음 | XML 조건식 오타 | `category != null and category != ''` 를 정확히 입력했는지 확인합니다 |

---

## ✅ 단계 10 완료 체크리스트

- [ ] `SearchCondition.java` 가 `com.food.domain` 패키지에 작성됐습니다.
- [ ] `RestaurantMapper.java` 의 `selectAll` 파라미터가 `SearchCondition` 으로 변경됐습니다.
- [ ] `RestaurantMapper.xml` 의 `selectAll` SQL 이 동적 SQL 로 수정됐습니다.
- [ ] `RestaurantService.java` 의 `getList` 파라미터가 `SearchCondition` 으로 변경됐습니다.
- [ ] `RestaurantServiceImpl.java` 의 `getList` 가 수정됐습니다.
- [ ] `RestaurantController.java` 의 `list` 메서드가 수정됐습니다.
- [ ] `list.jsp` 에 검색 폼이 추가됐습니다.
- [ ] 카테고리 필터링이 동작합니다.
- [ ] 키워드 검색이 동작합니다.
- [ ] 검색 후 선택한 조건이 폼에 유지됩니다.

모든 항목이 체크됐으면 **단계 11. 마무리 점검 및 오류 해결**로 이동합니다.
