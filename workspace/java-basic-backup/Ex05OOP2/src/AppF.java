
interface TheInterface {
	// 1. 모든 메서드는 추상메서드 : public abstract 형식 ( 작성하지 않으면 자동 설정 )
	// void m1() {} // 오류 : 일반 메서드 추가 안됨
	void m2(); // 자동으로 public abstract void m2();
	
	// 2. 모든 필드는 public static final ( 작성하지 않으면 자동 설정 )
	int v1 = 100; // 자동으로 public static final int v1 = 100;
}
// 5. 인터페이스간에 상속 가능
interface TheInterface2 extends TheInterface {
	void m3();
}
// 4-1. 인터페이스는 다른 클래스가 구현해서 사용
//      반드시 구현하는 인터페이스의 추상메서드 재정의
class TheImplement implements TheInterface {
	@Override
	public void m2() {
	}
}

public class AppF {

	public static void main(String[] args) {
		
		// 3. 인터페이스의 인스턴스 만들 수 없음 ( new 사용 불가능 )
		// TheInterface i1 = new TheInterface(); // 오류
		
		// 4-1.
		TheInterface i2 = new TheImplement();
		
		// 4-2. 인터페이스 사용 : 익명 내부 클래스
		//      TheInterface를 구현하는 이름 없는 클래스 만들기 + 그 클래스의 인스턴스 만들기
		TheInterface i3 = new TheInterface() {
			@Override
			public void m2() {
				System.out.println("이름 없는 클래스의 m2() 호출");
			}
		};
		i3.m2();

	}

}