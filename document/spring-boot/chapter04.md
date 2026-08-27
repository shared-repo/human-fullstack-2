# 4장. JPA 데이터 연동

---

## 학습 목표

- Spring Data JPA와 MariaDB를 연결하고 설정할 수 있다.
- 이미지 게시판에 필요한 Entity를 설계하고 연관관계를 매핑할 수 있다.
- `JpaRepository`를 활용해 기본 CRUD를 구현할 수 있다.
- Query Method, JPQL, `@Query`의 차이를 이해하고 상황에 맞게 사용할 수 있다.
- 트랜잭션과 영속성 컨텍스트의 동작 원리를 이해한다.
- 3장의 더미 데이터를 실제 MariaDB 연동으로 교체할 수 있다.

---

## 4.1 Spring Data JPA 개요

### JPA, Hibernate, Spring Data JPA의 관계

세 기술은 서로 다른 계층에 위치합니다.

```
Spring Data JPA       ← Repository 인터페이스 자동 구현, 편의 기능 제공
      ↓
   Hibernate          ← JPA 구현체, 실제 SQL 생성 및 실행
      ↓
    JPA API           ← 표준 인터페이스 명세 (jakarta.persistence.*)
      ↓
   JDBC / DB          ← MariaDB
```

| 기술 | 역할 |
|---|---|
| JPA | Java 표준 ORM 명세. `@Entity`, `@Id`, `@Column` 등 어노테이션 정의 |
| Hibernate | JPA의 실제 구현체. SQL 생성, 캐싱, 영속성 컨텍스트 관리 |
| Spring Data JPA | `JpaRepository` 인터페이스 제공. 반복적인 CRUD 코드를 자동으로 구현 |

### Spring Data JPA가 해결하는 문제

JPA만 사용하면 `EntityManager`를 직접 다루는 코드가 반복됩니다.

```java
// 순수 JPA — EntityManager 직접 사용
@Repository
public class BoardRepository {

    @PersistenceContext
    private EntityManager em;

    public Board findById(Long id) {
        return em.find(Board.class, id);
    }

    public List<Board> findAll() {
        return em.createQuery("SELECT b FROM Board b", Board.class)
                 .getResultList();
    }

    public void save(Board board) {
        em.persist(board);
    }
}
```

Spring Data JPA를 사용하면 인터페이스 선언만으로 위 코드가 자동 구현됩니다.

```java
// Spring Data JPA — 인터페이스만 선언
public interface BoardRepository extends JpaRepository<Board, Long> {
    // 기본 CRUD 메서드는 자동 구현됨
}
```

---

## 4.2 Entity 설계

이미지 게시판에 필요한 세 Entity와 연관관계를 설계합니다.

### ERD

```
Member (회원)
  id          PK
  username    로그인 아이디
  password    비밀번호
  nickname    닉네임
  created_at

Board (게시글)
  id          PK
  title       제목
  content     내용
  view_count  조회수
  member_id   FK → Member
  created_at
  updated_at

AttachedImage (첨부 이미지)
  id          PK
  original_name  원본 파일명
  stored_name    서버 저장 파일명
  file_path      저장 경로
  file_size      파일 크기 (bytes)
  board_id    FK → Board
  created_at
```

### 공통 BaseEntity

생성일, 수정일은 모든 Entity에서 공통으로 사용되므로 상위 클래스로 분리합니다.

```java
// src/main/java/com/example/imageboard/entity/BaseEntity.java
package com.example.imageboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass                          // 테이블을 만들지 않고 자식 Entity에 필드만 상속
@EntityListeners(AuditingEntityListener.class) // 날짜 자동 처리
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

`@CreatedDate`, `@LastModifiedDate`가 동작하려면 메인 클래스에 `@EnableJpaAuditing`을 추가합니다.

```java
// ImageboardApplication.java
@SpringBootApplication
@EnableJpaAuditing          // JPA Auditing 활성화
public class ImageboardApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImageboardApplication.class, args);
    }
}
```

### Member Entity

```java
// src/main/java/com/example/imageboard/entity/Member.java
package com.example.imageboard.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자 (외부 직접 생성 방지)
@ToString(exclude = "boards")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @OneToMany(mappedBy = "member")
    private java.util.List<Board> boards = new java.util.ArrayList<>();

    // 생성 팩토리 메서드 — 객체 생성 방법을 한 곳에서 관리
    public static Member create(String username, String password, String nickname) {
        Member member = new Member();
        member.username = username;
        member.password = password;
        member.nickname = nickname;
        return member;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}
```

### Board Entity

```java
// src/main/java/com/example/imageboard/entity/Board.java
package com.example.imageboard.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "board")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"member", "images"})
public class Board extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob                                    // 긴 텍스트 (TEXT 타입)
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private int viewCount = 0;

    // 다대일 연관관계 — Board N : Member 1
    @ManyToOne(fetch = FetchType.LAZY)      // 지연 로딩 (권장)
    @JoinColumn(name = "member_id")
    private Member member;

    // 일대다 연관관계 — Board 1 : AttachedImage N
    @OneToMany(mappedBy = "board",
               cascade = CascadeType.ALL,  // Board 저장/삭제 시 Image도 함께 처리
               orphanRemoval = true)       // Board에서 제거된 Image는 DB에서도 삭제
    private List<AttachedImage> images = new ArrayList<>();

    // ── 생성 팩토리 ──────────────────────────────────────
    public static Board create(String title, String content, Member member) {
        Board board = new Board();
        board.title = title;
        board.content = content;
        board.member = member;
        return board;
    }

    // ── 비즈니스 메서드 ───────────────────────────────────
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void addImage(AttachedImage image) {
        this.images.add(image);
        image.setBoard(this);
    }

    /** 대표 이미지 경로 반환 (첫 번째 이미지) */
    public String getThumbnailUrl() {
        if (images.isEmpty()) return null;
        return "/images/" + images.get(0).getStoredName();
    }
}
```

### AttachedImage Entity

```java
// src/main/java/com/example/imageboard/entity/AttachedImage.java
package com.example.imageboard.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "attached_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttachedImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalName;    // 원본 파일명 (예: 고양이.jpg)

    @Column(nullable = false, unique = true)
    private String storedName;      // 서버 저장 파일명 (예: uuid.jpg)

    @Column(nullable = false)
    private String filePath;        // 저장 경로

    private long fileSize;          // 파일 크기 (bytes)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    // board 필드 setter (Board.addImage()에서만 사용)
    void setBoard(Board board) {
        this.board = board;
    }

    public static AttachedImage create(String originalName, String storedName,
                                       String filePath, long fileSize) {
        AttachedImage image = new AttachedImage();
        image.originalName = originalName;
        image.storedName = storedName;
        image.filePath = filePath;
        image.fileSize = fileSize;
        return image;
    }
}
```

### Entity 설계 핵심 원칙

**① 기본 생성자는 `protected`로 제한**

JPA는 리플렉션으로 객체를 생성하기 위해 기본 생성자가 필요합니다. 그러나 `public`으로 열어두면 잘못된 방법으로 객체를 생성할 수 있으므로 `protected`로 제한합니다.

```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)
```

**② Setter 대신 의미 있는 메서드 제공**

무분별한 Setter는 객체의 상태를 예측하기 어렵게 만듭니다. 비즈니스 의미가 담긴 메서드를 통해서만 상태를 변경합니다.

```java
// ❌ Setter 방식 — 언제 왜 바뀌는지 알기 어려움
board.setTitle("새 제목");
board.setViewCount(board.getViewCount() + 1);

// ✅ 의미 있는 메서드 — 의도가 명확
board.update("새 제목", "새 내용");
board.increaseViewCount();
```

**③ 연관관계는 지연 로딩(`LAZY`)으로**

```java
// ❌ 즉시 로딩 — Board 조회 시 Member를 항상 JOIN해서 가져옴
@ManyToOne(fetch = FetchType.EAGER)  // 기본값이지만 피해야 함

// ✅ 지연 로딩 — Member가 실제로 필요할 때만 SELECT
@ManyToOne(fetch = FetchType.LAZY)
```

---

## 4.3 Repository 구현

### JpaRepository 기본 CRUD

```java
// src/main/java/com/example/imageboard/repository/BoardRepository.java
package com.example.imageboard.repository;

import com.example.imageboard.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, Long> {
    // JpaRepository<Entity 타입, PK 타입>
}
```

`JpaRepository`를 상속하면 다음 메서드가 자동으로 제공됩니다.

| 메서드 | 설명 |
|---|---|
| `save(entity)` | 저장 (id 없으면 INSERT, 있으면 UPDATE) |
| `findById(id)` | 단건 조회 → `Optional<T>` 반환 |
| `findAll()` | 전체 조회 |
| `findAll(Pageable)` | 페이징 조회 |
| `count()` | 전체 건수 |
| `delete(entity)` | 삭제 |
| `deleteById(id)` | id로 삭제 |
| `existsById(id)` | 존재 여부 확인 |

```java
// src/main/java/com/example/imageboard/repository/MemberRepository.java
package com.example.imageboard.repository;

import com.example.imageboard.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
```

```java
// src/main/java/com/example/imageboard/repository/AttachedImageRepository.java
package com.example.imageboard.repository;

import com.example.imageboard.entity.AttachedImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachedImageRepository extends JpaRepository<AttachedImage, Long> {
}
```

---

## 4.4 쿼리 작성 방법 세 가지

Spring Data JPA는 쿼리를 작성하는 세 가지 방법을 제공합니다.

### ① Query Method — 메서드 이름으로 쿼리 생성

메서드 이름 규칙을 따르면 Spring Data JPA가 자동으로 JPQL을 생성합니다.

```java
public interface BoardRepository extends JpaRepository<Board, Long> {

    // SELECT b FROM Board b WHERE b.title LIKE %:title%
    List<Board> findByTitleContaining(String title);

    // SELECT b FROM Board b WHERE b.member = :member ORDER BY b.createdAt DESC
    List<Board> findByMemberOrderByCreatedAtDesc(Member member);

    // SELECT b FROM Board b WHERE b.title LIKE %:keyword% OR b.content LIKE %:keyword%
    List<Board> findByTitleContainingOrContentContaining(String titleKeyword,
                                                          String contentKeyword);

    // SELECT COUNT(b) FROM Board b WHERE b.member = :member
    long countByMember(Member member);

    // SELECT b FROM Board b WHERE b.viewCount >= :count
    List<Board> findByViewCountGreaterThanEqual(int count);
}
```

**주요 키워드**

| 키워드 | 예시 메서드 | 조건 |
|---|---|---|
| `findBy` | `findByTitle` | `WHERE title = ?` |
| `Containing` | `findByTitleContaining` | `WHERE title LIKE %?%` |
| `StartingWith` | `findByTitleStartingWith` | `WHERE title LIKE ?%` |
| `OrderBy` | `findByMemberOrderByCreatedAtDesc` | `ORDER BY created_at DESC` |
| `GreaterThan` | `findByViewCountGreaterThan` | `WHERE view_count > ?` |
| `Between` | `findByCreatedAtBetween` | `WHERE created_at BETWEEN ? AND ?` |
| `IsNull` | `findByThumbnailIsNull` | `WHERE thumbnail IS NULL` |
| `countBy` | `countByMember` | `SELECT COUNT(*)` |

> 메서드 이름이 길어지면 가독성이 떨어집니다. 조건이 두 개를 초과하면 `@Query` 사용을 권장합니다.

### ② JPQL + @Query

JPQL(Java Persistence Query Language)은 엔티티 객체를 대상으로 하는 쿼리 언어입니다. 테이블명 대신 **클래스명**, 컬럼명 대신 **필드명**을 사용합니다.

```java
public interface BoardRepository extends JpaRepository<Board, Long> {

    // 기본 JPQL
    @Query("SELECT b FROM Board b ORDER BY b.createdAt DESC")
    List<Board> findAllOrderByCreatedAtDesc();

    // JOIN FETCH — 연관 Entity를 한 번에 조회 (N+1 문제 해결)
    @Query("SELECT b FROM Board b JOIN FETCH b.member ORDER BY b.createdAt DESC")
    List<Board> findAllWithMember();

    // 파라미터 바인딩
    @Query("SELECT b FROM Board b WHERE b.title LIKE %:keyword% OR b.content LIKE %:keyword%")
    List<Board> searchByKeyword(@Param("keyword") String keyword);

    // 페이징 + 검색
    @Query("SELECT b FROM Board b WHERE b.title LIKE %:keyword% ORDER BY b.createdAt DESC")
    Page<Board> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    // countQuery 분리 (페이징 성능 최적화)
    @Query(value = "SELECT b FROM Board b JOIN FETCH b.member WHERE b.title LIKE %:keyword%",
           countQuery = "SELECT COUNT(b) FROM Board b WHERE b.title LIKE %:keyword%")
    Page<Board> searchWithMember(@Param("keyword") String keyword, Pageable pageable);
}
```

**JPQL vs SQL 비교**

```sql
-- SQL (테이블/컬럼 기준)
SELECT b.id, b.title, m.nickname
FROM board b
JOIN member m ON b.member_id = m.id
WHERE b.title LIKE '%스프링%'
```

```java
// JPQL (엔티티/필드 기준)
"SELECT b FROM Board b JOIN b.member m WHERE b.title LIKE '%스프링%'"
```

### ③ 네이티브 쿼리 — @Query(nativeQuery = true)

DB 특화 문법이 필요하거나 복잡한 쿼리를 직접 SQL로 작성할 때 사용합니다.

```java
@Query(value = "SELECT * FROM board ORDER BY view_count DESC LIMIT :limit",
       nativeQuery = true)
List<Board> findTopNByViewCount(@Param("limit") int limit);
```

> 네이티브 쿼리는 DB 종속적이므로 꼭 필요한 경우에만 사용합니다.

### 선택 기준 요약

| 방법 | 사용 상황 |
|---|---|
| Query Method | 단순 조건 1~2개 |
| JPQL + @Query | 복잡한 조건, 정렬, 페이징 |
| 네이티브 쿼리 | DB 전용 함수, 극한 성능 최적화 |

---

## 4.5 트랜잭션과 영속성 컨텍스트

### 영속성 컨텍스트 복습

영속성 컨텍스트는 JPA가 관리하는 Entity의 1차 캐시입니다. 같은 트랜잭션 내에서 동일한 id로 Entity를 두 번 조회하면 DB를 두 번 조회하지 않고 캐시에서 반환합니다.

```java
@Transactional
public void example() {
    Board board1 = boardRepository.findById(1L).get(); // DB SELECT
    Board board2 = boardRepository.findById(1L).get(); // 캐시에서 반환 (SELECT 없음)

    System.out.println(board1 == board2); // true (같은 인스턴스)
}
```

### 변경 감지 (Dirty Checking)

트랜잭션 내에서 영속 상태의 Entity 필드를 변경하면, 트랜잭션 커밋 시점에 JPA가 자동으로 UPDATE SQL을 실행합니다. **`save()`를 직접 호출하지 않아도 됩니다.**

```java
@Transactional
public void updateBoard(Long id, String title, String content) {
    Board board = boardRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));

    board.update(title, content); // 영속 상태의 필드 변경
    // ← 트랜잭션 종료 시 자동으로 UPDATE SQL 실행 (save() 불필요)
}
```

### @Transactional 위치

```java
// ❌ Repository에만 @Transactional — 여러 Repository 호출 시 각각 별도 트랜잭션
public void createBoard(...) {
    boardRepository.save(board);     // 트랜잭션 1
    imageRepository.save(image);     // 트랜잭션 2 — board 저장 실패해도 image는 저장됨
}

// ✅ Service에 @Transactional — 모든 작업이 하나의 트랜잭션
@Transactional
public void createBoard(...) {
    boardRepository.save(board);     // 트랜잭션 1 참여
    imageRepository.save(image);     // 트랜잭션 1 참여 — 하나라도 실패하면 모두 롤백
}
```

### @Transactional(readOnly = true)

조회 전용 메서드에 `readOnly = true`를 설정하면 성능 최적화 효과가 있습니다.

```java
@Transactional(readOnly = true)  // 플러시 생략, 스냅샷 미생성 → 성능 향상
public List<BoardResponse> findAll() {
    return boardRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
}
```

---

## 4.6 N+1 문제와 해결

### N+1 문제 발생

게시글 목록을 조회하면 Board 1번 SELECT 후, 각 Board의 Member를 조회하기 위해 N번의 SELECT가 추가로 발생합니다.

```java
// 게시글이 10개면 총 11번 SELECT 발생 (1 + 10)
List<Board> boards = boardRepository.findAll();
boards.forEach(b -> System.out.println(b.getMember().getNickname())); // 각각 SELECT
```

```sql
-- 실행되는 SQL
SELECT * FROM board;                          -- 1번
SELECT * FROM member WHERE id = 1;            -- board_id=1의 member
SELECT * FROM member WHERE id = 2;            -- board_id=2의 member
... (N번 반복)
```

### 해결 — JOIN FETCH

```java
// BoardRepository
@Query("SELECT b FROM Board b JOIN FETCH b.member ORDER BY b.createdAt DESC")
List<Board> findAllWithMember();
```

```sql
-- JOIN FETCH 적용 시 SQL 1번으로 해결
SELECT b.*, m.*
FROM board b
INNER JOIN member m ON b.member_id = m.id
ORDER BY b.created_at DESC;
```

---

## 4.7 Service 구현 — 더미 데이터에서 JPA로 교체

3장에서 더미 데이터를 반환하던 `BoardService`를 실제 JPA 연동으로 교체합니다.

```java
// src/main/java/com/example/imageboard/service/BoardService.java
package com.example.imageboard.service;

import com.example.imageboard.dto.BoardCreateRequest;
import com.example.imageboard.dto.BoardResponse;
import com.example.imageboard.entity.Board;
import com.example.imageboard.entity.Member;
import com.example.imageboard.repository.BoardRepository;
import com.example.imageboard.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true) // 클래스 기본은 readOnly
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;

    /** 게시글 목록 조회 */
    public List<BoardResponse> findAll() {
        return boardRepository.findAllWithMember().stream()
                .map(this::toResponse)
                .toList();
    }

    /** 게시글 단건 조회 */
    @Transactional // 조회수 증가(쓰기)가 필요하므로 readOnly 해제
    public BoardResponse findById(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
        board.increaseViewCount(); // 변경 감지로 UPDATE 자동 실행
        return toResponse(board);
    }

    /** 게시글 등록 */
    @Transactional
    public Long create(BoardCreateRequest request) {
        // 임시: 첫 번째 회원을 작성자로 설정 (6장 Spring Security 연동 후 변경)
        Member member = memberRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("회원이 없습니다."));

        Board board = Board.create(request.getTitle(), request.getContent(), member);
        boardRepository.save(board);
        return board.getId();
    }

    /** 게시글 수정 */
    @Transactional
    public void update(Long id, BoardCreateRequest request) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
        board.update(request.getTitle(), request.getContent()); // 변경 감지
    }

    /** 게시글 삭제 */
    @Transactional
    public void delete(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
        boardRepository.delete(board);
    }

    /** Entity → DTO 변환 */
    private BoardResponse toResponse(Board board) {
        return BoardResponse.builder()
                .id(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .author(board.getMember().getNickname())
                .viewCount(board.getViewCount())
                .thumbnailUrl(board.getThumbnailUrl())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build();
    }
}
```

---

## 4.8 Controller 수정 — 수정·삭제 엔드포인트 추가

3장의 `BoardController`에 수정·삭제 기능을 추가합니다.

```java
/** 게시글 수정 폼 */
@GetMapping("/{id}/edit")
public String editForm(@PathVariable Long id, Model model) {
    BoardResponse board = boardService.findById(id);
    BoardCreateRequest request = new BoardCreateRequest();
    request.setTitle(board.getTitle());
    request.setContent(board.getContent());
    model.addAttribute("boardId", id);
    model.addAttribute("boardCreateRequest", request);
    return "board/edit";
}

/** 게시글 수정 */
@PutMapping("/{id}")
public String update(@PathVariable Long id,
                     @Valid @ModelAttribute BoardCreateRequest request,
                     BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
        return "board/edit";
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
```

---

## 4.9 초기 데이터 설정

애플리케이션 실행 시 테스트용 회원 데이터를 자동으로 생성합니다.

```java
// src/main/java/com/example/imageboard/config/DataInitializer.java
package com.example.imageboard.config;

import com.example.imageboard.entity.Member;
import com.example.imageboard.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (memberRepository.count() == 0) {
            memberRepository.save(
                Member.create("admin", "password123", "관리자")
            );
            System.out.println("[초기화] 테스트 회원 데이터 생성 완료");
        }
    }
}
```

---

## 4.10 실행 및 동작 확인

### application.yml 확인

```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/imageboard?characterEncoding=UTF-8&serverTimezone=Asia/Seoul
    username: boarduser
    password: board1234
    driver-class-name: org.mariadb.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update   # 엔티티 기반으로 테이블 자동 생성/변경
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

### 확인 순서

```bash
./gradlew bootRun
```

1. 애플리케이션 실행 → Hibernate가 `board`, `member`, `attached_image` 테이블을 자동 생성
2. `DataInitializer`가 테스트 회원 1건 INSERT
3. `http://localhost:8080/boards/create`에서 게시글 작성 → MariaDB에 저장 확인
4. 목록 화면에서 게시글 조회 → 콘솔에 SQL 출력 확인

### 생성된 테이블 확인

```sql
-- MariaDB에서 테이블 생성 확인
SHOW TABLES;
DESC board;
DESC member;
DESC attached_image;
```

---

## 정리

| 핵심 개념 | 내용 |
|---|---|
| `@MappedSuperclass` | 테이블 없이 공통 필드를 상속 |
| `@EnableJpaAuditing` | `@CreatedDate`, `@LastModifiedDate` 자동 처리 |
| `FetchType.LAZY` | 연관 Entity를 실제 사용 시점에 조회 |
| 변경 감지 | 트랜잭션 내 영속 Entity 변경 → 커밋 시 자동 UPDATE |
| `@Transactional(readOnly = true)` | 조회 전용 트랜잭션 최적화 |
| JOIN FETCH | N+1 문제 해결 |
| Query Method | 메서드 이름으로 쿼리 자동 생성 |
| `@Query` | JPQL로 복잡한 쿼리 직접 작성 |

---

## 연습 문제

1. `BoardRepository`에 Query Method를 추가하여 제목에 특정 키워드가 포함된 게시글을 조회해 보세요.
2. 게시글 목록에서 조회수가 높은 순으로 정렬하는 `@Query`를 작성해 보세요.
3. `application.yml`에서 `ddl-auto`를 `create`로 변경하고 재실행하면 어떻게 되는지 확인해 보세요. (단, 실습 후 반드시 `update`로 복구하세요.)

---

## 다음 장 예고

5장에서는 이미지 게시판의 핵심 기능인 이미지 업로드를 구현합니다. `MultipartFile`로 파일을 서버에 저장하고, `AttachedImage` Entity와 연동하여 게시글에 이미지를 첨부합니다.
