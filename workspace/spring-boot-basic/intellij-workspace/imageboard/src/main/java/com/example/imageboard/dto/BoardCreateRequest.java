package com.example.imageboard.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class BoardCreateRequest {

    private String title;
    private String content;

    // 이미지 파일 목록 (선택 입력, 최대 5개)
    private List<MultipartFile> images;
}