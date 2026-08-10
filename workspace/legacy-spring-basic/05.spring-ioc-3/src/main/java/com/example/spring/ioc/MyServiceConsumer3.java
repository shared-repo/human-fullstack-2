package com.example.spring.ioc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Setter;

@Component("serviceConsumer3")
@Scope("prototype")
public class MyServiceConsumer3 implements ServiceConsumer {

	private MessageService messageService;
	private TimeService timeService;

	public MyServiceConsumer3() {}
	@Autowired
	public MyServiceConsumer3(MessageService messageService, TimeService timeService) {
		this.messageService = messageService;
		this.timeService = timeService;
	}

	///////////////
	public void doSomething() {
		String message = messageService.getMessage();
		System.out.println(message);
		message = timeService.getTimeString();
		System.out.println(message);
	}

}
