# 3장. 웹 계층 구현 — 게시판 뼈대 만들기

---

## 학습 목표

- Spring Boot 환경에서 Controller → Service → Repository 계층 구조를 구현할 수 있다.
- Thymeleaf의 기본 표현식과 레이아웃(Fragment)을 사용할 수 있다.
- 정적 리소스(CSS, JS, 이미지)를 올바른 위치에 배치하고 참조할 수 있다.
- 게시판 목록, 상세, 수정 화면을 구현할 수 있다.

---

## 3.1 Spring MVC와의 연속성

Spring Boot는 Spring MVC를 그대로 사용합니다. 기존에 학습한 `@Controller`, `@RequestMapping`, `Model`, `@GetMapping`, `@PostMapping` 등 모든 어노테이션이 동일하게 동작합니다. 달라지는 것은 설정 방식뿐입니다.

| 구분 | Spring MVC (기존) | Spring Boot |
|---|---|---|
| DispatcherServlet 등록 | `web.xml` 또는 `AbstractAnnotationConfigDispatcherServletInitializer` | Auto Configuration이 자동 등록 |
| ViewResolver 설정 | `@Bean`으로 직접 등록 | `spring.thymeleaf.*` 프로퍼티로 자동 설정 |
| 정적 리소스 경로 | `<mvc:resources>` 또는 `addResourceHandlers()` | `/static` 폴더를 자동으로 서빙 |
| 컴포넌트 스캔 | `@ComponentScan` 명시 | `@SpringBootApplication`에 포함 |

### 계층 구조 설계

이미지 게시판의 웹 계층은 다음 세 계층으로 구성됩니다.

```
HTTP 요청
    ↓
Controller   — URL 매핑, 요청/응답 처리, 뷰 반환
    ↓
Service      — 비즈니스 로직, 트랜잭션 처리
    ↓
Repository   — 데이터베이스 접근 (4장에서 구현)
```

3장에서는 Controller와 Service를 구현하고, Repository는 임시 데이터(더미 데이터)로 대체합니다. 4장에서 JPA를 연동하면 더미 데이터를 실제 DB 연동으로 교체합니다.

---

## 3.2 DTO 설계

Controller와 Service 사이에서 데이터를 전달하는 DTO(Data Transfer Object)를 먼저 설계합니다. 엔티티를 직접 뷰에 전달하면 불필요한 필드 노출이나 순환참조 문제가 발생할 수 있어 DTO를 별도로 만드는 것이 좋습니다.

```java
// src/main/java/com/example/imageboard/dto/BoardResponse.java
package com.example.imageboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BoardResponse {

    private Long id;
    private String title;
    private String content;
    private String author;
    private int viewCount;
    private String thumbnailUrl;     // 대표 이미지 경로
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

```java
// src/main/java/com/example/imageboard/dto/BoardCreateRequest.java
package com.example.imageboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoardCreateRequest {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 100, message = "제목은 100자 이내로 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;
}
```

```java
// src/main/java/com/example/imageboard/dto/BoardUpdateRequest.java
package com.example.imageboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoardUpdateRequest {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 100, message = "제목은 100자 이내로 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;
}
```

> **BoardCreateRequest vs BoardUpdateRequest**: 지금은 두 DTO의 필드가 동일하지만, 나중에 첨부 파일·카테고리 등 등록 전용 필드가 생기면 분리해 둔 구조가 유리합니다.

---

## 3.3 Service 구현

3장에서는 DB 없이 동작하도록 더미 데이터를 반환하는 Service를 먼저 만듭니다.

```java
// src/main/java/com/example/imageboard/service/BoardService.java
package com.example.imageboard.service;

import com.example.imageboard.dto.BoardCreateRequest;
import com.example.imageboard.dto.BoardResponse;
import com.example.imageboard.dto.BoardUpdateRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;

@Service
public class BoardService {

    // ── 임시 더미 데이터 (4장에서 JPA로 교체) ──────────────────────────
    private List<BoardResponse> getDummyList() {
        return LongStream.rangeClosed(1, 10)
                .mapToObj(i -> BoardResponse.builder()
                        .id(i)
                        .title("게시글 제목 " + i)
                        .content("게시글 내용입니다. " + i)
                        .author("작성자" + i)
                        .viewCount((int) (i * 10))
                        .thumbnailUrl(null)
                        .createdAt(LocalDateTime.now().minusDays(i))
                        .build())
                .toList();
    }
    // ─────────────────────────────────────────────────────────────────────

    /** 게시글 목록 조회 */
    public List<BoardResponse> findAll() {
        return getDummyList();
    }

    /** 게시글 단건 조회 */
    public BoardResponse findById(Long id) {
        return getDummyList().stream()
                .filter(b -> b.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
    }

    /** 게시글 등록 (4장에서 실제 저장 로직으로 교체) */
    public Long create(BoardCreateRequest request) {
        // 더미: 저장 후 id 1L 반환
        return 1L;
    }

    /** 게시글 수정 (4장에서 실제 수정 로직으로 교체) */
    public void update(Long id, BoardUpdateRequest request) {
        // 더미: 수정 대상이 존재하는지 확인만 수행
        findById(id);
        // 실제 저장 로직은 4장에서 구현
    }

    /** 게시글 삭제 (4장에서 실제 삭제 로직으로 교체) */
    public void delete(Long id) {
        // 더미: 삭제 대상이 존재하는지 확인만 수행
        findById(id);
        // 실제 삭제 로직은 4장에서 구현
    }
}
```

---

## 3.4 Controller 구현

```java
// src/main/java/com/example/imageboard/controller/BoardController.java
package com.example.imageboard.controller;

import com.example.imageboard.dto.BoardCreateRequest;
import com.example.imageboard.dto.BoardResponse;
import com.example.imageboard.dto.BoardUpdateRequest;
import com.example.imageboard.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    /** 게시글 목록 */
    @GetMapping
    public String list(Model model) {
        List<BoardResponse> boards = boardService.findAll();
        model.addAttribute("boards", boards);
        return "board/list";           // templates/board/list.html
    }

    /** 게시글 상세 */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        BoardResponse board = boardService.findById(id);
        model.addAttribute("board", board);
        return "board/detail";         // templates/board/detail.html
    }

    /** 게시글 작성 폼 */
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("boardCreateRequest", new BoardCreateRequest());
        return "board/create";         // templates/board/create.html
    }

    /** 게시글 저장 */
    @PostMapping
    public String create(@Valid @ModelAttribute BoardCreateRequest request,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            return "board/create";     // 검증 실패 시 폼으로 돌아감
        }
        Long id = boardService.create(request);
        return "redirect:/boards/" + id;
    }

    /** 게시글 수정 폼 */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        BoardResponse board = boardService.findById(id);
        BoardUpdateRequest boardUpdateRequest = new BoardUpdateRequest();
        boardUpdateRequest.setTitle(board.getTitle());
        boardUpdateRequest.setContent(board.getContent());
        model.addAttribute("board", board);
        model.addAttribute("boardUpdateRequest", boardUpdateRequest);
        return "board/edit";           // templates/board/edit.html
    }

    /** 게시글 수정 처리 */
    @PutMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute BoardUpdateRequest request,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("board", boardService.findById(id));
            return "board/edit";       // 검증 실패 시 수정 폼으로 돌아감
        }
        boardService.update(id, request);
        return "redirect:/boards/" + id;
    }

    /** 게시글 삭제 */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        boardService.delete(id);
        return "redirect:/boards";
    }
}
```

### @Controller vs @RestController

| 어노테이션 | 반환 방식 | 주 용도 |
|---|---|---|
| `@Controller` | 뷰 이름 문자열 반환 → ViewResolver가 템플릿 파일 렌더링 | HTML 페이지 응답 |
| `@RestController` | 반환 객체를 JSON으로 직렬화 | REST API |

이 과정은 Thymeleaf로 서버 사이드 렌더링을 하므로 `@Controller`를 사용합니다.

### redirect: vs forward:

```java
return "redirect:/boards/" + id;  // HTTP 302 응답 → 브라우저가 새 URL로 재요청
return "board/list";              // forward: 같은 요청을 그대로 뷰로 전달
```

POST 후 목록이나 상세로 이동할 때는 반드시 `redirect:`를 사용합니다. 브라우저 새로고침 시 폼 재제출(중복 저장)이 발생하는 **PRG(Post-Redirect-Get) 패턴**을 따르기 위해서입니다.

---

## 3.5 정적 리소스 처리

Spring Boot는 `src/main/resources/static/` 아래의 파일을 자동으로 정적 리소스로 서빙합니다.

```
src/main/resources/static/
├── css/
│   └── style.css        → http://localhost:8080/css/style.css
├── js/
│   └── main.js          → http://localhost:8080/js/main.js
└── images/
    └── logo.png         → http://localhost:8080/images/logo.png
```

기본 CSS 파일을 생성합니다.

```css
/* src/main/resources/static/css/style.css */
* {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
}

body {
    font-family: 'Noto Sans KR', sans-serif;
    background-color: #f8f9fa;
    color: #333;
}

.container {
    max-width: 1100px;
    margin: 0 auto;
    padding: 0 20px;
}

/* 네비게이션 */
nav {
    background-color: #343a40;
    padding: 14px 0;
}

nav .container {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

nav a {
    color: #fff;
    text-decoration: none;
    font-size: 1rem;
}

nav .brand {
    font-size: 1.3rem;
    font-weight: bold;
}

/* 게시판 테이블 */
.board-table {
    width: 100%;
    border-collapse: collapse;
    background: #fff;
    margin-top: 20px;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 1px 4px rgba(0,0,0,0.1);
}

.board-table th,
.board-table td {
    padding: 14px 16px;
    text-align: left;
    border-bottom: 1px solid #dee2e6;
}

.board-table th {
    background-color: #495057;
    color: #fff;
}

.board-table tr:hover {
    background-color: #f1f3f5;
}

.board-table a {
    color: #212529;
    text-decoration: none;
}

.board-table a:hover {
    color: #0d6efd;
    text-decoration: underline;
}

/* 버튼 */
.btn {
    display: inline-block;
    padding: 8px 18px;
    border-radius: 4px;
    font-size: 0.9rem;
    cursor: pointer;
    border: none;
    text-decoration: none;
}

.btn-primary {
    background-color: #0d6efd;
    color: #fff;
}

.btn-secondary {
    background-color: #6c757d;
    color: #fff;
}

.btn-danger {
    background-color: #dc3545;
    color: #fff;
}

/* 폼 */
.form-group {
    margin-bottom: 18px;
}

.form-group label {
    display: block;
    margin-bottom: 6px;
    font-weight: 600;
}

.form-control {
    width: 100%;
    padding: 10px 12px;
    border: 1px solid #ced4da;
    border-radius: 4px;
    font-size: 1rem;
}

.form-control:focus {
    border-color: #0d6efd;
    outline: none;
}

.error-message {
    color: #dc3545;
    font-size: 0.85rem;
    margin-top: 4px;
}

/* 게시글 상세 */
.board-detail {
    background: #fff;
    padding: 30px;
    border-radius: 8px;
    box-shadow: 0 1px 4px rgba(0,0,0,0.1);
    margin-top: 20px;
}

.board-detail h2 {
    font-size: 1.5rem;
    margin-bottom: 12px;
}

.board-meta {
    color: #6c757d;
    font-size: 0.9rem;
    margin-bottom: 20px;
    border-bottom: 1px solid #dee2e6;
    padding-bottom: 12px;
}

.board-content {
    line-height: 1.8;
    min-height: 200px;
}

.page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 30px;
    margin-bottom: 10px;
}
```

---

## 3.6 Thymeleaf 기초

Thymeleaf는 HTML 파일에 `th:*` 속성을 추가하는 방식으로 동작합니다. 순수 HTML로도 브라우저에서 열 수 있어 디자이너와 협업에 유리합니다.

### 기본 표현식

| 표현식 | 설명 | 예시 |
|---|---|---|
| `${...}` | 변수 표현식 (Model 데이터) | `${board.title}` |
| `@{...}` | URL 표현식 | `@{/boards/{id}(id=${board.id})}` |
| `#{...}` | 메시지 표현식 (i18n) | `#{board.title}` |
| `*{...}` | 선택 변수 표현식 (`th:object` 내부) | `*{title}` |
| `~{...}` | 프래그먼트 표현식 (레이아웃) | `~{layout/default :: head}` |

### 주요 속성

```html
<!-- 텍스트 출력 -->
<span th:text="${board.title}">제목 자리</span>

<!-- HTML 출력 (XSS 주의) -->
<div th:utext="${board.content}">내용 자리</div>

<!-- 조건 렌더링 -->
<div th:if="${boards.isEmpty()}">게시글이 없습니다.</div>
<div th:unless="${boards.isEmpty()}">게시글 목록</div>

<!-- 반복 -->
<tr th:each="board : ${boards}">
    <td th:text="${board.title}">제목</td>
</tr>

<!-- 반복 상태 변수 -->
<tr th:each="board, stat : ${boards}">
    <td th:text="${stat.index + 1}">1</td>    <!-- 0부터 시작 -->
    <td th:text="${stat.count}">1</td>         <!-- 1부터 시작 -->
    <td th:text="${board.title}">제목</td>
</tr>

<!-- URL 링크 -->
<a th:href="@{/boards/{id}(id=${board.id})}">상세보기</a>

<!-- 속성 설정 -->
<input th:value="${board.title}" type="text">
<img th:src="@{/images/logo.png}" alt="로고">

<!-- 클래스 조건부 추가 -->
<li th:class="${stat.first} ? 'first' : ''">...</li>
```

### 날짜 포맷

```html
<!-- LocalDateTime 포맷 -->
<td th:text="${#temporals.format(board.createdAt, 'yyyy-MM-dd HH:mm')}">2026-01-01</td>
```

---

## 3.7 공통 레이아웃 (Fragment)

Thymeleaf의 Fragment 기능을 사용하면 헤더, 푸터 등 공통 영역을 한 곳에서 관리할 수 있습니다.

### 레이아웃 템플릿 생성

```html
<!-- src/main/resources/templates/layout/default.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:fragment="head(title)">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${title} + ' | 이미지 게시판'">이미지 게시판</title>
    <link rel="stylesheet" th:href="@{/css/style.css}">
</head>
<body>

<!-- 네비게이션 프래그먼트 -->
<nav th:fragment="nav">
    <div class="container">
        <a class="brand" th:href="@{/}">📋 이미지 게시판</a>
        <div>
            <a th:href="@{/boards}">게시글 목록</a>
            &nbsp;|&nbsp;
            <a th:href="@{/members/login}">로그인</a>
        </div>
    </div>
</nav>

<!-- 푸터 프래그먼트 -->
<footer th:fragment="footer">
    <div class="container" style="text-align:center; padding: 30px 0; color: #aaa; font-size: 0.85rem;">
        © 2026 이미지 게시판
    </div>
</footer>

</body>
</html>
```

### Fragment 사용 방법

각 페이지에서 `th:replace` 또는 `th:insert`로 프래그먼트를 불러옵니다.

```html
<!-- th:replace — 태그 자체를 프래그먼트로 교체 (권장) -->
<head th:replace="~{layout/default :: head('게시글 목록')}"></head>
<nav th:replace="~{layout/default :: nav}"></nav>

<!-- th:insert — 현재 태그 안에 프래그먼트를 삽입 -->
<div th:insert="~{layout/default :: footer}"></div>
```

---

## 3.8 게시판 목록 화면

```html
<!-- src/main/resources/templates/board/list.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">

<head th:replace="~{layout/default :: head('게시글 목록')}"></head>

<body>

<nav th:replace="~{layout/default :: nav}"></nav>

<div class="container">
    <div class="page-header">
        <h2>게시글 목록</h2>
        <a class="btn btn-primary" th:href="@{/boards/create}">글쓰기</a>
    </div>

    <!-- 게시글이 없을 때 -->
    <div th:if="${boards.isEmpty()}"
         style="text-align:center; padding: 60px 0; color: #aaa;">
        등록된 게시글이 없습니다.
    </div>

    <!-- 게시글 목록 테이블 -->
    <table class="board-table" th:unless="${boards.isEmpty()}">
        <thead>
            <tr>
                <th style="width:60px">번호</th>
                <th>제목</th>
                <th style="width:100px">작성자</th>
                <th style="width:80px">조회수</th>
                <th style="width:140px">작성일</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="board, stat : ${boards}">
                <td th:text="${stat.count}">1</td>
                <td>
                    <a th:href="@{/boards/{id}(id=${board.id})}"
                       th:text="${board.title}">게시글 제목</a>
                    <!-- 썸네일이 있으면 이미지 아이콘 표시 -->
                    <span th:if="${board.thumbnailUrl != null}"
                          style="color:#0d6efd; font-size:0.8rem;">🖼</span>
                </td>
                <td th:text="${board.author}">작성자</td>
                <td th:text="${board.viewCount}">0</td>
                <td th:text="${#temporals.format(board.createdAt, 'yyyy-MM-dd')}">
                    2026-01-01
                </td>
            </tr>
        </tbody>
    </table>
</div>

<footer th:replace="~{layout/default :: footer}"></footer>

</body>
</html>
```

---

## 3.9 게시글 상세 화면

```html
<!-- src/main/resources/templates/board/detail.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">

<head th:replace="~{layout/default :: head('게시글 상세')}"></head>

<body>

<nav th:replace="~{layout/default :: nav}"></nav>

<div class="container">
    <div class="board-detail">

        <h2 th:text="${board.title}">게시글 제목</h2>

        <div class="board-meta">
            <span>작성자: <strong th:text="${board.author}">작성자</strong></span>
            &nbsp;|&nbsp;
            <span>조회수: <strong th:text="${board.viewCount}">0</strong></span>
            &nbsp;|&nbsp;
            <span>작성일:
                <strong th:text="${#temporals.format(board.createdAt, 'yyyy-MM-dd HH:mm')}">
                    2026-01-01 00:00
                </strong>
            </span>
        </div>

        <!-- 대표 이미지 -->
        <div th:if="${board.thumbnailUrl != null}" style="margin-bottom: 20px;">
            <img th:src="${board.thumbnailUrl}"
                 alt="대표 이미지"
                 style="max-width:100%; border-radius:8px;">
        </div>

        <!-- 본문 -->
        <div class="board-content" th:text="${board.content}">
            게시글 내용
        </div>

    </div>

    <!-- 하단 버튼 -->
    <div style="margin-top: 16px; display:flex; gap:8px;">
        <a class="btn btn-secondary" th:href="@{/boards}">목록</a>
        <a class="btn btn-primary" th:href="@{/boards/{id}/edit(id=${board.id})}">수정</a>
        <form th:action="@{/boards/{id}(id=${board.id})}" method="post"
              onsubmit="return confirm('삭제하시겠습니까?')">
            <input type="hidden" name="_method" value="DELETE">
            <button class="btn btn-danger" type="submit">삭제</button>
        </form>
    </div>
</div>

<footer th:replace="~{layout/default :: footer}"></footer>

</body>
</html>
```

> **HTTP Method Override**: HTML 폼은 `GET`과 `POST`만 지원합니다. `DELETE` 요청을 보내려면 숨김 필드 `_method=DELETE`를 사용합니다. Spring Boot는 `HiddenHttpMethodFilter`를 통해 이를 자동 처리합니다. `application.yml`에 아래 설정을 추가하세요.
>
> ```yaml
> spring:
>   mvc:
>     hiddenmethod:
>       filter:
>         enabled: true
> ```

---

## 3.10 게시글 작성 폼

```html
<!-- src/main/resources/templates/board/create.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">

<head th:replace="~{layout/default :: head('게시글 작성')}"></head>

<body>

<nav th:replace="~{layout/default :: nav}"></nav>

<div class="container">
    <div class="board-detail">
        <h2>게시글 작성</h2>

        <form th:action="@{/boards}" th:object="${boardCreateRequest}" method="post">

            <div class="form-group">
                <label for="title">제목</label>
                <input class="form-control" type="text" id="title"
                       th:field="*{title}" placeholder="제목을 입력하세요">
                <!-- 검증 오류 메시지 -->
                <p class="error-message"
                   th:if="${#fields.hasErrors('title')}"
                   th:errors="*{title}">제목 오류</p>
            </div>

            <div class="form-group">
                <label for="content">내용</label>
                <textarea class="form-control" id="content"
                          th:field="*{content}" rows="10"
                          placeholder="내용을 입력하세요"></textarea>
                <p class="error-message"
                   th:if="${#fields.hasErrors('content')}"
                   th:errors="*{content}">내용 오류</p>
            </div>

            <div style="display:flex; gap:8px; justify-content:flex-end;">
                <a class="btn btn-secondary" th:href="@{/boards}">취소</a>
                <button class="btn btn-primary" type="submit">저장</button>
            </div>

        </form>
    </div>
</div>

<footer th:replace="~{layout/default :: footer}"></footer>

</body>
</html>
```

### th:object와 th:field

`th:object`와 `th:field`를 함께 쓰면 폼 바인딩을 간결하게 처리할 수 있습니다.

```html
<form th:object="${boardCreateRequest}">
    <!-- th:field="*{title}" 는 아래 세 속성을 한 번에 처리 -->
    <!-- id="title", name="title", value="${boardCreateRequest.title}" -->
    <input th:field="*{title}">
</form>
```

검증 오류가 있을 때 `th:errors`가 해당 필드의 오류 메시지를 출력하고, `th:field`가 자동으로 `class="error"`를 추가합니다.

---

## 3.11 게시글 수정 폼

수정 폼은 작성 폼(`create.html`)과 구조가 거의 같지만 두 가지가 다릅니다.

1. **기존 데이터 미리 채우기**: 서버에서 받아온 `boardUpdateRequest`의 값이 폼 필드에 표시됩니다.
2. **HTTP PUT 전송**: HTML 폼은 `PUT`을 지원하지 않으므로 숨김 필드 `_method=PUT`을 사용합니다.

```html
<!-- src/main/resources/templates/board/edit.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">

<head th:replace="~{layout/default :: head('게시글 수정')}"></head>

<body>

<nav th:replace="~{layout/default :: nav}"></nav>

<div class="container">
    <div class="board-detail">
        <h2>게시글 수정</h2>

        <!--
            PUT 메서드 오버라이드:
            form의 method="post" + 숨김 필드 _method=PUT
            → HiddenHttpMethodFilter가 PUT으로 변환
        -->
        <form th:action="@{/boards/{id}(id=${board.id})}"
              th:object="${boardUpdateRequest}"
              method="post">
            <input type="hidden" name="_method" value="PUT">

            <div class="form-group">
                <label for="title">제목</label>
                <input class="form-control" type="text" id="title"
                       th:field="*{title}" placeholder="제목을 입력하세요">
                <p class="error-message"
                   th:if="${#fields.hasErrors('title')}"
                   th:errors="*{title}">제목 오류</p>
            </div>

            <div class="form-group">
                <label for="content">내용</label>
                <textarea class="form-control" id="content"
                          th:field="*{content}" rows="10"
                          placeholder="내용을 입력하세요"></textarea>
                <p class="error-message"
                   th:if="${#fields.hasErrors('content')}"
                   th:errors="*{content}">내용 오류</p>
            </div>

            <div style="display:flex; gap:8px; justify-content:flex-end;">
                <a class="btn btn-secondary"
                   th:href="@{/boards/{id}(id=${board.id})}">취소</a>
                <button class="btn btn-primary" type="submit">수정 완료</button>
            </div>

        </form>
    </div>
</div>

<footer th:replace="~{layout/default :: footer}"></footer>

</body>
</html>
```

### create.html과 edit.html 비교

| 항목 | create.html | edit.html |
|---|---|---|
| 폼 액션 | `@{/boards}` | `@{/boards/{id}(id=${board.id})}` |
| HTTP 메서드 | POST | POST + `_method=PUT` |
| 초기값 | 빈 값 | 기존 제목·내용이 채워진 상태 |
| Controller 이동 | `redirect:/boards/{새 id}` | `redirect:/boards/{id}` |

---

## 3.12 루트 경로 리다이렉트

`http://localhost:8080`에 접속하면 게시판 목록으로 이동하도록 합니다.

```java
// src/main/java/com/example/imageboard/controller/HomeController.java
package com.example.imageboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/boards";
    }
}
```

---

## 3.13 실행 및 확인

### URL 목록

| HTTP Method | URL | 설명 |
|---|---|---|
| GET | `/` | 게시판 목록으로 리다이렉트 |
| GET | `/boards` | 게시글 목록 |
| GET | `/boards/{id}` | 게시글 상세 |
| GET | `/boards/create` | 게시글 작성 폼 |
| POST | `/boards` | 게시글 저장 |
| GET | `/boards/{id}/edit` | 게시글 수정 폼 |
| PUT | `/boards/{id}` | 게시글 수정 처리 (`_method=PUT`) |
| DELETE | `/boards/{id}` | 게시글 삭제 (`_method=DELETE`) |

### 실행 확인

```bash
./gradlew bootRun
```

브라우저에서 `http://localhost:8080`에 접속하면 게시글 목록 화면이 표시됩니다. 더미 데이터 10건이 테이블에 출력되고, 제목 링크를 클릭하면 상세 화면으로 이동합니다. 상세 화면의 **수정** 버튼을 클릭하면 기존 제목·내용이 채워진 수정 폼이 열리고, **삭제** 버튼을 클릭하면 확인 후 목록으로 돌아갑니다.

---

## 정리

| 핵심 개념 | 내용 |
|---|---|
| `@Controller` | 뷰 이름을 반환하는 웹 요청 처리 클래스 |
| PRG 패턴 | POST 처리 후 `redirect:`로 중복 제출 방지 |
| `th:each` | Thymeleaf 반복 표현식 |
| `th:object` / `th:field` | 폼 객체 바인딩 및 필드 연결 |
| `th:replace` | 프래그먼트로 공통 레이아웃 적용 |
| `/static` | 정적 리소스 자동 서빙 경로 |
| `_method=PUT/DELETE` | HTML 폼의 HTTP 메서드 오버라이드 (`HiddenHttpMethodFilter`) |

---

## 연습 문제

1. 게시글 목록에서 조회수가 100 이상인 게시글의 제목에 🔥 아이콘을 표시해 보세요. (`th:if` 활용)
2. 게시글 목록 테이블에 홀수/짝수 행의 배경색을 다르게 적용해 보세요. (`stat.odd`, `stat.even` 활용)
3. 네비게이션에 현재 페이지 URL을 확인하여 활성 메뉴에 스타일을 적용해 보세요. (`#httpServletRequest.requestURI` 활용)

---

## 다음 장 예고

4장에서는 Spring Data JPA를 연동합니다. Entity를 설계하고 `JpaRepository`를 사용해 3장의 더미 데이터를 실제 MariaDB 연동으로 교체합니다.
