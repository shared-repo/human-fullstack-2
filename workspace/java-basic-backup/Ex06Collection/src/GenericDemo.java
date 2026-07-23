
class TheObject {
	// int x; // 이 변수에는 정수만 저장
	Object v; // 이 변수에는 모든 자료형의 데이터 저장	
}

class TheGeneric<T, E> {
	T v; // 미확정 자료형 T 타입의 변수 선언 ( 이 클래스로 변수를 만들 때 결정 )
	E v2;
}

public class GenericDemo {

	public static void main(String[] args) {
		
		// 1. 
		TheObject obj1 = new TheObject();
		obj1.v = 11; // 다른 자료형 -> Object : 묵시적(암시적) 형변환
		TheObject obj2 = new TheObject();
		obj2.v = "홍길동";
		
		System.out.println( (int)obj1.v + 30 ); // Object -> 다른 자료형 : 명시적 형변환 필요
		// System.out.println( (int)obj2.v + 30 ); // 오류 : 문자열 -> 정수 변환 오류
		
		// 2. 
		TheGeneric<Integer, String> gobj1 = new TheGeneric<>(); // T타입을 Integer로, E타입은 String으로 사용
		gobj1.v = 11;
		TheGeneric<String, Double> gobj2 = new TheGeneric<>(); // T타입을 String으로 E타입은 Double로 사용
		gobj2.v = "장동건";
		
		System.out.println( gobj1.v + 30 ); // generic은 형변환 불필요
		// System.out.println( (int)gobj2.v + 30 ); // 컴파일 오류 : 형변환 문제 상황 자동 검출

	}

}