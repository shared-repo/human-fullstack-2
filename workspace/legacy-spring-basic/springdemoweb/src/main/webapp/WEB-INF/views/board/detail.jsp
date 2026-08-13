<%@ page language="java" 
		 contentType="text/html; charset=utf-8"
    	 pageEncoding="utf-8"%>

<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
    
<!DOCTYPE html>

<html>
<head>
	<meta charset="utf-8" />
	<title>글상세보기</title>
	<link rel="Stylesheet" href="/springdemoweb/resources/styles/default.css" />
	<link rel="Stylesheet" href="/springdemoweb/resources/styles/input.css" />
</head>
<body>

	<div id="pageContainer">
	
		<jsp:include page="/WEB-INF/views/modules/header.jsp" />
		
		<div style="padding-top:25px;text-align:center">
		<div id="inputcontent">
		    <div id="inputmain">
		        <div class="inputsubtitle">게시글 정보</div>		        
		        <table>
		            <tr>
		                <th>제목</th>
		                <td></td>
		            </tr>
		            <tr>
		                <th>작성자</th>
		                <td></td>
		            </tr>
		            <tr>
		            	<th>조회수</th>
		            	<td></td>
		            </tr>
		            <tr>
		            	<th>작성일자</th>
		            	<td></td>
		            </tr>
		            <tr>
		            	<th>수정일자</th>
		            	<td></td>
		            </tr>
		            <tr>
		                <th>첨부파일</th>
		                <td>		                			                
		                </td>
		            </tr>
		            <tr>
		                <th>글내용</th>
						<td>
						</td>
		            </tr>
		        </table>
		        <div class="buttons">
		        	<input type="button" id="edit_button" value="편집" style="height:25px" />
		        	<input type="button" id="delete_button" value="삭제" style="height:25px" />
		        	<input type="button" id="tolist_button" value="목록보기" style="height:25px" />
		        </div>
		    </div>
		</div>   	

		<br>
	
	</div>
	</div>
	
	<script src="http://code.jquery.com/jquery-3.7.1.js"></script>
	<script>
	
	</script>
	
</body>
</html>