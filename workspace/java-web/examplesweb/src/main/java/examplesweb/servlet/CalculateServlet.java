package examplesweb.servlet;

import java.io.IOException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { "/calculate" })
public class CalculateServlet extends HttpServlet {
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		// 1. 요청 데이터 읽기
		// 2. 요청 처리
		// 3. JSP에서 읽을 수 있도록 데이터 전달 (request에 저장)
		// 4. 응답 컨텐츠 생산 (JSP로 forward 이동)
		RequestDispatcher rd = req.getRequestDispatcher("06.calculate.jsp"); // 이동기 만들기
		rd.forward(req, resp); // forward 이동
		
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// 1. 요청 데이터 읽기
		int operand1 = Integer.parseInt(req.getParameter("operand1"));
		String op = req.getParameter("op");
		int operand2 = Integer.parseInt(req.getParameter("operand2"));
		
		// 2. 요청 처리
		double result = 0; // 초기화 : 변수를 만들면서 특정 값을 저장하는 것
		boolean valid = true;
		switch (op) { 
		case "+":
			result = operand1 + operand2;
			break;
		case "-":
			result = operand1 - operand2;
			break;
		case "*":
			result = operand1 * operand2;
			break;
		case "/":
			result = (double) operand1 / operand2;
			break;
		case "%":
			result = operand1 % operand2; // % : 나눗셈의 나머지를 반환하는 연산
			break;
		default:			
			valid = false;
		}
		// 3. JSP에서 읽을 수 있도록 데이터 전달 (request에 저장)
		req.setAttribute("operation", String.format("%d %s %d", operand1, op, operand2));
		if (valid) {
			req.setAttribute("result", result);
		} else {
			req.setAttribute("result", "유효하지 않은 연산자");
		}
		// 4. 응답 컨텐츠 생산 (JSP로 forward 이동)
		RequestDispatcher rd = req.getRequestDispatcher("06.calculate-result.jsp"); // 이동기 만들기
		rd.forward(req, resp); // forward 이동
	}

}













