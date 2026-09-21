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
