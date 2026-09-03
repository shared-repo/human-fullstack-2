<%@ page language="java" 
		 contentType="text/html; charset=UTF-8"
    	 pageEncoding="UTF-8"%>

<!DOCTYPE html>

<html>
<head>
	<meta charset='utf-8' />
	<title>Home</title>
	<link rel='Stylesheet' href='/springdemoweb4/resources/styles/default.css' />
</head>
<body>

	<div id='pageContainer'>
		
		<%-- jsp:param --> header.jsp에 전달하는 데이터 --%>
		<jsp:include page="/WEB-INF/views/modules/header.jsp">
			<jsp:param name="bgcolor" value="hotpink" />
		</jsp:include>
		
		<div id='content'>
			<br /><br /><br />
			<h2 style='text-align:center'>Welcome to Demo WebSite !!!</h2>
		</div>
	</div>

</body>
</html>


    