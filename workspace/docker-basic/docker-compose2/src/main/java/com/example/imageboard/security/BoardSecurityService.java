// src/main/java/com/example/imageboard/security/BoardSecurityService.java
package com.example.imageboard.security;

import com.example.imageboard.entity.Board;
import com.example.imageboard.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("boardSecurity")   // @PreAuthorize에서 빈 이름으로 참조
@RequiredArgsConstructor
public class BoardSecurityService {

    private final BoardRepository boardRepository;

    /**
     * 현재 로그인 사용자가 해당 게시글의 작성자인지 확인합니다.
     *
     * @param boardId 게시글 ID
     * @param memberId 현재 로그인 사용자 ID
     * @return 작성자이면 true
     */
    public boolean isOwner(Long boardId, Long memberId) {
        return boardRepository.findById(boardId)
                .map(board -> board.getMember().getId().equals(memberId))
                .orElse(false);
    }
}
