# 2단원. Spring Boot WebSocket 설정

---

## 2-1. 의존성 추가 및 프로젝트 구성

### 학습 목표
- Spring Boot 프로젝트에 WebSocket 의존성을 추가할 수 있다.
- 기존 Spring Boot MVC 프로젝트와 WebSocket 설정이 어떻게 통합되는지 설명할 수 있다.

---

### 의존성 추가

**build.gradle**

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-websocket'

    // Lombok (선택)
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Thymeleaf (실습 UI용, 선택)
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
}
```

**pom.xml (Maven 사용 시)**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

> 💡 `spring-boot-starter-websocket`에는 `spring-websocket`과 `spring-messaging`이 포함되어 있다.  
> 별도로 버전을 명시하지 않아도 Spring Boot BOM이 호환 버전을 자동 관리한다.

---

### 프로젝트 구조

WebSocket 기능을 추가할 때 권장하는 패키지 구조:

```
src/main/java/com/springexample/webchat/
├── config/
│   └── WebSocketConfig.java       ← WebSocket 설정 클래스
├── handler/
│   └── EchoWebSocketHandler.java  ← WebSocket 핸들러
├── controller/
│   └── ChatController.java        ← REST 또는 STOMP 컨트롤러
├── model/
│   └── ChatMessage.java           ← 메시지 도메인 모델
└── ChatApplication.java
```

---

### 기존 Spring Boot MVC와의 통합 포인트

| 구분 | Spring MVC (기존) | WebSocket (추가) |
|---|---|---|
| 진입점 | `@Controller` / `@RestController` | `WebSocketHandler` / `@MessageMapping` |
| 프로토콜 | HTTP | WebSocket (ws://) |
| 데이터 흐름 | 요청-응답 (1회성) | 지속 연결 (양방향) |
| 설정 클래스 | `WebMvcConfigurer` | `WebSocketConfigurer` |
| 보안 | Spring Security (HTTP) | Spring Security (WebSocket) |

> 기존 MVC 컨트롤러와 WebSocket은 **같은 애플리케이션 내에 공존**할 수 있다.  
> REST API로 채팅방 목록을 조회하고, 실시간 메시지는 WebSocket으로 처리하는 방식이 일반적이다.

---

## 2-2. 순수 WebSocket 핸들러 구현

### 학습 목표
- `WebSocketHandler`를 구현해 메시지를 처리할 수 있다.
- `WebSocketConfigurer`로 엔드포인트를 등록할 수 있다.
- 에코(Echo) 서버를 직접 구현하고 동작을 확인할 수 있다.

---

### WebSocketHandler 구현

Spring WebSocket은 핸들러를 구현하는 두 가지 방법을 제공한다.

| 인터페이스 / 추상 클래스 | 특징 |
|---|---|
| `WebSocketHandler` | 인터페이스 직접 구현, 모든 메서드 오버라이드 필요 |
| `TextWebSocketHandler` | 텍스트 메시지 처리에 특화된 추상 클래스 (권장) |
| `BinaryWebSocketHandler` | 바이너리 메시지 처리에 특화 |

#### EchoWebSocketHandler.java

```java
package com.springexample.webchat.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class EchoWebSocketHandler extends TextWebSocketHandler {

    /**
     * 클라이언트로부터 텍스트 메시지를 수신했을 때 호출
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
            throws Exception {

        String payload = message.getPayload();
        System.out.println("수신 메시지: " + payload + " | 세션 ID: " + session.getId());

        // 수신한 메시지를 그대로 돌려보냄 (Echo)
        session.sendMessage(new TextMessage("에코: " + payload));
    }
}
```

---

### WebSocketConfigurer로 엔드포인트 등록

#### WebSocketConfig.java

```java
package com.springexample.webchat.config;

import com.springexample.webchat.handler.EchoWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket                          // WebSocket 기능 활성화
public class WebSocketConfig implements WebSocketConfigurer {

    private final EchoWebSocketHandler echoWebSocketHandler;

    public WebSocketConfig(EchoWebSocketHandler echoWebSocketHandler) {
        this.echoWebSocketHandler = echoWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
            .addHandler(echoWebSocketHandler, "/ws/echo")  // 엔드포인트 등록
            .setAllowedOrigins("*");                        // CORS 허용 (개발 환경)
    }
}
```

**주요 설정 옵션**

| 메서드 | 설명 |
|---|---|
| `addHandler(handler, path)` | 핸들러와 엔드포인트 경로 등록 |
| `setAllowedOrigins("*")` | 모든 출처 허용 (개발용) |
| `setAllowedOrigins("https://example.com")` | 특정 출처만 허용 (운영 권장) |
| `withSockJS()` | SockJS 폴백 활성화 (3단원에서 상세히 다룸) |

---

### 에코 서버 동작 확인

1단원에서 작성한 `echo-test.html`을 그대로 사용하여 테스트한다.

```
브라우저 ──── "안녕하세요" ────▶ /ws/echo ──▶ EchoWebSocketHandler
브라우저 ◀─── "에코: 안녕하세요" ────────────── EchoWebSocketHandler
```

**브라우저 개발자 도구 확인 방법**
1. F12 → Network 탭 → WS 필터 선택
2. `/ws/echo` 연결 클릭
3. Messages 탭에서 송수신 프레임 확인

---

## 2-3. 연결 생명주기 관리

### 학습 목표
- WebSocket 연결 수립, 메시지 수신, 연결 종료 이벤트를 각각 처리할 수 있다.
- `WebSocketSession`을 컬렉션으로 관리하여 다수의 클라이언트에게 메시지를 브로드캐스트할 수 있다.

---

### WebSocket 연결 생명주기

```
클라이언트 연결 요청
        ↓
afterConnectionEstablished()   ← 연결 수립 완료
        ↓
handleTextMessage()            ← 메시지 수신 (반복)
        ↓
afterConnectionClosed()        ← 연결 종료
```

---

### 생명주기 이벤트 전체 구현

#### ChatWebSocketHandler.java

```java
package com.springexample.webchat.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // 연결된 세션을 스레드 안전하게 관리
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    /**
     * 클라이언트 연결 수립 시 호출
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("✅ 연결: " + session.getId()
                + " | 현재 접속자: " + sessions.size());

        // 입장 알림 브로드캐스트
        broadcast("[알림] 새로운 사용자가 입장했습니다. (현재 " + sessions.size() + "명)");
    }

    /**
     * 텍스트 메시지 수신 시 호출
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
            throws Exception {
        String payload = message.getPayload();
        System.out.println("📩 메시지: " + payload + " | 세션: " + session.getId());

        // 모든 세션에 브로드캐스트
        broadcast(payload);
    }

    /**
     * 연결 종료 시 호출
     * @param status 종료 상태 코드 및 이유
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
            throws Exception {
        sessions.remove(session);
        System.out.println("❌ 종료: " + session.getId()
                + " | 코드: " + status.getCode()
                + " | 남은 접속자: " + sessions.size());
    }

    /**
     * 전송/수신 중 오류 발생 시 호출
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception)
            throws Exception {
        System.err.println("⚠️ 오류 발생 - 세션: " + session.getId()
                + " | " + exception.getMessage());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
        sessions.remove(session);
    }

    /**
     * 연결된 모든 세션에 메시지 전송 (브로드캐스트)
     */
    private void broadcast(String message) {
        sessions.forEach(s -> {
            if (s.isOpen()) {
                try {
                    s.sendMessage(new TextMessage(message));
                } catch (Exception e) {
                    System.err.println("전송 실패 - 세션: " + s.getId());
                }
            }
        });
    }
}
```

---

### WebSocketSession 주요 메서드

| 메서드 | 설명 |
|---|---|
| `session.getId()` | 세션 고유 ID 반환 |
| `session.isOpen()` | 연결 상태 확인 (true: 열림) |
| `session.sendMessage(message)` | 해당 세션에 메시지 전송 |
| `session.close()` | 연결 종료 |
| `session.getAttributes()` | 세션에 저장된 속성 맵 반환 |
| `session.getPrincipal()` | 인증된 사용자 정보 반환 (인증 설정 시) |

---

### CloseStatus 주요 코드

| 코드 | 상수 | 의미 |
|---|---|---|
| 1000 | `NORMAL` | 정상 종료 |
| 1001 | `GOING_AWAY` | 브라우저 탭 닫힘 / 페이지 이동 |
| 1002 | `PROTOCOL_ERROR` | 프로토콜 오류 |
| 1011 | `SERVER_ERROR` | 서버 내부 오류 |

---

### 설정 파일 업데이트 (엔드포인트 추가)

```java
@Override
public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    // 에코 핸들러
    registry.addHandler(echoWebSocketHandler, "/ws/echo")
            .setAllowedOrigins("*");

    // 채팅 핸들러 추가
    registry.addHandler(chatWebSocketHandler, "/ws/chat")
            .setAllowedOrigins("*");
}
```

---

### 실습: 간단한 단체 채팅 테스트

아래 HTML로 두 개의 브라우저 탭을 열어, 한 탭에서 보낸 메시지가 다른 탭에도 실시간으로 표시되는지 확인한다.

```html
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>단체 채팅 테스트</title>
    <style>
        body { font-family: sans-serif; max-width: 600px; margin: 40px auto; }
        #log { border: 1px solid #ccc; padding: 10px; height: 250px;
               overflow-y: scroll; background: #f9f9f9; margin-bottom: 10px; }
        input { width: 70%; padding: 8px; }
        button { padding: 8px 14px; margin-left: 6px; cursor: pointer; }
    </style>
</head>
<body>
    <h2>단체 채팅 (순수 WebSocket)</h2>
    <div id="log"></div>
    <input id="name" placeholder="닉네임" style="width:20%" />
    <input id="msg"  placeholder="메시지" style="width:48%" />
    <button onclick="send()">전송</button>

    <script>
        const log = document.getElementById("log");
        const socket = new WebSocket("ws://localhost:8080/ws/chat");

        socket.onopen    = () => addLog("✅ 연결됨", "#999");
        socket.onmessage = (e) => addLog(e.data, "#0070f3");
        socket.onclose   = () => addLog("❌ 연결 종료", "#999");

        function addLog(text, color) {
            const p = document.createElement("p");
            p.textContent = text;
            p.style.cssText = `margin:4px 0; color:${color}`;
            log.appendChild(p);
            log.scrollTop = log.scrollHeight;
        }

        function send() {
            const name = document.getElementById("name").value || "익명";
            const msg  = document.getElementById("msg").value.trim();
            if (!msg || socket.readyState !== WebSocket.OPEN) return;
            socket.send(`[${name}] ${msg}`);
            document.getElementById("msg").value = "";
        }

        document.getElementById("msg").addEventListener("keydown",
            (e) => { if (e.key === "Enter") send(); });
    </script>
</body>
</html>
```

---

### 실습 체크리스트

- [ ] `spring-boot-starter-websocket` 의존성을 추가하고 빌드가 성공하는지 확인한다.
- [ ] `EchoWebSocketHandler`를 구현하고 에코 동작을 테스트한다.
- [ ] `ChatWebSocketHandler`의 생명주기 메서드가 콘솔에 올바르게 출력되는지 확인한다.
- [ ] 두 개의 탭을 열어 한 탭의 메시지가 다른 탭에 전달되는지 확인한다.
- [ ] 탭을 닫을 때 `afterConnectionClosed()`가 호출되고 접속자 수가 줄어드는지 확인한다.

---

## 단원 정리

| 핵심 개념 | 요약 |
|---|---|
| 의존성 | `spring-boot-starter-websocket` 추가 |
| 핸들러 구현 | `TextWebSocketHandler` 상속 권장 |
| 엔드포인트 등록 | `@EnableWebSocket` + `WebSocketConfigurer` |
| 생명주기 | `afterConnectionEstablished` → `handleTextMessage` → `afterConnectionClosed` |
| 세션 관리 | `ConcurrentHashMap`으로 스레드 안전하게 세션 목록 유지 |
| 브로드캐스트 | 세션 목록 순회 후 `session.sendMessage()` 호출 |

> ⚠️ **순수 WebSocket의 한계**  
> 직접 세션을 관리하고 라우팅 로직을 코드로 작성해야 해 복잡도가 높아진다.  
> 채팅방별 메시지 분리, 구독 관리 등은 **3단원의 STOMP**를 사용하면 훨씬 간결하게 해결된다.

---

## 확인 문제

1. `spring-boot-starter-websocket`에 포함된 두 가지 라이브러리는 무엇인가?
2. `@EnableWebSocket` 어노테이션의 역할을 설명하시오.
3. `TextWebSocketHandler`와 `WebSocketHandler` 인터페이스의 차이점은?
4. `afterConnectionClosed()`의 두 번째 파라미터 `CloseStatus`로 알 수 있는 정보를 서술하시오.
5. 다수의 클라이언트 세션을 관리할 때 `HashSet` 대신 `ConcurrentHashMap.newKeySet()`을 사용하는 이유는?

