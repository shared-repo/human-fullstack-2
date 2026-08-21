# 나만의 맛집 노트 — 단계별 실습 매뉴얼 목차

> **대상** : Spring Framework 과정 수료생 (이해도 보충 실습)
> **환경** : Java 21 + Spring Framework 7.0.x + MyBatis + MariaDB
> **기간** : 하루 8시간 × 2.5일 (총 20시간)

---

## 단계 1. 프로젝트 환경 설정 (약 2시간)

**다루는 기술 요소**

- Maven 프로젝트 생성 (war 패키징)
- `pom.xml` — spring-webmvc, mybatis-spring, HikariCP, mariadb-java-client, jstl, lombok, slf4j/logback
- `web.xml` — CharacterEncodingFilter, ContextLoaderListener, DispatcherServlet
- `applicationContext.xml` — HikariCP DataSource, SqlSessionFactory, MapperScannerConfigurer, DataSourceTransactionManager, `<tx:annotation-driven/>`
- `spring-mvc.xml` — InternalResourceViewResolver, `<mvc:annotation-driven/>`, `<context:component-scan/>`
- `logback.xml` — 콘솔 로그 설정
- DB 연결 확인 (단순 SELECT 1 테스트)

---

## 단계 2. DB 테이블 설계 및 도메인 클래스 작성 (약 1시간)

**다루는 기술 요소**

- `member` 테이블 DDL — id(PK), login_id, password, name, reg_date
- `restaurant` 테이블 DDL — no(PK, AUTO_INCREMENT), member_id(FK), name, category, address, memo, visit_date, reg_date
- `Member.java`, `Restaurant.java` 도메인 클래스 — Lombok `@Data`, `@NoArgsConstructor`
- MyBatis `mybatis-config.xml` — typeAlias 등록

---

## 단계 3. 회원가입 구현 (약 1.5시간)

**다루는 기술 요소**

- `MemberMapper` 인터페이스 — `existsLoginId()`, `insert()`
- `MemberMapper.xml` — `<select>`, `<insert>`, `useGeneratedKeys`
- `MemberService` 인터페이스 + `MemberServiceImpl` — 아이디 중복 체크 로직, `@Service`
- `MemberController` — `@GetMapping("/register")`, `@PostMapping("/register")`
- `register.jsp` — 폼(아이디/비밀번호/이름), 오류 메시지 표시

---

## 단계 4. 로그인 · 로그아웃 구현 (약 1.5시간)

**다루는 기술 요소**

- `MemberMapper` — `selectByLoginId()`
- `MemberServiceImpl` — 비밀번호 일치 확인, `@Transactional(readOnly=true)`
- `MemberController` — 로그인 POST 처리, `HttpSession`에 로그인 회원 저장, 로그아웃(세션 무효화)
- `login.jsp` — 폼, 실패 메시지
- `LoginCheckInterceptor` — `HandlerInterceptor` 구현, `preHandle()`에서 세션 확인 후 로그인 페이지 리다이렉트
- `spring-mvc.xml` — `<mvc:interceptors>` 등록, 제외 경로(`/member/**`) 설정

---

## 단계 5. 맛집 목록 조회 구현 (약 2시간)

**다루는 기술 요소**

- `RestaurantMapper` 인터페이스 + XML — `selectAll()` (로그인 회원의 맛집만 조회, `WHERE member_id = #{memberId}`)
- `RestaurantService` + `RestaurantServiceImpl` — `@Transactional(readOnly=true)`
- `RestaurantController` — `@GetMapping("/restaurant/list")`, Model에 목록 담기
- `list.jsp` — JSTL `<c:forEach>`로 목록 출력, 등록·상세·수정·삭제 링크

---

## 단계 6. 맛집 등록 구현 (약 1.5시간)

**다루는 기술 요소**

- `RestaurantMapper` — `insert()`
- `RestaurantServiceImpl` — `@Transactional`, 세션에서 로그인 회원 id 꺼내서 Restaurant에 설정
- `RestaurantController` — `@GetMapping("/restaurant/write")`, `@PostMapping("/restaurant/write")`
- `write.jsp` — 등록 폼(가게명/카테고리/주소/메모/방문일)
- MyBatis `<insert>`, `useGeneratedKeys="true"`, `keyProperty="no"`

---

## 단계 7. 맛집 상세 조회 구현 (약 1시간)

**다루는 기술 요소**

- `RestaurantMapper` — `selectOne()`
- `RestaurantController` — `@GetMapping("/restaurant/detail")`
- `detail.jsp` — 맛집 정보 출력, 본인 작성 글일 때만 수정·삭제 버튼 표시 (`c:if`)
- 본인 확인 로직 — 세션의 `memberId`와 `restaurant.memberId` 비교

---

## 단계 8. 맛집 수정 구현 (약 1.5시간)

**다루는 기술 요소**

- `RestaurantMapper` — `update()`
- `RestaurantController` — `@GetMapping("/restaurant/edit")` (기존 데이터 폼에 채우기), `@PostMapping("/restaurant/edit")`
- `edit.jsp` — 기존 값이 채워진 수정 폼
- 본인 확인 후 수정 처리 (타인 수정 방지)
- `@Transactional`

---

## 단계 9. 맛집 삭제 구현 (약 0.5시간)

**다루는 기술 요소**

- `RestaurantMapper` — `delete()`
- `RestaurantController` — `@PostMapping("/restaurant/delete")`
- 본인 확인 후 삭제, 삭제 후 목록 페이지 리다이렉트
- `@Transactional`

---

## 단계 10. 카테고리 필터링 및 키워드 검색 추가 (약 2시간)

**다루는 기술 요소**

- `RestaurantMapper` — `selectAll()` 수정, 파라미터 `Map<String, Object>`
- MyBatis 동적 SQL — `<where>`, `<if test="...">` (카테고리 조건, 가게명 키워드 `LIKE`)
- `list.jsp` — 카테고리 드롭다운, 검색어 입력 폼, 검색 조건 유지 (`value="${param.keyword}"`)

---

## 단계 11. 마무리 점검 및 오류 해결 (약 2시간)

**다루는 기술 요소**

- 전체 기능 동작 확인 체크리스트
- 자주 발생하는 오류 패턴 — NullPointerException, 404/500, MyBatis BindingException, 세션 미설정
- `@ControllerAdvice` + `@ExceptionHandler` 기본 오류 페이지 적용
- 코드 전체 흐름 복습 (Controller → Service → Mapper → DB → JSP)

---

## 예상 일정

| 일차 | 단계 | 예상 소요 시간 |
|---|---|---|
| 1일차 | 단계 1 ~ 4 (환경설정 + 회원 기능) | 약 6시간 |
| 2일차 | 단계 5 ~ 9 (맛집 CRUD) | 약 8시간 |
| 3일차 오전 | 단계 10 ~ 11 (검색 + 마무리) | 약 4시간 |
| 3일차 오후 | 버퍼 (오류 해결 · 개인 진도 보충) | 약 4시간 |
