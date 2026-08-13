package com.demoweb.dao;

import java.util.List;

import com.demoweb.dto.BoardDto;

public interface BoardDao {
	
	void insertBoard(BoardDto board);
	
	List<BoardDto> selectAllBoard();
	

}
