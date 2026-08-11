<%@ page language="java" 
		 contentType="text/html; charset=UTF-8"
		 pageEncoding="UTF-8" %>
		 
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

		<div id="header" style='background-color:${ not empty param.bgcolor ? param.bgcolor : "" }'>
			<div class="title">
				<a href="/springdemoweb/home">DEMO WEBSITE</a>
			</div>
			<div class="links">
			<c:choose>
				<c:when test="${ empty sessionScope.loginuser }">
					<a href='/springdemoweb/account/login'>로그인</a>
					<a href="/springdemoweb/account/register">회원가입</a>
				</c:when>
				<c:otherwise>
					${ sessionScope.loginuser.memberId }님 환영합니다.
					<a href="/springdemoweb/account/logout">로그아웃</a>
				</c:otherwise>
			</c:choose>				
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
