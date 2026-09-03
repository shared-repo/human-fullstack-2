// src/main/java/com/example/imageboard/entity/AttachedImage.java
package com.example.imageboard.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "attached_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttachedImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalName;    // 원본 파일명 (예: 고양이.jpg)

    @Column(nullable = false, unique = true)
    private String storedName;      // 서버 저장 파일명 (예: uuid.jpg)

    @Column(nullable = false)
    private String filePath;        // 저장 경로

    private long fileSize;          // 파일 크기 (bytes)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    // board 필드 setter (Board.addImage()에서만 사용)
    void setBoard(Board board) {
        this.board = board;
    }

    public static AttachedImage create(String originalName, String storedName,
                                       String filePath, long fileSize) {
        AttachedImage image = new AttachedImage();
        image.originalName = originalName;
        image.storedName = storedName;
        image.filePath = filePath;
        image.fileSize = fileSize;
        return image;
    }
}
