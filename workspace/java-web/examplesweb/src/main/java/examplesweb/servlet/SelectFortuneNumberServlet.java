package examplesweb.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Random;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { "/select-fortune-number" })
public class SelectFortuneNumberServlet extends HttpServlet {
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	
		// request.getParameter : 브라우저에서 전송한 요청 데이터 읽기
		String strStart = request.getParameter("start"); // 항상 문자열로 데이터를 읽습니다.
		String strStop = request.getParameter("stop");
		
		System.out.printf("%s / %s\n", strStart, strStop);
		
		int iStart = Integer.parseInt(strStart);
		int iStop = Integer.parseInt(strStop);
		
		// int fortuneNumber = (int)((Math.random() * (iStop - iStart)) + iStart);
		Random r = new Random();
		int fortuneNumber = r.nextInt(iStart, iStop);	
		
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
		out.println("<th>행운 번호</th>");
		out.println("<td>" + fortuneNumber + "</td>");
		out.println("</tr>");
		out.println("</table>");

		out.println("</body>");
		out.println("</html>");
		
	}

}
