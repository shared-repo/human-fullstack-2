import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

/*
	create table bank_account
	(
		owner varchar(100),
	    balance int
	);
	
	insert into bank_account values('철수', 1000);
	insert into bank_account values('영희', 1000);
	
	select * from bank_account;
	
	update bank_account set balance = 1000;
*/

public class TransactionTest {

	public static void main(String[] args) {
	
		final String URL = "jdbc:mariadb://localhost:3306/labdb";
		final String USER = "human", PASSWORD = "human";
		
		try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
			
			conn.setAutoCommit(false); // executeUpdate 호출할 때 commit 수행 금지 -> 수동으로 commit or rollback
			
			String sql = 
					"UPDATE bank_account " +
					"SET balance = balance + ? " +
					"WHERE owner = ?"; 
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {				
			
				pstmt.setInt(1, -500);
				pstmt.setString(2, "철수");
				pstmt.executeUpdate();	// 기본 설정 : 실행 즉시 commit
				
				int x = 10 / 0; // 오류 발생
				
				pstmt.setInt(1, 500);
				pstmt.setString(2, "영희");
				pstmt.executeUpdate();
				
				conn.commit(); // 마지막 commit or rollback 이후의 모든 변경 사항을 확정
				System.out.println("계좌 이체 완료");
			} catch (Exception ex) {
				conn.rollback(); // 마지막 commit or rollback 이후의 모든 변경 사항을 취소
				System.out.println("계좌 이체 실패");				
			}
			
			conn.setAutoCommit(true); // 연결의 auto-commit 설정을 복원
			
		} catch (Exception ex) {			
			System.out.println("계좌 이체 실패");
			
		} 
	}

}