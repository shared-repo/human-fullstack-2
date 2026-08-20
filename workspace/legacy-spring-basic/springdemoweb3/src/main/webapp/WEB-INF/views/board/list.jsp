<%@ page language="java" 
		 contentType="text/html; charset=utf-8"
    	 pageEncoding="utf-8"%>
    	 
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>

<html>
<head>
	<meta charset="utf-8" />
	<title>게시글 목록</title>
	<link rel="Stylesheet" href="/springdemoweb3/resources/styles/default.css" />
	<style>
	a { text-decoration: none }
	</style>
</head>
<body>

	<div id="pageContainer">
	
	
		<jsp:include page="/WEB-INF/views/modules/header.jsp" />
		
		<div style="padding-top:25px;text-align:center">
		
			[ <a href="write">게시글 쓰기</a> ]
			<br /><br />
			
			<table border="1" style="width:600px;margin:0 auto">
				<tr style="background-color:orange;height:30px">
					<th style="width:100px">번호</th>
					<th style="width:300px">제목</th>
					<th style="width:125px">작성자</th>
					<th style="width:50px">조회수</th>
					<th style="width:125px">작성일</th>
					<th style="width:125px">수정일</th>
				</tr>
				<%-- 게시물 목록 표시 : 위의 <tr>과 <th>를 참고해서 구현, 반복문 사용 필요 --%>
				<c:forEach var="board" items="${ boards }">
				<tr style="height:30px">
					<td>${ board.boardNo }</td>
					<td style="text-align:left;padding-left:5px">
						<a href="detail?boardNo=${ board.boardNo }">${ board.title }</a>
					</td>
					<td style="text-align:left;padding-left:5px">${ board.writer }</td>
					<td>${ board.readCount }</td>
					<td>${ board.writeDate }</td>
					<td>${ board.modifyDate }</td>
				</tr>
				</c:forEach>				
			</table>
									
			<br /><br /><br /><br />
		
		</div>
		
	</div>
		

</body>
</html>











