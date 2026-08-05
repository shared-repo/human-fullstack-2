<%@ page language="java" 
		 contentType="text/html; charset=utf-8" 
		 pageEncoding="utf-8"%>

<!DOCTYPE html>

<html>
<head>
	<meta charset='utf-8' />
	<title>Register</title>
	<link rel='Stylesheet' href='/demoweb/styles/default.css' />
	<link rel='Stylesheet' href='/demoweb/styles/input.css' />
</head>
<body>

	<div id='pageContainer'>
		
		<jsp:include page="/WEB-INF/views/modules/header.jsp"></jsp:include>
				
		<div id="inputcontent">
			<br /><br />
		    <div id="inputmain">
		        <div class="inputsubtitle">회원기본정보</div>
		        <!-- <form id="registerform" action="/demoweb/account/register" method="post"> --><!-- 절대경로표시 -->
		        <form id="registerform" action="register" method="post"><!-- 상대경로표시 -->
		        <table>
		            <tr>
		                <th>아이디(ID)</th>
		                <td>
		                    <input type="text" id="memberId" name="memberId" style="width:180px" />
		                    <button id="btn-dup-check" style="width:90px">중복검사</button>
		                </td>
		            </tr>
		            <tr>
		                <th>비밀번호</th>
		                <td>
		                	<input type="password" id="passwd" name="passwd" style="width:280px" />
		                </td>
		            </tr>
		            <tr>
		                <th>비밀번호 확인</th>
		                <td>
		                    <input type="password" id="confirm" name="confirm" style="width:280px" />
		                </td>
		            </tr>
		            <tr>
		                <th>이메일</th>
		                <td>
		                	<input type="text" id="email" name="email" style="width:280px" />
		                </td>
		            </tr>
		                       		            
		        </table>
		        <div class="buttons">
		        	<input id="register" type="button" value="등록" style="height:25px" />
		        	<input id="cancel" type="button" value="취소" style="height:25px"  />
		        </div>
		        </form>
		    </div>
		</div>   	
	</div>

	<script src="http://code.jquery.com/jquery-3.7.1.js"></script>
	<script type="text/javascript">
	$(function() { // 이 함수 안에서 작성되는 코드는 브라우저가 현재 HTML을 모두 처리한 후 실행됩니다.
		
		$('#register').on('click', function(event) {			
			// 여기에 유효성 검사 코드를 작성합니다.			
			$('#registerform').submit(); // 서버로 전송
		});
		$('#cancel').on('click', function(event) {
			location.href = "/demoweb/home";
		});
		
	});
	
	</script>

</body>
</html>




























