<%@ page import="java.io.File"%>
<%@ page language="java" 
		 contentType="text/html; charset=UTF-8"
    	 pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="jakarta.tags.core" %>    	 

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>파일 목록</title>
</head>
<body>
	
	<h3>[<a href="09.fileupload.jsp">파일 업로드</a>]</h3>
	<hr>
	<h3>파일 목록</h3>	
	<%
	String path = application.getRealPath("/upload-files");
	File directory = new File(path);
	File[] files = directory.listFiles(); // upload-files 디렉터리에 있는 파일과 디렉터리 목록 반환
	pageContext.setAttribute("files", files); // jstl, el에서 사용할 수 있도록 내장 객체에 데이터 저장
	%>
	<% for (File file : files) { %>
	<p>
		<a href="download?filename=<%= file.getName() %>">
			<%= file.getName() %>
		</a>
	</p>
	<% } %>
	<hr>
	<c:forEach var="file" items="${ files }">
	<p>
		<a href="download?filename=${ file.name }">
			${ file.name } <%-- el은 property 사용 --%>
		</a>
		
	</p>
	</c:forEach>

</body>
</html>










