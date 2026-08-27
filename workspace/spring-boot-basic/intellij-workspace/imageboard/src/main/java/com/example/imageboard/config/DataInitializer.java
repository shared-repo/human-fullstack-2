// src/main/java/com/example/imageboard/config/DataInitializer.java
package com.example.imageboard.config;

import com.example.imageboard.entity.Member;
import com.example.imageboard.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// ApplicationRunner 인터페이스를 구현한 클래스를 IoC 컨테이너에 등록하면
// 애플리케이션이 시작되었을 때 자동으로 등록된 빈의 run 메서드롤 호출
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (memberRepository.count() == 0) {
            Member member = Member.create("admin", "password123", "관리자");
            memberRepository.save(member);
            System.out.println("[초기화] 테스트 회원 데이터 생성 완료");
        }
    }
}
