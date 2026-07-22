
class TheBase2 {
	private int no;
	String name;
	public void pm() {
		System.out.println("TheBase2.pm()이 호출되었습니다.");
	}
}
class TheDerived2 extends TheBase2 { 	
	String desc;
	public void cm() {
		System.out.println("TheDerived2.cm()이 호출되었습니다.");
	}
}
public class AppB { 
	public static void main(String[] args) {
		
		// 1. 상속 관계의 클래스는 참조 타입과 인스턴스 타입이 다를 수 있습니다. ( 형변환 가능 )
		
		// 1-1. 묵시적 형변환 ( TheDerived2 -> TheBase2 )
		TheBase2 b1 = new TheDerived2();
		
		// 1-2. 명시적 형변환 1 ( TheBase2 -> TheDerived2 )
		// TheDerived2 d1 = (TheDerived2)new TheBase2(); // 실행할 때 오류 발생
		
		// 1-2. 명시적 형변환 2
		TheDerived2 d2 = (TheDerived2)b1; // 실행할 때 오류 발생 X
		
		// 2. instanceof : 형변환 가능 여부 확인 (안전한 형변환)
		
		TheBase2 b3 = new TheBase2();
		TheBase2 b4 = new TheDerived2();
		
		if (b3 instanceof TheDerived2) {
			System.out.println("b3은 TheDerived2로 형변환 가능");
		}
		
		if (b4 instanceof TheDerived2) {
			System.out.println("b4는 TheDerived2로 형변환 가능");
		}
		
		
		System.out.println("End of program !!!");
		
	}
}