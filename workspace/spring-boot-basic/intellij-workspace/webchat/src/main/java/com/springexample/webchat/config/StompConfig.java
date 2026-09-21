package com.springexample.webchat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker  // STOMP 메시지 브로커 활성화
public class StompConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 메시지 브로커 설정
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // 메시지 브로커가 구독 요청을 처리할 토픽 prefix
        // 클라이언트는 /topic/** 또는 /queue/**로 구독
        registry.enableSimpleBroker("/topic", "/queue");

        // 클라이언트가 서버로 메시지를 보낼 때 사용할 prefix
        // @MessageMapping 메서드로 라우팅됨
        registry.setApplicationDestinationPrefixes("/app");

        // 사용자별 개인 채널을 위한 prefix 설정 (기본값: /user)
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * STOMP 엔드포인트 등록
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/ws")           // WebSocket 연결 엔드포인트
                .setAllowedOriginPatterns("*") // CORS 허용 (개발용)
                .addInterceptors(new UsernameHandshakeInterceptor()) // username 추출
                .setHandshakeHandler(new CustomHandshakeHandler())   // Principal 등록
                .withSockJS();                 // SockJS 폴백 활성화
    }
}
