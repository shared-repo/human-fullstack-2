// src/main/java/com/example/imageboard/config/DataInitializer.java
package com.example.imageboard.config;

import com.example.imageboard.entity.Member;
import com.example.imageboard.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.File;

// ApplicationRunner 인터페이스를 구현한 클래스를 IoC 컨테이너에 등록하면
// 애플리케이션이 시작되었을 때 자동으로 등록된 빈의 run 메서드롤 호출
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;  // 추가

    @Override
    public void run(ApplicationArguments args) {

        File file = new File("/app/config");
        if (!file.exists()) {
            file.mkdirs();
        }

        File file2 = new File("/app/config/test.txt");
        try {
            file2.createNewFile();
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        if (memberRepository.count() == 0) {
            memberRepository.save(
                    Member.create(
                            "admin",
                            passwordEncoder.encode("password123"),   // 암호화 적용
                            "관리자"
                    )
            );
        }
    }
}
