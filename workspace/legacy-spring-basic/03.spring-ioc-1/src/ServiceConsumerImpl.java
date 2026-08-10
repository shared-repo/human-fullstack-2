import org.springframework.context.support.GenericXmlApplicationContext;

public class ServiceConsumerImpl implements ServiceConsumer {

	@Override
	public void doSomething() {
		
		//1. 서비스 객체 생성 1 : 참조 변수도 클래스 타입, 인스턴스도 클래스 타입 ( 의존성 발생 )
		//MessageServiceImpl messageService = new MessageServiceImpl();
		
		//2. 서비스 객체 생성 2 : 참조 변수는 인터페이스 타입, 인스턴스는 클래스 타입 ( 의존성 발생 )
		//MessageService messageService = new MessageServiceImpl();
		
		//3.  서비스 객체 생성 3 : 참조 변수는 인터페이스 타입, 인스턴스 생성 구문 다른 곳으로 이동 ( 의존성 제거 )
		int no = (int)(Math.random() * 2) + 1; //1 or 2
//		MessageServiceFactory factory = new MessageServiceFactory();
//		MessageService messageService = factory.getInstance(no + "");
		
		GenericXmlApplicationContext context = new GenericXmlApplicationContext("app-context.xml");
		MessageService messageService = null;
		if (no == 1) {
			// getBean(id, type) : Spring IoC Container에 등록된 id="id" 객체를 반환
			messageService = context.getBean("messageService", MessageService.class);
		} else {
			messageService = context.getBean("messageService2", MessageService.class);
		}
		context.close();
		
		// 서비스 호출
		String message = messageService.makeMessage();
		System.out.println(message);
		
	}

}
