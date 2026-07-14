
public class SelectionStatement {

	public static void main(String[] args) {
		
		// 계산기 만들기
		// 1. 숫자 입력 -> 연산자(+,-,*,/,%) 입력 -> 숫자 입력
		// 2. 입력된 연산자에 따라 연산
		// 3. 결과 출력
		
		//////////////////////////////////////
		
		// 입력기 준비
		java.util.Scanner scanner = new java.util.Scanner(System.in);
		
		// 숫자 입력
		System.out.print("첫 번째 숫자 : ");
		int operand1 = scanner.nextInt();
		
		// 연산자 입력
		System.out.print("연산자 : ");
		String op = scanner.next();
		
		// 숫자 입력
		System.out.print("두 번째 숫자 : ");
		int operand2 = scanner.nextInt();
		
		// 연산
		double result = 0;
		boolean valid = true;
		
		// if (op == "+") {
		if (op.equals("+")) { // 문자열의 내용을 비교할 때 equals 메서드 사용 ( 비교 연산자 사용하면 X )
			result = operand1 + operand2;
		} else if (op.equals("-")) {
			result = operand1 - operand2;
		} else if (op.equals("*")) {
			result = operand1 * operand2;
		} else if (op.equals("/")) {
			result = operand1 / (double)operand2;
		} else if (op.equals("%")) {
			result = operand1 % operand2;
		} else {
			valid = false;
			System.out.println("지원하지 않는 연산자입니다.");
		}
		if (valid) {
			System.out.printf("%d %s %d = %.2f\n", operand1, op, operand2, result);
		}
		

	}

}
















