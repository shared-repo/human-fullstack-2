package com.demoweb.service;

import java.util.List;

import com.demoweb.dto.BoardDto;

public interface BoardService {
	
	void writeBoard(BoardDto board);
	
	List<BoardDto> findAllBaord();

}
