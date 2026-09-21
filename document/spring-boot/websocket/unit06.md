# 6단원. 사용자 인증과 보안

---

## 6-1. Spring Security + WebSocket 통합

### 학습 목표
- Spring Security를 WebSocket 환경에 적용하는 방법을 설명할 수 있다.
- HTTP 인증과 WebSocket 인증의 차이를 이해할 수 있다.
- `ChannelInterceptor`로 메시지 수신 시 인가를 처리할 수 있다.

---

### 의존성 확인

> ✅ **pre-unit06에서 이미 추가 완료**  
> `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `jjwt-*`, `h2`는 pre-unit06에서 이미 `build.gradle`에 추가했다.  
> 이 단원에서 새로 추가할 의존성은 없다.

---

### HTTP 인증과 WebSocket 인증의 차이

| 구분 | HTTP (REST) | WebSocket |
|---|---|---|
| 인증 시점 | 매 요청마다 | 최초 핸드셰이크 1회 |
| 인증 방식 | Authorization 헤더 | 핸드셰이크 요청의 헤더 또는 쿼리 파라미터 |
| 세션 유지 | Stateless (JWT) | 연결이 유지되는 동안 Principal 유지 |
| Spring 처리 | `SecurityFilterChain` | `HandshakeInterceptor` + `ChannelInterceptor` |

WebSocket은 HTTP 핸드셰이크로 연결을 수립한 뒤 프로토콜이 전환되기 때문에,  
**연결 수립 시점(핸드셰이크)**에 인증을 처리하고 이후 메시지는 `ChannelInterceptor`로 인가한다.

---

## 6-2. JWT 기반 WebSocket 인증

### 학습 목표
- JWT를 생성하고 검증하는 유틸리티 클래스를 구현할 수 있다.
- WebSocket 연결 시 JWT를 STOMP 헤더로 전달하는 방법을 이해할 수 있다.
- 서버에서 JWT를 검증하고 Principal을 등록하는 흐름을 구현할 수 있다.

---

### JWT 토큰 유틸리티

> ✅ **pre-unit06에서 이미 생성 완료**  
> `JwtTokenProvider.java`와 `application.properties`의 `jwt.secret` 설정은 pre-unit06에서 이미 작성했다.  
> 이 단원에서는 해당 클래스를 그대로 주입받아 사용한다.

> ⚠️ 운영 환경에서는 환경 변수나 Vault 등으로 시크릿 키를 관리하고, 소스 코드에 직접 작성하지 않는다.

---

### ChannelInterceptor로 메시지 인가 처리

HTTP 필터는 WebSocket 연결 수립 이후의 STOMP 메시지에는 적용되지 않는다.  
STOMP 메시지 채널에 대한 인가는 **`ChannelInterceptor`** 를 사용한다.

```
클라이언트 SEND 프레임
    ↓
[ChannelInterceptor.preSend()]   ← 메시지 처리 전 인가 검사
    ↓  (인가 실패 시 예외 발생)
[@MessageMapping 메서드 실행]
```

#### WebSocketAuthChannelInterceptor.java

```java
package com.springexample.webchat.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    public WebSocketAuthChannelInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // CONNECT 프레임에서만 토큰 검증 (연결 수립 시 1회)
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String token = accessor.getFirstNativeHeader("Authorization");

            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);

                if (jwtTokenProvider.validateToken(token)) {
                    Authentication auth = jwtTokenProvider.getAuthentication(token);
                    // Principal 등록 → @SendToUser, convertAndSendToUser 에서 사용
                    accessor.setUser(auth);
                } else {
                    throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
                }
            } else {
                throw new IllegalArgumentException("Authorization 헤더가 없습니다.");
            }
        }

        return message;
    }
}
```

#### WebSocketConfig.java (ChannelInterceptor 등록)

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor channelInterceptor;

    public WebSocketConfig(WebSocketAuthChannelInterceptor channelInterceptor) {
        this.channelInterceptor = channelInterceptor;
    }

    // ... configureMessageBroker, registerStompEndpoints 동일

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 인바운드 채널(클라이언트 → 서버)에 인터셉터 등록
        registration.interceptors(channelInterceptor);
    }
}
```

---

### JWT 인증 HTTP 필터

REST API 요청에 대한 JWT 검증은 기존 HTTP 필터에서 처리한다.

#### JwtAuthenticationFilter.java

```java
package com.springexample.webchat.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            Authentication auth = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    /** Authorization 헤더에서 Bearer 토큰 추출 */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
```

---

### SecurityConfig — JwtAuthenticationFilter 등록

pre-unit06에서 작성한 `SecurityConfig`에 `JwtAuthenticationFilter`를 주입받아 필터 체인에 추가한다.  
**세션 정책 및 `formLogin()`, `logout()` 설정은 변경하지 않는다.**

#### SecurityConfig.java (완성본)

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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // ✅ unit06에서 추가: JwtAuthenticationFilter 주입
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {          // pre-unit06과 동일
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager( // pre-unit06과 동일
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->               // pre-unit06과 동일 (IF_REQUIRED 유지)
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/css/**", "/js/**",
                                 "/images/**", "/h2-console/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form                     // pre-unit06과 동일
                .loginPage("/login.html")
                .loginProcessingUrl("/auth/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login.html?error=true")
                .permitAll()
            )
            .logout(logout -> logout                    // pre-unit06과 동일
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/login.html")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .headers(headers ->
                headers.frameOptions(frame -> frame.disable())
            )
            // ✅ unit06에서 추가: HTTP 요청의 JWT 검증 필터 등록
            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

> **핵심 포인트**  
> - `JwtAuthenticationFilter`는 HTTP REST 요청의 `Authorization` 헤더에서 JWT를 추출해 검증한다.  
> - WebSocket STOMP 연결의 JWT 검증은 `WebSocketAuthChannelInterceptor`가 담당한다 (아래 참고).  
> - `formLogin()`과 `IF_REQUIRED` 세션은 그대로 유지되므로 일반 웹 페이지 로그인 흐름에는 영향이 없다.

---

### WebSocket JWT 발급 API

> ✅ **pre-unit06에서 이미 완성**  
> `AuthController`의 `GET /auth/token` 엔드포인트는 pre-unit06에서 이미 구현했다.  
> 세션으로 인증된 사용자가 이 엔드포인트를 호출하면 WebSocket 연결에 사용할 JWT를 발급받는다.  
> 이 단원에서 `AuthController`에 추가할 코드는 없다.

```
GET /auth/token
  → 세션 쿠키 자동 전송 → Principal 확인
  → JwtTokenProvider.createToken(username)
  → { "token": "eyJ...", "username": "alice" }
```

---

### 클라이언트: JWT를 STOMP 헤더로 전달

> **적용 위치**: `room.html`의 `<script>` 블록.  
> pre-unit06에서 `DOMContentLoaded`가 `GET /auth/token`으로 `wsToken`과 `sender`를 이미 확보한다.  
> 이 단원에서는 **`connectStomp()` 함수 구현**만 추가하면 된다.

#### connectStomp() 함수 추가

```javascript
// ── connectStomp(): 세션에서 발급받은 JWT로 STOMP 연결 ──
// isNewEntry: true  → 최초 입장 (ENTER 메시지 전송)
//             false → 페이지 새로고침 재연결 (ENTER 메시지 생략)
function connectStomp(token, username, isNewEntry = false) {
    const socket = new SockJS('/ws');
    stompClient  = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect(
        { 'Authorization': 'Bearer ' + token },   // CONNECT 프레임 헤더에 JWT 포함
        function (frame) {
            console.log('✅ 인증 연결 성공:', frame);

            stompClient.subscribe('/topic/chat/' + roomId, function (msg) {
                renderGroupMessage(JSON.parse(msg.body));
            });

            stompClient.subscribe('/user/queue/private', function (msg) {
                renderDmMessage(JSON.parse(msg.body));
            });

            // 최초 입장일 때만 ENTER 메시지 전송
            if (isNewEntry) {
                stompClient.send('/app/chat/message', {}, JSON.stringify({
                    type   : 'ENTER',
                    roomId : roomId,
                    sender : username,
                    content: ''
                }));
            }

            // 입장 UI 숨김, 채팅 UI 표시
            document.getElementById('loginArea').hidden = true;
            document.getElementById('mainArea').hidden  = false;
        },
        function (error) {
            console.error('❌ 연결 실패 (인증 오류):', error);
        }
    );
}
```

> **전체 흐름 정리** (pre-unit06 코드와 이 단원 코드의 연결 관계)
>
> ```
> [DOMContentLoaded]  ← pre-unit06에서 작성
>     │  GET /auth/token → { token, username }
>     │  wsToken = token;  sender = username;
>     │  sessionStorage에 entered_{roomId} 있으면 → connectStomp(wsToken, sender, false)
>     ▼
> [enterRoom()]       ← pre-unit06에서 작성
>     │  sessionStorage.setItem('entered_' + roomId, 'true')
>     │  connectStomp(wsToken, sender, true)
>     ▼
> [connectStomp()]    ← 이 단원에서 추가
>     │  SockJS + STOMP.over()
>     │  stompClient.connect({ Authorization: 'Bearer ' + token }, ...)
>     ▼
> [WebSocketAuthChannelInterceptor]  ← 이 단원 서버 코드
>     │  CONNECT 프레임 헤더에서 토큰 추출 → 검증 → accessor.setUser(auth)
>     ▼
> [STOMP 구독 · 메시지 송수신]
> ```

---

### 전체 인증 흐름 (세션 + JWT 혼합)

```
━━━━━━━━━━━━━ ① 세션 로그인 (pre-unit06) ━━━━━━━━━━━━━

[login.html]  POST /auth/login (HTML 폼)
    ▼
[Spring Security formLogin]
    │  UserDetailsServiceImpl → DB 인증 → 세션(JSESSIONID) 생성
    ▼
[/ (채팅 목록)] → [room.html 로드]

━━━━━━━━━━━━━ ② WebSocket JWT 발급 (pre-unit06) ━━━━━━━━━━━━━

[room.html DOMContentLoaded]
    │  GET /auth/token  (세션 쿠키 자동 전송)
    ▼
[AuthController.getToken()]
    │  Principal(세션) → JwtTokenProvider.createToken(username)
    ▼
[room.html]  wsToken, sender 확보

━━━━━━━━━━━━━ ③ STOMP 연결 (이 단원) ━━━━━━━━━━━━━

[connectStomp(wsToken, sender, isNewEntry)]
    │  new SockJS('/ws')
    │  CONNECT 헤더: Authorization: Bearer <wsToken>
    ▼
[WebSocketAuthChannelInterceptor.preSend()]
    │  토큰 검증 (JwtTokenProvider.validateToken)
    │  Authentication 생성 → accessor.setUser(auth)
    ▼
[STOMP 연결 수립 완료]
    │  Principal.getName() = "alice"
    ▼
[@MessageMapping] / [convertAndSendToUser]
    │  principal.getName()으로 사용자 식별
```

---

## 6-3. CSRF 방어

### 학습 목표
- WebSocket 환경에서 CSRF 위협이 발생하는 조건을 설명할 수 있다.
- Spring Security의 WebSocket CSRF 설정 방법을 적용할 수 있다.

---

### WebSocket과 CSRF

일반 HTTP 요청에서는 CSRF 토큰으로 위조 요청을 차단한다.  
WebSocket 핸드셰이크는 HTTP GET 요청으로 이루어지므로 이론적으로 CSRF 공격에 노출될 수 있다.  
그러나 **JWT 기반 인증을 사용하는 경우** CSRF 위협이 크게 감소한다.

| 인증 방식 | CSRF 위험도 | 이유 |
|---|---|---|
| 세션 쿠키 기반 | 높음 | 브라우저가 자동으로 쿠키를 전송 |
| JWT (Authorization 헤더) | 낮음 | 헤더는 자바스크립트로만 설정 가능 |

---

### Spring Security WebSocket CSRF 설정

#### 방법 1: CSRF 전체 비활성화 (JWT 사용 시 일반적)

```java
http.csrf(csrf -> csrf.disable());
```

JWT를 Authorization 헤더로 전달하는 Stateless 방식에서는 CSRF 토큰이 불필요하므로  
`disable()`이 일반적으로 사용된다.

---

#### 방법 2: WebSocket 경로만 CSRF 제외

세션 기반 인증을 유지하면서 WebSocket 핸드셰이크 경로만 CSRF 검사를 우회한다.

```java
http.csrf(csrf -> csrf
    .ignoringRequestMatchers("/ws/**")
);
```

---

#### 방법 3: SockJS CSRF 토큰 연동

SockJS는 첫 요청 시 `/ws/info` 엔드포인트를 호출한다.  
Spring Security의 CSRF 토큰을 SockJS 헤더에 포함하면 CSRF 보호를 유지할 수 있다.

```javascript
// Thymeleaf + Spring Security CSRF 토큰 연동 예시
const csrfToken  = document.querySelector('meta[name="_csrf"]').content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

stompClient.connect(
    {
        [csrfHeader]  : csrfToken,
        'Authorization': 'Bearer ' + token
    },
    connectCallback
);
```

```html
<!-- Thymeleaf layout에 CSRF 메타 태그 추가 -->
<meta name="_csrf"        th:content="${_csrf.token}" />
<meta name="_csrf_header" th:content="${_csrf.headerName}" />
```

> 💡 JWT + Stateless 방식을 사용하는 이 과정에서는 **방법 1(CSRF disable)** 을 적용한다.

---

### 실습 체크리스트

- [ ] `login.html`에서 alice / 1234로 로그인하고 채팅방 페이지(`room.html`)에 진입한다.
- [ ] 개발자 도구 Network 탭에서 `GET /auth/token` 요청이 전송되고 `{ token, username }` 응답이 오는지 확인한다.
- [ ] `connectStomp()` 함수가 `Authorization: Bearer <token>` 헤더와 함께 STOMP CONNECT 프레임을 전송하는지 확인한다.
- [ ] 서버 콘솔에서 `WebSocketAuthChannelInterceptor`가 토큰을 수신하고 Principal을 등록하는지 확인한다.
- [ ] 토큰 없이 연결을 시도했을 때 `IllegalArgumentException`이 발생하는지 확인한다.
- [ ] 만료된 토큰으로 연결했을 때 연결이 거부되는지 확인한다.
- [ ] `@MessageMapping` 메서드에서 `principal.getName()`이 올바른 username을 반환하는지 확인한다.

---

## 단원 정리

| 핵심 개념 | 요약 |
|---|---|
| HTTP vs WebSocket 인증 | HTTP는 매 요청마다, WebSocket은 핸드셰이크 1회 인증 |
| `ChannelInterceptor` | STOMP 메시지 채널의 인가 처리 (preSend에서 토큰 검증) |
| `StompHeaderAccessor` | STOMP 프레임 헤더에 접근하는 유틸리티 |
| `JwtTokenProvider` | JWT 생성, 검증, Authentication 추출 담당 |
| `JwtAuthenticationFilter` | HTTP 요청에 대한 JWT 검증 필터 |
| `accessor.setUser(auth)` | 검증된 Authentication을 Principal로 STOMP 세션에 등록 |
| CSRF | JWT + Stateless 환경에서는 `csrf.disable()` 적용 |

---

## 확인 문제

1. Spring Security의 HTTP 필터가 WebSocket 연결 이후의 STOMP 메시지에 적용되지 않는 이유를 설명하시오.
2. `ChannelInterceptor.preSend()`에서 `StompCommand.CONNECT`일 때만 토큰을 검증하는 이유는?
3. `accessor.setUser(auth)`를 호출하는 목적과, 이후 `@MessageMapping` 메서드에서 어떻게 활용되는지 설명하시오.
4. 세션 쿠키 기반 인증보다 JWT 기반 인증에서 CSRF 위험도가 낮은 이유를 설명하시오.
5. 운영 환경에서 `jwt.secret`을 `application.properties`에 직접 작성하면 안 되는 이유와 대안을 서술하시오.

