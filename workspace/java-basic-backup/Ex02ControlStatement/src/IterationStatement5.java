
public class IterationStatement5 {

	public static void main(String[] args) {
		
		// 입력기 만들기
		java.util.Scanner scanner = new java.util.Scanner(System.in);
		
		outer: while(true) { // outer : while 문에 붙이는 이름			
			// 기능 목록 표시
			System.out.println("******************************");
			System.out.println("* 1. 당첨 예상 번호 뽑기.");
			System.out.println("* 2. 종료.");
			System.out.println("******************************");
			
			// 기능 선택 ( 사용자 입력 )
			System.out.print("어떤 기능을 실행할까요 : ");
			String selection = scanner.nextLine();
			
			// 선택적 기능 실행 ( 사용자 입력에 따라 )
			System.out.println();
			switch (selection) {
			case "1" : 
			
				int[] numbers = new int[6]; // 정수 6개를 저장하는 배열 만들기
				for (int i = 0; i < 6; i++) { // 번호 뽑기 반복문
					int number = (int)(Math.random() * 45 + 1); // 1 ~ 45 범위의 난수 발생
					numbers[i] = number; // 반복 횟수에 따라 해당하는 배열의 위치에 데이터 저장
					for (int j = 0; j < i; j++) { // 중복 검사 반복문 : 현재 뽑은 숫자의 위치(i) 전까지 반복하면서 중복 검사
						if (numbers[i] == numbers[j]) {
							i--; // 번호 뽑기 반복문에서 i의 값이 증가하는 것을 막기 위해서 미리 값을 감소
							break;
						}
					}
				}
				System.out.print("선택된 번호 : ");
				for (int i = 0; i < 6; i++) {
					System.out.printf("[%2d]", numbers[i]);
				}
				System.out.println();
			
				break;
			case "2" : 
				System.out.println("프로그램을 종료합니다.");
				System.out.println("행운을 빕니다.");
				// break; // switch를 break
				break outer; // outer라는 이름이 붙은 반복문 또는 switch 문 종료
			default : // 위의 case 중 해당되는 것이 없을 때 실행되는 영역 
				System.out.println("지원하지 않는 기능입니다.");
			}
			System.out.println();
			
		}
		
		

	}

}
