import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;

public class LottoFrame2 extends JFrame {
	
	JTextField[] numberFields;
	
	public LottoFrame2() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 윈도우를 닫으면 프로그램도 종료하는 설정
		setSize(500, 600);
		setTitle("로또");
		setLocation(100, 100);
		setResizable(false);
		setLayout(null); // 화면 구성 방식(위치, 크기)을 직접 설정하는 선택
		
		JButton button = new JButton();
		button.setText("당첨 예상 번호 뽑기");
		button.setSize(200, 50);
		button.setLocation(150, 175);
		button.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < 6; i++) {
					int rn = (int)(Math.random() * 45 + 1);
					numberFields[i].setText(String.valueOf(rn));
				}
			}
		});
		add(button); // 버튼을 윈도우에 부착
		
		numberFields = new JTextField[6];
		for (int i = 0; i < numberFields.length; i++) {
			numberFields[i] = new JTextField();
			numberFields[i].setSize(60, 60);
			numberFields[i].setLocation(35 + i * 70, 275);
			numberFields[i].setEditable(false);
			numberFields[i].setHorizontalAlignment(JTextField.CENTER);
			numberFields[i].setFont(new Font("Consolas", Font.BOLD, 20));
			add(numberFields[i]);
		}
		
		setVisible(true);
	}
	
	public static void main(String[] args) {
		
		LottoFrame2 f = new LottoFrame2();

	}

}
