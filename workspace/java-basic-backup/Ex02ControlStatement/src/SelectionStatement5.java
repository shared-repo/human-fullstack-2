
public class SelectionStatement5 {

	public static void main(String[] args) {

		// 점수 처리기 만들기
		// 1. 변수를 만들고 사용자 입력을 받아서 변수에 저장 * 3
		// 2. 세 변수에 저장된 값을 더해서 다른 변수에 저장
		// 3. 합을 3으로 나누어서 평균을 계산하고 다른 변수에 저장
		// 4. 합과 평균을 출력

		// 5. 평균을 기준으로 등급을 계산하고 계산된 등급을 출력
		// 90 ~ 100 : A
		// 80 ~ 90 : B
		// 70 ~ 80 : C
		// 60 ~ 70 : D
		// 0 ~ 60 : F

		///////////////////////////////////

		// 1.
		java.util.Scanner scanner = new java.util.Scanner(System.in);

		System.out.println("첫 번째 점수를 입력하세요 (0 ~ 100) : ");
		int score1 = scanner.nextInt();

		System.out.println("두 번째 점수를 입력하세요 (0 ~ 100) : ");
		int score2 = scanner.nextInt();

		System.out.println("세 번째 점수를 입력하세요 (0 ~ 100) : ");
		int score3 = scanner.nextInt();

		// 2.
		int total = score1 + score2 + score3;

		// 3.
		double average = total / 3.; // 3.0 -> 3.

		System.out.printf("\"평균\" : %.2f\n", average); // %.2f : 소숫점 이하 2자리까지 출력
		
		// 4.
		char grade = '_';
		boolean valid = true;
		// switch (average) { // switch 문은 실수형 데이터를 평가할 수 없습니다.
		switch ( (int)average / 10 ) {
		case 10: 
		case 9:  grade = 'A'; break;
		case 8:	 grade = 'B'; break;
		case 7:  grade = 'C'; break;
		case 6:  grade = 'D'; break;
		case 5:  
		case 4:  
		case 3:  
		case 2:  
		case 1:  
		case 0:  grade = 'F'; break;
		default:
			System.out.println("잘못된 데이터");
			valid = false;
		
		}
		//if (valid == true) {
		if (valid) {
			System.out.printf("등급 : %c", grade);
		}

	}

}
