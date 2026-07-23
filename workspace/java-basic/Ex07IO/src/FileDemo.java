import java.io.File;
import java.io.IOException;

public class FileDemo {

	public static void main(String[] args) throws IOException {
		
		String path = "D:\\instructor-och\\human-fullstack-2\\workspace\\java-basic\\Ex07IO";
		File file = new File(path);
		
		if (file.exists()) { // 파일 또는 디렉터리가 있는지 없는지 검사
			System.out.println("존재하는 디렉터리입니다.");
		} else {
			System.out.println("존재하지 않는 디렉터리입니다.");
		}
		
		path = "D:\\instructor-och\\human-fullstack-2\\workspace\\java-basic\\Ex07IO\\newfile.txt";
		file = new File(path);
		if (file.exists()) {
			file.delete(); // 지정된 경로와 이름으로 파일 삭제
			System.out.println("파일을 삭제했습니다.");
		}
		
		file.createNewFile(); // 지정된 경로와 이름으로 새 파일 만들기
		
		path = "D:\\instructor-och\\human-fullstack-2\\workspace\\java-basic\\Ex07IO\\newdir";
		file = new File(path);
		file.mkdir(); // 지정된 경로의 디렉터리 만들기
		

	}

}
