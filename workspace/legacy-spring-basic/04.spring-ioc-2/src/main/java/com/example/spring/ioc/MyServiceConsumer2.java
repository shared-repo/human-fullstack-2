package com.example.spring.ioc;

import lombok.Setter;

public class MyServiceConsumer2 implements ServiceConsumer {

	@Setter
	private MessageService messageService;
	@Setter
	private TimeService timeService;	

	///////////////
	public void doSomething() {
		String message = messageService.getMessage();
		System.out.println(message);
		message = timeService.getTimeString();
		System.out.println(message);
	}

}
