package kr.seoulplan.web.chat;

import java.net.ServerSocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatProxyControllerTest {

	private static final String SESSION_ID = "6f1c2b9e-1d2a-4a59-9d7e-1b2c3d4e5f60";

	private static final String REQUEST = """
			{"session_id": "%s", "message": "보행일상권이란?"}""".formatted(SESSION_ID);

	private MockRestServiceServer server;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://chatbot-api");
		this.server = MockRestServiceServer.bindTo(builder).build();
		this.mockMvc = mockMvcFor(new ChatbotApiClient(builder.build()));
	}

	private static MockMvc mockMvcFor(ChatbotApiClient client) {
		return MockMvcBuilders.standaloneSetup(new ChatProxyController(client)).build();
	}

	private void expectUpstream(org.springframework.test.web.client.ResponseCreator response) {
		this.server.expect(requestTo("http://chatbot-api/api/chat"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().json(REQUEST))
			.andRespond(response);
	}

	private org.springframework.test.web.servlet.ResultActions postChat(String body) throws Exception {
		return this.mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content(body));
	}

	@Test
	void forwardsRequestAndReturnsResponse() throws Exception {
		expectUpstream(withSuccess("""
				{"session_id": "%s", "answer": "보행일상권은 ...\\n참고: p.54", "category": "urban_planning",
				 "sources": [{"page": 54, "snippet": "보행일상권"}]}""".formatted(SESSION_ID),
				MediaType.APPLICATION_JSON));

		postChat(REQUEST).andExpect(status().isOk())
			.andExpect(jsonPath("$.session_id").value(SESSION_ID))
			.andExpect(jsonPath("$.category").value("urban_planning"))
			.andExpect(jsonPath("$.answer").value("보행일상권은 ...\n참고: p.54"))
			.andExpect(jsonPath("$.sources[0].page").value(54));
		this.server.verify();
	}

	@Test
	void upstream502IsPassedThroughWithDetail() throws Exception {
		expectUpstream(withStatus(HttpStatus.BAD_GATEWAY).contentType(MediaType.APPLICATION_JSON)
			.body("{\"detail\": \"일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.\"}"));

		postChat(REQUEST).andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.detail").value("일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
	}

	@Test
	void upstream503IsPassedThrough() throws Exception {
		expectUpstream(withStatus(HttpStatus.SERVICE_UNAVAILABLE).contentType(MediaType.APPLICATION_JSON)
			.body("{\"detail\": \"챗봇 데이터가 아직 준비되지 않았습니다.\"}"));

		postChat(REQUEST).andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.detail").value("챗봇 데이터가 아직 준비되지 않았습니다."));
	}

	@Test
	void upstream422BecomesBadRequest() throws Exception {
		expectUpstream(withStatus(HttpStatus.UNPROCESSABLE_CONTENT).contentType(MediaType.APPLICATION_JSON)
			.body("{\"detail\": [{\"msg\": \"too long\"}]}"));

		postChat(REQUEST).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value(ChatbotApiClient.INVALID_MESSAGE));
	}

	@Test
	void unexpectedUpstreamErrorBecomesBadGateway() throws Exception {
		expectUpstream(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error"));

		postChat(REQUEST).andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.detail").value(ChatbotApiClient.UPSTREAM_ERROR_MESSAGE));
	}

	@Test
	void invalidRequestsAreRejectedWithoutCallingUpstream() throws Exception {
		postChat("{\"session_id\": \"%s\", \"message\": \"   \"}".formatted(SESSION_ID))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value(ChatbotApiClient.INVALID_MESSAGE));
		postChat("{\"session_id\": \"%s\", \"message\": \"%s\"}".formatted(SESSION_ID, "가".repeat(1001)))
			.andExpect(status().isBadRequest());
		postChat("{\"session_id\": \"not-a-uuid\", \"message\": \"질문\"}").andExpect(status().isBadRequest());
		postChat("{\"message\": \"질문\"}").andExpect(status().isBadRequest());
		postChat("not json").andExpect(status().isBadRequest());
		this.server.verify(); // 위 요청들은 FastAPI로 전달되지 않음
	}

	@Test
	void connectionFailureReturns503() throws Exception { // AC-9
		int closedPort;
		try (ServerSocket socket = new ServerSocket(0)) {
			closedPort = socket.getLocalPort();
		}
		RestClient restClient = RestClient.builder().baseUrl("http://127.0.0.1:" + closedPort).build();
		MockMvc mvc = mockMvcFor(new ChatbotApiClient(restClient));

		mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.detail").value(ChatbotApiClient.UNAVAILABLE_MESSAGE));
	}

}
