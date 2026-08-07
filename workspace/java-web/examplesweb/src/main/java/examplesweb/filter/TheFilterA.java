package examplesweb.filter;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebFilter(urlPatterns = { "*.jsp" }) // 모든 jsp 파일에 대한 요청은 이 필터를 거쳐야 합니다.
public class TheFilterA implements Filter {

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {
		
		System.out.println("----------> Filter A");
		
		HttpServletRequest hreq = (HttpServletRequest)req;
		String uri = hreq.getRequestURI(); // 현재 요청의 경로 조회
		
		// if (uri.contains("hello")) {
		if (uri.contains("03") || uri.contains("07")) {
			HttpServletResponse hresp = (HttpServletResponse)resp;
			hresp.sendRedirect("index.html");
			return;
		}
		
		chain.doFilter(req, resp); // 다음 체인 또는 servlet(jsp) 등으로 요청 전달
		
	}

}
