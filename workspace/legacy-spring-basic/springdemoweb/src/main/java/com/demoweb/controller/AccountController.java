package com.demoweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.demoweb.dto.MemberDto;
import com.demoweb.service.MemberService;

@Controller
@RequestMapping(path = { "/account" })
public class AccountController {
	
	private MemberService memberService = new MemberService();
	
	@GetMapping(path = { "/login" })
	public String showLoginForm() {
		
		return "account/login";
	}
	
	@PostMapping(path = { "/login" })
	public String login(MemberDto member) {
		System.out.println("-----------------> " + member);
		MemberDto selectedMember = memberService.login(member.getMemberId(), member.getPasswd());
		System.out.println("-----------------> " + selectedMember);
		return "redirect:/home";
	}
	
	@GetMapping(path = { "/register" })
	public String showRegisterForm() {
		
		return "account/register";
	}
	
	@PostMapping(path = { "/register" })
	public String register(MemberDto member) {
		System.out.println("-----------------> " + member);
		memberService.registerMember(member);
		return "redirect:login";
	}

}
