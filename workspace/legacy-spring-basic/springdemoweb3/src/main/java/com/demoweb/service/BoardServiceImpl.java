package com.demoweb.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.demoweb.dto.BoardAttachDto;
import com.demoweb.dto.BoardDto;
import com.demoweb.mapper.BoardMapper;

import lombok.Setter;

@Service("boardService")
public class BoardServiceImpl implements BoardService {
	
	private final BoardMapper boardMapper;
	public BoardServiceImpl(BoardMapper boardMapper) {
		this.boardMapper = boardMapper;
	}

	@Override
	public void writeBoard(BoardDto board) {
		System.out.println("---------------------> " + board.getBoardNo()); // --> 0
		boardMapper.insertBoard(board);
		System.out.println("---------------------> " + board.getBoardNo()); // --> 새로 생성된 글 번호
		
		for (BoardAttachDto attach : board.getBoardAttachList()) {
			attach.setBoardNo(board.getBoardNo());
			boardMapper.insertBoardAttach(attach);
		}
	}

	@Override
	public List<BoardDto> findAllBaord() {
		// List<BoardDto> boards = boardMapper.selectAllBoard();
		List<BoardDto> boards = boardMapper.selectAllBoardWithResultMap();
		return boards;
	}

	@Override
	public BoardDto findBoardByBoardNo(int boardNo) {
		// 1. 두 테이블의 데이터를 각각 조회 (직접 결합)
//		BoardDto board = boardMapper.selectBoardByBoardNo(boardNo);
//		List<BoardAttachDto> attachList = boardMapper.selectBoardAttachByBoardNo(boardNo);
//		board.setBoardAttachList(attachList);
		
		// 2. 두 테이블의 데이터를 조인해서 조회 (MyBatis에서 결합, 하나의 resultMap으로 조인)
//		BoardDto board = boardMapper.selectBoardByBoardNoWithResultMap(boardNo);		
		
		// 3. 두 테이블의 데이터를 조인해서 조회 (MyBatis에서 결합, resultMap 분리해서 참조)
		BoardDto board = boardMapper.selectBoardByBoardNoWithResultMap2(boardNo);				
		
		// 4. 두 테이블의 데이터를 각각 조회 (MyBatis에서 결합)
//		BoardDto board = boardMapper.selectBoardByBoardNoWithResultMap3(boardNo);		
		
		return board;
	}

	@Override
	public void uploadBoardAttach(BoardAttachDto boardAttach) {
		boardMapper.insertBoardAttach(boardAttach);
	}

	@Override
	public BoardAttachDto findBoardAttachByAttachNo(int attachNo) {
		BoardAttachDto dto = boardMapper.selectBoardAttachByAttachNo(attachNo);
		return dto;
	}

}
