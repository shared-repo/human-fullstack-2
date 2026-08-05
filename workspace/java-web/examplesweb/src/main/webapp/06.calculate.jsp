<%@ page language="java" 
		 contentType="text/html; charset=UTF-8"
    	 pageEncoding="UTF-8"%>
    	 
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>계산기</title>
</head>
<body>

	<h2>계산기</h2>
	<form action="/examplesweb/calculate" method="post">
		첫 번째 숫자 : <input type="text" name="operand1"><br>
		연산자 : <input type="text" name="op"><br>
		두 번째 숫자 : <input type="text" name="operand2"><br>
		<input type="submit" value="계산">
	</form>

</body>
</html>