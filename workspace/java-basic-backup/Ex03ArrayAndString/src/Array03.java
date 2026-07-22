
public class Array03 {

	// main 메서드의 전달인자는 프로그램 실행할 때 입력한 데이터를 배열 형식으로 받는 변수
	public static void main(String[] args) {
		
		System.out.println("main 메서드의 전달인자를 확인합니다.");
		
		for (String arg : args) {
			System.out.println(arg);
		}

	}

}
