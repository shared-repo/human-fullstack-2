package com.example.spring.ioc;

import org.springframework.stereotype.Component;

public class MyMessageService implements MessageService {
	
	// 스프링 빈의 scope 속성을 테스트하기 위한 필드
	int data;
	public MyMessageService() {
		data = (int)(Math.random() * 900) + 100;
	}
	
	public String getMessage() {
		return "Hello, Spring IoC Container !!! " + data;
	}
	
	public void init() {
		System.out.println("MyMessageService.init()");
	}
	public void destroy() {
		System.out.println("MyMessageService.destroy()");
	}
	

}
