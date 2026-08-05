<%@ page language="java" 
		 contentType="text/html; charset=UTF-8"
    	 pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>JSP Object</title>
</head>
<body>

	<%-- 아래 코드에서 out 변수는 선언 없이 사용할 수 있는 내장 객체입니다. --%>
	<h2><% out.println("JSP 내장 객체 연습"); %></h2>
	
	<%-- 아래 4개의 내장 객체에는 데이터를 저장할 수 있습니다. : setAttribute 메서드 사용 --%>
	<% pageContext.setAttribute("page-data", "pageContext에 저장된 데이터"); %>
	<% request.setAttribute("request-data", "request에 저장된 데이터"); %>
	<% session.setAttribute("session-data", "session에 저장된 데이터"); %>
	<% application.setAttribute("application-data", "application에 저장된 데이터"); %>
	
	<%-- 내장 객체에 저장된 데이터 읽기 : getAttribute 메서드 사용 --%>
	<h3><%= pageContext.getAttribute("page-data") %></h3>
	<h3><%= request.getAttribute("request-data") %></h3>
	<h3><%= session.getAttribute("session-data") %></h3>
	<h3><%= application.getAttribute("application-data") %></h3>
	

</body>
</html>