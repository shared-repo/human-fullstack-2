package kr.seoulplan.web.chat;

import java.net.URI;
import java.time.Duration;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 챗봇 API(FastAPI) 연결 설정 (F3-14).
 *
 * @param baseUrl FastAPI 주소 (예: http://localhost:8000)
 * @param connectTimeout 연결 타임아웃
 * @param readTimeout 응답 대기 타임아웃 (F3-9: 60초)
 */
@Validated
@ConfigurationProperties(prefix = "chatbot.api")
public record ChatbotApiProperties(
		@NotNull URI baseUrl,
		@DefaultValue("5s") Duration connectTimeout,
		@DefaultValue("60s") Duration readTimeout) {
}
