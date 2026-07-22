
public class Thermostat2 {
	
	int temperature;
	
//	void increase() {
//		temperature += 1;
//	}
	void increase(int amount) {
		temperature += amount;
	}
	void decrease(int amount) {
		temperature -= amount;
	}
	void display() {
		System.out.printf("[TEMPERATURE : %d\n", temperature);
	}

}
