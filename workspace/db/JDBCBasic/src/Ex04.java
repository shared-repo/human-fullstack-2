import java.beans.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mariadb.jdbc.Driver;

public class Ex04 {

    private static final String URL      = "jdbc:mariadb://localhost:3306/shopdb";
    private static final String USER     = "human";
    private static final String PASSWORD = "human";

    public static void main(String[] args) {
    	    	
    	// try(연결생성) : 예외 처리 종료 후 자동으로 연결 닫기 -> 연결 닫으면 관련 리소스도 자동으로 종료
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
        	
            // 3. SQL 작성        	
        	String sql = "SELECT * FROM product WHERE product_id = ?";
            
            // 4. 명령 객체 생성
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, 3); // SQL의 1번째 ?에 저장할 값            

            // 5. 명령 실행 + 결과가 있으면 결과 처리 (Select Query인 경우)            
            try (ResultSet rs = pstmt.executeQuery()) {
            	if (rs.next()) { // pk를 조건으로 사용하는 조회이므로 결과는 없거나 한 개
                    // 단건이므로 while 대신 if 사용
                    System.out.println("상품명: " + rs.getString("product_name"));
                    System.out.println("가격  : " + rs.getInt("price"));
                } else {
                    System.out.println("해당 상품이 없습니다.");
                }
            }

        } catch (SQLException e) {
            System.err.println("DB 연결 실패: " + e.getMessage());
            e.printStackTrace();
        } 
    }

}
