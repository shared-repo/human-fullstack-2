import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

	private final String URL = "jdbc:mariadb://localhost:3306/shopdb";
	private final String USER = "human", PASSWORD = "human";

	// product 목록을 조회해서 반환하는 메서드
	public List<ProductDto> selectProductList() {

		List<ProductDto> products = new ArrayList<>();

		String sql = "SELECT product_id, product_name, category_id, price, stock, ifnull(description, '') description, created_at "
				+ "FROM product ORDER BY product_id ";

		try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
				PreparedStatement pstmt = conn.prepareStatement(sql);
				ResultSet rs = pstmt.executeQuery()) {

			while (rs.next()) {
				ProductDto p = new ProductDto();
				p.setProductId(rs.getInt("product_id"));
				p.setProductName(rs.getString("product_name"));
				p.setCategoryId(rs.getInt("category_id"));
				p.setPrice(rs.getInt("price"));
				p.setStock(rs.getInt("stock"));
				p.setDescription(rs.getString("description"));
				p.setCreatedAt(rs.getDate("created_at"));
				products.add(p);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return products;
	}

	public ProductDto selectProductById(int productId) {

		ProductDto product = null;

		String sql = "SELECT product_id, product_name, category_id, price, stock, ifnull(description, '') description, created_at "
				+ "FROM product WHERE product_id = ? ORDER BY product_id ";

		try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
				PreparedStatement pstmt = conn.prepareStatement(sql);) {

			pstmt.setInt(1, productId);

			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					product = new ProductDto();
					product.setProductId(rs.getInt("product_id"));
					product.setProductName(rs.getString("product_name"));
					product.setCategoryId(rs.getInt("category_id"));
					product.setPrice(rs.getInt("price"));
					product.setStock(rs.getInt("stock"));
					product.setDescription(rs.getString("description"));
					product.setCreatedAt(rs.getDate("created_at"));
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return product;

	}

	public int insertProduct(ProductDto product2) {

		String sql = "insert into product (category_id, product_name, price, stock, description) "
				+ "values (?, ?, ?, ?, ?) ";

		try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
				PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			pstmt.setInt(1, product2.getCategoryId());
			pstmt.setString(2, product2.getProductName());
			pstmt.setInt(3, product2.getPrice());
			pstmt.setInt(4, product2.getStock());
			pstmt.setString(5, product2.getDescription());

			pstmt.executeUpdate();

			// AUTO_INCREMENT로 생성된 PK 값 조회
			try (ResultSet keys = pstmt.getGeneratedKeys()) {
				if (keys.next()) {
					return keys.getInt(1);
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return -1;

	}

	public void deleteProduct(int idToDelete) {
		String sql = "delete from product where product_id = ? ";

		try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, idToDelete);

			pstmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

}
