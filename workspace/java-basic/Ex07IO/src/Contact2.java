import java.io.Serializable;

public class Contact2 implements Serializable {
	
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
	
	public Contact2() {}
	public Contact2(int no, String name, String phone, String email) {
		this.no = no;
		this.name = name;
		this.phone = phone;
		this.email = email;
	}
	
	public void display() {
		System.out.printf("[%3d][%10s][%15s][%s]\n", no, name, phone, email);
	}
	
	

}
