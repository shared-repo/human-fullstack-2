# pre-unit06. Spring Security 기반 로그인 구현

> **이 단원의 위치**  
> unit05까지 완료한 상태에서 unit06을 진행하기 전에 먼저 실습한다.  
> 여기서 구현한 `User` 엔티티, `UserDetailsService`, `JwtTokenProvider`, `AuthController`, `login.html`은 unit06에서 그대로 사용된다.

---

## 학습 목표

- Spring Security의 `UserDetailsService`로 DB 기반 사용자 인증을 구현할 수 있다.
- `PasswordEncoder`(BCrypt)로 비밀번호를 안전하게 저장하고 검증할 수 있다.
- `formLogin()`으로 세션 기반 로그인을 구성하고, 인증된 세션에서 WebSocket용 JWT를 발급할 수 있다.
- 일반 웹 페이지는 세션 인증, WebSocket/STOMP 연결은 JWT 인증으로 각각 처리하는 혼합 방식을 이해할 수 있다.

---

## 전체 인증 흐름

이 단원은 **세션 + JWT 혼합(Hybrid) 방식**을 사용한다.

| 구분 | 인증 방식 |
|---|---|
| 일반 웹 페이지 (`/`, `/chat/**`) | Spring Security 세션 |
| WebSocket / STOMP 연결 | JWT (unit06에서 완성) |

```
━━━━━━━━━━━━━━━━ ① 세션 로그인 ━━━━━━━━━━━━━━━━

[login.html]
    │  POST /auth/login (HTML 폼 제출)
    ▼
[Spring Security formLogin]
    │  UserDetailsServiceImpl → DB 인증 → 세션(JSESSIONID) 생성
    ▼
[/ (채팅 목록)]  ← defaultSuccessUrl 로 리다이렉트

━━━━━━━━━━━━━━ ② WebSocket JWT 발급 ━━━━━━━━━━━━━━

[room.html 로드]
    │  GET /auth/token  (세션 쿠키 자동 전송)
    ▼
[AuthController]
    │  Principal(세션) → JwtTokenProvider.createToken(username)
    ▼
[room.html]  { token, username } 수신
    │  STOMP 연결 시 JWT 헤더에 포함
    ▼
[unit06에서 구현]
```

> **왜 혼합 방식인가?**  
> WebSocket 핸드셰이크는 HTTP 쿠키를 지원하지만, STOMP 프레임 단위 인증에는 쿠키를 사용하기 어렵다.  
> 세션으로 로그인한 뒤 WebSocket 전용 JWT를 발급받으면, STOMP 헤더에 토큰을 실어 무상태(Stateless) 채널 인증이 가능해진다.

---

## 의존성 추가

`build.gradle`에 JPA와 H2 의존성을 추가한다.

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-websocket'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'

    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.11.5'
    runtimeOnly    'io.jsonwebtoken:jjwt-impl:0.11.5'
    runtimeOnly    'io.jsonwebtoken:jjwt-jackson:0.11.5'

    // 개발용 인메모리 DB (운영 환경에서는 MySQL 등으로 교체)
    runtimeOnly 'com.h2database:h2'
}
```

---

## 데이터베이스 설정

`application.properties`에 H2 설정을 추가한다.

```properties
# H2 인메모리 DB (개발 환경)
spring.datasource.url=jdbc:h2:mem:chatdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=create
spring.jpa.show-sql=true

# H2 콘솔 (개발 환경에서 데이터 확인용)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JWT 시크릿 키
jwt.secret=SpringWebSocketJwtSecretKey1234567890ABCDEF
```

> ⚠️ `spring.jpa.hibernate.ddl-auto=create`는 애플리케이션 재시작마다 테이블을 새로 생성한다.  
> 데이터를 유지해야 할 경우 `update`로 변경한다.

> 💡 **MySQL로 교체할 경우** `spring.datasource.*`를 MySQL 설정으로 바꾸고 `runtimeOnly 'com.mysql:mysql-connector-j'`를 추가한다.

---

## User 엔티티

#### User.java

```java
package com.springexample.webchat.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;   // 로그인 ID

    @Column(nullable = false)
    private String password;   // BCrypt 암호화 저장

    protected User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Long getId()           { return id; }
    public String getUsername()   { return username; }
    public String getPassword()   { return password; }
}
```

---

## UserRepository

#### UserRepository.java

```java
package com.springexample.webchat.repository;

import com.springexample.webchat.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

---

## UserDetailsServiceImpl

Spring Security가 로그인 시 호출하는 사용자 조회 서비스다.

#### UserDetailsServiceImpl.java

```java
package com.springexample.webchat.config;

import com.springexample.webchat.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        com.springexample.webchat.domain.User user =
            userRepository.findByUsername(username)
                .orElseThrow(() ->
                    new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
```

---

## SecurityConfig — PasswordEncoder · AuthenticationManager 등록

#### SecurityConfig.java

```java
package com.springexample.webchat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            // ① 세션 허용 — 일반 웹 페이지는 세션 인증 사용
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/ws/**").permitAll()              // WebSocket 핸드셰이크
                .requestMatchers("/css/**", "/js/**",
                                 "/images/**", "/h2-console/**").permitAll()
                .anyRequest().authenticated()                       // 그 외 모두 로그인 필요
            )
            // ② 폼 로그인 — HTML 폼 제출로 세션 생성
            .formLogin(form -> form
                .loginPage("/login.html")                          // 로그인 화면 경로
                .loginProcessingUrl("/auth/login")                 // 폼 action URL
                .defaultSuccessUrl("/", true)                      // 성공 시 이동
                .failureUrl("/login.html?error=true")              // 실패 시 이동
                .permitAll()
            )
            // ③ 로그아웃
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/login.html")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .headers(headers ->
                headers.frameOptions(frame -> frame.disable())     // H2 콘솔 iframe 허용
            );

        return http.build();
    }
}
```

> **포인트 정리**
> - `IF_REQUIRED`: 세션이 필요할 때만 생성한다 (`STATELESS`에서 변경).  
> - `formLogin()`: Spring Security가 직접 `/auth/login` POST 요청을 처리하므로 `AuthController`에 로그인 엔드포인트가 필요 없다.  
> - 인증되지 않은 사용자가 보호된 페이지에 접근하면 Spring Security가 자동으로 `/login.html`로 리다이렉트한다.  
> - `JwtAuthenticationFilter` 등록은 unit06에서 추가한다.

---

## JwtTokenProvider

unit06에서도 동일하게 사용하는 JWT 유틸리티 클래스다.

#### JwtTokenProvider.java

```java
package com.springexample.webchat.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Collections;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final Key secretKey;
    private final long expireMs = 1000L * 60 * 60; // 1시간

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String createToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireMs))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        String username = Jwts.parserBuilder()
                .setSigningKey(secretKey).build()
                .parseClaimsJws(token)
                .getBody().getSubject();

        return new UsernamePasswordAuthenticationToken(
            username, null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
```

---

## 테스트 데이터 초기화

애플리케이션 시작 시 테스트 사용자를 자동으로 DB에 저장한다.

#### DataInitializer.java

```java
package com.springexample.webchat.config;

import com.springexample.webchat.domain.User;
import com.springexample.webchat.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {
        if (userRepository.count() > 0) return; // 이미 데이터가 있으면 건너뜀

        userRepository.saveAll(List.of(
            new User("alice", passwordEncoder.encode("1234")),
            new User("bob",   passwordEncoder.encode("1234")),
            new User("carol", passwordEncoder.encode("1234"))
        ));

        System.out.println("[DataInitializer] 테스트 사용자 3명 생성 완료");
    }
}
```

| 테스트 계정 | 비밀번호 |
|---|---|
| alice | 1234 |
| bob   | 1234 |
| carol | 1234 |

---

## AuthController — WebSocket JWT 발급 API

로그인은 `formLogin()`이 처리하므로 `AuthController`는 세션 인증 완료 후 WebSocket 연결에 필요한 **JWT를 발급**하는 역할만 담당한다.

#### AuthController.java

```java
package com.springexample.webchat.controller;

import com.springexample.webchat.config.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * GET /auth/token
     * 세션으로 인증된 사용자에게 WebSocket 연결용 JWT를 발급한다.
     * 세션이 없으면 Spring Security가 /login.html 로 리다이렉트한다.
     */
    @GetMapping("/token")
    public ResponseEntity<?> getToken(Principal principal) {
        String token = jwtTokenProvider.createToken(principal.getName());
        return ResponseEntity.ok(Map.of(
            "token",    token,
            "username", principal.getName()
        ));
    }
}
```

> **흐름 요약**  
> 1. `formLogin()`이 `/auth/login` POST 처리 → 세션 생성  
> 2. `room.html` 로드 시 `GET /auth/token` 호출 → 세션 쿠키 자동 전송  
> 3. Spring Security가 세션을 확인하고 `Principal`에 사용자명을 주입  
> 4. `JwtTokenProvider.createToken()`으로 WebSocket 전용 JWT 발급

---

## login.html

`src/main/resources/static/login.html`에 작성한다.

`formLogin()`을 사용하므로 JavaScript `fetch()`가 필요 없다.  
HTML `<form>` 제출만으로 Spring Security가 인증을 처리하고, 성공/실패에 따라 자동으로 리다이렉트한다.

```html
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>로그인</title>
    <style>
        body { font-family: sans-serif; display: flex; justify-content: center;
               align-items: center; height: 100vh; margin: 0; background: #f0f2f5; }
        .login-box { background: white; padding: 40px; border-radius: 8px;
                     box-shadow: 0 2px 10px rgba(0,0,0,.1); width: 320px; }
        h2 { margin: 0 0 24px; text-align: center; color: #333; }
        label { display: block; margin-bottom: 4px; font-size: 14px; color: #555; }
        input { width: 100%; padding: 10px; margin-bottom: 16px; border: 1px solid #ddd;
                border-radius: 4px; box-sizing: border-box; font-size: 14px; }
        button { width: 100%; padding: 12px; background: #4a90e2; color: white;
                 border: none; border-radius: 4px; font-size: 16px; cursor: pointer; }
        button:hover { background: #357abd; }
        .error { color: red; font-size: 13px; margin-top: 8px; text-align: center; }
    </style>
</head>
<body>
<div class="login-box">
    <h2>채팅 로그인</h2>
    <!--
        action  → SecurityConfig의 loginProcessingUrl("/auth/login")과 일치해야 한다.
        method  → 반드시 POST
        name    → Spring Security 기본값: username / password (변경하지 않는다)
    -->
    <form action="/auth/login" method="post">
        <label>아이디</label>
        <input type="text" name="username" placeholder="alice" required />
        <label>비밀번호</label>
        <input type="password" name="password" placeholder="1234" required />
        <button type="submit">로그인</button>
    </form>
    <div class="error" id="errorMsg"></div>
</div>

<script>
    // 로그인 실패 시 URL에 ?error=true 가 붙어서 돌아온다
    if (new URLSearchParams(location.search).get('error')) {
        document.getElementById('errorMsg').textContent =
            '아이디 또는 비밀번호가 올바르지 않습니다.';
    }
</script>
</body>
</html>
```

> **이전 방식(fetch)과 비교**
>
> | 항목 | fetch 방식 (변경 전) | form 방식 (변경 후) |
> |---|---|---|
> | 로그인 처리 | `AuthController.login()` | Spring Security `formLogin()` |
> | 성공 처리 | JS로 localStorage 저장 → 이동 | 서버가 세션 생성 → 자동 리다이렉트 |
> | 실패 처리 | JS로 에러 메시지 표시 | `?error=true` 파라미터로 전달 |
> | localStorage | JWT 저장 | 사용 안 함 |

---

## room.html 수정 — 닉네임 입력 제거

unit05의 `room.html (그룹 채팅 + DM 통합)` 코드를 기반으로 아래 4곳을 수정한다.  
세션 인증을 기반으로 `GET /auth/token`을 호출해 사용자명과 WebSocket JWT를 얻는 방식으로 변경한다.

---

### ① HTML loginArea 영역 수정

**위치**: `<body>` 내 `<!-- 로그인 영역 -->` 주석 아래의 `<div id="loginArea">` 블록

unit05의 loginArea는 닉네임 `<input>`과 입장 버튼으로 구성되어 있다.

```html
<!-- 변경 전 (unit05 코드) -->
<!-- 로그인 영역 -->
<div id="loginArea">
    <input id="nicknameInput" placeholder="닉네임을 입력하세요" />
    <button onclick="enterRoom()">입장</button>
</div>
```

닉네임 입력란을 제거하고, 서버에서 받아온 사용자명을 표시하는 환영 메시지와 로그아웃 버튼으로 교체한다.

```html
<!-- 변경 후 -->
<!-- 로그인 영역 -->
<div id="loginArea">
    <p><strong id="welcomeUser"></strong> 님, 환영합니다.</p>
    <button id="enterBtn" onclick="enterRoom()">채팅방 입장</button>
    <button onclick="logout()">로그아웃</button>
</div>
```

> `welcomeUser` 요소는 아래 ②의 `DOMContentLoaded`에서 서버 응답의 `username`으로 채워진다.

---

### ② 전역 변수 선언 추가 및 DOMContentLoaded 핸들러 교체

**위치 1 — 전역 변수**: `<script th:inline="javascript">` 블록 상단의 전역 변수 선언부

unit05의 `<script>` 블록은 다음 전역 변수로 시작한다.

```javascript
// 변경 전 (unit05 코드) — script 블록 상단
const roomId = /*[[${room.roomId}]]*/ 'room1';
let stompClient = null;
let sender      = '';
```

여기에 `wsToken` 변수를 한 줄 추가한다.

```javascript
// 변경 후 — script 블록 상단
const roomId = /*[[${room.roomId}]]*/ 'room1';
let stompClient = null;
let sender      = '';
let wsToken     = null;   // ✅ 추가: WebSocket 연결에 사용할 JWT
```

---

**위치 2 — DOMContentLoaded**: `<script>` 블록 맨 아래의 `DOMContentLoaded` 핸들러

unit05의 `DOMContentLoaded` 핸들러는 입력 필드의 키보드 이벤트만 등록한다.

```javascript
// 변경 전 (unit05 코드) — script 블록 맨 아래
document.addEventListener('DOMContentLoaded', function () {
    document.getElementById('groupMsgInput').addEventListener('keydown',
        (e) => { if (e.key === 'Enter') sendGroupMessage(); });
    document.getElementById('dmMsgInput').addEventListener('keydown',
        (e) => { if (e.key === 'Enter') sendDm(); });
    document.getElementById('nicknameInput').addEventListener('keydown',
        (e) => { if (e.key === 'Enter') enterRoom(); });
});
```

이 핸들러 전체를 아래로 교체한다.  
`async function`으로 변경하고, `GET /auth/token` 호출로 세션 인증을 확인한 뒤 JWT와 사용자명을 수신한다.  
`sender`에 서버 응답값을 할당하고, `nicknameInput` 키 이벤트는 더 이상 필요하지 않으므로 제거한다.  
페이지 새로고침 시에는 `sessionStorage`를 확인해 자동으로 재연결한다.

```javascript
// 변경 후
document.addEventListener('DOMContentLoaded', async function () {
    // 세션 쿠키가 자동 전송 → 서버가 로그인 여부 확인
    const res = await fetch('/auth/token');

    if (!res.ok) {
        // 세션 만료 또는 미로그인 → 로그인 화면으로 이동
        location.href = '/login.html';
        return;
    }

    const { token, username } = await res.json();
    wsToken = token;      // 전역 변수에 JWT 저장
    sender  = username;   // unit05에서 선언된 전역 변수 sender에 할당

    document.getElementById('welcomeUser').textContent = username;

    // 페이지 새로고침인 경우: sessionStorage에 기록이 있으면 자동 재연결
    if (sessionStorage.getItem('entered_' + roomId)) {
        document.getElementById('loginArea').hidden = true;
        document.getElementById('mainArea').hidden  = false;
        connectStomp(wsToken, sender, false);  // ENTER 메시지 생략
    }

    // 기존 키 이벤트 유지 (nicknameInput 이벤트는 제거)
    document.getElementById('groupMsgInput').addEventListener('keydown',
        (e) => { if (e.key === 'Enter') sendGroupMessage(); });
    document.getElementById('dmMsgInput').addEventListener('keydown',
        (e) => { if (e.key === 'Enter') sendDm(); });
});
```

> `sender`는 unit05에서 `let sender = ''`로 선언된 전역 변수다.  
> 기존에는 `enterRoom()`에서 `nicknameInput` 값을 읽어 할당했으나, 이제는 `DOMContentLoaded`에서 서버 응답값으로 할당한다.

---

### ③ enterRoom() 함수 교체

**위치**: `<script>` 내 `function enterRoom() { ... }` 전체

unit05의 `enterRoom()`은 닉네임 읽기 → 유효성 검사 → UI 전환 → SockJS 연결 → 구독 → ENTER 전송을 모두 수행한다.

```javascript
// 변경 전 (unit05 코드)
function enterRoom() {
    sender = document.getElementById('nicknameInput').value.trim();
    if (!sender) { alert('닉네임을 입력하세요.'); return; }

    document.getElementById('loginArea').hidden = true;
    document.getElementById('mainArea').hidden  = false;

    // username을 쿼리 파라미터로 전달 → 서버에서 Principal로 등록
    const socket = new SockJS('/ws?username=' + encodeURIComponent(sender));
    stompClient  = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, function (frame) {

        // 1. 그룹 채팅 구독
        stompClient.subscribe('/topic/chat/' + roomId, function (msg) {
            renderGroupMessage(JSON.parse(msg.body));
        });

        // 2. DM 개인 채널 구독 (항상 열어둠)
        stompClient.subscribe('/user/queue/private', function (msg) {
            renderDmMessage(JSON.parse(msg.body));
        });

        // 3. 입장 메시지 전송
        stompClient.send('/app/chat/message', {}, JSON.stringify({
            type   : 'ENTER',
            roomId : roomId,
            sender : sender,
            content: ''
        }));

    }, function (error) {
        alert('연결 실패: ' + error);
    });
}
```

변경 사항은 다음과 같다.

- `nicknameInput` 값 읽기 및 유효성 검사 → 제거 (사용자명은 이미 `DOMContentLoaded`에서 `sender`에 할당됨)
- SockJS 생성·STOMP connect·구독·ENTER 전송 로직 → 제거 (unit06의 `connectStomp()` 함수가 담당)
- `loginArea` / `mainArea` UI 전환 → 유지 (버튼 클릭 시 화면 전환은 여기서 처리)
- `sessionStorage` 기록 추가 → 새로고침 후 자동 재연결 여부를 판단하기 위해 필요

```javascript
// 변경 후
function enterRoom() {
    document.getElementById('loginArea').hidden = true;
    document.getElementById('mainArea').hidden  = false;

    // 처음 입장 기록 — 새로고침 시 자동 재연결에 사용
    sessionStorage.setItem('entered_' + roomId, 'true');
    connectStomp(wsToken, sender, true);  // ENTER 메시지 전송
}
```

> **`isNewEntry` 구분 정리**
>
> | 상황 | 호출 경로 | `isNewEntry` |
> |---|---|---|
> | 처음 입장 (버튼 클릭) | `enterRoom()` | `true` → ENTER 메시지 전송 |
> | 페이지 새로고침 | `DOMContentLoaded` 자동 재연결 | `false` → ENTER 생략 |

---

### ④ 로그아웃 함수 추가

**위치**: `<script>` 내 `function escapeHtml()` 다음에 신규 추가 (unit05에는 없는 함수)

```javascript
// 신규 추가
function logout() {
    // sessionStorage 정리 후 서버 세션 무효화
    sessionStorage.clear();
    fetch('/auth/logout', { method: 'POST' })
        .finally(() => { location.href = '/login.html'; });
}
```

> `POST /auth/logout`은 `SecurityConfig`의 `logoutUrl("/auth/logout")`이 처리한다.  
> 서버 세션이 무효화되고 `JSESSIONID` 쿠키가 삭제된다.

---

## 실습 체크리스트

- [ ] 애플리케이션을 실행하고 H2 콘솔(`http://localhost:8080/h2-console`)에서 `users` 테이블에 3명의 테스트 사용자가 저장됐는지 확인한다.
- [ ] `login.html`에 직접 접근하면 로그인 폼이 표시되는지 확인한다.
- [ ] alice / 1234로 로그인하면 `/`(채팅 목록)으로 이동하는지 확인한다.
- [ ] 잘못된 비밀번호로 로그인했을 때 `/login.html?error=true`로 리다이렉트되고 에러 메시지가 표시되는지 확인한다.
- [ ] 로그인 후 개발자 도구 → Application → Cookies에서 `JSESSIONID` 쿠키가 생성됐는지 확인한다.
- [ ] 채팅방에 진입했을 때 `GET /auth/token` 요청이 전송되고 `{ token, username }` 응답이 오는지 개발자 도구 Network 탭에서 확인한다.
- [ ] `welcomeUser` 영역에 올바른 사용자명이 표시되는지 확인한다.
- [ ] 로그아웃 버튼 클릭 시 서버 세션이 무효화되고 `login.html`로 이동하는지 확인한다.
- [ ] 로그아웃 후 `/auth/token`을 직접 호출하면 `login.html`로 리다이렉트되는지 확인한다.

---

## unit06 진행 전 확인 사항

아래 항목이 모두 완료된 상태에서 unit06을 진행한다.

| 확인 항목 | 완료 여부 |
|---|---|
| `User` 엔티티, `UserRepository` 생성 | ☐ |
| `UserDetailsServiceImpl` 등록 | ☐ |
| `PasswordEncoder`, `AuthenticationManager` 빈 등록 | ☐ |
| `SecurityConfig` — `formLogin()`, `IF_REQUIRED` 세션, `logout()` 설정 완료 | ☐ |
| `DataInitializer`로 테스트 사용자 3명 확인 | ☐ |
| `JwtTokenProvider` 생성 | ☐ |
| `GET /auth/token` 엔드포인트 정상 동작 확인 | ☐ |
| `login.html` 폼 로그인 → 세션 생성 확인 | ☐ |
| `room.html` — 닉네임 입력 제거, `/auth/token` 호출로 교체 완료 | ☐ |

> unit06에서는 이 단원에서 만든 `JwtTokenProvider`를 그대로 사용하고,  
> `JwtAuthenticationFilter`와 `WebSocketAuthChannelInterceptor`를 추가해 WebSocket 인증을 완성한다.
