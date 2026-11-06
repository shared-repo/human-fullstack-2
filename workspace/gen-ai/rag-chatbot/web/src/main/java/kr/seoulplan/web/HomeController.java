package kr.seoulplan.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** home 화면 (F3-1). 이 애플리케이션의 유일한 화면이다. */
@Controller
public class HomeController {

	@GetMapping("/")
	public String home() {
		return "home";
	}

}
