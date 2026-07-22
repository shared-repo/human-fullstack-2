// class TheAbstract { // 오류 : 추상 메서드를 가진 클래스는 비추상 클래스로 만들 수 없음
abstract class TheAbstract { // abstract 클래스 : 인스턴스를 만들 수 없는 클래스 ( new 할 수 없는 클래스 )

	// 2. 추상메서드 : 본문을 가질 수 없는 메서드
	// abstract void am() {} // 오류 : abstract 메서드는 본문( {...} )을 가질 수 없음
	abstract void am();
}


// 3-1. 추상 클래스는 다른 클래스에서 상속해서 사용
//      추상 클래스를 상속하는 클래스는 반드시 추상 메서드를 재정의해야 합니다.
class TheConcrete extends TheAbstract {
	@Override
	void am() { }	
}

public class AppE {

	public static void main(String[] args) {
		
		// 1. 추상클래스
		// TheAbstract a = new TheAbstract(); // 오류 : 추상클래스는 인스턴스 만들 수 없음
		TheAbstract a; // 추상클래스의 참조 변수는 만들 수 있음
		a = new TheConcrete();
		
		// 3-2. 추상 클래스 사용 : 익명 내부 클래스
		TheAbstract a2 = 
				// TheAbstract를 상속하는 이름 없는 클래스를 만들고 그 클래스의 인스턴스 생성
				new TheAbstract() { 		
					@Override
					void am() {
						System.out.println("익명 내부 클래스의 am 메서드");
					}
				};
		a2.am();

	}

}