import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

public class ToDoManager2 {

	private Scanner scanner = new Scanner(System.in);	
	private ArrayList<ToDo2> toDos = new ArrayList<>();
	
	@SuppressWarnings("unchecked")
	public ToDoManager2() {
		
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = new FileInputStream("todos.dat");
			ois = new ObjectInputStream(fis);
			toDos = (ArrayList<ToDo2>)ois.readObject();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try { ois.close(); } catch (Exception ex) {}
			try { fis.close(); } catch (Exception ex) {}
		}
	}
	
	public String selectTask() {
		System.out.println("************** 할 일 관리 ****************");
		System.out.print("* 1.등록 ");
		System.out.print("2.수정 ");
		System.out.print("3.삭제 ");
		System.out.print("4.목록 ");
		System.out.print("5.검색 ");
		System.out.println("0.종료 *");
		System.out.println("****************************************");
		System.out.print("작업을 선택하세요 : ");
		String selectedTask = scanner.nextLine();
		return selectedTask;
	}

	public void run() {
		main: while (true) {
			System.out.println();
			String task = selectTask();
			System.out.println();
			
			switch (task) {
			case "0": 
				save();
				break main; // main 이름이 붙은 반복문 또는 switch문 종료
			case "1": 
				register();
				break;
			case "4":
				list();
				break;
			case "5":
				search();
				break;
			default : 
				System.out.println("지원하지 않는 태스크입니다.");
			}
		}
		
		System.out.println("프로그램을 종료합니다.");
	}
	
	private void save() {
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try {
			fos = new FileOutputStream("todos.dat");
			oos = new ObjectOutputStream(fos);
			oos.writeObject(toDos);
			
		} catch (IOException e) {
			e.printStackTrace(); // 오류 메시지를 화면에 출력하는 명령
		} finally {
			try { oos.close(); } catch(Exception e) {}
			try { fos.close(); } catch(Exception e) {}
		}		
	}

	public void register() {
		System.out.println("[ 새 할 일 정보 ]");
		System.out.print("할 일 : ");
		String title = scanner.nextLine();
		long tick = new Date().getTime(); // Date : 날짜 관리 클래스, getTime() : 1970.1.1 0:0:0 초 이후 경과 시간
		ToDo2 toDo = new ToDo2(tick, title, false);
		toDos.add(toDo);
		
		System.out.println("할 일 등록 완료");
	}
	
	public void list() {
		if (toDos.size() == 0) {
			System.out.println("등록된 할 일이 없습니다.");
		} else {
			System.out.println("[ 연락처 목록 ]");
			for (ToDo2 toDo : toDos) {
				toDo.display();
			}
		}
	}
	
	public void search() {
		ArrayList<ToDo2> result = new ArrayList<ToDo2>(); // 검색 결과를 저장할 리스트 변수
		
		System.out.println("[ 검색 정보 ]");
		System.out.print("검색어 : ");
		String keyword = scanner.nextLine();		
		for (ToDo2 toDo : toDos) {
			if (toDo.getTitle().contains(keyword)) { // contains : 어떤 문자열을 포함하고 있으면 true
				result.add(toDo);
			}
		}
		
		if (result.size() == 0) {
			System.out.println("검색 결과가 없습니다.");
		} else {
			System.out.println("[ 검색 결과 ]");
			for (ToDo2 toDo : result) {
				toDo.display();
			}
		}
	}
	
	public static void main(String[] args) {
		
		ToDoManager2 manager = new ToDoManager2();
		manager.run();

	}

}











