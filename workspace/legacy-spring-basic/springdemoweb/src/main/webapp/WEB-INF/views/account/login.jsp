<%@ page language="java" 
		 contentType="text/html; charset=utf-8" 
		 pageEncoding="utf-8"%>
		 
<%@ taglib prefix="c" uri="jakarta.tags.core" %>		 

<!DOCTYPE html>

<html>
<head>
	<meta charset='utf-8' />
	<title>Login</title>
	<link rel='Stylesheet' href='/springdemoweb/resources/styles/default.css' />
	<link rel='Stylesheet' href='/springdemoweb/resources/styles/input.css' />
</head>
<body>

	<div id='pageContainer'>

		<jsp:include page="/WEB-INF/views/modules/header.jsp"></jsp:include>
	
		<div id="inputcontent">
			<br /><br />
		    <div id="inputmain">
		        <div class="inputsubtitle">로그인정보</div>
		        
		        <!-- <form action="/springdemoweb/account/login" method="post"> --> <!-- 절대 경로 -->
		        <form action="login" method="post">
		       
		        <table>
		            <tr>
		                <th>아이디(ID)</th>
		                <td>
		                    <input 	type="text" name="memberId" style="width:280px" 
		                    		value='${ not empty memberId ? memberId : "" }' />
		                </td>
		            </tr>
		            <tr>
		                <th>비밀번호</th>
		                <td>
		                	<input type="password" name="passwd" style="width:280px" />
		                </td>
		            </tr>
		        </table>
		        
		        <div class="buttons">
		        	<input type="submit" value="로그인" style="height:25px" />
		        	<input type="button" id="cancel-btn" value="취소" style="height:25px" />
		        	<input type="button" id="reset-pwd-btn" value="비밀번호초기화" style="height:25px" />
		        </div>
		        </form>
		        
		    </div>
		    
		    <c:if test="${ not empty memberId }">
	    	<div style="color:red;">
		        로그인 실패. 아이디와 패스워드를 확인하세요
		    </div>
		    </c:if>
		    
		</div>  	
	</div>
	
	<script src="http://code.jquery.com/jquery-3.7.1.js"></script>
	<script type="text/javascript">
	
	</script>

</body>
</html>


