package com.demoweb.interceptor;

import org.springframework.web.servlet.HandlerInterceptor;

import com.demoweb.dto.MemberDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuthInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler)
			throws Exception {
		
		String uri = req.getRequestURI();
		uri = uri.replace("/springdemoweb4", "");
		MemberDto member = (MemberDto)req.getSession().getAttribute("loginuser");
		if (member == null) { 
			resp.sendRedirect("/springdemoweb4/account/login?returnUrl=" + uri);
			return false; // 요청 처리 중단
		}
		return true;
	}	

}
