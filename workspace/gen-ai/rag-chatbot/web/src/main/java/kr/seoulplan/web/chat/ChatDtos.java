package kr.seoulplan.web.chat;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 챗봇 API와 주고받는 JSON 형식 (PRD 3.2.1). FastAPI와 같은 snake_case 필드명을 쓴다. */
public final class ChatDtos {

	public static final int MAX_MESSAGE_CHARS = 1000;

	private ChatDtos() {
	}

	public record ChatRequest(
			@JsonProperty("session_id") @NotNull UUID sessionId,
			@NotBlank @Size(max = MAX_MESSAGE_CHARS) String message) {
	}

	public record ChatResponse(
			@JsonProperty("session_id") UUID sessionId,
			String answer,
			String category,
			List<Source> sources) {
	}

	public record Source(int page, String snippet) {
	}

	/** 오류 응답. FastAPI의 {"detail": "..."} 형식과 같다. */
	public record ErrorResponse(String detail) {
	}

}
