package com.demoweb.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.demoweb.dto.BoardAttachDto;
import com.demoweb.dto.BoardDto;

@Mapper
public interface BoardMapper {
	
	void insertBoard(BoardDto board);
	
	List<BoardDto> selectAllBoard();
	List<BoardDto> selectAllBoardWithResultMap();

	BoardDto selectBoardByBoardNo(@Param("boardNo") int boardNo);
	BoardDto selectBoardByBoardNoWithResultMap(@Param("boardNo") int boardNo);
	BoardDto selectBoardByBoardNoWithResultMap2(@Param("boardNo") int boardNo);
	BoardDto selectBoardByBoardNoWithResultMap3(@Param("boardNo") int boardNo);
	
	void insertBoardAttach(BoardAttachDto boardAttach);
	
	List<BoardAttachDto> selectBoardAttachByBoardNo(@Param("boardNo") int boardNo);	

	BoardAttachDto selectBoardAttachByAttachNo(@Param("attachNo") int attachNo);

}
