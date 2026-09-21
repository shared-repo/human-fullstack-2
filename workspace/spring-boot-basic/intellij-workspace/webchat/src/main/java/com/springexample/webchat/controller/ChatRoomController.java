package com.springexample.webchat.controller;

import com.springexample.webchat.domain.ChatRoom;
import com.springexample.webchat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomRepository chatRoomRepository;

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
