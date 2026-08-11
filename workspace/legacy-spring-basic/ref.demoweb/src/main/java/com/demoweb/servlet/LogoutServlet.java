package com.demoweb.servlet;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/account/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);        

        if (session != null && session.getAttribute("loginuser") != null) {
        	// session.removeAttribute("loginuser"); // session에 포함된 특정 데이터만 삭제
            session.invalidate(); // session 자체를 삭제 ( session에 포함된 모든 데이터 삭제 )
        }

        // request.getContextPath() : 웹애플리케이션 이름
        System.out.println("----------------------> " + request.getContextPath());
        response.sendRedirect(request.getContextPath() + "/home");
    }

}









