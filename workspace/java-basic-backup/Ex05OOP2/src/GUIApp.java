import javax.swing.JFrame;

public class GUIApp {
	
	public static void main(String[] args) {
		
		// 윈도우 만들기
		JFrame frame = new JFrame();
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 윈도우를 닫으면 프로그램도 종료하는 설정
		frame.setSize(500, 600);
		frame.setTitle("나의 첫번째 윈도우");
		frame.setLocation(100, 100);
		
		frame.setVisible(true);
		
		
		
	}

}
