package com.demoweb.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.View;

import com.demoweb.common.Util;
import com.demoweb.dto.BoardAttachDto;
import com.demoweb.dto.BoardDto;
import com.demoweb.service.BoardService;
import com.demoweb.view.DownloadView;
import com.demoweb.view.DownloadView2;

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
		
		return "board/write";
	}
	
	@PostMapping(path = { "/write" })
	public String write(
			BoardDto board,
			@RequestParam(value = "attach", required = false) MultipartFile attach, // <input type="file" name="attach" 데이터 수신
			MultipartHttpServletRequest req, // HttpServletRequest + 파일업로드처리
			HttpSession session) {
				
//		MultipartFile attach2 = req.getFile("attach");
//		if (attach2 != null && !attach2.isEmpty()) {
//			System.out.println("------------------> 2. " + attach2.getOriginalFilename());
//		}
		
		ArrayList<BoardAttachDto> list = new ArrayList<>(); // 첨부 파일 정보를 저장할 변수
		if (attach != null && !attach.isEmpty()) {
			System.out.println("------------------> 1. " + attach.getOriginalFilename());
			try {
				// 첨부 파일을 디스크에 저장 (여기서는 board-attach 폴더에 저장)
				String dir = req.getServletContext().getRealPath("/board-attach"); // 웹 경로 -> 컴퓨터 경로
				String userFileName = attach.getOriginalFilename();
				String savedFileName = Util.makeUniqueFileName(userFileName); // 파일 이름 충돌 방지를 위한 고유한 일므			
				attach.transferTo(new File(dir, savedFileName)); // 파일 저장
				
				// 첨부 파일 정보를 DTO 객체에 저장 ( 데이터베이스에 저장하기 위해 )
				BoardAttachDto dto = new BoardAttachDto();
				dto.setSavedFileName(savedFileName);
				dto.setUserFileName(userFileName);				
				list.add(dto);
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		board.setBoardAttachList(list);
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
	
	@GetMapping(path = { "/download/{attachNo}" })
	public View download(
			@PathVariable("attachNo") int attachNo,
			Model model) {
		// 1. 데이터 조회
		BoardAttachDto dto = boardService.findBoardAttachByAttachNo(attachNo);
		
		// 2. View에서 사용할 수 있도록 Model 타입 전달인자에 데이터 저장
		model.addAttribute("attach", dto);
		
		// 3. 다운로드 처리
		// DownloadView view = new DownloadView();
		DownloadView2 view = new DownloadView2();
		return view;
	}

}











