# 4단원. 1:N 단체 채팅 구현

---

## 4-1. 채팅방 구조 설계

### 학습 목표
- 채팅 서비스에 필요한 도메인 모델을 설계할 수 있다.
- 메시지 타입을 분류하고 각 타입의 역할을 설명할 수 있다.

---

### 도메인 모델 설계

단체 채팅에 필요한 핵심 모델은 두 가지다.

```
ChatRoom (채팅방)
 ├── roomId    : 방 고유 식별자 (UUID)
 └── roomName  : 방 이름

ChatMessage (채팅 메시지)
 ├── type      : 메시지 타입 (ENTER / TALK / LEAVE)
 ├── roomId    : 소속 채팅방 ID
 ├── sender    : 발신자 닉네임
 └── content   : 메시지 내용
```

---

### 메시지 타입 분류

| 타입 | 발생 시점 | 처리 내용 |
|---|---|---|
| `ENTER` | 사용자가 채팅방 입장 | "○○님이 입장했습니다" 알림 전송 |
| `TALK` | 사용자가 메시지 전송 | 채팅방 전체에 메시지 브로드캐스트 |
| `LEAVE` | 사용자가 채팅방 퇴장 | "○○님이 퇴장했습니다" 알림 전송 |

---

### 도메인 클래스 구현

#### ChatMessage.java

```java
package com.springexample.webchat.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessage {

    // 메시지 타입 열거형
    public enum MessageType {
        ENTER, TALK, LEAVE
    }

    private MessageType type;    // 메시지 타입
    private String roomId;       // 채팅방 ID
    private String sender;       // 발신자 닉네임
    private String content;      // 메시지 내용
}
```

#### ChatRoom.java

```java
package com.springexample.webchat.domain;

import lombok.Getter;
import java.util.UUID;

@Getter
public class ChatRoom {

    private String roomId;
    private String roomName;

    // 정적 팩토리 메서드로 생성
    public static ChatRoom create(String roomName) {
        ChatRoom room = new ChatRoom();
        room.roomId   = UUID.randomUUID().toString();
        room.roomName = roomName;
        return room;
    }
}
```

---

### 채팅방 저장소 구현

이 단원에서는 DB 없이 **메모리(Map)** 로 채팅방 목록을 관리한다.

#### ChatRoomRepository.java

```java
package com.springexample.webchat.repository;

import com.springexample.webchat.domain.ChatRoom;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ChatRoomRepository {

    // 채팅방 저장소 (roomId → ChatRoom)
    private final Map<String, ChatRoom> chatRoomMap = new ConcurrentHashMap<>();

    /** 전체 채팅방 목록 반환 */
    public List<ChatRoom> findAllRooms() {
        return new ArrayList<>(chatRoomMap.values());
    }

    /** roomId로 채팅방 단건 조회 */
    public ChatRoom findById(String roomId) {
        return chatRoomMap.get(roomId);
    }

    /** 채팅방 생성 */
    public ChatRoom createChatRoom(String roomName) {
        ChatRoom room = ChatRoom.create(roomName);
        chatRoomMap.put(room.getRoomId(), room);
        return room;
    }
}
```

---

## 4-2. 서버 측 구현

### 학습 목표
- `@MessageMapping`과 `@SendTo`로 메시지를 라우팅할 수 있다.
- `SimpMessagingTemplate`으로 서버에서 클라이언트에게 능동적으로 메시지를 전송할 수 있다.
- 채팅방 관리를 위한 REST API를 구현할 수 있다.

---

### STOMP 메시지 컨트롤러

#### ChatController.java

```java
package com.springexample.webchat.controller;

import com.springexample.webchat.domain.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    /**
     * 클라이언트가 /app/chat/message 로 SEND하면 호출
     * 처리 결과를 /topic/chat/{roomId} 구독자에게 전달
     */
    @MessageMapping("/chat/message")
    @SendTo("/topic/chat/{roomId}")        // ← 동적 라우팅은 아래 방식 사용 권장
    public ChatMessage message(ChatMessage chatMessage) {

        // ENTER 타입이면 입장 메시지로 content 설정
        if (ChatMessage.MessageType.ENTER.equals(chatMessage.getType())) {
            chatMessage.setContent(chatMessage.getSender() + "님이 입장했습니다.");
        }
        return chatMessage;
    }
}
```

> ⚠️ `@SendTo`는 고정 경로만 지정 가능하다. 채팅방 ID처럼 **동적 경로**가 필요한 경우  
> `SimpMessagingTemplate`을 사용하는 아래 방식이 더 적합하다.

---

### SimpMessagingTemplate을 활용한 동적 라우팅

#### ChatController.java (개선 버전)

```java
package com.springexample.webchat.controller;

import com.springexample.webchat.domain.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 클라이언트가 /app/chat/message 로 SEND하면 호출
     */
    @MessageMapping("/chat/message")
    public void message(ChatMessage chatMessage) {

        // 메시지 타입별 content 가공
        if (ChatMessage.MessageType.ENTER.equals(chatMessage.getType())) {
            chatMessage.setContent(chatMessage.getSender() + "님이 입장했습니다.");

        } else if (ChatMessage.MessageType.LEAVE.equals(chatMessage.getType())) {
            chatMessage.setContent(chatMessage.getSender() + "님이 퇴장했습니다.");
        }

        // 채팅방 구독자 전체에게 전송 (동적 경로)
        messagingTemplate.convertAndSend(
            "/topic/chat/" + chatMessage.getRoomId(),  // 동적 토픽
            chatMessage
        );
    }
}
```

**`SimpMessagingTemplate` 주요 메서드**

| 메서드 | 설명 |
|---|---|
| `convertAndSend(destination, payload)` | 지정 토픽의 모든 구독자에게 전송 |
| `convertAndSendToUser(user, destination, payload)` | 특정 사용자에게만 전송 (5단원) |

---

### 채팅방 REST API

채팅방 목록 조회와 생성은 기존 REST 방식으로 처리한다.

#### ChatRoomController.java

```java
package com.springexample.webchat.controller;

import com.springexample.webchat.domain.ChatRoom;
import com.springexample.webchat.repository.ChatRoomRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/chat")
public class ChatRoomController {

    private final ChatRoomRepository chatRoomRepository;

    public ChatRoomController(ChatRoomRepository chatRoomRepository) {
        this.chatRoomRepository = chatRoomRepository;
    }

    /** 채팅방 목록 페이지 */
    @GetMapping("/rooms")
    public String rooms(Model model) {
        List<ChatRoom> rooms = chatRoomRepository.findAllRooms();
        model.addAttribute("rooms", rooms);
        return "chat/rooms";              // templates/chat/rooms.html
    }

    /** 채팅방 생성 */
    @PostMapping("/room")
    public String createRoom(@RequestParam String roomName) {
        chatRoomRepository.createChatRoom(roomName);
        return "redirect:/chat/rooms";
    }

    /** 채팅방 입장 페이지 */
    @GetMapping("/room/{roomId}")
    public String roomDetail(@PathVariable String roomId, Model model) {
        ChatRoom room = chatRoomRepository.findById(roomId);
        model.addAttribute("room", room);
        return "chat/room";               // templates/chat/room.html
    }
}
```

---

## 4-3. 클라이언트 측 구현

### 학습 목표
- SockJS + STOMP로 채팅방에 입장하고 메시지를 주고받을 수 있다.
- 메시지 타입(ENTER / TALK / LEAVE)에 따라 다르게 UI를 렌더링할 수 있다.

---

### 채팅방 목록 페이지 (rooms.html)

```html
<!-- templates/chat/rooms.html -->
<!DOCTYPE html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>채팅방 목록</title>
    <style>
        body { font-family: sans-serif; max-width: 700px; margin: 40px auto; }
        table { width: 100%; border-collapse: collapse; }
        th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }
        th { background: #f0f0f0; }
        form { margin-top: 20px; display: flex; gap: 8px; }
        input { flex: 1; padding: 8px; }
        button { padding: 8px 16px; cursor: pointer; }
    </style>
</head>
<body>
    <h2>채팅방 목록</h2>

    <table>
        <thead>
            <tr><th>방 이름</th><th>입장</th></tr>
        </thead>
        <tbody>
            <tr th:each="room : ${rooms}">
                <td th:text="${room.roomName}"></td>
                <td>
                    <a th:href="@{/chat/room/{id}(id=${room.roomId})}">입장</a>
                </td>
            </tr>
            <tr th:if="${#lists.isEmpty(rooms)}">
                <td colspan="2" style="text-align:center">생성된 채팅방이 없습니다.</td>
            </tr>
        </tbody>
    </table>

    <form action="/chat/room" method="post">
        <input type="text" name="roomName" placeholder="채팅방 이름을 입력하세요" required />
        <button type="submit">채팅방 생성</button>
    </form>
</body>
</html>
```

---

### 채팅방 페이지 (room.html)

```html
<!-- templates/chat/room.html -->
<!DOCTYPE html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title th:text="${room.roomName}">채팅방</title>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
    <style>
        body { font-family: sans-serif; max-width: 700px; margin: 40px auto; }
        #chatArea { border: 1px solid #ccc; height: 350px; overflow-y: scroll;
                    padding: 12px; background: #fafafa; margin-bottom: 10px; }
        .msg       { margin: 6px 0; }
        .msg.enter { color: #999; font-style: italic; }
        .msg.leave { color: #999; font-style: italic; }
        .msg.talk  { color: #333; }
        .msg.talk .sender { font-weight: bold; color: #0070f3; margin-right: 6px; }
        .input-row { display: flex; gap: 8px; }
        .input-row input  { flex: 1; padding: 8px; }
        .input-row button { padding: 8px 16px; cursor: pointer; }
    </style>
</head>
<body>

    <h2 th:text="${room.roomName}">채팅방</h2>
    <p><a href="/chat/rooms">← 목록으로</a></p>

    <!-- 닉네임 입력 (입장 전) -->
    <div id="enterArea">
        <input type="text" id="senderInput" placeholder="닉네임을 입력하세요" />
        <button onclick="enterRoom()">입장</button>
    </div>

    <!-- 채팅 영역 (입장 후 표시) -->
    <div id="chatArea" style="display:none"></div>
    <div class="input-row" id="sendArea" style="display:none">
        <input type="text" id="msgInput" placeholder="메시지를 입력하세요" />
        <button onclick="sendMessage()">전송</button>
        <button onclick="leaveRoom()" style="background:#e00;color:#fff">퇴장</button>
    </div>

    <script th:inline="javascript">
        const roomId = /*[[${room.roomId}]]*/ '';
        let stompClient = null;
        let sender = '';

        function enterRoom() {
            sender = document.getElementById('senderInput').value.trim();
            if (!sender) { alert('닉네임을 입력하세요.'); return; }

            // UI 전환
            document.getElementById('enterArea').style.display = 'none';
            document.getElementById('chatArea').style.display  = 'block';
            document.getElementById('sendArea').style.display  = 'flex';

            // STOMP 연결
            const socket = new SockJS('/ws');
            stompClient  = Stomp.over(socket);
            stompClient.debug = null;

            stompClient.connect({}, function () {
                // 채팅방 구독
                stompClient.subscribe('/topic/chat/' + roomId, function (msg) {
                    const data = JSON.parse(msg.body);
                    renderMessage(data);
                });

                // 입장 메시지 전송
                stompClient.send('/app/chat/message', {}, JSON.stringify({
                    type   : 'ENTER',
                    roomId : roomId,
                    sender : sender,
                    content: ''
                }));
            });
        }

        function sendMessage() {
            const content = document.getElementById('msgInput').value.trim();
            if (!content || !stompClient) return;

            stompClient.send('/app/chat/message', {}, JSON.stringify({
                type   : 'TALK',
                roomId : roomId,
                sender : sender,
                content: content
            }));

            document.getElementById('msgInput').value = '';
        }

        function leaveRoom() {
            if (!stompClient) return;

            stompClient.send('/app/chat/message', {}, JSON.stringify({
                type   : 'LEAVE',
                roomId : roomId,
                sender : sender,
                content: ''
            }));

            stompClient.disconnect(function () {
                window.location.href = '/chat/rooms';
            });
        }

        function renderMessage(data) {
            const chatArea = document.getElementById('chatArea');
            const div      = document.createElement('div');
            div.className  = 'msg ' + data.type.toLowerCase();

            if (data.type === 'TALK') {
                div.innerHTML =
                    '<span class="sender">' + escapeHtml(data.sender) + '</span>' +
                    escapeHtml(data.content);
            } else {
                div.textContent = data.content;
            }

            chatArea.appendChild(div);
            chatArea.scrollTop = chatArea.scrollHeight;
        }

        // XSS 방지용 이스케이프
        function escapeHtml(str) {
            return str.replace(/&/g,'&amp;').replace(/</g,'&lt;')
                      .replace(/>/g,'&gt;').replace(/"/g,'&quot;');
        }

        // Enter 키로 메시지 전송
        document.addEventListener('DOMContentLoaded', function () {
            document.getElementById('msgInput').addEventListener('keydown', function (e) {
                if (e.key === 'Enter') sendMessage();
            });
            document.getElementById('senderInput').addEventListener('keydown', function (e) {
                if (e.key === 'Enter') enterRoom();
            });
        });
    </script>
</body>
</html>
```

---

## 4-4. 실습: 기본 단체 채팅 웹앱 완성

### 전체 동작 흐름 정리

```
[채팅방 목록 페이지]
    ↓  채팅방 생성 (POST /chat/room)
    ↓  채팅방 목록 조회 (GET /chat/rooms)
    ↓  입장 클릭
[채팅방 페이지]
    ↓  닉네임 입력 → enterRoom()
    ↓  SockJS + STOMP 연결 → /ws
    ↓  /topic/chat/{roomId} 구독
    ↓  ENTER 메시지 전송 → /app/chat/message
    ↓  서버 ChatController.message() 처리
    ↓  /topic/chat/{roomId} 구독자 전원에게 브로드캐스트
    ↓  TALK 메시지 반복 수신/전송
    ↓  LEAVE 메시지 전송 → stompClient.disconnect() → 목록 페이지로 이동
```

---

### 최종 프로젝트 구조

```
src/main/
├── java/com/springexample/webchat/
│   ├── config/
│   │   └── WebSocketConfig.java
│   ├── controller/
│   │   ├── ChatController.java       ← STOMP 메시지 처리
│   │   └── ChatRoomController.java   ← REST (방 목록/생성/입장)
│   ├── domain/
│   │   ├── ChatMessage.java
│   │   └── ChatRoom.java
│   └── repository/
│       └── ChatRoomRepository.java
└── resources/
    └── templates/chat/
        ├── rooms.html                ← 채팅방 목록
        └── room.html                 ← 채팅방 (STOMP 클라이언트)
```

---

### 실습 체크리스트

- [ ] 채팅방 목록 페이지에서 새 채팅방을 생성하고 목록에 표시되는지 확인한다.
- [ ] 두 개의 브라우저 탭에서 같은 채팅방에 입장한다.
- [ ] 한 탭에서 메시지를 보내면 다른 탭에도 실시간으로 표시되는지 확인한다.
- [ ] 입장(ENTER) 메시지와 퇴장(LEAVE) 메시지가 회색 기울임꼴로 표시되는지 확인한다.
- [ ] 한 탭이 퇴장했을 때 남은 탭에서 퇴장 알림이 수신되는지 확인한다.
- [ ] 다른 채팅방에 입장한 탭에서는 메시지가 보이지 않는지 확인한다. (채팅방 격리 검증)

---

## 단원 정리

| 핵심 개념 | 요약 |
|---|---|
| `ChatMessage.MessageType` | ENTER / TALK / LEAVE 세 가지 타입으로 메시지 분류 |
| `ChatRoom` | UUID 기반 채팅방 식별자, 정적 팩토리 메서드로 생성 |
| `@MessageMapping` | 클라이언트 SEND 목적지와 서버 메서드를 매핑 |
| `@SendTo` | 고정 토픽으로 메시지 반환 시 사용 |
| `SimpMessagingTemplate` | 동적 토픽, 서버 주도 전송 시 사용 |
| `convertAndSend` | 지정 토픽의 모든 구독자에게 브로드캐스트 |
| 클라이언트 구독 경로 | `/topic/chat/{roomId}` — 채팅방별로 격리됨 |
| XSS 방지 | `escapeHtml()`로 사용자 입력 값 이스케이프 처리 |

---

## 확인 문제

1. `@SendTo`와 `SimpMessagingTemplate.convertAndSend()`의 차이를 적절한 사용 상황과 함께 설명하시오.
2. 채팅방 A에 입장한 사용자가 채팅방 B의 메시지를 수신하지 못하는 이유를 STOMP 토픽 구조로 설명하시오.
3. `MessageType.ENTER`일 때 서버에서 content를 재설정하는 이유는 무엇인가?
4. `ChatRoomRepository`에서 `HashMap` 대신 `ConcurrentHashMap`을 사용하는 이유를 설명하시오.
5. 클라이언트가 퇴장할 때 `stompClient.disconnect()` 전에 LEAVE 메시지를 먼저 전송해야 하는 이유는?

