
public class MainApp {

	public static void main(String[] args) {
		
		// Person 클래스 변수 만들기 + 사용
		Person person;	// 참조형 변수 만들기
		person = new Person(); // 인스턴스 만들기 + 인스턴스의 주소를 person 변수에 저장
		
		person.no = 1; // 클래스의 멤버는 .연산자로 접근
		person.name = "아이유"; 
		person.email = "iu@example.com";
		person.phone = "010-6533-7789";
		
		String info = person.info();
		System.out.println(info);
		
		//////////////////////////////////////////////////////
		
		// Thermostat 클래스 변수 만들기 + 사용
		Thermostat2 t = new Thermostat2(); // 참조변수 만들기 + 인스턴스 만들기 + 인스턴스 주소를 참조변수에 저장하기
		t.increase(10);
		t.display();
		t.increase(20);
		t.display();
		t.decrease(10);
		t.display();
		
		////////////////////////////////////////////////
		
		// Battery 클래스 변수 만들기 + 사용
		Battery2 battery = new Battery2();
		battery.level = 50;
		battery.use(10);
		battery.display();
		battery.use(100);
		battery.display();
		battery.charge(50);
		battery.display();
		
		////////////////////////////////////////////////
				
		// BankAccount 클래스 변수 만들기 + 사용
		BankAccount2 ba = new BankAccount2();
		ba.owner = "홍길동";
		ba.balance = 1000;
		ba.deposit(1000);
		ba.display();
		ba.deposit(1000);
		ba.display();
		ba.withdraw(5000);
		ba.display();
		ba.withdraw(2000);
		ba.display();
		
		////////////////////////////////////////////////
		
		int[] ar = new int[10];
		Person[] persons = new Person[10]; // (Person) 객체의 배열 만들기 -> 주소의 배열이 생성됩니다.
		
		for (int i = 0; i < persons.length; i++) {
			persons[i] = new Person();
			persons[i].no = i + 1;
			persons[i].name = "Person " + (i + 1);
		}
		
		for (int i = 0; i < persons.length; i++) {
			String m = persons[i].info();
			System.out.println(m);			
		}
		
		

	}

}
















