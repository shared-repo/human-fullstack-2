class TheBase3 {
	private int no;
	String name;
	public void pm() {
		System.out.println("TheBase3.pm()이 호출되었습니다.");
	}
}
class TheDerived3 extends TheBase3 { 	
	String desc;
	
	@Override // @~~ : annotation : 메서드, 클래스 등에 대한 부연 설명 및 구현
	public void pm() {
		System.out.println("TheDerived3.pm()이 호출되었습니다.");
	}
}
public class AppC { 
	public static void main(String[] args) {
		
		// 1. 메서드 재정의
		TheBase3 b1 = new TheBase3();
		b1.pm();		
		TheDerived3 d1 = new TheDerived3();
		d1.pm();
		
		// 2. 참조타입과 인스턴스 타입이 다를 때 재정의된 메서드 호출하면 어떤 메서드가 호출되나요?
		//    메서드 호출 형식은 같지만 참조하고 있는 인스턴스에 따라 다른 기능 수행 -> 다형성
		TheBase3 b2 = new TheDerived3();
		b2.pm(); // 어느 pm이 호출될까요? -> 인스턴스 타입 기준으로 호출
		
		
		System.out.println("End of program !!!");
		
	}
}