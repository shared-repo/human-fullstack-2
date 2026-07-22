
public class BankAccount2 {
	
	String owner;
	double balance;
	
	void deposit(double amount) {
		if (amount <= 0) {
			return;
		}
		balance += amount;		
	}
	void withdraw(double amount) {
		if (amount <= 0) {
			return;
		}
		if (balance < amount) {
			System.out.println("잔액이 부족합니다.");
			return;
		}
		balance -= amount;		
	}
	void display() {
		System.out.printf("[예금주 : %s][잔액 : %f]\n", owner, balance);
	}
	
	
	

}
