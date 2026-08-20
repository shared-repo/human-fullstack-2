package com.demoweb.service;

import java.util.List;

import com.demoweb.dto.BoardAttachDto;
import com.demoweb.dto.BoardDto;

public interface BoardService {
	
	void writeBoard(BoardDto board);
	
	List<BoardDto> findAllBaord();

	BoardDto findBoardByBoardNo(int boardNo);
	
	void uploadBoardAttach(BoardAttachDto boardAttach);

	BoardAttachDto findBoardAttachByAttachNo(int attachNo);

}
