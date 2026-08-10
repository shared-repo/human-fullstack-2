package com.example.spring.ioc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Setter;

@Component("serviceConsumer2")
public class MyServiceConsumer2 implements ServiceConsumer {

	@Setter(onMethod_ = { @Autowired }) // lombok의 @Setter가 자동으로 만드는 setter 메서드에 @Autowired 설정
	private MessageService messageService;
	@Setter(onMethod_ = { @Autowired })
	private TimeService timeService;

	///////////////
	public void doSomething() {
		String message = messageService.getMessage();
		System.out.println(message);
		message = timeService.getTimeString();
		System.out.println(message);
	}

}
