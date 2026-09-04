// src/main/java/com/example/imageboard/entity/BoardLike.java
package com.example.imageboard.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "board_like",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"board_id", "member_id"}  // 회원당 게시글 1개 좋아요 보장
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public static BoardLike of(Board board, Member member) {
        BoardLike like = new BoardLike();
        like.board = board;
        like.member = member;
        return like;
    }
}
