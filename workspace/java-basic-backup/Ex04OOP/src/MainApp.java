import java.util.Scanner;

import pa.TheClassOne; // 이 파일에서 TheClassOne 이름을 사용하면 pa.TheClassOne으로 해석하는 설정 
import pa.subb.TheClassThree;// 이 파일에서 TheClassThree 이름을 사용하면 pa.subb.TheClassThree으로 해석하는 설정

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
		
		////////////////////////////////////////////////
		
		System.out.println("-------------------------------");
		MethodTest mt = new MethodTest();
		mt.m1();
		mt.m2("메서드 연습", 100);
		int result = mt.m3(10, 20);
		System.out.println("result : " + result);
		
		// 메서드 오버로딩 테스트
		System.out.println( mt.sum(10, 20) );
		System.out.println( mt.sum(10, 20, 30) );
		System.out.println( mt.sum(10, 20, 30, 40) );
		
		// 가변 인자 배열 사용
		System.out.println( mt.sum2(10, 20) );
		System.out.println( mt.sum2(10, 20, 30) );
		System.out.println( mt.sum2(10, 20, 30, 40) );
		
		
		System.out.println("-------------------------------");
		Person2 p;
		p = new Person2(); // 이 때 자동으로 전달인자 없는 생성자 메서드 호출
		System.out.println(p.info());
		p = new Person2(1, "홍길동", "010-6523-9887", "hdk@example.com"); // 전달인자 있는 생성자 메서드 호출
		System.out.println(p.info());
		
		System.out.println("-------------------------------");
		
		Person3 p3 = new Person3();
		// p3.no = 1; // 오류 : private 멤버는 외부에서 접근 불가능
		p3.setNo(1);
		p3.setName("아이유");
		p3.setPhone("010-2258-3369");
		p3.setEmail("iu@example.com");
		String r = p3.info(); // public 멤버는 외부에서 접근 가능
		System.out.println(r);
		
		System.out.println("-------------------------------");
		
		StaticAndFinal sf1 = null;
		sf1 = new StaticAndFinal();
		
		StaticAndFinal sf2 = new StaticAndFinal();
		
		System.out.printf("[%d - %d][%d - %d]\n", sf1.ino, sf2.ino, sf1.sno, sf2.sno);
		sf2.ino = 200;
		sf2.sno = 200;
		System.out.printf("[%d - %d][%d - %d]\n", sf1.ino, sf2.ino, sf1.sno, sf2.sno);
		
		System.out.println("sno : " + StaticAndFinal.sno); // static 멤버는 클래스 이름으로 접근하는 것이 정석
		System.out.println("sno : " + StaticAndFinal.getSno()); // static 멤버는 클래스 이름으로 접근하는 것이 정석
		
		
		System.out.println("---------------------------------------");
		
		// 클래스를 사용할 때 패키지 이름도 같이 표시해야 합니다.
		pa.TheClassOne co = new pa.TheClassOne();
		pa.suba.TheClassTwo ct = new pa.suba.TheClassTwo();
		
		// import 구문을 사용하면 패키지 이름 생략 가능
		TheClassOne co2 = new TheClassOne();
		TheClassThree ct2 = new TheClassThree();
		
		java.util.Scanner scanner = new java.util.Scanner(System.in);
		Scanner scanner2 = new Scanner(System.in);
		
		// pa.PackageScopeClass obj = new pa.PackageScopeClass(); // 오류 : public이 아닌 클래스는 패키지 외부에서 사용 불가능
		
	}

}
















