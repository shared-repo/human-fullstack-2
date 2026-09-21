# 7단원. 채팅 메시지 영속성 처리

---

## 7-1. 메시지 저장 전략

### 학습 목표
- WebSocket 메시지를 데이터베이스에 저장하는 방법을 이해할 수 있다.
- 동기 저장과 비동기 저장의 차이와 선택 기준을 설명할 수 있다.
- `@MessageMapping` 핸들러 내에서 JPA를 이용한 메시지 저장을 구현할 수 있다.

---

### 의존성 추가

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly    'com.h2database:h2'          // 개발용 인메모리 DB
    // 운영 환경: runtimeOnly 'com.mysql:mysql-connector-j'
}
```

---

### 메시지 저장 전략 비교

| 전략 | 방식 | 장점 | 단점 |
|---|---|---|---|
| **동기 저장** | `@MessageMapping` 안에서 즉시 저장 | 구현 간단, 저장 실패 즉시 감지 | DB 응답 지연이 메시지 전송 속도에 영향 |
| **비동기 저장** | `@Async` 또는 메시지 큐(Kafka 등) 활용 | 전송 속도와 저장 분리, 성능 유리 | 구현 복잡, 저장 실패 별도 처리 필요 |

> 학습 단계에서는 **동기 저장**으로 구현하고, 이후 성능 요구사항에 따라 비동기로 전환한다.

---

### 도메인 및 레포지토리

#### ChatMessageEntity.java

```java
package com.springexample.webchat.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomId;
    private String sender;
    private String content;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    private LocalDateTime sentAt;

    public enum MessageType { ENTER, TALK, LEAVE, DM }

    @PrePersist
    public void prePersist() {
        this.sentAt = LocalDateTime.now();
    }

    // --- Getter / Setter ---

    public Long getId()                  { return id; }
    public String getRoomId()            { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getSender()            { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getContent()           { return content; }
    public void setContent(String content){ this.content = content; }
    public MessageType getType()         { return type; }
    public void setType(MessageType type){ this.type = type; }
    public LocalDateTime getSentAt()     { return sentAt; }
}
```

> `ChatMessage`(DTO, 4단원)와 `ChatMessageEntity`(JPA Entity)를 분리하는 이유:  
> DTO는 WebSocket 메시지 전송용 구조이고, Entity는 DB 매핑용 구조다.  
> 두 역할을 분리하면 각각 독립적으로 변경할 수 있다.

---

#### ChatMessageRepository.java

```java
package com.springexample.webchat.repository;

import com.springexample.webchat.domain.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessageEntity, Long> {

    // 특정 채팅방의 최근 메시지를 시간 오름차순으로 조회
    List<ChatMessageEntity> findTop50ByRoomIdOrderBySentAtAsc(String roomId);
}
```

---

### application.properties 설정

```properties
# H2 파일 DB (개발용 — 재시작해도 데이터 유지)
spring.datasource.url=jdbc:h2:file:./data/chatdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA
# create-drop : 시작 시 DROP→CREATE, 종료 시 DROP  → 재시작마다 데이터 소멸 ❌
# update      : 시작 시 스키마 변경분만 반영, 기존 데이터 유지            ✅
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# H2 콘솔 (개발 편의)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

> **`mem:` vs `file:` 비교**
>
> | 설정 | 저장 위치 | 재시작 후 데이터 |
> |------|----------|----------------|
> | `jdbc:h2:mem:chatdb` | JVM 메모리 | 소멸 |
> | `jdbc:h2:file:./data/chatdb` | 프로젝트 루트 `data/` 디렉터리 | 유지 |
>
> `file:` 방식을 쓰면 `./data/chatdb.mv.db` 파일에 데이터가 기록되어  
> Spring Boot를 재시작해도 기존 메시지가 보존된다.  
> `ddl-auto=update`와 함께 사용해야 재시작 시 테이블이 삭제되지 않는다.

---

### @MessageMapping 내에서 메시지 저장

기존 `ChatController`에 `ChatMessageRepository`를 주입하여 수신 메시지를 즉시 저장한다.

#### ChatController.java (저장 로직 추가)

```java
package com.springexample.webchat.controller;

import com.springexample.webchat.domain.ChatMessage;
import com.springexample.webchat.domain.ChatMessageEntity;
import com.springexample.webchat.repository.ChatMessageRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository messageRepository;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                          ChatMessageRepository messageRepository) {
        this.messagingTemplate = messagingTemplate;
        this.messageRepository = messageRepository;
    }

    @MessageMapping("/chat/message")
    public void handleMessage(ChatMessage chatMessage) {

        // 1. 입장 메시지 content 설정
        if (ChatMessage.MessageType.ENTER.equals(chatMessage.getType())) {
            chatMessage.setContent(chatMessage.getSender() + "님이 입장했습니다.");
        }

        // 2. DB 저장
        ChatMessageEntity entity = toEntity(chatMessage);
        messageRepository.save(entity);

        // 3. 구독자에게 브로드캐스트
        messagingTemplate.convertAndSend(
            "/topic/chat/" + chatMessage.getRoomId(), chatMessage);
    }

    /** DTO → Entity 변환 */
    private ChatMessageEntity toEntity(ChatMessage dto) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setRoomId(dto.getRoomId());
        entity.setSender(dto.getSender());
        entity.setContent(dto.getContent());
        entity.setType(ChatMessageEntity.MessageType.valueOf(dto.getType().name()));
        return entity;
    }
}
```

---

## 7-2. 이전 메시지 조회 (채팅 히스토리)

### 학습 목표
- 채팅방 입장 시 이전 메시지를 REST API로 불러올 수 있다.
- 최근 N개 메시지를 페이지네이션 없이 단순 조회하는 방식을 구현할 수 있다.
- 클라이언트에서 히스토리를 화면에 먼저 렌더링한 뒤 실시간 메시지를 수신하는 흐름을 구현할 수 있다.

---

### REST API — 히스토리 조회 엔드포인트

#### ChatHistoryController.java

```java
package com.springexample.webchat.controller;

import com.springexample.webchat.domain.ChatMessageEntity;
import com.springexample.webchat.repository.ChatMessageRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatHistoryController {

    private final ChatMessageRepository messageRepository;

    public ChatHistoryController(ChatMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * GET /chat/history/{roomId}
     * 해당 채팅방의 최근 50개 메시지를 반환한다.
     */
    @GetMapping("/history/{roomId}")
    public List<ChatMessageEntity> getHistory(@PathVariable String roomId) {
        return messageRepository.findTop50ByRoomIdOrderBySentAtAsc(roomId);
    }
}
```

> `SecurityConfig`에 `/chat/history/**` 경로를 `permitAll()`에 추가해야 인증 없이 조회할 수 있다.  
> 또는 JWT 헤더를 포함한 REST 요청을 보내도 된다.

---

### 클라이언트: 입장 시 히스토리 로드

pre-unit06의 `DOMContentLoaded` → `GET /auth/token` 구조를 그대로 유지하면서,  
`connectStomp()` 안에서 STOMP 연결 **전에** 히스토리를 먼저 불러오도록 변경한다.  
이렇게 하면 최초 입장과 페이지 재로드 두 경로 모두 히스토리가 자동으로 로드된다.

> `enterRoom()` 함수와 `DOMContentLoaded` 핸들러는 **pre-unit06에서 이미 완성**되어 있으므로 이 단원에서 수정하지 않는다.  
> 이 단원에서 변경하는 코드는 `connectStomp()` 함수 하나뿐이다.

#### connectStomp() 함수 교체 — 히스토리 로드 추가

```javascript
// ── connectStomp(): 히스토리 로드 → STOMP 연결 ──
// isNewEntry: true  → 최초 입장 (ENTER 메시지 전송)
//             false → 페이지 재로드 후 재연결 (ENTER 메시지 전송 안 함)
async function connectStomp(token, username, isNewEntry = false) {
    // ✅ unit07에서 추가: STOMP 연결 전에 이전 메시지 히스토리 로드
    const historyRes = await fetch('/chat/history/' + roomId);
    const history    = await historyRes.json();
    history.forEach(msg => renderGroupMessage(msg));

    // 이하 unit06과 동일
    const socket = new SockJS('/ws');
    stompClient  = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect(
        { 'Authorization': 'Bearer ' + token },
        function (frame) {
            console.log('✅ 인증 연결 성공:', frame);

            stompClient.subscribe('/topic/chat/' + roomId, function (msg) {
                renderGroupMessage(JSON.parse(msg.body));
            });

            stompClient.subscribe('/user/queue/private', function (msg) {
                renderDmMessage(JSON.parse(msg.body));
            });

            if (isNewEntry) {
                stompClient.send('/app/chat/message', {}, JSON.stringify({
                    type   : 'ENTER',
                    roomId : roomId,
                    sender : username,
                    content: ''
                }));
            }

            document.getElementById('loginArea').hidden = true;
            document.getElementById('mainArea').hidden  = false;
        },
        function (error) {
            console.error('❌ 연결 실패:', error);
        }
    );
}
```

---

### 히스토리 로드 흐름

```
[클라이언트 - 채팅방 입장]
    │
    ├─ GET /chat/history/{roomId}
    │       ↓
    │   [ChatHistoryController]
    │       ↓
    │   [ChatMessageRepository.findTop50ByRoomId...]
    │       ↓
    │   JSON 응답 → renderGroupMessage() 로 화면에 렌더링
    │
    └─ STOMP CONNECT → 실시간 메시지 수신 시작
```

> 히스토리를 먼저 렌더링한 뒤 STOMP 구독을 시작하면,  
> 과거 메시지와 실시간 메시지가 자연스럽게 이어진다.

---

## 7-3. 온라인 상태 관리

### 학습 목표
- WebSocket 연결·해제 이벤트로 사용자 접속 상태를 관리할 수 있다.
- `SessionConnectedEvent`, `SessionDisconnectEvent`를 활용해 온라인 목록을 유지할 수 있다.
- `ConcurrentHashMap`을 이용한 메모리 기반 상태 관리를 구현할 수 있다.

---

### Spring WebSocket 세션 이벤트

Spring은 STOMP 연결·해제 시 아래 이벤트를 발행한다.

| 이벤트 | 발생 시점 |
|---|---|
| `SessionConnectedEvent` | 클라이언트가 STOMP CONNECT 완료 시 |
| `SessionDisconnectEvent` | 클라이언트 연결 해제(탭 닫기, 네트워크 끊김 등) 시 |

---

### 온라인 사용자 관리

#### OnlineUserService.java

```java
package com.springexample.webchat.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OnlineUserService {

    // 스레드-세이프한 온라인 사용자 목록
    private final Set<String> onlineUsers =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void addUser(String username) {
        onlineUsers.add(username);
    }

    public void removeUser(String username) {
        onlineUsers.remove(username);
    }

    public Set<String> getOnlineUsers() {
        return Collections.unmodifiableSet(onlineUsers);
    }

    public boolean isOnline(String username) {
        return onlineUsers.contains(username);
    }
}
```

---

#### WebSocketEventListener.java

```java
package com.springexample.webchat.config;

import com.springexample.webchat.service.OnlineUserService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private final OnlineUserService onlineUserService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(OnlineUserService onlineUserService,
                                  SimpMessagingTemplate messagingTemplate) {
        this.onlineUserService  = onlineUserService;
        this.messagingTemplate  = messagingTemplate;
    }

    /** STOMP 연결 완료 시 */
    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor =
            StompHeaderAccessor.wrap(event.getMessage());

        String username = (accessor.getUser() != null)
            ? accessor.getUser().getName() : null;

        if (username != null) {
            onlineUserService.addUser(username);
            // 온라인 목록 변경을 전체 구독자에게 알림
            messagingTemplate.convertAndSend(
                "/topic/online-users", onlineUserService.getOnlineUsers());
        }
    }

    /** STOMP 연결 해제 시 */
    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor =
            StompHeaderAccessor.wrap(event.getMessage());

        String username = (accessor.getUser() != null)
            ? accessor.getUser().getName() : null;

        if (username != null) {
            onlineUserService.removeUser(username);
            messagingTemplate.convertAndSend(
                "/topic/online-users", onlineUserService.getOnlineUsers());
        }
    }
}
```

---

### 클라이언트: 온라인 사용자 목록 수신

**적용 위치 ①** — `connectStomp()` 함수의 `stompClient.connect()` 성공 콜백 안,  
기존 `/topic/chat/{roomId}` 구독 바로 아래에 추가한다.

```javascript
async function connectStomp(token, username, isNewEntry = false) {
    // ... 히스토리 로드 생략 ...

    stompClient.connect(
        { 'Authorization': 'Bearer ' + token },
        function (frame) {
            stompClient.subscribe('/topic/chat/' + roomId, function (msg) {
                renderGroupMessage(JSON.parse(msg.body));
            });

            stompClient.subscribe('/user/queue/private', function (msg) {
                renderDmMessage(JSON.parse(msg.body));
            });

            // ↓ 여기에 추가 — 온라인 사용자 목록 구독
            stompClient.subscribe('/topic/online-users', function (msg) {
                const users = JSON.parse(msg.body);   // Set → JSON 배열로 직렬화됨
                renderOnlineUsers(users);
            });

            // 최초 입장일 때만 ENTER 메시지 전송
            if (isNewEntry) {
                stompClient.send('/app/chat/message', {}, JSON.stringify({
                    type: 'ENTER', roomId: roomId, sender: username, content: ''
                }));
            }

            document.getElementById('loginArea').hidden = true;
            document.getElementById('mainArea').hidden  = false;
        },
        function (error) { console.error('❌ 연결 실패:', error); }
    );
}
```

**적용 위치 ②** — `renderOnlineUsers()` 함수는 `<script>` 블록 안 전역 함수로 추가한다.

```javascript
function renderOnlineUsers(users) {
    const list = document.getElementById('online-users');
    list.innerHTML = '';
    users.forEach(u => {
        const li = document.createElement('li');
        li.textContent = u;
        list.appendChild(li);
    });
}
```

**적용 위치 ③** — `room.html` `<body>` 안, `#mainArea` 내부에 온라인 목록 패널 추가.

```html
<div id="mainArea" hidden>
    <!-- 기존: 채팅 영역, 그룹/DM 입력 영역 ... -->

    <!-- ↓ 여기에 추가 -->
    <div id="online-panel">
        <h4>온라인 사용자</h4>
        <ul id="online-users"></ul>
    </div>
</div>
```

---

### 페이지 새로고침 시 온라인 목록이 사라지는 문제

#### 원인: CONNECT 이벤트와 클라이언트 구독 사이의 타이밍 불일치

```
클라이언트가 CONNECT 프레임 전송
    ↓
SessionConnectedEvent 발행
    → handleConnect() 실행
    → /topic/online-users 브로드캐스트  ← 클라이언트 아직 미구독 → 수신 못함
    ↓
클라이언트가 CONNECTED 응답 수신
    → stompClient.subscribe('/topic/online-users', ...) 등록  ← 이미 늦음
```

자신의 연결이 발생시킨 브로드캐스트를 자신이 받지 못하므로,  
새로고침 후에는 다른 사용자가 연결/해제하기 전까지 목록이 빈 채로 남는다.

#### 해결책 A (권장): REST API로 현재 목록 즉시 조회

STOMP 구독 성공 직후 REST API를 한 번 호출해 현재 온라인 목록을 받아온다.

**서버 — `OnlineUserController.java` 추가**

```java
package com.springexample.webchat.controller;

import com.springexample.webchat.service.OnlineUserService;
import org.springframework.web.bind.annotation.*;
import java.util.Set;

@RestController
@RequestMapping("/chat")
public class OnlineUserController {

    private final OnlineUserService onlineUserService;

    public OnlineUserController(OnlineUserService onlineUserService) {
        this.onlineUserService = onlineUserService;
    }

    /** 현재 온라인 사용자 목록 조회 */
    @GetMapping("/online-users")
    public Set<String> getOnlineUsers() {
        return onlineUserService.getOnlineUsers();
    }
}
```

> **SecurityConfig 추가 불필요**: unit06에서 이미 `.requestMatchers("/", "/chat/**").permitAll()`로  
> `/chat/**` 전체를 허용했으므로 `/chat/online-users`는 별도 설정 없이 접근 가능하다.

**클라이언트 — `connectStomp()` 성공 콜백 수정**

```javascript
stompClient.connect(
    { 'Authorization': 'Bearer ' + token },
    async function (frame) {
        stompClient.subscribe('/topic/chat/' + roomId, function (msg) {
            renderGroupMessage(JSON.parse(msg.body));
        });
        stompClient.subscribe('/user/queue/private', function (msg) {
            renderDmMessage(JSON.parse(msg.body));
        });
        stompClient.subscribe('/topic/online-users', function (msg) {
            renderOnlineUsers(JSON.parse(msg.body));
        });

        // ↓ 구독 등록 직후 현재 온라인 목록을 REST로 즉시 조회
        const res   = await fetch('/chat/online-users');
        const users = await res.json();
        renderOnlineUsers(users);

        stompClient.send('/app/chat/message', {}, JSON.stringify({
            type: 'ENTER', roomId: roomId, sender: username, content: ''
        }));

        document.getElementById('loginArea').hidden = true;
        document.getElementById('mainArea').hidden  = false;
    },
    function (error) { console.error('❌ 연결 실패:', error); }
);
```

> **동작 원리**: 구독 등록 → REST 조회로 현재 목록 즉시 렌더링 →  
> 이후 연결/해제 이벤트는 `/topic/online-users` STOMP 메시지로 갱신.

#### 해결책 B (참고): 연결 시 개인 큐로 목록 전송

`handleConnect()` 에서 신규 접속자에게 `/user/queue/online-users`로 현재 목록을 개별 전송하는 방법이다.

```java
// WebSocketEventListener.handleConnect() 수정
if (username != null) {
    onlineUserService.addUser(username);

    // 전체 구독자에게 변경 알림
    messagingTemplate.convertAndSend(
        "/topic/online-users", onlineUserService.getOnlineUsers());

    // 신규 접속자에게 현재 목록 개별 전송
    messagingTemplate.convertAndSendToUser(
        username, "/queue/online-users", onlineUserService.getOnlineUsers());
}
```

클라이언트는 `/user/queue/online-users`도 추가로 구독한다.

```javascript
// 신규 접속 시 현재 목록 수신 (일회성)
stompClient.subscribe('/user/queue/online-users', function (msg) {
    renderOnlineUsers(JSON.parse(msg.body));
});
```

> **해결책 A vs B 비교**
>
> | | A (REST API) | B (개인 큐) |
> |---|---|---|
> | 구현 난이도 | 낮음 | 중간 |
> | 프로토콜 일관성 | REST + WebSocket 혼용 | WebSocket 단일 사용 |
> | 새로고침 대응 | ✅ | ✅ |
> | 실습 권장 | ✅ | 참고용 |

---

### 온라인 상태 관리 흐름

```
[클라이언트 접속]
    │  STOMP CONNECT
    ▼
[WebSocketAuthChannelInterceptor]  →  Principal 등록
    │
    ▼
[SessionConnectedEvent 발행]
    │
[WebSocketEventListener.handleConnect()]
    │  onlineUserService.addUser(username)
    │  convertAndSend("/topic/online-users", onlineUsers)
    ▼
[모든 구독자 화면에 온라인 목록 갱신]

[클라이언트 연결 해제]
    │  SessionDisconnectEvent 발행
    ▼
[WebSocketEventListener.handleDisconnect()]
    │  onlineUserService.removeUser(username)
    │  convertAndSend("/topic/online-users", onlineUsers)
    ▼
[모든 구독자 화면에 온라인 목록 갱신]
```

> **주의**: `ConcurrentHashMap` 기반 온라인 목록은 단일 서버 인스턴스에서만 유효하다.  
> 서버를 여러 대 운영(스케일 아웃)하면 각 서버의 목록이 달라진다.  
> 이 경우 **Redis Pub/Sub** 또는 외부 메시지 브로커를 사용해야 한다. (9단원 참고)

---

### 실습 체크리스트

- [ ] `ChatMessageEntity`를 생성하고 H2 콘솔(`/h2-console`)에서 테이블이 생성되는지 확인한다.
- [ ] 메시지를 전송한 후 H2 콘솔에서 `SELECT * FROM CHAT_MESSAGE`로 저장 여부를 확인한다.
- [ ] 채팅방에 입장했을 때 이전 메시지가 화면에 먼저 표시되는지 확인한다.
- [ ] 두 개의 탭을 열고 한 탭을 닫았을 때 온라인 목록이 갱신되는지 확인한다.

---

### 핵심 개념 정리

| 개념 | 설명 |
|---|---|
| `ChatMessageEntity` | DB 저장용 JPA Entity (DTO인 `ChatMessage`와 분리) |
| `@PrePersist` | Entity 저장 직전 `sentAt` 자동 설정 |
| `findTop50ByRoomIdOrderBySentAtAsc` | Spring Data JPA 메서드 이름 쿼리 |
| `SessionConnectedEvent` | STOMP 연결 완료 시 Spring이 발행하는 이벤트 |
| `SessionDisconnectEvent` | STOMP 연결 해제 시 Spring이 발행하는 이벤트 |
| `ConcurrentHashMap` | 멀티스레드 환경에서 안전한 온라인 사용자 목록 관리 |

---

### 복습 문제

1. DTO(`ChatMessage`)와 Entity(`ChatMessageEntity`)를 분리해서 사용하는 이유는 무엇인가?
2. `findTop50ByRoomIdOrderBySentAtAsc` 메서드가 동작하는 원리를 Spring Data JPA 관점에서 설명하시오.
3. 히스토리를 REST API로 불러온 뒤 STOMP 구독을 시작하는 이유는 무엇인가?
4. `SessionDisconnectEvent`가 발생하는 시나리오를 두 가지 이상 설명하시오.
5. 단일 서버의 `ConcurrentHashMap` 기반 온라인 목록이 다중 서버 환경에서 문제가 되는 이유를 설명하시오.
