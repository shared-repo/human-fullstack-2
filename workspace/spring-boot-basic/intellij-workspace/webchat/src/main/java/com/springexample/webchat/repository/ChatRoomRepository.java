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
