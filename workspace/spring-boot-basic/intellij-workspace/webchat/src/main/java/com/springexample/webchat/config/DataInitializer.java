package com.springexample.webchat.config;

import com.springexample.webchat.domain.User;
import com.springexample.webchat.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {
        if (userRepository.count() > 0) return; // 이미 데이터가 있으면 건너뜀

        userRepository.saveAll(List.of(
                new User("아이유", passwordEncoder.encode("1234")),
                new User("장동건",   passwordEncoder.encode("1234")),
                new User("에이티즈", passwordEncoder.encode("1234"))
        ));

        System.out.println("[DataInitializer] 테스트 사용자 3명 생성 완료");
    }
}
