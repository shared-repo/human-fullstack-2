# 7장. 예외 처리 및 검증

---

## 학습 목표

- Bean Validation 어노테이션의 동작 원리와 주요 제약 조건을 이해한다.
- 커스텀 제약 어노테이션을 직접 만들 수 있다.
- `BindingResult`를 활용해 폼 검증 오류를 뷰에 전달할 수 있다.
- 계층적 커스텀 예외 구조를 설계할 수 있다.
- `@ControllerAdvice`로 애플리케이션 전역 예외를 일관되게 처리할 수 있다.
- 사용자 친화적인 오류 페이지를 구성할 수 있다.
- `LocaleChangeInterceptor`와 `LocaleResolver`를 조합해 지역별 동적 언어 전환을 구현할 수 있다.

---

## 7.1 검증의 두 단계

이미지 게시판에서 사용자 입력을 검증하는 시점은 두 곳입니다.

```
[사용자 입력]
      ↓
① 웹 계층 검증 (Bean Validation)
   — 형식 검사: 빈 값, 길이, 형식 등
   — BindingResult로 뷰에 오류 전달
      ↓
② 비즈니스 계층 검증 (Service)
   — 도메인 규칙 검사: 중복 아이디, 작성자 권한 등
   — 커스텀 예외로 던지고 @ControllerAdvice에서 처리
```

두 계층의 역할을 명확히 분리하면 코드 가독성과 유지보수성이 높아집니다.

---

## 7.2 Bean Validation 심화

### 주요 제약 어노테이션

```java
public class BoardCreateRequest {

    // 문자열
    @NotNull              // null 불가
    @NotEmpty             // null 및 빈 문자열("") 불가
    @NotBlank             // null, 빈 문자열, 공백만 있는 문자열 불가 (문자열 검증 권장)
    @Size(min=1, max=100) // 길이 범위
    @Pattern(regexp="...")// 정규식 패턴
    @Email                // 이메일 형식
    private String title;

    // 숫자
    @Min(0)               // 최솟값
    @Max(100)             // 최댓값
    @Positive             // 양수
    @PositiveOrZero       // 0 이상
    @Digits(integer=5, fraction=2) // 정수 5자리, 소수 2자리 이내
    private int viewCount;

    // 날짜
    @Future               // 미래 날짜
    @Past                 // 과거 날짜
    @FutureOrPresent      // 현재 또는 미래
    private LocalDateTime scheduledAt;
}
```

### @NotNull vs @NotEmpty vs @NotBlank

| 어노테이션 | null | `""` | `" "` | `"hello"` |
|---|---|---|---|---|
| `@NotNull` | ❌ 실패 | ✅ 통과 | ✅ 통과 | ✅ 통과 |
| `@NotEmpty` | ❌ 실패 | ❌ 실패 | ✅ 통과 | ✅ 통과 |
| `@NotBlank` | ❌ 실패 | ❌ 실패 | ❌ 실패 | ✅ 통과 |

문자열 필드에는 `@NotBlank`가 가장 엄격하고 일반적으로 권장됩니다.

### 검증 그룹 (Validation Groups)

같은 DTO를 등록·수정에 공통으로 사용할 때, 상황에 따라 다른 검증 규칙을 적용하고 싶을 때 그룹을 활용합니다.

```java
// 검증 그룹 마커 인터페이스 정의
public class ValidationGroups {
    public interface Create {}  // 등록 시 사용
    public interface Update {}  // 수정 시 사용
}
```

```java
// DTO에 그룹 지정
@Getter
@Setter
public class BoardCreateRequest {

    @NotBlank(groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    @Size(max = 100, groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    private String title;

    @NotBlank(groups = ValidationGroups.Create.class)  // 등록 시에만 필수
    private String content;

    // 등록 시에만 작성자 검증
    @NotNull(groups = ValidationGroups.Create.class)
    private Long memberId;
}
```

```java
// Controller에서 @Validated로 그룹 지정
import org.springframework.validation.annotation.Validated;

@PostMapping
public String create(@Validated(ValidationGroups.Create.class) @ModelAttribute BoardCreateRequest request,
                     BindingResult bindingResult) { ... }

@PutMapping("/{id}")
public String update(@Validated(ValidationGroups.Update.class) @ModelAttribute BoardCreateRequest request,
                     BindingResult bindingResult) { ... }
```

> `@Valid`는 그룹을 지원하지 않습니다. 검증 그룹을 사용할 때는 `@Validated`를 사용합니다.

---

## 7.3 커스텀 제약 어노테이션

Bean Validation이 제공하지 않는 규칙은 직접 어노테이션을 만들 수 있습니다. 이미지 게시판에서 허용된 이미지 확장자를 검증하는 어노테이션을 만들어봅니다.

### 어노테이션 정의

```java
// src/main/java/com/example/imageboard/validation/AllowedImageExtension.java
package com.example.imageboard.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AllowedImageExtensionValidator.class) // 검증 로직 클래스 지정
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedImageExtension {

    String message() default "허용되지 않는 이미지 형식입니다. (jpg, jpeg, png, gif, webp만 허용)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    // 허용 확장자 목록 (기본값 설정)
    String[] allowed() default {"jpg", "jpeg", "png", "gif", "webp"};
}
```

### 검증 로직 구현

```java
// src/main/java/com/example/imageboard/validation/AllowedImageExtensionValidator.java
package com.example.imageboard.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

public class AllowedImageExtensionValidator
        implements ConstraintValidator<AllowedImageExtension, MultipartFile> {

    private List<String> allowedExtensions;

    @Override
    public void initialize(AllowedImageExtension annotation) {
        // 어노테이션에 선언된 허용 확장자 목록 초기화
        this.allowedExtensions = Arrays.asList(annotation.allowed());
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        // 파일이 없으면 검증 통과 (필수 여부는 @NotNull로 별도 처리)
        if (file == null || file.isEmpty()) {
            return true;
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return false;
        }

        String extension = originalFilename
                .substring(originalFilename.lastIndexOf('.') + 1)
                .toLowerCase();

        return allowedExtensions.contains(extension);
    }
}
```

### 커스텀 어노테이션 적용

```java
// BoardCreateRequest.java — 이미지 검증 적용
@Getter
@Setter
public class BoardCreateRequest {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 100, message = "제목은 100자 이내로 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    // 각 이미지 파일에 커스텀 검증 적용
    @Valid  // List 내부 요소에도 검증 적용
    private List<@AllowedImageExtension MultipartFile> images;
}
```

---

## 7.4 BindingResult와 Thymeleaf 오류 바인딩

### BindingResult 기본 사용

```java
@PostMapping
public String create(@Valid @ModelAttribute BoardCreateRequest request,
                     BindingResult bindingResult,  // 반드시 @Valid 직후에 위치
                     Model model) {

    if (bindingResult.hasErrors()) {
        // 오류가 있으면 폼으로 다시 이동 (Model에 오류 정보 자동 포함)
        return "board/create";
    }
    // ...
}
```

> `BindingResult`는 반드시 검증 대상 파라미터 바로 뒤에 선언해야 합니다. 순서가 틀리면 `MethodArgumentNotValidException`이 발생합니다.

### BindingResult 주요 메서드

```java
bindingResult.hasErrors()                  // 오류 존재 여부
bindingResult.getErrorCount()              // 전체 오류 수
bindingResult.hasFieldErrors("title")      // 특정 필드 오류 여부
bindingResult.getFieldError("title")       // 특정 필드의 첫 번째 오류
bindingResult.getAllErrors()               // 모든 오류 목록

// 코드로 오류 직접 추가
bindingResult.rejectValue("title",         // 필드명
                          "size.exceeded", // 오류 코드
                          "제목이 너무 깁니다."); // 기본 메시지
```

### 서비스 계층 검증 결과를 BindingResult에 추가

```java
@PostMapping("/register")
public String register(@Valid @ModelAttribute MemberCreateRequest request,
                       BindingResult bindingResult) {

    // ① Bean Validation 오류 확인
    if (bindingResult.hasErrors()) {
        return "member/register";
    }

    // ② 비밀번호 일치 확인 (도메인 규칙)
    if (!request.isPasswordMatch()) {
        bindingResult.rejectValue("passwordConfirm", "mismatch", "비밀번호가 일치하지 않습니다.");
        return "member/register";
    }

    // ③ 아이디 중복 확인 (DB 조회)
    try {
        memberService.register(request);
    } catch (DuplicateUsernameException e) {
        bindingResult.rejectValue("username", "duplicate", e.getMessage());
        return "member/register";
    }

    return "redirect:/members/login?registered=true";
}
```

### Thymeleaf 오류 메시지 바인딩

```html
<!-- 방법 1: 특정 필드 오류 메시지 -->
<input th:field="*{title}" class="form-control">
<p th:if="${#fields.hasErrors('title')}"
   th:errors="*{title}"
   class="error-message">제목 오류</p>

<!-- 방법 2: 오류가 있을 때 input에 클래스 추가 -->
<input th:field="*{title}"
       th:class="${#fields.hasErrors('title')} ? 'form-control error-border' : 'form-control'">

<!-- 방법 3: 모든 오류를 한 곳에 표시 -->
<div th:if="${#fields.hasAnyErrors()}"
     style="background:#f8d7da; padding:12px; border-radius:4px; margin-bottom:16px;">
    <ul style="margin:0; padding-left:20px;">
        <li th:each="error : ${#fields.allErrors()}"
            th:text="${error}"></li>
    </ul>
</div>

<!-- 방법 4: 전역 오류 (특정 필드에 묶이지 않는 오류) -->
<div th:if="${#fields.hasGlobalErrors()}">
    <p th:each="error : ${#fields.globalErrors()}"
       th:text="${error}"
       class="error-message"></p>
</div>
```

### 오류 메시지 커스터마이즈 — messages.properties

`src/main/resources/messages.properties` 파일을 생성하면 오류 메시지를 코드 기반으로 관리할 수 있습니다.

```properties
# src/main/resources/messages.properties

# Bean Validation 기본 메시지 오버라이드
# 형식: {어노테이션 단순명}.{객체명}.{필드명} 또는 {어노테이션 단순명}.{필드명} 또는 {어노테이션 단순명}
NotBlank.boardCreateRequest.title=게시글 제목은 필수 입력 항목입니다.
NotBlank.boardCreateRequest.content=게시글 내용은 필수 입력 항목입니다.
Size.boardCreateRequest.title=제목은 {2}자 이상 {1}자 이하로 입력해주세요.

NotBlank.memberCreateRequest.username=아이디를 입력해주세요.
Size.memberCreateRequest.username=아이디는 {2}~{1}자 이내로 입력해주세요.
Pattern.memberCreateRequest.username=아이디는 영문자와 숫자만 사용할 수 있습니다.
NotBlank.memberCreateRequest.password=비밀번호를 입력해주세요.
Size.memberCreateRequest.password=비밀번호는 {2}자 이상 입력해주세요.

# 커스텀 오류 코드
mismatch.memberCreateRequest.passwordConfirm=비밀번호가 일치하지 않습니다.
duplicate.memberCreateRequest.username=이미 사용 중인 아이디입니다.
```

```java
// MessageSource 설정 — application.yml에 추가
spring:
  messages:
    basename: messages          # messages.properties 파일 위치
    encoding: UTF-8
    cache-duration: 3600        # 운영 환경: 1시간 캐시
```

---

## 7.5 커스텀 예외 클래스 설계

### 예외 계층 구조

애플리케이션 전용 최상위 예외를 정의하고, 상황별 예외를 하위 클래스로 구성합니다.

```
RuntimeException (JDK)
    └── ImageboardException          ← 애플리케이션 최상위 예외
            ├── EntityNotFoundException      ← 조회 대상 없음 (404)
            │       ├── BoardNotFoundException
            │       └── MemberNotFoundException
            ├── BusinessException           ← 비즈니스 규칙 위반 (400)
            │       ├── DuplicateUsernameException
            │       └── FileUploadException
            └── AccessDeniedException       ← 권한 없음 (403)
                    └── NotBoardOwnerException
```

### 최상위 예외 클래스

```java
// src/main/java/com/example/imageboard/exception/ImageboardException.java
package com.example.imageboard.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ImageboardException extends RuntimeException {

    private final HttpStatus status;        // HTTP 상태 코드
    private final String errorCode;         // 클라이언트 식별용 오류 코드

    public ImageboardException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
```

### 하위 예외 클래스

```java
// EntityNotFoundException.java — 조회 실패 (HTTP 404)
package com.example.imageboard.exception;

import org.springframework.http.HttpStatus;

public class EntityNotFoundException extends ImageboardException {

    public EntityNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "ENTITY_NOT_FOUND");
    }
}
```

```java
// BoardNotFoundException.java
package com.example.imageboard.exception;

public class BoardNotFoundException extends EntityNotFoundException {

    public BoardNotFoundException(Long id) {
        super("게시글을 찾을 수 없습니다. id=" + id);
    }
}
```

```java
// MemberNotFoundException.java
package com.example.imageboard.exception;

public class MemberNotFoundException extends EntityNotFoundException {

    public MemberNotFoundException(Long id) {
        super("회원을 찾을 수 없습니다. id=" + id);
    }

    public MemberNotFoundException(String username) {
        super("회원을 찾을 수 없습니다. username=" + username);
    }
}
```

```java
// BusinessException.java — 비즈니스 규칙 위반 (HTTP 400)
package com.example.imageboard.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends ImageboardException {

    public BusinessException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "BUSINESS_ERROR");
    }

    public BusinessException(String message, String errorCode) {
        super(message, HttpStatus.BAD_REQUEST, errorCode);
    }
}
```

```java
// DuplicateUsernameException.java
package com.example.imageboard.exception;

public class DuplicateUsernameException extends BusinessException {

    public DuplicateUsernameException(String username) {
        super("이미 사용 중인 아이디입니다: " + username, "DUPLICATE_USERNAME");
    }
}
```

```java
// FileUploadException.java
package com.example.imageboard.exception;

public class FileUploadException extends BusinessException {

    public FileUploadException(String message) {
        super(message, "FILE_UPLOAD_ERROR");
    }
}
```

```java
// NotBoardOwnerException.java — 권한 없음 (HTTP 403)
package com.example.imageboard.exception;

import org.springframework.http.HttpStatus;

public class NotBoardOwnerException extends ImageboardException {

    public NotBoardOwnerException() {
        super("게시글 작성자만 수정·삭제할 수 있습니다.", HttpStatus.FORBIDDEN, "NOT_BOARD_OWNER");
    }
}
```

### Service에서 커스텀 예외 사용

```java
// BoardService.java — 수정 전후 비교

// ❌ 수정 전 — 표준 예외 사용
public BoardResponse findById(Long id) {
    Board board = boardRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
    ...
}

// ✅ 수정 후 — 커스텀 예외 사용
public BoardResponse findById(Long id) {
    Board board = boardRepository.findById(id)
            .orElseThrow(() -> new BoardNotFoundException(id));
    ...
}
```

```java
// MemberService.java — 커스텀 예외 적용
@Transactional
public void register(MemberCreateRequest request) {
    if (memberRepository.existsByUsername(request.getUsername())) {
        throw new DuplicateUsernameException(request.getUsername());
    }
    // ...
}
```

---

## 7.6 @ControllerAdvice 전역 예외 처리

`@ControllerAdvice`는 모든 Controller에서 발생하는 예외를 한 곳에서 처리합니다.

```java
// src/main/java/com/example/imageboard/controller/GlobalExceptionHandler.java
package com.example.imageboard.controller;

import com.example.imageboard.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 404 — 조회 대상 없음
     * 게시글·회원 등 엔티티를 찾지 못했을 때
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public String handleEntityNotFound(EntityNotFoundException e, Model model) {
        log.warn("EntityNotFoundException: {}", e.getMessage());
        model.addAttribute("status", 404);
        model.addAttribute("message", e.getMessage());
        return "error/404";
    }

    /**
     * 400 — 비즈니스 규칙 위반
     * 중복 아이디, 잘못된 파일 형식 등
     */
    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException e,
                                 RedirectAttributes redirectAttributes) {
        log.warn("BusinessException: [{}] {}", e.getErrorCode(), e.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/error/400";
    }

    /**
     * 403 — 권한 없음
     * 작성자가 아닌 사용자의 수정·삭제 시도
     */
    @ExceptionHandler(NotBoardOwnerException.class)
    public String handleNotOwner(NotBoardOwnerException e, Model model) {
        log.warn("NotBoardOwnerException: {}", e.getMessage());
        model.addAttribute("status", 403);
        model.addAttribute("message", e.getMessage());
        return "error/403";
    }

    /**
     * 파일 크기 초과
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(MaxUploadSizeExceededException e,
                                      RedirectAttributes redirectAttributes) {
        log.warn("파일 크기 초과: {}", e.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage",
                "파일 크기가 너무 큽니다. 파일당 최대 10MB까지 업로드할 수 있습니다.");
        return "redirect:/boards/create";
    }

    /**
     * 처리되지 않은 모든 예외 (최후 보루)
     */
    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        log.error("예상치 못한 예외 발생", e);
        model.addAttribute("status", 500);
        model.addAttribute("message", "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        return "error/500";
    }
}
```

### @ExceptionHandler 적용 범위

```java
// @ControllerAdvice — 모든 Controller에 적용 (전역)
@ControllerAdvice
public class GlobalExceptionHandler { ... }

// 특정 패키지로 범위 제한
@ControllerAdvice("com.example.imageboard.controller")
public class GlobalExceptionHandler { ... }

// 특정 Controller 클래스로 범위 제한
@ControllerAdvice(assignableTypes = {BoardController.class, MemberController.class})
public class GlobalExceptionHandler { ... }

// Controller 안에서 해당 Controller에만 적용
@Controller
public class BoardController {
    @ExceptionHandler(BoardNotFoundException.class)
    public String handleBoardNotFound(...) { ... }
}
```

---

## 7.7 오류 페이지 구성

### Thymeleaf 오류 페이지

```html
<!-- src/main/resources/templates/error/404.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">

<head th:replace="~{layout/default :: head('페이지를 찾을 수 없습니다')}"></head>

<body>

<nav th:replace="~{layout/default :: nav}"></nav>

<div class="container" style="text-align:center; padding:80px 0;">
    <div style="font-size:4rem; margin-bottom:16px;">🔍</div>
    <h2 style="font-size:2rem; margin-bottom:12px;">404</h2>
    <p style="color:#6c757d; margin-bottom:8px;">페이지를 찾을 수 없습니다.</p>
    <p style="color:#adb5bd; font-size:0.9rem; margin-bottom:32px;"
       th:text="${message}">요청하신 게시글이 존재하지 않습니다.</p>
    <a class="btn btn-primary" th:href="@{/boards}">게시글 목록으로</a>
</div>

<footer th:replace="~{layout/default :: footer}"></footer>

</body>
</html>
```

```html
<!-- src/main/resources/templates/error/403.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">

<head th:replace="~{layout/default :: head('접근 권한 없음')"></head>

<body>

<nav th:replace="~{layout/default :: nav}"></nav>

<div class="container" style="text-align:center; padding:80px 0;">
    <div style="font-size:4rem; margin-bottom:16px;">🚫</div>
    <h2 style="font-size:2rem; margin-bottom:12px;">403</h2>
    <p style="color:#6c757d; margin-bottom:8px;">접근 권한이 없습니다.</p>
    <p style="color:#adb5bd; font-size:0.9rem; margin-bottom:32px;"
       th:text="${message}">이 작업을 수행할 권한이 없습니다.</p>
    <a class="btn btn-secondary" th:href="@{/boards}">게시글 목록으로</a>
</div>

<footer th:replace="~{layout/default :: footer}"></footer>

</body>
</html>
```

```html
<!-- src/main/resources/templates/error/500.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">

<head th:replace="~{layout/default :: head('서버 오류')}"></head>

<body>

<nav th:replace="~{layout/default :: nav}"></nav>

<div class="container" style="text-align:center; padding:80px 0;">
    <div style="font-size:4rem; margin-bottom:16px;">⚠️</div>
    <h2 style="font-size:2rem; margin-bottom:12px;">500</h2>
    <p style="color:#6c757d; margin-bottom:8px;">서버 내부 오류가 발생했습니다.</p>
    <p style="color:#adb5bd; font-size:0.9rem; margin-bottom:32px;"
       th:text="${message}">잠시 후 다시 시도해 주세요.</p>
    <a class="btn btn-secondary" th:href="@{/boards}">게시글 목록으로</a>
</div>

<footer th:replace="~{layout/default :: footer}"></footer>

</body>
</html>
```

### Spring Boot 기본 오류 페이지 설정

`src/main/resources/templates/error/` 폴더에 HTTP 상태 코드와 일치하는 파일을 두면 Spring Boot가 자동으로 연결합니다. `@ControllerAdvice`를 거치지 않은 예외도 이 페이지로 이동합니다.

```
templates/error/
├── 400.html    ← Bad Request
├── 403.html    ← Forbidden
├── 404.html    ← Not Found
└── 500.html    ← Internal Server Error
```

```yaml
# application.yml — 오류 페이지 설정
server:
  error:
    whitelabel:
      enabled: false   # Spring Boot 기본 Whitelabel 오류 페이지 비활성화
    path: /error
```

---

## 7.8 로깅 전략

예외 처리와 함께 로그 레벨을 적절히 설정하면 운영 중 문제를 빠르게 파악할 수 있습니다.

### Lombok @Slf4j 활용

```java
@Slf4j
@Service
public class BoardService {

    public BoardResponse findById(Long id) {
        log.debug("게시글 조회 요청: id={}", id);   // 개발 환경에서 상세 추적

        Board board = boardRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("게시글 없음: id={}", id);  // 예상 가능한 오류
                    return new BoardNotFoundException(id);
                });

        log.info("게시글 조회 성공: id={}, title={}", id, board.getTitle());
        return toResponse(board);
    }
}
```

### 로그 레벨 기준

| 레벨 | 사용 상황 |
|---|---|
| `ERROR` | 즉시 대응이 필요한 심각한 오류 (예: DB 연결 실패) |
| `WARN` | 오류이지만 서비스는 계속 가능 (예: 존재하지 않는 게시글 조회) |
| `INFO` | 주요 비즈니스 흐름 기록 (예: 회원가입, 게시글 저장) |
| `DEBUG` | 개발 중 상세 흐름 추적 (운영 환경에서 비활성화) |

```yaml
# application-dev.yml
logging:
  level:
    com.example.imageboard: DEBUG  # 개발: 전체 DEBUG

# application-prod.yml
logging:
  level:
    com.example.imageboard: INFO   # 운영: INFO 이상만
    com.example.imageboard.controller.GlobalExceptionHandler: WARN
```

---

## 7.9 국제화(i18n) — 지역별 동적 언어 전환

7.4에서 `messages.properties`로 검증 오류 메시지를 관리했습니다. 이 구조를 그대로 활용하면 애플리케이션 전체 텍스트를 로케일(언어·지역)에 따라 동적으로 바꿀 수 있습니다.

### 국제화 흐름

```
[HTTP 요청]
      ↓
LocaleChangeInterceptor — ?lang=en 파라미터 감지
      ↓
LocaleResolver — 로케일 결정 (세션 / 쿠키 / Accept-Language)
      ↓
MessageSource — 로케일에 맞는 messages_en.properties 조회
      ↓
Thymeleaf #{...} — 메시지 렌더링
```

### 다국어 메시지 파일 분리

7.4의 단일 `messages.properties`를 언어별로 분리합니다.

```
src/main/resources/
├── messages.properties           ← 기본 (언어 미지정 시 fallback)
├── messages_ko.properties        ← 한국어
└── messages_en.properties        ← 영어
```

```properties
# messages_ko.properties
board.list.title=게시판
board.create.title=게시글 작성
board.create.submit=등록
board.search.placeholder=제목 또는 내용을 검색하세요

member.login.title=로그인
member.login.submit=로그인
member.register.title=회원가입

# 기존 검증 오류 메시지도 언어별로 분리
NotBlank.boardCreateRequest.title=게시글 제목은 필수 입력 항목입니다.
NotBlank.memberCreateRequest.username=아이디를 입력해주세요.
```

```properties
# messages_en.properties
board.list.title=Board
board.create.title=Write Post
board.create.submit=Submit
board.search.placeholder=Search by title or content

member.login.title=Login
member.login.submit=Login
member.register.title=Sign Up

NotBlank.boardCreateRequest.title=Title is required.
NotBlank.memberCreateRequest.username=Username is required.
```

### LocaleResolver 설정

`LocaleResolver`는 현재 요청의 로케일을 결정합니다. Spring Boot 기본값인 `AcceptHeaderLocaleResolver` 대신, 사용자가 선택한 언어를 기억하는 `SessionLocaleResolver`를 등록합니다.

```java
// src/main/java/com/example/imageboard/config/WebConfig.java
package com.example.imageboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 세션에 로케일을 저장 — 브라우저를 닫을 때까지 유지
     * 기본 로케일: 한국어
     */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(Locale.KOREAN);
        return resolver;
    }

    /**
     * ?lang=en 같은 URL 파라미터로 로케일 전환
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");  // ?lang=en, ?lang=ko
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
```

### LocaleResolver 전략 비교

| 전략 | 저장 위치 | 유지 기간 | 사용 상황 |
|---|---|---|---|
| `AcceptHeaderLocaleResolver` | 브라우저 헤더 | 요청마다 재결정 | REST API, 언어 변경 불필요 |
| `SessionLocaleResolver` | 서버 세션 | 세션 종료 시까지 | 로그인 기반 웹 애플리케이션 |
| `CookieLocaleResolver` | 클라이언트 쿠키 | 쿠키 만료 시까지 | 비로그인 사용자도 언어 기억 필요 |

> `AcceptHeaderLocaleResolver`는 읽기 전용이라 `LocaleChangeInterceptor`와 함께 사용할 수 없습니다. URL 파라미터로 언어 전환이 필요하다면 반드시 `SessionLocaleResolver` 또는 `CookieLocaleResolver`를 직접 등록해야 합니다.

### Thymeleaf #{} 표현식

메시지 키를 `#{...}`로 참조하면 현재 로케일에 맞는 값이 자동으로 출력됩니다.

```html
<!-- 기존: 하드코딩 -->
<h1>게시판</h1>
<button>등록</button>

<!-- 변경: #{메시지 키}로 교체 -->
<h1 th:text="#{board.list.title}">게시판</h1>
<button th:text="#{board.create.submit}">등록</button>

<!-- 메시지에 파라미터 삽입 -->
<!-- messages_ko.properties: board.count=전체 {0}개의 게시글 -->
<p th:text="#{board.count(${totalCount})}">전체 0개의 게시글</p>
```

### 언어 전환 링크

```html
<!-- 네비게이션 바 등에 언어 선택 버튼 추가 -->
<!-- 현재 URL에 ?lang= 파라미터를 붙여 LocaleChangeInterceptor가 감지하도록 함 -->
<a th:href="@{''(lang=ko)}">한국어</a>
<a th:href="@{''(lang=en)}">English</a>
```

> `@{''(lang=ko)}`는 현재 경로를 유지하면서 쿼리 파라미터만 추가하는 Thymeleaf 표현식입니다. 페이지를 이동해도 선택한 언어가 세션에 저장되어 유지됩니다.

### application.yml 설정 확인

7.4에서 추가한 설정을 그대로 유지합니다. `basename`을 `messages`로 설정하면 Spring이 `messages_ko.properties`, `messages_en.properties`를 자동으로 인식합니다.

```yaml
spring:
  messages:
    basename: messages   # messages*.properties 모두 자동 인식
    encoding: UTF-8
    cache-duration: 3600
```

---

## 7.10 전체 흐름 정리

```
[사용자 요청]
      ↓
[Controller]
  @Valid / @Validated → BindingResult 처리 (형식 검증)
      ↓ (검증 통과)
[Service]
  비즈니스 규칙 위반 → 커스텀 예외 throw
      ↓ (정상)
[Repository]
  DB 오류 → DataAccessException (Spring이 래핑)
      ↓
[GlobalExceptionHandler (@ControllerAdvice)]
  EntityNotFoundException  → error/404.html
  BusinessException        → redirect + 플래시 메시지
  NotBoardOwnerException   → error/403.html
  Exception                → error/500.html
```

---

## 정리

| 핵심 개념 | 내용 |
|---|---|
| `@NotBlank` | null·빈 문자열·공백 문자열 모두 거부 |
| `@Validated` + 그룹 | 등록·수정 상황별 다른 검증 규칙 적용 |
| 커스텀 제약 어노테이션 | `@Constraint` + `ConstraintValidator` 구현으로 직접 검증 규칙 정의 |
| `BindingResult.rejectValue()` | 코드에서 직접 검증 오류 추가 |
| `messages.properties` | Bean Validation 오류 메시지를 코드 기반으로 관리 |
| 커스텀 예외 계층 | 최상위 예외 → 상황별 하위 예외로 구조화 |
| `@ControllerAdvice` | 전역 예외 처리 — 오류 유형별로 다른 응답 반환 |
| `@Slf4j` + 레벨 전략 | 예외 심각도에 따라 ERROR/WARN/INFO/DEBUG 구분 |
| `LocaleResolver` | 요청의 로케일 결정 (Session / Cookie / AcceptHeader) |
| `LocaleChangeInterceptor` | `?lang=en` URL 파라미터로 로케일 동적 전환 |
| `#{...}` (Thymeleaf) | 메시지 키로 현재 로케일의 다국어 텍스트 렌더링 |

---

## 연습 문제

1. `BoardCreateRequest`에 파일 개수를 제한하는 커스텀 제약 어노테이션 `@MaxFileCount(max=5)`를 직접 만들어 보세요.
2. 존재하지 않는 게시글 URL(`/boards/99999`)로 접근했을 때 404 오류 페이지가 표시되는지 확인해 보세요.
3. `messages.properties`에 `NotBlank.boardCreateRequest.title` 메시지를 추가하고, 기본 메시지 대신 커스텀 메시지가 표시되는지 확인해 보세요.
4. `GlobalExceptionHandler`에 `@ExceptionHandler(Exception.class)` 하나만 있을 때와 예외 유형별로 나눴을 때의 차이를 설명해 보세요.
5. `SessionLocaleResolver`를 `CookieLocaleResolver`로 교체하고, 브라우저를 재시작한 후에도 언어 선택이 유지되는지 확인해 보세요.
6. `messages_ko.properties`와 `messages_en.properties`에 게시판 목록 화면의 모든 텍스트를 옮기고, 언어 전환 링크를 네비게이션 바에 추가해 보세요.

---

## 다음 장 예고

8장에서는 테스트를 작성합니다. `@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest`의 차이를 이해하고 Controller, Service, Repository를 각각 단위·통합 테스트합니다.
