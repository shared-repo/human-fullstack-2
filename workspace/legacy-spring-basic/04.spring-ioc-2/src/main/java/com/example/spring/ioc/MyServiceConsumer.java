package com.example.spring.ioc;

import org.springframework.context.support.GenericXmlApplicationContext;

import lombok.Setter;

public class MyServiceConsumer implements ServiceConsumer {

	// 1. 직접 객체 생성
	// private TimeService timeService = new MyTimeService(); 
	// private MessageService messageService = new MyMessageService();
	
	// 2. 자동 주입 (Setter 주입)
	private MessageService messageService;
	public void setMessageService(MessageService messageService) {
		this.messageService = messageService;
	}
	
	private TimeService timeService;	
	public void setTimeService(TimeService timeService) {
		this.timeService = timeService;
	}

	///////////////
	public void doSomething() {
		System.out.println("---------------------> doSomething 메서드가 호출되었습니다.");
		String message = messageService.getMessage();
		System.out.println(message);
		message = timeService.getTimeString();
		System.out.println(message);
	}

}
