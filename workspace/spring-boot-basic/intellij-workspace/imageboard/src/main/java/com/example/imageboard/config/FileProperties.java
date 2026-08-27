// src/main/java/com/example/imageboard/config/FileProperties.java
package com.example.imageboard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "file") // application.properties or application.yml 파일에서 file.xxx 속성을 읽고 변수에 저장
public class FileProperties {

    private String uploadDir;   // 설정파일의 file.upload-dir 속성의 값을 저장하는 변수
    private String thumbnailDir;   // 설정파일의 file.thumbnail-dir 속성의 값을 저장하는 변수
    private List<String> allowedExtensions;   // 설정파일의 file.allowed-extensions 속성의 값을 저장하는 변수
}
