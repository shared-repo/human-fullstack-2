import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer {

	public static void main(String[] args) {
		
		System.out.println("메시지 테스트 서버가 시작되었습니다.");
		
		ServerSocket listener = null; // 클라이언트의 연결 요청을 수신해서 통신 소켓을 만들고 연결하는 도구
		try {
			listener = new ServerSocket(9999, 100);
			
			while (true) {
				Socket socket = listener.accept(); // 클라이언트 연결 요청 기다리기 -> 연결 요청이 오면 소켓 생성 + 연결
				// 대화 ( 데이터 수신 + 데이터 송신 )
				InputStream is = null;			// 데이터 수신 스트림 (byte[])
				InputStreamReader isr = null;	// byte[] -> char[] 변환기
				BufferedReader br = null;		// 한 줄 단위로 끊어서 처리하는 도구
				
				OutputStream os = null;			// 데이터 송신 스트림 (byte[])
				PrintWriter pw = null;			// char[] -> byte[], 그 외 system.out 객체의 기능 포함
				try {
					is = socket.getInputStream();	// 소켓에 연결된 읽기 스트림 가져오기
					isr = new InputStreamReader(is);
					br = new BufferedReader(isr);
					String message = br.readLine();	// 클라이언트가 송신한 데이터 읽기
					System.out.printf("[%s] : %s\n", socket.getRemoteSocketAddress(), message);
					os = socket.getOutputStream();// 소켓에 연결된 쓰기 스트림 가져오기
					pw = new PrintWriter(os); 
					pw.println("[MESSAGE FROM SERVER] : " + message); // 클라이언트로 데이터 전송
					pw.flush(); // 전송되지 않은 데이터 강제 전송 
					
				} catch (Exception ex2) {
					ex2.printStackTrace();
				} finally {
					try { br.close(); } catch (Exception ex2) {}
					try { isr.close(); } catch (Exception ex2) {}
					try { is.close(); } catch (Exception ex2) {}
					try { pw.close(); } catch (Exception ex2) {}
					try { os.close(); } catch (Exception ex2) {}
					try { socket.close(); } catch (Exception ex2) {}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			System.out.println("finally");
			try { listener.close(); } catch(Exception ex) {}
		}

	}

}
