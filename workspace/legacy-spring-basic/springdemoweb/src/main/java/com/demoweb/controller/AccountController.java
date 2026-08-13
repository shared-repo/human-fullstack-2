package com.demoweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.demoweb.dto.MemberDto;
import com.demoweb.service.MemberService;
import com.demoweb.service.MemberServiceImpl;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping(path = { "/account" })
public class AccountController {
	
	private MemberService memberService;
	public AccountController(MemberService memberService) {
		this.memberService = memberService;
	}

	@GetMapping(path = { "/login" })
	public String showLoginForm() {
		
		return "account/login";
	}
	
	@PostMapping(path = { "/login" })
	public String login(MemberDto member, HttpSession session, Model model) {		
		MemberDto selectedMember = memberService.login(member.getMemberId(), member.getPasswd());
		if (selectedMember != null) {
			session.setAttribute("loginuser", selectedMember);
			return "redirect:/home";
		} else {
			model.addAttribute("memberId", member.getMemberId()); // JSP에서 로그인 실패 메시지 출력을 결정하는 데이터
			return "account/login";
		}		
	}
	
	@GetMapping(path = { "/register" })
	public String showRegisterForm() {		
		return "account/register";
	}
	
	@PostMapping(path = { "/register" })
	public String register(MemberDto member) {
		memberService.registerMember(member);
		return "redirect:login";
	}
	
	@GetMapping(path = { "/logout" })
	public String logout(HttpSession session) {
		// session.removeAttribute("loginuser");		
		session.invalidate();
		return "redirect:/home";
	}

}
