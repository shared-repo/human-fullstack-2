package com.demoweb.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.mariadb.jdbc.Driver;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.demoweb.dto.BoardDto;
import com.demoweb.dto.MemberDto;

import lombok.Setter;

@Repository("boardDao")
public class BoardDaoImpl implements BoardDao {
	
	@Setter(onMethod_ = { @Autowired })
	private DataSource dataSource;
	
	private SqlSessionTemplate sqlSessionTemplate;
	public BoardDaoImpl(SqlSessionTemplate sqlSessionTemplate) {
		this.sqlSessionTemplate = sqlSessionTemplate;
	}
	
	private final String BOARD_MAPPER = "com.demoweb.mapper.BoardMapper";	

	@Override
	public void insertBoard(BoardDto board) {
		System.out.println("----------------------> before : " + board.getBoardNo()); // ? -> 0
		sqlSessionTemplate.insert(BOARD_MAPPER + ".insertBoard", board);
		System.out.println("----------------------> after : " + board.getBoardNo()); // ? -> 새로 생성된 글번호
	}
	
	@Override
	public List<BoardDto> selectAllBoard() {		
		List<BoardDto> boards = sqlSessionTemplate.selectList(BOARD_MAPPER + ".selectAllBoard");
		return boards;
	}

	@Override
	public BoardDto selectBoardByBoardNo(int boardNo) {		
		BoardDto board = sqlSessionTemplate.selectOne(BOARD_MAPPER + ".selectBoardByBoardNo", boardNo);
		return board;
	}

}














