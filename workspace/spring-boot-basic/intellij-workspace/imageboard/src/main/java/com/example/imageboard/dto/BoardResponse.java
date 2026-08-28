package com.example.imageboard.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

// BoardResponse.java 수정
@Getter
@Builder
public class BoardResponse {

    private Long id;
    private String title;
    private String content;
    private Long memberId;
    private String author;
    private int viewCount;
    private String thumbnailUrl;
    private List<ImageResponse> images;    // 추가
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class ImageResponse {
        private Long id;
        private String originalName;
        private String storedName;
    }
}
