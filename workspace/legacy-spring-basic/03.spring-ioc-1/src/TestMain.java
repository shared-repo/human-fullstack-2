import java.lang.reflect.Method;

public class TestMain {

	public static void main(String[] args) {

		ServiceConsumerImpl sc = new ServiceConsumerImpl();
		
		sc.doSomething();
		
		///////////////////////////////////////

		// reflection example
//		try {
//			// "java.lang.String" -> String 클래스 정보
//			Class claz = Class.forName("java.lang.String"); // String
//			String s = (String)claz.getDeclaredConstructor().newInstance(); // new String();
//			System.out.println(s.getClass().toString());
//			
//			// 클래스의 모든 메서드를 배열로 가져오기.
//			Method[] methods = claz.getMethods();
//			for (Method method : methods) {
//				System.out.println(method.getName()); // 메서드 이름 출력
//				
//				if (method.getName().equals("length")) {					
//					//invoke() : 지정된 메서드를 실행하는 명령
//					int length = (int)method.invoke(s); //s.length() // 메서드 호출
//					System.out.println(length);
//				}
//			}
//			
//		} catch (Exception ex) {
//			ex.printStackTrace();
//		}
		
	}

}
