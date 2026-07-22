
public class ConsoleInputOutput {

	public static void main(String[] args) {
		
		// 입력기 생성
		java.util.Scanner scanner = new java.util.Scanner(System.in);
		
		// 입력을 위한 메시지 출력 ( print : 출력 끝에 enter를 포함하지 않는 출력 )
		System.out.print("이름을 입력하세요 : "); // 
		// 사용자가 입력하고 엔터를 치면 입력된 값을 name 변수에 저장
		String name = scanner.nextLine();
		
		// 입력을 위한 메시지 출력
		System.out.print("이메일을 입력하세요 : ");
		// 사용자가 입력하고 엔터를 치면 입력된 값을 email 변수에 저장
		String email = scanner.nextLine();
		
		// 입력을 위한 메시지 출력
		System.out.print("나이를 입력하세요 : ");
		// 사용자가 입력하고 엔터를 치면 입력된 값을 age 변수에 저장
		int age = scanner.nextInt();
		
		// 입력된 내용을 출력 ( println : 출력 끝에 enter를 포함 )
		// System.out.println("[이름 : " + name + "][이메일 : " + email + "]"); // 문자열 + 데이터 -> 결합된 새 문자열
		System.out.print("[이름 : " + name + "][이메일 : " + email + "]\n");
		// name은 첫 번째 %s로 email은 두 번째 %s로 age는 세 번째 %d로 삽입
		System.out.printf("[이름 : %s][이메일 : %s][나이 : %d]", name, email, age); 
		
		char grade = 'A'; // 문자형 예시
		
		scanner.close(); // 다 사용한 입력기를 (다른 프로그램이 사용할 수 있도록) 반환
		
	}
	
}

// 1. printf에서 사용하는 서식 문자열 (서식 문자열과 실제 데이터의 자료형이 일치하지 않으면 오류)
// %s : 문자열 데이터 ( 문자열 : 0개 이상의 문자 조합 -> 큰 따옴표로 표시 )
// %c : 문자 데이터 ( 문자 : 1개의 문자 -> 작은 따옴표로 표시 )
// %d : 정수형 데이터
// %f : 실수형 데이터
// %b : 진위형 데이터 (boolean)

// 2. escape sequence ( 문자열 안에서 표현하기 어려운 문자 표시 방법)
// \n : enter
// \t : tab
// \r : home
// \b : backspace
// \" : "
// \' : '
// \\ : \
















