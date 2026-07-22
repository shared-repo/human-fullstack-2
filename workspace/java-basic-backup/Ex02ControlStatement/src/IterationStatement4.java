
public class IterationStatement4 {

	public static void main(String[] args) {
				
		java.util.Scanner scanner = new java.util.Scanner(System.in);
				
		while (true) { // 무한루프

			// 기능 구현 영역 ( 점수 입력 -> 등급 계산 -> 출력 )
			{
			// 1.
			System.out.print("첫 번째 점수를 입력하세요 (0 ~ 100) : ");
			int score1 = scanner.nextInt();
			if (score1 < 0 || score1 > 100) {
				continue; // 아래의 실행문은 건너 뛰고 반복문의 처음으로 돌아가세요.
			}
	
			int score2 = 0;
			do {
				System.out.print("두 번째 점수를 입력하세요 (0 ~ 100) : ");
				score2 = scanner.nextInt();
			} while (score2 < 0 || score2 > 100);
	
			int score3 = 0;
			do {
				System.out.print("세 번째 점수를 입력하세요 (0 ~ 100) : ");
				score3 = scanner.nextInt();
			} while (score3 < 0 || score3 > 100);
	
			// 2.
			int total = score1 + score2 + score3;
	
			// 3.
			double average = total / 3.; // 3.0 -> 3.
	
			System.out.printf("\"평균\" : %.2f\n", average); // %.2f : 소숫점 이하 2자리까지 출력
	
			// 4.
			char grade = '_';
			boolean valid = true;
			// switch (average) { // switch 문은 실수형 데이터를 평가할 수 없습니다.
			switch ((int) average / 10) {
			case 10:
			case 9:
				grade = 'A';
				break;
			case 8:
				grade = 'B';
				break;
			case 7:
				grade = 'C';
				break;
			case 6:
				grade = 'D';
				break;
			case 5:
			case 4:
			case 3:
			case 2:
			case 1:
			case 0:
				grade = 'F';
				break;
			default:
				System.out.println("잘못된 데이터");
				valid = false;
	
			}
			// if (valid == true) {
			if (valid) {
				System.out.printf("등급 : %c\n", grade);
			}
			}
	
			System.out.print("계속할까요(y/n)? ");
			String yn = scanner.next();
			// if (yn.equalsIgnoreCase("y") == false) { // 입력이 "y"가 아니라면
			if (!yn.equalsIgnoreCase("y")) { // 입력이 "y"가 아니라면
				break;
			}
		}

	}
}
