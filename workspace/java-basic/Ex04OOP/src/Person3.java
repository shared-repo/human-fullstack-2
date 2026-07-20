
// 클래스는 사용자 정의 자료형
public class Person3 {
	
	// private : 클래스 외부에서 접근 할 수 없음
	private int no;
	private String name;
	private String phone;
	private String email;	
	
	public int getNo() {
		return no;
	}
	public void setNo(int no) {
		this.no = no;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}


	// 2. 기능 : 메서드 (함수)
	public String info() {		
		return "[" + no + "][" + name + "][" + email + "][" + phone + "]";		
	}	
	
}
