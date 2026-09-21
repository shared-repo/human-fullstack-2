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
