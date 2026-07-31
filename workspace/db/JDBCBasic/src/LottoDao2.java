import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LottoDao2 {
	
	private final String URL = "jdbc:mariadb://localhost:3306/labdb";
	private final String USER = "human", PASSWORD = "human";

	public void deleteAll() {
		// String sql = "truncate table lotto"; 
		String sql = "delete from lotto"; 

		try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void insertLotto(LottoDto2 dto) {		 
		String sql = "insert into lotto values (?, ?, ?, ?, ?, ?, ?, ?)"; 

		try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
				PreparedStatement pstmt = conn.prepareStatement(sql)) {
			
			pstmt.setInt(1, dto.getRnd());
			pstmt.setInt(2, dto.getNumber1());
			pstmt.setInt(3, dto.getNumber2());
			pstmt.setInt(4, dto.getNumber3());
			pstmt.setInt(5, dto.getNumber4());
			pstmt.setInt(6, dto.getNumber5());
			pstmt.setInt(7, dto.getNumber6());
			pstmt.setInt(8, dto.getBonus());

			pstmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}

	public LottoDto2 selectLottoByRnd(int rnd) {
		LottoDto2 lotto = null;

		String sql = "SELECT * FROM lotto WHERE rnd = ?";

		try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
				PreparedStatement pstmt = conn.prepareStatement(sql);) {

			pstmt.setInt(1, rnd);

			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					lotto = new LottoDto2();
					lotto.setRnd(rnd);
					lotto.setNumber1(rs.getInt("number1"));
					lotto.setNumber2(rs.getInt("number2"));
					lotto.setNumber3(rs.getInt("number3"));
					lotto.setNumber4(rs.getInt("number4"));
					lotto.setNumber5(rs.getInt("number5"));
					lotto.setNumber6(rs.getInt("number6"));
					lotto.setBonus(rs.getInt("bonus"));
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return lotto;
	}

	public void insertLottoBatch(List<LottoDto2> list) {
		String sql = "insert into lotto values (?, ?, ?, ?, ?, ?, ?, ?)"; 

		try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {
			
			conn.setAutoCommit(false);
			
			for (LottoDto2 dto: list) {
				pstmt.setInt(1, dto.getRnd());
				pstmt.setInt(2, dto.getNumber1());
				pstmt.setInt(3, dto.getNumber2());
				pstmt.setInt(4, dto.getNumber3());
				pstmt.setInt(5, dto.getNumber4());
				pstmt.setInt(6, dto.getNumber5());
				pstmt.setInt(7, dto.getNumber6());
				pstmt.setInt(8, dto.getBonus());
	
				pstmt.addBatch(); // 일괄처리를 위해 작업을 모아두는 명령
			}
			
			pstmt.executeBatch(); // addBatch로 모아둔 작업을 일괄 처리
			
			conn.commit();
			conn.setAutoCommit(true);

		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}

}
