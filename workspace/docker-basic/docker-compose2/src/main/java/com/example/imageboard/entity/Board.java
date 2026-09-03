// src/main/java/com/example/imageboard/entity/Board.java
package com.example.imageboard.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "board")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"member", "images"})
public class Board extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob                                    // 긴 텍스트 (TEXT 타입)
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private int viewCount = 0;

    // 다대일 연관관계 — Board N : Member 1
    @ManyToOne(fetch = FetchType.LAZY)      // 지연 로딩 (권장)
    @JoinColumn(name = "member_id")         // Foreign Key Column 지정
    private Member member;

    // 일대다 연관관계 — Board 1 : AttachedImage N
    @OneToMany(mappedBy = "board",
            cascade = CascadeType.ALL,  // Board 저장/삭제 시 Image도 함께 처리
            orphanRemoval = true)       // Board에서 제거된 Image는 DB에서도 삭제
    private List<AttachedImage> images = new ArrayList<>();

    // ── 생성 팩토리 ──────────────────────────────────────
    public static Board create(String title, String content, Member member) {
        Board board = new Board();
        board.title = title;
        board.content = content;
        board.member = member;
        return board;
    }

    // ── 비즈니스 메서드 ───────────────────────────────────
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void addImage(AttachedImage image) {
        this.images.add(image);
        image.setBoard(this);
    }

    /** 대표 이미지 경로 반환 (첫 번째 이미지) */
    public String getThumbnailUrl() {
        if (images.isEmpty()) return null;
        return "/images/" + images.get(0).getStoredName();
    }
}
