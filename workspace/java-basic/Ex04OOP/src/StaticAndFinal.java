
public class StaticAndFinal {
	
	int ino;
	// static 멤버는 인스턴스와 관계 없이 1번 만들어지고 모든 인스턴스가 공유
	static int sno;
	
	// static 메서드도 만들 수 있습니다.
	static int getSno() {
		// ino = 100; // 오류 : static 메서드 안에서 인스턴스 멤버를 사용할 수 없습니다.
		return sno;
	}
	
	// static 초기화 구문 : static 멤버 초기화 하는 용도 ( 클래스가 메모리로 처음 로딩될때 호출 )
	static {
		System.out.println("static 초기화 구문");
		sno = 100;		
	}
	
	/////////////////////////////////////
	int vno = 1; // 필드 초기화 : 객체 생성할 때 지정된 값으로 초기화
	final int fno1 = 1; // final 변수 : 값을 변경 할 수 없음 -> 상수 
	final int fno2;
	static final int sfno = 1; // static final 멤버는 필드 초기화만 가능
	
	StaticAndFinal() {
		vno = 20;
		// fno1 = 20; // 이미 초기화된 final member는 생성자에서 값 저장 불가능
		fno2 = 20; // final member는 생성자에서 값 저장 가능
		// sfno = 20; // static final 멤버는 필드 생성자에서 값 저장 불가능
	}
	
	void m() {
		vno = 10;
//		fno1 = 10; // final member의 값 변경 X
//		fno2 = 10; // final member의 값 변경 X
//		sfno = 10; // final member의 값 변경 X
	}	

}
