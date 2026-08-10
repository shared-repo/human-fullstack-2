package com.spring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller // @Component + 웹 기능 : IoC 컨테이너에 등록되는 객체
public class HomeController {
	
	@RequestMapping(path = { "/", "/home" })
	public String home() {
		
		return "home";	// --> DispatcherServlet은 /WEB-INF/views/ + home + .jsp 파일을 View로 처리
	}

}
