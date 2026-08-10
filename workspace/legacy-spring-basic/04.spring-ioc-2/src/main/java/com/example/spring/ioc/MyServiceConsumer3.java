package com.example.spring.ioc;

import lombok.Setter;

public class MyServiceConsumer3 implements ServiceConsumer {

	private MessageService messageService;
	private TimeService timeService;

	public MyServiceConsumer3() {}
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
