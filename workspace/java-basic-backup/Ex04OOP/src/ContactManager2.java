
public class ContactManager2 {

	// 여러 개의 연락처를 관리하는 배열 필드 선언
	private Contact2[] contacts = new Contact2[1000];
	// 다음에 등록할 연락처의 배열에서의 위치 번호를 저장하는 필드 선언
	private int nextIdx = 0;
	
	java.util.Scanner scanner = new java.util.Scanner(System.in);
	
	// 연락처 관리 프로그램 실행을 관리하는 메서드 정의
	public String selectTask() {
		System.out.println("*******************************");
		System.out.println("* 1. 연락처 등록.");
		System.out.println("* 2. 연락처 목록.");
		System.out.println("* 3. 연락처 검색.");
		System.out.println("* 4. 연락처 삭제.");
		System.out.println("* 5. 연락처 수정.");
		System.out.println("* 9. 종료.");
		System.out.println("*******************************");
		
		System.out.print("원하는 작업을 선택하세요 : ");
		String selection = scanner.nextLine();
		
		return selection;
	}
	
	public void doManage() {
		main: while (true) {
			String selection = selectTask();
			System.out.println();
			switch (selection) {
			case "1": 
				register();
				break;
			case "2": 
				list();
				break;
			case "3": break;
			case "4": break;
			case "5": break;
			case "9": 
				System.out.println("프로그램을 종료합니다.");
				break main; // main 이름이 지정된 반복문 또는 switch문 종료
			default: 
				System.out.println("지원하지 않는 작업입니다.");
				break;
			}
			System.out.println();
		}
	}
	
	
	// 연락처 관리에 필요한 메서드 (등록, 수정, 삭제, 검색, 목록)
	public void register() {
		// 연락처 내용 입력
		System.out.println("[연락처 정보 입력]");
		System.out.print("이름 : ");
		String name = scanner.nextLine();
		System.out.print("전화번호 : ");
		String phone = scanner.nextLine();
		System.out.print("이메일 : ");
		String email = scanner.nextLine();
		
		// 입력 내용을 사용해서 연락처 객체 생성
		Contact2 contact = new Contact2(nextIdx + 1, name, phone, email);
		
		// 연락처 목록 관리 배열에 연락처 추가(저장)
		contacts[nextIdx] = contact;
		
		nextIdx++; // 다음 연락처 등록 위치 변경 (1증가)
		
		System.out.println("연락처를 등록했습니다.");
		
	}
	public void list() {
		if (nextIdx == 0) {
			System.out.println("등록된 연락처가 없습니다.");
		}
		System.out.println("[ 연락처 목록 ]");
		for (int i = 0; i < nextIdx; i++) {
			contacts[i].display();
		}
	}
	public void search() {}
	public void delete() {}
	public void update() {}
	
	
	
	public static void main(String[] args) {
		
		ContactManager2 manager = new ContactManager2();
		manager.doManage();
		
		
//		Contact2 contact = new Contact2(1, "John Doe", "010-6547-1236", "johndoe@example.com");
//		contact.display();
//		
//		Contact2 contact2 = new Contact2();
//		contact2.setNo(2);
//		contact2.setName("Jane Doe");
//		contact2.setPhone("010-8521-9632");
//		contact2.setEmail("janedoe@example.com");
//		contact2.display();

	}

}
