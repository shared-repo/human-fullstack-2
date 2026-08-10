package com.example.spring.ioc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import lombok.Setter;

@Component("serviceConsumer4")
public class MyServiceConsumer4 implements ServiceConsumer {

	@Autowired // 필드에 자동으로 의존 객체 주입
	private MessageService messageService;
	@Autowired
	private TimeService timeService;	

	///////////////
	public void doSomething() {
		String message = messageService.getMessage();
		System.out.println(message);
		message = timeService.getTimeString();
		System.out.println(message);
	}

}
