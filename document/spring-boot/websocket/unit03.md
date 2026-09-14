# 3단원. STOMP 프로토콜과 메시지 브로커

---

## 3-1. STOMP 개요

### 학습 목표
- 순수 WebSocket의 한계를 설명하고 STOMP가 필요한 이유를 이해할 수 있다.
- STOMP의 구독/발행(Pub/Sub) 모델 동작 방식을 설명할 수 있다.

---

### 순수 WebSocket의 한계

2단원에서 구현한 순수 WebSocket 방식은 다음과 같은 문제가 있다.

| 문제 | 내용 |
|---|---|
| 메시지 라우팅 부재 | 수신한 메시지를 어디로 보낼지 직접 코드로 판단해야 함 |
| 채팅방 분리 어려움 | 방 번호별로 세션을 직접 필터링하는 로직 필요 |
| 메시지 형식 미표준화 | 클라이언트·서버 간 메시지 포맷을 별도로 정해야 함 |
| 구독 개념 없음 | 특정 토픽에 관심 있는 클라이언트만 골라 전송하기 어려움 |

이러한 문제를 해결하기 위해 **STOMP 프로토콜**을 사용한다.

---

### STOMP란?

**STOMP**(Simple Text Oriented Messaging Protocol)는 WebSocket 위에서 동작하는 **메시징 서브 프로토콜**이다.

- 텍스트 기반의 간단한 명령(Command) 구조로 이루어진다.
- **구독(Subscribe) / 발행(Publish)** 모델을 기본으로 제공한다.
- Spring, RabbitMQ, ActiveMQ 등 다양한 서버에서 지원한다.

---

### STOMP 프레임 구조

STOMP는 데이터를 **프레임(Frame)** 단위로 전송하며, 구조는 다음과 같다.

```
COMMAND
header1:value1
header2:value2

Body^@
```

> `^@` 는 NULL 문자(프레임 종료 표시)

**주요 STOMP 명령어**

| 명령어 | 방향 | 설명 |
|---|---|---|
| `CONNECT` | 클라이언트 → 서버 | 연결 요청 |
| `CONNECTED` | 서버 → 클라이언트 | 연결 승인 |
| `SUBSCRIBE` | 클라이언트 → 서버 | 특정 토픽 구독 |
| `SEND` | 클라이언트 → 서버 | 메시지 전송 |
| `MESSAGE` | 서버 → 클라이언트 | 구독자에게 메시지 전달 |
| `UNSUBSCRIBE` | 클라이언트 → 서버 | 구독 취소 |
| `DISCONNECT` | 클라이언트 → 서버 | 연결 종료 요청 |

---

### 구독/발행(Pub/Sub) 모델

STOMP의 핵심 개념은 **토픽(Topic)** 기반의 구독/발행 모델이다.

```
클라이언트 A ──SUBSCRIBE /topic/chat/room1──▶ 브로커
클라이언트 B ──SUBSCRIBE /topic/chat/room1──▶ 브로커
클라이언트 C ──SUBSCRIBE /topic/chat/room2──▶ 브로커

클라이언트 A ──SEND /app/chat/room1──▶ 서버 처리 ──▶ 브로커
                                                        ↓
                                   클라이언트 A ◀── MESSAGE
                                   클라이언트 B ◀── MESSAGE
                                   (room1 구독자에게만 전달)
```

- 클라이언트는 관심 있는 **토픽을 구독**한다.
- 서버는 해당 토픽을 구독한 **모든 클라이언트에게 자동으로 메시지를 전달**한다.
- 라우팅 로직을 직접 작성할 필요가 없다.

---

### 순수 WebSocket vs STOMP 비교

| 항목 | 순수 WebSocket | STOMP |
|---|---|---|
| 메시지 라우팅 | 직접 구현 | 토픽 기반 자동 라우팅 |
| 채팅방 분리 | 세션 필터링 직접 구현 | 토픽별 구독으로 자동 분리 |
| 메시지 포맷 | 자유 형식 | 표준 프레임 구조 |
| 구독 관리 | 직접 구현 | 브로커가 자동 관리 |
| 코드 복잡도 | 높음 | 낮음 |

---

## 3-2. Spring의 Message Broker 구조

### 학습 목표
- `@EnableWebSocketMessageBroker`로 STOMP 기반 메시지 브로커를 설정할 수 있다.
- Spring 내장 SimpleBroker와 외부 브로커의 차이를 설명할 수 있다.
- 클라이언트에서 서버까지의 메시지 흐름을 단계별로 설명할 수 있다.

---

### WebSocketMessageBrokerConfigurer 설정

#### WebSocketConfig.java (STOMP 버전으로 교체)

```java
package com.springexample.webchat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker          // STOMP 메시지 브로커 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 메시지 브로커 설정
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // 메시지 브로커가 구독 요청을 처리할 토픽 prefix
        // 클라이언트는 /topic/** 또는 /queue/**로 구독
        registry.enableSimpleBroker("/topic", "/queue");

        // 클라이언트가 서버로 메시지를 보낼 때 사용할 prefix
        // @MessageMapping 메서드로 라우팅됨
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * STOMP 엔드포인트 등록
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
            .addEndpoint("/ws")           // WebSocket 연결 엔드포인트
            .setAllowedOriginPatterns("*") // CORS 허용 (개발용)
            .withSockJS();                 // SockJS 폴백 활성화
    }
}
```

**핵심 설정 정리**

| 설정 | 값 | 의미 |
|---|---|---|
| `enableSimpleBroker` | `/topic`, `/queue` | 내장 브로커가 이 prefix의 구독을 처리 |
| `setApplicationDestinationPrefixes` | `/app` | 클라이언트 SEND 목적지의 prefix |
| `addEndpoint` | `/ws` | WebSocket 핸드셰이크 URL |
| `withSockJS()` | - | SockJS 폴백 사용 선언 |

---

### 메시지 흐름 전체 구조

```
[클라이언트]
    │
    │  SEND /app/chat/room1
    ▼
[DispatcherServlet / Channel Interceptor]
    │
    │  /app prefix 제거 후 → /chat/room1
    ▼
[@MessageMapping("/chat/{roomId}")]   ← 서버 비즈니스 로직
    │
    │  return 또는 @SendTo("/topic/chat/room1")
    ▼
[Message Broker (SimpleBroker)]
    │
    │  /topic/chat/room1 구독자 목록 조회
    ▼
[클라이언트 A, B, C]  ← MESSAGE 프레임 전달
```

---

### SimpleBroker vs 외부 브로커

| 구분 | SimpleBroker (내장) | RabbitMQ / ActiveMQ (외부) |
|---|---|---|
| 설정 난이도 | 낮음 | 높음 |
| 별도 서버 | 불필요 | 별도 설치/운영 필요 |
| 메시지 영속성 | 없음 (메모리) | 있음 |
| 다중 서버 지원 | 불가 | 가능 (스케일아웃) |
| 개발/학습 환경 | 적합 | 부적합 |
| 운영 환경 | 소규모 | 중·대규모 |

> 💡 이 과정에서는 **SimpleBroker**를 사용한다.  
> 외부 브로커 연동은 9단원에서 다룬다.

---

## 3-3. SockJS 폴백(Fallback) 처리

### 학습 목표
- SockJS가 필요한 이유를 설명할 수 있다.
- 서버와 클라이언트 양쪽에 SockJS를 적용할 수 있다.

---

### SockJS란?

일부 환경(구형 브라우저, 특정 방화벽·프록시)에서는 WebSocket 연결이 차단될 수 있다.  
**SockJS**는 WebSocket을 먼저 시도하고, 실패하면 자동으로 대체 전송 방식으로 전환해주는 **폴백(Fallback) 라이브러리**다.

**SockJS 폴백 순서**

```
WebSocket 시도
    ↓ (실패 시)
HTTP Streaming
    ↓ (실패 시)
HTTP Long Polling
```

---

### 서버 측 설정 (이미 적용됨)

`WebSocketConfig.java`의 `withSockJS()` 호출로 서버 측 설정은 완료된다.

```java
registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*")
        .withSockJS();   // SockJS 활성화
```

---

### 클라이언트 측 설정

SockJS와 STOMP 클라이언트 라이브러리를 함께 사용한다.

**CDN 방식 (빠른 실습용)**

```html
<!-- SockJS 클라이언트 -->
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<!-- STOMP 클라이언트 -->
<script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
```

**기본 연결 코드**

```javascript
// 1. SockJS로 연결 (WebSocket 대신 사용)
const socket = new SockJS('/ws');

// 2. STOMP 클라이언트 생성
const stompClient = Stomp.over(socket);

// 3. 디버그 로그 끄기 (선택)
stompClient.debug = null;

// 4. 연결
stompClient.connect({}, function (frame) {
    console.log('✅ STOMP 연결 성공:', frame);

    // 5. 토픽 구독
    stompClient.subscribe('/topic/chat/room1', function (message) {
        const body = JSON.parse(message.body);
        console.log('📩 수신:', body);
    });
});

// 6. 메시지 전송
function sendMessage(content) {
    stompClient.send(
        '/app/chat/room1',        // 서버의 @MessageMapping 경로
        {},                        // 헤더 (비어 있어도 됨)
        JSON.stringify({           // 메시지 본문 (JSON 직렬화)
            sender: '홍길동',
            content: content
        })
    );
}

// 7. 연결 종료
function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
        console.log('❌ STOMP 연결 종료');
    }
}
```

---

### STOMP 연결 흐름 요약

```
new SockJS('/ws')
    ↓  (WebSocket 또는 폴백 전송으로 연결)
Stomp.over(socket)
    ↓
stompClient.connect()
    ↓  (CONNECT 프레임 전송 → CONNECTED 프레임 수신)
stompClient.subscribe('/topic/chat/room1', callback)
    ↓  (SUBSCRIBE 프레임 전송)
stompClient.send('/app/chat/room1', {}, body)
    ↓  (SEND 프레임 전송)
서버 @MessageMapping 처리 → 브로커 → 구독자 callback 호출
```

---

### 실습: STOMP 연결 확인 페이지

```html
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>STOMP 연결 테스트</title>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
    <style>
        body { font-family: sans-serif; max-width: 600px; margin: 40px auto; }
        #log { border: 1px solid #ccc; padding: 10px; height: 200px;
               overflow-y: scroll; background: #f9f9f9; margin-bottom: 10px; }
        button { padding: 8px 16px; margin-right: 8px; cursor: pointer; }
    </style>
</head>
<body>
    <h2>STOMP 연결 테스트</h2>
    <div id="log"></div>
    <button onclick="connect()">연결</button>
    <button onclick="disconnect()">종료</button>

    <script>
        let stompClient = null;
        const log = document.getElementById("log");

        function addLog(text, color = "#333") {
            const p = document.createElement("p");
            p.textContent = text;
            p.style.cssText = `margin:4px 0; color:${color}`;
            log.appendChild(p);
            log.scrollTop = log.scrollHeight;
        }

        function connect() {
            const socket = new SockJS('/ws');
            stompClient = Stomp.over(socket);
            stompClient.debug = null;

            stompClient.connect({}, function (frame) {
                addLog('✅ STOMP 연결 성공: ' + frame.headers['version'], '#0a0');

                stompClient.subscribe('/topic/test', function (msg) {
                    addLog('📩 수신: ' + msg.body, '#0070f3');
                });

                // 연결 직후 테스트 메시지 전송
                stompClient.send('/app/test', {}, JSON.stringify({ content: '연결 테스트!' }));

            }, function (error) {
                addLog('⚠️ 연결 오류: ' + error, '#e00');
            });
        }

        function disconnect() {
            if (stompClient) {
                stompClient.disconnect();
                addLog('❌ 연결 종료', '#999');
                stompClient = null;
            }
        }
    </script>
</body>
</html>
```

---

### 실습 체크리스트

- [ ] `@EnableWebSocketMessageBroker` 설정 후 애플리케이션이 정상 기동되는지 확인한다.
- [ ] 브라우저에서 STOMP 연결 테스트 페이지에 접속하고 연결 성공 메시지를 확인한다.
- [ ] 브라우저 개발자 도구 → Network → WS에서 STOMP CONNECT / CONNECTED 프레임을 확인한다.
- [ ] SUBSCRIBE 프레임과 SEND 프레임이 올바르게 전송되는지 확인한다.
- [ ] `withSockJS()`를 제거했을 때와 적용했을 때 Network 탭의 차이를 비교한다.

---

## 단원 정리

| 핵심 개념 | 요약 |
|---|---|
| STOMP | WebSocket 위의 메시징 서브 프로토콜, Pub/Sub 모델 제공 |
| STOMP 프레임 | COMMAND + 헤더 + 본문 구조 |
| `@EnableWebSocketMessageBroker` | Spring STOMP 브로커 활성화 어노테이션 |
| `enableSimpleBroker` | 내장 브로커가 처리할 토픽 prefix 지정 |
| `setApplicationDestinationPrefixes` | 클라이언트 SEND 목적지 prefix → `@MessageMapping` 라우팅 |
| SimpleBroker | 개발용 내장 브로커, 단일 서버 환경에 적합 |
| SockJS | WebSocket 불가 환경을 위한 폴백 라이브러리 |
| `Stomp.over(socket)` | SockJS 연결 위에 STOMP 클라이언트 래핑 |

---

## 확인 문제

1. 순수 WebSocket 방식에서 채팅방을 분리하려면 어떤 로직이 필요한가? STOMP를 사용하면 어떻게 달라지는가?
2. STOMP의 `SEND`와 `MESSAGE` 명령어의 차이를 방향과 역할 측면에서 설명하시오.
3. `setApplicationDestinationPrefixes("/app")`의 역할을 메시지 흐름과 연결하여 설명하시오.
4. `enableSimpleBroker("/topic", "/queue")`에서 `/topic`과 `/queue`를 함께 등록하는 이유는 무엇인가?
5. SockJS를 사용할 때 클라이언트 코드에서 `new WebSocket(url)` 대신 `new SockJS(url)`을 사용하는 이유를 설명하시오.

