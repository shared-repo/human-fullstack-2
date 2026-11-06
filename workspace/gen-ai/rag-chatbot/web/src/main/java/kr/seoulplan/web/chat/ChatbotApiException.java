package kr.seoulplan.web.chat;

import org.springframework.http.HttpStatus;

/** 챗봇 API 호출 실패. 브라우저에 돌려줄 상태 코드와 사용자용 메시지를 담는다. */
public class ChatbotApiException extends RuntimeException {

	private final HttpStatus status;

	public ChatbotApiException(HttpStatus status, String userMessage) {
		this(status, userMessage, null);
	}

	public ChatbotApiException(HttpStatus status, String userMessage, Throwable cause) {
		super(userMessage, cause);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return this.status;
	}

}
