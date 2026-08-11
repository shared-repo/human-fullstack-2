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

@WebServlet(urlPatterns = { "/account/register" })
public class RegisterServlet extends HttpServlet {
	
	private MemberService memberService = new MemberService(); // 다른 레이어의 클래스는 보통 필드로 선언합니다.
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		RequestDispatcher rd = req.getRequestDispatcher("/WEB-INF/views/account/register.jsp"); // 이동기 만들기
		rd.forward(req, resp); // forward 이동
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		// 1. 요청 데이터 읽기
		String memberId = req.getParameter("memberId");
		String passwd = req.getParameter("passwd");
		String email = req.getParameter("email");
		
		// 2. 데이터를 MemberDto 객체에 저장
		MemberDto member = new MemberDto();
		member.setMemberId(memberId);
		member.setPasswd(passwd);
		member.setEmail(email);
		
		// 3. Service 클래스 호출
		memberService.registerMember(member);
		
		// 4. home 또는 login 화면으로 redirect 이동
		resp.sendRedirect("/demoweb/account/login");
		
	}

}








