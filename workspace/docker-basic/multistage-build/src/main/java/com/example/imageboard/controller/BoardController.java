package com.example.imageboard.controller;

import com.example.imageboard.dto.BoardCreateRequest;
import com.example.imageboard.dto.BoardResponse;
import com.example.imageboard.dto.BoardUpdateRequest;
import com.example.imageboard.security.CustomUserDetails;
import com.example.imageboard.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping(path = { "/boards" })
@RequiredArgsConstructor // final 및 @NonNull annotation이 적용된 필드를 전달인자로 하는 생성자 메서드 자동 구현
public class BoardController {

    private final BoardService boardService;

    /** 게시글 목록 */
//    @GetMapping(path = { "", "/", "/list" })
//    public String list(Model model) {
//        List<BoardResponse> boards = boardService.findAll();
//        model.addAttribute("boards", boards); // Model 타입 전달인자에 데이터를 저장하면 View에서 사용할 수 있습니다.
//        // return "board/list-old";           // templates/board/list-old.html
//        return "board/list";           // templates/board/list.html
//    }
    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {

        Page<BoardResponse> boardPage = boardService.findAllByPage(keyword, page, 2);

        model.addAttribute("boardPage", boardPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        return "board/list";
    }


    /** 게시글 상세 */
    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        BoardResponse board = boardService.findById(id);
        model.addAttribute("board", board);
        return "board/detail";         // templates/board/detail.html
    }

    /** 게시글 작성 폼 */
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("boardCreateRequest", new BoardCreateRequest());
        return "board/create";         // templates/board/create.html
    }

    /** 게시글 저장 */
    @PostMapping
    public String create(@Valid @ModelAttribute BoardCreateRequest request,
                         BindingResult bindingResult, // BindingResult 타입 전달인자 : @Valid에 의해 유효성 검사한 결과 저장
                         @AuthenticationPrincipal CustomUserDetails userDetails,
                         Model model) {

        // System.out.println(request.getTitle() + " / " + request.getContent());
        if (bindingResult.hasErrors()) {
            // 오류가 있으면 폼으로 다시 이동 (Model에 오류 정보 자동 포함)
            return "board/create";
        }

        Long id = boardService.create(request, userDetails.getMemberId());
        return "redirect:/boards/" + id;
    }

    /** 게시글 삭제 — 작성자 본인만 */
    @DeleteMapping("/{id}")
    @PreAuthorize("@boardSecurity.isOwner(#id, #userDetails.memberId)") // 처리기 실행 전에 인증 검사
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal CustomUserDetails userDetails) { // 현재 로그인한 사용자 정보 수신 (UserDetails 구현 객체)
        boardService.delete(id);
        return "redirect:/boards";
    }

    /** 게시글 수정 폼 — 작성자 본인만 */
    @GetMapping("/{id}/edit")
    @PreAuthorize("@boardSecurity.isOwner(#id, #userDetails.memberId)")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal CustomUserDetails userDetails,
                           Model model) {
        BoardResponse board = boardService.findById(id);
        BoardUpdateRequest request = new BoardUpdateRequest();
        request.setTitle(board.getTitle());
        request.setContent(board.getContent());
        model.addAttribute("boardId", id);
        model.addAttribute("board", board);
        model.addAttribute("boardUpdateRequest", request);
        return "board/edit";
    }

    /** 게시글 수정 */
    @PutMapping("/{id}")
    @PreAuthorize("@boardSecurity.isOwner(#id, #userDetails.memberId)")
    public String update(@PathVariable Long id,
                         @AuthenticationPrincipal CustomUserDetails userDetails,
                         @Valid @ModelAttribute BoardUpdateRequest request,
                         BindingResult bindingResult,
                         Model model) {

        if (bindingResult.hasErrors()) {
            BoardResponse board = boardService.findById(id);
            model.addAttribute("boardId", id);
            model.addAttribute("board", board);
            model.addAttribute("boardUpdateRequest", request);
            // 오류가 있으면 폼으로 다시 이동 (Model에 오류 정보 자동 포함)
            return "board/edit";
        }

        boardService.update(id, request);
        return "redirect:/boards/" + id;
    }

    /** 이미지 개별 삭제 */
    // @DeleteMapping("/{boardId}/images/{imageId}")
    @PostMapping("/{boardId}/images/{imageId}")
    @ResponseBody // 이 메서드의 반환 값은 html 파일 이름이 아니고 반환 내용 그대로 응답 처리하는 설정
    public ResponseEntity<Void> deleteImage(@PathVariable Long boardId,
                                      @PathVariable Long imageId) {
        boardService.deleteImage(boardId, imageId);
        return ResponseEntity.noContent().build(); // 성공 적인 처리 + 응답 결과가 없음 ( status code : 204 )
    }

}
