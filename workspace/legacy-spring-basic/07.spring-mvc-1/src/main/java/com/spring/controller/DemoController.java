package com.spring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller // @Component + 웹 기능 : IoC 컨테이너에 등록되는 객체
public class DemoController {
	
	@RequestMapping(path = { "/greetings" })
	public String home() {
		
		return "greetings";	// --> DispatcherServlet은 /WEB-INF/views/ + greetings + .jsp 파일을 View로 처리
	}

}
