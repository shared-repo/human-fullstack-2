// src/main/java/com/example/imageboard/service/MemberService.java
package com.example.imageboard.service;

import com.example.imageboard.dto.MemberCreateRequest;
import com.example.imageboard.entity.Member;
import com.example.imageboard.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /** 회원가입 */
    public void register(MemberCreateRequest request) {

        // 아이디 중복 확인
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        // 비밀번호 암호화 후 저장
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        Member member = Member.create(
                request.getUsername(),
                encodedPassword,         // 평문 ❌ → BCrypt 해시 ✅
                request.getNickname()
        );
        memberRepository.save(member);
    }
}
