package com.demoweb.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.mariadb.jdbc.Driver;
import org.springframework.stereotype.Repository;

import com.demoweb.dto.BoardDto;
import com.demoweb.dto.MemberDto;

@Repository("boardDao")
public class BoardDaoImpl implements BoardDao {

	private final String URL = "jdbc:mariadb://localhost:3306/demoweb";
	private final String USER = "human", PASSWORD = "human";
	
	@Override
	public void insertBoard(BoardDto board) {
		String sql =  "insert into board (title, writer, content) values (?, ?, ?)";

		try {
			DriverManager.registerDriver(new Driver());
			try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
					 PreparedStatement pstmt = conn.prepareStatement(sql)) {
	
					pstmt.setString(1, board.getTitle());
					pstmt.setString(2, board.getWriter());
					pstmt.setString(3, board.getContent());
	
					pstmt.executeUpdate();			
	
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (Exception ex) {}
		
	}

	
	@Override
	public List<BoardDto> selectAllBoard() {
		String sql =  "SELECT boardno, title, writer, writedate, modifydate, readcount, deleted "
					+ "FROM board "
					+ "ORDER BY boardno desc";

		ArrayList<BoardDto> boards = new ArrayList<>();
	
		try {
			DriverManager.registerDriver(new Driver());
	
			try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
	             PreparedStatement pstmt = conn.prepareStatement(sql)) {
		
				try (ResultSet rs = pstmt.executeQuery()) {
	
					while (rs.next()) {
	                   BoardDto board = new BoardDto();
	
	                   board.setBoardNo(rs.getInt("boardno"));
	                   board.setTitle(rs.getString("title"));
	                   board.setWriter(rs.getString("writer"));
	                   board.setWriteDate(rs.getTimestamp("writedate"));
	                   board.setModifyDate(rs.getTimestamp("modifydate"));
	                   board.setReadCount(rs.getInt("readcount"));
	                   board.setDeleted(rs.getBoolean("deleted"));
	                   
	                   boards.add(board);
	               }
	
	           }
	
	       } catch (SQLException e) {
	           e.printStackTrace();
	       }
	
	   } catch (Exception ex) {
	   }
	
	   return boards;
	}


	@Override
	public BoardDto selectBoardByBoardNo(int boardNo) {
		String sql =  "SELECT boardno, title, writer, content, writedate, modifydate, readcount "
				+ "FROM board "
				+ "WHERE boardNo = ? AND deleted = FALSE ";
		BoardDto board = null;	
		try {
			DriverManager.registerDriver(new Driver());	
			try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
	            PreparedStatement pstmt = conn.prepareStatement(sql)) {
				pstmt.setInt(1, boardNo);
				try (ResultSet rs = pstmt.executeQuery()) {	
					if (rs.next()) {
	                   board = new BoardDto();	
	                   board.setBoardNo(rs.getInt("boardno"));
	                   board.setTitle(rs.getString("title"));
	                   board.setWriter(rs.getString("writer"));
	                   board.setContent(rs.getString("content"));
	                   board.setWriteDate(rs.getTimestamp("writedate"));
	                   board.setModifyDate(rs.getTimestamp("modifydate"));
	                   board.setReadCount(rs.getInt("readcount"));
	               }	
	           }	
	       } catch (SQLException e) {
	           e.printStackTrace();
	       }	
	   } catch (Exception ex) {
	   }	
	   return board;
	}

}














