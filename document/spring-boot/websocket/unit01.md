# 1단원. WebSocket 이해

---

## 1-1. HTTP의 한계와 실시간 통신의 필요성

### 학습 목표
- HTTP 프로토콜의 동작 방식과 구조적 한계를 설명할 수 있다.
- 실시간 통신이 필요한 서비스 유형을 나열하고, 기존 해결 방식의 문제점을 비교할 수 있다.

---

### HTTP의 기본 동작 방식

HTTP는 **요청(Request) → 응답(Response)** 의 단방향 구조로 동작한다.  
클라이언트가 먼저 요청해야만 서버가 응답할 수 있으며, 응답이 끝나면 연결이 종료된다.

```
클라이언트 ──── Request ────▶ 서버
클라이언트 ◀─── Response ─── 서버
            (연결 종료)
```

이 구조는 정적인 웹 페이지 제공에는 적합하지만, **서버가 먼저 데이터를 보내야 하는 상황**에서는 근본적인 한계가 있다.

---

### 실시간 통신이 필요한 서비스

| 서비스 유형 | 실시간 요구 사항 |
|---|---|
| 채팅 애플리케이션 | 상대방 메시지를 즉시 수신 |
| 주식 / 코인 시세 | 가격 변동을 실시간 반영 |
| 온라인 게임 | 플레이어 위치 및 이벤트 동기화 |
| 협업 도구 (구글 Docs 등) | 다른 사용자의 편집 내용 실시간 반영 |
| 배달/택시 위치 추적 | GPS 위치 지속 업데이트 |

---

### 기존 해결 방식과 문제점

#### ① Polling (단순 폴링)

클라이언트가 **일정 주기마다** 서버에 요청을 반복해서 보내는 방식.

```
클라이언트 ──── Request ────▶ 서버  (새 데이터 없음)
클라이언트 ◀─── Response ─── 서버
... (1초 후)
클라이언트 ──── Request ────▶ 서버  (새 데이터 없음)
클라이언트 ◀─── Response ─── 서버
... (1초 후)
클라이언트 ──── Request ────▶ 서버  (새 데이터 있음!)
클라이언트 ◀─── Response ─── 서버
```

**문제점**
- 새 데이터가 없어도 주기적으로 요청이 발생 → **불필요한 서버 부하**
- 폴링 주기가 길면 실시간성이 떨어지고, 짧으면 서버 부담 증가
- 동시 접속자가 많을수록 트래픽 폭발적 증가

---

#### ② Long Polling

서버가 새 데이터가 생길 때까지 **응답을 보류**하다가, 이벤트 발생 시 응답하는 방식.

```
클라이언트 ──── Request ────▶ 서버
                              (서버 대기 중...)
                              (이벤트 발생!)
클라이언트 ◀─── Response ─── 서버
클라이언트 ──── Request ────▶ 서버  (즉시 재연결)
                              (서버 대기 중...)
```

**문제점**
- 단순 Polling보다 효율적이나, 이벤트가 잦으면 연결/해제가 반복됨
- 서버에서 다수의 연결을 **대기 상태로 유지**해야 해 메모리 사용 증가
- HTTP 헤더 오버헤드가 매 요청마다 발생

---

#### ③ SSE (Server-Sent Events)

서버 → 클라이언트 방향의 **단방향 스트림**. 연결 유지 후 서버가 이벤트를 밀어 보냄.

**한계**
- 클라이언트 → 서버 방향은 별도 HTTP 요청이 필요 (단방향)
- 채팅처럼 **양방향 실시간 통신**에는 적합하지 않음

---

### 세 가지 방식 비교 요약

| 방식 | 방향 | 연결 유지 | 실시간성 | 서버 부하 |
|---|---|---|---|---|
| Polling | 단방향 | X | 낮음 | 높음 |
| Long Polling | 단방향 | 대기 중 유지 | 중간 | 중간 |
| SSE | 서버→클라이언트 | O | 높음 | 낮음 |
| **WebSocket** | **양방향** | **O** | **높음** | **낮음** |

> 💡 **결론:** 채팅처럼 클라이언트와 서버가 모두 자유롭게 메시지를 주고받아야 하는 서비스에는 **WebSocket**이 가장 적합하다.

---

## 1-2. WebSocket 프로토콜 개요

### 학습 목표
- WebSocket의 연결 수립(핸드셰이크) 과정을 설명할 수 있다.
- HTTP와 WebSocket의 연결 구조 차이를 비교할 수 있다.
- ws:// 와 wss:// 의 차이를 이해하고 보안 연결의 필요성을 설명할 수 있다.

---

### WebSocket이란?

WebSocket은 **하나의 TCP 연결** 위에서 클라이언트와 서버가 **양방향으로 자유롭게** 데이터를 주고받을 수 있는 프로토콜이다. (RFC 6455)

- 한 번 연결되면 어느 쪽에서든 먼저 메시지를 보낼 수 있다.
- 연결이 명시적으로 닫힐 때까지 유지된다.
- HTTP에 비해 **헤더 오버헤드가 매우 작다** (최소 2바이트).

```
클라이언트 ◀──────────────────▶ 서버
           (연결 유지, 양방향)
           ──── Message A ────▶
           ◀─── Message B ────
           ──── Message C ────▶
           ◀─── Message D ────
```

---

### HTTP Upgrade 핸드셰이크 과정

WebSocket은 HTTP를 통해 연결을 시작한 뒤, WebSocket 프로토콜로 **업그레이드**된다.

#### Step 1. 클라이언트 업그레이드 요청 (HTTP Request)

```http
GET /ws/chat HTTP/1.1
Host: example.com
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
Sec-WebSocket-Version: 13
```

| 헤더 | 역할 |
|---|---|
| `Upgrade: websocket` | WebSocket으로 프로토콜 전환 요청 |
| `Connection: Upgrade` | 연결 업그레이드 의사 표시 |
| `Sec-WebSocket-Key` | 보안 검증용 랜덤 키 (Base64 인코딩) |
| `Sec-WebSocket-Version` | WebSocket 프로토콜 버전 (현재 13) |

#### Step 2. 서버 업그레이드 승인 (HTTP Response)

```http
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
```

- 상태 코드 **101 Switching Protocols** → 업그레이드 승인
- `Sec-WebSocket-Accept`: 클라이언트의 Key를 서버가 검증한 응답 값

#### Step 3. WebSocket 연결 수립 완료

이 시점부터 HTTP 프로토콜은 종료되고, **WebSocket 프레임** 단위로 통신이 이루어진다.

```
[HTTP 핸드셰이크] ──▶ [101 Switching] ──▶ [WebSocket 양방향 통신]
```

---

### WebSocket 프레임 구조

WebSocket은 데이터를 **프레임(Frame)** 단위로 전송한다. HTTP의 요청/응답 대비 헤더가 극적으로 작다.

| 구분 | HTTP 요청 헤더 크기 | WebSocket 프레임 오버헤드 |
|---|---|---|
| 일반 요청 | 200~800 바이트 | **최소 2 바이트** |

---

### ws:// vs wss://

| 스킴 | 설명 | 포트 | 암호화 |
|---|---|---|---|
| `ws://` | 일반 WebSocket | 80 | X (평문 전송) |
| `wss://` | 보안 WebSocket (TLS) | 443 | O (SSL/TLS 암호화) |

**wss:// 를 사용해야 하는 이유**
- 채팅 메시지는 개인 정보를 포함할 수 있으므로 반드시 암호화 필요
- 대부분의 브라우저에서 HTTPS 페이지 내에서 `ws://` 연결은 **Mixed Content 오류**로 차단됨
- 실서비스에서는 항상 `wss://` 사용

> 💡 Spring Boot에서는 Nginx 등 리버스 프록시에서 SSL을 종료(SSL Termination)하면, 애플리케이션 레벨에서는 `ws://`로 동작하더라도 외부에서는 `wss://`로 안전하게 노출된다.

---

## 1-3. 브라우저 WebSocket API 기초 실습

### 학습 목표
- 브라우저 내장 WebSocket API를 사용해 서버와 연결하고 메시지를 주고받을 수 있다.
- WebSocket의 생명주기(연결 → 통신 → 종료) 이벤트를 코드로 처리할 수 있다.

---

### 브라우저 WebSocket API

브라우저는 별도 라이브러리 없이 WebSocket을 지원한다.

```javascript
// 1. WebSocket 객체 생성 (연결 시작)
const socket = new WebSocket("ws://localhost:8080/ws/chat");

// 2. 연결 성공 이벤트
socket.onopen = function (event) {
    console.log("✅ WebSocket 연결 성공");
    socket.send("안녕하세요!"); // 메시지 전송
};

// 3. 메시지 수신 이벤트
socket.onmessage = function (event) {
    console.log("📩 수신 메시지:", event.data);
};

// 4. 연결 종료 이벤트
socket.onclose = function (event) {
    console.log("❌ WebSocket 연결 종료:", event.code, event.reason);
};

// 5. 에러 이벤트
socket.onerror = function (error) {
    console.error("⚠️ WebSocket 오류:", error);
};

// 6. 연결 종료 (클라이언트에서 명시적 종료)
// socket.close();
```

---

### WebSocket 연결 상태 (readyState)

| 상수 | 값 | 설명 |
|---|---|---|
| `WebSocket.CONNECTING` | 0 | 연결 중 |
| `WebSocket.OPEN` | 1 | 연결 완료, 통신 가능 |
| `WebSocket.CLOSING` | 2 | 종료 진행 중 |
| `WebSocket.CLOSED` | 3 | 연결 종료 |

```javascript
if (socket.readyState === WebSocket.OPEN) {
    socket.send("메시지 전송 가능!");
}
```

---

### 실습: 에코(Echo) 테스트

**목표:** 서버에 메시지를 보내면 서버가 그대로 돌려주는(Echo) 동작을 확인한다.

#### 실습용 HTML 파일 (echo-test.html)

```html
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>WebSocket Echo 테스트</title>
    <style>
        body { font-family: sans-serif; max-width: 600px; margin: 40px auto; }
        #log { border: 1px solid #ccc; padding: 10px; height: 200px;
               overflow-y: scroll; background: #f9f9f9; margin-bottom: 10px; }
        input { width: 70%; padding: 8px; }
        button { padding: 8px 16px; margin-left: 8px; cursor: pointer; }
        .recv { color: #0070f3; }
        .send { color: #333; }
        .sys  { color: #999; font-style: italic; }
    </style>
</head>
<body>
    <h2>WebSocket Echo 테스트</h2>
    <div id="log"></div>
    <input type="text" id="msgInput" placeholder="메시지를 입력하세요" />
    <button onclick="sendMessage()">전송</button>
    <button onclick="disconnect()" style="background:#e00;color:#fff">연결 종료</button>

    <script>
        const log = document.getElementById("log");
        const input = document.getElementById("msgInput");

        function appendLog(text, cls) {
            const p = document.createElement("p");
            p.textContent = text;
            p.className = cls;
            p.style.margin = "4px 0";
            log.appendChild(p);
            log.scrollTop = log.scrollHeight;
        }

        // WebSocket 연결
        const socket = new WebSocket("ws://localhost:8080/ws/echo");

        socket.onopen = () => appendLog("✅ 연결 성공", "sys");

        socket.onmessage = (e) => appendLog("📩 수신: " + e.data, "recv");

        socket.onclose = (e) => appendLog("❌ 연결 종료 (code: " + e.code + ")", "sys");

        socket.onerror = () => appendLog("⚠️ 오류 발생", "sys");

        function sendMessage() {
            const msg = input.value.trim();
            if (!msg || socket.readyState !== WebSocket.OPEN) return;
            socket.send(msg);
            appendLog("📤 전송: " + msg, "send");
            input.value = "";
        }

        function disconnect() {
            socket.close();
        }

        // Enter 키로 전송
        input.addEventListener("keydown", (e) => {
            if (e.key === "Enter") sendMessage();
        });
    </script>
</body>
</html>
```

---

### Spring Boot 에코 서버 구현 (미리 보기)

> 2단원에서 상세히 다루며, 여기서는 실습 환경 확인 용도로 참고한다.

```java
@Component
public class EchoWebSocketHandler extends TextWebSocketHandler {

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
            throws Exception {
        // 수신한 메시지를 그대로 돌려보냄
        String payload = message.getPayload();
        session.sendMessage(new TextMessage("에코: " + payload));
    }
}
```

---

### 실습 체크리스트

- [ ] `echo-test.html`을 브라우저에서 열고 에코 서버에 연결한다.
- [ ] 메시지를 전송하고 에코 응답이 화면에 출력되는지 확인한다.
- [ ] 브라우저 개발자 도구 → **Network** 탭 → **WS** 필터로 WebSocket 프레임을 확인한다.
- [ ] 연결 종료 버튼을 눌러 `onclose` 이벤트가 발생하는지 확인한다.

---

## 단원 정리

| 핵심 개념 | 요약 |
|---|---|
| HTTP의 한계 | 요청 없이 서버가 먼저 응답 불가 |
| Polling / Long Polling | 실시간성 확보를 위한 우회 방식, 비효율적 |
| WebSocket | TCP 기반 양방향 지속 연결 프로토콜 |
| 핸드셰이크 | HTTP Upgrade 요청 → 101 응답 → WebSocket 전환 |
| wss:// | SSL/TLS 암호화 WebSocket, 실서비스 필수 |
| 브라우저 API | `new WebSocket()` + 4가지 이벤트 핸들러 |

---

## 확인 문제

1. HTTP와 WebSocket의 가장 큰 구조적 차이는 무엇인가?
2. Long Polling이 단순 Polling보다 효율적인 이유를 설명하시오.
3. WebSocket 핸드셰이크에서 HTTP 상태 코드 101의 의미는?
4. 실서비스에서 `ws://` 대신 `wss://`를 사용해야 하는 이유 두 가지를 서술하시오.
5. 브라우저 WebSocket API의 `readyState` 값 중 메시지를 전송할 수 있는 상태는?

