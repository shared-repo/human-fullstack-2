package com.springexample.webchat.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessage {

    // 메시지 타입 열거형
    public enum MessageType {
        ENTER, TALK, LEAVE,
        DM          // 1:1 귓속말 타입
    }

    private MessageType type;    // 메시지 타입
    private String roomId;       // 채팅방 ID
    private String sender;       // 발신자 닉네임
    private String receiver;     // 수신자 (DM 전용)
    private String content;      // 메시지 내용
}
