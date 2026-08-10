package com.example.spring.ioc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration // app-context.xml 파일의 역할을 수행하는 클래스 
public class MyBeanConfig {
	
	@Bean(initMethod = "init", destroyMethod = "destroy") // <bean id="" class="">와 같은 역할 수행, id는 메서드 이름, 클래스는 반환하는 객체의 클래스
	@Scope("prototype")
	public MessageService messageService() {		
		return new MyMessageService();
	}	
	@Bean
	public TimeService timeService() {		
		return new MyTimeService();
	}	
	@Bean
	public ServiceConsumer serviceConsumer() {
		MyServiceConsumer bean = new MyServiceConsumer();
		bean.setMessageService(messageService()); // messageService() 호출은 <bean ... ref="">와 같은 역할
		bean.setTimeService(timeService());
		return bean;
	}
	@Bean
	public ServiceConsumer serviceConsumer2() {
		MyServiceConsumer2 bean = new MyServiceConsumer2();
		bean.setMessageService(messageService()); // messageService() 호출은 <bean ... ref="">와 같은 역할
		bean.setTimeService(timeService());
		return bean;
	}
	@Bean
	@Scope("prototype")
	public ServiceConsumer serviceConsumer3() {
		MyServiceConsumer3 bean = new MyServiceConsumer3(messageService(), timeService());
		return bean;
	}
	@Bean
	public ServiceConsumer serviceConsumer4() {
		return new MyServiceConsumer4();
	}

}
