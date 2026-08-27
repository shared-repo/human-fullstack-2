// src/main/java/com/example/imageboard/config/WebConfig.java
package com.example.imageboard.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {    // WebMvcConfigurer 구현 클래스는 web.xml과 servlet-context.xml 파일의 코드 버전

    private final FileProperties fileProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // /images/** 요청 → 업로드 디렉터리 파일로 응답
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + fileProperties.getUploadDir() + "/");

        // /thumbnails/** 요청 → 썸네일 디렉터리 파일로 응답
        registry.addResourceHandler("/thumbnails/**")
                .addResourceLocations("file:" + fileProperties.getThumbnailDir() + "/");
    }
}
