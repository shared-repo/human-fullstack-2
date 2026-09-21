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
