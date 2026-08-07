package examplesweb.listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

@WebListener
public class ExamplesWebListener implements ServletContextListener, HttpSessionListener {

	@Override
	// 웹애플리케이션이 시작될 때 자동으로 호출
	public void contextInitialized(ServletContextEvent sce) {
		System.out.println("1. 웹애플리케이션이 시작되었습니다.");
	}
	
	@Override
	// 사용자가 브라우저를 열고 첫 번째 세션에 대한 요청을 보냈을 때
	public void sessionCreated(HttpSessionEvent se) {
		System.out.println("2. 세션이 시작되었습니다.");
	}

	@Override
	// 특정 세션 기준 마지막 요청이 발생한 후 일정시간 동안 추가 요청이 없을 때 (기본 30분)
	// invalidate를 호출했을 때
	public void sessionDestroyed(HttpSessionEvent se) {
		System.out.println("3. 세션이 종료되었습니다.");
	}

	@Override
	// 웹 애플리케이션이 종료될 때 자동으로 호출
	public void contextDestroyed(ServletContextEvent sce) {
		System.out.println("4. 웹애플리케이션이 종료되었습니다.");
	}

	

}
