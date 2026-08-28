package com.example.imageboard.service;

import com.example.imageboard.dto.BoardCreateRequest;
import com.example.imageboard.dto.BoardResponse;
import com.example.imageboard.dto.BoardUpdateRequest;
import com.example.imageboard.entity.AttachedImage;
import com.example.imageboard.entity.Board;
import com.example.imageboard.entity.Member;
import com.example.imageboard.repository.AttachedImageRepository;
import com.example.imageboard.repository.BoardRepository;
import com.example.imageboard.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final AttachedImageRepository imageRepository;
    private final FileService fileService;

    /** 게시글 목록 조회 */
    public List<BoardResponse> findAll() {
        return boardRepository.findAllWithMember().stream()
                .map(this::toResponse)
                .toList();
    }

    // 게시글 목록을 페이지 단위로 조회
    public Page<BoardResponse> findAllByPage(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);  // page:0-based 페이지 번호, size:한 페이지에 포함되는 데이터 갯수

        Page<Board> boardPage = (keyword == null || keyword.isBlank())
                ? boardRepository.findAllWithMemberByPage(pageable)
                : boardRepository.searchWithMemberByPage(keyword, pageable);

        return boardPage.map(this::toResponse);
    }


    /** 게시글 단건 조회 */
    public BoardResponse findById(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
        board.increaseViewCount(); // 변경 감지로 UPDATE 자동 실행
        return toResponse(board);
    }

    /** 게시글 등록 */
    public Long create(BoardCreateRequest request, Long memberId) {  // memberId 파라미터 추가
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        Board board = Board.create(request.getTitle(), request.getContent(), member);

        // 이미지 업로드 처리
        if (request.getImages() != null) {
            request.getImages().stream()
                    .filter(file -> !file.isEmpty())
                    .forEach(file -> {
                        String storedName = fileService.store(file);
                        fileService.createThumbnail(storedName);

                        AttachedImage image = AttachedImage.create(
                                file.getOriginalFilename(),
                                storedName,
                                "/images/" + storedName,
                                file.getSize()
                        );
                        board.addImage(image);  // Board에 이미지 추가
                    });
        }

        boardRepository.save(board);
        return board.getId();
    }

    /** 게시글 삭제 */
    public void delete(Long id) {
        // boardRepository.deleteById(id); // 해당 id의 데이터가 있는지 알 수 없는 상태에서 삭제 시도
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));

        // 첨부 이미지 파일 삭제
        board.getImages()
                .forEach(image -> fileService.delete(image.getStoredName()));

        boardRepository.delete(board);

    }

    /** 게시글 수정 */
    public void update(Long id, BoardUpdateRequest request) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
        board.update(request.getTitle(), request.getContent()); // 변경 감지

        // 추가 이미지 저장
        if (request.getImages() != null) {
            request.getImages().stream()
                    .filter(file -> !file.isEmpty())
                    .forEach(file -> {
                        String storedName = fileService.store(file);
                        fileService.createThumbnail(storedName);

                        AttachedImage image = AttachedImage.create(
                                file.getOriginalFilename(),
                                storedName,
                                "/images/" + storedName,
                                file.getSize()
                        );
                        board.addImage(image);
                    });
        }

        boardRepository.save(board);
    }

    public void deleteImage(Long boardId, Long imageId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        AttachedImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("이미지를 찾을 수 없습니다."));

        // Board에서 이미지 제거 (orphanRemoval로 DB 레코드 자동 삭제)
        board.getImages().remove(image);
        boardRepository.save(board);

        // 실제 파일 삭제
        fileService.delete(image.getStoredName());
    }

    /** Entity → DTO 변환 */
    private BoardResponse toResponse(Board board) {
        List<BoardResponse.ImageResponse> images = board.getImages().stream()
                .map(img -> BoardResponse.ImageResponse.builder()
                        .id(img.getId())
                        .originalName(img.getOriginalName())
                        .storedName(img.getStoredName())
                        .build())
                .toList();

        return BoardResponse.builder()
                .id(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .author(board.getMember().getNickname())
                .memberId(board.getMember().getId())
                .viewCount(board.getViewCount())
                .thumbnailUrl(board.getThumbnailUrl())
                .images(images)
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build();
    }

}
