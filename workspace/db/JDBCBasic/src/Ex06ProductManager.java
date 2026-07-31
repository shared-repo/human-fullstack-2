import java.util.List;

public class Ex06ProductManager {

	public static void main(String[] args) {
		
		ProductDao dao = new ProductDao();
		
		// 1. 전체 목록 조회
		List<ProductDto> products = dao.selectProductList();
		
		// products 사용하는 코드
		for (ProductDto product : products) {
			System.out.println(product.toString());
		}
		
		System.out.println("=".repeat(30));
		
		// 2. product_id로 검색
		int productId = 100;
		ProductDto product = dao.selectProductById(productId);
		
		if (product == null) {
			System.out.println("해당 제품이 없습니다.");
		} else {
			System.out.println(product);
		}
		
		// 3. 새 제품 등록
//		ProductDto product2 = new ProductDto();
//		product2.setCategoryId(3); // 도서 카테고리
//		product2.setProductName("인공지능 시작하기");
//		product2.setPrice(30000);
//		product2.setStock(30);
//		product2.setDescription("당신의 업무에 인공지능을 적용하는 최고의 지침서");
//		
//		int generatedKey = dao.insertProduct(product2);
//		System.out.printf("%d번 제품 등록 완료", generatedKey);
		
		// 4. 제품 삭제
		int idToDelete = 18;
		dao.deleteProduct(idToDelete);
		System.out.println("삭제 완료");


	}

}












