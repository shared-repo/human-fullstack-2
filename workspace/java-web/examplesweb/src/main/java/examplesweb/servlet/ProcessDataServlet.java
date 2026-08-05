package examplesweb.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { "/process-data" })
public class ProcessDataServlet extends HttpServlet {
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	
		// request.getParameter : 브라우저에서 전송한 요청 데이터 읽기
		String name = request.getParameter("name");		// name : 브라우저의 입력요소 이름
		String email = request.getParameter("email");	// email : 브라우저의 입력요소 이름
		
		System.out.printf("%s / %s\n", name, email);
		
		// 응답 컨텐츠의 종류와 문자셋 설정
		response.setContentType("text/html;charset=utf-8");
		
		PrintWriter out = response.getWriter();	// network stream을 대상으로 하는 IO 객체
		out.println("<!DOCTYPE html>");
		out.println("<html>");
		out.println("<head>");
		out.println("</head>");
		out.println("<body>");
		out.println("<table width='400' border='1' align='center'>");
		out.println("<tr style='height: 30px'>");
		out.println("<th>Your Name</th>");
		out.println("<td>" + name + "</td>");
		out.println("<tr style='height: 30px'>");
		out.println("<th>Your Email</th>");
		out.println("<td>" + email + "</td>");
		out.println("</table>");

		out.println("</body>");
		out.println("</html>");
		
	}

}
