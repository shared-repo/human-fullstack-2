
public class SelectionStatement3 {

	public static void main(String[] args) {
		
		// 1. -50 ~ 50 난수 발생
		// 2. 뽑힌 난수가 음수인지, 양수인지, 0인지 구분해서 출력
		double n = Math.random(); // 0 ~ 1 범위의 난수 뽑기 (1은 포함하지 않습니다.)
		n = (int)(n * 101 - 50);
		// System.out.println(n);
		
		if (n < 0) {
			System.out.printf("%d는 음수입니다.\n", (int)n);
		} else if (n > 0) {
			System.out.printf("%d는 양수입니다.\n", (int)n);
		} else {
			System.out.printf("%d는 0입니다.\n", (int)n);
		}
		
		/////////////////////////////////////////////////
		
		// 1. 아이디와 패스워드 입력
		// 2. 아이디가 "iamuser"이고 패스워드가 "12345"라면 "로그인 성공" 출력 아니면 "로그인 실패" 출력
		java.util.Scanner scanner = new java.util.Scanner(System.in);
		
		System.out.print("아이디 : ");
		String id = scanner.nextLine();
		
		System.out.print("패스워드 : ");
		String passwd = scanner.nextLine();
		
		if (id.equals("iamuser") && passwd.equals("12345")) {
			System.out.println("로그인 성공");
		} else {
			System.out.println("로그인 실패");
		}
		
		/////////////////////////////////////////////////
		
		// 1. 소득 입력
		// 2. 세금 계산 및 출력
		//    세율 : 
		//    0 ~ 2000만원 -> 0
		//    2000만원 ~ 4000만원 -> 10%
		//    4000만원 ~ 6000만원 -> 15%
		//    6000만원 ~ 8000만원 -> 20%
		//    8000만원 이상       -> 30%
		
		
		// 1. 입장객의 나이 입력
		// 2. 입장료 계산
		//    기준:
		//    0 ~ 7세 : 무료
		//    60세 이상 : 무료
		//    8 ~ 19세 : 5000원
		//    20세 ~ 59세 : 10000원

	}

}
















