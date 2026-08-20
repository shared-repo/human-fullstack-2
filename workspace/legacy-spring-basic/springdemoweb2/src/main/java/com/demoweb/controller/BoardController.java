package com.demoweb.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.demoweb.dto.BoardDto;
import com.demoweb.service.BoardService;

import jakarta.servlet.http.HttpSession;
import lombok.Setter;

@Controller
@RequestMapping(path = { "/board" })
public class BoardController {
	
	@Setter(onMethod_ = { @Autowired })
	private BoardService boardService;
	
	@GetMapping(path = { "/list" })
	public String list(Model model) {
		
		List<BoardDto> boards = boardService.findAllBaord();
		model.addAttribute("boards", boards);
		
		return "board/list";
	}
	
	@GetMapping(path = { "/write" })
	public String writeForm(HttpSession session) {
		
		if (session.getAttribute("loginuser") == null) { // 로그인하지 않은 경우
			return "redirect:/account/login";
		}
		
		return "board/write";
	}
	
	@PostMapping(path = { "/write" })
	public String write(BoardDto board, HttpSession session) {
		
		if (session.getAttribute("loginuser") == null) { // 로그인하지 않은 경우
			return "redirect:/account/login";
		}
		
		boardService.writeBoard(board);
		
		return "redirect:list";
	}
	
	@GetMapping(path = { "/detail" })
	public String detail(
			@RequestParam(value="boardNo", defaultValue = "-1")int boardNo,
			Model model) {
		
		System.out.println("----------------------> " + boardNo);
		
		if (boardNo == -1) {
			return "redirect:list";
		}		
		
		BoardDto board = boardService.findBoardByBoardNo(boardNo);
		if (board != null) {			
			model.addAttribute("board", board);			
			return "board/detail";
		} else {
			return "redirect:list";
		}
	}

}











