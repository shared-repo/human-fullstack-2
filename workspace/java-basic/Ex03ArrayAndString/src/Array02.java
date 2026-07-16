
public class Array02 {

	public static void main(String[] args) {

		// 1. 2차원 배열 만들기
		int[][] ar = new int[5][7]; // 해석하자면 5행 7열의 데이터 구조 만들기

		for (int row = 0; row < 5; row++) {
			// System.out.println(ar[row]);
			for (int col = 0; col < 7; col++) {
				ar[row][col] = (int) (Math.random() * 900) + 100; // 100 ~ 1000
			}
		}

		for (int row = 0; row < ar.length; row++) {
			for (int col = 0; col < ar[row].length; col++) {
				System.out.printf("[%d]", ar[row][col]);
			}
			System.out.println();
		}

		// 2. 2차원 배열 초기화
		// int[] ar2 = new int[] {1,2,3};
		// int[][] ar2 = new int[][] { {1,2}, {3,4}, {5,6} }; // 3행 2열 배열 초기화
		int[][] ar2 = { { 1, 2 }, { 3, 4 }, { 5, 6 } }; // 3행 2열 배열 초기화

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 2; col++) {
				System.out.printf("[%d]", ar2[row][col]);
			}
			System.out.println();
		}

		//////////////////////////////////////////////////////////////

		// 3. 각 행의 요소 갯수가 다른 2차원 배열 만들기
		// int[][] ar3 = new int[5][7]; // 모든 행의 요소 갯수가 7
		int[][] ar3 = new int[5][]; // 모든 행의 요소 갯수가 미확정 (배열 인스턴스 생성 X)

		for (int row = 0; row < ar3.length; row++) {
			ar3[row] = new int[row + 3]; // 각 행에 크기가 서로 다른 배열 지정
		}

		for (int row = 0; row < 5; row++) {
			for (int col = 0; col < ar3[row].length; col++) {
				ar3[row][col] = (int) (Math.random() * 900) + 100; // 100 ~ 1000
			}
		}

		for (int row = 0; row < 5; row++) {
			for (int col = 0; col < ar3[row].length; col++) {
				System.out.printf("[%d]", ar3[row][col]);
			}
			System.out.println();
		}

	}

}
