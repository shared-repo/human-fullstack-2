// src/main/java/com/example/imageboard/controller/MemberController.java
package com.example.imageboard.controller;

import com.example.imageboard.dto.MemberCreateRequest;
import com.example.imageboard.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /** 로그인 폼 */
    @GetMapping("/login")
    public String loginForm(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        return "member/login";
    }

    /** 회원가입 폼 */
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("memberCreateRequest", new MemberCreateRequest());
        return "member/register";
    }

    /** 회원가입 처리 */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute MemberCreateRequest request,
                           BindingResult bindingResult,
                           Model model) {


        // ① Bean Validation 오류 확인
        if (bindingResult.hasErrors()) {
            return "member/register";
        }

        // ② 비밀번호 일치 확인 (도메인 규칙)
        if (!request.isPasswordMatch()) {
            bindingResult.rejectValue("passwordConfirm", "mismatch", "비밀번호가 일치하지 않습니다.");
            return "member/register";
        }

        try {
            memberService.register(request);
        } catch (IllegalArgumentException e) {
            // 아이디 중복
            bindingResult.rejectValue("username", "duplicate", e.getMessage());
            return "member/register";
        }

        return "redirect:/members/login?registered=true";
    }
}
