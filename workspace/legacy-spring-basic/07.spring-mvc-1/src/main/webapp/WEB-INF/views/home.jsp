<%@ page language="java" 
		 contentType="text/html; charset=UTF-8"
    	 pageEncoding="UTF-8"%>
    	 
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>spring example home</title>
</head>
<body>

	<h2>Spring MVC Demo</h2>
	<hr>
	<h3>1. <a href="demo/param?data1=this%20is%20test%20data&data2=100">Process Request Parameter (GET)</a></h3>
	<h3>2. Process Request Parameter (POST)</h3>
	<form action="demo/param" method="post">
		이름 : <input type="text" name="name" value="홍길동" /><br />
		전화 : <input type="text" name="phone" /><br />
		메일 : <input type="text" name="email" /><br />
		나이 : <input type="text" name="age" /><br />
		생일 : <input type="date" name="birthDate" /><br />
		<input type="submit" value="전송" />
	</form>
	<h3>3. <a href="#">Transfer Data from Controller to View (Refer to 1, 2)</a></h3>
	<h3>4. <a href="demo/redirect">Redirect to Another Action</a></h3>
	<h3>5. <a href="demo/forward">Forward to HTML</a></h3>
	<h3>6-1. <a href="static/hello.html">static resource not in resources</a></h3>
	<h3>6-2. <a href="resources/hello.html">static resource in resources</a></h3>
	<h3>7. <a href="passing-view">Passing View</a></h3>
	<h3>8. <a href="demo/pathvar/10">Path Variable</a></h3>
	<h3>9. <a href="demo/custom-view">Custom View</a></h3>
	<h3>10. <a href="#" id="ajax-link">Ajax Request</a></h3>

</body>
</html>