
public class IterationStatement {

	public static void main(String[] args) {
		
		java.util.Scanner scanner = new java.util.Scanner(System.in);
		
		// 구구단의 단 입력 -> 해당 단을 출력
		// 5 x 1 = 5
		// 5 x 2 = 10
		// ..
		// 5 x 9 = 45
		System.out.print("출력한 단을 입력하세요 : ");
		int dan = scanner.nextInt();
		for (int i = 1; i <= 9; i++) { // i++ : i = i + 1
			System.out.printf("%d x %d = %d\n", dan, i, dan * i);
		}
		
		// 1단부터 9단까지 구구단 출력 ( 중첩 반복문 )
		for (int y = 1; y <= 9; y++) {
			for (int x = 1; x <= 9; x++) {
				System.out.printf("%d x %d = %2d  ", x, y, x * y); // %2d : 숫자가 한자리라도 두자리에 출력
			}
			System.out.println(); // 줄바꿈
		}

	}

}
