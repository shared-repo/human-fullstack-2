
class TheBase {
	private int no;
	String name = "test";
	public void pm() {
		System.out.println("TheBase.pm()이 호출되었습니다.");
	}
	
	TheBase() {
		System.out.println("TheBase의 생성자");
	}
}
class TheDerived1 extends TheBase { // TheBase의 모든 멤버를 자동으로 포함
	// 상속 받은 클래스는 새로운 멤버를 추가하거나 기존 멤버를 변경해야 합니다.
	String desc;
	public void cm() {
		super.name = "John Doe"; // 자식 클래스에서 부모의 멤버 사용 가능 (super는 부모의 멤버를 표시하는 도구)
		System.out.println(name);
		// no = 100; // 오류 : 부모 클래스의 private 멤버는 자식 클래스에서 사용할 수 없습니다.		
		System.out.println("TheDerived1.cm()이 호출되었습니다.");
	}
	
	TheDerived1() {
		super(); // 부모 생성자 호출 : 표현하지 않으면 자동으로 호출
		System.out.println("TheDerived1의 생성자");
	}
}
public class AppA { // public class의 이름과 파일 이름은 일치해야 합니다 -> 한 파일에 한 개만 가능.
	public static void main(String[] args) {
		TheBase b1 = new TheBase(); // 부모클래스의 인스턴스 생성
		b1.pm(); // 부모클래스의 메서드 호출
		// b1.cm(); // 오류 : 부모 클래스는 자식의 멤버를 사용할 수 없습니다.
		
		TheDerived1 d1 = new TheDerived1(); // 자식클래스의 인스턴스 생성 : 부모 생성자 호출 -> 자식 생성자 호출
		d1.pm(); // TheBase에서 상속한 멤버 사용
		d1.cm(); // 자식클래스의 메서드 호출
	}
}