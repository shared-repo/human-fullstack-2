import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Scanner;

public class LottoApp2 {

	private LottoDao2 dao = new LottoDao2();
	
	public void run() {
		
		Scanner scanner = new Scanner(System.in);
				
		// 로또 번호 추출기
		// 1. 숫자 여섯 개를 저장할 배열 만들기
		int[] numbers = new int[6];

		outer: while (true) {
			
			System.out.println("******************************");
			System.out.println("* 1. 당첨 예상 번호 뽑기.");
			System.out.println("* 2. 과거 당첨 번호 초기화 1.");
			System.out.println("* 3. 과거 당첨 번호 초기화 2.");
			System.out.println("* 4. 회차별 당첨 번호 조회.");
			System.out.println("* 9. 종료.");
			System.out.println("******************************");
			System.out.print("작업 선택 : ");
			String selection = scanner.nextLine();
			
			switch(selection) {
			case "9": 				
				System.out.println("행운을 빕니다.");
				System.out.println("프로그램을 종료합니다.");
				break outer; // outer라는 이름이 붙은 반복문 또는 switch문 break
				
			case "1":
				// 2. 당첨 예상 번호 뽑기
				while (true) {
					// 2-1. [1, 45] 범위의 (중복되지 않는) 6개의 난수 뽑기 -> 배열에 저장 : ( 반복문 사용 )
					for (int i = 0; i < 6; i++) {
						numbers[i] = (int) (Math.random() * 45) + 1; // [1 ~ 45]
						for (int j = 0; j < i; j++) { // 현재 뽑힌 위치 이전까지 반복
							if (numbers[i] == numbers[j]) { // 현재 뽑힌 숫자와 이전 숫자가 같다면(중복)
								i--; // for 문에 i가 증가하더라도 현재 i 위치를 다시 뽑도록 미리 1감소
								// i = -1; // for 문에 i가 증가하더라도 처음부터 다시 뽑도록 -1로 설정
							}
						}
					}

					// 2-2. 평균이 [20 ~ 26] 범위를 벗어나면 2-1부터 다시 (다시뽑기)
					int sum = 0;
					for (int i = 0; i < 6; i++) {
						sum += numbers[i];
					}
					int average = sum / 6;
					if (average >= 20 && average <= 26) { // 정상
						System.out.printf("[AVERAGE %2d]", average);
						break;
					}
				}

				// 3. 뽑힌 숫자 출력 : ( 반복문 사용 )
				for (int number : numbers) {
					System.out.printf("[%2d]", number);
				}
				System.out.println();
				
				break;
			
			case "2":
				
				// 기존 데이터 제거 : lotto 테이블의 모든 데이터를 삭제
				dao.deleteAll();
				
				// lotto-winning-numbers.csv 파일의 데이터를 읽어서 DB에 저장 (반복문 처리)
				try (FileInputStream fis = new FileInputStream("lotto-winning-numbers.csv");
					 InputStreamReader isr = new InputStreamReader(fis);
					 BufferedReader br = new BufferedReader(isr)) {
					while (true) {
						String line = br.readLine();
						if (line == null) { // EOF (파일의 끝)
							break;
						}
						String[] data = line.split(",");
						LottoDto2 dto = 
							new LottoDto2(Integer.parseInt(data[0]),
								Integer.parseInt(data[1]),
								Integer.parseInt(data[2]),
								Integer.parseInt(data[3]),
								Integer.parseInt(data[4]),
								Integer.parseInt(data[5]),
								Integer.parseInt(data[6]),
								Integer.parseInt(data[7]));
						dao.insertLotto(dto);						
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				
				break;
				
			case "3":
				// 기존 데이터 제거 : lotto 테이블의 모든 데이터를 삭제
				dao.deleteAll();
				
				// lotto-winning-numbers.csv 파일의 데이터를 읽어서 DB에 저장 (반복문 처리)				
				try (FileInputStream fis = new FileInputStream("lotto-winning-numbers.csv");
					 InputStreamReader isr = new InputStreamReader(fis);
					 BufferedReader br = new BufferedReader(isr)) {
					
					ArrayList<LottoDto2> list = new ArrayList<>();
					while (true) {
						String line = br.readLine();
						if (line == null) { // EOF (파일의 끝)
							break;
						}
						String[] data = line.split(",");
						LottoDto2 dto = 
							new LottoDto2(Integer.parseInt(data[0]),
								Integer.parseInt(data[1]),
								Integer.parseInt(data[2]),
								Integer.parseInt(data[3]),
								Integer.parseInt(data[4]),
								Integer.parseInt(data[5]),
								Integer.parseInt(data[6]),
								Integer.parseInt(data[7]));
						list.add(dto);	
					}
					dao.insertLottoBatch(list);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				
				break;
				
			case "4":
				System.out.print("검색할 로또 회차 : ");
				String srnd = scanner.nextLine();
				int rnd = Integer.parseInt(srnd);
				
				// 입력된 회차로 데이터베이스에서 데이터 조회
				// 조회 결과 출력
				LottoDto2 dto = dao.selectLottoByRnd(rnd);
				if (dto == null) {
					System.out.println("해당 회차의 당첨번호가 없습니다.");
				} else {
					System.out.println(dto);
				}
				
				break;
				
			default:
				System.out.println("지원하지 않는 명령입니다.");
			}			
		}	
	}

	public static void main(String[] args) throws Exception {

		LottoApp2 app = new LottoApp2();
		app.run();
		
	}	

}