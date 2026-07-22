import java.util.Arrays;

public class Lab01 {
	
	public static void main(String[] args) {
		
		int[] numbers = new int[10];
		for (int i = 0; i < numbers.length; i++) {
			numbers[i] = (int)(Math.random() * 900) + 100;
		}		
		for (int i = 0; i < numbers.length; i++) {
			System.out.printf("[%d]", numbers[i]);
		}
		System.out.println();
		
//		1. 최대값과 최소값 찾기
//		   100 ~ 1000 범위의 난수를 10개 만들고 배열에 저장
//		   배열에서 최대값을 찾아서 출력
// 		   배열에서 최소값을 찾아서 출력
		
		int min = 1000, max = 0;
		for (int i = 0; i < numbers.length; i++) {
//			if (max < numbers[i]) {
//				max = numbers[i];
//			}
//			if (min > numbers[i]) {
//				min = numbers[i];
//			}
			max = max < numbers[i] ? numbers[i] : max;
			min = min > numbers[i] ? numbers[i] : min;
		}		
		System.out.printf(" -- [%d - %d]\n", min, max);
		
//		2. 배열 뒤집기
//		   100 ~ 1000 범위의 난수를 10개 만들고 배열에 저장
//		   배열 요소의 순서를 반대 방향으로 재구성
//		   재구성된 배열 출력
		int[] reversedNumbers = new int[numbers.length];
		for (int i = 0; i < numbers.length; i++) {
			reversedNumbers[numbers.length -i - 1] = numbers[i];
		}
		for (int i = 0; i < reversedNumbers.length; i++) {
			System.out.printf("[%d]", reversedNumbers[i]);
		}
		System.out.println();
		
//		3. 평균보다 큰 값 찾기
//		   100 ~ 1000 범위의 난수를 10개 만들고 배열에 저장
//		   평균 구하기
//		   평균 보다 큰 요소만 출력
		int sum = 0;
		for (int i = 0; i < numbers.length; i++) {
			sum += numbers[i];
		}
		double avg = sum / (double)numbers.length;
		System.out.printf("평균 : [%f] -- ", avg);
		for (int i = 0; i < numbers.length; i++) {
			if (numbers[i] >= avg) {
				System.out.printf("[%d]", numbers[i]);
			}
		}
		System.out.println();
		
//		4. 정렬하기
//		   100 ~ 1000 범위의 난수를 10개 만들고 배열에 저장
//		   배열의 값을 오름차순으로 정렬
		
		// Arrays.sort(numbers);
		
		for (int i = 0; i < numbers.length - 1; i++) { // 가장 큰 수를 가장 오른쪽으로 이동하는 싸이클 
			for (int j = 0; j < numbers.length - 1 - i; j++) {
				if (numbers[j] > numbers[j+1]) { // 왼쪽의 숫자가 더 크면 오른쪽으로 이동 ( 숫자 교환 )
					int temp = numbers[j];
					numbers[j] = numbers[j+1];
					numbers[j+1] = temp;
				}
			}			
		}
		
		for (int i = 0; i < numbers.length; i++) {
			System.out.printf("[%d]", numbers[i]);
		}
		
	}

}
