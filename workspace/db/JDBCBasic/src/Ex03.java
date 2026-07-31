import java.beans.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mariadb.jdbc.Driver;

public class Ex03 {

    private static final String URL      = "jdbc:mariadb://localhost:3306/shopdb";
    private static final String USER     = "human";
    private static final String PASSWORD = "human";

    public static void main(String[] args) {
    	    	
    	// try(연결생성) : 예외 처리 종료 후 자동으로 연결 닫기 -> 연결 닫으면 관련 리소스도 자동으로 종료
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
        	
            // 3. SQL 작성
        	int    categoryId = 1;
        	int    maxPrice   = 50000;
        	String sql = "SELECT product_id, product_name, price "
                    // + "FROM product WHERE category_id = " + categoryId + " AND price <= " + maxPrice;
        			+ "FROM product WHERE category_id = ? AND price <= ?";
            
            // 4. 명령 객체 생성
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, categoryId); // SQL의 1번째 ?에 저장할 값
            pstmt.setInt(2, maxPrice); // SQL의 2번째 ?에 저장할 값

            // 5. 명령 실행 + 결과가 있으면 결과 처리 (Select Query인 경우)            
            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.printf("%-10s %-30s %10s%n", "상품ID", "상품명", "가격");
                System.out.println("-".repeat(62));

                while (rs.next()) {
                    System.out.printf("%-10d %-30s %,10d원%n",
                        rs.getInt("product_id"),
                        rs.getString("product_name"),
                        rs.getInt("price"));
                }
            }

        } catch (SQLException e) {
            System.err.println("DB 연결 실패: " + e.getMessage());
            e.printStackTrace();
        } 
    }

}
