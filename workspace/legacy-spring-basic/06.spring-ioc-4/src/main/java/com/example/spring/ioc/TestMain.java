package com.example.spring.ioc;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

public class TestMain {

	public static void main(String[] args) {

//		// 1. 직접 객체 생성 ( new 사용 )
//		ServiceConsumer consumer = new MyServiceConsumer();
//		consumer.doSomething();
		
		// 2. IoC 컨테이너 사용해서 객체 생성
		AnnotationConfigApplicationContext appContext = 
				new AnnotationConfigApplicationContext(MyBeanConfig.class);
		
		ServiceConsumer consumer = 
			appContext.getBean("serviceConsumer", ServiceConsumer.class);
		consumer.doSomething();
		
		System.out.println("------------------------------------------->");
		
		ServiceConsumer consumer2 = 
			appContext.getBean("serviceConsumer2", ServiceConsumer.class);
		consumer2.doSomething();
		
		consumer2 = 
			appContext.getBean("serviceConsumer2", ServiceConsumer.class);
		consumer2.doSomething();
		
		System.out.println("------------------------------------------->");
		
		ServiceConsumer consumer3 = 
			appContext.getBean("serviceConsumer3", ServiceConsumer.class);
		consumer3.doSomething();
		
		consumer3 = 
			appContext.getBean("serviceConsumer3", ServiceConsumer.class);
		consumer3.doSomething();
		
		System.out.println("------------------------------------------->");
		
		ServiceConsumer consumer4 = 
			appContext.getBean("serviceConsumer4", ServiceConsumer.class);
		consumer4.doSomething();
		
		consumer4 = 
			appContext.getBean("serviceConsumer4", ServiceConsumer.class);
		consumer4.doSomething();
		
		System.out.println("------------------------------------------->");
		
		appContext.close();
		
	}

}
