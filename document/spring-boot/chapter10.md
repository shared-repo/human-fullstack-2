# 10장. Ajax 활용 — 동적 웹 기능 구현

---

## 학습 목표

- Ajax의 동작 원리를 이해하고 전통적인 요청-응답 방식과의 차이를 설명할 수 있다.
- `@RestController`와 `ResponseEntity<T>`로 JSON 응답을 반환하는 API를 설계할 수 있다.
- Fetch API로 GET·POST 요청을 보내고 응답을 처리할 수 있다.
- 6장에서 학습한 CSRF 보호를 Ajax 요청에 적용할 수 있다.
- 좋아요·댓글·자동완성 등 실전 비동기 기능을 구현할 수 있다.
- `FormData`로 파일을 Ajax 업로드하고 업로드 미리보기를 구현할 수 있다.
- `@RestControllerAdvice`로 Ajax 요청에 일관된 JSON 오류 응답을 반환할 수 있다.
- 8장에서 학습한 MockMvc로 Ajax 엔드포인트를 테스트할 수 있다.

---

## 10.1 Ajax 개요

### 전통적인 요청-응답 방식의 한계

지금까지 구현한 이미지 게시판은 모든 사용자 행동(게시글 작성, 수정, 삭제)에 **전체 페이지를 새로 불러오는** 방식으로 동작합니다.

```
[사용자 클릭]
      ↓
[브라우저] → POST /boards/1/like → [서버]
                                        ↓ (302 Redirect)
[브라우저] ← Location: /boards/1 ←────
      ↓
전체 페이지 재렌더링 (헤더·푸터·이미지 목록 모두 다시 로딩)
```

좋아요 버튼 하나를 누르기 위해 페이지 전체를 다시 받아오는 것은 비효율적이고 사용성이 떨어집니다.

### Ajax란?

**Ajax(Asynchronous JavaScript and XML)** 는 페이지 전체를 갱신하지 않고, 서버와 **필요한 데이터만** 비동기로 주고받는 기술입니다.

```
[사용자 클릭]
      ↓
[JavaScript] → POST /api/boards/1/likes (비동기) → [서버]
                                                          ↓ (JSON 응답)
[JavaScript] ← { "liked": true, "likeCount": 5 } ←─────
      ↓
버튼 상태·숫자만 교체 (페이지 재로딩 없음)
```

### XMLHttpRequest vs Fetch API

| 비교 항목 | XMLHttpRequest | Fetch API |
|---|---|---|
| 등장 시기 | 초기 Ajax (IE5, 1999) | ES6 (2015) |
| 문법 | 콜백 기반, 복잡함 | Promise 기반, 간결함 |
| async/await 지원 | 비공식 래핑 필요 | 기본 지원 |
| 현재 권장 | ❌ 신규 개발 비권장 | ✅ 권장 |

이 장에서는 현대적인 방식인 **Fetch API**를 사용합니다.

---

## 10.2 @RestController와 JSON 응답

### @Controller vs @RestController

기존 `BoardController`는 뷰 이름(문자열)을 반환해 Thymeleaf 템플릿을 렌더링합니다. Ajax 엔드포인트는 뷰 대신 **JSON 데이터**를 반환해야 합니다.

```java
// @Controller — 뷰 이름 반환 → Thymeleaf 렌더링
@Controller
public class BoardController {
    @GetMapping("/boards")
    public String list(Model model) {
        model.addAttribute("boards", boardService.findAll());
        return "board/list";  // templates/board/list.html 렌더링
    }
}
```

```java
// @RestController — 객체 반환 → Jackson이 JSON으로 직렬화
@RestController
public class CommentApiController {
    @GetMapping("/api/boards/{boardId}/comments")
    public List<CommentResponse> getComments(@PathVariable Long boardId) {
        return commentService.findByBoardId(boardId);
        // → [{"id":1,"content":"댓글입니다","author":"홍길동"}, ...]
    }
}
```

`@RestController`는 `@Controller` + `@ResponseBody`의 축약입니다.

```java
// 두 선언은 동일하게 동작함
@Controller
@ResponseBody        // ← 이 어노테이션이 핵심
public class CommentApiController { ... }

// ↕ 축약

@RestController
public class CommentApiController { ... }
```

### ResponseEntity<T> — HTTP 상태 코드 제어

단순히 객체를 반환하면 항상 HTTP 200이 응답됩니다. `ResponseEntity<T>`를 사용하면 상태 코드를 명시적으로 제어할 수 있습니다.

```java
// HTTP 200 (기본)
return commentResponse;

// HTTP 201 Created — 리소스 생성 성공
return ResponseEntity.status(HttpStatus.CREATED).body(commentResponse);

// HTTP 204 No Content — 삭제 성공 (응답 본문 없음)
return ResponseEntity.noContent().build();

// HTTP 404 Not Found — 빈 응답
return ResponseEntity.notFound().build();

// 헤더 포함
return ResponseEntity.ok()
        .header("X-Total-Count", String.valueOf(totalCount))
        .body(commentList);
```

### Jackson 직렬화 설정

Spring Boot는 Jackson을 기본 JSON 라이브러리로 사용합니다. 자주 쓰는 설정을 살펴봅니다.

```java
// DTO에서 특정 필드 제어
public class MemberResponse {

    private Long id;
    private String username;

    @JsonIgnore                        // JSON에서 제외 — 비밀번호 등 민감 정보
    private String password;

    @JsonProperty("displayName")       // JSON 키 이름 변경 (nickname → displayName)
    private String nickname;
}
```

```yaml
# application.yml — Jackson 전역 설정
spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss   # 날짜 포맷
    time-zone: Asia/Seoul
    default-property-inclusion: non_null  # null 필드는 JSON에서 제외
```

> **엔티티를 직접 반환하지 마세요.** `Board`·`Member` 엔티티를 직접 JSON으로 반환하면 양방향 연관관계로 인한 무한 재귀(StackOverflowError)가 발생합니다. **반드시 응답 전용 DTO(`~Response`)로 변환하여 반환**하세요.

---

## 10.3 Fetch API 실습 기초

### GET 요청 — 데이터 조회

```javascript
// 기본 GET 요청
async function fetchComments(boardId) {
    const response = await fetch(`/api/boards/${boardId}/comments`);

    if (!response.ok) {
        throw new Error(`HTTP 오류: ${response.status}`);
    }

    const comments = await response.json();  // JSON 문자열 → JS 객체
    return comments;
}

// 사용
fetchComments(1)
    .then(comments => console.log(comments))
    .catch(error => console.error('댓글 조회 실패:', error));
```

### POST 요청 — JSON 바디 전송

```javascript
// JSON 바디로 POST 요청
async function addComment(boardId, content) {
    const response = await fetch(`/api/boards/${boardId}/comments`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',  // ← 서버가 JSON임을 인식하도록 필수
        },
        body: JSON.stringify({ content }),        // JS 객체 → JSON 문자열
    });

    if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || '댓글 등록에 실패했습니다.');
    }

    return await response.json();
}
```

### 공통 Fetch 래퍼 함수

매번 에러 처리 코드를 반복하지 않도록 공통 함수를 만들어 둡니다. CSRF 토큰 처리(10.4)까지 포함할 것이므로 별도 파일로 분리합니다.

```javascript
// src/main/resources/static/js/api.js

// 페이지 로드 시 메타 태그에서 CSRF 정보 읽기 (10.4에서 추가되는 메타 태그)
const csrfToken  = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

/**
 * CSRF 토큰이 포함된 Fetch 공통 래퍼
 * - POST · PUT · DELETE 요청에 자동으로 CSRF 헤더 추가
 * - 204 No Content 응답은 null 반환
 * - 오류 응답은 서버 메시지로 예외 throw
 */
async function apiRequest(url, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    // 상태를 변경하는 요청에만 CSRF 헤더 추가
    const method = (options.method || 'GET').toUpperCase();
    if (method !== 'GET' && csrfToken) {
        headers[csrfHeader] = csrfToken;
    }

    const response = await fetch(url, { ...options, headers });

    // 204 No Content — 본문 없이 성공
    if (response.status === 204) return null;

    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.message || `서버 오류 (${response.status})`);
    }

    return data;
}

/**
 * 디바운스 유틸리티 (10.7 자동완성에서 사용)
 * 마지막 호출 이후 delay(ms)가 지나야 fn을 실행
 */
function debounce(fn, delay) {
    let timerId;
    return function (...args) {
        clearTimeout(timerId);
        timerId = setTimeout(() => fn.apply(this, args), delay);
    };
}
```

---

## 10.4 Spring Security와 Ajax — CSRF 토큰 처리

6장에서 학습한 것처럼 Spring Security는 POST·PUT·DELETE 요청에 CSRF 토큰을 요구합니다. 폼 기반 요청은 Thymeleaf가 자동으로 삽입해 줬지만, **Ajax 요청은 직접 헤더에 포함**해야 합니다.

### CSRF 토큰을 메타 태그로 노출

레이아웃 `<head>` 프래그먼트에 메타 태그를 추가합니다.

```html
<!-- templates/layout/default.html — head 프래그먼트 수정 -->
<head th:fragment="head(title)">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <!-- CSRF 토큰 메타 태그 — JavaScript에서 읽어 Ajax 헤더에 사용 -->
    <meta name="_csrf"        th:content="${_csrf.token}">
    <meta name="_csrf_header" th:content="${_csrf.headerName}">

    <title th:text="${title} + ' | 이미지 게시판'">이미지 게시판</title>
    <link rel="stylesheet" th:href="@{/css/style.css}">
</head>
```

이렇게 하면 10.3에서 작성한 `api.js`의 `csrfToken`·`csrfHeader` 변수가 값을 읽어 모든 비 GET 요청 헤더에 자동으로 포함됩니다.

> **왜 헤더로 전송하나?** CSRF 공격자의 악성 페이지는 다른 출처(origin)에서 실행됩니다. 동일 출처 정책(Same-Origin Policy)에 의해 악성 페이지는 우리 사이트의 DOM에 접근할 수 없으므로 메타 태그에서 토큰을 읽지 못합니다. 토큰을 헤더로 전송하는 방식은 동일 출처에서만 가능하기 때문에 CSRF를 효과적으로 차단합니다.

### SecurityConfig — Ajax 인증 오류 JSON 응답

비로그인 상태에서 Ajax로 인증이 필요한 엔드포인트를 호출하면, 기본적으로 로그인 페이지(HTML)로 리다이렉트됩니다. JavaScript는 HTML을 JSON으로 파싱하려다 오류가 발생합니다. 401 JSON 응답을 반환하도록 설정합니다.

```java
// SecurityConfig.java — exceptionHandling 추가
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // ... 기존 authorizeHttpRequests, formLogin, logout 설정 ...

        // Ajax 인증 오류 처리
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) -> {
                String contentType = request.getHeader("Content-Type");
                boolean isAjax = contentType != null
                        && contentType.contains("application/json");

                if (isAjax) {
                    // Ajax 요청 → JSON 오류 응답
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"message\":\"로그인이 필요합니다.\"}");
                } else {
                    // 일반 요청 → 로그인 페이지 리다이렉트 (기존 동작)
                    response.sendRedirect("/members/login");
                }
            })
        )

        .userDetailsService(userDetailsService);

    return http.build();
}
```

---

## 10.5 실전 예제 ① — 좋아요(Like) 기능

### 도메인 설계

회원 한 명이 게시글 하나에 한 번만 좋아요를 누를 수 있습니다. `BoardLike` 엔티티로 관계를 표현합니다.

```java
// src/main/java/com/example/imageboard/entity/BoardLike.java
package com.example.imageboard.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "board_like",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"board_id", "member_id"}  // 회원당 게시글 1개 좋아요 보장
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public static BoardLike of(Board board, Member member) {
        BoardLike like = new BoardLike();
        like.board = board;
        like.member = member;
        return like;
    }
}
```

### BoardLikeRepository

```java
// src/main/java/com/example/imageboard/repository/BoardLikeRepository.java
package com.example.imageboard.repository;

import com.example.imageboard.entity.BoardLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface BoardLikeRepository extends JpaRepository<BoardLike, Long> {

    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);

    long countByBoardId(Long boardId);

    @Transactional
    void deleteByBoardIdAndMemberId(Long boardId, Long memberId);
}
```

### LikeService

```java
// src/main/java/com/example/imageboard/service/LikeService.java
package com.example.imageboard.service;

import com.example.imageboard.entity.Board;
import com.example.imageboard.entity.BoardLike;
import com.example.imageboard.entity.Member;
import com.example.imageboard.exception.BoardNotFoundException;
import com.example.imageboard.exception.MemberNotFoundException;
import com.example.imageboard.repository.BoardLikeRepository;
import com.example.imageboard.repository.BoardRepository;
import com.example.imageboard.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final BoardLikeRepository boardLikeRepository;
    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;

    /**
     * 좋아요 토글 — 이미 좋아요 상태면 취소, 아니면 추가
     *
     * @return liked: 토글 후 좋아요 상태, likeCount: 현재 좋아요 수
     */
    @Transactional
    public LikeResult toggle(Long boardId, Long memberId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardNotFoundException(boardId));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        boolean alreadyLiked = boardLikeRepository
                .existsByBoardIdAndMemberId(boardId, memberId);

        if (alreadyLiked) {
            boardLikeRepository.deleteByBoardIdAndMemberId(boardId, memberId);
        } else {
            boardLikeRepository.save(BoardLike.of(board, member));
        }

        long likeCount = boardLikeRepository.countByBoardId(boardId);
        return new LikeResult(!alreadyLiked, likeCount);
    }

    @Transactional(readOnly = true)
    public LikeResult getStatus(Long boardId, Long memberId) {
        boolean liked = boardLikeRepository
                .existsByBoardIdAndMemberId(boardId, memberId);
        long count = boardLikeRepository.countByBoardId(boardId);
        return new LikeResult(liked, count);
    }

    /** 좋아요 결과 — Java 16+ Record */
    public record LikeResult(boolean liked, long likeCount) {}
}
```

### LikeApiController

```java
// src/main/java/com/example/imageboard/controller/api/LikeApiController.java
package com.example.imageboard.controller.api;

import com.example.imageboard.security.CustomUserDetails;
import com.example.imageboard.service.LikeService;
import com.example.imageboard.service.LikeService.LikeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/boards/{boardId}/likes")
@RequiredArgsConstructor
public class LikeApiController {

    private final LikeService likeService;

    /** 좋아요 상태 조회 — 비로그인도 허용 (liked: false 반환) */
    @GetMapping
    public ResponseEntity<LikeResult> getStatus(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.ok(new LikeResult(false, 0L));
        }
        return ResponseEntity.ok(
                likeService.getStatus(boardId, userDetails.getMemberId()));
    }

    /** 좋아요 토글 — 로그인 필요 */
    @PostMapping
    public ResponseEntity<LikeResult> toggle(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        LikeResult result = likeService.toggle(boardId, userDetails.getMemberId());
        return ResponseEntity.ok(result);
    }
}
```

### SecurityConfig — /api/** 접근 권한 추가

```java
// SecurityConfig.java — authorizeHttpRequests에 추가
.authorizeHttpRequests(auth -> auth
    // ... 기존 설정 ...

    // API — GET은 비로그인 허용, 상태 변경은 로그인 필요
    .requestMatchers(HttpMethod.GET,    "/api/**").permitAll()
    .requestMatchers(HttpMethod.POST,   "/api/**").authenticated()
    .requestMatchers(HttpMethod.DELETE, "/api/**").authenticated()
    .anyRequest().authenticated()
)
```

### 게시글 상세 화면 — 좋아요 버튼

```html
<!-- templates/board/detail.html — 좋아요 버튼 추가 -->
<div style="margin-top:24px; text-align:center;">
    <button id="like-btn"
            th:data-board-id="${board.id}"
            th:class="${liked} ? 'btn btn-danger' : 'btn btn-secondary'"
            onclick="toggleLike(this)">
        ❤️ <span id="like-count" th:text="${likeCount}">0</span>
    </button>
</div>

<script src="/js/api.js"></script>
<script src="/js/like.js"></script>
```

```javascript
// src/main/resources/static/js/like.js

async function toggleLike(btn) {
    const boardId = btn.dataset.boardId;

    try {
        btn.disabled = true;  // 중복 클릭 방지

        const result = await apiRequest(`/api/boards/${boardId}/likes`, {
            method: 'POST',
        });

        document.getElementById('like-count').textContent = result.likeCount;
        btn.className = result.liked ? 'btn btn-danger' : 'btn btn-secondary';

    } catch (error) {
        if (error.message.includes('로그인')) {
            alert('로그인 후 좋아요를 누를 수 있습니다.');
            location.href = '/members/login';
        } else {
            alert('오류가 발생했습니다: ' + error.message);
        }
    } finally {
        btn.disabled = false;
    }
}
```

```java
// BoardController.java — detail() 메서드에 좋아요 정보 추가
@GetMapping("/{id}")
public String detail(@PathVariable Long id,
                     @AuthenticationPrincipal CustomUserDetails userDetails,
                     Model model) {
    BoardResponse board = boardService.findById(id);
    model.addAttribute("board", board);
    boardService.increaseViewCount(id);

    // 좋아요 정보 — 로그인 사용자의 좋아요 여부 포함
    if (userDetails != null) {
        LikeService.LikeResult likeResult =
                likeService.getStatus(id, userDetails.getMemberId());
        model.addAttribute("liked",     likeResult.liked());
        model.addAttribute("likeCount", likeResult.likeCount());
    } else {
        model.addAttribute("liked",     false);
        model.addAttribute("likeCount", boardLikeRepository.countByBoardId(id));
    }

    return "board/detail";
}
```

---

## 10.6 실전 예제 ② — 댓글 CRUD

### Comment 엔티티

```java
// src/main/java/com/example/imageboard/entity/Comment.java
package com.example.imageboard.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false, length = 500)
    private String content;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public static Comment create(Board board, Member member, String content) {
        Comment comment = new Comment();
        comment.board   = board;
        comment.member  = member;
        comment.content = content;
        return comment;
    }
}
```

### CommentRepository

```java
// src/main/java/com/example/imageboard/repository/CommentRepository.java
package com.example.imageboard.repository;

import com.example.imageboard.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 게시글 ID로 댓글 목록 오름차순 조회
    List<Comment> findByBoardIdOrderByCreatedAtAsc(Long boardId);
}
```

### DTO

```java
// src/main/java/com/example/imageboard/dto/CommentResponse.java
package com.example.imageboard.dto;

import com.example.imageboard.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponse {

    private Long id;
    private String content;
    private String author;        // 작성자 닉네임
    private Long authorId;        // 작성자 ID (클라이언트 권한 확인용)
    private LocalDateTime createdAt;

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .author(comment.getMember().getNickname())
                .authorId(comment.getMember().getId())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
```

```java
// src/main/java/com/example/imageboard/dto/CommentCreateRequest.java
package com.example.imageboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentCreateRequest {

    @NotBlank(message = "댓글 내용을 입력해주세요.")
    @Size(max = 500, message = "댓글은 500자 이내로 입력해주세요.")
    private String content;
}
```

### CommentService

```java
// src/main/java/com/example/imageboard/service/CommentService.java
package com.example.imageboard.service;

import com.example.imageboard.dto.CommentCreateRequest;
import com.example.imageboard.dto.CommentResponse;
import com.example.imageboard.entity.Board;
import com.example.imageboard.entity.Comment;
import com.example.imageboard.entity.Member;
import com.example.imageboard.exception.*;
import com.example.imageboard.repository.BoardRepository;
import com.example.imageboard.repository.CommentRepository;
import com.example.imageboard.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository   boardRepository;
    private final MemberRepository  memberRepository;

    @Transactional(readOnly = true)
    public List<CommentResponse> findByBoardId(Long boardId) {
        return commentRepository.findByBoardIdOrderByCreatedAtAsc(boardId)
                .stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public CommentResponse create(Long boardId, CommentCreateRequest request,
                                  Long memberId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardNotFoundException(boardId));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        Comment comment = Comment.create(board, member, request.getContent());
        return CommentResponse.from(commentRepository.save(comment));
    }

    @Transactional
    public void delete(Long commentId, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "댓글을 찾을 수 없습니다. id=" + commentId));

        // 작성자 본인만 삭제 가능
        if (!comment.getMember().getId().equals(memberId)) {
            throw new NotBoardOwnerException();
        }

        commentRepository.delete(comment);
    }
}
```

### CommentApiController

```java
// src/main/java/com/example/imageboard/controller/api/CommentApiController.java
package com.example.imageboard.controller.api;

import com.example.imageboard.dto.CommentCreateRequest;
import com.example.imageboard.dto.CommentResponse;
import com.example.imageboard.security.CustomUserDetails;
import com.example.imageboard.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards/{boardId}/comments")
@RequiredArgsConstructor
public class CommentApiController {

    private final CommentService commentService;

    /** 댓글 목록 조회 — 비로그인도 허용 */
    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable Long boardId) {
        return ResponseEntity.ok(commentService.findByBoardId(boardId));
    }

    /** 댓글 등록 — 로그인 필요 */
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long boardId,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CommentResponse comment =
                commentService.create(boardId, request, userDetails.getMemberId());
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    /** 댓글 삭제 — 작성자 본인만 */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long boardId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        commentService.delete(commentId, userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }
}
```

### 게시글 상세 화면 — 댓글 섹션

```html
<!-- templates/board/detail.html — 댓글 섹션 추가 -->
<section class="comment-section"
         th:data-board-id="${board.id}"
         th:data-member-id="${#authentication != null and #authentication.principal != 'anonymousUser'
                              ? #authentication.principal.memberId : ''}">

    <h3 style="margin-top:40px;">
        댓글 <span id="comment-count">0</span>개
    </h3>

    <!-- 댓글 목록 (JavaScript로 동적 렌더링) -->
    <div id="comment-list" style="margin-bottom:20px;"></div>

    <!-- 댓글 작성 폼 — 로그인 사용자만 표시 -->
    <div sec:authorize="isAuthenticated()">
        <textarea id="comment-input" class="form-control" rows="3"
                  placeholder="댓글을 입력하세요 (최대 500자)"
                  style="margin-bottom:8px;"></textarea>
        <button class="btn btn-primary" onclick="submitComment()">등록</button>
    </div>
    <p sec:authorize="!isAuthenticated()"
       style="color:#6c757d; font-size:0.9rem;">
        댓글을 작성하려면 <a th:href="@{/members/login}">로그인</a>하세요.
    </p>
</section>

<script src="/js/api.js"></script>
<script src="/js/comment.js"></script>
```

```javascript
// src/main/resources/static/js/comment.js

const section        = document.querySelector('.comment-section');
const boardId        = section?.dataset.boardId;
const currentMember  = section?.dataset.memberId;

/** 날짜 포맷 헬퍼 */
function formatDate(dateStr) {
    const d = new Date(dateStr);
    return d.toLocaleDateString('ko-KR') + ' '
         + d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
}

/** 댓글 하나를 HTML로 렌더링 */
function renderComment(c) {
    const isOwner = currentMember && String(c.authorId) === String(currentMember);
    return `
        <div id="comment-${c.id}"
             style="padding:12px; border-bottom:1px solid #dee2e6;">
            <div style="display:flex; justify-content:space-between; margin-bottom:4px;">
                <span style="font-weight:bold;">${c.author}</span>
                <span style="color:#6c757d; font-size:0.85rem;">${formatDate(c.createdAt)}</span>
            </div>
            <p style="margin:0 0 6px;">${c.content}</p>
            ${isOwner
                ? `<button onclick="deleteComment(${c.id})"
                           style="background:none; border:none; color:#dc3545;
                                  font-size:0.85rem; cursor:pointer; padding:0;">삭제</button>`
                : ''}
        </div>`;
}

/** 댓글 목록 로딩 */
async function loadComments() {
    try {
        const comments = await apiRequest(`/api/boards/${boardId}/comments`);
        const list  = document.getElementById('comment-list');
        const count = document.getElementById('comment-count');

        list.innerHTML = comments.length
            ? comments.map(renderComment).join('')
            : '<p style="color:#6c757d; text-align:center; padding:20px;">첫 댓글을 남겨보세요!</p>';

        count.textContent = comments.length;
    } catch (e) {
        console.error('댓글 로딩 실패:', e);
    }
}

/** 댓글 등록 */
async function submitComment() {
    const input   = document.getElementById('comment-input');
    const content = input.value.trim();

    if (!content) { alert('댓글 내용을 입력해주세요.'); return; }

    try {
        await apiRequest(`/api/boards/${boardId}/comments`, {
            method: 'POST',
            body: JSON.stringify({ content }),
        });
        input.value = '';
        await loadComments();
    } catch (e) {
        alert('댓글 등록에 실패했습니다: ' + e.message);
    }
}

/** 댓글 삭제 */
async function deleteComment(commentId) {
    if (!confirm('댓글을 삭제하시겠습니까?')) return;

    try {
        await apiRequest(`/api/boards/${boardId}/comments/${commentId}`, {
            method: 'DELETE',
        });
        // 삭제된 요소만 DOM에서 제거
        document.getElementById(`comment-${commentId}`)?.remove();

        const count = document.getElementById('comment-count');
        count.textContent = parseInt(count.textContent) - 1;
    } catch (e) {
        alert('댓글 삭제에 실패했습니다: ' + e.message);
    }
}

// 페이지 로드 시 댓글 목록 초기화
document.addEventListener('DOMContentLoaded', loadComments);
```

---

## 10.7 실전 예제 ③ — 실시간 검색 자동완성

### 디바운싱(Debouncing)

사용자가 키를 입력할 때마다 서버에 요청을 보내면 과부하가 발생합니다. **디바운싱**은 마지막 입력 이후 일정 시간(예: 300ms)이 지났을 때만 요청을 전송하는 기법입니다.

```
키 입력: "스 → 스프 → 스프링 → 스프링 부"  (300ms 이내 연속 입력)
서버 요청: "스프링 부" 입력 300ms 후 단 한 번만 전송
```

`debounce` 함수는 10.3에서 `api.js`에 이미 작성해 두었습니다.

### 자동완성 API 엔드포인트

```java
// src/main/java/com/example/imageboard/controller/api/BoardApiController.java
package com.example.imageboard.controller.api;

import com.example.imageboard.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardApiController {

    private final BoardService boardService;

    /**
     * 게시글 제목 자동완성 — 키워드가 2자 미만이면 빈 목록 반환
     *
     * @param keyword 검색 키워드
     * @return 제목 목록 (최대 10개)
     */
    @GetMapping("/autocomplete")
    public List<String> autocomplete(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().length() < 2) {
            return List.of();
        }
        return boardService.autocomplete(keyword.trim());
    }
}
```

```java
// BoardRepository.java — 자동완성 쿼리 추가
@Query("SELECT b.title FROM Board b WHERE b.title LIKE %:keyword% ORDER BY b.createdAt DESC")
List<String> findTitlesByKeyword(@Param("keyword") String keyword, Pageable pageable);
```

```java
// BoardService.java — autocomplete 메서드 추가
@Transactional(readOnly = true)
public List<String> autocomplete(String keyword) {
    return boardRepository.findTitlesByKeyword(keyword, PageRequest.of(0, 10));
}
```

### 검색 폼 — 자동완성 드롭다운

```html
<!-- templates/board/list.html — 검색 입력 영역 수정 -->
<div style="position:relative; display:inline-block;">
    <input type="text" id="search-input" name="keyword"
           class="form-control" placeholder="제목으로 검색"
           autocomplete="off"
           th:value="${keyword}"
           oninput="handleSearchInput(this.value)">

    <!-- 자동완성 드롭다운 -->
    <div id="autocomplete-list"
         hidden
         style="position:absolute; top:100%; left:0; right:0; background:#fff;
                border:1px solid #dee2e6; border-top:none; border-radius:0 0 4px 4px;
                z-index:1000; max-height:200px; overflow-y:auto;
                box-shadow:0 4px 6px rgba(0,0,0,.1);">
    </div>
</div>

<script src="/js/api.js"></script>
<script src="/js/autocomplete.js"></script>
```

```javascript
// src/main/resources/static/js/autocomplete.js

const searchInput = document.getElementById('search-input');
const acList      = document.getElementById('autocomplete-list');

/** 드롭다운 닫기 */
function closeList() {
    acList.hidden = true;
    acList.innerHTML = '';
}

/** 다른 영역 클릭 시 드롭다운 닫기 */
document.addEventListener('click', (e) => {
    if (!e.target.closest('#search-input') && !e.target.closest('#autocomplete-list')) {
        closeList();
    }
});

/** 항목 클릭 — 검색 실행 */
function selectItem(title) {
    searchInput.value = title;
    closeList();
    searchInput.closest('form')?.submit();
}

/** 자동완성 목록 렌더링 */
function renderList(items) {
    if (!items.length) { closeList(); return; }

    acList.innerHTML = items.map(title => `
        <div onclick="selectItem('${title.replace(/'/g, "\\'")}')"
             style="padding:8px 12px; cursor:pointer; font-size:0.9rem;"
             onmouseenter="this.style.background='#f8f9fa'"
             onmouseleave="this.style.background=''">
            ${title}
        </div>`).join('');

    acList.hidden = false;
}

/** 디바운싱 적용 — 300ms 대기 후 요청 */
const handleSearchInput = debounce(async (keyword) => {
    if (!keyword || keyword.trim().length < 2) { closeList(); return; }

    try {
        const items = await apiRequest(
            `/api/boards/autocomplete?keyword=${encodeURIComponent(keyword)}`
        );
        renderList(items);
    } catch (e) {
        console.error('자동완성 오류:', e);
        closeList();
    }
}, 300);
```

---

## 10.8 파일 업로드 Ajax 처리

5장에서 구현한 파일 업로드는 폼 제출 방식으로, 업로드 완료 후 페이지가 이동했습니다. Ajax 방식으로 업로드하면 **게시글 작성 화면을 유지하면서 미리보기를 즉시 표시**할 수 있습니다.

### FormData — 멀티파트 Ajax 전송

```javascript
// FormData로 파일 전송하기
async function uploadImage(file) {
    const formData = new FormData();
    formData.append('image', file);  // 서버 @RequestParam("image")와 이름 일치

    // ⚠️ FormData 전송 시 Content-Type을 명시하지 않아야 함
    // 브라우저가 multipart/form-data; boundary=... 를 자동으로 설정함
    const headers = {};
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;   // CSRF 헤더만 추가
    }

    const response = await fetch('/api/images/upload', {
        method: 'POST',
        headers,          // Content-Type 없음 ← 의도된 생략
        body: formData,
    });

    if (!response.ok) throw new Error('업로드에 실패했습니다.');
    return await response.json();
    // → { storedName: "uuid.jpg", originalUrl: "...", thumbnailUrl: "..." }
}
```

### ImageUploadApiController

```java
// src/main/java/com/example/imageboard/controller/api/ImageUploadApiController.java
package com.example.imageboard.controller.api;

import com.example.imageboard.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageUploadApiController {

    private final FileService fileService;

    /**
     * 이미지 즉시 업로드 — 게시글 작성 중 미리보기용
     * 5장의 FileService를 그대로 재사용
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("image") MultipartFile file) {

        String storedName = fileService.store(file);
        fileService.createThumbnail(storedName);

        return ResponseEntity.ok(Map.of(
                "storedName",    storedName,
                "originalUrl",   "/images/" + storedName,
                "thumbnailUrl",  "/thumbnails/" + storedName
        ));
    }
}
```

### 파일 선택 즉시 미리보기

```html
<!-- templates/board/create.html — 이미지 입력 영역 수정 -->
<div class="form-group">
    <label>이미지 첨부</label>
    <div>
        <input type="file" id="image-input" accept="image/*" multiple
               style="display:none" onchange="handleImageSelect(this)">
        <button type="button" class="btn btn-secondary"
                onclick="document.getElementById('image-input').click()">
            📎 이미지 선택
        </button>
    </div>

    <!-- 미리보기 영역 -->
    <div id="preview-area"
         style="display:flex; flex-wrap:wrap; gap:8px; margin-top:12px;"></div>

    <!-- 업로드된 파일명을 hidden input으로 누적 (폼 제출 시 서버로 전달) -->
    <div id="uploaded-names"></div>
</div>
<script src="/js/api.js"></script>
<script src="/js/image-upload.js"></script>
```

```javascript
// src/main/resources/static/js/image-upload.js

const previewArea     = document.getElementById('preview-area');
const uploadedNames   = document.getElementById('uploaded-names');

async function handleImageSelect(input) {
    for (const file of Array.from(input.files)) {
        if (!file.type.startsWith('image/')) {
            alert(`${file.name}은 이미지 파일이 아닙니다.`); continue;
        }

        // 로컬 미리보기 + 로딩 스피너 즉시 표시
        const tempId = crypto.randomUUID();
        previewArea.appendChild(makePreviewEl(tempId, URL.createObjectURL(file), true));

        try {
            const result = await uploadImage(file);

            // 미리보기 이미지를 서버 URL로 교체
            const img = document.getElementById(`img-${tempId}`);
            if (img) img.src = result.thumbnailUrl;
            document.getElementById(`spinner-${tempId}`)?.remove();

            // 폼 제출 시 서버가 받을 파일명 누적
            const hidden = document.createElement('input');
            hidden.type  = 'hidden';
            hidden.name  = 'uploadedImageNames';
            hidden.value = result.storedName;
            uploadedNames.appendChild(hidden);

        } catch (e) {
            alert(`${file.name} 업로드 실패: ${e.message}`);
            document.getElementById(`preview-${tempId}`)?.remove();
        }
    }
    input.value = '';  // 같은 파일 재선택 가능하도록 초기화
}

function makePreviewEl(id, src, loading) {
    const div = document.createElement('div');
    div.id    = `preview-${id}`;
    div.style.cssText = 'position:relative; width:100px; height:100px;';
    div.innerHTML = `
        <img id="img-${id}" src="${src}"
             style="width:100%;height:100%;object-fit:cover;border-radius:4px;border:1px solid #dee2e6;">
        ${loading ? `<div id="spinner-${id}"
                         style="position:absolute;inset:0;background:rgba(255,255,255,.7);
                                display:flex;align-items:center;justify-content:center;font-size:1.2rem;">
                         ⏳</div>` : ''}
        <button type="button" onclick="removePreview('${id}')"
                style="position:absolute;top:2px;right:2px;background:#dc3545;color:#fff;
                       border:none;border-radius:50%;width:20px;height:20px;
                       cursor:pointer;font-size:11px;line-height:1;padding:0;">✕</button>`;
    return div;
}

function removePreview(id) {
    document.getElementById(`preview-${id}`)?.remove();
}
```

---

## 10.9 Ajax 엔드포인트 테스트

8장에서 학습한 `@WebMvcTest`와 MockMvc로 Ajax API를 테스트합니다. 폼 기반 Controller 테스트와의 차이점은 **요청·응답 모두 JSON**을 사용한다는 점입니다.

### CommentApiController 테스트

```java
// src/test/java/com/example/imageboard/controller/api/CommentApiControllerTest.java
package com.example.imageboard.controller.api;

import com.example.imageboard.dto.CommentCreateRequest;
import com.example.imageboard.dto.CommentResponse;
import com.example.imageboard.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentApiController.class)
@DisplayName("CommentApiController 테스트")
class CommentApiControllerTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;   // 객체 ↔ JSON 변환
    @MockBean  CommentService commentService;

    // ── 댓글 목록 조회 ────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/boards/{id}/comments — 댓글 목록을 JSON 배열로 반환한다")
    @WithMockUser
    void getComments_returnsJsonList() throws Exception {
        // given
        List<CommentResponse> comments = List.of(
                CommentResponse.builder()
                        .id(1L).content("첫 번째 댓글").author("홍길동")
                        .authorId(1L).createdAt(LocalDateTime.now()).build(),
                CommentResponse.builder()
                        .id(2L).content("두 번째 댓글").author("이몽룡")
                        .authorId(2L).createdAt(LocalDateTime.now()).build()
        );
        given(commentService.findByBoardId(1L)).willReturn(comments);

        // when & then
        mockMvc.perform(get("/api/boards/1/comments")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].content").value("첫 번째 댓글"))
                .andExpect(jsonPath("$[0].author").value("홍길동"))
                .andDo(print());
    }

    @Test
    @DisplayName("GET — 댓글이 없으면 빈 배열을 반환한다")
    @WithMockUser
    void getComments_emptyList() throws Exception {
        given(commentService.findByBoardId(anyLong())).willReturn(List.of());

        mockMvc.perform(get("/api/boards/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── 댓글 등록 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST — 댓글 등록 성공 시 HTTP 201과 생성된 댓글을 반환한다")
    @WithMockUser
    void createComment_success() throws Exception {
        // given
        CommentCreateRequest request = new CommentCreateRequest();
        request.setContent("테스트 댓글입니다.");

        CommentResponse saved = CommentResponse.builder()
                .id(1L).content("테스트 댓글입니다.").author("testuser")
                .authorId(1L).createdAt(LocalDateTime.now()).build();

        given(commentService.create(anyLong(), any(), any())).willReturn(saved);

        // when & then
        mockMvc.perform(post("/api/boards/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)           // 요청 Content-Type
                        .content(objectMapper.writeValueAsString(request))  // 객체 → JSON 문자열
                        .with(csrf()))
                .andExpect(status().isCreated())           // HTTP 201
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.content").value("테스트 댓글입니다."));
    }

    @Test
    @DisplayName("POST — 내용이 비어 있으면 HTTP 400을 반환한다")
    @WithMockUser
    void createComment_blankContent() throws Exception {
        // given
        CommentCreateRequest request = new CommentCreateRequest();
        request.setContent("");   // @NotBlank 위반

        // when & then
        mockMvc.perform(post("/api/boards/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest());   // HTTP 400
    }

    // ── 댓글 삭제 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE — 삭제 성공 시 HTTP 204를 반환한다")
    @WithMockUser
    void deleteComment_success() throws Exception {
        willDoNothing().given(commentService).delete(anyLong(), any());

        mockMvc.perform(delete("/api/boards/1/comments/1").with(csrf()))
                .andExpect(status().isNoContent());    // HTTP 204, 응답 본문 없음
    }

    @Test
    @DisplayName("DELETE — 비로그인 상태에서 요청 시 HTTP 401을 반환한다")
    void deleteComment_unauthenticated() throws Exception {
        mockMvc.perform(delete("/api/boards/1/comments/1").with(csrf()))
                .andExpect(status().isUnauthorized());  // HTTP 401
    }
}
```

### jsonPath 표현식 참고

```java
// 최상위 필드
jsonPath("$.id").value(1)
jsonPath("$.content").value("내용")

// 배열
jsonPath("$.length()").value(3)            // 배열 크기
jsonPath("$[0].author").value("홍길동")    // 첫 번째 요소 필드
jsonPath("$[*].id").isArray()              // 모든 요소 id가 배열인지

// 중첩 객체
jsonPath("$.data.totalElements").value(10)

// 존재 여부
jsonPath("$.id").exists()
jsonPath("$.password").doesNotExist()       // 민감 정보 미포함 검증
```

---

## 10.10 에러 처리와 UX 고려사항

### @RestControllerAdvice — JSON 에러 응답 통일

7장의 `GlobalExceptionHandler`는 HTML 오류 페이지를 반환합니다. Ajax 요청 전용 컨트롤러에는 JSON 오류 응답이 필요합니다. `basePackages`로 적용 범위를 API 패키지로 한정합니다.

```java
// src/main/java/com/example/imageboard/controller/api/ApiExceptionHandler.java
package com.example.imageboard.controller.api;

import com.example.imageboard.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(basePackages = "com.example.imageboard.controller.api")
public class ApiExceptionHandler {

    /** 응답 형식 통일 헬퍼 */
    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(Map.of("status", status.value(), "message", message));
    }

    /** 404 — 엔티티 없음 */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException e) {
        log.warn("API 404: {}", e.getMessage());
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** 400 — 비즈니스 규칙 위반 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException e) {
        log.warn("API 400 [{}]: {}", e.getErrorCode(), e.getMessage());
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** 403 — 권한 없음 */
    @ExceptionHandler(NotBoardOwnerException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(NotBoardOwnerException e) {
        return error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    /** 400 — @Valid 검증 실패 (필드별 오류 목록 포함) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException e) {

        Map<String, String> fieldErrors = e.getBindingResult()
                .getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (a, b) -> a   // 같은 필드의 첫 번째 오류만
                ));

        return ResponseEntity.badRequest().body(Map.of(
                "status",  400,
                "message", "입력값을 확인해주세요.",
                "errors",  fieldErrors
        ));
    }

    /** 500 — 예상치 못한 예외 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("API 예상치 못한 예외", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }
}
```

오류 응답 JSON 예시:

```json
// 단순 오류
{ "status": 404, "message": "게시글을 찾을 수 없습니다. id=999" }

// @Valid 검증 오류 — 필드별 메시지 포함
{
    "status": 400,
    "message": "입력값을 확인해주세요.",
    "errors": {
        "content": "댓글 내용을 입력해주세요."
    }
}
```

### 두 핸들러의 공존

7장의 `GlobalExceptionHandler`(@ControllerAdvice)와 이 장의 `ApiExceptionHandler`(@RestControllerAdvice)는 **적용 범위가 겹치지 않으면** 함께 존재할 수 있습니다.

| 핸들러 | 적용 범위 | 오류 응답 형식 |
|---|---|---|
| `GlobalExceptionHandler` | `@ControllerAdvice` — 전체 | HTML 오류 페이지 |
| `ApiExceptionHandler` | `@RestControllerAdvice(basePackages=".api")` — API 패키지만 | JSON |

### 중복 요청 방지 패턴

버튼을 빠르게 여러 번 클릭하면 같은 요청이 중복 전송됩니다. 요청 중에 버튼을 비활성화합니다.

```javascript
/** 버튼 로딩 상태 전환 유틸리티 */
function setLoading(btn, isLoading) {
    btn.disabled = isLoading;
    // 원본 텍스트를 data-* 속성에 보관
    if (isLoading) {
        btn.dataset.original = btn.textContent;
        btn.textContent = '처리 중...';
    } else {
        btn.textContent = btn.dataset.original ?? btn.textContent;
    }
}

// 사용 예 — 댓글 등록 버튼
async function submitComment() {
    const btn = document.querySelector('#comment-form .btn');
    try {
        setLoading(btn, true);

        const content = document.getElementById('comment-input').value.trim();
        if (!content) { alert('내용을 입력해주세요.'); return; }

        await apiRequest(`/api/boards/${boardId}/comments`, {
            method: 'POST', body: JSON.stringify({ content }),
        });
        document.getElementById('comment-input').value = '';
        await loadComments();

    } catch (e) {
        alert(e.message);
    } finally {
        setLoading(btn, false);   // 성공·실패 모두 버튼 복원
    }
}
```

---

## 정리

| 핵심 개념 | 내용 |
|---|---|
| `@RestController` | 모든 메서드에 `@ResponseBody` 적용 — 반환 객체를 JSON으로 직렬화 |
| `ResponseEntity<T>` | HTTP 상태 코드·헤더·바디를 명시적으로 제어 |
| Fetch API | Promise·async/await 기반 비동기 HTTP 요청 — `XMLHttpRequest`의 현대적 대안 |
| CSRF + Ajax | 레이아웃 메타 태그에서 토큰 추출 → 공통 래퍼 함수로 모든 요청에 자동 포함 |
| `FormData` | 멀티파트 파일 전송 — `Content-Type` 헤더를 직접 설정하면 안 됨 |
| 디바운싱 | 연속 이벤트(키 입력)에서 마지막 발생 후 일정 시간 뒤에만 실행 |
| `@RestControllerAdvice` | API 패키지 전용 JSON 오류 응답 통일 (`@ControllerAdvice`와 병존 가능) |
| `jsonPath()` | MockMvc 테스트에서 JSON 응답 경로·값 검증 |
| `ObjectMapper` | `@WebMvcTest`에서 요청 바디를 JSON 문자열로 직렬화 |
| 중복 요청 방지 | 요청 중 버튼 비활성화 + `finally`에서 복원 |

---

## 연습 문제

1. 좋아요 수를 게시글 목록 화면(`/boards`)에도 표시해 보세요. `GET /api/boards/{boardId}/likes`를 활용합니다.
2. 댓글 내용 수정 기능(`PATCH /api/boards/{boardId}/comments/{commentId}`)을 추가하고, 댓글 클릭 시 인라인 textarea로 전환되어 수정할 수 있도록 구현해 보세요.
3. 자동완성 드롭다운에서 키보드 방향키(↑↓)와 Enter 키로 항목을 선택할 수 있도록 개선해 보세요.
4. `LikeApiController`에 대한 `@WebMvcTest` 테스트를 작성해 보세요. 로그인 상태에서 `POST /api/boards/1/likes`를 호출했을 때 `liked: true`, `likeCount: 1`이 반환되는지 검증합니다.
5. `CommentService.create()`에 대한 Mockito 단위 테스트를 작성해 보세요. 존재하지 않는 게시글 ID를 전달했을 때 `BoardNotFoundException`이 발생하는지 검증합니다.
6. 이미지 업로드 중 허용되지 않는 확장자 파일을 선택했을 때, 서버의 오류 응답을 받아 사용자에게 명확한 메시지를 보여주도록 `handleImageSelect` 함수를 개선해 보세요.
