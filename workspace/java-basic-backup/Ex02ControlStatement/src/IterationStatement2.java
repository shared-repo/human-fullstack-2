
public class IterationStatement2 {

	public static void main(String[] args) {
		
		
		// *
		// **
		// ***
		// ...
		// **********
		for (int r = 0; r < 10; r++) {
			for (int c = 0; c < r + 1; c++ ) {
				System.out.print("*");				
			}
			System.out.println(); // 줄바꿈 (enter)
		}
		
		
		// **********
		// *********
		// ********
		// ...
		// *
		for (int r = 0; r < 10; r++) {
			for (int c = 0; c < 10 - r; c++ ) {
				System.out.print("*");				
			}
			System.out.println(); // 줄바꿈 (enter)
		}
		
		//          *
		//         **
		//        ***
		// ...
		// **********
		for (int r = 0; r < 10; r++) {
			for (int c = 0; c < 10 - r - 1; c++ ) {
				System.out.print(" ");				
			}
			for (int c = 0; c < r + 1; c++ ) {
				System.out.print("*");				
			}
			System.out.println(); // 줄바꿈 (enter)
		}
		
		// **********
		//  *********
		//   ********
		// ...
		//          *
		for (int r = 0; r < 10; r++) {
			for (int c = 0; c < r; c++ ) {
				System.out.print(" ");				
			}
			for (int c = 0; c < 10 - r; c++ ) {
				System.out.print("*");				
			}
			System.out.println(); // 줄바꿈 (enter)
		}

	}

}
