// src/main/java/com/example/imageboard/repository/BoardLikeRepository.java
package com.example.imageboard.repository;

import com.example.imageboard.entity.BoardLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface BoardLikeRepository extends JpaRepository<BoardLike, Long> {

    // select exists ( select 1 from board_like where board_id = ? and member_id = ? )
    // select 1 from board_like where board_id = ? and member_id = ?
    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);

    // select count(*) from board_like where board_id = ?
    long countByBoardId(Long boardId);

    @Transactional
    // delete from board_like where board_id = ? and member_id = ?
    void deleteByBoardIdAndMemberId(Long boardId, Long memberId);
}
