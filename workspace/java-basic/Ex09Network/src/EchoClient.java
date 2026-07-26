import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class EchoClient {

	public static void main(String[] args) {
		
		Scanner scanner = new Scanner(System.in);
		
		while (true) {
			
			System.out.print("서버로 전송할 메시지 (종료는 q) : ");
			String message = scanner.nextLine();
			
			if (message.equalsIgnoreCase("q")) {
				System.out.println("프로그램을 종료합니다.");
				break;
			}
			
			Socket socket = null; // 서버와 통신하기 위한 도구
			try {
				socket = new Socket("192.168.0.19", 9999); // 소켓을 만들고 서버와 연결
				
				// 대화 ( 데이터 수신 + 데이터 송신 )
				InputStream is = null;			// 데이터 수신 스트림 (byte[])
				InputStreamReader isr = null;	// byte[] -> char[] 변환기
				BufferedReader br = null;		// 한 줄 단위로 끊어서 처리하는 도구
				
				OutputStream os = null;			// 데이터 송신 스트림 (byte[])
				PrintWriter pw = null;			// char[] -> byte[], 그 외 system.out 객체의 기능 포함
				try {
					os = socket.getOutputStream();// 소켓에 연결된 쓰기 스트림 가져오기
					pw = new PrintWriter(os); 
					pw.println(message); // 클라이언트로 데이터 전송
					pw.flush(); // 전송되지 않은 데이터 강제 전송
					
					is = socket.getInputStream();	// 소켓에 연결된 읽기 스트림 가져오기
					isr = new InputStreamReader(is);
					br = new BufferedReader(isr);
					String receivedMessage = br.readLine();	// 클라이언트가 송신한 데이터 읽기
					System.out.println(receivedMessage);
					
				} catch (Exception ex2) {
					ex2.printStackTrace();
				} finally {
					try { br.close(); } catch (Exception ex2) {}
					try { isr.close(); } catch (Exception ex2) {}
					try { is.close(); } catch (Exception ex2) {}
					try { pw.close(); } catch (Exception ex2) {}
					try { os.close(); } catch (Exception ex2) {}
				}
				
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				try { socket.close(); } catch(Exception ex) {}
			}
			
			
			
		}

	}

}












