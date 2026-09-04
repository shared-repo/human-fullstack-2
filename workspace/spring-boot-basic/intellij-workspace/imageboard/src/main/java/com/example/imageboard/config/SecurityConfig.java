// src/main/java/com/example/imageboard/config/SecurityConfig.java
package com.example.imageboard.config;

// import com.example.imageboard.security.CustomUserDetailsService;
import com.example.imageboard.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity                          // Spring Security 활성화
@EnableMethodSecurity                       // @PreAuthorize 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    /** 비밀번호 암호화 인코더 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Security setting 2와 함께 사용하는 UserDetailsService 등록
//    @Bean
//    public UserDetailsService userDetailsService() {
//        // System.out.println(passwordEncoder().encode("Pa$$w0rd"));
//        InMemoryUserDetailsManager userDetailService = new InMemoryUserDetailsManager();
//        userDetailService.createUser(User
//                .withUsername("inmemoryuser")
//                // .password("{noop}Pa$$w0rd")
//                .password(passwordEncoder().encode("Pa$$w0rd"))
//                .roles("USER")
//                .build());
//        userDetailService.createUser(User
//                .withUsername("inmemoryadmin")
//                // .password("{noop}Pa$$w0rd")
//                .password(passwordEncoder().encode("Pa$$w0rd"))
//                .roles("ADMIN")
//                .build());
//
//        return userDetailService;
//
//    }

    /** HTTP 보안 설정 */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // 1.
//        http
//                .csrf(AbstractHttpConfigurer::disable)
//                .authorizeHttpRequests((authorize) -> authorize
//                        .requestMatchers("/", "/boards").permitAll()
//                        .requestMatchers("/account/**").permitAll()
//                        .requestMatchers("/js/**", "/css/**").permitAll()
//                        .anyRequest().authenticated())
//                .httpBasic(Customizer.withDefaults())
//                .formLogin(Customizer.withDefaults());

        // 2.
//        http
//                .csrf(AbstractHttpConfigurer::disable)
//                .authorizeHttpRequests((authorize) -> authorize
//                        .requestMatchers("/", "/boards").permitAll()
//                        .requestMatchers("/account/**").permitAll()
//                        .requestMatchers("/js/**", "/css/**").permitAll()
//                        .requestMatchers("/admin").hasRole("ADMIN")
//                        .anyRequest().authenticated())
//                        //.anyRequest().permitAll())
//                .httpBasic(Customizer.withDefaults())
//                .formLogin(AbstractHttpConfigurer::disable);

        // 3.
        http
                // URL별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 로그인 없이 접근 가능
                        .requestMatchers("/", "/boards").permitAll()
                        .requestMatchers("/members/login", "/members/register").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/thumbnails/**").permitAll()
                        // 게시글 작성·수정·삭제는 로그인 필요
                        .requestMatchers("/boards/create", "/boards/*/edit").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/boards/{id}").authenticated()
                        .requestMatchers("/boards/{id}").permitAll()
                        .requestMatchers("/boards", "/boards/**").authenticated()
                        // API — GET은 비로그인 허용, 상태 변경은 로그인 필요
                        .requestMatchers(HttpMethod.GET,    "/api/**").permitAll()
                        .requestMatchers(HttpMethod.POST,   "/api/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/**").authenticated()
                        // 그 외 모든 요청은 로그인 필요
                        .anyRequest().authenticated()
                )

                // 폼 로그인 설정 (로그인 방법 설정)
                .formLogin(form -> form
                        .loginPage("/members/login")          // 커스텀 로그인 페이지
                        .loginProcessingUrl("/members/login")   // 로그인 폼 action URL : 스프링 시큐리티가 등록한 로그인 처리 컨트롤러에 매핑되는 경로 설정
                        .defaultSuccessUrl("/boards", true)     // 로그인 성공 시 이동
                        .failureUrl("/members/login?error=true") // 로그인 실패 시 이동
                        .usernameParameter("username")          // 폼 필드명
                        .passwordParameter("password")
                        .permitAll()
                )

                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/members/logout")   // 스프링 시큐리티가 등록한 로그아웃 처리 컨트롤러에 매핑되는 경로 설정
                        .logoutSuccessUrl("/boards")
                        .invalidateHttpSession(true)            // 세션 무효화
                        .deleteCookies("JSESSIONID")            // 쿠키 삭제
                        .permitAll()
                )

                // CSRF 설정 (기본 활성화 — 폼에 자동으로 _csrf 토큰이 삽입됨)
                // 필요 시 비활성화: .csrf(csrf -> csrf.disable())

                // Ajax 인증 오류 처리
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            String contentType = request.getHeader("Content-Type");
                            boolean isAjax = contentType != null
                                    && contentType.contains("application/json");

                            if (isAjax) {
                                // Ajax 요청 → JSON 오류 응답
                                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"message\":\"로그인이 필요합니다.\"}");
                            } else {
                                // 일반 요청 → 로그인 페이지 리다이렉트 (기존 동작)
                                response.sendRedirect("/members/login");
                            }
                        })
                )

                // 커스텀 UserDetailsService 등록
                .userDetailsService(userDetailsService);

        return http.build();
    }
}
