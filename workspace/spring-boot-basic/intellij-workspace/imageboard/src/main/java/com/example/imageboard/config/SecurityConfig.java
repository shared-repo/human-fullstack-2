// src/main/java/com/example/imageboard/config/SecurityConfig.java
package com.example.imageboard.config;

// import com.example.imageboard.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
// @RequiredArgsConstructor
public class SecurityConfig {

    // private final CustomUserDetailsService userDetailsService;

    /** 비밀번호 암호화 인코더 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // System.out.println(passwordEncoder().encode("Pa$$w0rd"));
        InMemoryUserDetailsManager userDetailService = new InMemoryUserDetailsManager();
        userDetailService.createUser(User
                .withUsername("inmemoryuser")
                // .password("{noop}Pa$$w0rd")
                .password(passwordEncoder().encode("Pa$$w0rd"))
                .roles("USER")
                .build());
        userDetailService.createUser(User
                .withUsername("inmemoryadmin")
                // .password("{noop}Pa$$w0rd")
                .password(passwordEncoder().encode("Pa$$w0rd"))
                .roles("ADMIN")
                .build());

        return userDetailService;

    }

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
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/", "/boards").permitAll()
                        .requestMatchers("/account/**").permitAll()
                        .requestMatchers("/js/**", "/css/**").permitAll()
                        // .anyRequest().authenticated())
                        .anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable);

//        http
//                // URL별 접근 권한 설정
//                .authorizeHttpRequests(auth -> auth
//                        // 로그인 없이 접근 가능
//                        .requestMatchers("/", "/boards", "/boards/{id}").permitAll()
//                        .requestMatchers("/members/login", "/members/register").permitAll()
//                        .requestMatchers("/css/**", "/js/**", "/images/**", "/thumbnails/**").permitAll()
//                        // 게시글 작성·수정·삭제는 로그인 필요
//                        .requestMatchers("/boards/create", "/boards/*/edit").authenticated()
//                        //.requestMatchers("/boards", "/boards/**").authenticated()
//                        // 그 외 모든 요청은 로그인 필요
//                        .anyRequest().authenticated()
//                )
//
//                // 폼 로그인 설정
//                .formLogin(form -> form
//                        .loginPage("/members/login")            // 커스텀 로그인 페이지
//                        .loginProcessingUrl("/members/login")   // 로그인 폼 action URL
//                        .defaultSuccessUrl("/boards", true)     // 로그인 성공 시 이동
//                        .failureUrl("/members/login?error=true") // 로그인 실패 시 이동
//                        .usernameParameter("username")          // 폼 필드명
//                        .passwordParameter("password")
//                        .permitAll()
//                )
//
//                // 로그아웃 설정
//                .logout(logout -> logout
//                        .logoutUrl("/members/logout")
//                        .logoutSuccessUrl("/boards")
//                        .invalidateHttpSession(true)            // 세션 무효화
//                        .deleteCookies("JSESSIONID")            // 쿠키 삭제
//                        .permitAll()
//                )
//
//                // CSRF 설정 (기본 활성화 — 폼에 자동으로 _csrf 토큰이 삽입됨)
//                // 필요 시 비활성화: .csrf(csrf -> csrf.disable())
//
//                // 커스텀 UserDetailsService 등록
//                .userDetailsService(userDetailsService);

        return http.build();
    }
}
