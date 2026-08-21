# 단계 11. 마무리 점검 및 오류 해결

> **목표** : 완성된 애플리케이션의 전체 기능을 점검하고, 기본적인 오류 페이지를 추가하여 마무리합니다.
> **소요 시간** : 약 2시간

---

## 11.1 전체 기능 동작 확인

아래 시나리오를 순서대로 실행하며 모든 기능이 정상 동작하는지 확인합니다.

### 시나리오 1 — 회원가입 및 로그인

| 번호 | 동작 | 확인 방법 | 결과 |
|---|---|---|---|
| 1 | `/member/register` 에 접속 | 회원가입 폼이 표시됨 | ☐ |
| 2 | 새 아이디/비밀번호/이름 입력 후 가입 | 로그인 페이지로 이동 | ☐ |
| 3 | 같은 아이디로 다시 가입 시도 | "이미 사용 중인 아이디입니다." 표시 | ☐ |
| 4 | 올바른 아이디/비밀번호로 로그인 | 맛집 목록 페이지로 이동 | ☐ |
| 5 | 틀린 비밀번호로 로그인 시도 | 오류 메시지 표시 | ☐ |

### 시나리오 2 — 맛집 CRUD

| 번호 | 동작 | 확인 방법 | 결과 |
|---|---|---|---|
| 6 | 맛집 등록 | 등록 폼 입력 후 목록에 새 항목 표시 | ☐ |
| 7 | 맛집 상세 조회 | 가게 이름 클릭 시 상세 정보 표시 | ☐ |
| 8 | 맛집 수정 | 수정 폼에 기존 값이 채워지고 변경 후 반영 | ☐ |
| 9 | 맛집 삭제 | 확인 다이얼로그 후 목록에서 삭제 | ☐ |

### 시나리오 3 — 검색 및 보안

| 번호 | 동작 | 확인 방법 | 결과 |
|---|---|---|---|
| 10 | 카테고리 필터링 | 선택한 카테고리만 목록에 표시 | ☐ |
| 11 | 키워드 검색 | 입력한 단어가 포함된 가게만 표시 | ☐ |
| 12 | 로그아웃 | 로그인 페이지로 이동 | ☐ |
| 13 | 로그아웃 후 `/restaurant/list` 직접 접속 | 로그인 페이지로 리다이렉트 | ☐ |

---

## 11.2 기본 오류 페이지 추가

현재 존재하지 않는 페이지에 접근하면 Tomcat 의 기본 오류 화면이 표시됩니다.  
간단한 오류 페이지를 추가하여 사용자 경험을 개선합니다.

### 11.2.1 GlobalExceptionHandler 작성

`com.food.controller` 패키지 안에 `GlobalExceptionHandler.java` 파일을 만듭니다.

```java
package com.food.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 모든 예외를 잡아서 오류 페이지로 이동
    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/general";
    }

}
```

### 11.2.2 error 폴더 및 general.jsp 작성

`WEB-INF/views/` 아래에 `error` 폴더를 만들고, `general.jsp` 파일을 작성합니다.

```jsp
<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>오류 — 나만의 맛집 노트</title>
</head>
<body>

<h2>오류가 발생했습니다</h2>

<p>요청을 처리하는 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.</p>

<c:if test="${not empty errorMessage}">
    <p style="color: gray; font-size: 0.9em;">${errorMessage}</p>
</c:if>

<a href="/restaurant/list">목록으로 돌아가기</a>

</body>
</html>
```

---

## 11.3 전체 코드 흐름 최종 복습

완성된 애플리케이션의 전체 흐름을 다시 한번 정리합니다.

```
[브라우저] GET /restaurant/list?category=한식
    ↓
[LoginCheckInterceptor] 로그인 여부 확인
    ↓ 세션에 loginMember 있음 → 통과
[RestaurantController.list()]
    ↓ session에서 loginMember 꺼냄
    ↓ SearchCondition 생성 (memberId + category=한식)
    ↓ restaurantService.getList(condition) 호출
[RestaurantServiceImpl.getList()]
    ↓ @Transactional(readOnly=true) 트랜잭션 시작
    ↓ restaurantMapper.selectAll(condition) 호출
[RestaurantMapper.xml selectAll]
    ↓ SELECT ... WHERE member_id=1 AND category='한식' 실행
[MariaDB]
    ↓ 한식 맛집 목록 반환
[RestaurantServiceImpl → RestaurantController]
    ↓ model에 list, loginMember, condition 담기
    ↓ "restaurant/list" 반환
[InternalResourceViewResolver]
    ↓ /WEB-INF/views/restaurant/list.jsp 렌더링
[브라우저] 한식 맛집 목록 페이지 표시
```

---

## 11.4 최종 파일 구조 확인

모든 단계를 완료한 후의 프로젝트 구조입니다. 아래 구조와 일치하는지 확인합니다.

```
food-note
├── src/main/java/com/food
│   ├── controller
│   │   ├── GlobalExceptionHandler.java   ✅
│   │   ├── MemberController.java          ✅
│   │   └── RestaurantController.java      ✅
│   ├── domain
│   │   ├── Member.java                    ✅
│   │   ├── Restaurant.java                ✅
│   │   └── SearchCondition.java           ✅
│   ├── interceptor
│   │   └── LoginCheckInterceptor.java     ✅
│   ├── mapper
│   │   ├── MemberMapper.java              ✅
│   │   └── RestaurantMapper.java          ✅
│   ├── service
│   │   ├── MemberService.java             ✅
│   │   └── RestaurantService.java         ✅
│   └── service/impl
│       ├── MemberServiceImpl.java         ✅
│       └── RestaurantServiceImpl.java     ✅
│
├── src/main/resources
│   ├── mappers
│   │   ├── MemberMapper.xml               ✅
│   │   └── RestaurantMapper.xml           ✅
│   ├── logback.xml                        ✅
│   └── mybatis-config.xml                 ✅
│
└── src/main/webapp/WEB-INF
    ├── spring
    │   ├── applicationContext.xml         ✅
    │   └── spring-mvc.xml                 ✅
    ├── views
    │   ├── error
    │   │   └── general.jsp                ✅
    │   ├── member
    │   │   ├── login.jsp                  ✅
    │   │   └── register.jsp               ✅
    │   └── restaurant
    │       ├── detail.jsp                 ✅
    │       ├── edit.jsp                   ✅
    │       ├── list.jsp                   ✅
    │       └── write.jsp                  ✅
    └── web.xml                            ✅
```

---

## 11.5 자주 발생하는 오류 총정리

| 오류 | 주요 원인 | 확인 위치 |
|---|---|---|
| 서버 시작 오류 | `applicationContext.xml` DB 접속 정보 오류 | `username`, `password`, DB명 확인 |
| 404 Not Found | Controller 경로 오타, 서버 미재시작 | `@GetMapping`, `@PostMapping` 경로 확인 |
| 500 Internal Server Error | Java 코드 오류, SQL 오류 | 콘솔 스택 트레이스 확인 |
| `Invalid bound statement` | Mapper XML `namespace` 또는 `id` 오타 | XML 과 인터페이스 메서드명 비교 |
| 한글 깨짐 | `CharacterEncodingFilter` 누락 | `web.xml` 확인 |
| 로그인 후 다시 로그인 페이지로 이동 | 세션 저장 누락 | `session.setAttribute("loginMember", member)` 확인 |
| 목록에 데이터가 없음 | `memberId` 조건 오류 | SQL 의 `WHERE member_id = ?` 와 로그인 id 비교 |
| 수정/삭제 후 반영 안 됨 | `no` 값 전송 누락 | 폼의 `<input type="hidden" name="no">` 확인 |
| `@Transactional` 적용 안 됨 | `applicationContext.xml` 의 `<tx:annotation-driven/>` 누락 | XML 설정 확인 |

---

## 11.6 추가 개선 아이디어 (시간이 남는 경우)

기본 기능을 모두 완성했다면 아래 기능을 추가로 도전해 볼 수 있습니다.

**쉬운 개선**
- 목록 페이지에 총 건수 표시 (`${list.size()}개의 맛집`)
- 맛집 등록 날짜를 한국어 형식으로 표시 (`2025년 3월 10일`)
- 카테고리별 아이콘 이미지 추가 (CSS 클래스 적용)

**중간 난이도**
- 별점(1~5) 컬럼 추가 (`restaurant` 테이블에 `rating INT` 컬럼 추가)
- 목록을 방문일 기준으로 정렬하는 옵션 추가

---

## ✅ 최종 완료 체크리스트

### 파일 작성 완료

- [ ] 모든 Java 파일이 작성됐습니다. (Controller 3개, Service 2+2개, Mapper 2개, Domain 3개, Interceptor 1개)
- [ ] 모든 Mapper XML 이 작성됐습니다. (MemberMapper.xml, RestaurantMapper.xml)
- [ ] 모든 JSP 파일이 작성됐습니다. (login, register, list, write, detail, edit, error/general)
- [ ] 모든 설정 파일이 작성됐습니다. (web.xml, applicationContext.xml, spring-mvc.xml, mybatis-config.xml, logback.xml)

### 기능 동작 완료

- [ ] 회원가입이 동작합니다.
- [ ] 로그인/로그아웃이 동작합니다.
- [ ] 로그인 없이 맛집 페이지 접근 시 로그인 페이지로 이동합니다.
- [ ] 맛집 등록이 동작합니다.
- [ ] 맛집 목록 조회가 동작합니다.
- [ ] 맛집 상세 조회가 동작합니다.
- [ ] 맛집 수정이 동작합니다.
- [ ] 맛집 삭제가 동작합니다.
- [ ] 카테고리 필터링이 동작합니다.
- [ ] 키워드 검색이 동작합니다.
- [ ] 오류 발생 시 오류 페이지가 표시됩니다.

---

## 🎉 실습 완료

수고하셨습니다! 이번 실습을 통해 다음 내용을 직접 구현해 보셨습니다.

- **Spring MVC** : Controller → Service → Mapper 레이어 구조
- **MyBatis** : SQL Mapper XML 작성, 동적 SQL(`<where>`, `<if>`)
- **@Transactional** : 데이터 변경 시 트랜잭션 적용, 조회 시 `readOnly=true`
- **HandlerInterceptor** : 로그인 여부 확인 후 접근 제어
- **세션(Session)** : 로그인 정보 유지
- **JSTL** : JSP 에서 반복(`<c:forEach>`), 조건(`<c:if>`) 처리
