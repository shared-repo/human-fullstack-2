<%@ page language="java" 
		 contentType="text/html; charset=UTF-8"
    	 pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Result Page</title>
</head>
<body>

	<h2 style="text-align:center">RESULT PAGE</h2>
	<h2 style="text-align:center">1. <%= pageContext.getAttribute("page-data") %></h2>
	<h2 style="text-align:center">2. <%= request.getAttribute("req-data") %></h2>
	<h2 style="text-align:center">3. <%= session.getAttribute("session-data") %></h2>
	<h2 style="text-align:center">4. <%= application.getAttribute("application-data") %></h2>

</body>
</html>