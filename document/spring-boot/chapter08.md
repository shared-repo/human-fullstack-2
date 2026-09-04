# 8장. 테스트

---

## 학습 목표

- Spring Boot 테스트 어노테이션(`@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest`)의 차이와 적합한 사용 상황을 설명할 수 있다.
- Mockito로 의존성을 Mock 처리하여 Service를 단위 테스트할 수 있다.
- MockMvc로 Controller의 HTTP 요청·응답을 테스트할 수 있다.
- `@DataJpaTest`로 Repository 쿼리를 검증할 수 있다.
- `MockMultipartFile`로 파일 업로드 기능을 테스트할 수 있다.

---

## 8.1 Spring Boot 테스트 전략

### 테스트 피라미드

좋은 테스트 스위트는 빠르고 비용이 낮은 테스트를 많이, 느리고 비용이 높은 테스트를 적게 구성합니다.

```
         ▲
        / \
       / 통 \       @SpringBootTest
      / 합  테 \     — 전체 컨텍스트 로딩, 느림, 핵심 흐름 검증
     / 스  트  \
    /───────────\
   /  슬라이스   \   @WebMvcTest / @DataJpaTest
  / 테  스  트   \  — 계층별 부분 로딩, 빠름
 /───────────────\
/ 단  위  테스트  \  @ExtendWith(MockitoExtension.class)
───────────────────  — 의존성 Mock, 가장 빠름, 가장 많이 작성
```

### 테스트 어노테이션 비교

| 어노테이션 | 로딩 범위 | 속도 | 주 용도 |
|---|---|---|---|
| `@SpringBootTest` | 전체 ApplicationContext | 느림 | 통합 테스트, E2E 흐름 검증 |
| `@WebMvcTest` | Web 계층만 (Controller, Filter 등) | 빠름 | Controller 요청·응답 검증 |
| `@DataJpaTest` | JPA 관련 빈만 (Entity, Repository) | 빠름 | Repository 쿼리 검증 |
| `@ExtendWith(MockitoExtension)` | Spring 컨텍스트 없음 | 가장 빠름 | Service 단위 테스트 |

### 테스트 의존성

`spring-boot-starter-test`에는 테스트에 필요한 라이브러리가 이미 포함되어 있습니다.

```groovy
// build.gradle — 이미 추가되어 있음
testImplementation 'org.springframework.boot:spring-boot-starter-test'
```

포함된 주요 라이브러리:

| 라이브러리 | 역할 |
|---|---|
| JUnit 5 | 테스트 프레임워크 |
| Mockito | Mock 객체 생성 |
| AssertJ | 가독성 높은 Assertion |
| MockMvc | Controller HTTP 테스트 |
| H2 Database | 인메모리 DB (슬라이스 테스트용) |

---

## 8.2 Service 단위 테스트 — Mockito

Service는 Repository와 같은 외부 의존성 없이 비즈니스 로직만 독립적으로 테스트합니다. Mockito로 Repository를 가짜 객체(Mock)로 대체합니다.

### Mockito 핵심 개념

```java
// Mock 생성 방법
@Mock BoardRepository boardRepository;   // 가짜 객체 생성
@InjectMocks BoardService boardService;  // Mock을 주입받는 대상

// 동작 지정 (stubbing)
when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
when(boardRepository.save(any())).thenReturn(board);
doThrow(new RuntimeException()).when(boardRepository).delete(any());

// 호출 검증
verify(boardRepository).save(any(Board.class));         // 1회 호출 확인
verify(boardRepository, times(2)).findById(anyLong());  // 2회 호출 확인
verify(boardRepository, never()).delete(any());         // 미호출 확인
```

### BoardService 단위 테스트

```java
// src/test/java/com/example/imageboard/service/BoardServiceTest.java
package com.example.imageboard.service;

import com.example.imageboard.dto.BoardCreateRequest;
import com.example.imageboard.dto.BoardResponse;
import com.example.imageboard.entity.Board;
import com.example.imageboard.entity.Member;
import com.example.imageboard.exception.BoardNotFoundException;
import com.example.imageboard.repository.BoardRepository;
import com.example.imageboard.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)     // Spring 컨텍스트 없이 Mockito만 사용
@DisplayName("BoardService 단위 테스트")
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private FileService fileService;

    @InjectMocks
    private BoardService boardService;

    private Member testMember;
    private Board testBoard;

    @BeforeEach
    void setUp() {
        testMember = Member.create("testuser", "encoded_password", "테스터");
        testBoard = Board.create("테스트 제목", "테스트 내용", testMember);
    }

    // ── 조회 테스트 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("존재하는 게시글 ID로 조회하면 BoardResponse를 반환한다")
    void findById_success() {
        // given
        given(boardRepository.findById(1L)).willReturn(Optional.of(testBoard));

        // when
        BoardResponse response = boardService.findById(1L);

        // then
        assertThat(response.getTitle()).isEqualTo("테스트 제목");
        assertThat(response.getContent()).isEqualTo("테스트 내용");
        assertThat(response.getAuthor()).isEqualTo("테스터");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회하면 BoardNotFoundException이 발생한다")
    void findById_notFound() {
        // given
        given(boardRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> boardService.findById(999L))
                .isInstanceOf(BoardNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ── 저장 테스트 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("게시글을 저장하면 boardRepository.save()가 호출된다")
    void create_success() {
        // given
        BoardCreateRequest request = new BoardCreateRequest();
        request.setTitle("새 게시글");
        request.setContent("새 내용");

        given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
        given(boardRepository.save(any(Board.class))).willReturn(testBoard);

        // when
        boardService.create(request, 1L);

        // then
        then(boardRepository).should(times(1)).save(any(Board.class));
    }

    // ── 수정 테스트 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("게시글 수정 시 제목과 내용이 변경된다")
    void update_success() {
        // given
        given(boardRepository.findById(1L)).willReturn(Optional.of(testBoard));

        BoardCreateRequest request = new BoardCreateRequest();
        request.setTitle("수정된 제목");
        request.setContent("수정된 내용");

        // when
        boardService.update(1L, request);

        // then — 변경 감지로 save() 호출 없이 필드가 바뀌었는지 확인
        assertThat(testBoard.getTitle()).isEqualTo("수정된 제목");
        assertThat(testBoard.getContent()).isEqualTo("수정된 내용");
    }

    // ── 삭제 테스트 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("게시글 삭제 시 boardRepository.delete()가 호출된다")
    void delete_success() {
        // given
        given(boardRepository.findById(1L)).willReturn(Optional.of(testBoard));

        // when
        boardService.delete(1L);

        // then
        then(boardRepository).should(times(1)).delete(testBoard);
    }

    @Test
    @DisplayName("존재하지 않는 게시글 삭제 시 BoardNotFoundException이 발생한다")
    void delete_notFound() {
        // given
        given(boardRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> boardService.delete(999L))
                .isInstanceOf(BoardNotFoundException.class);

        then(boardRepository).should(never()).delete(any());
    }
}
```

### BDD 스타일 — given / when / then

Mockito는 BDD(행동 주도 개발) 스타일의 API를 제공합니다. `given()`은 `when()`과 동일하게 동작하지만 테스트의 의도를 더 명확하게 표현합니다.

```java
// 기존 Mockito 스타일
when(boardRepository.findById(1L)).thenReturn(Optional.of(board));

// BDD 스타일 (가독성 향상)
given(boardRepository.findById(1L)).willReturn(Optional.of(board));

// 검증도 BDD 스타일
then(boardRepository).should(times(1)).save(any());
```

---

## 8.3 MemberService 단위 테스트

```java
// src/test/java/com/example/imageboard/service/MemberServiceTest.java
package com.example.imageboard.service;

import com.example.imageboard.dto.MemberCreateRequest;
import com.example.imageboard.entity.Member;
import com.example.imageboard.exception.DuplicateUsernameException;
import com.example.imageboard.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 단위 테스트")
class MemberServiceTest {

    @Mock MemberRepository memberRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks MemberService memberService;

    @Test
    @DisplayName("신규 아이디로 회원가입 시 저장된다")
    void register_success() {
        // given
        MemberCreateRequest request = createRequest("newuser", "password123", "닉네임");
        given(memberRepository.existsByUsername("newuser")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encoded_pw");

        // when
        memberService.register(request);

        // then
        then(memberRepository).should().save(any(Member.class));
    }

    @Test
    @DisplayName("이미 존재하는 아이디로 회원가입 시 DuplicateUsernameException이 발생한다")
    void register_duplicateUsername() {
        // given
        MemberCreateRequest request = createRequest("existinguser", "password123", "닉네임");
        given(memberRepository.existsByUsername("existinguser")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> memberService.register(request))
                .isInstanceOf(DuplicateUsernameException.class)
                .hasMessageContaining("existinguser");

        then(memberRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("회원가입 시 비밀번호는 암호화되어 저장된다")
    void register_passwordEncoded() {
        // given
        MemberCreateRequest request = createRequest("user", "plaintext", "닉네임");
        given(memberRepository.existsByUsername(any())).willReturn(false);
        given(passwordEncoder.encode("plaintext")).willReturn("$2a$encoded");

        // when
        memberService.register(request);

        // then
        then(passwordEncoder).should().encode("plaintext");   // encode 호출 확인
        then(memberRepository).should().save(argThat(member ->
                member.getPassword().equals("$2a$encoded")    // 암호화된 값으로 저장 확인
        ));
    }

    private MemberCreateRequest createRequest(String username, String password, String nickname) {
        MemberCreateRequest req = new MemberCreateRequest();
        req.setUsername(username);
        req.setPassword(password);
        req.setPasswordConfirm(password);
        req.setNickname(nickname);
        return req;
    }
}
```

---

## 8.4 Repository 슬라이스 테스트 — @DataJpaTest

`@DataJpaTest`는 JPA 관련 빈만 로딩하고, 기본적으로 인메모리 H2 데이터베이스를 사용합니다. 실제 MariaDB 없이 Repository 쿼리를 빠르게 검증할 수 있습니다.

### H2 의존성 추가

```groovy
// build.gradle
dependencies {
    testRuntimeOnly 'com.h2database:h2'   // 테스트 전용 H2 DB
}
```

### BoardRepository 슬라이스 테스트

```java
// src/test/java/com/example/imageboard/repository/BoardRepositoryTest.java
package com.example.imageboard.repository;

import com.example.imageboard.entity.Board;
import com.example.imageboard.entity.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DisplayName("BoardRepository 슬라이스 테스트")
class BoardRepositoryTest {

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TestEntityManager em;   // 테스트 전용 EntityManager

    private Member savedMember;

    @BeforeEach
    void setUp() {
        savedMember = memberRepository.save(
                Member.create("testuser", "pw", "테스터")
        );
    }

    @Test
    @DisplayName("게시글을 저장하면 ID가 생성된다")
    void save_assignsId() {
        // given
        Board board = Board.create("제목", "내용", savedMember);

        // when
        Board saved = boardRepository.save(board);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("제목");
    }

    @Test
    @DisplayName("저장 후 flush·clear하면 DB에서 다시 조회한다")
    void findById_afterFlushAndClear() {
        // given
        Board board = Board.create("영속성 테스트", "내용", savedMember);
        boardRepository.save(board);

        em.flush();   // INSERT SQL 실행
        em.clear();   // 1차 캐시 초기화 → 이후 조회는 DB에서

        // when
        Optional<Board> found = boardRepository.findById(board.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("영속성 테스트");
    }

    @Test
    @DisplayName("findAllWithMember()는 Member를 JOIN FETCH하여 N+1 없이 조회한다")
    void findAllWithMember_noNPlus1() {
        // given — 게시글 3건 저장
        boardRepository.save(Board.create("제목1", "내용1", savedMember));
        boardRepository.save(Board.create("제목2", "내용2", savedMember));
        boardRepository.save(Board.create("제목3", "내용3", savedMember));
        em.flush();
        em.clear();

        // when
        Page<Board> result = boardRepository.findAllWithMember(PageRequest.of(0, 10));

        // then — 각 Board의 Member에 접근해도 추가 쿼리 없음
        assertThat(result.getContent()).hasSize(3);
        result.getContent().forEach(b ->
                assertThat(b.getMember().getNickname()).isEqualTo("테스터")
        );
    }

    @Test
    @DisplayName("키워드로 검색하면 제목 또는 내용에 포함된 게시글이 반환된다")
    void searchByKeyword() {
        // given
        boardRepository.save(Board.create("스프링 부트 입문", "JPA 내용", savedMember));
        boardRepository.save(Board.create("Thymeleaf 가이드", "스프링 MVC", savedMember));
        boardRepository.save(Board.create("관계없는 글", "관계없는 내용", savedMember));
        em.flush();
        em.clear();

        // when
        Page<Board> result = boardRepository.searchWithMember("스프링", PageRequest.of(0, 10));

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Board::getTitle)
                .containsExactlyInAnyOrder("스프링 부트 입문", "Thymeleaf 가이드");
    }

    @Test
    @DisplayName("게시글 삭제 시 DB에서 제거된다")
    void delete_removesFromDb() {
        // given
        Board board = boardRepository.save(Board.create("삭제 테스트", "내용", savedMember));
        Long boardId = board.getId();
        em.flush();
        em.clear();

        // when
        boardRepository.deleteById(boardId);
        em.flush();
        em.clear();

        // then
        assertThat(boardRepository.findById(boardId)).isEmpty();
    }
}
```

### TestEntityManager 활용

`@DataJpaTest`에서 제공하는 `TestEntityManager`는 테스트 전용 EntityManager입니다.

```java
em.persist(entity);     // 저장 (영속화)
em.flush();             // 1차 캐시 → DB 반영 (INSERT/UPDATE SQL 실행)
em.clear();             // 1차 캐시 초기화 → 이후 조회는 반드시 DB에서
em.find(Board.class, id); // 직접 조회
```

`flush() + clear()` 패턴은 영속성 컨텍스트 캐시를 비워 실제 DB에서 데이터를 조회하는지 검증할 때 사용합니다.

---

## 8.5 Controller 슬라이스 테스트 — @WebMvcTest

`@WebMvcTest`는 Web 계층(Controller, Filter, ArgumentResolver 등)만 로딩합니다. Service, Repository는 `@MockBean`으로 대체합니다.

### BoardController 슬라이스 테스트

```java
// src/test/java/com/example/imageboard/controller/BoardControllerTest.java
package com.example.imageboard.controller;

import com.example.imageboard.dto.BoardResponse;
import com.example.imageboard.exception.BoardNotFoundException;
import com.example.imageboard.security.CustomUserDetails;
import com.example.imageboard.service.BoardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

@WebMvcTest(BoardController.class)
@DisplayName("BoardController 슬라이스 테스트")
class BoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoardService boardService;

    // ── 목록 조회 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /boards — 게시글 목록 페이지를 반환한다")
    @WithMockUser   // 인증된 사용자로 요청
    void list_returnsListView() throws Exception {
        // given
        Page<BoardResponse> page = new PageImpl<>(List.of(createBoardResponse(1L, "테스트 제목")));
        given(boardService.findAll(any(), anyInt(), anyInt())).willReturn(page);

        // when & then
        mockMvc.perform(get("/boards"))
                .andExpect(status().isOk())                      // HTTP 200
                .andExpect(view().name("board/list"))            // 뷰 이름 확인
                .andExpect(model().attributeExists("boardPage")) // 모델 속성 확인
                .andDo(print());                                 // 요청·응답 콘솔 출력
    }

    @Test
    @DisplayName("GET /boards — 키워드 파라미터로 검색할 수 있다")
    @WithMockUser
    void list_withKeyword() throws Exception {
        // given
        Page<BoardResponse> emptyPage = Page.empty();
        given(boardService.findAll(eq("스프링"), anyInt(), anyInt())).willReturn(emptyPage);

        // when & then
        mockMvc.perform(get("/boards").param("keyword", "스프링"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("keyword", "스프링"));
    }

    // ── 상세 조회 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /boards/{id} — 존재하는 게시글 상세 페이지를 반환한다")
    @WithMockUser
    void detail_success() throws Exception {
        // given
        given(boardService.findById(1L)).willReturn(createBoardResponse(1L, "테스트 제목"));

        // when & then
        mockMvc.perform(get("/boards/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("board/detail"))
                .andExpect(model().attributeExists("board"));
    }

    @Test
    @DisplayName("GET /boards/{id} — 존재하지 않는 게시글은 404 오류 페이지를 반환한다")
    @WithMockUser
    void detail_notFound() throws Exception {
        // given
        given(boardService.findById(999L)).willThrow(new BoardNotFoundException(999L));

        // when & then
        mockMvc.perform(get("/boards/999"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/404"));
    }

    // ── 게시글 저장 ────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /boards — 유효한 데이터로 게시글을 저장하면 상세 페이지로 리다이렉트된다")
    @WithMockUser
    void create_success() throws Exception {
        // given
        given(boardService.create(any(), any())).willReturn(1L);

        // when & then
        mockMvc.perform(post("/boards")
                        .param("title", "새 게시글 제목")
                        .param("content", "새 게시글 내용")
                        .with(csrf()))             // CSRF 토큰 자동 추가
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/boards/1"));
    }

    @Test
    @DisplayName("POST /boards — 제목이 비어 있으면 작성 폼으로 돌아간다")
    @WithMockUser
    void create_blankTitle() throws Exception {
        // when & then
        mockMvc.perform(post("/boards")
                        .param("title", "")         // 빈 제목
                        .param("content", "내용")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("board/create"))  // 폼으로 복귀
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("boardCreateRequest", "title"));
    }

    // ── 미인증 접근 ────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /boards/create — 비로그인 상태면 로그인 페이지로 리다이렉트된다")
    void createForm_unauthenticated() throws Exception {
        mockMvc.perform(get("/boards/create"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/members/login**"));
    }

    // ── 헬퍼 메서드 ────────────────────────────────────────────────────

    private BoardResponse createBoardResponse(Long id, String title) {
        return BoardResponse.builder()
                .id(id)
                .memberId(1L)
                .title(title)
                .content("내용")
                .author("테스터")
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
```

### MockMvc 주요 메서드

```java
// 요청 구성
mockMvc.perform(get("/boards"))                           // GET 요청
mockMvc.perform(post("/boards").param("title", "제목"))  // POST + 폼 파라미터
mockMvc.perform(get("/boards").param("page", "1"))       // 쿼리 파라미터
mockMvc.perform(get("/boards/1"))                        // 경로 변수

// 응답 검증
.andExpect(status().isOk())              // HTTP 200
.andExpect(status().is3xxRedirection())  // 3xx 리다이렉트
.andExpect(status().isForbidden())       // HTTP 403
.andExpect(view().name("board/list"))    // 뷰 이름
.andExpect(redirectedUrl("/boards"))     // 리다이렉트 URL
.andExpect(model().attributeExists("boards"))       // 모델 속성 존재
.andExpect(model().attribute("keyword", "스프링")) // 모델 속성 값
.andExpect(model().hasErrors())          // 검증 오류 존재
.andExpect(content().string(containsString("제목"))) // 응답 본문 포함
.andDo(print())                          // 요청·응답 전체 출력
```

### @WithMockUser — 인증 사용자 시뮬레이션

```java
// 기본 사용 — username="user", role="USER"
@WithMockUser
void test() { ... }

// 커스텀 설정
@WithMockUser(username = "admin", roles = {"ADMIN"})
void adminTest() { ... }

// CustomUserDetails를 사용하는 경우
// — 별도 @WithSecurityContext 팩토리 구현이 필요하나, 간단한 테스트는 @WithMockUser로 충분
```

---

## 8.6 파일 업로드 테스트 — MockMultipartFile

`MockMultipartFile`은 실제 파일 없이 `MultipartFile`을 흉내 냅니다.

### FileService 단위 테스트

```java
// src/test/java/com/example/imageboard/service/FileServiceTest.java
package com.example.imageboard.service;

import com.example.imageboard.config.FileProperties;
import com.example.imageboard.exception.FileUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("FileService 단위 테스트")
class FileServiceTest {

    @TempDir                          // JUnit 5 — 테스트용 임시 디렉터리 자동 생성·삭제
    Path tempUploadDir;

    @TempDir
    Path tempThumbnailDir;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        FileProperties props = new FileProperties();
        props.setUploadDir(tempUploadDir.toString());
        props.setThumbnailDir(tempThumbnailDir.toString());
        props.setAllowedExtensions(List.of("jpg", "jpeg", "png", "gif", "webp"));
        fileService = new FileService(props);
    }

    @Test
    @DisplayName("jpg 파일을 업로드하면 UUID 기반 파일명으로 저장된다")
    void store_jpg_success() {
        // given — 1x1 픽셀 PNG 이미지 바이트 (최소 유효 이미지)
        MultipartFile file = new MockMultipartFile(
                "images",                        // 폼 필드명
                "cat.jpg",                       // 원본 파일명
                "image/jpeg",                    // Content-Type
                createMinimalJpegBytes()         // 파일 내용 (바이트 배열)
        );

        // when
        String storedName = fileService.store(file);

        // then
        assertThat(storedName).endsWith(".jpg");
        assertThat(storedName).isNotEqualTo("cat.jpg");        // UUID로 변환됨
        assertThat(tempUploadDir.resolve(storedName)).exists(); // 실제 파일 존재 확인
    }

    @Test
    @DisplayName("허용되지 않는 확장자 파일은 예외가 발생한다")
    void store_invalidExtension_throwsException() {
        // given
        MultipartFile file = new MockMultipartFile(
                "images", "virus.exe", "application/octet-stream",
                "fake content".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> fileService.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exe");
    }

    @Test
    @DisplayName("빈 파일을 업로드하면 예외가 발생한다")
    void store_emptyFile_throwsException() {
        // given
        MultipartFile emptyFile = new MockMultipartFile(
                "images", "empty.jpg", "image/jpeg", new byte[0]
        );

        // when & then
        assertThatThrownBy(() -> fileService.store(emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비어 있습니다");
    }

    @Test
    @DisplayName("썸네일 생성 후 썸네일 디렉터리에 파일이 존재한다")
    void createThumbnail_success() throws Exception {
        // given — 먼저 파일 저장
        MultipartFile file = new MockMultipartFile(
                "images", "test.jpg", "image/jpeg", createMinimalJpegBytes()
        );
        String storedName = fileService.store(file);

        // when
        fileService.createThumbnail(storedName);

        // then
        assertThat(tempThumbnailDir.resolve(storedName)).exists();
    }

    @Test
    @DisplayName("파일 삭제 시 업로드 파일과 썸네일이 모두 제거된다")
    void delete_removesAllFiles() throws Exception {
        // given
        MultipartFile file = new MockMultipartFile(
                "images", "delete_me.jpg", "image/jpeg", createMinimalJpegBytes()
        );
        String storedName = fileService.store(file);
        fileService.createThumbnail(storedName);

        // when
        fileService.delete(storedName);

        // then
        assertThat(tempUploadDir.resolve(storedName)).doesNotExist();
        assertThat(tempThumbnailDir.resolve(storedName)).doesNotExist();
    }

    // ── 헬퍼 — 최소 유효 JPEG 바이트 생성 ─────────────────────────────
    private byte[] createMinimalJpegBytes() {
        // SOI(시작) + EOI(종료) 마커만 있는 최소 JPEG
        return new byte[]{
                (byte) 0xFF, (byte) 0xD8,   // SOI
                (byte) 0xFF, (byte) 0xD9    // EOI
        };
    }
}
```

### Controller 파일 업로드 테스트

```java
// BoardControllerTest.java — 파일 업로드 테스트 추가
@Test
@DisplayName("POST /boards — 이미지와 함께 게시글을 저장하면 리다이렉트된다")
@WithMockUser
void create_withImage() throws Exception {
    // given
    MockMultipartFile imageFile = new MockMultipartFile(
            "images",          // request 파라미터명 (BoardCreateRequest.images)
            "photo.jpg",
            "image/jpeg",
            "fake image content".getBytes()
    );
    given(boardService.create(any(), any())).willReturn(1L);

    // when & then
    mockMvc.perform(multipart("/boards")    // multipart POST
                    .file(imageFile)
                    .param("title", "이미지 게시글")
                    .param("content", "이미지가 있는 내용")
                    .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/boards/1"));
}

@Test
@DisplayName("POST /boards — 여러 이미지를 함께 업로드할 수 있다")
@WithMockUser
void create_withMultipleImages() throws Exception {
    // given
    MockMultipartFile image1 = new MockMultipartFile(
            "images", "img1.jpg", "image/jpeg", "content1".getBytes());
    MockMultipartFile image2 = new MockMultipartFile(
            "images", "img2.png", "image/png", "content2".getBytes());
    given(boardService.create(any(), any())).willReturn(1L);

    // when & then
    mockMvc.perform(multipart("/boards")
                    .file(image1)
                    .file(image2)
                    .param("title", "다중 이미지 게시글")
                    .param("content", "내용")
                    .with(csrf()))
            .andExpect(status().is3xxRedirection());
}
```

---

## 8.7 통합 테스트 — @SpringBootTest

`@SpringBootTest`는 전체 ApplicationContext를 로딩합니다. 실제 MariaDB 대신 H2를 사용해 속도를 유지하면서도 전체 계층을 관통하는 흐름을 검증합니다.

```java
// src/test/java/com/example/imageboard/BoardIntegrationTest.java
package com.example.imageboard;

import com.example.imageboard.dto.BoardCreateRequest;
import com.example.imageboard.dto.BoardResponse;
import com.example.imageboard.entity.Member;
import com.example.imageboard.repository.MemberRepository;
import com.example.imageboard.service.BoardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional   // 테스트 후 롤백하여 DB 상태를 원상 복구
@DisplayName("게시판 통합 테스트")
class BoardIntegrationTest {

    @Autowired BoardService boardService;
    @Autowired MemberRepository memberRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Long testMemberId;

    @BeforeEach
    void setUp() {
        Member member = memberRepository.save(
                Member.create("integtest", passwordEncoder.encode("pw"), "통합테스터")
        );
        testMemberId = member.getId();
    }

    @Test
    @DisplayName("게시글 저장 후 조회하면 동일한 내용이 반환된다")
    void createAndFind() {
        // given
        BoardCreateRequest request = new BoardCreateRequest();
        request.setTitle("통합 테스트 제목");
        request.setContent("통합 테스트 내용");

        // when
        Long boardId = boardService.create(request, testMemberId);
        BoardResponse found = boardService.findById(boardId);

        // then
        assertThat(found.getTitle()).isEqualTo("통합 테스트 제목");
        assertThat(found.getContent()).isEqualTo("통합 테스트 내용");
        assertThat(found.getAuthor()).isEqualTo("통합테스터");
    }

    @Test
    @DisplayName("게시글 수정 후 조회하면 변경된 내용이 반환된다")
    void updateAndFind() {
        // given
        BoardCreateRequest createReq = new BoardCreateRequest();
        createReq.setTitle("원래 제목");
        createReq.setContent("원래 내용");
        Long boardId = boardService.create(createReq, testMemberId);

        BoardCreateRequest updateReq = new BoardCreateRequest();
        updateReq.setTitle("수정된 제목");
        updateReq.setContent("수정된 내용");

        // when
        boardService.update(boardId, updateReq);
        BoardResponse found = boardService.findById(boardId);

        // then
        assertThat(found.getTitle()).isEqualTo("수정된 제목");
        assertThat(found.getContent()).isEqualTo("수정된 내용");
    }
}
```

### 테스트용 application.yml 분리

`src/test/resources/application.yml`을 생성하면 테스트 실행 시 이 파일이 우선 적용됩니다.

```yaml
# src/test/resources/application.yml — 테스트 전용 설정
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL   # H2 인메모리 (MySQL 호환 모드)
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop              # 테스트 시작 시 생성, 종료 시 삭제
    show-sql: false                      # 테스트 중 SQL 출력 비활성화
  security:
    user:
      name: testuser
      password: testpass

file:
  upload-dir: ${java.io.tmpdir}/imageboard-test/uploads
  thumbnail-dir: ${java.io.tmpdir}/imageboard-test/thumbnails
  allowed-extensions:
    - jpg
    - jpeg
    - png
    - gif
    - webp

logging:
  level:
    com.example.imageboard: WARN        # 테스트 중 로그 최소화
```

---

## 8.8 테스트 작성 원칙

### FIRST 원칙

| 원칙 | 의미 | 적용 방법 |
|---|---|---|
| **F**ast | 빠르게 실행 | 슬라이스 테스트·Mock 우선 사용 |
| **I**ndependent | 독립적 | `@BeforeEach`로 테스트별 데이터 초기화, `@Transactional`로 롤백 |
| **R**epeatable | 반복 가능 | 하드코딩된 날짜·외부 API 의존 제거 |
| **S**elf-validating | 자가 검증 | `assertThat()`으로 명확한 성공·실패 판단 |
| **T**imely | 적시 작성 | 기능 구현과 함께 또는 직전에 작성 |

### 테스트 구조 — given / when / then

```java
@Test
@DisplayName("테스트 의도를 한국어로 명확하게 작성")
void methodName_condition_expectedResult() {

    // given — 테스트 환경 준비
    Board board = Board.create("제목", "내용", testMember);

    // when — 테스트 대상 실행
    board.increaseViewCount();

    // then — 결과 검증
    assertThat(board.getViewCount()).isEqualTo(1);
}
```

### AssertJ 주요 Assertion

```java
// 기본
assertThat(actual).isEqualTo(expected);
assertThat(actual).isNotNull();
assertThat(actual).isNull();
assertThat(flag).isTrue();
assertThat(flag).isFalse();

// 문자열
assertThat(str).isEqualTo("hello");
assertThat(str).contains("ello");
assertThat(str).startsWith("he");
assertThat(str).hasSize(5);

// 컬렉션
assertThat(list).hasSize(3);
assertThat(list).isEmpty();
assertThat(list).contains("a", "b");
assertThat(list).containsExactly("a", "b", "c");     // 순서 포함
assertThat(list).containsExactlyInAnyOrder("c", "a", "b"); // 순서 무관
assertThat(list).extracting("title")
                .containsExactlyInAnyOrder("제목1", "제목2");

// 예외
assertThatThrownBy(() -> service.findById(999L))
        .isInstanceOf(BoardNotFoundException.class)
        .hasMessageContaining("999");

// 파일
assertThat(path).exists();
assertThat(path).doesNotExist();
```

---

## 정리

| 핵심 개념 | 내용 |
|---|---|
| `@ExtendWith(MockitoExtension)` | Spring 없이 Mockito만으로 단위 테스트 |
| `@DataJpaTest` | JPA 빈만 로딩, H2로 Repository 쿼리 검증 |
| `@WebMvcTest` | Web 계층만 로딩, MockMvc로 HTTP 요청·응답 검증 |
| `@SpringBootTest` | 전체 컨텍스트 통합 테스트 |
| `@MockBean` | Spring 컨텍스트에서 특정 빈을 Mock으로 대체 |
| `@WithMockUser` | 테스트에서 인증된 사용자 시뮬레이션 |
| `MockMultipartFile` | 실제 파일 없이 파일 업로드 테스트 |
| `@TempDir` | 테스트용 임시 디렉터리 자동 생성·정리 |
| `@Transactional` (테스트) | 테스트 후 DB를 자동으로 롤백 |

---

## 연습 문제

1. `BoardService.findAll()`에 페이징 결과가 올바르게 반환되는지 Mockito로 검증하는 단위 테스트를 작성해 보세요.
2. `@DataJpaTest`에서 `MemberRepository.existsByUsername()`이 중복 아이디를 정확히 감지하는지 테스트해 보세요.
3. 비로그인 상태에서 `DELETE /boards/1`을 요청했을 때 로그인 페이지로 리다이렉트되는지 `@WebMvcTest`로 검증해 보세요.
4. 파일 크기가 0인 `MockMultipartFile`을 업로드할 때 `FileService`가 예외를 발생시키는지 테스트해 보세요.

---

## 다음 장 예고

9장에서는 빌드·배포를 다룹니다. `bootJar`로 실행 가능한 JAR를 패키징하고, 프로파일로 개발·운영 환경을 분리하며, 환경변수로 민감 정보를 외부화합니다.
