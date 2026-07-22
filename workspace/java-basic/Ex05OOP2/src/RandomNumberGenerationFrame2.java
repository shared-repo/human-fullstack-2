import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;

public class RandomNumberGenerationFrame2 extends JFrame {
	
	JTextField text;
	
	public RandomNumberGenerationFrame2() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 윈도우를 닫으면 프로그램도 종료하는 설정
		setSize(500, 600);
		setTitle("난수 발생기");
		setLocation(100, 100);
		setLayout(null); // 화면 구성 방식(위치, 크기)을 직접 설정하는 선택
		
		JButton button = new JButton();
		button.setText("난수 만들기");
		button.setSize(200, 50);
		button.setLocation(150, 175);
		button.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				int rn = (int)(Math.random() * 900 + 100);
				text.setText(String.valueOf(rn));
			}
		});
		add(button); // 버튼을 윈도우에 부착
		
		text = new JTextField();
		text.setSize(200, 50);
		text.setLocation(150, 275);
		text.setEditable(false);
		text.setHorizontalAlignment(JTextField.CENTER);
		text.setFont(new Font("Consolas", Font.BOLD, 30));
		add(text);
		
		setVisible(true);
	}
	
	public static void main(String[] args) {
		
		RandomNumberGenerationFrame2 f = new RandomNumberGenerationFrame2();

	}

}
