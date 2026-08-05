<%@ page language="java" 
		 contentType="text/html; charset=UTF-8"
    	 pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Redirect</title>
</head>
<body>
	<%
		pageContext.setAttribute("page-data", "This is Page Scope Data (from redirect)");
		request.setAttribute("req-data", "This is Request Scope Data (from redirect)");
		session.setAttribute("session-data", "This is Session Scope Data (from redirect)");
		application.setAttribute("application-data", "This is Application Scope Data (from redirect)");
		
		// response.sendRedirect("images/nature.jpg");
		response.sendRedirect("04.result.jsp"); // 04.result.jsp 페이지로 redirect 이동
	%>
</body>
</html>