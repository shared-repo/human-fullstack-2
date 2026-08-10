
public class App {

	public static void main(String[] args) {
		
		Person person = new Person();
		person.setName("장원영");
		person.setEmail("jwy@example.com");
		person.setPhone("010-6325-9877");
		
		System.out.println(person.toString());
		
		Person person2 = new Person("아이유", "iu@example.com", "010-5544-8899");
		System.out.println(person2.toString());
		
		// ------------------------------------------
		
		Person2 person3 = new Person2();
		person3.setName("장원영");
		person3.setEmail("jwy@example.com");
		person3.setPhone("010-6325-9877");
		
		System.out.println(person3.toString());

	}

}
