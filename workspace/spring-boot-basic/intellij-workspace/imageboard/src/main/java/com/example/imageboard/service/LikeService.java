// src/main/java/com/example/imageboard/service/LikeService.java
package com.example.imageboard.service;

import com.example.imageboard.entity.Board;
import com.example.imageboard.entity.BoardLike;
import com.example.imageboard.entity.Member;
import com.example.imageboard.repository.BoardLikeRepository;
import com.example.imageboard.repository.BoardRepository;
import com.example.imageboard.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final BoardLikeRepository boardLikeRepository;
    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;

    /**
     * 좋아요 토글 — 이미 좋아요 상태면 취소, 아니면 추가
     *
     * @return liked: 토글 후 좋아요 상태, likeCount: 현재 좋아요 수
     */
    @Transactional
    public LikeResult toggle(Long boardId, Long memberId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 글 번호입니다."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자 아이디입니다."));

        boolean alreadyLiked = boardLikeRepository
                .existsByBoardIdAndMemberId(boardId, memberId);

        if (alreadyLiked) {
            boardLikeRepository.deleteByBoardIdAndMemberId(boardId, memberId);
        } else {
            boardLikeRepository.save(BoardLike.of(board, member));
        }

        long likeCount = boardLikeRepository.countByBoardId(boardId);
        return new LikeResult(!alreadyLiked, likeCount);
    }

    @Transactional(readOnly = true)
    public LikeResult getStatus(Long boardId, Long memberId) {
        boolean liked = boardLikeRepository
                .existsByBoardIdAndMemberId(boardId, memberId);
        long count = boardLikeRepository.countByBoardId(boardId);
        return new LikeResult(liked, count);
    }

    public Long countByBoardId(Long id) {
        return boardLikeRepository.countByBoardId(id);
    }

    /** 좋아요 결과 — Java 16+ Record */
    public record LikeResult(boolean liked, long likeCount) {}
}
