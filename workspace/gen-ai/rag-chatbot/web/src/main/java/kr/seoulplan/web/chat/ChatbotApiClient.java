package kr.seoulplan.web.chat;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

import kr.seoulplan.web.chat.ChatDtos.ChatRequest;
import kr.seoulplan.web.chat.ChatDtos.ChatResponse;
import kr.seoulplan.web.chat.ChatDtos.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;

/**
 * FastAPI 챗봇 API 호출 (F3-9). 오류는 브라우저용 상태 코드와 메시지로 바꾼다.
 *
 * <ul>
 * <li>연결 실패 → 503, 응답 시간 초과 → 504</li>
 * <li>FastAPI 400/422(입력 오류) → 400</li>
 * <li>FastAPI 502/503 → 같은 상태 코드와 FastAPI의 안내 문구</li>
 * <li>그 밖의 오류 → 502</li>
 * </ul>
 */
public class ChatbotApiClient {

	static final String INVALID_MESSAGE = "질문은 1~1,000자로 입력해 주세요.";

	static final String UNAVAILABLE_MESSAGE = "챗봇 서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.";

	static final String TIMEOUT_MESSAGE = "응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.";

	static final String UPSTREAM_ERROR_MESSAGE = "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";

	private static final Logger log = LoggerFactory.getLogger(ChatbotApiClient.class);

	private final RestClient restClient;

	public ChatbotApiClient(RestClient restClient) {
		this.restClient = restClient;
	}

	public ChatResponse chat(ChatRequest request) {
		try {
			return this.restClient.post()
				.uri("/api/chat")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(request)
				.exchange((clientRequest, response) -> {
					if (response.getStatusCode().is2xxSuccessful()) {
						ChatResponse body = response.bodyTo(ChatResponse.class);
						if (body == null) {
							throw new ChatbotApiException(HttpStatus.BAD_GATEWAY, UPSTREAM_ERROR_MESSAGE);
						}
						return body;
					}
					throw toException(response.getStatusCode(), readDetail(response));
				});
		}
		catch (ResourceAccessException ex) {
			if (isTimeout(ex)) {
				log.warn("챗봇 API 응답 시간 초과: {}", ex.getMessage());
				throw new ChatbotApiException(HttpStatus.GATEWAY_TIMEOUT, TIMEOUT_MESSAGE, ex);
			}
			log.warn("챗봇 API 연결 실패: {}", ex.getMessage());
			throw new ChatbotApiException(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE_MESSAGE, ex);
		}
	}

	private static ChatbotApiException toException(HttpStatusCode status, String detail) {
		log.warn("챗봇 API 오류 응답: status={}, detail={}", status.value(), detail);
		int code = status.value();
		if (code == 400 || code == 422) {
			return new ChatbotApiException(HttpStatus.BAD_REQUEST, INVALID_MESSAGE);
		}
		if (code == 502 || code == 503) {
			String message = (detail != null && !detail.isBlank()) ? detail : UPSTREAM_ERROR_MESSAGE;
			return new ChatbotApiException(HttpStatus.valueOf(code), message);
		}
		return new ChatbotApiException(HttpStatus.BAD_GATEWAY, UPSTREAM_ERROR_MESSAGE);
	}

	/** FastAPI 오류 본문의 detail 문자열. 형식이 다르면(예: 422의 배열) null. */
	private static String readDetail(ConvertibleClientHttpResponse response) {
		try {
			ErrorResponse error = response.bodyTo(ErrorResponse.class);
			return (error != null) ? error.detail() : null;
		}
		catch (RuntimeException ex) {
			return null;
		}
	}

	private static boolean isTimeout(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
				return true;
			}
		}
		return false;
	}

}
