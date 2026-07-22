import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class TheFrame extends JFrame {
	
	class ClickHandler implements ActionListener { // ActionListener : Click 했을 때 호출할 메서드 규격
		@Override
		public void actionPerformed(ActionEvent e) {
			// JOptionPane.showMessageDialog(null, "눌러 주셔서 감사합니다.");
			Container container = getContentPane(); // 윈도우에서 사용자 영역 반환
			int r = (int)Math.floor(Math.random() * 256);
			int g = (int)Math.floor(Math.random() * 256);
			int b = (int)Math.floor(Math.random() * 256);
			Color c = new Color(r, g, b); // 색상 객체 : r, g, b 값으로 색상 생성
			container.setBackground(c);	
		}
	}
	
	public TheFrame() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 윈도우를 닫으면 프로그램도 종료하는 설정
		setSize(500, 600);
		setTitle("나의 두번째 윈도우");
		setLocation(100, 100);
		setLayout(null); // 화면 구성 방식(위치, 크기)을 직접 설정하는 선택
		
		JButton button = new JButton();
		button.setText("여기를 눌러주세요");
		button.setSize(200, 100);
		button.setLocation(150, 225);
		// button.addActionListener(new ClickHandler());
		button.addActionListener((e) -> {
			Container container = getContentPane(); // 윈도우에서 사용자 영역 반환
			int r = (int)Math.floor(Math.random() * 256);
			int g = (int)Math.floor(Math.random() * 256);
			int b = (int)Math.floor(Math.random() * 256);
			Color c = new Color(r, g, b); // 색상 객체 : r, g, b 값으로 색상 생성
			container.setBackground(c);
		});
		
		add(button); // 버튼을 윈도우에 부착
		
		setVisible(true);
	}
	

	public static void main(String[] args) {
		
		TheFrame window = new TheFrame();

	}

}
