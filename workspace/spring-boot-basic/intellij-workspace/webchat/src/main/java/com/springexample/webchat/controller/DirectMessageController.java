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
