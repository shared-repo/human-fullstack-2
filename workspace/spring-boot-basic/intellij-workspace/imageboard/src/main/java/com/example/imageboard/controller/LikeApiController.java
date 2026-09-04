// src/main/java/com/example/imageboard/controller/LikeApiController.java
package com.example.imageboard.controller;

import com.example.imageboard.security.CustomUserDetails;
import com.example.imageboard.service.LikeService;
import com.example.imageboard.service.LikeService.LikeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/boards/{boardId}/likes")
@RequiredArgsConstructor
public class LikeApiController {

    private final LikeService likeService;

    /** 좋아요 상태 조회 — 비로그인도 허용 (liked: false 반환) */
    @GetMapping
    public ResponseEntity<LikeResult> getStatus(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.ok(new LikeResult(false, 0L));
        }
        return ResponseEntity.ok(
                likeService.getStatus(boardId, userDetails.getMemberId()));
    }

    /** 좋아요 토글 — 로그인 필요 */
    @PostMapping
    public ResponseEntity<LikeResult> toggle(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        LikeResult result = likeService.toggle(boardId, userDetails.getMemberId());
        return ResponseEntity.ok(result);
    }
}
