package examplesweb.servlet;

import java.io.IOException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { "/dogame" })
public class GameServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	
		// 1. 요청 데이터 읽기
		String userStr = req.getParameter("user");
		int you = Integer.parseInt(userStr);
		
		// 2. 요청 처리
		int com = (int) (Math.random() * 3) + 1; // 1, 2, 3 : 컴퓨터 번호

		String result = "";
		if ((you == 1 && com == 3) || (you == 2 && com == 1) || (you == 3 && com == 2)) {
			result = "이겼습니다.";
		} else if (you == com) {
			result = "비겼습니다.";
		} else {
			result = "졌습니다.";
		}
		
		// 3. JSP에서 사용할 수 있도록 request 객체에 데이터 저장
		req.setAttribute("result", result);
		
		// 4. 응답컨텐츠 생산 ( JSP에서 처리하도록 forward )
		RequestDispatcher rd = req.getRequestDispatcher("05.game-result.jsp"); // 이동기 만들기
		rd.forward(req, resp); // forward 이동
		
	}
	
}










