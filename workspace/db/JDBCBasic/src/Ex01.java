import java.beans.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mariadb.jdbc.Driver;

public class Ex01 {

    private static final String URL      = "jdbc:mariadb://localhost:3306/shopdb";
    private static final String USER     = "human";
    private static final String PASSWORD = "human";

    public static void main(String[] args) {
    	
    	Connection conn = null;
    	PreparedStatement pstmt = null;
    	ResultSet rs = null;
        try {        	
        	// 1. 드라이버 준비 ( 생략 가능 )
        	// DriverManager.registerDriver(new Driver());
        	
        	// 2. 데이터베이스에 연결하고 연결 객체 만들기
        	conn = DriverManager.getConnection(URL, USER, PASSWORD);
        	
        	// 2-1. 연결 테스트
            System.out.println("DB 연결 성공!");
            System.out.println("연결 정보: " + conn.getMetaData().getURL());
            
            // 3. SQL 작성
            String sql = "SELECT * FROM category";
            
            // 4. 명령 객체 생성 (Statement or PreparedStatement)
            // Statement stmt = conn.createStatement();
            pstmt = conn.prepareStatement(sql);

            // 5. 명령 실행 + 결과가 있으면 (Select Query인 경우) 변수에 저장
            rs = pstmt.executeQuery();
            
            // 6. 결과가 있으면 결과 처리 (Select Query인 경우)
            System.out.printf("%5s %-20s\n", "ID", "카테고리명");
            System.out.println("-".repeat(26));

            while (rs.next()) { // 다음 행으로 이동 - 더이상 데이터가 없으면 false 반환 
//                int    id   = rs.getInt("category_id");		// 컬럼 이름으로 읽기
//                String name = rs.getString("category_name");
            	int    id   = rs.getInt(1);		// 컬럼 순서 번호로 읽기
                String name = rs.getString(2);
                System.out.printf("%5d %-20s\n", id, name);
            }

        } catch (SQLException e) {
            System.err.println("DB 연결 실패: " + e.getMessage());
            e.printStackTrace();
        } finally {
        	// 7. 연결 종료
        	try { rs.close(); } catch (Exception ex) {}
        	try { pstmt.close(); } catch (Exception ex) {}
        	try { conn.close(); } catch (Exception ex) {}
        }
    }

}
