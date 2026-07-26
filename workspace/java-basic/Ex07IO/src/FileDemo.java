import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
		
		System.out.println("=========================================");
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd a hh:mm"); // 날짜의 문자열 표현 형식을 지정하는 도구
		path = "C:\\Users\\human";
		File currentDir = new File(path);
		File[] filesAndDirs = currentDir.listFiles(); // 현재 디렉터리에 포함된 파일과 디렉터리 목록 반환
		
		for (File f : filesAndDirs) {
			Date d = new Date(f.lastModified());
			if (f.isDirectory()) { // 디렉터리인 경우
				System.out.printf("%s %5s %13s %s\n", sdf.format(d),
													  "<DIR>",
													  "",
													  f.getName());
			}
			if (f.isFile()) { // 파일인 경우
				System.out.printf("%s %5s %,13d %s\n", sdf.format(d),
													  "",
													  f.length(),
													  f.getName());
			}
		}
		
		
		
		

	}

}
















