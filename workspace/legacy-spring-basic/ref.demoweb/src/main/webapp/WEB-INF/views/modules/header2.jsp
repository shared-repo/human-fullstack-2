<%@page import="com.demoweb.dto.MemberDto"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

		<% 
		String bgColor = request.getParameter("bgcolor");
		bgColor = (bgColor == null || bgColor.length() == 0) ? "" : bgColor;
		%>

		<div id="header" style="background-color:<%= bgColor %>">
			<div class="title">
				<a href="/demoweb/home">DEMO WEBSITE</a>
			</div>
			<div class="links">
				<% MemberDto member = (MemberDto)session.getAttribute("loginuser"); %>
				<% if (member == null) { %>
				<a href='/demoweb/account/login'>로그인</a> <a
					href="/demoweb/account/register">회원가입</a>
				<% } else { %>
				<%= member.getMemberId() %>님 환영합니다. <a href="/demoweb/account/logout">로그아웃</a>
				<% } %>
			</div>
		</div>
		
		<div id="menu">
			<div>
				<ul>
					<li><a href="#">사용자관리</a></li>
					<li><a href="#">메일보내기</a></li>
					<li><a href="#">자료실</a></li>
					<li><a href="#">게시판</a></li>
				</ul>
			</div>
		</div>
		
		<div id="visitorInfo"
			style="background-color: #f5f5f5; border-bottom: 1px solid #dcdcdc; padding: 8px 15px; text-align: right; font-size: 12px;">
		
			<span> 누적 접속자 : <strong><%= application.getAttribute("totalVisitorCount") %></strong></span>
			&nbsp;&nbsp;|&nbsp;&nbsp;
			<span> 현재 접속자 : <strong><%= application.getAttribute("currentVisitorCount") %></strong></span>
		
		</div>
