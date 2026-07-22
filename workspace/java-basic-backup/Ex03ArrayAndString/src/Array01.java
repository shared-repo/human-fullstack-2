
public class Array01 {

	public static void main(String[] args) {
		
		// 1. 배열 생성 및 사용
		
		// 배열 선언
//		int[] ar; // 배열의 참조(주소)를 저장할 변수 선언
//		ar = new int[10]; // 값 10개를 관리하는 배열을 만들고 그 주소를 ar에 저장
		int[] ar = new int[10]; // 위 두 줄의 코드를 한 줄로 작성 
		
		// 배열의 각 요소에 값 저장
		for (int i = 0; i < 10; i++) {
			ar[i] = (int)(Math.random() * 900 + 100);
		}
		
		// 배열의 각 요소의 값 읽기
		for (int i = 0; i < 10; i++) {
			System.out.println(ar[i]);
		}
		
		// 2. 배열 초기화 1
		int[] ar2 = new int[] { 1, 2, 3, 4, 5 };
		for (int i = 0; i < 5; i++) {
			System.out.println(ar2[i]);
		}
		
		// 3. 배열 초기화 2
		int[] ar3 = { 1, 2, 3, 4, 5 };
		for (int i = 0; i < 5; i++) {
			System.out.println(ar3[i]);
		}
		
		// 4. 배열 각 요소의 초깃값
		int[] ar4 = new int[5];
		for (int i = 0; i < 5; i++) {
			System.out.println(ar4[i]);
		}
		
		// 5. 배열의 속성 : 배열.length
		int[] ar5 = new int[5];
		for (int i = 0; i < ar5.length; i++) { // 배열.length : 배열에 포함된 요소 갯수
			ar5[i] = (int)(Math.random() * 900) + 100; // 0~1 -> 0~900 -> 100~1000
		}
		for (int i = 0; i < ar5.length; i++) {
			System.out.println(ar5[i]);
		}
		
		// 6. enhanced for
		for (int n : ar5) { // ar5 배열에서 처음부터 끝까지 순서대로 값을 하나씩 꺼내서 n에 저장
			System.out.println(n);
		}
		
		// ref. 참조 변수의 초기화
		int[] ar7 = null; // null : 참조변수가 가르키는 인스턴스가 없음
		System.out.println(ar7[0]); // null로 초기화 되면 컴파일 오류는 사라지지만 실행 오류 발생

	}

}
