// src/main/java/com/example/imageboard/config/WebConfig.java
package com.example.imageboard.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

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

    /**
     * 세션에 로케일을 저장 — 브라우저를 닫을 때까지 유지
     * 기본 로케일: 한국어
     */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        // resolver.setDefaultLocale(Locale.KOREAN);
        return resolver;
    }

    /**
     * ?lang=en 같은 URL 파라미터로 로케일 전환
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");  // ?lang=en, ?lang=ko
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

}
