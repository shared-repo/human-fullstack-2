
// class Child extends Object {
class Child { // 자동으로 Object를 상속 -> 모든 클래스는 직/간접으로 Object 상속	
	int x;	
	
	@Override
	// 동일성 비교 메서드 
	// 기본 구현은 참조 비교 -> 재정의를 통해 기준을 변경할 수 있습니다.
	public boolean equals(Object obj) { 
		Child other = (Child)obj;
		return this.x == other.x;
	}
	
	@Override
	// 인스턴스의 정보를 간단한 문자열로 반환하는 메서드
	public String toString() {
		return String.format("[X : %d]", x);
	}
	
	@Override
	// 인스턴스를 대표하는 고유 값을 반환하는 메서드 : 비교에 사용
	public int hashCode() {
		return x;
	}
	
}
public class TheObject {

	public static void main(String[] args) {
		
		Child obj1 = new Child();		
		obj1.x = 10;
		Child obj2 = new Child();		
		obj2.x = 10;
		
		if (obj1 == obj2) { // 비교 연산자는 항상 참조(주소)를 비교 -> 재정의 불가능
			System.out.println("두 변수가 참조하는 인스턴스가 같습니다.");
		} else {
			System.out.println("두 변수가 참조하는 인스턴스가 다릅니다.");
		}
		
		if (obj1.equals(obj2)) { // equals 메서드의 기본 구현은 참조(주소)를 비교 -> 재정의를 통해 변경할 수 있습니다.
			System.out.println("두 객체의 값이 같습니다.");
		} else {
			System.out.println("두 객체의 값이 다릅니다.");
		}
		
		
		String s1 = new String("Hello");
		String s2 = new String("Hello");
		if (s1 == s2) { // 비교 연산자는 항상 참조(주소)를 비교
			System.out.println("두 문자열 인스턴스가 같습니다.");
		} else {
			System.out.println("두 문자열 인스턴스가 다릅니다.");
		}
		
		if (s1.equals(s2)) { // equals 메서드의 기본 구현은 참조(주소)를 비교 -> 재정의를 통해 변경할 수 있습니다.
			System.out.println("두 문자열의 값이 같습니다.");
		} else {
			System.out.println("두 문자열의 값이 다릅니다.");
		}
		
		////////////////////
		
		System.out.println(obj1.toString());
		System.out.println(obj1); // 문자열이 필요한 경우 일반적으로 toString()을 호출
		
	}

}