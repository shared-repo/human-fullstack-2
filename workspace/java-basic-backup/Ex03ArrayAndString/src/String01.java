
public class String01 {

	public static void main(String[] args) {

		// 1.
		String str1 = "Hello, String";
		String str2 = "Hello, String";

		// 문자열에 대한 == 연산자는 주소 비교 ( 값 비교 X )
		if (str1 == str2) { // 같은 문자열 리터럴은 한 번만 만들어지기 때문에 주소가 같습니다.
			System.out.println("1. 두 문자열의 주소가 같습니다.");
		} else {
			System.out.println("1. 두 문자열의 주소가 다릅니다.");
		}

		// 2.
		String str3 = new String("Hello, String"); // new : 매번 새 공간 생성
		String str4 = new String("Hello, String"); // new : 매번 새 공간 생성

		// 문자열에 대한 == 연산자는 주소 비교 ( 값 비교 X )
		if (str3 == str4) { // new로 만든 문자열은 매번 새로 만들어지기 때문에 주소가 다릅니다.
			System.out.println("2. 두 문자열의 주소가 같습니다.");
		} else {
			System.out.println("2. 두 문자열의 주소가 다릅니다.");
		}

		// 문자열에 대한 내용(값) 비교는 equals 메서드 사용
		if (str3.equals(str4)) { // new로 만든 문자열은 매번 만들어지기 때문에 주소가 다릅니다.
			System.out.println("3. 두 문자열의 내용이 같습니다.");
		} else {
			System.out.println("3. 두 문자열의 내용이 다릅니다.");
		}

		// 3.
		String str5 = new String("Hello, String"); // new : 매번 새 공간 생성
		String str6 = str5; // 주소 복사 ( 두 참조 변수가 같은 인스턴스를 참조 )
		str5 = str5 + " 추가된 내용"; // 문자열의 변경 사항은 새로운 문자열을 만들어서 적용
		
		System.out.println("4. " + str6);

		// 문자열에 대한 == 연산자는 주소 비교 ( 값 비교 X )
		if (str5 == str6) {
			System.out.println("5. 두 문자열의 주소가 같습니다.");
		} else {
			System.out.println("5. 두 문자열의 주소가 다릅니다.");
		}
		
		// 문자열을 + 연산할 때에는 StringBuilder를 사용
		StringBuilder sb = new StringBuilder(256);
		sb.append("첫 번째 문자열 ");
		sb.append("두 번째 문자열 ");
		sb.append("세 번째 문자열 ");
		System.out.println(sb.toString());

	}

}
