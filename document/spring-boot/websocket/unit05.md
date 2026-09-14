# 5단원. 1:1 귓속말(DM) 채팅 구현

---

## 5-1. 개인 구독 채널 설계

### 학습 목표
- STOMP의 `/topic`과 `/queue`의 차이를 설명할 수 있다.
- 사용자별 개인 채널 구조를 설계할 수 있다.
- 하나의 STOMP 연결로 그룹 채팅과 DM을 동시에 처리하는 구조를 이해할 수 있다.

---

### /topic vs /queue

4단원에서 사용한 `/topic`은 **모든 구독자에게 동일한 메시지**를 전달하는 브로드캐스트 채널이다.  
1:1 DM처럼 **특정 사용자에게만** 메시지를 보내려면 `/queue`를 사용한다.

| 구분 | `/topic` | `/queue` |
|---|---|---|
| 전달 대상 | 해당 토픽의 모든 구독자 | 특정 사용자 개인 |
| 주요 용도 | 단체 채팅, 공지 | 1:1 DM, 개인 알림 |
| 구독 경로 예시 | `/topic/chat/room1` | `/user/queue/private` |
| Spring 지원 | `enableSimpleBroker` | `enableSimpleBroker` + `@SendToUser` |

---

### 개인 채널 동작 구조

Spring은 `/user` prefix를 사용해 **사용자별로 격리된 채널**을 자동으로 생성한다.

```
클라이언트 A (username: alice)
    구독: /user/queue/private
    → 실제 내부 경로: /queue/private-user-alice

클라이언트 B (username: bob)
    구독: /user/queue/private
    → 실제 내부 경로: /queue/private-user-bob
```

- 클라이언트는 모두 `/user/queue/private`을 구독하지만
- 서버가 username을 기준으로 **각자의 개인 채널**에 메시지를 전달한다.
- alice에게 보낸 메시지는 bob에게 도달하지 않는다.

---

### 통합 채팅 구조

이 단원에서는 그룹 채팅과 DM을 **하나의 `room.html` 페이지**에 통합한다.  
하나의 STOMP 연결로 두 채널을 동시에 구독하는 방식이다.

```
room.html (하나의 STOMP 연결)
    │
    ├─ 구독: /topic/chat/{roomId}    → 그룹 메시지 수신
    └─ 구독: /user/queue/private     → DM 메시지 수신

    │
    ├─ 전송: /app/chat/message       → 그룹 메시지 발신
    └─ 전송: /app/dm                 → DM 발신
```

> 실제 채팅 서비스도 이 구조를 사용한다.  
> 페이지를 분리하면 STOMP 연결을 두 번 맺어야 하고, 세션 관리가 복잡해진다.

---

### /user prefix 설정 추가

`WebSocketConfig.java`에 `/user` prefix를 명시적으로 등록한다.

```java
@Override
public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic", "/queue");
    registry.setApplicationDestinationPrefixes("/app");

    // 사용자별 개인 채널을 위한 prefix 설정 (기본값: /user)
    registry.setUserDestinationPrefix("/user");
}
```

> 💡 `/user`는 Spring WebSocket의 기본값이므로 명시하지 않아도 동작하지만,  
> 명시적으로 작성하면 팀원이 설정 의도를 바로 파악할 수 있어 권장한다.

---

### DM 메시지 도메인 모델

4단원의 `ChatMessage`에 DM 수신자 필드를 추가한다.

```java
package com.springexample.webchat.domain;

public class ChatMessage {

    public enum MessageType {
        ENTER, TALK, LEAVE,
        DM          // 1:1 귓속말 타입 추가
    }

    private MessageType type;
    private String roomId;      // 단체 채팅방 ID (DM에서는 null)
    private String sender;      // 발신자
    private String receiver;    // 수신자 (DM 전용)
    private String content;

    // --- Getter / Setter ---
    public MessageType getType()              { return type; }
    public void setType(MessageType type)     { this.type = type; }
    public String getRoomId()                 { return roomId; }
    public void setRoomId(String roomId)      { this.roomId = roomId; }
    public String getSender()                 { return sender; }
    public void setSender(String sender)      { this.sender = sender; }
    public String getReceiver()               { return receiver; }
    public void setReceiver(String receiver)  { this.receiver = receiver; }
    public String getContent()                { return content; }
    public void setContent(String content)    { this.content = content; }
}
```

---

## 5-2. 서버 측 구현

### 학습 목표
- `@SendToUser`로 특정 사용자에게만 메시지를 전달할 수 있다.
- `SimpMessagingTemplate.convertAndSendToUser()`로 서버에서 능동적으로 DM을 전송할 수 있다.
- `Principal`을 통해 인증된 사용자를 식별하는 방법을 이해할 수 있다.

---

### Principal이란?

Spring Security에서 **현재 인증된 사용자**를 나타내는 객체다.  
`@SendToUser`와 `convertAndSendToUser()`는 내부적으로 `Principal.getName()`을 사용해  
메시지를 보낼 사용자를 식별한다.

```
클라이언트 연결 시 Principal 설정
    ↓
convertAndSendToUser("alice", ...) → Principal.getName() = "alice" 인 세션 탐색
    ↓
해당 세션에만 MESSAGE 프레임 전달
```

이 단원에서는 Spring Security 없이 **WebSocket 핸드셰이크 시 닉네임을 Principal로 등록**하는  
간단한 방식을 사용한다. (인증 연동은 6단원에서 다룬다.)

---

### 핸드셰이크 인터셉터로 Principal 등록

#### UsernameHandshakeInterceptor.java

```java
package com.springexample.webchat.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class UsernameHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        // 쿼리 파라미터에서 username 추출
        // 예) /ws?username=alice
        String query = request.getURI().getQuery();
        if (query != null && query.startsWith("username=")) {
            String username = query.substring("username=".length());
            attributes.put("username", username);
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) { }
}
```

#### DefaultHandshakePrincipal.java (Principal 구현체)

```java
package com.springexample.webchat.config;

import java.security.Principal;

public class DefaultHandshakePrincipal implements Principal {

    private final String name;

    public DefaultHandshakePrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
```

---

### CustomHandshakeHandler — Principal 주입

#### CustomHandshakeHandler.java

```java
package com.springexample.webchat.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {

        String username = (String) attributes.get("username");
        if (username == null || username.isBlank()) {
            username = "guest-" + System.currentTimeMillis();
        }
        return new DefaultHandshakePrincipal(username);
    }
}
```

---

### WebSocketConfig.java (최종)

```java
package com.springexample.webchat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
            .addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .addInterceptors(new UsernameHandshakeInterceptor()) // username 추출
            .setHandshakeHandler(new CustomHandshakeHandler())   // Principal 등록
            .withSockJS();
    }
}
```

---

### DM 메시지 컨트롤러

#### DirectMessageController.java

```java
package com.springexample.webchat.controller;

import com.springexample.webchat.domain.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class DirectMessageController {

    private final SimpMessagingTemplate messagingTemplate;

    public DirectMessageController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 클라이언트가 /app/dm 으로 SEND하면 호출
     * receiver 에게만 메시지 전달
     */
    @MessageMapping("/dm")
    public void directMessage(ChatMessage message, Principal principal) {

        // 발신자는 Principal에서 가져옴 (클라이언트 위변조 방지)
        message.setSender(principal.getName());
        message.setType(ChatMessage.MessageType.DM);

        // 수신자의 /user/queue/private 채널에 전송
        messagingTemplate.convertAndSendToUser(
            message.getReceiver(),
            "/queue/private",
            message
        );

        // 발신자 자신의 화면에도 전송 (보낸 메시지 확인용)
        messagingTemplate.convertAndSendToUser(
            principal.getName(),
            "/queue/private",
            message
        );
    }
}
```

**`convertAndSendToUser()` 동작 원리**

```
convertAndSendToUser("alice", "/queue/private", message)
    ↓
실제 전송 경로: /user/alice/queue/private
    ↓
alice 가 구독 중인 /user/queue/private 세션에 전달
```

---

## 5-3. 클라이언트 측 구현 — room.html 통합

### 학습 목표
- 그룹 채팅과 DM을 하나의 페이지에서 동시에 처리할 수 있다.
- 메시지 타입(TALK / DM)에 따라 렌더링을 분기할 수 있다.
- STOMP 연결 하나로 여러 채널을 구독하는 방식을 구현할 수 있다.

---

### 프로젝트 구조 추가 사항

```
src/main/resources/templates/chat/
    ├── rooms.html      ← 채팅방 목록 (4단원)
    └── room.html       ← 채팅방 + DM 통합 (이 단원에서 확장)
```

별도의 `dm.html`은 만들지 않는다.  
`room.html` 하나에서 그룹 채팅과 DM을 모두 처리한다.

> **참고 — 서버 측 뷰 라우팅은 4단원 `ChatRoomController`를 그대로 사용한다.**  
> `GET /chat/room/{roomId}` 핸들러가 `room` 객체(`roomId`, `roomName`)를  
> `model.addAttribute("room", room)`으로 이미 전달하므로 추가 구현이 필요 없다.  
> Thymeleaf에서 채팅방 이름은 반드시 **`${room.roomName}`** 으로 참조한다  
> (`${room.name}` 은 오류 발생 — `ChatRoom`의 실제 필드명은 `roomName`이다).

---

### room.html (그룹 채팅 + DM 통합)

```html
<!DOCTYPE html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title th:text="'채팅방: ' + ${room.roomName}">채팅방</title>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
    <style>
        body { font-family: sans-serif; max-width: 800px; margin: 40px auto; padding: 0 16px; }
        h2   { margin-bottom: 4px; }
        .sub { color: #888; font-size: 13px; margin-bottom: 16px; }

        /* 채팅 영역 */
        #chatArea {
            border: 1px solid #ccc; height: 380px; overflow-y: scroll;
            padding: 12px; background: #fafafa; margin-bottom: 10px;
        }

        /* 공통 메시지 */
        .msg { margin: 6px 0; }
        .msg.enter, .msg.leave { color: #999; font-style: italic; font-size: 13px; }

        /* 그룹 채팅 말풍선 */
        .msg.talk { display: flex; flex-direction: column; align-items: flex-start; }
        .msg.talk .sender { font-size: 11px; color: #666; margin-bottom: 2px; }
        .msg.talk .bubble { background: #e9e9e9; padding: 7px 11px;
                            border-radius: 10px; max-width: 65%; word-break: break-all; }

        /* DM 말풍선 */
        .msg.dm       { display: flex; flex-direction: column; }
        .msg.dm.mine  { align-items: flex-end; }
        .msg.dm.other { align-items: flex-start; }
        .msg.dm .meta { font-size: 11px; color: #888; margin-bottom: 2px; }
        .msg.dm.mine  .bubble { background: #f0c040; color: #333; }
        .msg.dm.other .bubble { background: #dce8ff; color: #333; }
        .msg.dm .bubble { padding: 7px 11px; border-radius: 10px;
                          max-width: 65%; word-break: break-all; }

        /* 입력 영역 */
        .input-section { margin-bottom: 10px; }
        .input-section label { display: block; font-size: 12px;
                               color: #555; margin-bottom: 4px; }
        .input-row { display: flex; gap: 8px; }
        .input-row input  { flex: 1; padding: 8px; border: 1px solid #ccc; border-radius: 4px; }
        .input-row button { padding: 8px 14px; cursor: pointer;
                            background: #0070f3; color: #fff;
                            border: none; border-radius: 4px; }

        /* 로그인 영역 */
        #loginArea { margin-bottom: 16px; }
        #loginArea input { padding: 8px; border: 1px solid #ccc;
                           border-radius: 4px; margin-right: 8px; }
        #loginArea button { padding: 8px 14px; cursor: pointer; }

        /* 구분선 */
        .divider { border: none; border-top: 1px solid #e0e0e0; margin: 10px 0; }
    </style>
</head>
<body>
    <h2 th:text="${room.roomName}">채팅방 이름</h2>
    <p class="sub">채팅방 ID: <span th:text="${room.roomId}"></span></p>

    <!-- 로그인 영역 -->
    <div id="loginArea">
        <input id="nicknameInput" placeholder="닉네임을 입력하세요" />
        <button onclick="enterRoom()">입장</button>
    </div>

    <!-- 채팅 영역 (입장 후 표시) -->
    <div id="mainArea" hidden>

        <!-- 메시지 표시 -->
        <div id="chatArea"></div>

        <hr class="divider">

        <!-- 그룹 채팅 입력 -->
        <div class="input-section">
            <label>그룹 채팅</label>
            <div class="input-row">
                <input id="groupMsgInput" placeholder="채팅방 전체에 메시지 전송" />
                <button onclick="sendGroupMessage()">전송</button>
            </div>
        </div>

        <!-- DM 입력 -->
        <div class="input-section">
            <label>귓속말 (DM)</label>
            <div class="input-row">
                <input id="dmReceiver" placeholder="받는 사람 닉네임"
                       style="max-width: 160px; flex: none;" />
                <input id="dmMsgInput" placeholder="귓속말 메시지" />
                <button onclick="sendDm()">전송</button>
            </div>
        </div>
    </div>

    <script th:inline="javascript">
        const roomId = /*[[${room.roomId}]]*/ 'room1';
        let stompClient = null;
        let sender      = '';

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

        /* 그룹 메시지 전송 */
        function sendGroupMessage() {
            const content = document.getElementById('groupMsgInput').value.trim();
            if (!content) return;

            stompClient.send('/app/chat/message', {}, JSON.stringify({
                type   : 'TALK',
                roomId : roomId,
                sender : sender,
                content: content
            }));
            document.getElementById('groupMsgInput').value = '';
        }

        /* DM 전송 */
        function sendDm() {
            const receiver = document.getElementById('dmReceiver').value.trim();
            const content  = document.getElementById('dmMsgInput').value.trim();
            if (!receiver) { alert('받는 사람 닉네임을 입력하세요.'); return; }
            if (!content)  return;

            stompClient.send('/app/dm', {}, JSON.stringify({
                receiver: receiver,
                content : content
            }));
            document.getElementById('dmMsgInput').value = '';
        }

        /* 그룹 메시지 렌더링 */
        function renderGroupMessage(data) {
            const chatArea = document.getElementById('chatArea');
            const div = document.createElement('div');

            if (data.type === 'TALK') {
                div.className = 'msg talk';
                div.innerHTML =
                    '<span class="sender">' + escapeHtml(data.sender) + '</span>' +
                    '<span class="bubble">' + escapeHtml(data.content) + '</span>';
            } else {
                // ENTER / LEAVE
                div.className = 'msg ' + data.type.toLowerCase();
                div.textContent = data.content;
            }

            chatArea.appendChild(div);
            chatArea.scrollTop = chatArea.scrollHeight;
        }

        /* DM 메시지 렌더링 */
        function renderDmMessage(data) {
            const chatArea = document.getElementById('chatArea');
            const isMine   = data.sender === sender;

            const div = document.createElement('div');
            div.className = 'msg dm ' + (isMine ? 'mine' : 'other');

            const meta = document.createElement('div');
            meta.className   = 'meta';
            meta.textContent = isMine
                ? '▶ ' + data.receiver + ' 에게 (귓속말)'
                : '◀ ' + data.sender   + ' 로부터 (귓속말)';

            const bubble = document.createElement('div');
            bubble.className   = 'bubble';
            bubble.textContent = data.content;

            div.appendChild(meta);
            div.appendChild(bubble);
            chatArea.appendChild(div);
            chatArea.scrollTop = chatArea.scrollHeight;
        }

        function escapeHtml(str) {
            return str.replace(/&/g,'&amp;').replace(/</g,'&lt;')
                      .replace(/>/g,'&gt;').replace(/"/g,'&quot;');
        }

        document.addEventListener('DOMContentLoaded', function () {
            document.getElementById('groupMsgInput').addEventListener('keydown',
                (e) => { if (e.key === 'Enter') sendGroupMessage(); });
            document.getElementById('dmMsgInput').addEventListener('keydown',
                (e) => { if (e.key === 'Enter') sendDm(); });
            document.getElementById('nicknameInput').addEventListener('keydown',
                (e) => { if (e.key === 'Enter') enterRoom(); });
        });
    </script>
</body>
</html>
```

---

### 메시지 타입별 렌더링 흐름

```
STOMP 메시지 수신
    │
    ├─ /topic/chat/{roomId}  → renderGroupMessage()
    │       ├─ type: TALK    → 그룹 말풍선 (회색)
    │       ├─ type: ENTER   → 시스템 안내 (이탤릭)
    │       └─ type: LEAVE   → 시스템 안내 (이탤릭)
    │
    └─ /user/queue/private   → renderDmMessage()
            ├─ sender == 나  → 오른쪽 말풍선 (노란색) "▶ 수신자에게"
            └─ sender != 나  → 왼쪽 말풍선 (하늘색)  "◀ 발신자로부터"
```

---

### 실습 체크리스트

- [ ] 두 개의 브라우저 탭에서 같은 채팅방에 각각 다른 닉네임으로 입장한다.
- [ ] 그룹 채팅 메시지를 보내면 두 탭에 모두 표시되는지 확인한다.
- [ ] A → B 로 DM을 전송하고, B의 화면에만 DM 말풍선이 표시되는지 확인한다.
- [ ] 발신자(A)의 화면에도 "▶ B에게 (귓속말)" 말풍선이 표시되는지 확인한다.
- [ ] C라는 세 번째 탭을 열어, A → B DM이 C에는 전달되지 않는지 확인한다.
- [ ] 브라우저 개발자 도구 → Network → WS 탭에서 두 구독 프레임을 모두 확인한다.

---

### 단원 정리

| 핵심 개념 | 요약 |
|---|---|
| `/queue` | 개인 구독 채널 prefix, 특정 사용자에게만 전달 |
| `/user` prefix | Spring이 사용자별 채널을 격리하기 위해 사용하는 prefix |
| `setUserDestinationPrefix` | `/user` prefix를 WebSocket 설정에 명시적으로 등록 |
| `HandshakeInterceptor` | 연결 시 username을 세션 속성으로 추출 |
| `CustomHandshakeHandler` | 세션 속성에서 `Principal` 객체를 생성해 Spring에 등록 |
| `convertAndSendToUser` | 지정한 username의 개인 채널에 능동적으로 메시지 전송 |
| 통합 구독 | 하나의 STOMP 연결에서 `/topic`과 `/user/queue`를 동시에 구독 |
| 타입 기반 렌더링 | `type` 필드(TALK / DM)에 따라 렌더링 방식 분기 |

---

### 확인 문제

1. STOMP에서 `/topic`과 `/queue`의 메시지 전달 대상이 다른 이유를 설명하시오.
2. 클라이언트가 `/user/queue/private`을 구독할 때, 내부적으로 어떤 경로로 변환되는지 설명하시오.
3. `convertAndSendToUser("alice", "/queue/private", message)`를 호출할 때의 실제 전송 경로는?
4. `HandshakeInterceptor`와 `CustomHandshakeHandler`가 각각 담당하는 역할의 차이를 설명하시오.
5. 서버에서 발신자를 `message.getSender()`가 아닌 `principal.getName()`으로 설정하는 이유는?
