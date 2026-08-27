# 5장. 이미지 게시판 핵심 기능 구현

---

## 학습 목표

- `MultipartFile`을 사용해 이미지를 서버에 저장할 수 있다.
- UUID 기반 파일명 전략으로 파일 충돌 없이 관리할 수 있다.
- 저장된 이미지를 URL로 서빙하도록 리소스 핸들러를 설정할 수 있다.
- Thumbnailator를 이용해 썸네일 이미지를 생성할 수 있다.
- `Pageable`로 게시글 목록을 페이징 처리할 수 있다.
- 제목·내용 키워드 검색 기능을 구현할 수 있다.

---

## 5.1 파일 업로드 사전 준비

### 의존성 추가

썸네일 생성을 위한 Thumbnailator 라이브러리를 `build.gradle`에 추가합니다.

```groovy
// build.gradle
dependencies {
    // 기존 의존성 ...

    // 썸네일 생성 라이브러리
    implementation 'net.coobird:thumbnailator:0.4.20'
}
```

### application.yml — 파일 업로드 설정

```yaml
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB       # 파일 하나의 최대 크기
      max-request-size: 30MB    # 요청 전체(여러 파일 합산)의 최대 크기

# 업로드 파일 저장 경로 (커스텀 프로퍼티)
file:
  upload-dir: ${user.home}/imageboard/uploads   # OS 홈 디렉터리 하위
  thumbnail-dir: ${user.home}/imageboard/thumbnails
  allowed-extensions:
    - jpg
    - jpeg
    - png
    - gif
    - webp
```

### FileProperties 설정 클래스

```java
// src/main/java/com/example/imageboard/config/FileProperties.java
package com.example.imageboard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "file")
public class FileProperties {

    private String uploadDir;
    private String thumbnailDir;
    private List<String> allowedExtensions;
}
```

### 정적 리소스 핸들러 설정

서버에 저장된 이미지를 URL로 접근할 수 있도록 리소스 핸들러를 등록합니다. Spring Boot의 `/static` 폴더는 JAR 내부에 있어 외부에 저장된 업로드 파일을 서빙할 수 없습니다. 따라서 외부 디렉터리를 URL에 매핑하는 설정이 필요합니다.

```java
// src/main/java/com/example/imageboard/config/WebConfig.java
package com.example.imageboard.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final FileProperties fileProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // /images/** 요청 → 업로드 디렉터리 파일로 응답
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + fileProperties.getUploadDir() + "/");

        // /thumbnails/** 요청 → 썸네일 디렉터리 파일로 응답
        registry.addResourceHandler("/thumbnails/**")
                .addResourceLocations("file:" + fileProperties.getThumbnailDir() + "/");
    }
}
```

```
URL 매핑 결과
http://localhost:8080/images/abc123.jpg
    → ${user.home}/imageboard/uploads/abc123.jpg

http://localhost:8080/thumbnails/abc123.jpg
    → ${user.home}/imageboard/thumbnails/abc123.jpg
```

---

## 5.2 파일 저장 서비스

파일 저장, 삭제, 검증, 썸네일 생성 등 파일 관련 로직을 전담하는 서비스 클래스를 만듭니다.

```java
// src/main/java/com/example/imageboard/service/FileService.java
package com.example.imageboard.service;

import com.example.imageboard.config.FileProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileProperties fileProperties;

    /**
     * 이미지 파일을 서버에 저장하고 저장된 파일명을 반환합니다.
     *
     * @param file 업로드된 MultipartFile
     * @return 서버에 저장된 파일명 (UUID 기반)
     */
    public String store(MultipartFile file) {
        validateFile(file);

        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName);
        String storedName = UUID.randomUUID() + "." + extension;  // 충돌 방지

        Path uploadPath = Paths.get(fileProperties.getUploadDir());
        Path filePath = uploadPath.resolve(storedName);

        try {
            Files.createDirectories(uploadPath);  // 디렉터리 없으면 생성
            file.transferTo(filePath);            // 파일 저장
            log.info("파일 저장 완료: {} → {}", originalName, storedName);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장에 실패했습니다: " + originalName, e);
        }

        return storedName;
    }

    /**
     * 썸네일을 생성하고 저장된 파일명을 반환합니다.
     * 원본 이미지와 동일한 파일명을 사용합니다.
     *
     * @param storedName 원본 파일의 저장 파일명
     */
    public void createThumbnail(String storedName) {
        Path sourcePath = Paths.get(fileProperties.getUploadDir(), storedName);
        Path thumbnailPath = Paths.get(fileProperties.getThumbnailDir());
        Path targetPath = thumbnailPath.resolve(storedName);

        try {
            Files.createDirectories(thumbnailPath);
            Thumbnails.of(sourcePath.toFile())
                    .size(300, 300)          // 최대 가로·세로 300px (비율 유지)
                    .keepAspectRatio(true)   // 원본 비율 유지
                    .outputQuality(0.85)     // 이미지 품질 85%
                    .toFile(targetPath.toFile());
            log.info("썸네일 생성 완료: {}", storedName);
        } catch (IOException e) {
            throw new RuntimeException("썸네일 생성에 실패했습니다: " + storedName, e);
        }
    }

    /**
     * 업로드된 파일과 썸네일을 삭제합니다.
     *
     * @param storedName 삭제할 파일명
     */
    public void delete(String storedName) {
        deleteFile(Paths.get(fileProperties.getUploadDir(), storedName));
        deleteFile(Paths.get(fileProperties.getThumbnailDir(), storedName));
    }

    // ── private 헬퍼 ─────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }
        String extension = extractExtension(file.getOriginalFilename()).toLowerCase();
        if (!fileProperties.getAllowedExtensions().contains(extension)) {
            throw new IllegalArgumentException(
                "허용되지 않는 파일 형식입니다: " + extension
                + " (허용: " + fileProperties.getAllowedExtensions() + ")"
            );
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("파일 확장자가 없습니다.");
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private void deleteFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("파일 삭제 실패: {}", path, e);
        }
    }
}
```

### UUID 기반 파일명 전략

같은 이름의 파일을 여러 사용자가 업로드해도 충돌이 발생하지 않도록 UUID로 파일명을 변경합니다.

```
업로드: 고양이.jpg
저장:   550e8400-e29b-41d4-a716-446655440000.jpg
```

원본 파일명은 `AttachedImage.originalName`에 보관하고, 서버 저장 파일명은 `AttachedImage.storedName`에 보관합니다.

---

## 5.3 BoardService — 이미지 포함 게시글 저장

`BoardService`에 이미지 업로드 로직을 추가합니다.

```java
// BoardService.java — 이미지 관련 메서드 추가
// (기존 의존성에 FileService, AttachedImageRepository 추가)

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final AttachedImageRepository imageRepository;
    private final FileService fileService;           // 추가

    /** 게시글 + 이미지 등록 */
    @Transactional
    public Long create(BoardCreateRequest request) {
        Member member = memberRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("회원이 없습니다."));

        Board board = Board.create(request.getTitle(), request.getContent(), member);

        // 이미지 업로드 처리
        if (request.getImages() != null) {
            request.getImages().stream()
                    .filter(file -> !file.isEmpty())
                    .forEach(file -> {
                        String storedName = fileService.store(file);
                        fileService.createThumbnail(storedName);

                        AttachedImage image = AttachedImage.create(
                                file.getOriginalFilename(),
                                storedName,
                                "/images/" + storedName,
                                file.getSize()
                        );
                        board.addImage(image);  // Board에 이미지 추가
                    });
        }

        boardRepository.save(board);  // Board + Image 함께 저장 (CascadeType.ALL)
        return board.getId();
    }

    /** 게시글 + 이미지 수정 */
    @Transactional
    public void update(Long id, BoardUpdateRequest request) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        board.update(request.getTitle(), request.getContent());

        // 추가 이미지 저장 (기존 이미지는 유지, 개별 삭제는 deleteImage()로 처리)
        if (request.getImages() != null) {
            request.getImages().stream()
                    .filter(file -> !file.isEmpty())
                    .forEach(file -> {
                        String storedName = fileService.store(file);
                        fileService.createThumbnail(storedName);

                        AttachedImage image = AttachedImage.create(
                                file.getOriginalFilename(),
                                storedName,
                                "/images/" + storedName,
                                file.getSize()
                        );
                        board.addImage(image);
                    });
        }
    }

    /** 게시글 + 이미지 삭제 */
    @Transactional
    public void delete(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // 첨부 이미지 파일 삭제
        board.getImages()
             .forEach(image -> fileService.delete(image.getStoredName()));

        boardRepository.delete(board);  // orphanRemoval로 DB 레코드도 함께 삭제
    }
}
```

### BoardCreateRequest — 이미지 필드 추가

```java
// BoardCreateRequest.java — images 필드 추가
@Getter
@Setter
public class BoardCreateRequest {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 100, message = "제목은 100자 이내로 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    // 이미지 파일 목록 (선택 입력, 최대 5개)
    private List<MultipartFile> images;
}
```

### BoardUpdateRequest — 이미지 필드 추가

3장에서 정의한 `BoardUpdateRequest`에도 이미지 필드를 추가합니다. 수정 시 새 이미지를 추가할 수 있어야 하기 때문입니다.

```java
// BoardUpdateRequest.java — images 필드 추가
@Getter
@Setter
public class BoardUpdateRequest {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 100, message = "제목은 100자 이내로 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    // 추가할 이미지 파일 목록 (선택 입력, 최대 5개)
    private List<MultipartFile> images;
}
```

---

## 5.4 게시글 작성 폼 — 이미지 업로드 UI

```html
<!-- templates/board/create.html — 이미지 업로드 추가 -->
<!-- enctype="multipart/form-data" 반드시 추가 -->
<form th:action="@{/boards}" th:object="${boardCreateRequest}"
      method="post" enctype="multipart/form-data">

    <div class="form-group">
        <label for="title">제목</label>
        <input class="form-control" type="text" id="title"
               th:field="*{title}" placeholder="제목을 입력하세요">
        <p class="error-message"
           th:if="${#fields.hasErrors('title')}"
           th:errors="*{title}"></p>
    </div>

    <div class="form-group">
        <label for="content">내용</label>
        <textarea class="form-control" id="content"
                  th:field="*{content}" rows="10"
                  placeholder="내용을 입력하세요"></textarea>
        <p class="error-message"
           th:if="${#fields.hasErrors('content')}"
           th:errors="*{content}"></p>
    </div>

    <!-- 이미지 업로드 -->
    <div class="form-group">
        <label>이미지 첨부 <span style="color:#aaa; font-weight:normal;">(최대 5개, jpg·png·gif·webp, 파일당 10MB)</span></label>
        <input class="form-control" type="file" name="images"
               accept="image/*" multiple>

        <!-- 미리보기 영역 -->
        <div id="preview-area" style="display:flex; gap:10px; flex-wrap:wrap; margin-top:10px;"></div>
    </div>

    <div style="display:flex; gap:8px; justify-content:flex-end;">
        <a class="btn btn-secondary" th:href="@{/boards}">취소</a>
        <button class="btn btn-primary" type="submit">저장</button>
    </div>
</form>

<!-- 이미지 미리보기 스크립트 -->
<script>
    document.querySelector('input[type="file"]').addEventListener('change', function (e) {
        const preview = document.getElementById('preview-area');
        preview.innerHTML = '';

        const files = Array.from(e.target.files).slice(0, 5); // 최대 5개
        files.forEach(file => {
            if (!file.type.startsWith('image/')) return;

            const reader = new FileReader();
            reader.onload = ev => {
                const img = document.createElement('img');
                img.src = ev.target.result;
                img.style.cssText = 'width:120px; height:120px; object-fit:cover; border-radius:4px; border:1px solid #dee2e6;';
                preview.appendChild(img);
            };
            reader.readAsDataURL(file);
        });
    });
</script>
```

> **`enctype="multipart/form-data"` 필수**: 파일을 포함한 폼을 전송하려면 반드시 이 인코딩 타입을 지정해야 합니다. 누락 시 파일이 전송되지 않습니다.

---

## 5.5 게시글 상세 화면 — 이미지 표시

### BoardResponse — images 필드 추가

상세 화면에서 `board.images`를 사용하려면 `BoardResponse`에 이미지 목록 필드가 있어야 합니다. 템플릿보다 **먼저** DTO와 변환 메서드를 수정합니다.

```java
// BoardResponse.java 수정
@Getter
@Builder
public class BoardResponse {

    private Long id;
    private String title;
    private String content;
    private String author;
    private int viewCount;
    private String thumbnailUrl;
    private List<ImageResponse> images;    // 추가
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class ImageResponse {
        private Long id;
        private String originalName;
        private String storedName;
    }
}
```

`BoardService`의 `toResponse()` 메서드에 이미지 변환 로직도 추가합니다.

```java
private BoardResponse toResponse(Board board) {
    List<BoardResponse.ImageResponse> images = board.getImages().stream()
            .map(img -> BoardResponse.ImageResponse.builder()
                    .id(img.getId())
                    .originalName(img.getOriginalName())
                    .storedName(img.getStoredName())
                    .build())
            .toList();

    return BoardResponse.builder()
            .id(board.getId())
            .title(board.getTitle())
            .content(board.getContent())
            .author(board.getMember().getNickname())
            .viewCount(board.getViewCount())
            .thumbnailUrl(board.getThumbnailUrl())
            .images(images)
            .createdAt(board.getCreatedAt())
            .updatedAt(board.getUpdatedAt())
            .build();
}
```

### 상세 화면 템플릿

```html
<!-- templates/board/detail.html — 이미지 갤러리 추가 -->

<!-- 첨부 이미지 갤러리 -->
<div th:if="${not #lists.isEmpty(board.images)}" style="margin: 20px 0;">
    <h4 style="margin-bottom: 10px;">첨부 이미지 (<span th:text="${#lists.size(board.images)}">0</span>개)</h4>
    <div style="display:flex; gap:12px; flex-wrap:wrap;">
        <div th:each="image : ${board.images}" style="position:relative;">
            <!-- 썸네일 클릭 시 원본 이미지 표시 -->
            <a th:href="@{'/images/' + ${image.storedName}}" target="_blank">
                <img th:src="@{'/thumbnails/' + ${image.storedName}}"
                     th:alt="${image.originalName}"
                     style="width:150px; height:150px; object-fit:cover;
                            border-radius:6px; border:1px solid #dee2e6;
                            cursor:pointer; transition:opacity .2s;"
                     onmouseover="this.style.opacity=0.8"
                     onmouseout="this.style.opacity=1">
            </a>
            <!-- 파일명 표시 -->
            <div th:text="${image.originalName}"
                 style="font-size:0.75rem; color:#6c757d; margin-top:4px;
                        max-width:150px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">
            </div>
        </div>
    </div>
</div>
```

---

## 5.6 페이징 처리

### Repository — Pageable 적용

```java
// BoardRepository.java — 페이징 쿼리 추가
public interface BoardRepository extends JpaRepository<Board, Long> {

    // 전체 목록 페이징
    @Query("SELECT b FROM Board b JOIN FETCH b.member ORDER BY b.createdAt DESC")
    Page<Board> findAllWithMember(Pageable pageable);

    // 키워드 검색 + 페이징
    @Query(value = """
            SELECT b FROM Board b JOIN FETCH b.member
            WHERE b.title LIKE %:keyword% OR b.content LIKE %:keyword%
            ORDER BY b.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(b) FROM Board b
            WHERE b.title LIKE %:keyword% OR b.content LIKE %:keyword%
            """)
    Page<Board> searchWithMember(@Param("keyword") String keyword, Pageable pageable);
}
```

> **countQuery 분리**: 페이징에서 전체 건수(`COUNT`)를 구할 때 `JOIN FETCH`가 포함되면 성능이 떨어집니다. `countQuery`를 별도로 지정하면 카운트 쿼리에서는 JOIN을 생략합니다.

### BoardService — 페이징·검색 적용

```java
// BoardService.java — findAll 수정
public Page<BoardResponse> findAll(String keyword, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);  // 0-based 페이지 번호

    Page<Board> boardPage = (keyword == null || keyword.isBlank())
            ? boardRepository.findAllWithMember(pageable)
            : boardRepository.searchWithMember(keyword, pageable);

    return boardPage.map(this::toResponse);
}
```

### BoardController — 페이징·검색 파라미터 수신

```java
// BoardController.java — list 메서드 수정
@GetMapping
public String list(@RequestParam(defaultValue = "") String keyword,
                   @RequestParam(defaultValue = "0") int page,
                   Model model) {

    Page<BoardResponse> boardPage = boardService.findAll(keyword, page, 10);

    model.addAttribute("boardPage", boardPage);
    model.addAttribute("keyword", keyword);
    model.addAttribute("currentPage", page);
    return "board/list";
}
```

---

## 5.7 게시판 목록 화면 — 페이징·검색 UI

```html
<!-- templates/board/list.html — 검색·페이징 추가 -->
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

    <!-- 검색 폼 -->
    <form th:action="@{/boards}" method="get" style="margin-bottom:16px; display:flex; gap:8px;">
        <input class="form-control" type="text" name="keyword"
               th:value="${keyword}" placeholder="제목 또는 내용 검색"
               style="max-width:300px;">
        <button class="btn btn-secondary" type="submit">검색</button>
        <a class="btn btn-secondary" th:href="@{/boards}"
           th:if="${keyword != null and !keyword.isEmpty()}">초기화</a>
    </form>

    <!-- 검색 결과 건수 -->
    <div th:if="${keyword != null and !keyword.isEmpty()}"
         style="margin-bottom:8px; color:#6c757d; font-size:0.9rem;">
        '<span th:text="${keyword}"></span>' 검색 결과:
        <strong th:text="${boardPage.totalElements}">0</strong>건
    </div>

    <!-- 게시글 없음 -->
    <div th:if="${boardPage.isEmpty()}"
         style="text-align:center; padding:60px 0; color:#aaa;">
        게시글이 없습니다.
    </div>

    <!-- 게시글 목록 테이블 -->
    <table class="board-table" th:unless="${boardPage.isEmpty()}">
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
            <tr th:each="board, stat : ${boardPage.content}">
                <!-- 전체 번호 = 전체 건수 - (현재 페이지 * 페이지 크기) - 현재 인덱스 -->
                <td th:text="${boardPage.totalElements
                               - (boardPage.number * boardPage.size)
                               - stat.index}">1</td>
                <td>
                    <a th:href="@{/boards/{id}(id=${board.id})}"
                       th:text="${board.title}">게시글 제목</a>
                    <span th:if="${board.thumbnailUrl != null}"
                          style="color:#0d6efd; font-size:0.8rem; margin-left:4px;">🖼</span>
                </td>
                <td th:text="${board.author}">작성자</td>
                <td th:text="${board.viewCount}">0</td>
                <td th:text="${#temporals.format(board.createdAt, 'yyyy-MM-dd')}">2026-01-01</td>
            </tr>
        </tbody>
    </table>

    <!-- 페이지 네비게이션 -->
    <div th:if="${boardPage.totalPages > 1}"
         style="display:flex; justify-content:center; gap:4px; margin-top:20px;">

        <!-- 이전 페이지 -->
        <a class="btn btn-secondary"
           th:if="${boardPage.hasPreviousPage()}"
           th:href="@{/boards(page=${boardPage.number - 1}, keyword=${keyword})}">
            &laquo;
        </a>

        <!-- 페이지 번호 (최대 10개 표시) -->
        <th:block th:with="
            startPage=${T(Math).max(0, boardPage.number - 4)},
            endPage=${T(Math).min(boardPage.totalPages - 1, boardPage.number + 5)}">
            <a th:each="i : ${#numbers.sequence(startPage, endPage)}"
               th:href="@{/boards(page=${i}, keyword=${keyword})}"
               th:text="${i + 1}"
               th:class="${i == boardPage.number} ? 'btn btn-primary' : 'btn btn-secondary'">
                1
            </a>
        </th:block>

        <!-- 다음 페이지 -->
        <a class="btn btn-secondary"
           th:if="${boardPage.hasNextPage()}"
           th:href="@{/boards(page=${boardPage.number + 1}, keyword=${keyword})}">
            &raquo;
        </a>
    </div>
</div>

<footer th:replace="~{layout/default :: footer}"></footer>

</body>
</html>
```

---

## 5.8 이미지 개별 삭제 및 수정 폼 완성

### Controller — editForm() · update() 수정

3장에서 만든 `editForm()`과 `update()`를 이미지 업로드를 고려한 형태로 수정합니다.

```java
// BoardController.java — editForm, update 수정
import com.example.imageboard.dto.BoardUpdateRequest;

/** 게시글 수정 폼 */
@GetMapping("/{id}/edit")
public String editForm(@PathVariable Long id, Model model) {
    BoardResponse board = boardService.findById(id);

    BoardUpdateRequest boardUpdateRequest = new BoardUpdateRequest();
    boardUpdateRequest.setTitle(board.getTitle());
    boardUpdateRequest.setContent(board.getContent());

    model.addAttribute("board", board);
    model.addAttribute("boardUpdateRequest", boardUpdateRequest);
    return "board/edit";
}

/** 게시글 수정 처리 — multipart/form-data로 수신 */
@PutMapping("/{id}")
public String update(@PathVariable Long id,
                     @Valid @ModelAttribute BoardUpdateRequest request,
                     BindingResult bindingResult,
                     Model model) {
    if (bindingResult.hasErrors()) {
        model.addAttribute("board", boardService.findById(id));
        return "board/edit";
    }
    boardService.update(id, request);
    return "redirect:/boards/" + id;
}

/** 이미지 개별 삭제 */
@DeleteMapping("/{boardId}/images/{imageId}")
public String deleteImage(@PathVariable Long boardId,
                          @PathVariable Long imageId) {
    boardService.deleteImage(boardId, imageId);
    return "redirect:/boards/" + boardId + "/edit";
}
```

### Service — deleteImage() 추가

```java
// BoardService.java — deleteImage 추가
@Transactional
public void deleteImage(Long boardId, Long imageId) {
    Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

    AttachedImage image = imageRepository.findById(imageId)
            .orElseThrow(() -> new IllegalArgumentException("이미지를 찾을 수 없습니다."));

    // Board에서 이미지 제거 (orphanRemoval로 DB 레코드 자동 삭제)
    board.getImages().remove(image);

    // 실제 파일 삭제
    fileService.delete(image.getStoredName());
}
```

### edit.html — 완성본

3장의 `edit.html`에 ① 기존 이미지 삭제, ② 새 이미지 추가, ③ 미리보기를 통합한 완성본입니다.

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
            enctype="multipart/form-data" : 새 이미지 업로드를 위해 필수
            _method=PUT : HTML 폼은 PUT을 지원하지 않으므로 오버라이드
        -->
        <form th:action="@{/boards/{id}(id=${board.id})}"
              th:object="${boardUpdateRequest}"
              method="post"
              enctype="multipart/form-data">
            <input type="hidden" name="_method" value="PUT">

            <!-- 제목 -->
            <div class="form-group">
                <label for="title">제목</label>
                <input class="form-control" type="text" id="title"
                       th:field="*{title}" placeholder="제목을 입력하세요">
                <p class="error-message"
                   th:if="${#fields.hasErrors('title')}"
                   th:errors="*{title}">제목 오류</p>
            </div>

            <!-- 내용 -->
            <div class="form-group">
                <label for="content">내용</label>
                <textarea class="form-control" id="content"
                          th:field="*{content}" rows="10"
                          placeholder="내용을 입력하세요"></textarea>
                <p class="error-message"
                   th:if="${#fields.hasErrors('content')}"
                   th:errors="*{content}">내용 오류</p>
            </div>

            <!-- 기존 첨부 이미지 -->
            <div th:if="${not #lists.isEmpty(board.images)}" class="form-group">
                <label>현재 첨부 이미지</label>
                <div style="display:flex; gap:10px; flex-wrap:wrap; margin-top:8px;">
                    <div th:each="image : ${board.images}" style="text-align:center;">
                        <img th:src="@{'/thumbnails/' + ${image.storedName}}"
                             th:alt="${image.originalName}"
                             style="width:100px; height:100px; object-fit:cover;
                                    border-radius:4px; border:1px solid #dee2e6;">
                        <div th:text="${image.originalName}"
                             style="font-size:0.7rem; color:#6c757d; margin-top:4px;
                                    max-width:100px; overflow:hidden;
                                    text-overflow:ellipsis; white-space:nowrap;"></div>
                        <!-- 이미지 개별 삭제 (별도 폼으로 처리) -->
                        <form th:action="@{/boards/{bId}/images/{iId}(bId=${board.id}, iId=${image.id})}"
                              method="post" style="margin-top:4px;">
                            <input type="hidden" name="_method" value="DELETE">
                            <button class="btn btn-danger" type="submit"
                                    style="font-size:0.75rem; padding:4px 8px;"
                                    onclick="return confirm('이미지를 삭제하시겠습니까?')">삭제</button>
                        </form>
                    </div>
                </div>
            </div>

            <!-- 새 이미지 추가 -->
            <div class="form-group">
                <label>이미지 추가 <span style="color:#aaa; font-weight:normal;">(jpg·png·gif·webp, 파일당 10MB)</span></label>
                <input class="form-control" type="file" name="images"
                       accept="image/*" multiple>
                <!-- 미리보기 영역 -->
                <div id="preview-area"
                     style="display:flex; gap:10px; flex-wrap:wrap; margin-top:10px;"></div>
            </div>

            <div style="display:flex; gap:8px; justify-content:flex-end;">
                <a class="btn btn-secondary"
                   th:href="@{/boards/{id}(id=${board.id})}">취소</a>
                <button class="btn btn-primary" type="submit">수정 완료</button>
            </div>

        </form>
    </div>
</div>

<!-- 새 이미지 미리보기 스크립트 -->
<script>
    document.querySelector('input[type="file"]').addEventListener('change', function (e) {
        const preview = document.getElementById('preview-area');
        preview.innerHTML = '';

        const files = Array.from(e.target.files).slice(0, 5);
        files.forEach(file => {
            if (!file.type.startsWith('image/')) return;

            const reader = new FileReader();
            reader.onload = ev => {
                const img = document.createElement('img');
                img.src = ev.target.result;
                img.style.cssText = 'width:100px; height:100px; object-fit:cover; ' +
                                    'border-radius:4px; border:1px solid #dee2e6;';
                preview.appendChild(img);
            };
            reader.readAsDataURL(file);
        });
    });
</script>

<footer th:replace="~{layout/default :: footer}"></footer>

</body>
</html>
```

> **이미지 삭제와 수정 폼의 분리**: 이미지 개별 삭제는 별도 `<form>`으로 즉시 처리하고, 수정 폼 제출은 제목·내용·신규 이미지만 담당합니다. 하나의 폼에 삭제·추가를 모두 넣으면 폼 제출 시점까지 삭제를 지연시켜야 하므로 구현이 복잡해집니다.

---

## 5.9 파일 업로드 예외 처리

파일 크기 초과, 허용되지 않는 형식 등의 오류를 사용자에게 명확히 알려줍니다.

```java
// src/main/java/com/example/imageboard/controller/GlobalExceptionHandler.java
package com.example.imageboard.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 파일 크기 초과
     * Referer를 확인해 작성 폼과 수정 폼 양쪽에서 발생한 오류를 적절히 처리합니다.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(MaxUploadSizeExceededException e,
                                      HttpServletRequest request,
                                      RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage",
                "파일 크기가 너무 큽니다. 파일당 최대 10MB까지 업로드할 수 있습니다.");
        return resolveRedirect(request);
    }

    /** 허용되지 않는 파일 형식 등 잘못된 인자 */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException e,
                                        HttpServletRequest request,
                                        RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return resolveRedirect(request);
    }

    /**
     * Referer URL을 기반으로 오류 발생 이전 페이지로 돌아갑니다.
     * - 수정 폼(/boards/{id}/edit)에서 발생한 오류 → 수정 폼으로 리다이렉트
     * - 그 외(작성 폼 등) → 작성 폼으로 리다이렉트
     */
    private String resolveRedirect(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/edit")) {
            // Referer에서 /boards/{id}/edit 경로를 추출해 돌아감
            String path = URI.create(referer).getPath();
            return "redirect:" + path;
        }
        return "redirect:/boards/create";
    }
}
```

공통 레이아웃에 플래시 메시지 표시 영역을 추가합니다.

```html
<!-- layout/default.html — nav 아래에 추가 -->
<div th:if="${errorMessage}"
     style="background:#f8d7da; color:#842029; padding:12px 20px;
            border-bottom:1px solid #f5c2c7; text-align:center;">
    <span th:text="${errorMessage}"></span>
</div>
```

---

## 5.10 전체 흐름 정리

### 게시글 작성 흐름

```
[게시글 작성 폼] create.html
  ↓ 제목, 내용, 이미지 파일 선택
[POST /boards] — multipart/form-data
  ↓
[BoardController.create()]
  ↓
[BoardService.create()]
  ├─ FileService.store()            → UUID 파일명으로 uploads/ 저장
  ├─ FileService.createThumbnail()  → thumbnails/ 저장
  └─ boardRepository.save()         → Board + AttachedImage DB 저장
  ↓
[redirect:/boards/{id}]
  ↓
[게시글 상세 화면] detail.html
  ├─ /thumbnails/{storedName}       → 썸네일 표시 (150×150)
  └─ /images/{storedName}           → 원본 이미지 링크
```

### 게시글 수정 흐름

```
[게시글 상세 화면] detail.html — 수정 버튼 클릭
  ↓
[GET /boards/{id}/edit]
  ↓
[BoardController.editForm()]
  └─ BoardService.findById()        → 기존 제목·내용·이미지 목록 조회
  ↓
[게시글 수정 폼] edit.html
  ├─ 기존 이미지 썸네일 표시 + 개별 삭제 버튼
  └─ 새 이미지 파일 선택 (선택)

  [이미지 개별 삭제 선택 시]
    ↓ DELETE /boards/{boardId}/images/{imageId}
    [BoardService.deleteImage()]
      ├─ board.getImages().remove(image)  → orphanRemoval로 DB 삭제
      └─ FileService.delete()             → 파일·썸네일 삭제
    ↓ redirect:/boards/{id}/edit (수정 폼으로 복귀)

  [수정 완료 버튼 클릭 시]
    ↓ PUT /boards/{id} — multipart/form-data
    [BoardController.update()]
      ↓
    [BoardService.update()]
      ├─ board.update(title, content)     → 제목·내용 수정
      ├─ FileService.store()              → 새 이미지 저장 (선택)
      ├─ FileService.createThumbnail()    → 새 썸네일 생성 (선택)
      └─ board.addImage()                 → 새 이미지 연결 (선택)
    ↓ redirect:/boards/{id}
    [게시글 상세 화면] detail.html
```

---

## 정리

| 핵심 개념 | 내용 |
|---|---|
| `MultipartFile` | 업로드된 파일을 담는 Spring 인터페이스 |
| `enctype="multipart/form-data"` | 파일을 포함한 폼 전송 시 필수 속성 |
| UUID 파일명 | 파일명 충돌 방지. 원본 파일명은 DB에 별도 보관 |
| `addResourceHandlers()` | 외부 디렉터리를 URL로 서빙하기 위한 리소스 핸들러 설정 |
| Thumbnailator | 이미지 리사이즈 및 썸네일 생성 라이브러리 |
| `PageRequest.of(page, size)` | 페이징 요청 객체 생성 |
| `Page<T>` | 페이징 결과. 데이터 + 전체 건수 + 페이지 정보 포함 |
| `@ControllerAdvice` | 전역 예외 처리 |

---

## 연습 문제

1. 파일 업로드 시 이미지가 아닌 파일(예: `.pdf`, `.txt`)을 선택했을 때 오류 메시지가 표시되는지 확인해 보세요.
2. `Thumbnails.of().size(300, 300)` 설정을 `size(100, 100)`으로 변경하고 썸네일 크기가 달라지는지 확인해 보세요.
3. 검색어를 입력한 상태에서 페이지를 이동할 때 검색어가 유지되는지 확인해 보세요. URL에 `keyword` 파라미터가 포함되는지 살펴보세요.
4. 한 게시글에 이미지를 5개 이상 업로드하면 어떻게 되는지 확인하고, 최대 개수를 제한하는 검증 로직을 추가해 보세요.

---

## 다음 장 예고

6장에서는 Spring Security를 적용합니다. 로그인·로그아웃, 회원가입, 게시글 작성자 권한 제어를 구현하고, 현재 임시로 고정된 작성자(id=1)를 실제 로그인 사용자로 교체합니다.
