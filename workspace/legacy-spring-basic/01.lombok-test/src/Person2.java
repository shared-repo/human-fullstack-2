import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data // 모든 필드에 @Getter, @Setter 설정, @ToString
@NoArgsConstructor
@AllArgsConstructor
public class Person2 {
	
	private String name;
	private String email;
	private String phone;

}
