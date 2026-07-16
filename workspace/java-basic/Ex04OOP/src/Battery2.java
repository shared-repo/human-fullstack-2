
public class Battery2 {
	
	int level;
	
	void use(int amount) {
		level -= amount;
		if (level < 0) {
			level = 0;
		}
	}
	void charge(int amount) {
		level += amount;
		if (level > 100) {
			level = 100;
		}
	}
	void display() {
		System.out.printf("[배터리 잔량 : %d%%]\n", level);
	}

}
