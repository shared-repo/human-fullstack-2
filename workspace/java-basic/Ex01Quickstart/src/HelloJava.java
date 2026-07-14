
// // -> 한 줄 주석
// 자바의 모든 코드는 class와 같은 타입 내부에 작성해야 합니다.
// -> 클래스는 자바 프로그램의 기본 구성 단위
// -> 원칙적으로 클래스 이름은 파일 이름과 같도록 작성
public class HelloJava { // 중괄호로 클래스의 영역 구분

	/* 영역 주석 */
	/*
	 * static void main : 자바 프로그램의 시작 메서드 (함수)
	 */
	public static void main(String[] args) { // 중괄호로 메서드의 영역 구분
		
		// system.out.println("이 출력은 오류"); // 오류 -> 대소문자 구분 : System과 system은 다른 의미
		
		// System.out.println : (터미널) 출력 메서드
		System.out.println("Hello, Java Programming World !!!!!"); // 문장의 끝에는 ; 표시 ( 없으면 오류 )
		System.out.println("This is my first Java program");
		System.out.println("Bye~~~~");

	}

}
