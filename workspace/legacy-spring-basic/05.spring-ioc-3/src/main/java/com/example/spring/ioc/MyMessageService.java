package com.example.spring.ioc;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component("messageService") // <bean id="messageService" class="...MyMessageService" />
@Scope("prototype")
public class MyMessageService implements MessageService {
	
	// 스프링 빈의 scope 속성을 테스트하기 위한 필드
	int data;
	public MyMessageService() {
		data = (int)(Math.random() * 900) + 100;
	}
	
	public String getMessage() {
		return "Hello, Spring IoC Container !!! " + data;
	}
	
	@PostConstruct // <bean ... init-method="init">과 같은 역할
	public void init() {
		System.out.println("MyMessageService.init()");
	}
	
	@PreDestroy // <bean ... destroy-method="init">과 같은 역할
	public void destroy() {
		System.out.println("MyMessageService.destroy()");
	}
	

}
