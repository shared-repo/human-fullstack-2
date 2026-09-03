// src/main/java/com/example/imageboard/controller/GlobalExceptionHandler.java
package com.example.imageboard.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

@ControllerAdvice // @Controller로 지정된 bean에서 발생한 오류를 처리하는 예외 처리기 등록
public class GlobalExceptionHandler {

    /**
     * 파일 크기 초과
     * Referer를 확인해 작성 폼과 수정 폼 양쪽에서 발생한 오류를 적절히 처리합니다.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(MaxUploadSizeExceededException e,
                                      HttpServletRequest request,
                                      RedirectAttributes redirectAttributes) {
        System.out.println("----------------------------> " + e.getMessage());

        // addFlashAttribute : redirect로 이동하지만 데이터를 공유할 수 있도록 저장 (이동 후 데이터 소멸)
        redirectAttributes.addFlashAttribute("errorMessage",
                "파일 크기가 너무 큽니다. 파일당 최대 10MB까지 업로드할 수 있습니다.");
        return resolveRedirect(request);
    }

    /** 허용되지 않는 파일 형식 등 잘못된 인자 */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException e,
                                        HttpServletRequest request,
                                        RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return resolveRedirect(request);
    }

    /**
     * Referer URL을 기반으로 오류 발생 이전 페이지로 돌아갑니다.
     * - 수정 폼(/boards/{id}/edit)에서 발생한 오류 → 수정 폼으로 리다이렉트
     * - 그 외(작성 폼 등) → 작성 폼으로 리다이렉트
     */
    private String resolveRedirect(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        //if (referer != null && referer.contains("/edit")) {
        if (referer != null) {
            // Referer에서 /boards/{id}/edit 경로를 추출해 돌아감
            String path = URI.create(referer).getPath();
            return "redirect:" + path;
        }
        return "redirect:/boards/create";
    }
}
