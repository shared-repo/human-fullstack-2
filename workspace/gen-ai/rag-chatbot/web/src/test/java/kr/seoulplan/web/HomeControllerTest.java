package kr.seoulplan.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
class HomeControllerTest {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@Test
	void homeRendersTitleLauncherAndModal() throws Exception {
		this.mockMvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(view().name("home"))
			.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
			.andExpect(content().string(containsString("2040 서울도시기본계획 안내")))
			.andExpect(content().string(containsString("id=\"chat-launcher\"")))
			.andExpect(content().string(containsString("<dialog id=\"chat-dialog\"")))
			.andExpect(content().string(containsString("maxlength=\"1000\"")))
			.andExpect(content().string(containsString("/js/chat.js")));
	}

	@Test
	void staticAssetsAreServed() throws Exception {
		this.mockMvc.perform(get("/js/chat.js")).andExpect(status().isOk());
		this.mockMvc.perform(get("/css/chat.css")).andExpect(status().isOk());
	}

}
