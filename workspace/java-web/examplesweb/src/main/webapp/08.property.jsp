<%@ page import="examplesweb.dto.Person" %>
<%@ page language="java" 
		 contentType="text/html; charset=UTF-8"
    	 pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Property Demo</title>
</head>
<body>

	<%-- 아래 두 개의 인스턴스 생성 구문은 동일한 기능 --%>
	<% Person person = new Person(); %>
	<jsp:useBean id="person2" class="examplesweb.dto.Person" scope="page" />
	
	<%-- 아래 두 그룹의 데이터 저장 구문은 동일한 기능 --%>
	<%-- property : 변수처럼 사용하지만 실제로는 getter/setter를 호출하는 표현 방식 --%>
	<% person.setName("장동건"); %>
	<% person.setEmail("jdk@example.com"); %>
	<% person.setPhone("010-6523-7894"); %>
	<jsp:setProperty name="person2" property="name" value="공유" />
	<jsp:setProperty name="person2" property="email" value="ky@example.com" />
	<jsp:setProperty name="person2" property="phone" value="010-5214-3369" />
	
	<%-- 아래 두 그룹의 데이터 읽기 구문은 동일한 기능 --%>
	<h2>
		[<%= person.getName() %>]
		[<%= person.getPhone() %>]
		[<%= person.getEmail() %>]
	</h2>
	<h2>
		[<jsp:getProperty name="person2" property="name" />]
		[<jsp:getProperty name="person2" property="phone" />]
		[<jsp:getProperty name="person2" property="email" />]
	</h2>

</body>
</html>