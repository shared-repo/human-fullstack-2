<%@ page language="java" 
		 contentType="text/html; charset=UTF-8"
    	 pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Forward</title>
</head>
<body>
	<%
		pageContext.setAttribute("page-data", "This is Page Scope Data (from forward)");
		request.setAttribute("req-data", "This is Request Scope Data (from forward)");
		session.setAttribute("session-data", "This is Session Scope Data (from forward)");
		application.setAttribute("application-data", "This is Application Scope Data (from forward)");
		
		// 1. jsp에서만 사용하는 forward 방식
		// pageContext.forward("images/nature.jpg");
		// pageContext.forward("04.result.jsp"); // 04.result.jsp 페이지로 forward 이동
		
		// 2. jsp와 servlet에서 사용하는 forward 방식
		// RequestDispatcher rd = request.getRequestDispatcher("04.result.jsp");
		// rd.forward(request, response);
	%>
	
	<%-- 3. jsp에서만 사용하는 forward 방식 --%>
	<jsp:forward page="04.result.jsp" />
</body>
</html>