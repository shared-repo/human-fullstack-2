// src/main/java/com/example/imageboard/security/CustomUserDetails.java
package com.example.imageboard.security;

import com.example.imageboard.entity.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Member member;    // 원본 엔티티 보관
    public CustomUserDetails(Member member) {
        this.member = member;
    }

    // 권한 목록 — 현재는 ROLE_USER 단일 권한
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() { return member.getPassword(); }

    @Override
    public String getUsername() { return member.getUsername(); }

    // 계정 상태 (현재는 모두 활성화)
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

    /** 편의 메서드 — Member ID 직접 접근 */
    public Long getMemberId() { return member.getId(); }

    /** 편의 메서드 — 닉네임 직접 접근 */
    public String getNickname() { return member.getNickname(); }
}
