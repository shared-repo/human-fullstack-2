import java.io.Serializable;

public class ToDo2 implements Serializable {
	
	private long no;
	private String title;
	private boolean completed;
	
	public ToDo2() {}
	public ToDo2(long no, String title, boolean completed) {
		this.no = no;
		this.title = title;
		this.completed = completed;
	}
	
	public long getNo() {
		return no;
	}
	public void setNo(long no) {
		this.no = no;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public boolean isCompleted() {
		return completed;
	}
	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public void display() {
		System.out.printf("[%d][%s][%s]\n", no, title, completed ? "완료" : "진행중");
	}
	
}
