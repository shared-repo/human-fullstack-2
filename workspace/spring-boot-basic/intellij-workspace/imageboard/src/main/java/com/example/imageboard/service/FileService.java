// src/main/java/com/example/imageboard/service/FileService.java
package com.example.imageboard.service;

import com.example.imageboard.config.FileProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileProperties fileProperties;

    /**
     * 이미지 파일을 서버에 저장하고 저장된 파일명을 반환합니다.
     *
     * @param file 업로드된 MultipartFile
     * @return 서버에 저장된 파일명 (UUID 기반)
     */
    public String store(MultipartFile file) {
        validateFile(file);

        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName);
        String storedName = UUID.randomUUID() + "." + extension;  // 충돌 방지를 위해 고유한 파일이름 생성

        Path uploadPath = Paths.get(fileProperties.getUploadDir());
        Path filePath = uploadPath.resolve(storedName);

        try {
            Files.createDirectories(uploadPath);  // 디렉터리 없으면 생성
            file.transferTo(filePath);            // 파일 저장
            // log.info("파일 저장 완료: {} → {}", originalName, storedName);
            System.out.printf("-------------> 파일 저장 완료: {} → {}\n", originalName, storedName);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장에 실패했습니다: " + originalName, e);
        }

        return storedName;
    }

    /**
     * 썸네일을 생성하고 저장된 파일명을 반환합니다.
     * 원본 이미지와 동일한 파일명을 사용합니다.
     *
     * @param storedName 원본 파일의 저장 파일명
     */
    public void createThumbnail(String storedName) {
        Path sourcePath = Paths.get(fileProperties.getUploadDir(), storedName);
        Path thumbnailPath = Paths.get(fileProperties.getThumbnailDir());
        Path targetPath = thumbnailPath.resolve(storedName);

        try {
            Files.createDirectories(thumbnailPath);
            Thumbnails.of(sourcePath.toFile())
                    .size(300, 300)          // 최대 가로·세로 300px (비율 유지)
                    .keepAspectRatio(true)   // 원본 비율 유지
                    .outputQuality(0.85)     // 이미지 품질 85%
                    .toFile(targetPath.toFile());
            // log.info("썸네일 생성 완료: {}", storedName);
            System.out.printf("썸네일 생성 완료: {}\n", storedName);
        } catch (IOException e) {
            throw new RuntimeException("썸네일 생성에 실패했습니다: " + storedName, e);
        }
    }

    /**
     * 업로드된 파일과 썸네일을 삭제합니다.
     *
     * @param storedName 삭제할 파일명
     */
    public void delete(String storedName) {
        deleteFile(Paths.get(fileProperties.getUploadDir(), storedName));
        deleteFile(Paths.get(fileProperties.getThumbnailDir(), storedName));
    }

    // ── private 헬퍼 ─────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }
        String extension = extractExtension(file.getOriginalFilename()).toLowerCase();
        if (!fileProperties.getAllowedExtensions().contains(extension)) {
            throw new IllegalArgumentException(
                    "허용되지 않는 파일 형식입니다: " + extension
                            + " (허용: " + fileProperties.getAllowedExtensions() + ")"
            );
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("파일 확장자가 없습니다.");
        }
        // aaa/bbb/ccc/ddd.eee.fff.jpg -> jpg
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private void deleteFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // log.warn("파일 삭제 실패: {}", path, e);
            System.out.printf("----------> 파일 삭제 실패: {}\n", path, e);
        }
    }
}
