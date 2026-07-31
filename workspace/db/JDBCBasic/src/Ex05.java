import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Ex05 {

    private static final String URL      = "jdbc:mariadb://localhost:3306/shopdb";
    private static final String USER     = "human";
    private static final String PASSWORD = "human";

    public static void main(String[] args) {
    	    	
    	// try(연결생성) : 예외 처리 종료 후 자동으로 연결 닫기 -> 연결 닫으면 관련 리소스도 자동으로 종료
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
        	
            // 3. SQL 작성        	
        	String sql = "INSERT INTO category (category_name) VALUES (?)";
            
            // 4. 명령 객체 생성
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, "아웃도어");

            // 5. 명령 실행 + 결과가 있으면 결과 처리 (Select Query인 경우)
            // ResultSet rs = pstmt.executeQuery(); // only select sql
            int count = pstmt.executeUpdate(); // select가 아닌 모든 sql -> 반환 값은 실행된 SQL을 통해 변경된 행의 갯수
            System.out.println("삽입된 행 수: " + count);

        } catch (SQLException e) {
            System.err.println("DB 연결 실패: " + e.getMessage());
            e.printStackTrace();
        } 
    }

}
