package examplesweb.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

@WebServlet("/hello2") // web.xml 파일에 서블릿 등록 + URL 매핑
public class HelloServlet2 extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		PrintWriter out = resp.getWriter();	// network stream을 대상으로 하는 IO 객체, 이 객체에 write 하면 브라우저로 전송
		out.println("<!DOCTYPE html>");
		out.println("<html>");
		out.println("<head>");
		out.println("</head>");
		out.println("<body>");
		out.println("<h1>");
		out.println("Hello Servlet 2");
		out.println("</h1>");
		out.println("<h1>");
		out.println(new Date());
		out.println("</h1>");
		out.println("</body>");
		out.println("</html>");
	}

}
