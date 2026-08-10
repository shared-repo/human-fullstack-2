package com.example.spring.ioc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.stereotype.Component;

import lombok.Setter;

@Component("serviceConsumer")
public class MyServiceConsumer implements ServiceConsumer {

	private MessageService messageService;
	@Autowired
	public void setMessageService(MessageService messageService) {
		this.messageService = messageService;
	}
	
	private TimeService timeService;
	@Autowired
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
