
// 클래스는 사용자 정의 자료형
public class Person2 {
	
	// 1. 특성 : 변수
	int no;
	String name;
	String phone;
	String email;
	
	// 생성자 메서드 
	// - 객체 초기화 / 클래스 이름과 같은 이름 / 결과형 없음 / 오버로딩 가능
	// - 객체 생성시(new 연산자 사용할 때) 자동으로 호출
	// - 생성자 메서드를 만들지 않으면 전달인자 없는 생성자 메서드가 자동으로 제공
	Person2() {
		System.out.println("전달인자 없는 생성자 메서드");
	}
	// 생성자 메서드 오버로딩
	// - 전달인자 있는 생성자 메서드를 만들면 기본 생성자 메서드가 자동으로 만들어지지 않기 때문에
	//   기본생성자 메서드도 만드는 것이 필요합니다.
	Person2(int no, String name, String phone, String email) {
		this(); // 같은 클래스에 있는 다른 생성자 메서드 호출 (여기서는 전달인자 없는 생성자 메서드)
		
		System.out.println("전달인자 있는 생성자 메서드");
		this.no = no; // this : 인스턴스의 멤버임을 표시하는 도구
		this.name = name;
		this.phone = phone;
		this.email = email;
	}


	// 2. 기능 : 메서드 (함수)
	String info() {		
		return "[" + no + "][" + name + "][" + email + "][" + phone + "]";		
	}

	
	
}
