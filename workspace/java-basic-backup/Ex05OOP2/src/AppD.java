class TheShape {
	public void draw() {
		System.out.println("TheShape.draw");
	}
}
class TheRectangle extends TheShape {
	public void draw() {
		System.out.println("TheRectangle.draw");
	}
}
class TheOval extends TheShape {
	public void draw() {
		System.out.println("TheOval.draw");
	}
}
class TheLine extends TheShape {
	public void draw() {
		System.out.println("TheLine.draw");
	}
}

public class AppD {

	public static void main(String[] args) {
		
		// 1. 전달인자에 다형성 적용
		TheRectangle r1 = new TheRectangle();
		doDraw(r1);		
		TheOval o1 = new TheOval();
		doDraw(o1);
		TheLine l1 = new TheLine();
		doDraw(l1);
		
		System.out.println("---------------------------------");
		
		// 2. 배열에 다형성 적용
		//    Rectangle 4개, Oval 4개, Line 4개 만들어서 사용 --> 배열 활용
		//    배열은 같은 자료형의 데이터만 모아서 저장할 수 있습니다.
		TheShape[] shapes = new TheShape[12]; // TheShape 배열을 만들면 이 배열에는 TheShape 인스턴스와 자식 인스턴스를 모두 저장할 수 있습니다.
		for (int i = 0; i < shapes.length; i++) { // 배열.length : 배열 요소의 갯수 (여기서는 12개)
			if (i%3 == 0) {
				shapes[i] = new TheRectangle(); // Shape 참조변수 -> Rectangle 인스턴스 ( 부모참조 -> 자식인스턴스 )
			} else if (i%3 == 1) {
				shapes[i] = new TheOval();
			} else {
				shapes[i] = new TheLine();
			}
		}
		for (int i = 0; i < shapes.length; i++) {
			shapes[i].draw(); // shapes[i] 변수가 참조하는 인스턴스의 draw() 메서드 호출
		}
		
		
	}
	
//	static void doDraw(TheRectangle r) {
//		r.draw();
//	}
//	static void doDraw(TheOval o) {
//		o.draw();
//	}
//	static void doDraw(TheLine l) {
//		l.draw();
//	}
	
	// 전달인자 TheShape s는 TheLine, TheRectangle, TheOval을 받을 수 있습니다.
	// --> 메서드 오버로딩 필요 없습니다.
	static void doDraw(TheShape s) {
		// 만약 참조 타입 기준으로 호출한다면 항상 TheShape의 draw 호출
		// 인스턴스 타입 기준으로 호출하기 때문에 전달인자에 따라 다른 호출 수행
		s.draw(); 
	}

}