import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Person {
	
	@Getter @Setter // getName(), setName()을 생성하는 명령
	private String name;
	@Getter @Setter
	private String email;
	@Getter @Setter
	private String phone;

}
