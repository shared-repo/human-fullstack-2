package com.example.imageboard.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.multipart.support.MultipartFilter;

@Configuration
public class WebFilterConfig {

    @Bean
    public FilterRegistrationBean<MultipartFilter> multipartFilterRegistrationBean() {
        FilterRegistrationBean<MultipartFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new MultipartFilter());
        // Ensure MultipartFilter runs before HiddenHttpMethodFilter and Spring Security
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE); // 이 필터가 최우선으로 요청을 수신하고 처리하도록 설정
        return registrationBean;
    }
}