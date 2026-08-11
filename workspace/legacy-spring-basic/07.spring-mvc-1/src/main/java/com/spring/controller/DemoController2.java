package com.spring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.spring.dto.PersonDto;

import jakarta.servlet.http.HttpServletRequest;

@Controller // @Component + 웹 기능 : IoC 컨테이너에 등록되는 객체
@RequestMapping(path = { "/demo" }) // 이 클래스의 모든 @~Mapping의 경로 앞에 /demo 추가
public class DemoController2 {
	
	// 1. HttpServletRequest 객체로 데이터 읽기
//	// @RequestMapping(path = { "/param" }, method = RequestMethod.GET) // GET 방식 요청 처리
//	@GetMapping(path = { "/param" })
//	public String processParam(HttpServletRequest req) {
//		
//		String data1 = req.getParameter("data1");
//		String data2 = req.getParameter("data2");
//		
//		System.out.println("----------------------> " + data1 + " / " + data2);
//		
//		return "demo/result";	// --> DispatcherServlet은 /WEB-INF/views/ + demo/result + .jsp 파일을 View로 처리
//	}
	
	// 2. 전달인자로 직접 요청 데이터 수신
//	@GetMapping(path = { "/param" })
//	public String processParam(
//			@RequestParam("data1") String data1, 
//			@RequestParam("data2") int data2) {	
//		
//		System.out.println("----------------------> " + data1 + " / " + data2);
//		
//		return "demo/result";	// --> DispatcherServlet은 /WEB-INF/views/ + demo/result + .jsp 파일을 View로 처리
//	}
	
	// 3. View로 데이터 전달
	@GetMapping(path = { "/param" })
	public String processParam(
			@RequestParam("data1") String data1, 
			@RequestParam("data2") int data2,
			Model model) { // Model 타입의 변수는 View로 데이터를 전달하는 통로	
		
		System.out.println("----------------------> " + data1 + " / " + data2);
		
		model.addAttribute("data1", data1); // Model 타입 전달인자에 데이터를 저장하면 View에서 읽을 수 있습니다.
		model.addAttribute("data2", data2);
		
		return "demo/result";	// --> DispatcherServlet은 /WEB-INF/views/ + demo/result + .jsp 파일을 View로 처리
	}
	
	// 4. POST 요청 처리 + DTO로 데이터 수신 + View로 DTO 전달
	// @RequestMapping(path = { "/param" }, method = RequestMethod.POST) // POST 방식 요청 처리
	@PostMapping(path = { "/param" })
	public String processParam2(
			@ModelAttribute("person") PersonDto person, // model.addAttribute("person", person)과 같은 효과 
			Model model) {
		
		System.out.println("----------------------> " + person);
		
		model.addAttribute("person2", person);
		
		return "demo/result";	// --> DispatcherServlet은 /WEB-INF/views/ + demo/result + .jsp 파일을 View로 처리
	}
	
	// 5. redirect
	@GetMapping(path = { "/redirect" })
	public String redirect() {
	
		System.out.println("-------------------> home으로 redirect");
		return "redirect:/home"; 
	}
	
	// 6. forward
	@GetMapping(path = { "/forward" })
	public String forward() {
	
		System.out.println("-------------------> 으로 forward");
		return "forward:/resources/forward-result.html"; 
	}
	
	// 8. Path Variable
	@GetMapping(path = { "/pathvar/{data1}" })
	public String pathVariable(@PathVariable("data1") int data1) {
	
		System.out.println("-------------------> Path Variable : " + data1);
		return "redirect:/home"; 
	}

}
