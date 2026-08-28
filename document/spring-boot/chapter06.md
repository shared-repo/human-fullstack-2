# 6장. Spring Security — 로그인·권한 처리

---

## 학습 목표

- Spring Security의 인증·인가 흐름을 이해한다.
- `SecurityConfig`로 URL별 접근 권한을 설정할 수 있다.
- `UserDetailsService`를 구현하여 DB 기반 로그인을 연동할 수 있다.
- 회원가입 시 BCrypt로 비밀번호를 암호화할 수 있다.
- 폼 로그인과 로그아웃을 구현할 수 있다.
- `@PreAuthorize`로 게시글 작성자 본인만 수정·삭제할 수 있도록 제어할 수 있다.
- Thymeleaf에서 로그인 상태와 사용자 정보를 표시할 수 있다.

---

## 6.1 Spring Security 개요

### 인증(Authentication)과 인가(Authorization)

| 개념 | 의미 | 예시 |
|---|---|---|
| 인증 (Authentication) | 사용자가 누구인지 확인 | 로그인 |
| 인가 (Authorization) | 인증된 사용자가 무엇을 할 수 있는지 결정 | 관리자만 삭제 가능 |

### Spring Security 처리 흐름

Spring Security는 서블릿 필터 체인으로 동작합니다. HTTP 요청이 Controller에 도달하기 전에 필터 체인을 통과합니다.

```
HTTP 요청
    ↓
[FilterChain]
    ├─ SecurityContextPersistenceFilter  — 세션에서 인증 정보 복원
    ├─ UsernamePasswordAuthenticationFilter  — 로그인 폼 처리
    ├─ ExceptionTranslationFilter        — 인증·인가 예외 처리
    └─ FilterSecurityInterceptor         — URL 접근 권한 검사
    ↓
DispatcherServlet → Controller
```

### 의존성 추가

```groovy
// build.gradle
dependencies {
    // 기존 의존성 ...

    // Spring Security
    implementation 'org.springframework.boot:spring-boot-starter-security'

    // Thymeleaf + Spring Security 통합 (뷰에서 인증 정보 사용)
    implementation 'org.thymeleaf.extras:thymeleaf-extras-springsecurity6'
}
```

> Spring Security를 추가하는 순간 모든 URL이 로그인 페이지로 리다이렉트됩니다. `SecurityConfig`를 설정하기 전까지는 애플리케이션이 접근 불가 상태가 됩니다. 이어지는 6.2절의 설정을 반드시 함께 적용하세요.

---

## 6.2 SecurityConfig 설정

```java
// src/main/java/com/example/imageboard/config/SecurityConfig.java
package com.example.imageboard.config;

import com.example.imageboard.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity                          // Spring Security 활성화
@EnableMethodSecurity                       // @PreAuthorize 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    /** 비밀번호 암호화 인코더 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** HTTP 보안 설정 */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // URL별 접근 권한 설정
            .authorizeHttpRequests(auth -> auth
                // 로그인 없이 접근 가능
                .requestMatchers("/", "/boards", "/boards/{id}").permitAll()
                .requestMatchers("/members/login", "/members/register").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/thumbnails/**").permitAll()
                // 게시글 작성·수정·삭제는 로그인 필요
                .requestMatchers("/boards/create", "/boards/*/edit").authenticated()
                .requestMatchers("/boards", "/boards/**").authenticated()
                // 그 외 모든 요청은 로그인 필요
                .anyRequest().authenticated()
            )

            // 폼 로그인 설정
            .formLogin(form -> form
                .loginPage("/members/login")            // 커스텀 로그인 페이지
                .loginProcessingUrl("/members/login")   // 로그인 폼 action URL
                .defaultSuccessUrl("/boards", true)     // 로그인 성공 시 이동
                .failureUrl("/members/login?error=true") // 로그인 실패 시 이동
                .usernameParameter("username")          // 폼 필드명
                .passwordParameter("password")
                .permitAll()
            )

            // 로그아웃 설정
            .logout(logout -> logout
                .logoutUrl("/members/logout")
                .logoutSuccessUrl("/boards")
                .invalidateHttpSession(true)            // 세션 무효화
                .deleteCookies("JSESSIONID")            // 쿠키 삭제
                .permitAll()
            )

            // CSRF 설정 (기본 활성화 — 폼에 자동으로 _csrf 토큰이 삽입됨)
            // 필요 시 비활성화: .csrf(csrf -> csrf.disable())

            // 커스텀 UserDetailsService 등록
            .userDetailsService(userDetailsService);

        return http.build();
    }
}
```

### authorizeHttpRequests 패턴 우선순위

규칙은 **위에서 아래로** 순서대로 평가됩니다. 먼저 일치한 규칙이 적용되므로, 더 구체적인 규칙을 위에 배치합니다.

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/boards/create").authenticated()  // 더 구체적 → 위
    .requestMatchers("/boards/**").permitAll()           // 더 넓음 → 아래
)
```

---

## 6.3 회원 도메인 — UserDetails 연동

### CustomUserDetails

Spring Security는 인증 시 사용자 정보를 `UserDetails` 인터페이스로 관리합니다. `Member` 엔티티를 `UserDetails`로 변환하는 클래스를 만듭니다.

```java
// src/main/java/com/example/imageboard/security/CustomUserDetails.java
package com.example.imageboard.security;

import com.example.imageboard.entity.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Member member;    // 원본 엔티티 보관

    public CustomUserDetails(Member member) {
        this.member = member;
    }

    // 권한 목록 — 현재는 ROLE_USER 단일 권한
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() { return member.getPassword(); }

    @Override
    public String getUsername() { return member.getUsername(); }

    // 계정 상태 (현재는 모두 활성화)
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

    /** 편의 메서드 — Member ID 직접 접근 */
    public Long getMemberId() { return member.getId(); }

    /** 편의 메서드 — 닉네임 직접 접근 */
    public String getNickname() { return member.getNickname(); }
}
```

### CustomUserDetailsService

```java
// src/main/java/com/example/imageboard/security/CustomUserDetailsService.java
package com.example.imageboard.security;

import com.example.imageboard.entity.Member;
import com.example.imageboard.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    /**
     * 로그인 시 Spring Security가 호출합니다.
     * username으로 DB에서 회원을 조회하고 UserDetails로 반환합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "존재하지 않는 아이디입니다: " + username));

        return new CustomUserDetails(member);
    }
}
```

### MemberRepository — username 조회 추가

```java
// MemberRepository.java
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByUsername(String username);

    boolean existsByUsername(String username);   // 중복 아이디 확인용
}
```

---

## 6.4 회원가입 구현

### MemberCreateRequest DTO

```java
// src/main/java/com/example/imageboard/dto/MemberCreateRequest.java
package com.example.imageboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberCreateRequest {

    @NotBlank(message = "아이디를 입력해주세요.")
    @Size(min = 4, max = 20, message = "아이디는 4~20자 이내로 입력해주세요.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 영문자와 숫자만 사용할 수 있습니다.")
    private String username;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, message = "비밀번호는 8자 이상 입력해주세요.")
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    private String passwordConfirm;

    @NotBlank(message = "닉네임을 입력해주세요.")
    @Size(max = 20, message = "닉네임은 20자 이내로 입력해주세요.")
    private String nickname;

    /** 비밀번호 일치 확인 */
    public boolean isPasswordMatch() {
        return password != null && password.equals(passwordConfirm);
    }
}
```

### MemberService

```java
// src/main/java/com/example/imageboard/service/MemberService.java
package com.example.imageboard.service;

import com.example.imageboard.dto.MemberCreateRequest;
import com.example.imageboard.entity.Member;
import com.example.imageboard.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /** 회원가입 */
    @Transactional
    public void register(MemberCreateRequest request) {

        // 아이디 중복 확인
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        // 비밀번호 암호화 후 저장
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        Member member = Member.create(
                request.getUsername(),
                encodedPassword,         // 평문 ❌ → BCrypt 해시 ✅
                request.getNickname()
        );
        memberRepository.save(member);
    }
}
```

### MemberController

```java
// src/main/java/com/example/imageboard/controller/MemberController.java
package com.example.imageboard.controller;

import com.example.imageboard.dto.MemberCreateRequest;
import com.example.imageboard.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /** 로그인 폼 */
    @GetMapping("/login")
    public String loginForm(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        return "member/login";
    }

    /** 회원가입 폼 */
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("memberCreateRequest", new MemberCreateRequest());
        return "member/register";
    }

    /** 회원가입 처리 */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute MemberCreateRequest request,
                           BindingResult bindingResult,
                           Model model) {

        // Bean Validation 오류
        if (bindingResult.hasErrors()) {
            return "member/register";
        }

        // 비밀번호 불일치
        if (!request.isPasswordMatch()) {
            model.addAttribute("passwordMismatch", "비밀번호가 일치하지 않습니다.");
            return "member/register";
        }

        try {
            memberService.register(request);
        } catch (IllegalArgumentException e) {
            // 아이디 중복
            model.addAttribute("duplicateUsername", e.getMessage());
            return "member/register";
        }

        return "redirect:/members/login?registered=true";
    }
}
```

---

## 6.5 로그인·회원가입 화면

### 로그인 화면

```html
<!-- src/main/resources/templates/member/login.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">

<head th:replace="~{layout/default :: head('로그인')}"></head>

<body>

<nav th:replace="~{layout/default :: nav}"></nav>

<div class="container" style="max-width:420px; margin-top:60px;">
    <div class="board-detail">
        <h2 style="margin-bottom:24px;">로그인</h2>

        <!-- 로그인 성공 후 등록 안내 -->
        <div th:if="${param.registered}"
             style="background:#d1e7dd; color:#0a3622; padding:12px; border-radius:4px; margin-bottom:16px;">
            회원가입이 완료됐습니다. 로그인해 주세요.
        </div>

        <!-- 로그인 오류 메시지 -->
        <div th:if="${errorMessage}"
             style="background:#f8d7da; color:#842029; padding:12px; border-radius:4px; margin-bottom:16px;"
             th:text="${errorMessage}">
        </div>

        <!-- Spring Security가 /members/login POST로 처리 -->
        <form action="/members/login" method="post">
            <!-- CSRF 토큰 (Thymeleaf가 자동 삽입) -->
            <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">

            <div class="form-group">
                <label for="username">아이디</label>
                <input class="form-control" type="text" id="username"
                       name="username" placeholder="아이디를 입력하세요" autofocus>
            </div>

            <div class="form-group">
                <label for="password">비밀번호</label>
                <input class="form-control" type="password" id="password"
                       name="password" placeholder="비밀번호를 입력하세요">
            </div>

            <button class="btn btn-primary" type="submit" style="width:100%; margin-top:8px;">
                로그인
            </button>
        </form>

        <div style="text-align:center; margin-top:16px; color:#6c757d; font-size:0.9rem;">
            계정이 없으신가요?
            <a th:href="@{/members/register}" style="color:#0d6efd;">회원가입</a>
        </div>
    </div>
</div>

<footer th:replace="~{layout/default :: footer}"></footer>

</body>
</html>
```

### 회원가입 화면

```html
<!-- src/main/resources/templates/member/register.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">

<head th:replace="~{layout/default :: head('회원가입')}"></head>

<body>

<nav th:replace="~{layout/default :: nav}"></nav>

<div class="container" style="max-width:480px; margin-top:60px;">
    <div class="board-detail">
        <h2 style="margin-bottom:24px;">회원가입</h2>

        <form th:action="@{/members/register}" th:object="${memberCreateRequest}" method="post">

            <div class="form-group">
                <label for="username">아이디</label>
                <input class="form-control" type="text" id="username"
                       th:field="*{username}" placeholder="영문자·숫자 4~20자">
                <p class="error-message" th:if="${#fields.hasErrors('username')}"
                   th:errors="*{username}"></p>
                <p class="error-message" th:if="${duplicateUsername}"
                   th:text="${duplicateUsername}"></p>
            </div>

            <div class="form-group">
                <label for="password">비밀번호</label>
                <input class="form-control" type="password" id="password"
                       th:field="*{password}" placeholder="8자 이상">
                <p class="error-message" th:if="${#fields.hasErrors('password')}"
                   th:errors="*{password}"></p>
            </div>

            <div class="form-group">
                <label for="passwordConfirm">비밀번호 확인</label>
                <input class="form-control" type="password" id="passwordConfirm"
                       th:field="*{passwordConfirm}" placeholder="비밀번호를 다시 입력하세요">
                <p class="error-message" th:if="${passwordMismatch}"
                   th:text="${passwordMismatch}"></p>
            </div>

            <div class="form-group">
                <label for="nickname">닉네임</label>
                <input class="form-control" type="text" id="nickname"
                       th:field="*{nickname}" placeholder="20자 이내">
                <p class="error-message" th:if="${#fields.hasErrors('nickname')}"
                   th:errors="*{nickname}"></p>
            </div>

            <button class="btn btn-primary" type="submit" style="width:100%;">
                가입하기
            </button>
        </form>

        <div style="text-align:center; margin-top:16px; color:#6c757d; font-size:0.9rem;">
            이미 계정이 있으신가요?
            <a th:href="@{/members/login}" style="color:#0d6efd;">로그인</a>
        </div>
    </div>
</div>

<footer th:replace="~{layout/default :: footer}"></footer>

</body>
</html>
```

---

## 6.6 네비게이션 — 로그인 상태 표시

`thymeleaf-extras-springsecurity6`를 사용하면 뷰에서 인증 상태를 확인할 수 있습니다.

```html
<!-- layout/default.html — nav 프래그먼트 수정 -->
<!-- 네임스페이스 추가 -->
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">

<nav th:fragment="nav">
    <div class="container">
        <a class="brand" th:href="@{/}">📋 이미지 게시판</a>
        <div>
            <a th:href="@{/boards}">게시글 목록</a>
            &nbsp;|&nbsp;

            <!-- 로그인 상태일 때 -->
            <span sec:authorize="isAuthenticated()">
                <span sec:authentication="principal.nickname"
                      style="color:#adb5bd; font-size:0.9rem;"></span>님
                &nbsp;|&nbsp;
                <a th:href="@{/boards/create}">글쓰기</a>
                &nbsp;|&nbsp;
                <form th:action="@{/members/logout}" method="post"
                      style="display:inline;">
                    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
                    <button type="submit"
                            style="background:none; border:none; color:#fff; cursor:pointer;">
                        로그아웃
                    </button>
                </form>
            </span>

            <!-- 비로그인 상태일 때 -->
            <span sec:authorize="!isAuthenticated()">
                <a th:href="@{/members/login}">로그인</a>
                &nbsp;|&nbsp;
                <a th:href="@{/members/register}">회원가입</a>
            </span>
        </div>
    </div>
</nav>
```

### sec:authorize 주요 표현식

| 표현식 | 의미 |
|---|---|
| `isAuthenticated()` | 로그인한 사용자 |
| `!isAuthenticated()` | 비로그인 사용자 |
| `hasRole('ADMIN')` | ROLE_ADMIN 권한 보유 |
| `hasAnyRole('USER','ADMIN')` | USER 또는 ADMIN 권한 보유 |
| `principal.username` | 로그인 사용자의 username |
| `principal.nickname` | 로그인 사용자의 nickname (CustomUserDetails 필드) |

---

## 6.7 @PreAuthorize — 작성자 권한 제어

`@EnableMethodSecurity`가 활성화된 상태에서 `@PreAuthorize`를 메서드에 붙이면 Controller 호출 전에 권한을 검사합니다.

### 작성자 확인 로직

```java
// src/main/java/com/example/imageboard/security/BoardSecurityService.java
package com.example.imageboard.security;

import com.example.imageboard.entity.Board;
import com.example.imageboard.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("boardSecurity")   // @PreAuthorize에서 빈 이름으로 참조
@RequiredArgsConstructor
public class BoardSecurityService {

    private final BoardRepository boardRepository;

    /**
     * 현재 로그인 사용자가 해당 게시글의 작성자인지 확인합니다.
     *
     * @param boardId 게시글 ID
     * @param memberId 현재 로그인 사용자 ID
     * @return 작성자이면 true
     */
    public boolean isOwner(Long boardId, Long memberId) {
        return boardRepository.findById(boardId)
                .map(board -> board.getMember().getId().equals(memberId))
                .orElse(false);
    }
}
```

### Controller에 @PreAuthorize 적용

```java
// BoardController.java — 수정·삭제에 권한 검사 추가
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.imageboard.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/** 게시글 수정 폼 — 작성자 본인만 */
@GetMapping("/{id}/edit")
@PreAuthorize("@boardSecurity.isOwner(#id, #userDetails.memberId)")
public String editForm(@PathVariable Long id,
                       @AuthenticationPrincipal CustomUserDetails userDetails,
                       Model model) {
    BoardResponse board = boardService.findById(id);
    BoardCreateRequest request = new BoardCreateRequest();
    request.setTitle(board.getTitle());
    request.setContent(board.getContent());
    model.addAttribute("boardId", id);
    model.addAttribute("board", board);
    model.addAttribute("boardCreateRequest", request);
    return "board/edit";
}

/** 게시글 수정 처리 — 작성자 본인만 */
@PutMapping("/{id}")
@PreAuthorize("@boardSecurity.isOwner(#id, #userDetails.memberId)")
public String update(@PathVariable Long id,
                     @Valid @ModelAttribute BoardCreateRequest request,
                     BindingResult bindingResult,
                     @AuthenticationPrincipal CustomUserDetails userDetails) {
    if (bindingResult.hasErrors()) {
        return "board/edit";
    }
    boardService.update(id, request);
    return "redirect:/boards/" + id;
}

/** 게시글 삭제 — 작성자 본인만 */
@DeleteMapping("/{id}")
@PreAuthorize("@boardSecurity.isOwner(#id, #userDetails.memberId)")
public String delete(@PathVariable Long id,
                     @AuthenticationPrincipal CustomUserDetails userDetails) {
    boardService.delete(id);
    return "redirect:/boards";
}
```

### @AuthenticationPrincipal

`@AuthenticationPrincipal`은 현재 로그인한 사용자의 `UserDetails` 객체를 파라미터에 직접 주입합니다.

```java
// SecurityContext에서 직접 꺼내는 방식 (번거로움)
SecurityContext context = SecurityContextHolder.getContext();
CustomUserDetails userDetails = (CustomUserDetails) context.getAuthentication().getPrincipal();

// @AuthenticationPrincipal로 간결하게 주입 (권장)
public String method(@AuthenticationPrincipal CustomUserDetails userDetails) { ... }
```

---

## 6.8 BoardService — 임시 작성자를 로그인 사용자로 교체

5장까지 `memberId = 1L`로 고정했던 작성자를 실제 로그인 사용자로 교체합니다.

```java
// BoardService.java — create 메서드 수정
@Transactional
public Long create(BoardCreateRequest request, Long memberId) {  // memberId 파라미터 추가
    Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

    Board board = Board.create(request.getTitle(), request.getContent(), member);

    if (request.getImages() != null) {
        request.getImages().stream()
                .filter(file -> !file.isEmpty())
                .forEach(file -> {
                    String storedName = fileService.store(file);
                    fileService.createThumbnail(storedName);
                    AttachedImage image = AttachedImage.create(
                            file.getOriginalFilename(), storedName,
                            "/images/" + storedName, file.getSize());
                    board.addImage(image);
                });
    }

    boardRepository.save(board);
    return board.getId();
}
```

```java
// BoardController.java — create 메서드에 로그인 사용자 전달
@PostMapping
public String create(@Valid @ModelAttribute BoardCreateRequest request,
                     BindingResult bindingResult,
                     @AuthenticationPrincipal CustomUserDetails userDetails) {
    if (bindingResult.hasErrors()) {
        return "board/create";
    }
    Long id = boardService.create(request, userDetails.getMemberId()); // 로그인 사용자 ID 전달
    return "redirect:/boards/" + id;
}
```

---

## 6.9 상세 화면 — 수정·삭제 버튼 조건부 표시

본인이 작성한 게시글에만 수정·삭제 버튼을 표시합니다.

```html
<!-- templates/board/detail.html — 하단 버튼 수정 -->
<div style="margin-top: 16px; display:flex; gap:8px;">
    <a class="btn btn-secondary" th:href="@{/boards}">목록</a>

    <!-- 로그인 사용자 = 작성자일 때만 표시 -->
    <th:block sec:authorize="isAuthenticated()">
        <th:block th:if="${#authentication.principal.memberId == board.memberId}">
            <a class="btn btn-primary"
               th:href="@{/boards/{id}/edit(id=${board.id})}">수정</a>
            <form th:action="@{/boards/{id}(id=${board.id})}" method="post"
                  onsubmit="return confirm('삭제하시겠습니까?')">
                <input type="hidden" name="_method" value="DELETE">
                <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
                <button class="btn btn-danger" type="submit">삭제</button>
            </form>
        </th:block>
    </th:block>
</div>
```

`BoardResponse`에 `memberId` 필드를 추가합니다.

```java
// BoardResponse.java — memberId 추가
@Getter
@Builder
public class BoardResponse {
    private Long id;
    private Long memberId;       // 추가 — 작성자 ID (뷰에서 권한 비교용)
    private String title;
    private String content;
    private String author;
    // ... 나머지 필드
}
```

`toResponse()` 메서드에도 `memberId` 설정을 추가합니다.

```java
private BoardResponse toResponse(Board board) {
    return BoardResponse.builder()
            .id(board.getId())
            .memberId(board.getMember().getId())  // 추가
            // ... 나머지 필드
            .build();
}
```

---

## 6.10 DataInitializer 수정 — 비밀번호 암호화 적용

기존에 평문으로 저장하던 테스트 회원의 비밀번호를 BCrypt로 암호화합니다.

```java
// DataInitializer.java 수정
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;  // 추가

    @Override
    public void run(ApplicationArguments args) {
        if (memberRepository.count() == 0) {
            memberRepository.save(
                Member.create(
                    "admin",
                    passwordEncoder.encode("password123"),   // 암호화 적용
                    "관리자"
                )
            );
        }
    }
}
```

---

## 6.11 BCrypt 비밀번호 암호화 이해

```java
PasswordEncoder encoder = new BCryptPasswordEncoder();

// 암호화 — 같은 평문이라도 실행할 때마다 다른 해시값 생성 (salt 포함)
String hash1 = encoder.encode("password123");
String hash2 = encoder.encode("password123");
System.out.println(hash1.equals(hash2));  // false

// 검증 — 평문과 해시를 비교 (Spring Security 내부에서 자동 처리)
boolean matches = encoder.matches("password123", hash1);  // true
```

BCrypt는 단방향 해시 함수입니다. 저장된 해시에서 원래 비밀번호를 역산하는 것이 사실상 불가능합니다. 비밀번호를 DB에 **절대 평문으로 저장하지 마세요.**

---

## 정리

| 핵심 개념 | 내용 |
|---|---|
| `SecurityFilterChain` | URL별 접근 권한, 로그인·로그아웃 설정 |
| `UserDetailsService` | DB에서 사용자를 조회해 `UserDetails`로 반환 |
| `BCryptPasswordEncoder` | 비밀번호 단방향 암호화 |
| `@AuthenticationPrincipal` | 현재 로그인 사용자 정보를 메서드 파라미터에 주입 |
| `@PreAuthorize` | 메서드 호출 전 SpEL 표현식으로 권한 검사 |
| `sec:authorize` | Thymeleaf에서 로그인 상태·권한에 따른 조건부 렌더링 |
| CSRF 토큰 | POST 폼에 자동 삽입되는 위조 요청 방지 토큰 |

---

## 연습 문제

1. 로그인하지 않은 상태에서 `/boards/create`에 직접 접근하면 어떤 URL로 리다이렉트되는지 확인해 보세요.
2. 본인이 작성하지 않은 게시글의 수정 URL(`/boards/{id}/edit`)에 직접 접근했을 때 어떻게 동작하는지 확인해 보세요.
3. 회원가입 후 로그인하여 게시글을 작성하고, 다른 계정으로 로그인했을 때 수정·삭제 버튼이 표시되지 않는지 확인해 보세요.
4. `sec:authorize="hasRole('ADMIN')"` 조건을 추가하고, `Member` 엔티티에 역할(role) 필드를 추가하여 관리자 전용 기능을 구현해 보세요.

---

## 다음 장 예고

7장에서는 예외 처리와 검증을 정교하게 다듬습니다. `@ControllerAdvice`로 전역 예외 처리를 구성하고, Bean Validation 메시지를 커스터마이즈하며, 커스텀 예외 클래스를 설계합니다.
