package com.example.imageboard.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

// spring은 오류가 발생하면 "/error" 요청 발생 -> BasicErrorController가 이 요청을 처리
// 변경하고 싶으면 ErrorController를 구현한 Controller를 만들고 "/error" 요청에 대한 처리기 구현

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        // Retrieve the exception propagated from the Servlet filter chain
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        // Check if the error is a file size or 413 Payload Too Large error
        if ((statusCode != null && statusCode == 413) ||
                (exception != null && exception.toString().contains("MaxUploadSizeExceededException"))) {

            redirectAttributes.addFlashAttribute("errorMessage",
                    "파일 크기가 너무 큽니다. 허용된 최대 파일 크기를 초과했습니다.");

            String referer = request.getHeader("Referer");
            if (referer != null) {
                String path = URI.create(referer).getPath();
                return "redirect:" + path;
            }
            return "redirect:/boards/create";
        }

        // Return a default error page template (e.g., error/error.html) for other errors
        return "error/error";
    }
}