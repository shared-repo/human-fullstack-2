package com.springexample.webchat.controller;

import com.springexample.webchat.config.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping("/login")
//    public String loginForm() {
//        return "auth/login"; // @RestController 설정 때문에 html이 아니라 문자열 그대로 반환
//    }
    public ModelAndView loginForm() {
        ModelAndView mav = new ModelAndView();
        mav.setViewName("auth/login");
        return mav;
    }

    /**
     * GET /auth/token
     * 세션으로 인증된 사용자에게 WebSocket 연결용 JWT를 발급한다.
     * 세션이 없으면 Spring Security가 /login.html 로 리다이렉트한다.
     */
    @GetMapping("/token")
    public ResponseEntity<?> getToken(Principal principal) {
        String token = jwtTokenProvider.createToken(principal.getName());
        return ResponseEntity.ok(Map.of(
                "token",    token,
                "username", principal.getName()
        ));
    }
}
