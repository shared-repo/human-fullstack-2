import com.sun.jdi.Value;

public class MethodTest {
	
	void m1() {
		System.out.println("m1 메서드 호출");
	}
	
	void m2(String s, int i) {
		System.out.println("m2 메서드 호출");
		System.out.println("전달인자 1 : " + s);
		System.out.println("전달인자 2 : " + i);
	}
	
	int m3(int a, int b) {
		System.out.println("m3 메서드 호출");
		System.out.println("전달인자 1 : " + a);
		System.out.println("전달인자 2 : " + b);
		return a + b;
	}
	
	// 메서드 오버로딩 : 이름은 같지만 전달인자의 갯수와 종류(자료형)으로 메서드를 구분하는 기법
	// int sum2(int a, int b) {
	int sum(int a, int b) {
		return a + b;
	}	
	//int sum3(int a, int b, int c) {
	int sum(int a, int b, int c) {
		return a + b + c;
	}
	//int sum4(int a, int b, int c, int d) {
	int sum(int a, int b, int c, int d) {
		return a + b + c + d;
	}
	
	// 가변인자배열 : 모든 전달인자를 배열로 받는 기능 -> 전달인자의 갯수에 영향받지 않음 / 같은 자료형만 사용 가능
	int sum2(int...values) {
		System.out.println(values.getClass());
		int total = 0;
		for( int n: values) {
			total += n;
		}
		return total;
	}

}



















