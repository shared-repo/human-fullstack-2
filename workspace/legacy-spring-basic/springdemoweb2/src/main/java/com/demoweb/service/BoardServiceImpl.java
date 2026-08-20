package com.demoweb.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.demoweb.dao.BoardDao;
import com.demoweb.dto.BoardDto;

import lombok.Setter;

@Service("boardService")
public class BoardServiceImpl implements BoardService {
	
	@Setter(onMethod_ = { @Autowired } ) // @Setter와 @Autowired를 이용해서 의존 주입
	private BoardDao boardDao;

	@Override
	public void writeBoard(BoardDto board) {
		
		boardDao.insertBoard(board);
		
	}

	@Override
	public List<BoardDto> findAllBaord() {
		List<BoardDto> boards = boardDao.selectAllBoard();
		return boards;
	}

	@Override
	public BoardDto findBoardByBoardNo(int boardNo) {
		BoardDto board = boardDao.selectBoardByBoardNo(boardNo);
		return board;
	}

}
