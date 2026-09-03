// src/main/java/com/example/imageboard/entity/Member.java
package com.example.imageboard.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자 (외부 직접 생성 방지)
@ToString(exclude = "boards")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @OneToMany(mappedBy = "member") // Board Entity에 설정된 관계 필드 member에 관계를 연결
    private java.util.List<Board> boards = new java.util.ArrayList<>();

    // 생성 팩토리 메서드 — 객체 생성 방법을 한 곳에서 관리
    public static Member create(String username, String password, String nickname) {
        Member member = new Member();
        member.username = username;
        member.password = password;
        member.nickname = nickname;
        return member;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}
