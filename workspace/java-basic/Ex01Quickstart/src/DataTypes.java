
public class DataTypes {

	public static void main(String[] args) {
		
		// 1. 각 자료형별 변수 선언
		
		// 변수 선언 형식 : 자료형이름 변수이름
		int age; 	// 변수 선언
		age = 35; 	// 변수에 값 저장
		int height = 178; // 변수 선언 + 값 저장 ( 초기화 )
		
		// int pi = 3.14; // 오류 : int 변수에 double 저장할 수 없음
		double pi = 3.14;
		
		boolean valid = true; // boolean literal : true or false
		
		// char grade = "A"; // 오류 : 자바에서 ""는 문자열(0개 이상의 문자 집합)에 사용
		char grade = 'A';
		
		///////////////////////////////
		
		// 2. 리터럴의 기본 자료형과 형변환
		
		// float average = 93.75; // 오류 : 리터럴에도 자료형이 있음 -> 실수 리터럴은 double 타입
		float average = 93.75F; // 실수F : 이 실수의 자료형을 float으로 지정
		
		//long distance = 10000000000; // 오류 : 리터럴에도 자료형이 있음 -> 정수 리터럴은 int 타입 (-21.5억 ~ 21.5억)
		long distance = 10000000000L;	// 정수L : 이 정수의 자료형을 long으로 지정
		
		////////////////////////////////
		
		// 3. 형변환
		
		double v1 = 10; // 암시적(묵시적) 형변환 : 데이터의 손실이 없는 경우 자동으로 형 변환
		int v2 = (int)12.34; // 명시적 형변환 : 데이터의 손실이 있는 경우에는 명시적 형 변환
		
		////////////////////////////////
		
		// 4. 문자 데이터를 저장하는 방법
		
		char grade2 = '수'; // 컴퓨터는 숫자만 저장할 수 있는데 '수'는 어떻게 저장할까? --> 문자 코드에 따라 숫자 저장
		System.out.printf("[문자 : %c][코드 : %d]\n", grade2, (int)grade2);
		
		///////////////////////////////
		
		// 5. 데이터 오버플로우와 언더플로우
		
		int m = Integer.MAX_VALUE; // int로 저장할 수 있는 가장 큰 값 저장 (2147483647)		
		System.out.println(m);
		m = m + 2; // 최대값을 넘으면 최소값으로 변환
		System.out.println(m);
		
		
		/////////////////////////////////
		
		// 연산 결과 데이터의 자료형
		int v3 = 10;
		int v4 = 4;
		// double v5 = v3 / v4; // 정수와 정수를 연산하면 결과 데이터의 자료형은 정수 (소수점 이하 데이터 손실)
		double v5 = v3 / (double)v4; 
		System.out.println(v5);
		
		
		
		
		
		
		
		
		

	}

}
