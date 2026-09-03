package com.example.imageboard.repository;

import com.example.imageboard.entity.Board;
import com.example.imageboard.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {

    // JOIN FETCH — 연관 Entity를 한 번에 조회 (N+1 문제 해결)
    @Query("SELECT b FROM Board b JOIN FETCH b.member ORDER BY b.createdAt DESC")
    List<Board> findAllWithMember();

    // 전체 목록 페이징
    @Query("SELECT b FROM Board b JOIN FETCH b.member ORDER BY b.createdAt DESC")
    Page<Board> findAllWithMemberByPage(Pageable pageable);

    // 키워드 검색 + 페이징
    @Query(value = """
            SELECT b FROM Board b JOIN FETCH b.member
            WHERE b.title LIKE %:keyword% OR b.content LIKE %:keyword%
            ORDER BY b.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(b) FROM Board b
            WHERE b.title LIKE %:keyword% OR b.content LIKE %:keyword%
            """)
    Page<Board> searchWithMemberByPage(@Param("keyword") String keyword, Pageable pageable);

}
