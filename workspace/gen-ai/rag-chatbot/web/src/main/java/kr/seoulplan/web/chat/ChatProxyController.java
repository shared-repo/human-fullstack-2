package kr.seoulplan.web.chat;

import jakarta.validation.Valid;
import kr.seoulplan.web.chat.ChatDtos.ChatRequest;
import kr.seoulplan.web.chat.ChatDtos.ChatResponse;
import kr.seoulplan.web.chat.ChatDtos.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 브라우저의 챗봇 요청을 FastAPI로 전달하는 프록시 (F3-9).
 *
 * <p>브라우저는 FastAPI를 직접 호출하지 않는다(CORS 회피, 내부 API 은닉).
 */
@RestController
public class ChatProxyController {

	private static final Logger log = LoggerFactory.getLogger(ChatProxyController.class);

	private final ChatbotApiClient chatbotApiClient;

	public ChatProxyController(ChatbotApiClient chatbotApiClient) {
		this.chatbotApiClient = chatbotApiClient;
	}

	@PostMapping("/api/chat")
	public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
		long started = System.nanoTime();
		ChatResponse response = this.chatbotApiClient.chat(request);
		log.info("chat session={} category={} sources={} elapsed={}ms", request.sessionId(), response.category(),
				(response.sources() != null) ? response.sources().size() : 0, (System.nanoTime() - started) / 1_000_000);
		return response;
	}

	@ExceptionHandler(ChatbotApiException.class)
	ResponseEntity<ErrorResponse> handleChatbotApi(ChatbotApiException ex) {
		return ResponseEntity.status(ex.getStatus()).body(new ErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler({ MethodArgumentNotValidException.class, HttpMessageNotReadableException.class })
	ResponseEntity<ErrorResponse> handleInvalidRequest(Exception ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(new ErrorResponse(ChatbotApiClient.INVALID_MESSAGE));
	}

}
