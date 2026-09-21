package com.springexample.webchat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // ① 세션 허용 — 일반 웹 페이지는 세션 인증 사용
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/ws/**").permitAll()              // WebSocket 핸드셰이크
                        .requestMatchers("/css/**", "/js/**",
                                "/images/**", "/h2-console/**").permitAll()
                        .anyRequest().authenticated()                       // 그 외 모두 로그인 필요
                )
                // ② 폼 로그인 — HTML 폼 제출로 세션 생성
                .formLogin(form -> form
                        .loginPage("/auth/login")                          // 로그인 화면 경로
                        .loginProcessingUrl("/auth/login")                 // 폼 action URL
                        .defaultSuccessUrl("/", true)                      // 성공 시 이동
                        .failureUrl("/auth/login?error=true")              // 실패 시 이동
                        .permitAll()
                )
                // ③ 로그아웃
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .headers(headers ->
                        headers.frameOptions(frame -> frame.disable())     // H2 콘솔 iframe 허용
                );

        return http.build();
    }
}
