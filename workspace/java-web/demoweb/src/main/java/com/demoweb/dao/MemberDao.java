package com.demoweb.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mariadb.jdbc.Driver;

import com.demoweb.dto.MemberDto;

public class MemberDao {
	
	private final String URL = "jdbc:mariadb://localhost:3306/demoweb";
	private final String USER = "human", PASSWORD = "human";

	public void insertMember(MemberDto member) {
		
		String sql =  "insert into tbl_member (memberid, passwd, email) "
					+ "values (?, ?, ?) ";

		try {
			DriverManager.registerDriver(new Driver());
			try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
					 PreparedStatement pstmt = conn.prepareStatement(sql)) {

					pstmt.setString(1, member.getMemberId());
					pstmt.setString(2, member.getPasswd());
					pstmt.setString(3, member.getEmail());

					pstmt.executeUpdate();			

			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (Exception ex) {}
		
		
	}
	
	public MemberDto selectByMemberId(String memberId) {

	    String sql = "SELECT memberid, passwd, email, usertype, regdate, active " +
	                 "FROM tbl_member " +
	                 "WHERE memberid = ? AND active = TRUE";

	    MemberDto member = null;

	    try {
	        DriverManager.registerDriver(new Driver());

	        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
	             PreparedStatement pstmt = conn.prepareStatement(sql)) {

	            pstmt.setString(1, memberId);

	            try (ResultSet rs = pstmt.executeQuery()) {

	                if (rs.next()) {
	                    member = new MemberDto();

	                    member.setMemberId(rs.getString("memberid"));
	                    member.setPasswd(rs.getString("passwd"));
	                    member.setEmail(rs.getString("email"));
	                    member.setUserType(rs.getString("usertype"));
	                    member.setRegDate(rs.getTimestamp("regdate"));
	                    member.setActive(rs.getBoolean("active"));
	                }

	            }

	        } catch (SQLException e) {
	            e.printStackTrace();
	        }

	    } catch (Exception ex) {
	    }

	    return member;
	}
	

}
