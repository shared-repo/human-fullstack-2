package com.demoweb.servlet;

import java.io.IOException;

import com.demoweb.dto.MemberDto;
import com.demoweb.service.MemberService;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = { "/account/login" })
public class LoginServlet extends HttpServlet {
	
	private MemberService memberService = new MemberService();
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		RequestDispatcher rd = req.getRequestDispatcher("/WEB-INF/views/account/login.jsp"); // 이동기 만들기
		rd.forward(req, resp); // forward 이동
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	    // 1. 요청 데이터 읽기
	    String memberId = request.getParameter("memberId");
	    String passwd = request.getParameter("passwd");

	    // 2. 로그인 처리	    
	    MemberDto member = memberService.login(memberId, passwd);

	    if (member != null) { // 3. 로그인 성공
	    	// 로그인 처리 ( 세션 객체에 데이터 저장 )
	    	HttpSession session = request.getSession();// 세션 객체 준비 (JSP에서는 내장 객체이지만 서블릿에서는 request 객체에서 조회)
	    	session.setAttribute("loginuser", member);
	    	
	    	response.sendRedirect("/demoweb/home");
	    }
	    else { // 4. 로그인 실패
	    	
	    	// 로그인 실패에 대한 정보를 JSP에서 읽을 수 있도록 request에 저장
	    	request.setAttribute("loginFail", true);
	    	request.setAttribute("message", "아이디 또는 비밀번호가 일치하지 않습니다.");
	    	request.setAttribute("memberId", memberId);

	    	// login.jsp로 forward 이동
	    	request.getRequestDispatcher("/WEB-INF/views/account/login.jsp")
	    	       .forward(request, response);
	    }
	}

}









